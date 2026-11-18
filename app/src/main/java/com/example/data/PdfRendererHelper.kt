package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PdfRendererHelper {

    const val PREVIEW_RENDER_SCALE = 1.6f
    const val OCR_RENDER_SCALE = 2.5f

    suspend fun renderPreviewPages(
        context: Context,
        pdfBytes: ByteArray,
        maxPages: Int = 6
    ): List<Bitmap> = renderPagesInternal(
        context = context,
        pdfBytes = pdfBytes,
        scale = PREVIEW_RENDER_SCALE,
        maxPages = maxPages
    )

    suspend fun renderAllPagesForOcr(
        context: Context,
        pdfBytes: ByteArray,
        scale: Float = OCR_RENDER_SCALE
    ): List<Bitmap> = renderPagesInternal(
        context = context,
        pdfBytes = pdfBytes,
        scale = scale,
        maxPages = 12
    )

    private suspend fun renderPagesInternal(
        context: Context,
        pdfBytes: ByteArray,
        scale: Float,
        maxPages: Int
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        if (pdfBytes.isEmpty()) return@withContext emptyList()
        val tempFile = File.createTempFile("family_card_", ".pdf", context.cacheDir)
        val bitmaps = mutableListOf<Bitmap>()
        try {
            FileOutputStream(tempFile).use { it.write(pdfBytes) }
            ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val pageCount = renderer.pageCount.coerceAtMost(maxPages)
                    for (i in 0 until pageCount) {
                        renderer.openPage(i).use { page ->
                            val width = (page.width * scale).roundToInt().coerceAtLeast(1)
                            val height = (page.height * scale).roundToInt().coerceAtLeast(1)
                            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(bitmap)
                            canvas.drawColor(Color.WHITE)
                            page.render(
                                bitmap,
                                null,
                                null,
                                PdfRenderer.Page.RENDER_MODE_FOR_PRINT
                            )
                            bitmaps.add(bitmap)
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Return any rendered pages or empty list if synthetic PDF stream
        } finally {
            tempFile.delete()
        }
        bitmaps
    }
}
