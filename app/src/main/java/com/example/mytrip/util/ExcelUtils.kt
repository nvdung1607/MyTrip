package com.example.mytrip.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.mytrip.data.db.entities.*
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFClientAnchor
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore

object ExcelUtils {

    // ─── Style helpers ────────────────────────────────────────────────────────

    private fun headerStyle(wb: XSSFWorkbook, bgColor: IndexedColors = IndexedColors.TEAL): XSSFCellStyle =
        wb.createCellStyle().apply {
            fillForegroundColor = bgColor.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            alignment = HorizontalAlignment.CENTER
            val f = wb.createFont().apply { bold = true; color = IndexedColors.WHITE.index }
            setFont(f)
            borderBottom = BorderStyle.THIN
            borderTop = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }

    private fun titleStyle(wb: XSSFWorkbook): XSSFCellStyle =
        wb.createCellStyle().apply {
            fillForegroundColor = IndexedColors.LIGHT_CORNFLOWER_BLUE.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            val f = wb.createFont().apply { bold = true; fontHeightInPoints = 14 }
            setFont(f)
        }

    private fun dayHeaderStyle(wb: XSSFWorkbook): XSSFCellStyle =
        wb.createCellStyle().apply {
            fillForegroundColor = IndexedColors.LIGHT_TURQUOISE.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            val f = wb.createFont().apply { bold = true }
            setFont(f)
        }

    private fun totalStyle(wb: XSSFWorkbook): XSSFCellStyle =
        wb.createCellStyle().apply {
            fillForegroundColor = IndexedColors.DARK_TEAL.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            alignment = HorizontalAlignment.CENTER
            val f = wb.createFont().apply { bold = true; color = IndexedColors.WHITE.index }
            setFont(f)
        }

    private fun subtitleStyle(wb: XSSFWorkbook): XSSFCellStyle =
        wb.createCellStyle().apply {
            fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            val f = wb.createFont().apply { bold = true; fontHeightInPoints = 11 }
            setFont(f)
        }

    private fun goodStyle(wb: XSSFWorkbook): XSSFCellStyle =
        wb.createCellStyle().apply {
            fillForegroundColor = IndexedColors.LIGHT_GREEN.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            val f = wb.createFont().apply { bold = true; color = IndexedColors.DARK_GREEN.index }
            setFont(f)
        }

    private fun badStyle(wb: XSSFWorkbook): XSSFCellStyle =
        wb.createCellStyle().apply {
            fillForegroundColor = IndexedColors.ROSE.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            val f = wb.createFont().apply { bold = true; color = IndexedColors.RED.index }
            setFont(f)
        }

    private fun wrapStyle(wb: XSSFWorkbook): XSSFCellStyle =
        wb.createCellStyle().apply { wrapText = true }

    private fun Row.cell(col: Int, value: String, style: XSSFCellStyle? = null) {
        val c = createCell(col)
        c.setCellValue(value)
        style?.let { c.cellStyle = it }
    }

    private fun Row.cell(col: Int, value: Double, style: XSSFCellStyle? = null) {
        val c = createCell(col)
        c.setCellValue(value)
        style?.let { c.cellStyle = it }
    }

    // ─── Main export function ─────────────────────────────────────────────────

    fun exportTripToExcel(
        context: Context,
        trip: Trip,
        days: List<Day>,
        activitiesMap: Map<Long, List<Activity>>,
        expenses: List<Expense>,
        records: List<ExpenseRecord>,
        notes: List<Note>,
        memberNames: List<String>
    ): Uri {
        val wb = XSSFWorkbook()

        val hdr = headerStyle(wb)
        val title = titleStyle(wb)
        val dayHdr = dayHeaderStyle(wb)
        val totalSt = totalStyle(wb)
        val subSt = subtitleStyle(wb)
        val goodSt = goodStyle(wb)
        val badSt = badStyle(wb)
        val wrapSt = wrapStyle(wb)

        buildPlannedSheet(wb, trip, days, activitiesMap, hdr, title, dayHdr)
        buildActualSheet(wb, trip, days, activitiesMap, hdr, title, dayHdr)
        buildExpenseSheet(wb, trip, expenses, records, memberNames, hdr, title, totalSt, subSt, goodSt, badSt)
        buildNoteSheet(wb, trip, days, notes, hdr, title, dayHdr, wrapSt, context)

        val fileName = "MyTrip_${trip.name.replace(" ", "_")}_${DateUtils.formatDate(System.currentTimeMillis()).replace("/", "-")}.xlsx"
        val file = File(context.getExternalFilesDir("Exports"), fileName)
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { wb.write(it) }

        // Save to public Downloads directory
        val success = saveToDownloads(context, wb, fileName)
        if (success) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                android.widget.Toast.makeText(
                    context,
                    "Đã tải file Excel về thư mục Downloads!",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }

        wb.close()

        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    // ─── Sheet 1: Lịch trình dự kiến ─────────────────────────────────────────

    private fun buildPlannedSheet(
        wb: XSSFWorkbook, trip: Trip, days: List<Day>,
        activitiesMap: Map<Long, List<Activity>>,
        hdr: XSSFCellStyle, title: XSSFCellStyle, dayHdr: XSSFCellStyle
    ) {
        val sheet = wb.createSheet("Lịch trình dự kiến")
        val cols = listOf(3800, 3000, 3000, 6500, 3500, 5000, 3500, 5000, 8000, 6000)
        cols.forEachIndexed { i, w -> sheet.setColumnWidth(i, w) }

        var r = 0
        sheet.createRow(r++).apply {
            cell(0, "${trip.type.icon} ${trip.name} – Lịch trình dự kiến", title)
            cell(1, "Từ: ${DateUtils.formatDate(trip.startDate)}")
            cell(2, "Đến: ${DateUtils.formatDate(trip.endDate)}")
            cell(3, "Số người: ${trip.numPeople}")
        }
        sheet.addMergedRegion(CellRangeAddress(0, 0, 0, 9))
        r++ // blank

        sheet.createRow(r++).apply {
            listOf("Ngày","Loại","Giờ xuất","Địa điểm / Hoạt động","Khoảng cách","Khách sạn","Giá phòng (k)","Điểm check-in","Link Maps","Ghi chú")
                .forEachIndexed { i, t -> cell(i, t, hdr) }
        }

        for (day in days.sortedBy { it.dayNumber }) {
            sheet.createRow(r++).apply {
                cell(0, "Ngày ${day.dayNumber} – ${DateUtils.formatDate(day.date)}", dayHdr)
                (1..9).forEach { createCell(it).cellStyle = dayHdr }
            }
            val acts = activitiesMap[day.id] ?: emptyList()
            for (act in acts.sortedBy { it.orderIndex }) {
                sheet.createRow(r++).apply {
                    cell(0, "")
                    cell(1, "${act.activityType.icon} ${act.activityType.label}")
                    cell(2, if (act.departureTime.isNotEmpty()) "${act.departureTime}→${act.arrivalTime}" else "")
                    cell(3, act.name)
                    cell(4, if (act.distanceKm > 0) "${act.distanceKm} km" else "")
                    cell(5, act.hotelName)
                    cell(6, if (act.hotelPricePlanned > 0) act.hotelPricePlanned.toDouble() else Double.NaN)
                    // checkInSpots JSON → plain text
                    val spots = try {
                        val arr = org.json.JSONArray(act.checkInSpots)
                        (0 until arr.length()).joinToString(", ") { arr.getString(it) }
                    } catch (_: Exception) { "" }
                    cell(7, spots)
                    cell(8, act.mapsLink)
                    cell(9, act.notes)
                }
            }
        }
    }

    // ─── Sheet 2: Lịch trình thực tế ─────────────────────────────────────────

    private fun buildActualSheet(
        wb: XSSFWorkbook, trip: Trip, days: List<Day>,
        activitiesMap: Map<Long, List<Activity>>,
        hdr: XSSFCellStyle, title: XSSFCellStyle, dayHdr: XSSFCellStyle
    ) {
        val sheet = wb.createSheet("Lịch trình thực tế")
        listOf(3800, 3000, 3000, 3000, 6500, 4000, 7000).forEachIndexed { i, w ->
            sheet.setColumnWidth(i, w)
        }

        var r = 0
        sheet.createRow(r++).apply {
            cell(0, "${trip.type.icon} ${trip.name} – Lịch trình thực tế", title)
        }
        sheet.addMergedRegion(CellRangeAddress(0, 0, 0, 6))
        r++

        sheet.createRow(r++).apply {
            listOf("Ngày","Loại","Giờ xuất TT","Giờ đến TT","Địa điểm","Trạng thái","Ghi chú thực tế")
                .forEachIndexed { i, t -> cell(i, t, hdr) }
        }

        for (day in days.sortedBy { it.dayNumber }) {
            val acts = (activitiesMap[day.id] ?: emptyList())
                .sortedBy { it.orderIndex }
            if (acts.isEmpty()) continue

            sheet.createRow(r++).apply {
                cell(0, "Ngày ${day.dayNumber} – ${DateUtils.formatDate(day.date)}", dayHdr)
                (1..6).forEach { createCell(it).cellStyle = dayHdr }
            }
            for (act in acts) {
                sheet.createRow(r++).apply {
                    cell(0, "")
                    cell(1, "${act.activityType.icon} ${act.activityType.label}")
                    cell(2, act.actualDepartureTime.ifBlank { act.departureTime })
                    cell(3, act.actualArrivalTime.ifBlank { act.arrivalTime })
                    cell(4, act.name)
                    cell(5, act.status.label)
                    cell(6, act.actualNotes.ifBlank { act.notes })
                }
            }
        }

        if (r == 3) { // no data
            sheet.createRow(r).cell(0, "Chưa có hoạt động nào được thực hiện")
        }
    }

    // ─── Sheet 3: Chi phí ────────────────────────────────────────────────────

    private fun buildExpenseSheet(
        wb: XSSFWorkbook, trip: Trip,
        expenses: List<Expense>, records: List<ExpenseRecord>,
        memberNames: List<String>,
        hdr: XSSFCellStyle, title: XSSFCellStyle, totalSt: XSSFCellStyle,
        subSt: XSSFCellStyle, goodSt: XSSFCellStyle, badSt: XSSFCellStyle
    ) {
        val sheet = wb.createSheet("Chi phí")
        listOf(5500, 4500, 4500, 4500, 6000).forEachIndexed { i, w -> sheet.setColumnWidth(i, w) }

        var r = 0
        sheet.createRow(r++).apply {
            cell(0, "💰 ${trip.name} – Tổng kết Chi phí", title)
        }
        sheet.addMergedRegion(CellRangeAddress(0, 0, 0, 4))
        r++

        // ── Phần A: Bảng hạng mục ────────────────────────────────────────────
        sheet.createRow(r++).apply { cell(0, "A. BẢNG CHI PHÍ THEO HẠNG MỤC", subSt); (1..4).forEach { createCell(it).cellStyle = subSt } }
        sheet.addMergedRegion(CellRangeAddress(r - 1, r - 1, 0, 4))

        sheet.createRow(r++).apply {
            listOf("Hạng mục", "Dự kiến (VND)", "Thực tế (VND)", "Chênh lệch", "Ghi chú")
                .forEachIndexed { i, t -> cell(i, t, hdr) }
        }

        var totalPlanned = 0L
        var totalActual = 0L

        for (exp in expenses) {
            val actual = records.filter { it.category == exp.category }.sumOf { it.amount }
            val diff = actual - exp.planned
            totalPlanned += exp.planned
            totalActual += actual
            sheet.createRow(r++).apply {
                cell(0, "${exp.category.icon} ${exp.category.label}")
                cell(1, MoneyUtils.formatVnd(exp.planned))
                cell(2, MoneyUtils.formatVnd(actual))
                val diffStyle = if (diff > 0) badSt else if (diff < 0) goodSt else null
                cell(3, if (diff >= 0) "+${MoneyUtils.formatShort(diff)}" else MoneyUtils.formatShort(diff), diffStyle)
                cell(4, exp.description)
            }
        }

        val diffTotal = totalActual - totalPlanned
        sheet.createRow(r++).apply {
            cell(0, "TỔNG", totalSt)
            cell(1, MoneyUtils.formatVnd(totalPlanned), totalSt)
            cell(2, MoneyUtils.formatVnd(totalActual), totalSt)
            cell(3, if (diffTotal >= 0) "+${MoneyUtils.formatShort(diffTotal)}" else MoneyUtils.formatShort(diffTotal), totalSt)
            cell(4, "", totalSt)
        }

        r++ // blank row

        // ── Phần B: Chia chi phí ─────────────────────────────────────────────
        sheet.createRow(r++).apply {
            cell(0, "B. CHIA & QUYẾT TOÁN CHI PHÍ", subSt); (1..4).forEach { createCell(it).cellStyle = subSt }
        }
        sheet.addMergedRegion(CellRangeAddress(r - 1, r - 1, 0, 4))

        val numPeople = trip.numPeople.coerceAtLeast(1)
        val perPerson = totalActual / numPeople

        sheet.createRow(r++).apply {
            cell(0, "Tổng chi phí thực tế:")
            cell(1, MoneyUtils.formatVnd(totalActual))
        }
        sheet.createRow(r++).apply {
            cell(0, "Số người:")
            cell(1, numPeople.toString())
        }
        sheet.createRow(r++).apply {
            cell(0, "Mỗi người phải trả:")
            cell(1, MoneyUtils.formatVnd(perPerson))
        }
        r++

        sheet.createRow(r++).apply {
            listOf("Tên", "Đã trả", "Phần bằng nhau", "Số dư", "Kết quả quyết toán")
                .forEachIndexed { i, t -> cell(i, t, hdr) }
        }

        val paidByPerson = records.groupBy { it.paidBy }.mapValues { (_, list) -> list.sumOf { it.amount } }

        for (name in memberNames) {
            val paid = paidByPerson[name] ?: 0L
            val balance = paid - perPerson
            val result = if (balance >= 0) "✅ Được hoàn ${MoneyUtils.formatShort(balance)}"
                         else "❗ Cần trả ${MoneyUtils.formatShort(-balance)}"
            val resultStyle = if (balance >= 0) goodSt else badSt
            sheet.createRow(r++).apply {
                cell(0, name)
                cell(1, MoneyUtils.formatVnd(paid))
                cell(2, MoneyUtils.formatVnd(perPerson))
                cell(3, if (balance >= 0) "+${MoneyUtils.formatShort(balance)}" else MoneyUtils.formatShort(balance),
                    if (balance >= 0) goodSt else badSt)
                cell(4, result, resultStyle)
            }
        }

        r++ // blank row

        // ── Phần C: Chi tiết giao dịch ───────────────────────────────────────
        sheet.createRow(r++).apply {
            cell(0, "C. CHI TIẾT GIAO DỊCH", subSt); (1..4).forEach { createCell(it).cellStyle = subSt }
        }
        sheet.addMergedRegion(CellRangeAddress(r - 1, r - 1, 0, 4))

        sheet.createRow(r++).apply {
            listOf("Thời gian", "Hạng mục", "Mô tả", "Người trả", "Số tiền")
                .forEachIndexed { i, t -> cell(i, t, hdr) }
        }

        for (rec in records.sortedBy { it.timestamp }) {
            sheet.createRow(r++).apply {
                cell(0, DateUtils.formatFull(rec.timestamp))
                cell(1, "${rec.category.icon} ${rec.category.label}")
                cell(2, rec.description)
                val paidByText = if (rec.category == ExpenseCategory.ADVANCE && !rec.advancedTo.isNullOrBlank()) {
                    "${rec.paidBy} ➡️ ${rec.advancedTo}"
                } else {
                    rec.paidBy
                }
                cell(3, paidByText)
                cell(4, MoneyUtils.formatVnd(rec.amount))
            }
        }
    }

    // ─── Sheet 4: Nhật ký (Notes) ─────────────────────────────────────────────

    private fun buildNoteSheet(
        wb: XSSFWorkbook, trip: Trip, days: List<Day>, notes: List<Note>,
        hdr: XSSFCellStyle, title: XSSFCellStyle, dayHdr: XSSFCellStyle, wrapSt: XSSFCellStyle,
        context: Context
    ) {
        val sheet = wb.createSheet("Nhật ký")
        // Columns: Ngày, Thời gian, Loại, Tên, Nhận xét, Đánh giá, Chi phí, Người trả, Ảnh
        listOf(3800, 3500, 3000, 5000, 8000, 2500, 3500, 3000, 4000)
            .forEachIndexed { i, w -> sheet.setColumnWidth(i, w) }

        // Drawing for images
        val drawing = sheet.createDrawingPatriarch()

        var r = 0
        sheet.createRow(r++).apply {
            cell(0, "📖 ${trip.name} – Nhật ký & Ghi chú", title)
        }
        sheet.addMergedRegion(CellRangeAddress(0, 0, 0, 8))
        r++

        sheet.createRow(r++).apply {
            listOf("Ngày","Thời gian","Loại","Tên / Địa điểm","Nhận xét","Sao","Chi phí","Người trả","Ảnh")
                .forEachIndexed { i, t -> cell(i, t, hdr) }
        }

        val dayMap = days.associateBy { it.id }
        val sortedNotes = notes.sortedBy { it.timestamp }

        for (note in sortedNotes) {
            val dayLabel = note.dayId?.let { dayMap[it]?.let { d -> "Ngày ${d.dayNumber}" } } ?: ""
            val rowHeight = 80 // points for image row

            val row = sheet.createRow(r)
            row.heightInPoints = if (note.photoPath != null) rowHeight.toFloat() else 15f

            row.cell(0, dayLabel)
            row.cell(1, DateUtils.formatFull(note.timestamp))
            row.cell(2, "${note.tag.icon} ${note.tag.label}")
            row.cell(3, note.name)
            row.cell(4, note.comment, wrapSt)
            row.cell(5, "★".repeat(note.rating))
            row.cell(6, if (note.cost > 0) MoneyUtils.formatVnd(note.cost) else "")
            row.cell(7, note.paidBy)

            // Embed thumbnail image if available
            if (note.photoPath != null) {
                try {
                    val imgBytes = loadThumbnail(context, note.photoPath, 120, 80)
                    if (imgBytes != null) {
                        val picIdx = wb.addPicture(imgBytes, Workbook.PICTURE_TYPE_JPEG)
                        val anchor = XSSFClientAnchor(0, 0, 0, 0, 8, r, 9, r + 1)
                        anchor.anchorType = ClientAnchor.AnchorType.MOVE_AND_RESIZE
                        drawing.createPicture(anchor, picIdx)
                        row.cell(8, "") // cell exists but picture floats above
                    } else {
                        row.cell(8, File(note.photoPath).name)
                    }
                } catch (_: Exception) {
                    row.cell(8, File(note.photoPath).name)
                }
            } else {
                row.cell(8, "")
            }

            r++
        }

        if (sortedNotes.isEmpty()) {
            sheet.createRow(r).cell(0, "Chưa có ghi chú nào")
        }
    }

    // ─── Thumbnail helper ─────────────────────────────────────────────────────

    private fun loadThumbnail(context: Context, path: String, maxW: Int, maxH: Int): ByteArray? {
        return try {
            val uri = Uri.parse(path)
            val isUri = uri.scheme == "content" || uri.scheme == "file" || path.startsWith("android.resource://")

            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            if (isUri) {
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            } else {
                BitmapFactory.decodeFile(path, opts)
            }

            val scaleW = (opts.outWidth / maxW).coerceAtLeast(1)
            val scaleH = (opts.outHeight / maxH).coerceAtLeast(1)
            val scale = maxOf(scaleW, scaleH)
            
            val opts2 = BitmapFactory.Options().apply { inSampleSize = scale }
            val bmp = if (isUri) {
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts2) }
            } else {
                BitmapFactory.decodeFile(path, opts2)
            } ?: return null

            val scaled = Bitmap.createScaledBitmap(bmp, maxW, maxH, true)
            val bos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 80, bos)
            bos.toByteArray()
        } catch (_: Exception) { null }
    }

    // ─── Save to public Downloads folder using MediaStore ─────────────────────

    private fun saveToDownloads(context: Context, wb: XSSFWorkbook, fileName: String): Boolean {
        try {
            val resolver = context.contentResolver
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/")
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues) ?: return false
                resolver.openOutputStream(uri)?.use { wb.write(it) }
                return true
            } else {
                val targetDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(targetDir, fileName)
                FileOutputStream(file).use { wb.write(it) }
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    // ─── Share Excel ──────────────────────────────────────────────────────────

    fun shareExcelFile(context: Context, uri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ kế hoạch chuyến đi"))
    }
}
