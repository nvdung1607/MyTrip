package com.example.mytrip.ui.screens.summary

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mytrip.MyTripApplication
import com.example.mytrip.data.db.entities.*
import com.example.mytrip.data.repository.TripRepository
import com.example.mytrip.util.ExcelUtils
import com.example.mytrip.util.MoneyUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SummaryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TripRepository = (application as MyTripApplication).repository

    private val _trip = MutableStateFlow<Trip?>(null)
    val trip: StateFlow<Trip?> = _trip.asStateFlow()

    private val _days = MutableStateFlow<List<Day>>(emptyList())
    val days: StateFlow<List<Day>> = _days.asStateFlow()

    private val _activitiesMap = MutableStateFlow<Map<Long, List<Activity>>>(emptyMap())
    val activitiesMap: StateFlow<Map<Long, List<Activity>>> = _activitiesMap.asStateFlow()

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses.asStateFlow()

    private val _records = MutableStateFlow<List<ExpenseRecord>>(emptyList())
    val records: StateFlow<List<ExpenseRecord>> = _records.asStateFlow()

    private val _memberBalances = MutableStateFlow<Map<String, Long>>(emptyMap())
    val memberBalances: StateFlow<Map<String, Long>> = _memberBalances.asStateFlow()

    private var loadJob: kotlinx.coroutines.Job? = null

    fun loadData(tripId: Long) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            launch {
                repository.getTripById(tripId).collect { t ->
                    _trip.value = t
                    computeBalances()
                }
            }
            launch {
                repository.getDays(tripId).collect { dayList ->
                    _days.value = dayList
                    // Batch-load activities for all days in one go
                    val map = mutableMapOf<Long, List<com.example.mytrip.data.db.entities.Activity>>()
                    dayList.forEach { day ->
                        map[day.id] = repository.getActivitiesOnce(day.id)
                    }
                    _activitiesMap.value = map
                }
            }
            launch { repository.getNotes(tripId).collect { _notes.value = it } }
            launch { repository.getExpenses(tripId).collect { _expenses.value = it } }
            launch {
                repository.getExpenseRecords(tripId).collect { list ->
                    _records.value = list
                    computeBalances()
                }
            }
        }
    }

    private val _transfers = MutableStateFlow<List<MoneyUtils.Transfer>>(emptyList())
    val transfers: StateFlow<List<MoneyUtils.Transfer>> = _transfers.asStateFlow()

    private fun computeBalances() {
        val t = _trip.value ?: return
        val recs = _records.value
        val names = t.memberNames.trim('[', ']')
            .split(",").map { it.trim().trim('"') }.filter { it.isNotBlank() }
            .ifEmpty { listOf("Tôi") }
        val result = MoneyUtils.splitExpenses(recs, t.numPeople, names)
        _memberBalances.value = result.first
        _transfers.value = result.second
    }

    fun exportToExcel(context: Context) {
        val t = _trip.value ?: return
        val names = t.memberNames.trim('[', ']')
            .split(",").map { it.trim().trim('"') }.filter { it.isNotBlank() }.ifEmpty { listOf("Tôi") }
        viewModelScope.launch {
            try {
                val uri = ExcelUtils.exportTripToExcel(
                    context, t, _days.value, _activitiesMap.value,
                    _expenses.value, _records.value, _notes.value, names
                )
                ExcelUtils.shareExcelFile(context, uri)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private val _isExportingPhotos = MutableStateFlow(false)
    val isExportingPhotos: StateFlow<Boolean> = _isExportingPhotos.asStateFlow()

    fun exportPhotos(context: Context, onComplete: (Int) -> Unit) {
        val t = _trip.value ?: return
        val tripNotes = _notes.value
        viewModelScope.launch {
            _isExportingPhotos.value = true
            val count = com.example.mytrip.util.PhotoExportUtils.exportTripPhotos(context, t.name, tripNotes)
            _isExportingPhotos.value = false
            onComplete(count)
        }
    }

    companion object {
        fun factory(app: Application) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(c: Class<T>) = SummaryViewModel(app) as T
        }
    }
}
