package com.example.mytrip.ui.screens.notes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mytrip.MyTripApplication
import com.example.mytrip.data.db.entities.Day
import com.example.mytrip.data.db.entities.Note
import com.example.mytrip.data.db.entities.NoteTag
import com.example.mytrip.data.db.entities.Trip
import com.example.mytrip.data.repository.TripRepository
import com.example.mytrip.widget.MyTripWidgetUpdater
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class NoteFilter {
    object All : NoteFilter()
    data class ByDate(val startOfDay: Long) : NoteFilter()
    data class ByWeek(val weekNumber: Int) : NoteFilter() // 1 = N1-N7, 2 = N8-N14, etc.
    data class ByTag(val tag: NoteTag) : NoteFilter()
}

data class DateFilterOption(val startOfDay: Long, val label: String, val dayNumber: Int?)

@OptIn(ExperimentalCoroutinesApi::class)
class AllNotesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TripRepository =
        (application as MyTripApplication).repository

    private val _tripId = MutableStateFlow<Long?>(null)

    val trip: StateFlow<Trip?> = _tripId.flatMapLatest { id ->
        if (id != null) repository.getTripById(id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val days: StateFlow<List<Day>> = _tripId.flatMapLatest { id ->
        if (id != null) repository.getDays(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _allNotes = _tripId.flatMapLatest { id ->
        if (id != null) repository.getNotes(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _currentFilter = MutableStateFlow<NoteFilter>(NoteFilter.All)
    val currentFilter: StateFlow<NoteFilter> = _currentFilter.asStateFlow()

    val dateFilterOptions: StateFlow<List<DateFilterOption>> = combine(days, _allNotes) { dayList, notes ->
        val optionsMap = mutableMapOf<Long, DateFilterOption>()
        for (day in dayList) {
            optionsMap[day.date] = DateFilterOption(day.date, "Ngày ${day.dayNumber} (${com.example.mytrip.util.DateUtils.formatDate(day.date)})", day.dayNumber)
        }
        for (note in notes) {
            val startOfDay = com.example.mytrip.util.DateUtils.startOfDay(note.timestamp)
            if (!optionsMap.containsKey(startOfDay)) {
                optionsMap[startOfDay] = DateFilterOption(startOfDay, "Ngày khác (${com.example.mytrip.util.DateUtils.formatDate(startOfDay)})", null)
            }
        }
        optionsMap.values.sortedBy { it.startOfDay }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Filtered notes derived from combining notes, days list, and current filter state
    val filteredNotes: StateFlow<List<Note>> = combine(_allNotes, dateFilterOptions, _currentFilter) { notes, options, filter ->
        val resultList = when (filter) {
            is NoteFilter.All -> notes
            is NoteFilter.ByDate -> notes.filter { 
                it.timestamp in filter.startOfDay..(filter.startOfDay + 86399999L) 
            }
            is NoteFilter.ByWeek -> notes.filter { note ->
                val startOfDay = com.example.mytrip.util.DateUtils.startOfDay(note.timestamp)
                val dayNum = options.find { it.startOfDay == startOfDay }?.dayNumber
                if (dayNum != null) {
                    val w = ((dayNum - 1) / 7) + 1
                    w == filter.weekNumber
                } else false
            }
            is NoteFilter.ByTag -> notes.filter { it.tag == filter.tag }
        }
        resultList.sortedByDescending { it.timestamp }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun loadTrip(tripId: Long) {
        _tripId.value = tripId
    }

    fun setFilter(filter: NoteFilter) {
        _currentFilter.value = filter
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
            MyTripWidgetUpdater.update(getApplication())
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(
                modelClass: Class<T>,
                extras: androidx.lifecycle.viewmodel.CreationExtras
            ): T {
                val application =
                    extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                return AllNotesViewModel(application) as T
            }
        }
    }
}
