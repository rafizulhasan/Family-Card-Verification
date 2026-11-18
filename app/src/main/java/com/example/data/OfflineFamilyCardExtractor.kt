package com.example.data

import com.example.model.ExtractedFamilyCardData
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.Inflater
import kotlin.math.abs

data class PdfPositionedText(
    val pageNumber: Int,
    val x: Float,
    val y: Float,
    val text: String
)

object OfflineFamilyCardExtractor {

    fun extractPositionedTextFromPdf(pdfBytes: ByteArray): List<PdfPositionedText> {
        val rawContent = String(pdfBytes, StandardCharsets.ISO_8859_1)
        val streamRegex = Regex("stream\\r?\\n([\\s\\S]*?)\\r?\\nendstream")
        val results = mutableListOf<PdfPositionedText>()
        var pageIndex = 1

        for (match in streamRegex.findAll(rawContent)) {
            val streamData = match.groups[1]?.value?.toByteArray(StandardCharsets.ISO_8859_1) ?: continue
            val decompressed = tryInflateStream(streamData) ?: streamData
            val streamText = String(decompressed, StandardCharsets.ISO_8859_1)
            if (streamText.contains("BT") && streamText.contains("ET")) {
                val extractedFromPage = parsePdfTextBlocks(streamText, pageIndex)
                if (extractedFromPage.isNotEmpty()) {
                    results.addAll(extractedFromPage)
                    pageIndex++
                }
            }
        }
        return results
    }

    private fun tryInflateStream(bytes: ByteArray): ByteArray? {
        return try {
            val inflater = Inflater()
            inflater.setInput(bytes)
            val out = ByteArrayOutputStream(bytes.size * 2)
            val buffer = ByteArray(4096)
            while (!inflater.finished() && !inflater.needsInput()) {
                val count = inflater.inflate(buffer)
                if (count <= 0) break
                out.write(buffer, 0, count)
            }
            inflater.end()
            val result = out.toByteArray()
            if (result.isNotEmpty()) result else null
        } catch (_: Exception) {
            null
        }
    }

    private fun parsePdfTextBlocks(contentStream: String, pageNumber: Int): List<PdfPositionedText> {
        val items = mutableListOf<PdfPositionedText>()
        val btEtRegex = Regex("BT([\\s\\S]*?)ET")
        val tmRegex = Regex("(-?\\d+(?:\\.\\d+)?)\\s+(-?\\d+(?:\\.\\d+)?)\\s+(-?\\d+(?:\\.\\d+)?)\\s+(-?\\d+(?:\\.\\d+)?)\\s+(-?\\d+(?:\\.\\d+)?)\\s+(-?\\d+(?:\\.\\d+)?)\\s+Tm")
        val tdRegex = Regex("(-?\\d+(?:\\.\\d+)?)\\s+(-?\\d+(?:\\.\\d+)?)\\s+T[dD]")
        val hexTjRegex = Regex("<([0-9A-Fa-f\\s]+)>\\s*Tj")
        val parenLiteralRegex = Regex("\\(((?:\\\\.|[^\\\\()])*)\\)")

        for (blockMatch in btEtRegex.findAll(contentStream)) {
            val block = blockMatch.groupValues[1]
            var curX = 0f
            var curY = 0f
            var hasCoords = false

            tmRegex.find(block)?.let { m ->
                curX = m.groupValues[5].toFloatOrNull() ?: 0f
                curY = m.groupValues[6].toFloatOrNull() ?: 0f
                hasCoords = true
            }
            tdRegex.find(block)?.let { m ->
                curX += m.groupValues[1].toFloatOrNull() ?: 0f
                curY += m.groupValues[2].toFloatOrNull() ?: 0f
                hasCoords = true
            }

            if (!hasCoords) continue

            val textBuilder = StringBuilder()
            for (hexMatch in hexTjRegex.findAll(block)) {
                val hex = hexMatch.groupValues[1].replace(Regex("\\s+"), "")
                textBuilder.append(decodeUtf16BeOrByteHex(hex))
            }
            for (litMatch in parenLiteralRegex.findAll(block)) {
                val rawIso = litMatch.groupValues[1]
                val unescapedBytes = unescapePdfBytes(rawIso)
                textBuilder.append(decodePdfStringBytes(unescapedBytes))
            }

            val decoded = BengaliPdfUnicodeDecoder.decodeVisualBengali(textBuilder.toString())
            if (decoded.isNotBlank()) {
                val isFooter = curY <= 16f && (
                    decoded.contains("প্রস্তুতির সময়") ||
                        decoded.contains("প্রস্তুতির সময়") ||
                        decoded.contains("কপিরাইট") ||
                        decoded.contains("ডিএসএস") ||
                        Regex("^\\d+\\s*/\\s*\\d+$").matches(decoded)
                    )
                if (!isFooter) {
                    items.add(
                        PdfPositionedText(
                            pageNumber = pageNumber,
                            x = curX,
                            y = curY,
                            text = decoded
                        )
                    )
                }
            }
        }
        return items
    }

    private fun unescapePdfBytes(rawIsoString: String): ByteArray {
        val raw = rawIsoString.toByteArray(StandardCharsets.ISO_8859_1)
        val out = ByteArrayOutputStream(raw.size)
        var i = 0
        val n = raw.size
        while (i < n) {
            val b = raw[i].toInt() and 0xFF
            if (b == 0x5C && i + 1 < n) {
                val nxt = raw[i + 1].toInt() and 0xFF
                when (nxt) {
                    'n'.code -> {
                        out.write(0x0A)
                        i += 2
                    }
                    'r'.code -> {
                        out.write(0x0D)
                        i += 2
                    }
                    't'.code -> {
                        out.write(0x09)
                        i += 2
                    }
                    'b'.code -> {
                        out.write(0x08)
                        i += 2
                    }
                    'f'.code -> {
                        out.write(0x0C)
                        i += 2
                    }
                    '('.code, ')'.code, '\\'.code -> {
                        out.write(nxt)
                        i += 2
                    }
                    in '0'.code..'7'.code -> {
                        val octBuilder = StringBuilder()
                        octBuilder.append(nxt.toChar())
                        i += 2
                        var digits = 1
                        while (digits < 3 && i < n) {
                            val d = raw[i].toInt() and 0xFF
                            if (d in '0'.code..'7'.code) {
                                octBuilder.append(d.toChar())
                                i++
                                digits++
                            } else {
                                break
                            }
                        }
                        out.write(octBuilder.toString().toInt(8) and 0xFF)
                    }
                    else -> {
                        out.write(nxt)
                        i += 2
                    }
                }
            } else {
                out.write(b)
                i++
            }
        }
        return out.toByteArray()
    }

    private fun decodePdfStringBytes(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""
        val looksLikeUtf16Be = bytes.size >= 2 && bytes.size % 2 == 0 && (
            bytes.indices.step(2).any { idx ->
                val hi = bytes[idx].toInt() and 0xFF
                hi == 0x00 || hi == 0x09 || hi == 0xE0 || hi == 0xE1 || hi == 0x20
            }
            )
        return if (looksLikeUtf16Be) {
            String(bytes, StandardCharsets.UTF_16BE)
        } else {
            String(bytes, StandardCharsets.UTF_8)
        }
    }

    private fun decodeUtf16BeOrByteHex(hex: String): String {
        if (hex.length >= 4 && hex.length % 4 == 0) {
            val sb = StringBuilder(hex.length / 4)
            var i = 0
            while (i < hex.length) {
                val codePoint = hex.substring(i, i + 4).toIntOrNull(16) ?: 0
                if (codePoint > 0) sb.append(codePoint.toChar())
                i += 4
            }
            return sb.toString()
        }
        val sb = StringBuilder(hex.length / 2)
        var i = 0
        while (i + 1 < hex.length) {
            val b = hex.substring(i, i + 2).toIntOrNull(16) ?: 0
            if (b > 0) sb.append(b.toChar())
            i += 2
        }
        return sb.toString()
    }

    private data class VisualRow(
        val page: Int,
        val y: Float,
        val tokens: List<PdfPositionedText>
    )

    fun parseFromPositionedItems(items: List<PdfPositionedText>): ExtractedFamilyCardData {
        if (items.isEmpty()) return ExtractedFamilyCardData()

        val sortedByPageAndLine = items.sortedWith(
            compareBy<PdfPositionedText> { it.pageNumber }
                .thenByDescending { it.y }
                .thenBy { it.x }
        )

        val visualRows = mutableListOf<VisualRow>()
        var currentPage = -1
        var currentLineY = Float.NaN
        var currentRowTokens = mutableListOf<PdfPositionedText>()

        fun flushRow() {
            if (currentRowTokens.isNotEmpty()) {
                visualRows.add(
                    VisualRow(
                        page = currentPage,
                        y = currentLineY,
                        tokens = currentRowTokens.sortedBy { it.x }
                    )
                )
                currentRowTokens = mutableListOf()
            }
        }

        for (item in sortedByPageAndLine) {
            if (item.pageNumber != currentPage || currentLineY.isNaN() || abs(item.y - currentLineY) > 4.0f) {
                flushRow()
                currentPage = item.pageNumber
                currentLineY = item.y
                currentRowTokens.add(item)
            } else {
                currentRowTokens.add(item)
            }
        }
        flushRow()

        // Reconstruct logical key-value lines preserving 2-column and wrapped 1-column cells
        val logicalLines = mutableListOf<String>()
        var i = 0
        while (i < visualRows.size) {
            val row = visualRows[i]
            val tokens = row.tokens

            // Check if this is a 2-column row (tokens on both left x < 280 and right x >= 280)
            val leftTokens = tokens.filter { it.x < 280f }
            val rightTokens = tokens.filter { it.x >= 280f }

            if (leftTokens.isNotEmpty() && rightTokens.isNotEmpty() && tokens.none { it.x in 350f..385f }) {
                // Might have a wrapped continuation line right below (e.g., সামাজিক নিরাপত্তা / কর্মসূচির আওতায়)
                var leftLabel = leftTokens.filter { it.x < 130f }.joinToString(" ") { it.text }
                val leftVal = leftTokens.filter { it.x >= 130f }.joinToString(" ") { it.text }
                var rightLabel = rightTokens.filter { it.x < 390f }.joinToString(" ") { it.text }
                val rightVal = rightTokens.filter { it.x >= 390f }.joinToString(" ") { it.text }

                if (i + 1 < visualRows.size && visualRows[i + 1].page == row.page && (row.y - visualRows[i + 1].y) in 8f..15f) {
                    val nextRow = visualRows[i + 1]
                    val nextHasValues = nextRow.tokens.any { it.x in 130f..279f || it.x >= 390f }
                    if (!nextHasValues) {
                        val nextLeftLabel = nextRow.tokens.filter { it.x < 130f }.joinToString(" ") { it.text }
                        val nextRightLabel = nextRow.tokens.filter { it.x in 280f..389f }.joinToString(" ") { it.text }
                        if (nextLeftLabel.isNotBlank()) leftLabel = "$leftLabel $nextLeftLabel".trim()
                        if (nextRightLabel.isNotBlank()) rightLabel = "$rightLabel $nextRightLabel".trim()
                        i++
                    }
                }

                if (leftLabel.isNotBlank() && leftVal.isNotBlank()) {
                    logicalLines.add("$leftLabel : $leftVal")
                } else if (leftTokens.isNotEmpty()) {
                    logicalLines.add(leftTokens.joinToString(" : ") { it.text })
                }

                if (rightLabel.isNotBlank() && rightVal.isNotBlank()) {
                    logicalLines.add("$rightLabel : $rightVal")
                } else if (rightTokens.isNotEmpty()) {
                    logicalLines.add(rightTokens.joinToString(" : ") { it.text })
                }
                i++
                continue
            }

            // Check if this is a 1-column label+value row where the label wraps onto 1 or 2 following rows
            val labelPart = tokens.filter { it.x < 210f }.joinToString(" ") { it.text }
            val valuePart = tokens.filter { it.x >= 210f }.joinToString("   ") { it.text }

            if (labelPart.isNotBlank() && valuePart.isNotBlank() && tokens.none { it.x >= 350f }) {
                val combinedLabel = StringBuilder(labelPart)
                while (
                    i + 1 < visualRows.size &&
                    visualRows[i + 1].page == row.page &&
                    (visualRows[i].y - visualRows[i + 1].y) in 8f..15f &&
                    visualRows[i + 1].tokens.all { it.x < 210f }
                ) {
                    val contText = visualRows[i + 1].tokens.joinToString(" ") { it.text }
                    if (isSectionHeader(contText)) break
                    combinedLabel.append(" ").append(contText)
                    i++
                }
                logicalLines.add("${combinedLabel.toString().trim()} : $valuePart")
                i++
                continue
            }

            logicalLines.add(tokens.joinToString("   ") { it.text.trim() })
            i++
        }

        val joinedText = logicalLines.joinToString("\n")
        val maxPage = items.maxOfOrNull { it.pageNumber } ?: 1
        return OcrFamilyCardParser.parseFromOcrText(joinedText).copy(pageCount = maxPage)
    }

    private fun isSectionHeader(text: String): Boolean {
        val headers = listOf(
            "কার্যক্রমের তথ্য", "ছবি ও নথিপত্র", "ব্যক্তিগত তথ্য", "স্থায়ী ঠিকানা", "স্থায়ী ঠিকানা",
            "ব্যাংক / এমএফএস তথ্য", "পিএমটি তথ্য", "অতিরিক্ত ভাতার তথ্য",
            "পরিবারের সদস্য", "অর্থ ব্যবহার", "এন্ট্রি ব্যবহারকারীর তথ্য"
        )
        return headers.any { text.contains(it) } || text.startsWith("সদস্য ")
    }
}
