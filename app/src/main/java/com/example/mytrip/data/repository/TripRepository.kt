package com.example.mytrip.data.repository

import android.content.Context
import com.example.mytrip.data.db.dao.*
import com.example.mytrip.data.db.entities.*
import com.example.mytrip.util.CsvImportUtils
import com.example.mytrip.util.DateUtils
import com.example.mytrip.util.ExcelImportUtils
import com.example.mytrip.widget.MyTripWidgetUpdater
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray


class TripRepository(
    private val context: Context,
    private val tripDao: TripDao,
    private val clusterDao: ClusterDao,
    private val dayDao: DayDao,
    private val activityDao: ActivityDao,
    private val noteDao: NoteDao,
    private val expenseDao: ExpenseDao
) {

    /** Triggers an immediate widget refresh. Call after any data mutation. */
    private suspend fun refreshWidget() {
        MyTripWidgetUpdater.update(context)
    }
    // ── Trip ──────────────────────────────────────────────────────────
    fun getAllTrips(): Flow<List<Trip>> = tripDao.getAllTrips()
    fun getTripById(id: Long): Flow<Trip?> = tripDao.getTripById(id)
    suspend fun getTripByIdOnce(id: Long): Trip? = tripDao.getTripByIdOnce(id)

    suspend fun createTrip(trip: Trip): Long {
        val tripId = tripDao.insertTripEnforcingSingleOngoing(trip)
        // Tạo days tương ứng với số ngày
        val days = generateDays(trip.copy(id = tripId))
        dayDao.insertDays(days)
        // Tạo 6 hạng mục chi phí mặc định = 0
        val expenses = ExpenseCategory.entries.map {
            Expense(tripId = tripId, category = it, planned = 0)
        }
        expenseDao.insertExpenses(expenses)
        refreshWidget()
        return tripId
    }

    suspend fun updateTrip(trip: Trip) {
        tripDao.updateTripEnforcingSingleOngoing(trip)
        refreshWidget()
    }

    suspend fun deleteTrip(trip: Trip) {
        tripDao.deleteTrip(trip)
        refreshWidget()
    }

    suspend fun updateTripStatus(id: Long, status: TripStatus) {
        tripDao.updateStatusEnforcingSingleOngoing(id, status)
        refreshWidget()
    }

    suspend fun autoUpdateTripStatuses() {
        val now = System.currentTimeMillis()
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = now }
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
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
        refreshWidget()
    }
        
    suspend fun updateTripStartDate(trip: Trip, newStartDate: Long, newStatus: TripStatus? = null) {
        val durationDays = if (trip.endDate >= trip.startDate && trip.startDate > 0) {
            ((trip.endDate - trip.startDate) / 86_400_000L).toInt() + 1
        } else {
            // For sample trips, they might have endDate=0, startDate=0. 
            // We can fetch max dayNumber to know duration, or just assume it from Days
            val days = dayDao.getDaysForTripOnce(trip.id)
            if (days.isNotEmpty()) days.maxOf { it.dayNumber } else 1
        }
        
        val newEndDate = newStartDate + (durationDays - 1) * 86_400_000L
        val updatedTrip = trip.copy(
            startDate = newStartDate, 
            endDate = newEndDate,
            status = newStatus ?: trip.status
        )
        tripDao.updateTripEnforcingSingleOngoing(updatedTrip)
        
        val days = dayDao.getDaysForTripOnce(trip.id)
        val updatedDays = days.map {
            it.copy(date = newStartDate + (it.dayNumber - 1) * 86_400_000L)
        }
        updatedDays.forEach { dayDao.updateDay(it) }
    }

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
    suspend fun getDaysForTripOnce(tripId: Long): List<Day> = dayDao.getDaysForTripOnce(tripId)
    suspend fun insertDayAndGetId(day: Day): Long = dayDao.insertDay(day)
    suspend fun updateDay(day: Day) = dayDao.updateDay(day)
    suspend fun deleteDay(day: Day) = dayDao.deleteDay(day)

    suspend fun getTodayDay(tripId: Long): Day? {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis
        val endOfDay = startOfDay + 86_399_999L
        return dayDao.getDayByDate(tripId, startOfDay, endOfDay)
    }

    // ── Activity ──────────────────────────────────────────────────────
    fun getActivities(dayId: Long): Flow<List<Activity>> = activityDao.getActivitiesForDay(dayId)
    suspend fun getActivitiesOnce(dayId: Long): List<Activity> = activityDao.getActivitiesForDayOnce(dayId)
    suspend fun insertActivity(activity: Activity): Long {
        val id = activityDao.insertActivity(activity)
        refreshWidget()
        return id
    }

    suspend fun updateActivity(activity: Activity) {
        activityDao.updateActivity(activity)
        refreshWidget()
    }

    suspend fun deleteActivity(activity: Activity) {
        activityDao.deleteActivity(activity)
        refreshWidget()
    }

    suspend fun updateActivityStatus(id: Long, status: ActivityStatus) {
        activityDao.updateStatus(id, status)
        refreshWidget()
    }

    // ── Note ──────────────────────────────────────────────────────────
    fun getNotes(tripId: Long): Flow<List<Note>> = noteDao.getNotesForTrip(tripId)
    fun getNotesForDay(dayId: Long): Flow<List<Note>> = noteDao.getNotesForDay(dayId)
    suspend fun getNoteById(id: Long): Note? = noteDao.getNoteById(id)
    suspend fun getNotesOnce(tripId: Long): List<Note> = noteDao.getNotesForTripOnce(tripId)
    suspend fun insertNote(note: Note): Long {
        val noteId = noteDao.insertNote(note)
        if (note.cost > 0) {
            val record = ExpenseRecord(
                tripId = note.tripId,
                dayId = note.dayId,
                category = note.tag.toExpenseCategory(),
                amount = note.cost,
                paidBy = note.paidBy,
                description = note.name.ifBlank { note.comment.take(30) }.ifBlank { note.tag.label },
                timestamp = note.timestamp,
                noteId = noteId
            )
            expenseDao.insertRecord(record)
        }
        refreshWidget()
        return noteId
    }

    suspend fun updateNote(note: Note) {
        noteDao.updateNote(note)
        val existingRecord = expenseDao.getRecordByNoteId(note.id)
        if (note.cost > 0) {
            val updatedRecord = ExpenseRecord(
                id = existingRecord?.id ?: 0,
                tripId = note.tripId,
                dayId = note.dayId,
                category = note.tag.toExpenseCategory(),
                amount = note.cost,
                paidBy = note.paidBy,
                description = note.name.ifBlank { note.comment.take(30) }.ifBlank { note.tag.label },
                timestamp = note.timestamp,
                noteId = note.id
            )
            if (existingRecord != null) {
                expenseDao.updateRecord(updatedRecord)
            } else {
                expenseDao.insertRecord(updatedRecord)
            }
        } else {
            if (existingRecord != null) {
                expenseDao.deleteRecord(existingRecord)
            }
        }
        refreshWidget()
    }

    suspend fun deleteNote(note: Note) {
        noteDao.deleteNote(note)
        expenseDao.deleteRecordByNoteId(note.id)
        refreshWidget()
    }

    private fun NoteTag.toExpenseCategory(): ExpenseCategory = when (this) {
        NoteTag.HOTEL -> ExpenseCategory.HOTEL
        NoteTag.FOOD -> ExpenseCategory.FOOD
        NoteTag.ATTRACTION -> ExpenseCategory.TICKET
        NoteTag.SHOP -> ExpenseCategory.GIFT
        NoteTag.TRANSPORT -> ExpenseCategory.TRANSPORT
        NoteTag.PERSON -> ExpenseCategory.MISC
        NoteTag.ADVANCE -> ExpenseCategory.ADVANCE
        NoteTag.OTHER -> ExpenseCategory.MISC
    }

    // ── Expense ───────────────────────────────────────────────────────
    fun getExpenses(tripId: Long): Flow<List<Expense>> = expenseDao.getExpensesForTrip(tripId)
    suspend fun getExpensesOnce(tripId: Long): List<Expense> = expenseDao.getExpensesForTripOnce(tripId)
    suspend fun updateExpense(expense: Expense) {
        expenseDao.updateExpense(expense)
        refreshWidget()
    }

    fun getExpenseRecords(tripId: Long): Flow<List<ExpenseRecord>> = expenseDao.getRecordsForTrip(tripId)
    suspend fun getExpenseRecordsOnce(tripId: Long): List<ExpenseRecord> = expenseDao.getRecordsForTripOnce(tripId)
    suspend fun insertExpenseRecord(record: ExpenseRecord): Long = expenseDao.insertRecord(record)
    suspend fun updateExpenseRecord(record: ExpenseRecord) = expenseDao.updateRecord(record)
    suspend fun deleteExpenseRecord(record: ExpenseRecord) = expenseDao.deleteRecord(record)

    fun getTotalPlanned(tripId: Long): Flow<Long?> = expenseDao.getTotalPlanned(tripId)
    fun getTotalActual(tripId: Long): Flow<Long?> = expenseDao.getTotalActual(tripId)

    /**
     * Import a trip from CSV data parsed by CsvImportUtils.
     * Creates Trip, Clusters (from unique cluster names), Days, and default Expenses.
     * Returns the new tripId.
     */
    suspend fun importFromCsv(data: CsvImportUtils.TripImportData): Long {
        val namesJson = JSONArray(data.memberNames).toString()
        val trip = Trip(
            name = data.name,
            type = data.type,
            startDate = data.startDateMs,
            endDate = data.endDateMs,
            numPeople = data.numPeople,
            memberNames = namesJson,
            description = data.description,
            useClusters = data.days.any { it.clusterName.isNotBlank() },
            themeColor = com.example.mytrip.ui.theme.TripThemeColors.getRandomColor()
        )
        val tripId = tripDao.insertTrip(trip)

        // Build cluster map (cluster name -> clusterId) preserving insertion order
        val clusterMap = linkedMapOf<String, Long>()
        data.days.forEach { day ->
            val cn = day.clusterName.trim()
            if (cn.isNotBlank() && !clusterMap.containsKey(cn)) {
                val idx = clusterMap.size
                val clusterId = clusterDao.insertCluster(
                    Cluster(tripId = tripId, name = cn, orderIndex = idx)
                )
                clusterMap[cn] = clusterId
            }
        }

        // Insert days
        var currentDate = data.startDateMs
        for (daySeed in data.days.sortedBy { it.dayNumber }) {
            val clusterId = clusterMap[daySeed.clusterName.trim()]
            val spotsJson = if (daySeed.checkInSpots.isNotEmpty()) {
                org.json.JSONArray(daySeed.checkInSpots).toString()
            } else ""
            val dayId = dayDao.insertDay(
                Day(
                    tripId = tripId,
                    clusterId = clusterId,
                    dayNumber = daySeed.dayNumber,
                    date = currentDate,
                    title = daySeed.title,
                    notes = daySeed.notes
                )
            )
            // Auto-create a single activity from the day's title/km
            if (daySeed.distanceKm > 0) {
                activityDao.insertActivity(
                    Activity(
                        dayId = dayId,
                        orderIndex = 0,
                        name = daySeed.title,
                        distanceKm = daySeed.distanceKm,
                        checkInSpots = spotsJson,
                        notes = daySeed.notes
                    )
                )
            }
            currentDate += 86_400_000L
        }

        // Default expense categories
        val expenses = ExpenseCategory.entries.map {
            Expense(tripId = tripId, category = it, planned = 0)
        }
        expenseDao.insertExpenses(expenses)

        refreshWidget()
        return tripId
    }

    suspend fun importFromExcel(data: ExcelImportUtils.ParsedData): Long {
        val tripId = tripDao.insertTrip(data.trip.copy(id = 0L))

        val dayIdMap = mutableMapOf<Long, Long>()
        for (day in data.days) {
            val dayId = dayDao.insertDay(day.copy(tripId = tripId, id = 0L))
            dayIdMap[day.id] = dayId
        }

        for (act in data.activities) {
            val newDayId = dayIdMap[act.dayId] ?: continue
            activityDao.insertActivity(act.copy(dayId = newDayId, id = 0L))
        }

        val expenses = if (data.expenses.isEmpty()) {
            ExpenseCategory.entries.map { Expense(tripId = tripId, category = it, planned = 0) }
        } else {
            data.expenses.map { it.copy(tripId = tripId, id = 0L) }
        }
        expenseDao.insertExpenses(expenses)

        for (rec in data.records) {
            expenseDao.insertRecord(rec.copy(tripId = tripId, id = 0L))
        }

        for (note in data.notes) {
            val newDayId = note.dayId?.let { dayIdMap[it] }
            noteDao.insertNote(note.copy(tripId = tripId, dayId = newDayId, id = 0L))
        }

        refreshWidget()
        return tripId
    }

    suspend fun importSeedTrip(overrideType: TripType? = null, isTemplate: Boolean = false): Long {
        // Clear old notes & images if any (simplified: just clear all seed trips)
        // Usually, the easiest way is to let the new trip have its own ID.
        // We will insert the seed trip as a NEW trip.
        val todayMs = DateUtils.todayMillis()
        val seedTrip = com.example.mytrip.data.seed.TripSeedData.trip.copy(
            id = 0,
            name = if (isTemplate) "Hành trình Xuyên Việt (Mẫu)" else "Hành trình Xuyên Việt",
            startDate = if (isTemplate) 0L else todayMs,
            endDate = if (isTemplate) (29 * 86_400_000L) else (todayMs + 29 * 86_400_000L),
            type = overrideType ?: com.example.mytrip.data.seed.TripSeedData.trip.type,
            themeColor = com.example.mytrip.ui.theme.TripThemeColors.getRandomColor()
        )
        val seedDays = com.example.mytrip.data.seed.TripSeedData.days
        
        val tripId = tripDao.insertTrip(seedTrip)
        
        // Bổ sung các khoản chi phí dự kiến mẫu theo yêu cầu
        val expenses = listOf(
            Expense(tripId = tripId, category = ExpenseCategory.HOTEL, planned = 13_050_000L, description = "450.000đ/đêm x 29 đêm (Ngày 30 trả phòng)"),
            Expense(tripId = tripId, category = ExpenseCategory.FOOD, planned = 18_000_000L, description = "200.000đ/người x 3 người x 30 ngày"),
            Expense(tripId = tripId, category = ExpenseCategory.TRANSPORT, planned = 8_232_000L, description = "Xăng xe (4.800km / 100) x 7L x 24.500đ/L"),
            Expense(tripId = tripId, category = ExpenseCategory.TICKET, planned = 6_675_000L, description = "Vé tham quan (Đã bỏ vé Bà Nà Hills)"),
            Expense(tripId = tripId, category = ExpenseCategory.MISC, planned = 3_500_000L, description = "Phí cầu đường (BOT & Cao tốc Bắc-Nam)"),
            Expense(tripId = tripId, category = ExpenseCategory.GIFT, planned = 0L, description = "Mua sắm / Quà cáp")
        )
        expenseDao.insertExpenses(expenses)
        
        // Tạo 6 cụm theo lộ trình
        val clusterNames = listOf(
            "Cụm 1: Bắc Trung Bộ (N1-N3)",
            "Cụm 2: Nam Trung Bộ (N4-N9)",
            "Cụm 3: Đông Nam Bộ (N10-N13)",
            "Cụm 4: Miền Tây (N14-N20)",
            "Cụm 5: Tây Nguyên (N21-N26)",
            "Cụm 6: Hồi Hương (N27-N30)"
        )
        val clusterIds = clusterNames.mapIndexed { idx, name ->
            clusterDao.insertCluster(Cluster(tripId = tripId, name = name, orderIndex = idx))
        }
        
        var currentDayDate = if (isTemplate) 0L else DateUtils.todayMillis()
        for (daySeed in seedDays) {
            val clusterId = when (daySeed.dayNumber) {
                in 1..3  -> clusterIds[0]   // Bắc Trung Bộ
                in 4..9  -> clusterIds[1]   // Nam Trung Bộ
                in 10..13 -> clusterIds[2]  // Đông Nam Bộ
                in 14..20 -> clusterIds[3]  // Miền Tây
                in 21..26 -> clusterIds[4]  // Tây Nguyên
                else     -> clusterIds[5]   // Hồi Hương (N27-N30)
            }
            val day = Day(
                tripId = tripId,
                clusterId = clusterId,
                dayNumber = daySeed.dayNumber,
                date = currentDayDate,
                title = daySeed.title,
                notes = daySeed.notes
            )
            val dayId = dayDao.insertDay(day)
            
            val activities = daySeed.activities.mapIndexed { index, actSeed ->
                Activity(
                    dayId = dayId,
                    orderIndex = index,
                    name = actSeed.name,
                    departureTime = actSeed.departure,
                    arrivalTime = actSeed.arrival,
                    distanceKm = actSeed.distanceKm,
                    hotelName = actSeed.hotelName,
                    hotelPricePlanned = actSeed.hotelPriceK,
                    checkInSpots = actSeed.checkInSpots,
                    mapsLink = actSeed.mapsLink,
                    notes = actSeed.actNotes
                )
            }
            activityDao.insertActivities(activities)
            
            if (!isTemplate) {
                currentDayDate += 86_400_000L
            }
        }
        refreshWidget()
        return tripId
    }
}
