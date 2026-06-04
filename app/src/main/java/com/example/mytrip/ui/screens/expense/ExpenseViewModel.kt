package com.example.mytrip.ui.screens.expense

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

    private var tripId = 0L

    fun loadData(id: Long) {
        tripId = id
        viewModelScope.launch {
            repository.getTripById(id).collect { t ->
                _trip.value = t
                computeBalances()
            }
        }
        viewModelScope.launch {
            repository.getExpenses(id).collect { list ->
                _expenses.value = list
                _totalPlanned.value = list.sumOf { it.planned }
            }
        }
        viewModelScope.launch {
            repository.getExpenseRecords(id).collect { list ->
                _records.value = list
                _totalActual.value = list.sumOf { it.amount }
                computeBalances()
            }
        }
    }

    fun updatePlanned(expense: Expense) {
        viewModelScope.launch { repository.updateExpense(expense) }
    }

    fun addRecord(record: ExpenseRecord) {
        viewModelScope.launch { repository.insertExpenseRecord(record) }
    }

    fun updateRecord(record: ExpenseRecord) {
        viewModelScope.launch { repository.updateExpenseRecord(record) }
    }

    fun deleteRecord(record: ExpenseRecord) {
        viewModelScope.launch { repository.deleteExpenseRecord(record) }
    }

    private fun computeBalances() {
        val t = _trip.value ?: return
        val recs = _records.value
        val names = t.memberNames.trim('[', ']')
            .split(",").map { it.trim().trim('"') }.filter { it.isNotBlank() }
            .ifEmpty { listOf("Tôi") }
        val pairs = recs.map { it.paidBy to it.amount }
        _memberBalances.value = MoneyUtils.splitExpenses(pairs, t.numPeople, names)
    }


    companion object {
        fun factory(app: Application) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(c: Class<T>) = ExpenseViewModel(app) as T
        }
    }
}
