package com.example.mytrip.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TripType(val label: String, val icon: String) {
    CAR("Ô tô tự lái", "🚗"),
    MOTORBIKE("Phượt xe máy", "🏍️"),
    PUBLIC("Phương tiện công cộng", "✈️"),
    TREKKING("Treking / Đi bộ", "🥾"),
    CAMPING("Cắm trại", "⛺"),
    OTHER("Khác", "🗺️")
}

enum class TripStatus(val label: String) {
    PLANNING("Sắp đi"),
    ONGOING("Đang đi"),
    DONE("Đã kết thúc")
}

@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val type: TripType = TripType.CAR,
    val status: TripStatus = TripStatus.PLANNING,
    val startDate: Long,   // epoch millis
    val endDate: Long,     // epoch millis
    val coverImagePath: String? = null,
    val numPeople: Int = 1,
    val memberNames: String = "", // JSON array of names, e.g. ["Dũng","An","Bình"]
    val useClusters: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
