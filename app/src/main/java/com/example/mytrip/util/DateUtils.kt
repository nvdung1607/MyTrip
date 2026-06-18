package com.example.mytrip.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN"))
    private val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale("vi", "VN"))
    private val timeFormat = SimpleDateFormat("HH:mm", Locale("vi", "VN"))
    private val fullFormat = SimpleDateFormat("EEEE, dd/MM/yyyy", Locale("vi", "VN"))

    fun formatDate(millis: Long): String {
        if (millis == 0L) return "Chưa xác định"
        return dateFormat.format(Date(millis))
    }
    
    fun formatDayOfWeek(millis: Long): String {
        if (millis == 0L) return "---"
        return dayOfWeekFormat.format(Date(millis)).replaceFirstChar { it.uppercase() }
    }
    
    fun formatFull(millis: Long): String {
        if (millis == 0L) return "Chưa xác định"
        return fullFormat.format(Date(millis)).replaceFirstChar { it.uppercase() }
    }

    fun todayMillis(): Long {
        return startOfDay(System.currentTimeMillis())
    }

    fun startOfDay(millis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun isToday(millis: Long): Boolean {
        val today = todayMillis()
        return millis in today until today + 86_400_000L
    }

    fun isYesterday(millis: Long): Boolean {
        val yesterday = todayMillis() - 86_400_000L
        return millis in yesterday until yesterday + 86_400_000L
    }

    fun isTomorrow(millis: Long): Boolean {
        val tomorrow = todayMillis() + 86_400_000L
        return millis in tomorrow until tomorrow + 86_400_000L
    }

    fun countDays(startMillis: Long, endMillis: Long): Int {
        return ((endMillis - startMillis) / 86_400_000L).toInt() + 1
    }

    fun millisFromDateParts(year: Int, month: Int, day: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(year, month, day, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getCalendarFromMillis(millis: Long): Calendar {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        return cal
    }
}
