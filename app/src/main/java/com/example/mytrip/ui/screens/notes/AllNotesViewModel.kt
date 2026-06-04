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
    data class ByDay(val dayId: Long) : NoteFilter()
    data class ByWeek(val weekNumber: Int) : NoteFilter() // 1 = N1-N7, 2 = N8-N14, etc.
    data class ByTag(val tag: NoteTag) : NoteFilter()
}

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

    // Filtered notes derived from combining notes, days list, and current filter state
    val filteredNotes: StateFlow<List<Note>> = combine(_allNotes, days, _currentFilter) { notes, dayList, filter ->
        val sortedDays = dayList.sortedBy { it.date }

        // Helper: resolve dayNumber for a note, falling back to timestamp if dayId is null
        fun resolveDayNumber(note: Note): Int? {
            if (note.dayId != null) {
                return dayList.find { it.id == note.dayId }?.dayNumber
            }
            // Fallback: find the day whose date range covers the note's timestamp
            for (day in sortedDays) {
                val dayStart = day.date
                val dayEnd = dayStart + 86_399_999L
                if (note.timestamp in dayStart..dayEnd) {
                    return day.dayNumber
                }
            }
            return null
        }

        // Helper: find day by timestamp match
        fun resolveDayId(note: Note): Long? {
            if (note.dayId != null) return note.dayId
            for (day in sortedDays) {
                val dayStart = day.date
                val dayEnd = dayStart + 86_399_999L
                if (note.timestamp in dayStart..dayEnd) {
                    return day.id
                }
            }
            return null
        }

        val resultList = when (filter) {
            is NoteFilter.All -> notes
            is NoteFilter.ByDay -> notes.filter { resolveDayId(it) == filter.dayId }
            is NoteFilter.ByWeek -> notes.filter { note ->
                val dayNum = resolveDayNumber(note)
                if (dayNum != null) {
                    val w = ((dayNum - 1) / 7) + 1
                    w == filter.weekNumber
                } else false
            }
            is NoteFilter.ByTag -> notes.filter { it.tag == filter.tag }
        }
        resultList.sortedBy { it.timestamp }
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
