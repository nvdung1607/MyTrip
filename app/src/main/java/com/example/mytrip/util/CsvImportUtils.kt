package com.example.mytrip.util

import android.content.Context
import android.net.Uri
import com.example.mytrip.data.db.entities.TripType
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Parses a CSV file (matching trip_template.csv format) into a structured TripImportData.
 */
object CsvImportUtils {

    private val DATE_FMT = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    data class TripImportData(
        val name: String,
        val type: TripType,
        val startDateMs: Long,
        val endDateMs: Long,
        val numPeople: Int,
        val memberNames: List<String>,
        val description: String,
        val days: List<DayImportData>
    )

    data class DayImportData(
        val dayNumber: Int,
        val clusterName: String,
        val title: String,
        val distanceKm: Double,
        val city: String,
        val checkInSpots: List<String>,
        val notes: String
    )

    sealed class ImportResult {
        data class Success(val data: TripImportData) : ImportResult()
        data class Error(val message: String) : ImportResult()
    }

    fun parseFromUri(context: Context, uri: Uri): ImportResult {
        return try {
            val lines = context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readLines()
            } ?: return ImportResult.Error("Không thể đọc file")

            parseLines(lines)
        } catch (e: Exception) {
            ImportResult.Error("Lỗi đọc file: ${e.message}")
        }
    }

    fun parseFromAssets(context: Context, fileName: String): ImportResult {
        return try {
            val lines = context.assets.open(fileName).use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readLines()
            }
            parseLines(lines)
        } catch (e: Exception) {
            ImportResult.Error("Lỗi đọc file mẫu: ${e.message}")
        }
    }

    private fun parseLines(lines: List<String>): ImportResult {
        val dataLines = lines.filter { !it.trimStart().startsWith("#") && it.isNotBlank() }

        var section = ""
        var tripData: TripImportData? = null
        val dayList = mutableListOf<DayImportData>()
        var skipHeader = false

        for (line in dataLines) {
            val trimmed = line.trim()
            when {
                trimmed == "[TRIP]" -> { section = "TRIP"; skipHeader = true }
                trimmed == "[DAYS]" -> { section = "DAYS"; skipHeader = true }
                skipHeader -> { skipHeader = false } // skip column headers
                section == "TRIP" -> {
                    val cols = parseCsvLine(trimmed)
                    if (cols.size >= 6) {
                        tripData = TripImportData(
                            name = cols[0].trim(),
                            type = parseTripType(cols[1].trim()),
                            startDateMs = parseDate(cols[2].trim()),
                            endDateMs = parseDate(cols[3].trim()),
                            numPeople = cols[4].trim().toIntOrNull() ?: 1,
                            memberNames = cols[5].trim().split(",").map { it.trim() }.filter { it.isNotBlank() },
                            description = cols.getOrNull(6)?.trim() ?: "",
                            days = emptyList() // will be filled below
                        )
                    }
                }
                section == "DAYS" -> {
                    val cols = parseCsvLine(trimmed)
                    if (cols.size >= 4) {
                        val spotsCsv = cols.getOrNull(5) ?: ""
                        val spots = spotsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        dayList.add(
                            DayImportData(
                                dayNumber = cols[0].trim().toIntOrNull() ?: 0,
                                clusterName = cols[1].trim(),
                                title = cols[2].trim(),
                                distanceKm = cols[3].trim().toDoubleOrNull() ?: 0.0,
                                city = cols.getOrNull(4)?.trim() ?: "",
                                checkInSpots = spots,
                                notes = cols.getOrNull(6)?.trim() ?: ""
                            )
                        )
                    }
                }
            }
        }

        val trip = tripData ?: return ImportResult.Error("Không tìm thấy phần [TRIP] trong file")
        return ImportResult.Success(trip.copy(days = dayList))
    }

    /**
     * Parse a single CSV line respecting quoted fields (fields with commas inside double-quotes).
     */
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false

        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(ch)
            }
        }
        result.add(current.toString())
        return result
    }

    private fun parseTripType(s: String): TripType = when (s.uppercase()) {
        "OTO", "Ô TÔ", "CAR" -> TripType.CAR
        "MOTORBIKE", "PHƯỢT", "XE MÁY" -> TripType.MOTORBIKE
        "CONG_CONG", "CÔNG CỘNG", "PUBLIC" -> TripType.PUBLIC
        "TREKKING", "TREKING" -> TripType.TREKKING
        "CAM_TRAI", "CẮM TRẠI", "CAMPING" -> TripType.CAMPING
        else -> TripType.OTHER
    }

    private fun parseDate(s: String): Long {
        return try {
            DATE_FMT.parse(s)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }
}
