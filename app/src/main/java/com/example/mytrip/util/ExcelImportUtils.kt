package com.example.mytrip.util

import android.content.Context
import android.net.Uri
import com.example.mytrip.data.db.entities.*
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.text.SimpleDateFormat
import java.util.Locale

object ExcelImportUtils {

    private val DATE_FMT = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val FULL_DATE_FMT = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    data class ParsedData(
        val trip: Trip,
        val days: List<Day>,
        val activities: List<Activity>,
        val expenses: List<Expense>,
        val records: List<ExpenseRecord>,
        val notes: List<Note>
    )

    sealed class ImportResult {
        data class Success(val data: ParsedData) : ImportResult()
        data class Error(val message: String) : ImportResult()
    }

    fun parseFromUri(context: Context, uri: Uri): ImportResult {
        return try {
            val stream = context.contentResolver.openInputStream(uri)
                ?: return ImportResult.Error("Không thể đọc file")

            val wb = WorkbookFactory.create(stream) as XSSFWorkbook
            stream.close()

            // 1. Parse Trip Metadata from "Lịch trình dự kiến"
            val plannedSheet = wb.getSheet("Lịch trình dự kiến")
                ?: return ImportResult.Error("Không tìm thấy sheet 'Lịch trình dự kiến'")

            val row0 = plannedSheet.getRow(0)
            val titleStr = row0?.getCell(0)?.stringCellValue ?: ""
            // Expected format: "[icon] [name] – Lịch trình dự kiến"
            val tripName = titleStr.substringAfter(" ").substringBefore(" – ").trim()
            val tripType = TripType.values().find { titleStr.startsWith(it.icon) } ?: TripType.OTHER
            
            val startDateStr = row0?.getCell(1)?.stringCellValue?.replace("Từ: ", "")?.trim() ?: ""
            val endDateStr = row0?.getCell(2)?.stringCellValue?.replace("Đến: ", "")?.trim() ?: ""
            val startDateMs = try { DATE_FMT.parse(startDateStr)?.time ?: System.currentTimeMillis() } catch (e: Exception) { System.currentTimeMillis() }
            val endDateMs = try { DATE_FMT.parse(endDateStr)?.time ?: System.currentTimeMillis() } catch (e: Exception) { System.currentTimeMillis() }
            val numPeopleStr = row0?.getCell(3)?.stringCellValue?.replace("Số người: ", "")?.trim() ?: "1"
            val numPeople = numPeopleStr.toIntOrNull() ?: 1

            // Parse Member Names from "Chi phí" sheet
            val expenseSheet = wb.getSheet("Chi phí")
            val memberNames = mutableListOf<String>()
            if (expenseSheet != null) {
                var foundSettlement = false
                for (i in 0..expenseSheet.lastRowNum) {
                    val row = expenseSheet.getRow(i) ?: continue
                    val cell0 = row.getCell(0)?.stringCellValue ?: ""
                    if (cell0 == "Tên") {
                        foundSettlement = true
                        continue
                    }
                    if (foundSettlement) {
                        if (cell0.isBlank() || cell0.startsWith("C.")) break
                        memberNames.add(cell0)
                    }
                }
            }
            if (memberNames.isEmpty()) memberNames.add("Tôi")

            val trip = Trip(
                name = tripName.ifBlank { "Chuyến đi mới" },
                type = tripType,
                startDate = startDateMs,
                endDate = endDateMs,
                numPeople = numPeople,
                memberNames = "[\"${memberNames.joinToString("\",\"")}\"]",
                themeColor = com.example.mytrip.ui.theme.TripThemeColors.getRandomColor()
            )

            // 2. Parse Days & Planned Activities
            val days = mutableListOf<Day>()
            val activities = mutableListOf<Activity>()
            var currentDayId = 0L
            var orderIdx = 0

            for (i in 2..plannedSheet.lastRowNum) {
                val row = plannedSheet.getRow(i) ?: continue
                val c0 = row.getCell(0)?.stringCellValue ?: ""
                if (c0.startsWith("Ngày")) {
                    // "Ngày X – dd/MM/yyyy"
                    val parts = c0.split(" – ")
                    val dayNum = parts[0].replace("Ngày ", "").trim().toIntOrNull() ?: 1
                    val dateMs = try { DATE_FMT.parse(parts.getOrNull(1)?.trim() ?: "")?.time ?: startDateMs } catch (e: Exception) { startDateMs }
                    
                    currentDayId-- // Negative ID to link during insert
                    days.add(Day(id = currentDayId, tripId = 0L, dayNumber = dayNum, date = dateMs))
                    orderIdx = 0
                } else if (c0.isBlank()) {
                    val c1 = row.getCell(1)?.stringCellValue ?: ""
                    if (c1.isBlank()) continue
                    
                    val actType = ActivityType.values().find { c1.contains(it.label) } ?: ActivityType.ACTIVITY
                    val timeStr = row.getCell(2)?.stringCellValue ?: ""
                    val depTime = timeStr.substringBefore("→").trim()
                    val arrTime = timeStr.substringAfter("→").trim()
                    val actName = row.getCell(3)?.stringCellValue ?: ""
                    val distStr = row.getCell(4)?.stringCellValue ?: ""
                    val dist = distStr.replace(" km", "").trim().toDoubleOrNull() ?: 0.0
                    val hotel = row.getCell(5)?.stringCellValue ?: ""
                    val price = try { row.getCell(6)?.numericCellValue?.toLong() ?: 0L } catch (e: Exception) { 0L }
                    val spotsStr = row.getCell(7)?.stringCellValue ?: ""
                    val spotsJson = if (spotsStr.isNotBlank()) "[\"${spotsStr.replace(", ", "\",\"")}\"]" else "[]"
                    val maps = row.getCell(8)?.stringCellValue ?: ""
                    val notes = row.getCell(9)?.stringCellValue ?: ""

                    activities.add(
                        Activity(
                            id = 0L,
                            dayId = currentDayId,
                            orderIndex = orderIdx++,
                            activityType = actType,
                            name = actName,
                            departureTime = depTime,
                            arrivalTime = if (arrTime == depTime) "" else arrTime,
                            distanceKm = dist,
                            hotelName = hotel,
                            hotelPricePlanned = price,
                            checkInSpots = spotsJson,
                            mapsLink = maps,
                            notes = notes
                        )
                    )
                }
            }

            // 3. Parse Actual Activities from "Lịch trình thực tế"
            val actualSheet = wb.getSheet("Lịch trình thực tế")
            if (actualSheet != null) {
                var currentDayNumber = 1
                for (i in 2..actualSheet.lastRowNum) {
                    val row = actualSheet.getRow(i) ?: continue
                    val c0 = row.getCell(0)?.stringCellValue ?: ""
                    if (c0.startsWith("Ngày")) {
                        currentDayNumber = c0.replace("Ngày ", "").substringBefore(" –").trim().toIntOrNull() ?: 1
                    } else if (c0.isBlank()) {
                        val actName = row.getCell(4)?.stringCellValue ?: ""
                        if (actName.isBlank()) continue
                        
                        val matchedDayId = days.find { it.dayNumber == currentDayNumber }?.id
                        val matchedAct = activities.find { it.dayId == matchedDayId && it.name == actName }
                        if (matchedAct != null) {
                            val actDep = row.getCell(2)?.stringCellValue ?: ""
                            val actArr = row.getCell(3)?.stringCellValue ?: ""
                            val statusStr = row.getCell(5)?.stringCellValue ?: ""
                            val status = ActivityStatus.values().find { it.label == statusStr } ?: ActivityStatus.PENDING
                            val actualNotes = row.getCell(6)?.stringCellValue ?: ""
                            
                            val updatedAct = matchedAct.copy(
                                actualDepartureTime = if (actDep != matchedAct.departureTime) actDep else "",
                                actualArrivalTime = if (actArr != matchedAct.arrivalTime) actArr else "",
                                status = status,
                                actualNotes = if (actualNotes != matchedAct.notes) actualNotes else ""
                            )
                            val idx = activities.indexOf(matchedAct)
                            activities[idx] = updatedAct
                        }
                    }
                }
            }

            // 4. Parse Expenses & Records from "Chi phí"
            val expenses = mutableListOf<Expense>()
            val records = mutableListOf<ExpenseRecord>()
            if (expenseSheet != null) {
                var parsingCategory = false
                var parsingRecords = false
                for (i in 0..expenseSheet.lastRowNum) {
                    val row = expenseSheet.getRow(i) ?: continue
                    val c0 = row.getCell(0)?.stringCellValue ?: ""
                    
                    if (c0.startsWith("A. BẢNG CHI PHÍ")) {
                        parsingCategory = true; continue
                    } else if (c0.startsWith("B. CHIA")) {
                        parsingCategory = false; continue
                    } else if (c0.startsWith("C. CHI TIẾT")) {
                        parsingRecords = true; continue
                    }

                    if (parsingCategory && c0 != "Hạng mục" && c0 != "TỔNG" && c0.isNotBlank()) {
                        val catIconLabel = c0.trim()
                        val cat = ExpenseCategory.values().find { catIconLabel.contains(it.label) } ?: ExpenseCategory.MISC
                        val plannedStr = row.getCell(1)?.stringCellValue?.replace(".", "")?.replace("₫", "")?.trim() ?: "0"
                        val planned = plannedStr.toLongOrNull() ?: 0L
                        val desc = row.getCell(4)?.stringCellValue ?: ""
                        
                        if (planned > 0 || desc.isNotBlank()) {
                            expenses.add(Expense(id = 0L, tripId = 0L, category = cat, planned = planned, description = desc))
                        }
                    }

                    if (parsingRecords && c0 != "Thời gian" && c0.isNotBlank()) {
                        val timeMs = try { FULL_DATE_FMT.parse(c0)?.time ?: System.currentTimeMillis() } catch(e: Exception) { System.currentTimeMillis() }
                        val c1 = row.getCell(1)?.stringCellValue ?: ""
                        val cat = ExpenseCategory.values().find { c1.contains(it.label) } ?: ExpenseCategory.MISC
                        val desc = row.getCell(2)?.stringCellValue ?: ""
                        val paidBy = row.getCell(3)?.stringCellValue ?: ""
                        val amtStr = row.getCell(4)?.stringCellValue?.replace(".", "")?.replace("₫", "")?.trim() ?: "0"
                        val amt = amtStr.toLongOrNull() ?: 0L
                        
                        records.add(ExpenseRecord(id = 0L, tripId = 0L, category = cat, amount = amt, paidBy = paidBy, description = desc, timestamp = timeMs))
                    }
                }
            }

            // 5. Parse Notes
            val notesList = mutableListOf<Note>()
            val noteSheet = wb.getSheet("Nhật ký")
            if (noteSheet != null) {
                var currentDayNumber = 1
                for (i in 2..noteSheet.lastRowNum) {
                    val row = noteSheet.getRow(i) ?: continue
                    val c0 = row.getCell(0)?.stringCellValue ?: ""
                    if (c0.startsWith("Ngày")) {
                        currentDayNumber = c0.replace("Ngày ", "").trim().toIntOrNull() ?: 1
                    }
                    val matchedDayId = days.find { it.dayNumber == currentDayNumber }?.id

                    val timeStr = row.getCell(1)?.stringCellValue ?: ""
                    if (timeStr == "Thời gian" || timeStr.isBlank()) continue
                    
                    val timeMs = try { FULL_DATE_FMT.parse(timeStr)?.time ?: System.currentTimeMillis() } catch(e: Exception) { System.currentTimeMillis() }
                    val c2 = row.getCell(2)?.stringCellValue ?: ""
                    val tag = NoteTag.values().find { c2.contains(it.label) } ?: NoteTag.OTHER
                    val name = row.getCell(3)?.stringCellValue ?: ""
                    val comment = row.getCell(4)?.stringCellValue ?: ""
                    val ratingStr = row.getCell(5)?.stringCellValue ?: ""
                    val rating = ratingStr.count { it == '★' }
                    val costStr = row.getCell(6)?.stringCellValue?.replace(".", "")?.replace("₫", "")?.trim() ?: "0"
                    val cost = costStr.toLongOrNull() ?: 0L
                    val paidBy = row.getCell(7)?.stringCellValue ?: ""

                    notesList.add(Note(
                        id = 0L, tripId = 0L, dayId = matchedDayId, photoPath = null, photoPaths = emptyList(),
                        rating = rating, tag = tag, cost = cost, paidBy = paidBy, name = name, comment = comment,
                        gpsLat = null, gpsLng = null, timestamp = timeMs
                    ))
                }
            }

            wb.close()
            return ImportResult.Success(ParsedData(trip, days, activities, expenses, records, notesList))

        } catch (e: Exception) {
            e.printStackTrace()
            return ImportResult.Error("Lỗi khi đọc file: ${e.message}")
        }
    }
}
