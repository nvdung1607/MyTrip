package com.example.mytrip.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.mytrip.data.db.entities.Activity
import com.example.mytrip.data.db.entities.Day
import com.example.mytrip.data.db.entities.Trip
import java.io.File
import java.io.FileOutputStream

object ShareUtils {

    private const val PAGE_WIDTH = 595   // A4 width in points (72dpi)
    private const val PAGE_HEIGHT = 842  // A4 height in points
    private const val MARGIN = 48f
    private const val LINE_HEIGHT = 22f

    /**
     * Build a PDF file for the trip itinerary and return the content URI for sharing.
     */
    fun buildItineraryPdf(
        context: Context,
        trip: Trip,
        days: List<Day>,
        activitiesMap: Map<Long, List<Activity>>
    ): android.net.Uri? {
        val document = PdfDocument()
        var pageIndex = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var y = MARGIN

        val paintTitle = Paint().apply {
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            color = Color.parseColor("#1565C0")
            isAntiAlias = true
        }
        val paintSubtitle = Paint().apply {
            textSize = 13f
            typeface = Typeface.DEFAULT
            color = Color.parseColor("#455A64")
            isAntiAlias = true
        }
        val paintDayHeader = Paint().apply {
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            color = Color.parseColor("#0277BD")
            isAntiAlias = true
        }
        val paintActivity = Paint().apply {
            textSize = 12f
            typeface = Typeface.DEFAULT
            color = Color.parseColor("#212121")
            isAntiAlias = true
        }
        val paintActivitySub = Paint().apply {
            textSize = 11f
            typeface = Typeface.DEFAULT
            color = Color.parseColor("#757575")
            isAntiAlias = true
        }
        val paintDivider = Paint().apply {
            color = Color.parseColor("#E0E0E0")
            strokeWidth = 1f
        }

        fun ensureSpace(needed: Float) {
            if (y + needed > PAGE_HEIGHT - MARGIN) {
                document.finishPage(page)
                pageIndex++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = MARGIN
            }
        }

        // ── Title block ──────────────────────────────────────────────────
        ensureSpace(80f)
        canvas.drawText("🧳 ${trip.name}", MARGIN, y, paintTitle)
        y += LINE_HEIGHT + 6f
        val dateRange = "${DateUtils.formatDate(trip.startDate)} → ${DateUtils.formatDate(trip.endDate)}"
        val numDays = DateUtils.countDays(trip.startDate, trip.endDate)
        canvas.drawText("$dateRange  |  $numDays ngày  |  ${trip.numPeople} người", MARGIN, y, paintSubtitle)
        y += LINE_HEIGHT + 4f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paintDivider)
        y += 12f

        // ── Days ─────────────────────────────────────────────────────────
        for (day in days.sortedBy { it.dayNumber }) {
            ensureSpace(40f)
            val dayLabel = "Ngày ${day.dayNumber}  —  ${DateUtils.formatDate(day.date)}"
            val dayTitle = if (day.title.isNotBlank()) "  ${day.title}" else ""
            canvas.drawText("$dayLabel$dayTitle", MARGIN, y, paintDayHeader)
            y += LINE_HEIGHT

            val activities = activitiesMap[day.id]?.sortedBy { it.orderIndex } ?: emptyList()
            if (activities.isEmpty()) {
                ensureSpace(LINE_HEIGHT)
                canvas.drawText("  (Chưa có hoạt động)", MARGIN + 16f, y, paintActivitySub)
                y += LINE_HEIGHT
            } else {
                for (act in activities) {
                    ensureSpace(LINE_HEIGHT + 14f)
                    val timeStr = buildString {
                        if (act.departureTime.isNotBlank()) append("${act.departureTime}")
                        if (act.arrivalTime.isNotBlank()) append(" → ${act.arrivalTime}")
                    }
                    val bullet = "  ${act.activityType.icon}  ${act.name}"
                    canvas.drawText(bullet, MARGIN + 16f, y, paintActivity)
                    y += 14f
                    if (timeStr.isNotBlank()) {
                        canvas.drawText("       ⏰ $timeStr", MARGIN + 16f, y, paintActivitySub)
                        y += 14f
                    }
                    if (act.notes.isNotBlank()) {
                        val noteTrimmed = if (act.notes.length > 80) act.notes.take(80) + "..." else act.notes
                        canvas.drawText("       📝 $noteTrimmed", MARGIN + 16f, y, paintActivitySub)
                        y += 14f
                    }
                }
            }

            ensureSpace(6f)
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paintDivider)
            y += 10f
        }

        document.finishPage(page)

        // ── Save to cache ─────────────────────────────────────────────────
        return try {
            val cacheDir = File(context.cacheDir, "share").also { it.mkdirs() }
            val safeName = trip.name.replace(Regex("[^\\w\\s-]"), "").trim().replace(" ", "_")
            val file = File(cacheDir, "LichTrinh_${safeName}.pdf")
            FileOutputStream(file).use { document.writeTo(it) }
            document.close()
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            document.close()
            null
        }
    }

    /**
     * Launch Android share sheet for the given PDF URI.
     */
    fun sharePdf(context: Context, pdfUri: android.net.Uri, tripName: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            putExtra(Intent.EXTRA_SUBJECT, "Lịch trình: $tripName")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Chia sẻ lịch trình qua..."))
    }
}
