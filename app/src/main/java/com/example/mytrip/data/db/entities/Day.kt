package com.example.mytrip.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "days",
    foreignKeys = [
        ForeignKey(
            entity = Trip::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Cluster::class,
            parentColumns = ["id"],
            childColumns = ["clusterId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("tripId"), Index("clusterId")]
)
data class Day(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tripId: Long,
    val clusterId: Long? = null,   // null nếu không dùng cluster
    val dayNumber: Int,            // Ngày 1, 2, 3...
    val date: Long,                // epoch millis
    val title: String = "",        // VD: "Hà Nội → Đà Nẵng"
    val notes: String = "",        // ghi chú chung của ngày
    val isExpanded: Boolean = true
)
