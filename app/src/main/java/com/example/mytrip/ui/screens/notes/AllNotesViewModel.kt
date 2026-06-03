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
        val dayIdToNum = dayList.associate { it.id to it.dayNumber }
        
        when (filter) {
            is NoteFilter.All -> notes
            is NoteFilter.ByDay -> notes.filter { it.dayId == filter.dayId }
            is NoteFilter.ByWeek -> notes.filter { note ->
                val dayNum = dayIdToNum[note.dayId]
                if (dayNum != null) {
                    val w = ((dayNum - 1) / 7) + 1
                    w == filter.weekNumber
                } else false
            }
            is NoteFilter.ByTag -> notes.filter { it.tag == filter.tag }
        }
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
