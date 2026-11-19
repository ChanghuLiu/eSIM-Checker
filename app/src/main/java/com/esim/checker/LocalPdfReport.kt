package com.esim.checker

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.ByteArrayOutputStream

/** Builds a report entirely on-device. Callers decide whether to save or share it. */
object LocalPdfReport {
    fun create(context: Context, report: String): ByteArray {
        val document = PdfDocument()
        val output = ByteArrayOutputStream()
        val pageWidth = 612
        val pageHeight = 792
        val margin = 48f
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 19f
            isFakeBoldText = true
            color = android.graphics.Color.rgb(24, 48, 86)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11f
            color = android.graphics.Color.DKGRAY
        }
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 9f
            color = android.graphics.Color.GRAY
        }
        val lines = report.lines().flatMap { line -> wrap(line, bodyPaint, pageWidth - margin * 2) }
        var pageNumber = 1
        var lineIndex = 0
        try {
            while (lineIndex < lines.size || pageNumber == 1) {
                val page = document.startPage(
                    PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create(),
                )
                val canvas = page.canvas
                var y = 64f
                if (pageNumber == 1) {
                    canvas.drawText(context.getString(R.string.pdf_report_title), margin, y, titlePaint)
                    y += 34f
                } else {
                    canvas.drawText(context.getString(R.string.pdf_report_title), margin, y, titlePaint)
                    y += 28f
                }
                val bottom = pageHeight - 52f
                while (lineIndex < lines.size && y <= bottom) {
                    val line = lines[lineIndex]
                    if (line.isNotEmpty()) canvas.drawText(line, margin, y, bodyPaint)
                    y += 16f
                    lineIndex++
                }
                canvas.drawText(
                    context.getString(R.string.pdf_report_footer, pageNumber),
                    margin,
                    pageHeight - 28f,
                    footerPaint,
                )
                document.finishPage(page)
                pageNumber++
            }
            document.writeTo(output)
            return output.toByteArray()
        } finally {
            document.close()
        }
    }

    private fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return listOf("")
        val words = text.split(Regex("\\s+"))
        val result = mutableListOf<String>()
        var current = ""
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (current.isNotEmpty() && paint.measureText(candidate) > maxWidth) {
                result += current
                current = word
            } else {
                current = candidate
            }
        }
        if (current.isNotEmpty()) result += current
        return result
    }
}
