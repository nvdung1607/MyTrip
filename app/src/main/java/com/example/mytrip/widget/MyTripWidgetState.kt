package com.example.mytrip.widget

import com.example.mytrip.data.db.entities.Activity
import com.example.mytrip.data.db.entities.ActivityStatus
import com.example.mytrip.data.db.entities.TripStatus

/**
 * Immutable state snapshot passed to the Glance widget composables.
 * All data is pre-fetched by [MyTripWidget] before rendering.
 */
data class MyTripWidgetState(
    val hasActiveTrip: Boolean = false,
    // Trip info
    val tripId: Long = 0L,
    val tripName: String = "",
    val tripTypeIcon: String = "🗺️",
    val tripStatus: TripStatus = TripStatus.PLANNING,
    // Day progress
    val currentDay: Int = 0,
    val totalDays: Int = 0,
    val todayTitle: String = "",
    val daysUntilTrip: Long = 0L,  // used when status = PLANNING
    // Next activity
    val nextActivityName: String = "",
    val nextActivityTime: String = "",
    val nextActivityIcon: String = "📍",
    val hasNextActivity: Boolean = false,
    // Expense summary
    val totalActual: Long = 0L,    // VND
    val totalPlanned: Long = 0L,   // VND
    // Today's activity list (for Large widget, max 3)
    val todayActivities: List<ActivityItem> = emptyList()
)

data class ActivityItem(
    val name: String,
    val time: String,      // HH:mm
    val icon: String,
    val isDone: Boolean
)

/** Convert a Room Activity entity to the lightweight widget ActivityItem. */
fun Activity.toWidgetItem(): ActivityItem = ActivityItem(
    name = name,
    time = when {
        departureTime.isNotBlank() -> departureTime
        arrivalTime.isNotBlank()   -> arrivalTime
        else                       -> ""
    },
    icon = activityType.icon,
    isDone = status == ActivityStatus.DONE || status == ActivityStatus.SKIPPED
)
