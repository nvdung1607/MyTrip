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

    data class Transfer(val from: String, val to: String, val amount: Long)

    /**
     * Tính toán chia đầu người và số dư mỗi người, kèm theo hướng dẫn chuyển tiền chi tiết.
     * returns: Pair<Map<tên người, số dư>, List<Transfer>>
     * Số dư dương = được hoàn lại, Âm = cần trả thêm
     */
    fun splitExpenses(
        records: List<com.example.mytrip.data.db.entities.ExpenseRecord>,
        numPeople: Int,
        memberNames: List<String>
    ): Pair<Map<String, Long>, List<Transfer>> {
        val normalRecords = records.filter { it.category != com.example.mytrip.data.db.entities.ExpenseCategory.ADVANCE }
        val advanceRecords = records.filter { it.category == com.example.mytrip.data.db.entities.ExpenseCategory.ADVANCE }

        val totalActual = normalRecords.sumOf { it.amount }
        val perPerson = if (numPeople > 0) totalActual / numPeople else 0L

        // Khởi tạo balance = 0
        val balance = mutableMapOf<String, Long>()
        memberNames.forEach { balance[it] = 0L }

        // Chi phí chung: Người trả được cộng (+)
        normalRecords.forEach { rec ->
            balance[rec.paidBy] = (balance[rec.paidBy] ?: 0L) + rec.amount
        }

        // Trừ đi phần mỗi người phải đóng (-)
        memberNames.forEach {
            balance[it] = (balance[it] ?: 0L) - perPerson
        }

        // Xử lý ứng tiền: Người ứng (paidBy) được cộng, người nhận (advancedTo) bị trừ
        advanceRecords.forEach { rec ->
            val from = rec.paidBy
            val to = rec.advancedTo
            if (to != null && to.isNotBlank()) {
                balance[from] = (balance[from] ?: 0L) + rec.amount
                balance[to] = (balance[to] ?: 0L) - rec.amount
            }
        }

        // Tính toán các giao dịch cần thiết để thanh toán
        val debtors = balance.filterValues { it < 0 }.mapValues { -it.value }.toMutableMap()
        val creditors = balance.filterValues { it > 0 }.toMutableMap()
        val transfers = mutableListOf<Transfer>()

        val debtorKeys = debtors.keys.toMutableList()
        val creditorKeys = creditors.keys.toMutableList()

        var i = 0
        var j = 0

        while (i < debtorKeys.size && j < creditorKeys.size) {
            val debtor = debtorKeys[i]
            val creditor = creditorKeys[j]

            val debt = debtors[debtor] ?: 0L
            val credit = creditors[creditor] ?: 0L

            if (debt == 0L) { i++; continue }
            if (credit == 0L) { j++; continue }

            val amount = minOf(debt, credit)
            transfers.add(Transfer(from = debtor, to = creditor, amount = amount))

            debtors[debtor] = debt - amount
            creditors[creditor] = credit - amount

            if (debtors[debtor] == 0L) i++
            if (creditors[creditor] == 0L) j++
        }

        return Pair(balance, transfers)
    }
}
