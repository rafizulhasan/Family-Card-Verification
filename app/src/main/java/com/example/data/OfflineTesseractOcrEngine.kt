package com.example.data

import android.content.Context
import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class OcrRecognitionResult(
    val pagesText: List<String>,
    val nonWhitespaceCharCount: Int
)

object OfflineTesseractOcrEngine {

    private const val TESSDATA_DIR_NAME = "tessdata"
    private const val LANGUAGES = "ben+eng"

    suspend fun recognizePages(
        context: Context,
        scaledPageBitmaps: List<Bitmap>,
        recycleBitmapsAfterUse: Boolean = true,
        onPageProgress: (currentPage: Int, totalPages: Int) -> Unit = { _, _ -> }
    ): OcrRecognitionResult = withContext(Dispatchers.Default) {
        if (scaledPageBitmaps.isEmpty()) {
            return@withContext OcrRecognitionResult(emptyList(), 0)
        }

        val baseDir = prepareTessDataDirectory(context)
        val hasTessModels = File(baseDir, "$TESSDATA_DIR_NAME/ben.traineddata").exists() ||
            File(baseDir, "$TESSDATA_DIR_NAME/eng.traineddata").exists()

        if (!hasTessModels) {
            if (recycleBitmapsAfterUse) {
                scaledPageBitmaps.forEach { bmp ->
                    if (!bmp.isRecycled) bmp.recycle()
                }
            }
            return@withContext OcrRecognitionResult(emptyList(), 0)
        }

        val availableLang = when {
            File(baseDir, "$TESSDATA_DIR_NAME/ben.traineddata").exists() &&
                File(baseDir, "$TESSDATA_DIR_NAME/eng.traineddata").exists() -> LANGUAGES
            File(baseDir, "$TESSDATA_DIR_NAME/ben.traineddata").exists() -> "ben"
            else -> "eng"
        }

        val tessApi = TessBaseAPI()
        val pagesOutput = mutableListOf<String>()
        try {
            val initialized = tessApi.init(baseDir.absolutePath, availableLang)
            if (!initialized) {
                return@withContext OcrRecognitionResult(emptyList(), 0)
            }

            val total = scaledPageBitmaps.size
            for ((index, bitmap) in scaledPageBitmaps.withIndex()) {
                onPageProgress(index + 1, total)
                tessApi.setImage(bitmap)
                val recognized = tessApi.utF8Text.orEmpty()
                pagesOutput.add(BengaliPdfUnicodeDecoder.decodeVisualBengali(recognized))
                tessApi.clear()
                if (recycleBitmapsAfterUse && !bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
        } catch (_: Throwable) {
            // Gracefully return any pages recognized so far
        } finally {
            try {
                tessApi.recycle()
            } catch (_: Throwable) {
            }
            if (recycleBitmapsAfterUse) {
                scaledPageBitmaps.forEach { bmp ->
                    if (!bmp.isRecycled) bmp.recycle()
                }
            }
        }

        val charCount = pagesOutput.sumOf { page -> page.count { !it.isWhitespace() } }
        OcrRecognitionResult(
            pagesText = pagesOutput,
            nonWhitespaceCharCount = charCount
        )
    }

    private fun prepareTessDataDirectory(context: Context): File {
        val rootDir = File(context.filesDir, "tesseract")
        val tessDataDir = File(rootDir, TESSDATA_DIR_NAME)
        if (!tessDataDir.exists()) {
            tessDataDir.mkdirs()
        }

        for (langFile in listOf("ben.traineddata", "eng.traineddata")) {
            val targetFile = File(tessDataDir, langFile)
            if (!targetFile.exists() || targetFile.length() == 0L) {
                try {
                    context.assets.open("$TESSDATA_DIR_NAME/$langFile").use { input ->
                        FileOutputStream(targetFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (_: Exception) {
                    // Asset optional if not bundled in APK
                }
            }
        }
        return rootDir
    }
}
