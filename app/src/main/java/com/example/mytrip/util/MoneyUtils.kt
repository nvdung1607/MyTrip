package com.example.mytrip.util

import java.text.NumberFormat
import java.util.Locale

object MoneyUtils {
    private val vndFormat = NumberFormat.getInstance(Locale("vi", "VN"))

    /**
     * Chuyển từ "đơn vị nhập" (đã bỏ 3 số 0) sang VND thực.
     * VD: 500 → 500_000
     */
    fun inputToVnd(input: Long): Long = input * 1_000L

    /**
     * Chuyển từ VND thực về "đơn vị nhập" để hiển thị trong field.
     * VD: 500_000 → 500
     */
    fun vndToInput(amount: Long): Long = amount / 1_000L

    /**
     * Format VND đẹp: 500_000 → "500.000 ₫"
     */
    fun formatVnd(amount: Long): String = "${vndFormat.format(amount)} ₫"

    /**
     * Format nhỏ gọn: 500_000 → "500k", 1_500_000 → "1.5M"
     */
    fun formatShort(amount: Long): String = when {
        amount >= 1_000_000L -> {
            val m = amount / 1_000_000.0
            if (m == m.toLong().toDouble()) "${m.toLong()}M" else "${String.format("%.1f", m)}M"
        }
        amount >= 1_000L -> "${amount / 1_000}k"
        else -> "${amount} ₫"
    }

    /**
     * Parse chuỗi nhập (có thể có dấu . hoặc ,) về Long (đơn vị k)
     */
    fun parseInput(text: String): Long {
        val clean = text.replace(".", "").replace(",", "").trim()
        return clean.toLongOrNull() ?: 0L
    }

    /** Nút shortcut amounts (đơn vị k) */
    val SHORTCUTS = listOf(0L, 50L, 100L, 200L, 300L, 500L, 1_000L)

    /**
     * Tính toán chia đầu người và số dư mỗi người.
     * returns: Map<tên người, số tiền cần hoàn / cần trả thêm>
     * Dương = được hoàn lại, Âm = cần trả thêm
     */
    fun splitExpenses(
        records: List<Pair<String, Long>>, // (người trả, số tiền VND)
        numPeople: Int,
        memberNames: List<String>
    ): Map<String, Long> {
        val totalActual = records.sumOf { it.second }
        val perPerson = totalActual / numPeople

        // Tổng mỗi người đã trả
        val paid = mutableMapOf<String, Long>()
        memberNames.forEach { paid[it] = 0L }
        records.forEach { (name, amount) ->
            paid[name] = (paid[name] ?: 0L) + amount
        }

        // Số dư = đã trả - phần phải đóng
        return paid.mapValues { (_, totalPaid) -> totalPaid - perPerson }
    }
}
