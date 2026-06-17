package com.example.mytrip.ui.screens.expense

import android.app.Application

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mytrip.MyTripApplication
import com.example.mytrip.data.db.entities.*
import com.example.mytrip.data.repository.TripRepository
import org.json.JSONArray

import com.example.mytrip.util.MoneyUtils
import com.example.mytrip.widget.MyTripWidgetUpdater
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TripRepository = (application as MyTripApplication).repository

    private val _trip = MutableStateFlow<Trip?>(null)
    val trip: StateFlow<Trip?> = _trip.asStateFlow()

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses.asStateFlow()

    private val _records = MutableStateFlow<List<ExpenseRecord>>(emptyList())
    val records: StateFlow<List<ExpenseRecord>> = _records.asStateFlow()

    private val _totalPlanned = MutableStateFlow(0L)
    val totalPlanned: StateFlow<Long> = _totalPlanned.asStateFlow()

    private val _totalActual = MutableStateFlow(0L)
    val totalActual: StateFlow<Long> = _totalActual.asStateFlow()

    private val _memberBalances = MutableStateFlow<Map<String, Long>>(emptyMap())
    val memberBalances: StateFlow<Map<String, Long>> = _memberBalances.asStateFlow()

    private val _transfers = MutableStateFlow<List<MoneyUtils.Transfer>>(emptyList())
    val transfers: StateFlow<List<MoneyUtils.Transfer>> = _transfers.asStateFlow()

    private var tripId = 0L

    private var loadJob: kotlinx.coroutines.Job? = null

    fun loadData(id: Long) {
        tripId = id
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            launch {
                repository.getTripById(id).collect { t ->
                    _trip.value = t
                    computeBalances()
                }
            }
            launch {
                repository.getExpenses(id).collect { list ->
                    _expenses.value = list
                    _totalPlanned.value = list.sumOf { it.planned }
                }
            }
            launch {
                repository.getExpenseRecords(id).collect { list ->
                    _records.value = list
                    _totalActual.value = list.filter { it.category != ExpenseCategory.ADVANCE }.sumOf { it.amount }
                    computeBalances()
                }
            }
        }
    }

    fun updatePlanned(expense: Expense) {
        viewModelScope.launch {
            repository.updateExpense(expense)
            MyTripWidgetUpdater.update(getApplication())
        }
    }

    fun addRecord(record: ExpenseRecord) {
        viewModelScope.launch {
            repository.insertExpenseRecord(record)
            MyTripWidgetUpdater.update(getApplication())
        }
    }

    fun updateRecord(record: ExpenseRecord) {
        viewModelScope.launch {
            repository.updateExpenseRecord(record)
            MyTripWidgetUpdater.update(getApplication())
        }
    }

    fun deleteRecord(record: ExpenseRecord) {
        viewModelScope.launch {
            repository.deleteExpenseRecord(record)
            MyTripWidgetUpdater.update(getApplication())
        }
    }

    private fun computeBalances() {
        val t = _trip.value ?: return
        val recs = _records.value
        val names = try {
            val arr = JSONArray(t.memberNames)
            (0 until arr.length()).map { arr.getString(it) }.filter { it.isNotBlank() }.ifEmpty { listOf("Tôi") }
        } catch (_: Exception) {
            listOf("Tôi")
        }
        val result = MoneyUtils.splitExpenses(recs, t.numPeople, names)
        _memberBalances.value = result.first
        _transfers.value = result.second
    }


    companion object {
        fun factory(app: Application) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(c: Class<T>) = ExpenseViewModel(app) as T
        }
    }
}
