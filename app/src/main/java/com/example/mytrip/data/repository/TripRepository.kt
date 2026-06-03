package com.example.mytrip.data.repository

import com.example.mytrip.data.db.dao.*
import com.example.mytrip.data.db.entities.*
import kotlinx.coroutines.flow.Flow

class TripRepository(
    private val tripDao: TripDao,
    private val clusterDao: ClusterDao,
    private val dayDao: DayDao,
    private val activityDao: ActivityDao,
    private val noteDao: NoteDao,
    private val expenseDao: ExpenseDao
) {
    // ── Trip ──────────────────────────────────────────────────────────
    fun getAllTrips(): Flow<List<Trip>> = tripDao.getAllTrips()
    fun getTripById(id: Long): Flow<Trip?> = tripDao.getTripById(id)
    suspend fun getTripByIdOnce(id: Long): Trip? = tripDao.getTripByIdOnce(id)

    suspend fun createTrip(trip: Trip): Long {
        val tripId = tripDao.insertTrip(trip)
        // Tạo days tương ứng với số ngày
        val days = generateDays(trip.copy(id = tripId))
        dayDao.insertDays(days)
        // Tạo 6 hạng mục chi phí mặc định = 0
        val expenses = ExpenseCategory.values().map {
            Expense(tripId = tripId, category = it, planned = 0)
        }
        expenseDao.insertExpenses(expenses)
        return tripId
    }

    suspend fun updateTrip(trip: Trip) = tripDao.updateTrip(trip)
    suspend fun deleteTrip(trip: Trip) = tripDao.deleteTrip(trip)
    suspend fun updateTripStatus(id: Long, status: TripStatus) = tripDao.updateStatus(id, status)

    private fun generateDays(trip: Trip): List<Day> {
        val days = mutableListOf<Day>()
        var current = trip.startDate
        var index = 1
        while (current <= trip.endDate) {
            days.add(Day(tripId = trip.id, dayNumber = index, date = current))
            current += 86_400_000L // +1 day
            index++
        }
        return days
    }

    // ── Cluster ───────────────────────────────────────────────────────
    fun getClusters(tripId: Long): Flow<List<Cluster>> = clusterDao.getClustersForTrip(tripId)
    suspend fun insertCluster(cluster: Cluster): Long = clusterDao.insertCluster(cluster)
    suspend fun updateCluster(cluster: Cluster) = clusterDao.updateCluster(cluster)
    suspend fun deleteCluster(cluster: Cluster) = clusterDao.deleteCluster(cluster)

    // ── Day ───────────────────────────────────────────────────────────
    fun getDays(tripId: Long): Flow<List<Day>> = dayDao.getDaysForTrip(tripId)
    fun getDaysForCluster(clusterId: Long): Flow<List<Day>> = dayDao.getDaysForCluster(clusterId)
    suspend fun getDayById(id: Long): Day? = dayDao.getDayById(id)
    suspend fun updateDay(day: Day) = dayDao.updateDay(day)
    suspend fun deleteDay(day: Day) = dayDao.deleteDay(day)

    suspend fun getTodayDay(tripId: Long): Day? {
        val now = System.currentTimeMillis()
        val startOfDay = now - (now % 86_400_000L)
        val endOfDay = startOfDay + 86_399_999L
        return dayDao.getDayByDate(tripId, startOfDay, endOfDay)
    }

    // ── Activity ──────────────────────────────────────────────────────
    fun getActivities(dayId: Long): Flow<List<Activity>> = activityDao.getActivitiesForDay(dayId)
    suspend fun getActivitiesOnce(dayId: Long): List<Activity> = activityDao.getActivitiesForDayOnce(dayId)
    suspend fun insertActivity(activity: Activity): Long = activityDao.insertActivity(activity)
    suspend fun updateActivity(activity: Activity) = activityDao.updateActivity(activity)
    suspend fun deleteActivity(activity: Activity) = activityDao.deleteActivity(activity)
    suspend fun updateActivityStatus(id: Long, status: ActivityStatus) = activityDao.updateStatus(id, status)

    // ── Note ──────────────────────────────────────────────────────────
    fun getNotes(tripId: Long): Flow<List<Note>> = noteDao.getNotesForTrip(tripId)
    fun getNotesForDay(dayId: Long): Flow<List<Note>> = noteDao.getNotesForDay(dayId)
    suspend fun insertNote(note: Note): Long = noteDao.insertNote(note)
    suspend fun updateNote(note: Note) = noteDao.updateNote(note)
    suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)

    // ── Expense ───────────────────────────────────────────────────────
    fun getExpenses(tripId: Long): Flow<List<Expense>> = expenseDao.getExpensesForTrip(tripId)
    suspend fun updateExpense(expense: Expense) = expenseDao.updateExpense(expense)

    fun getExpenseRecords(tripId: Long): Flow<List<ExpenseRecord>> = expenseDao.getRecordsForTrip(tripId)
    suspend fun getExpenseRecordsOnce(tripId: Long): List<ExpenseRecord> = expenseDao.getRecordsForTripOnce(tripId)
    suspend fun insertExpenseRecord(record: ExpenseRecord): Long = expenseDao.insertRecord(record)
    suspend fun updateExpenseRecord(record: ExpenseRecord) = expenseDao.updateRecord(record)
    suspend fun deleteExpenseRecord(record: ExpenseRecord) = expenseDao.deleteRecord(record)

    fun getTotalPlanned(tripId: Long): Flow<Long?> = expenseDao.getTotalPlanned(tripId)
    fun getTotalActual(tripId: Long): Flow<Long?> = expenseDao.getTotalActual(tripId)
}
