package com.example.mytrip.widget

import android.content.Context
import com.example.mytrip.data.db.AppDatabase
import com.example.mytrip.data.db.entities.Activity
import com.example.mytrip.data.db.entities.TripStatus
import java.util.Calendar

/**
 * Lightweight data-access layer for the widget.
 * Uses suspend functions only — no Flow — to keep widget updates simple.
 * Accesses [AppDatabase] directly to avoid depending on the Application class
 * lifecycle (widget processes may start independently).
 */
class WidgetRepository(context: Context) {

    private val db = AppDatabase.getInstance(context.applicationContext)
    private val tripDao      = db.tripDao()
    private val dayDao       = db.dayDao()
    private val activityDao  = db.activityDao()
    private val expenseDao   = db.expenseDao()

    /**
     * Returns the current trip to show on the widget.
     * The widget intentionally shows only the trip that is actively ongoing.
     */
    suspend fun getActiveTrip(): com.example.mytrip.data.db.entities.Trip? {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val startOfToday = cal.timeInMillis

        // 1. Auto-finish ONGOING trips if they are fully past
        val ongoingTrips = tripDao.getTripsByStatusOnce(TripStatus.ONGOING)
        for (trip in ongoingTrips) {
             val tripEndOfDay = trip.endDate + 86_399_999L
             if (now > tripEndOfDay) {
                 tripDao.updateStatus(trip.id, TripStatus.DONE)
             }
        }

        // 2. Check if there is currently an ONGOING trip
        var active = tripDao.getTripsByStatusOnce(TripStatus.ONGOING).firstOrNull()

        // 3. If no ONGOING trip, check if any PLANNING trip should be started
        if (active == null) {
            val planningTrips = tripDao.getTripsByStatusOnce(TripStatus.PLANNING)
            for (trip in planningTrips) {
                val tripEndOfDay = trip.endDate + 86_399_999L
                if (startOfToday >= trip.startDate && now <= tripEndOfDay) {
                    tripDao.updateStatusEnforcingSingleOngoing(trip.id, TripStatus.ONGOING)
                    active = tripDao.getTripsByStatusOnce(TripStatus.ONGOING).firstOrNull()
                    break // Only start the first one we find
                } else if (now > tripEndOfDay) {
                    tripDao.updateStatus(trip.id, TripStatus.DONE)
                }
            }
        }

        // 4. If STILL no ONGOING trip, return the closest upcoming PLANNING trip
        if (active == null) {
            active = tripDao.getTripsByStatusOnce(TripStatus.PLANNING)
                .filter { it.startDate >= startOfToday } // only future/upcoming
                .minByOrNull { it.startDate }
        }

        return active
    }

    /** Today's [Day] for [tripId], or null if trip hasn't started or has ended. */
    suspend fun getTodayDay(tripId: Long): com.example.mytrip.data.db.entities.Day? {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis
        val endOfDay   = startOfDay + 86_399_999L
        return dayDao.getDayByDate(tripId, startOfDay, endOfDay)
    }

    /** All activities for a given day, ordered by orderIndex. */
    suspend fun getTodayActivities(dayId: Long): List<Activity> =
        activityDao.getActivitiesForDayOnce(dayId)

    /** Total actual spend for a trip (0 if none recorded). */
    suspend fun getTotalActual(tripId: Long): Long =
        expenseDao.getTotalActualOnce(tripId) ?: 0L

    /** Total planned budget for a trip (0 if none recorded). */
    suspend fun getTotalPlanned(tripId: Long): Long =
        expenseDao.getTotalPlannedOnce(tripId) ?: 0L

    /**
     * Builds a complete [MyTripWidgetState] snapshot.
     * Call this from a coroutine inside [MyTripWidget.provideGlance].
     */
    suspend fun buildWidgetState(): MyTripWidgetState {
        val trip = getActiveTrip()
            ?: return MyTripWidgetState(hasActiveTrip = false)

        // ── Day progress ──────────────────────────────────────────────
        val totalDays = ((trip.endDate - trip.startDate) / 86_400_000L + 1).toInt()
        val todayDay  = if (trip.status == TripStatus.ONGOING) getTodayDay(trip.id) else null
        val currentDay = todayDay?.dayNumber ?: 0

        // Days until trip starts
        val now = System.currentTimeMillis()
        val daysUntil = maxOf(0L, (trip.startDate - now) / 86_400_000L)

        // ── Today's activities ────────────────────────────────────────
        val activities = if (todayDay != null) getTodayActivities(todayDay.id) else emptyList()

        // Next pending activity (first PENDING, prioritise ones with a time set)
        val nextActivity = activities
            .filter { it.status == com.example.mytrip.data.db.entities.ActivityStatus.PENDING }
            .firstOrNull()

        // All activities for large widget (LazyColumn)
        val widgetActivities = activities.map { it.toWidgetItem() }

        // ── Expense summary ───────────────────────────────────────────────
        val totalActual  = getTotalActual(trip.id)
        val totalPlanned = getTotalPlanned(trip.id)
        val todayActual  = if (todayDay != null)
            expenseDao.getTodayActualOnce(trip.id, todayDay.id) ?: 0L
        else 0L

        return MyTripWidgetState(
            hasActiveTrip    = true,
            tripId           = trip.id,
            tripName         = trip.name,
            tripTypeIcon     = trip.type.icon,
            tripStatus       = trip.status,
            tripThemeColor   = trip.themeColor,
            currentDay       = currentDay,
            totalDays        = totalDays,
            todayTitle       = todayDay?.title ?: "",
            todayDayId       = todayDay?.id ?: -1L,
            daysUntilTrip    = daysUntil,
            numPeople        = trip.numPeople.coerceAtLeast(1),
            nextActivityName = nextActivity?.name ?: "",
            nextActivityTime = nextActivity?.let {
                it.departureTime.ifBlank { it.arrivalTime }
            } ?: "",
            nextActivityIcon = nextActivity?.activityType?.icon ?: "📍",
            hasNextActivity  = nextActivity != null,
            totalActual      = totalActual,
            totalPlanned     = totalPlanned,
            todayActual      = todayActual,
            todayActivities  = widgetActivities
        )
    }
}
