package com.example.mytrip.ui.screens.summary

import android.app.Application
import android.content.Context
import android.content.Intent
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

    fun loadData(tripId: Long) {
        viewModelScope.launch {
            repository.getTripById(tripId).collect { t ->
                _trip.value = t
                computeBalances()
            }
        }
        viewModelScope.launch {
            repository.getDays(tripId).collect { dayList ->
                _days.value = dayList
                // Load activities for each day
                val map = mutableMapOf<Long, List<Activity>>()
                dayList.forEach { day ->
                    map[day.id] = repository.getActivitiesOnce(day.id)
                }
                _activitiesMap.value = map
            }
        }
        viewModelScope.launch { repository.getNotes(tripId).collect { _notes.value = it } }
        viewModelScope.launch { repository.getExpenses(tripId).collect { _expenses.value = it } }
        viewModelScope.launch {
            repository.getExpenseRecords(tripId).collect { list ->
                _records.value = list
                computeBalances()
            }
        }
    }

    private fun computeBalances() {
        val t = _trip.value ?: return
        val recs = _records.value
        val names = t.memberNames.trim('[', ']')
            .split(",").map { it.trim().trim('"') }.filter { it.isNotBlank() }
            .ifEmpty { listOf("Tôi") }
        _memberBalances.value = MoneyUtils.splitExpenses(recs.map { it.paidBy to it.amount }, t.numPeople, names)
    }

    fun shareTextReport(context: Context) {
        val t = _trip.value ?: return
        val names = t.memberNames.trim('[', ']')
            .split(",").map { it.trim().trim('"') }.filter { it.isNotBlank() }.ifEmpty { listOf("Tôi") }
        val text = ExcelUtils.generateTextReport(t, _expenses.value, _records.value, names)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Chia sẻ tổng kết chuyến đi"))
    }

    fun exportToExcel(context: Context) {
        val t = _trip.value ?: return
        viewModelScope.launch {
            try {
                val uri = ExcelUtils.exportTripToExcel(
                    context, t, _days.value, _activitiesMap.value, _expenses.value, _records.value
                )
                ExcelUtils.shareExcelFile(context, uri)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        fun factory(app: Application) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(c: Class<T>) = SummaryViewModel(app) as T
        }
    }
}
