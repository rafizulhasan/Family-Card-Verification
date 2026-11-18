package com.example.data

import android.content.Context
import com.example.model.ExtractedFamilyCardData
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PipelineExtractionOutcome(
    val extractedData: ExtractedFamilyCardData,
    val engineUsedLabel: String,
    val usedOcrFallback: Boolean,
    val extractedCharCount: Int
)

object FamilyCardVerificationPipeline {

    suspend fun extractWithAutomaticOcrFallback(
        context: Context,
        pdfBytes: ByteArray,
        onStartingOcrFallback: (totalPages: Int) -> Unit = {},
        onOcrPageProgress: (currentPage: Int, totalPages: Int) -> Unit = { _, _ -> }
    ): PipelineExtractionOutcome = withContext(Dispatchers.Default) {
        val directPositioned = try {
            OfflineFamilyCardExtractor.extractPositionedTextFromPdf(pdfBytes)
        } catch (_: Exception) {
            emptyList()
        }
        val directChars = directPositioned.sumOf { item -> item.text.count { !it.isWhitespace() } }
        if (directChars > 0) {
            val parsedDirect = OfflineFamilyCardExtractor.parseFromPositionedItems(directPositioned)
            if (hasMeaningfulFormFields(parsedDirect) || directChars >= 120) {
                return@withContext PipelineExtractionOutcome(
                    extractedData = parsedDirect,
                    engineUsedLabel = "ডিজিটাল পিডিএফ টেক্সট স্ট্রিম",
                    usedOcrFallback = false,
                    extractedCharCount = directChars
                )
            }
        }

        val pdfBoxAttempt = tryExtractViaPdfBox(context, pdfBytes)
        if (pdfBoxAttempt != null && pdfBoxAttempt.extractedCharCount > 0) {
            if (hasMeaningfulFormFields(pdfBoxAttempt.extractedData) || pdfBoxAttempt.extractedCharCount >= 120) {
                return@withContext pdfBoxAttempt
            }
        }

        val scaledPageBitmaps = PdfRendererHelper.renderAllPagesForOcr(
            context = context,
            pdfBytes = pdfBytes,
            scale = PdfRendererHelper.OCR_RENDER_SCALE
        )
        onStartingOcrFallback(scaledPageBitmaps.size)

        val ocrResult = OfflineTesseractOcrEngine.recognizePages(
            context = context,
            scaledPageBitmaps = scaledPageBitmaps,
            recycleBitmapsAfterUse = true,
            onPageProgress = onOcrPageProgress
        )

        if (ocrResult.nonWhitespaceCharCount == 0) {
            throw IllegalArgumentException(
                "পিডিএফ ফাইল থেকে এমবেডেড টেক্সট স্ট্রিম এবং ২.৫x স্কেল Tesseract (ben+eng) OCR—উভয় পদ্ধতিতেই কোনো পাঠযোগ্য টেক্সট পাওয়া যায়নি। অনুগ্রহ করে স্পষ্ট পিডিএফ ফাইল প্রদান করুন।"
            )
        }

        val ocrExtractedData = OcrFamilyCardParser.parseFromOcrPages(ocrResult.pagesText)
        PipelineExtractionOutcome(
            extractedData = ocrExtractedData,
            engineUsedLabel = "স্ক্যানকৃত পিডিএফ • Tesseract (ben+eng) ২.৫x OCR",
            usedOcrFallback = true,
            extractedCharCount = ocrResult.nonWhitespaceCharCount
        )
    }

    private fun tryExtractViaPdfBox(
        context: Context,
        pdfBytes: ByteArray
    ): PipelineExtractionOutcome? {
        return try {
            if (!PDFBoxResourceLoader.isReady()) {
                PDFBoxResourceLoader.init(context.applicationContext)
            }
            PDDocument.load(pdfBytes).use { document ->
                val positionedCollector = ArrayList<PdfPositionedText>()
                val stripper = object : PDFTextStripper() {
                    override fun writeString(text: String?, textPositions: MutableList<TextPosition>?) {
                        if (!text.isNullOrBlank() && !textPositions.isNullOrEmpty()) {
                            val first = textPositions.first()
                            val pageHeight = currentPage?.mediaBox?.height ?: 842f
                            val pdfX = first.xDirAdj
                            val pdfY = pageHeight - first.yDirAdj
                            val decoded = BengaliPdfUnicodeDecoder.decodeVisualBengali(text)
                            if (decoded.isNotBlank()) {
                                val isFooter = pdfY in 0.01f..16f && (
                                    decoded.contains("প্রস্তুতির সময়") ||
                                        decoded.contains("কপিরাইট")
                                    )
                                if (!isFooter) {
                                    positionedCollector.add(
                                        PdfPositionedText(
                                            pageNumber = currentPageNo,
                                            x = pdfX,
                                            y = pdfY,
                                            text = decoded
                                        )
                                    )
                                }
                            }
                        }
                        super.writeString(text, textPositions)
                    }
                }
                stripper.sortByPosition = true
                val rawPlainText = BengaliPdfUnicodeDecoder.decodeVisualBengali(stripper.getText(document).orEmpty())
                val nonWsChars = positionedCollector.sumOf { item -> item.text.count { !it.isWhitespace() } }
                    .coerceAtLeast(rawPlainText.count { !it.isWhitespace() })

                if (nonWsChars == 0) {
                    return null
                }

                val coordParsed = OfflineFamilyCardExtractor.parseFromPositionedItems(positionedCollector)
                val finalData = if (hasMeaningfulFormFields(coordParsed)) {
                    coordParsed
                } else {
                    OcrFamilyCardParser.parseFromOcrText(rawPlainText)
                }

                PipelineExtractionOutcome(
                    extractedData = finalData,
                    engineUsedLabel = "ডিজিটাল পিডিএফ টেক্সট স্ট্রিম (PDFBox)",
                    usedOcrFallback = false,
                    extractedCharCount = nonWsChars
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun hasMeaningfulFormFields(data: ExtractedFamilyCardData): Boolean {
        return data.applicant.nameBangla.isNotBlank() ||
            data.applicant.nameEnglish.isNotBlank() ||
            data.applicant.nid.isNotBlank() ||
            data.familyMembers.isNotEmpty() ||
            data.householdInfo.toiletType.isNotBlank() ||
            data.householdInfo.ownedAssetsRaw.isNotBlank()
    }
}
