package com.example.mytrip.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ExpenseCategory(val label: String, val icon: String) {
    HOTEL("Tiền phòng", "🏨"),
    FOOD("Ăn uống", "🍜"),
    TRANSPORT("Di chuyển", "⛽"),
    TICKET("Vé tham quan", "🎟️"),
    GIFT("Quà cáp / Mua sắm", "🛍️"),
    MISC("Phát sinh", "💸"),
    ADVANCE("Ứng tiền", "💳")
}

@Entity(
    tableName = "expenses",
    foreignKeys = [ForeignKey(
        entity = Trip::class,
        parentColumns = ["id"],
        childColumns = ["tripId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("tripId")]
)
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tripId: Long,
    val category: ExpenseCategory,
    val planned: Long = 0,             // Dự kiến (VND)
    val description: String = ""       // Ghi chú thêm
)

// Bảng theo dõi chi tiêu thực tế (từng khoản, ai trả)
@Entity(
    tableName = "expense_records",
    foreignKeys = [ForeignKey(
        entity = Trip::class,
        parentColumns = ["id"],
        childColumns = ["tripId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("tripId")]
)
data class ExpenseRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tripId: Long,
    val dayId: Long? = null,
    val category: ExpenseCategory,
    val amount: Long,                  // Số tiền (VND)
    val paidBy: String,                // Tên người trả
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val noteId: Long? = null,
    val advancedTo: String? = null
)
