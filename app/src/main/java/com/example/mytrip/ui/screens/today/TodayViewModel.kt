package com.example.mytrip.ui.screens.today

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mytrip.MyTripApplication
import com.example.mytrip.data.db.entities.Activity
import com.example.mytrip.data.db.entities.ActivityStatus
import com.example.mytrip.data.db.entities.Day
import com.example.mytrip.data.db.entities.Note
import com.example.mytrip.data.db.entities.Trip
import com.example.mytrip.data.repository.TripRepository
import com.example.mytrip.util.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * selectedDayIndex: 0 = hôm qua, 1 = hôm nay, 2 = ngày mai
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TripRepository =
        (application as MyTripApplication).repository

    private val _trip = MutableStateFlow<Trip?>(null)
    val trip: StateFlow<Trip?> = _trip.asStateFlow()

    private val _allDays = MutableStateFlow<List<Day>>(emptyList())
    val allDays: StateFlow<List<Day>> = _allDays.asStateFlow()

    // 0=yesterday, 1=today, 2=tomorrow
    private val _selectedDayIndex = MutableStateFlow(1)
    val selectedDayIndex: StateFlow<Int> = _selectedDayIndex.asStateFlow()

    private val _todayDay = MutableStateFlow<Day?>(null)
    val todayDay: StateFlow<Day?> = _todayDay.asStateFlow()

    private val _todayActivities = MutableStateFlow<List<Activity>>(emptyList())
    val todayActivities: StateFlow<List<Activity>> = _todayActivities.asStateFlow()

    private val _todayNotes = MutableStateFlow<List<Note>>(emptyList())
    val todayNotes: StateFlow<List<Note>> = _todayNotes.asStateFlow()

    private var currentTripId: Long = -1L

    // Live flow from the currently selected day's activities
    private val _activeDayId = MutableStateFlow<Long?>(null)

    private val _activitiesFlow = _activeDayId.flatMapLatest { dayId ->
        if (dayId == null) flowOf(emptyList())
        else repository.getActivities(dayId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _notesFlow = _activeDayId.flatMapLatest { dayId ->
        if (dayId == null) flowOf(emptyList())
        else repository.getNotesForDay(dayId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            _activitiesFlow.collect { list ->
                _todayActivities.value = list
            }
        }
        viewModelScope.launch {
            _notesFlow.collect { list ->
                _todayNotes.value = list
            }
        }
    }

    fun loadData(tripId: Long) {
        currentTripId = tripId
        viewModelScope.launch {
            _trip.value = repository.getTripByIdOnce(tripId)
            repository.getDays(tripId).collect { days ->
                _allDays.value = days
                applyDaySelection(days, _selectedDayIndex.value)
            }
        }
    }

    fun changeDay(index: Int) {
        _selectedDayIndex.value = index
        applyDaySelection(_allDays.value, index)
    }

    private fun applyDaySelection(days: List<Day>, index: Int) {
        val targetMillis = when (index) {
            0 -> DateUtils.todayMillis() - 86_400_000L // yesterday
            2 -> DateUtils.todayMillis() + 86_400_000L // tomorrow
            else -> DateUtils.todayMillis()             // today
        }
        val day = days.firstOrNull { day ->
            val startOfDay = targetMillis
            val endOfDay = targetMillis + 86_399_999L
            day.date in startOfDay..endOfDay
        }
        _todayDay.value = day
        _activeDayId.value = day?.id
    }

    fun updateActivityStatus(activityId: Long, status: ActivityStatus) {
        viewModelScope.launch {
            repository.updateActivityStatus(activityId, status)
        }
    }

    fun markActivityDone(id: Long) = updateActivityStatus(id, ActivityStatus.DONE)
    fun markActivitySkipped(id: Long) = updateActivityStatus(id, ActivityStatus.SKIPPED)
    fun markActivityChanged(id: Long) = updateActivityStatus(id, ActivityStatus.CHANGED)

    fun updateActivityActualNotes(activity: Activity, notes: String) {
        viewModelScope.launch {
            repository.updateActivity(activity.copy(actualNotes = notes))
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
                    extras[androidx.lifecycle.viewmodel.MutableCreationExtras.APPLICATION_KEY]!!
                return TodayViewModel(application) as T
            }
        }
    }
}
