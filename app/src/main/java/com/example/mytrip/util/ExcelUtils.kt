package com.example.mytrip.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.mytrip.data.db.entities.*
import com.example.mytrip.util.DateUtils
import com.example.mytrip.util.MoneyUtils
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream

object ExcelUtils {

    /**
     * Export trip itinerary and expenses to Excel (.xlsx)
     * Returns Uri of the created file (FileProvider)
     */
    fun exportTripToExcel(
        context: Context,
        trip: Trip,
        days: List<Day>,
        activitiesMap: Map<Long, List<Activity>>,
        expenses: List<Expense>,
        records: List<ExpenseRecord>
    ): Uri {
        val workbook = XSSFWorkbook()

        // ── Sheet 1: Lịch trình ─────────────────────────────────────
        val itinSheet = workbook.createSheet("Lịch trình")
        itinSheet.setColumnWidth(0, 4000)
        itinSheet.setColumnWidth(1, 4000)
        itinSheet.setColumnWidth(2, 6000)
        itinSheet.setColumnWidth(3, 4000)
        itinSheet.setColumnWidth(4, 6000)
        itinSheet.setColumnWidth(5, 4000)
        itinSheet.setColumnWidth(6, 8000)

        // Header row styles
        val headerStyle = workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.TEAL.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            alignment = HorizontalAlignment.CENTER
            val font = workbook.createFont().apply {
                bold = true
                color = IndexedColors.WHITE.index
            }
            setFont(font)
        }

        val titleStyle = workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.LIGHT_CORNFLOWER_BLUE.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            val font = workbook.createFont().apply { bold = true; fontHeightInPoints = 14 }
            setFont(font)
        }

        // Title row
        var rowIdx = 0
        itinSheet.createRow(rowIdx++).apply {
            createCell(0).apply {
                setCellValue("[Ô tô] ${trip.name}")
                cellStyle = titleStyle
            }
            createCell(1).setCellValue("Từ: ${DateUtils.formatDate(trip.startDate)}")
            createCell(2).setCellValue("Đến: ${DateUtils.formatDate(trip.endDate)}")
            createCell(3).setCellValue("Số người: ${trip.numPeople}")
        }
        itinSheet.addMergedRegion(CellRangeAddress(0, 0, 0, 6))

        rowIdx++ // blank row

        // Column headers
        itinSheet.createRow(rowIdx++).apply {
            listOf("Ngày", "Thời gian", "Địa điểm", "Khoảng cách", "Khách sạn", "Giá phòng (k)", "Điểm check-in")
                .forEachIndexed { i, title ->
                    createCell(i).apply {
                        setCellValue(title)
                        cellStyle = headerStyle
                    }
                }
        }

        // Data rows
        val dayStyle = workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.LIGHT_TURQUOISE.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            val font = workbook.createFont().apply { bold = true }
            setFont(font)
        }

        for (day in days.sortedBy { it.dayNumber }) {
            // Day header
            itinSheet.createRow(rowIdx++).apply {
                createCell(0).apply {
                    setCellValue("Ngày ${day.dayNumber} - ${DateUtils.formatDate(day.date)}")
                    cellStyle = dayStyle
                }
                (1..6).forEach { createCell(it).cellStyle = dayStyle }
            }

            val activities = activitiesMap[day.id] ?: emptyList()
            for (act in activities.sortedBy { it.orderIndex }) {
                itinSheet.createRow(rowIdx++).apply {
                    createCell(0).setCellValue("")
                    createCell(1).setCellValue(
                        if (act.departureTime.isNotEmpty()) "${act.departureTime}→${act.arrivalTime}" else ""
                    )
                    createCell(2).setCellValue(act.name)
                    createCell(3).setCellValue(if (act.distanceKm > 0) "${act.distanceKm} km" else "")
                    createCell(4).setCellValue(act.hotelName)
                    createCell(5).setCellValue(
                        if (act.hotelPricePlanned > 0) MoneyUtils.vndToInput(act.hotelPricePlanned).toString() else ""
                    )
                    createCell(6).setCellValue(act.checkInSpots)
                }
            }
        }

        // ── Sheet 2: Chi phí ────────────────────────────────────────
        val expSheet = workbook.createSheet("Chi phí")
        expSheet.setColumnWidth(0, 5000)
        expSheet.setColumnWidth(1, 4000)
        expSheet.setColumnWidth(2, 4000)
        expSheet.setColumnWidth(3, 4000)

        expSheet.createRow(0).apply {
            listOf("Hạng mục", "Dự kiến (k)", "Thực tế (k)", "Chênh lệch").forEachIndexed { i, t ->
                createCell(i).apply { setCellValue(t); cellStyle = headerStyle }
            }
        }

        var eRow = 1
        for (exp in expenses) {
            val actual = records.filter { it.category == exp.category }.sumOf { it.amount }
            val diff = actual - exp.planned
            expSheet.createRow(eRow++).apply {
                createCell(0).setCellValue(exp.category.label)
                createCell(1).setCellValue(MoneyUtils.vndToInput(exp.planned).toDouble())
                createCell(2).setCellValue(MoneyUtils.vndToInput(actual).toDouble())
                createCell(3).setCellValue(MoneyUtils.vndToInput(diff).toDouble())
            }
        }

        // Total row
        val totalPlanned = expenses.sumOf { it.planned }
        val totalActual  = records.sumOf { it.amount }
        expSheet.createRow(eRow).apply {
            createCell(0).apply { setCellValue("TỔNG"); cellStyle = headerStyle }
            createCell(1).apply { setCellValue(MoneyUtils.vndToInput(totalPlanned).toDouble()); cellStyle = headerStyle }
            createCell(2).apply { setCellValue(MoneyUtils.vndToInput(totalActual).toDouble()); cellStyle = headerStyle }
            createCell(3).apply { setCellValue(MoneyUtils.vndToInput(totalActual - totalPlanned).toDouble()); cellStyle = headerStyle }
        }

        // Save and return URI
        val fileName = "MyTrip_${trip.name.replace(" ", "_")}_${DateUtils.formatDate(System.currentTimeMillis()).replace("/", "-")}.xlsx"
        val file = File(context.getExternalFilesDir("Exports"), fileName)
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { workbook.write(it) }
        workbook.close()

        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /**
     * Share the exported Excel file via Android Share Sheet
     */
    fun shareExcelFile(context: Context, uri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ kế hoạch chuyến đi"))
    }

    /**
     * Generate a simple expense report as plain text for sharing via Zalo/Messenger
     */
    fun generateTextReport(
        trip: Trip,
        expenses: List<Expense>,
        records: List<ExpenseRecord>,
        memberNames: List<String>
    ): String {
        val sb = StringBuilder()
        sb.appendLine("🗺️ BÁO CÁO CHI PHÍ: ${trip.name}")
        sb.appendLine("📅 ${DateUtils.formatDate(trip.startDate)} → ${DateUtils.formatDate(trip.endDate)}")
        sb.appendLine("👥 ${trip.numPeople} người")
        sb.appendLine("━".repeat(30))
        sb.appendLine()
        sb.appendLine("📊 NGÂN SÁCH:")
        for (exp in expenses) {
            val actual = records.filter { it.category == exp.category }.sumOf { it.amount }
            sb.appendLine("  ${exp.category.icon} ${exp.category.label}: " +
                    "DK ${MoneyUtils.formatShort(exp.planned)} | " +
                    "TT ${MoneyUtils.formatShort(actual)}")
        }
        sb.appendLine()
        val totalPlanned = expenses.sumOf { it.planned }
        val totalActual = records.sumOf { it.amount }
        sb.appendLine("💰 TỔNG DỰ KIẾN: ${MoneyUtils.formatVnd(totalPlanned)}")
        sb.appendLine("💳 TỔNG THỰC TẾ: ${MoneyUtils.formatVnd(totalActual)}")
        sb.appendLine("💵 MỖI NGƯỜI: ${MoneyUtils.formatVnd(totalActual / trip.numPeople)}")
        sb.appendLine()
        sb.appendLine("👤 AI TRẢ GÌ:")
        val paidByPerson = records.groupBy { it.paidBy }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
        val perPerson = totalActual / trip.numPeople
        for (name in memberNames) {
            val paid = paidByPerson[name] ?: 0L
            val balance = paid - perPerson
            val sign = if (balance >= 0) "✅ được hoàn" else "❗ cần trả thêm"
            sb.appendLine("  $name: đã trả ${MoneyUtils.formatShort(paid)} → $sign ${MoneyUtils.formatShort(kotlin.math.abs(balance))}")
        }
        return sb.toString()
    }
}
