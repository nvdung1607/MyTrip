package com.example.mytrip.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class NoteTag(val label: String, val icon: String) {
    HOTEL("Khách sạn", "🏨"),
    FOOD("Ăn uống", "🍜"),
    ATTRACTION("Điểm tham quan", "🏛️"),
    SHOP("Mua sắm", "🛍️"),
    TRANSPORT("Di chuyển", "🚗"),
    PERSON("Gặp gỡ", "👤"),
    OTHER("Khác", "📌")
}

@Entity(
    tableName = "notes",
    foreignKeys = [ForeignKey(
        entity = Trip::class,
        parentColumns = ["id"],
        childColumns = ["tripId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("tripId")]
)
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tripId: Long,
    val dayId: Long? = null,           // gắn vào ngày cụ thể
    val timestamp: Long = System.currentTimeMillis(),

    // Bắt buộc
    val photoPath: String? = null,     // đường dẫn ảnh local
    val photoPaths: List<String> = emptyList(), // danh sách tất cả ảnh local
    val rating: Int = 0,               // 1-5 sao
    val tag: NoteTag = NoteTag.OTHER,
    val cost: Long = 0,                // Chi phí thực tế (VND đã bỏ 3 số 0)
    val paidBy: String = "",           // Tên người trả

    // Tùy chọn
    val name: String = "",             // Tên địa điểm / món ăn
    val comment: String = "",          // Nhận xét

    // GPS (auto-fill)
    val gpsLat: Double? = null,
    val gpsLng: Double? = null
)
