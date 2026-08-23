package com.gsoft.opus.presentation.passation

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.gsoft.opus.R
import com.gsoft.opus.domain.model.Passation
import com.gsoft.opus.domain.model.PassationAttachment
import com.gsoft.opus.presentation.personnel.formatDateDisplay
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates a PDF "Fiche de Passation" using Android's built-in PdfDocument API,
 * mirroring the desktop jsPDF export (logos, Senegal flag separator, info
 * sections, instructions/incidents, attachments table, OPUS footer).
 *
 * The PDF is written to the app's cache directory and the File is returned so
 * the caller can share/open it.
 */
object PassationPdfExporter {

    private const val PAGE_WIDTH = 595   // A4 at 72dpi
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f

    fun export(context: Context, passation: Passation, attachments: List<PassationAttachment> = emptyList()): File {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = canvas(page)
        var y = drawHeader(context, canvas)
        y = drawFlagSeparator(canvas, y)
        y = drawSectionTitle(canvas, "Informations de la passation", y)
        y = drawInfoLine(canvas, "Date", formatDateDisplay(passation.datePassation), y)
        y = drawInfoLine(canvas, "Heure", passation.heureDisplay, y)

        y += 8
        y = drawSectionTitle(canvas, "Chef de poste descendant", y)
        y = drawInfoLine(canvas, "Grade", passation.chefDescendantGrade ?: "—", y)
        y = drawInfoLine(canvas, "Nom complet", passation.chefDescendantLastname ?: "—", y)

        y += 8
        y = drawSectionTitle(canvas, "Chef de poste montant", y)
        y = drawInfoLine(canvas, "Grade", passation.chefMontantGrade ?: "—", y)
        y = drawInfoLine(canvas, "Nom complet", passation.chefMontantLastname ?: "—", y)

        y += 8
        y = drawSectionTitle(canvas, "Instructions & Incidents", y)
        y = drawWrappedLabelValue(canvas, "Instructions Autorité :", passation.instructionsAutorite ?: "—", y)
        y = drawWrappedLabelValue(canvas, "Incidents survenus :", passation.incidentsSurvenus ?: "—", y)

        // Attachments table
        if (attachments.isNotEmpty()) {
            y += 6
            y = drawSectionTitle(canvas, "Pièces jointes (${attachments.size})", y)
            y = drawAttachmentsTable(canvas, attachments.map { it.title to it.originalFilename }, y)
        }

        drawFooter(context, canvas, 1, 1)
        doc.finishPage(page)
        doc.close()

        val file = File(context.cacheDir, "passation_${passation.datePassation}_${passation.id}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        return file
    }

    // ─── Drawing helpers ─────────────────────────────────────────────

    private fun canvas(page: PdfDocument.Page): Canvas = page.canvas

    private fun drawHeader(context: Context, canvas: Canvas): Float {
        val logoSize = 50f
        val logoY = 18f
        // PN logo (left)
        try {
            val pnBmp = BitmapFactory.decodeResource(context.resources, R.drawable.logo_pn)
            if (pnBmp != null) {
                val rect = RectF(MARGIN, logoY, MARGIN + logoSize, logoY + logoSize)
                canvas.drawBitmap(pnBmp, null, rect, null)
                pnBmp.recycle()
            }
        } catch (_: Exception) {}
        // CSP logo (right)
        try {
            val cspBmp = BitmapFactory.decodeResource(context.resources, R.drawable.logo_csp)
            if (cspBmp != null) {
                val rect = RectF(PAGE_WIDTH - MARGIN - logoSize, logoY, PAGE_WIDTH - MARGIN, logoY + logoSize)
                canvas.drawBitmap(cspBmp, null, rect, null)
                cspBmp.recycle()
            }
        } catch (_: Exception) {}

        // Title
        val titlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 26f
            isFakeBoldText = true
            color = Color.BLACK
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Fiche de Passation", PAGE_WIDTH / 2f, logoY + 24f, titlePaint)

        // Date
        val datePaint = Paint().apply {
            isAntiAlias = true
            textSize = 12f
            color = Color.DKGRAY
            textAlign = Paint.Align.CENTER
        }
        val dateStr = "Généré le ${SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date())}"
        canvas.drawText(dateStr, PAGE_WIDTH / 2f, logoY + 42f, datePaint)

        return logoY + logoSize + 12f
    }

    private fun drawFlagSeparator(canvas: Canvas, y: Float): Float {
        val h = 4f
        val xStart = MARGIN
        val xEnd = PAGE_WIDTH - MARGIN
        val third = (xEnd - xStart) / 3f

        // Black border
        canvas.drawRect(RectF(xStart, y, xEnd, y + h), Paint().apply { color = Color.BLACK })
        // Green
        canvas.drawRect(RectF(xStart + 0.5f, y + 0.5f, xStart + third - 0.5f, y + h - 0.5f), Paint().apply { color = Color.rgb(0, 150, 80) })
        // White
        canvas.drawRect(RectF(xStart + third + 0.5f, y + 0.5f, xStart + 2 * third - 0.5f, y + h - 0.5f), Paint().apply { color = Color.WHITE })
        // Red
        canvas.drawRect(RectF(xStart + 2 * third + 0.5f, y + 0.5f, xEnd - 0.5f, y + h - 0.5f), Paint().apply { color = Color.rgb(220, 40, 40) })

        return y + h + 16f
    }

    private fun drawSectionTitle(canvas: Canvas, title: String, y: Float): Float {
        val paint = Paint().apply {
            isAntiAlias = true
            textSize = 16f
            isFakeBoldText = true
            color = Color.BLACK
        }
        canvas.drawText(title, MARGIN, y, paint)
        return y + 14f
    }

    private fun drawInfoLine(canvas: Canvas, label: String, value: String, y: Float): Float {
        val labelPaint = Paint().apply {
            isAntiAlias = true
            textSize = 12f
            isFakeBoldText = true
            color = Color.BLACK
        }
        val valuePaint = Paint().apply {
            isAntiAlias = true
            textSize = 12f
            color = Color.DKGRAY
        }
        canvas.drawText("$label :", MARGIN, y, labelPaint)
        canvas.drawText(value, MARGIN + 120f, y, valuePaint)
        return y + 16f
    }

    private fun drawWrappedLabelValue(canvas: Canvas, label: String, value: String, y: Float): Float {
        val labelPaint = Paint().apply {
            isAntiAlias = true
            textSize = 12f
            isFakeBoldText = true
            color = Color.BLACK
        }
        canvas.drawText(label, MARGIN, y, labelPaint)

        val valuePaint = Paint().apply {
            isAntiAlias = true
            textSize = 12f
            color = Color.DKGRAY
        }
        val maxWidth = PAGE_WIDTH - 2 * MARGIN
        val lines = wrapText(value, valuePaint, maxWidth)
        var currentY = y + 14f
        for (line in lines) {
            canvas.drawText(line, MARGIN, currentY, valuePaint)
            currentY += 14f
        }
        return currentY + 4f
    }

    private fun drawAttachmentsTable(canvas: Canvas, rows: List<Pair<String, String>>, y: Float): Float {
        val colWidth = (PAGE_WIDTH - 2 * MARGIN) / 2f
        val rowH = 20f
        val headerH = 22f

        // Header
        val headerPaint = Paint().apply { color = Color.rgb(100, 100, 100) }
        canvas.drawRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + headerH), headerPaint)
        val headerTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 11f
            isFakeBoldText = true
            color = Color.WHITE
        }
        canvas.drawText("Titre", MARGIN + 6f, y + 15f, headerTextPaint)
        canvas.drawText("Fichier", MARGIN + colWidth + 6f, y + 15f, headerTextPaint)

        var currentY = y + headerH

        val cellPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
            color = Color.BLACK
        }
        val borderPaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 0.5f }

        rows.forEachIndexed { i, (title, filename) ->
            // Alternating row background
            if (i % 2 == 1) {
                canvas.drawRect(
                    RectF(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + rowH),
                    Paint().apply { color = Color.rgb(245, 245, 245) }
                )
            }
            canvas.drawText(title, MARGIN + 6f, currentY + 14f, cellPaint)
            canvas.drawText(filename, MARGIN + colWidth + 6f, currentY + 14f, cellPaint)
            // Row border
            canvas.drawLine(MARGIN, currentY + rowH, PAGE_WIDTH - MARGIN, currentY + rowH, borderPaint)
            currentY += rowH
        }

        // Column borders
        canvas.drawLine(MARGIN, y, MARGIN, currentY, borderPaint)
        canvas.drawLine(MARGIN + colWidth, y, MARGIN + colWidth, currentY, borderPaint)
        canvas.drawLine(PAGE_WIDTH - MARGIN, y, PAGE_WIDTH - MARGIN, currentY, borderPaint)
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, borderPaint)

        return currentY + 10f
    }

    private fun drawFooter(context: Context, canvas: Canvas, pageNum: Int, totalPages: Int) {
        val logoSize = 28f
        val footerTextY = PAGE_HEIGHT - 12f
        val footerLogoY = footerTextY - logoSize - 4f
        try {
            val opusBmp = BitmapFactory.decodeResource(context.resources, R.drawable.logo_opus)
            if (opusBmp != null) {
                val rect = RectF(
                    PAGE_WIDTH / 2f - logoSize / 2f,
                    footerLogoY,
                    PAGE_WIDTH / 2f + logoSize / 2f,
                    footerLogoY + logoSize
                )
                canvas.drawBitmap(opusBmp, null, rect, null)
                opusBmp.recycle()
            }
        } catch (_: Exception) {}

        val footerPaint = Paint().apply {
            isAntiAlias = true
            textSize = 8f
            color = Color.GRAY
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("OPUS — Page $pageNum/$totalPages", PAGE_WIDTH / 2f, footerTextY, footerPaint)
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return listOf("—")
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (word in words) {
            val test = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(test) <= maxWidth) {
                current = StringBuilder(test)
            } else {
                if (current.isNotEmpty()) {
                    lines.add(current.toString())
                }
                current = StringBuilder(word)
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return if (lines.isEmpty()) listOf(text) else lines
    }
}
