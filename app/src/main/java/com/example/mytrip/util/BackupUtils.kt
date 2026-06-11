package com.example.mytrip.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.mytrip.data.db.entities.*
import com.example.mytrip.data.repository.TripRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileReader
import java.io.FileWriter

/**
 * Backup & Restore utility — serializes a trip and all its data (Days, Activities,
 * Notes, Expenses, ExpenseRecords) to a JSON file that can be shared or saved,
 * then restored on the same or another device.
 */
object BackupUtils {

    // ── Export ────────────────────────────────────────────────────────────────

    /**
     * Export a single trip to a JSON string.
     */
    suspend fun exportTripToJson(
        repository: TripRepository,
        tripId: Long
    ): String = withContext(Dispatchers.IO) {
        val trip = repository.getTripByIdOnce(tripId)
            ?: throw IllegalArgumentException("Trip $tripId not found")
        val days = repository.getDaysForTripOnce(tripId)
        val activities = days.flatMap { day ->
            repository.getActivitiesOnce(day.id)
        }
        val notes = repository.getNotesOnce(tripId)
        val expenses = repository.getExpensesOnce(tripId)
        val records = repository.getExpenseRecordsOnce(tripId)

        val json = JSONObject().apply {
            put("version", 1)
            put("exportedAt", System.currentTimeMillis())
            put("trip", tripToJson(trip))
            put("days", JSONArray(days.map { dayToJson(it) }))
            put("activities", JSONArray(activities.map { actToJson(it) }))
            put("notes", JSONArray(notes.map { noteToJson(it) }))
            put("expenses", JSONArray(expenses.map { expenseToJson(it) }))
            put("records", JSONArray(records.map { recordToJson(it) }))
        }
        json.toString(2)
    }

    /**
     * Save JSON to cache and return a shareable URI.
     */
    fun saveToCache(context: Context, json: String, tripName: String): Uri? {
        return try {
            val cacheDir = File(context.cacheDir, "backup").also { it.mkdirs() }
            val safeName = tripName.replace(Regex("[^\\w\\s-]"), "").trim().replace(" ", "_")
            val file = File(cacheDir, "MyTrip_${safeName}_backup.json")
            FileWriter(file).use { it.write(json) }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Save JSON to Downloads and return the file path.
     */
    fun saveToDownloads(context: Context, json: String, tripName: String): File? {
        return try {
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS
            ).also { it.mkdirs() }
            val safeName = tripName.replace(Regex("[^\\w\\s-]"), "").trim().replace(" ", "_")
            val file = File(downloadsDir, "MyTrip_${safeName}_backup.json")
            FileWriter(file).use { it.write(json) }
            file
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Launch share intent for a backup URI.
     */
    fun shareBackup(context: Context, uri: Uri, tripName: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Backup chuyến đi: $tripName")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Chia sẻ backup qua..."))
    }

    // ── Import ────────────────────────────────────────────────────────────────

    /**
     * Restore a trip from a JSON string. Creates new DB records with new IDs.
     * Returns the new tripId.
     */
    suspend fun importFromJson(
        repository: TripRepository,
        json: String
    ): Long = withContext(Dispatchers.IO) {
        val root = JSONObject(json)
        val tripJson = root.getJSONObject("trip")
        val daysArray = root.getJSONArray("days")
        val activitiesArray = root.getJSONArray("activities")
        val notesArray = root.getJSONArray("notes")
        val expensesArray = root.getJSONArray("expenses")
        val recordsArray = root.getJSONArray("records")

        val trip = tripFromJson(tripJson)
        val newTripId = repository.createTrip(trip.copy(id = 0))

        // Build day ID map: old → new
        val dayIdMap = mutableMapOf<Long, Long>()
        for (i in 0 until daysArray.length()) {
            val dayJson = daysArray.getJSONObject(i)
            val oldDay = dayFromJson(dayJson)
            val newDayId = repository.insertDayAndGetId(oldDay.copy(id = 0, tripId = newTripId))
            dayIdMap[oldDay.id] = newDayId
        }

        // Activities
        for (i in 0 until activitiesArray.length()) {
            val act = actFromJson(activitiesArray.getJSONObject(i))
            val newDayId = dayIdMap[act.dayId] ?: continue
            repository.insertActivity(act.copy(id = 0, dayId = newDayId))
        }

        // Notes
        for (i in 0 until notesArray.length()) {
            val note = noteFromJson(notesArray.getJSONObject(i))
            val newDayId = note.dayId?.let { dayIdMap[it] }
            repository.insertNote(note.copy(id = 0, tripId = newTripId, dayId = newDayId))
        }

        // Expenses (budget categories)
        for (i in 0 until expensesArray.length()) {
            val exp = expenseFromJson(expensesArray.getJSONObject(i))
            repository.updateExpense(exp.copy(id = 0, tripId = newTripId))
        }

        // ExpenseRecords
        for (i in 0 until recordsArray.length()) {
            val rec = recordFromJson(recordsArray.getJSONObject(i))
            val newDayId = rec.dayId?.let { dayIdMap[it] }
            repository.insertExpenseRecord(rec.copy(id = 0, tripId = newTripId, dayId = newDayId))
        }

        newTripId
    }

    // ── Serializers ───────────────────────────────────────────────────────────

    private fun tripToJson(t: Trip) = JSONObject().apply {
        put("id", t.id); put("name", t.name); put("description", t.description)
        put("type", t.type.name); put("status", t.status.name)
        put("startDate", t.startDate); put("endDate", t.endDate)
        put("coverImagePath", t.coverImagePath ?: ""); put("numPeople", t.numPeople)
        put("memberNames", t.memberNames); put("useClusters", t.useClusters)
        put("themeColor", t.themeColor); put("createdAt", t.createdAt)
    }

    private fun tripFromJson(j: JSONObject) = Trip(
        id = j.getLong("id"), name = j.getString("name"),
        description = j.optString("description", ""),
        type = runCatching { TripType.valueOf(j.getString("type")) }.getOrDefault(TripType.CAR),
        status = runCatching { TripStatus.valueOf(j.getString("status")) }.getOrDefault(TripStatus.PLANNING),
        startDate = j.getLong("startDate"), endDate = j.getLong("endDate"),
        coverImagePath = j.optString("coverImagePath").ifBlank { null },
        numPeople = j.getInt("numPeople"), memberNames = j.getString("memberNames"),
        useClusters = j.optBoolean("useClusters", false),
        themeColor = j.optString("themeColor", ""), createdAt = j.optLong("createdAt", 0L)
    )

    private fun dayToJson(d: Day) = JSONObject().apply {
        put("id", d.id); put("tripId", d.tripId); put("clusterId", d.clusterId ?: -1L)
        put("dayNumber", d.dayNumber); put("date", d.date)
        put("title", d.title); put("notes", d.notes)
    }

    private fun dayFromJson(j: JSONObject) = Day(
        id = j.getLong("id"), tripId = j.getLong("tripId"),
        clusterId = j.getLong("clusterId").takeIf { it != -1L },
        dayNumber = j.getInt("dayNumber"), date = j.getLong("date"),
        title = j.optString("title", ""), notes = j.optString("notes", "")
    )

    private fun actToJson(a: Activity) = JSONObject().apply {
        put("id", a.id); put("dayId", a.dayId); put("orderIndex", a.orderIndex)
        put("activityType", a.activityType.name); put("name", a.name)
        put("departureTime", a.departureTime); put("arrivalTime", a.arrivalTime)
        put("distanceKm", a.distanceKm); put("hotelName", a.hotelName)
        put("hotelPricePlanned", a.hotelPricePlanned)
        put("checkInSpots", a.checkInSpots); put("mapsLink", a.mapsLink)
        put("notes", a.notes); put("status", a.status.name)
        put("actualDepartureTime", a.actualDepartureTime)
        put("actualArrivalTime", a.actualArrivalTime); put("actualNotes", a.actualNotes)
        put("reminderMinutes", a.reminderMinutes)
    }

    private fun actFromJson(j: JSONObject) = Activity(
        id = j.getLong("id"), dayId = j.getLong("dayId"),
        orderIndex = j.getInt("orderIndex"),
        activityType = runCatching { ActivityType.valueOf(j.getString("activityType")) }.getOrDefault(ActivityType.TRANSIT),
        name = j.getString("name"),
        departureTime = j.optString("departureTime", ""),
        arrivalTime = j.optString("arrivalTime", ""),
        distanceKm = j.optDouble("distanceKm", 0.0),
        hotelName = j.optString("hotelName", ""),
        hotelPricePlanned = j.optLong("hotelPricePlanned", 0),
        checkInSpots = j.optString("checkInSpots", ""),
        mapsLink = j.optString("mapsLink", ""),
        notes = j.optString("notes", ""),
        status = runCatching { ActivityStatus.valueOf(j.getString("status")) }.getOrDefault(ActivityStatus.PENDING),
        actualDepartureTime = j.optString("actualDepartureTime", ""),
        actualArrivalTime = j.optString("actualArrivalTime", ""),
        actualNotes = j.optString("actualNotes", ""),
        reminderMinutes = j.optInt("reminderMinutes", 0)
    )

    private fun noteToJson(n: Note) = JSONObject().apply {
        put("id", n.id); put("tripId", n.tripId); put("dayId", n.dayId ?: -1L)
        put("timestamp", n.timestamp); put("photoPath", n.photoPath ?: "")
        put("photoPaths", JSONArray(n.photoPaths))
        put("rating", n.rating); put("tag", n.tag.name); put("cost", n.cost)
        put("paidBy", n.paidBy); put("name", n.name); put("comment", n.comment)
        put("gpsLat", n.gpsLat ?: Double.NaN); put("gpsLng", n.gpsLng ?: Double.NaN)
    }

    private fun noteFromJson(j: JSONObject) = Note(
        id = j.getLong("id"), tripId = j.getLong("tripId"),
        dayId = j.getLong("dayId").takeIf { it != -1L },
        timestamp = j.getLong("timestamp"),
        photoPath = j.optString("photoPath").ifBlank { null },
        photoPaths = run {
            val arr = j.optJSONArray("photoPaths") ?: JSONArray()
            (0 until arr.length()).map { arr.getString(it) }
        },
        rating = j.optInt("rating", 0),
        tag = runCatching { NoteTag.valueOf(j.getString("tag")) }.getOrDefault(NoteTag.OTHER),
        cost = j.optLong("cost", 0),
        paidBy = j.optString("paidBy", ""),
        name = j.optString("name", ""), comment = j.optString("comment", ""),
        gpsLat = j.optDouble("gpsLat").takeIf { !it.isNaN() },
        gpsLng = j.optDouble("gpsLng").takeIf { !it.isNaN() }
    )

    private fun expenseToJson(e: Expense) = JSONObject().apply {
        put("id", e.id); put("tripId", e.tripId)
        put("category", e.category.name); put("planned", e.planned)
        put("description", e.description)
    }

    private fun expenseFromJson(j: JSONObject) = Expense(
        id = j.getLong("id"), tripId = j.getLong("tripId"),
        category = runCatching { ExpenseCategory.valueOf(j.getString("category")) }.getOrDefault(ExpenseCategory.MISC),
        planned = j.optLong("planned", 0), description = j.optString("description", "")
    )

    private fun recordToJson(r: ExpenseRecord) = JSONObject().apply {
        put("id", r.id); put("tripId", r.tripId); put("dayId", r.dayId ?: -1L)
        put("category", r.category.name); put("amount", r.amount)
        put("paidBy", r.paidBy); put("description", r.description)
        put("timestamp", r.timestamp); put("noteId", r.noteId ?: -1L)
    }

    private fun recordFromJson(j: JSONObject) = ExpenseRecord(
        id = j.getLong("id"), tripId = j.getLong("tripId"),
        dayId = j.getLong("dayId").takeIf { it != -1L },
        category = runCatching { ExpenseCategory.valueOf(j.getString("category")) }.getOrDefault(ExpenseCategory.MISC),
        amount = j.getLong("amount"), paidBy = j.optString("paidBy", ""),
        description = j.optString("description", ""),
        timestamp = j.getLong("timestamp"),
        noteId = j.getLong("noteId").takeIf { it != -1L }
    )
}
