package com.example.mytrip.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ActivityStatus(val label: String) {
    PENDING("Chưa thực hiện"),
    DONE("Đã hoàn thành"),
    SKIPPED("Đã bỏ qua"),
    CHANGED("Đã thay đổi")
}

@Entity(
    tableName = "activities",
    foreignKeys = [ForeignKey(
        entity = Day::class,
        parentColumns = ["id"],
        childColumns = ["dayId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("dayId")]
)
data class Activity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dayId: Long,
    val orderIndex: Int = 0,

    // Thông tin kế hoạch
    val name: String,                    // Tên địa điểm / hoạt động
    val departureTime: String = "",      // HH:mm
    val arrivalTime: String = "",        // HH:mm
    val distanceKm: Double = 0.0,
    val hotelName: String = "",          // Tên khách sạn / nơi nghỉ
    val hotelPricePlanned: Long = 0,     // Giá phòng dự kiến (VND, đã bỏ 3 số 0)
    val checkInSpots: String = "",       // JSON array ["Bà Nà Hills", "Cầu Vàng"]
    val mapsLink: String = "",           // Link Google Maps
    val notes: String = "",

    // Thực tế
    val status: ActivityStatus = ActivityStatus.PENDING,
    val actualDepartureTime: String = "",
    val actualArrivalTime: String = "",
    val actualNotes: String = ""         // Ghi chú thực tế (nếu thay đổi)
)
