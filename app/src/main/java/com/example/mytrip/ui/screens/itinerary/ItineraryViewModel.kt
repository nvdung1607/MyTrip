package com.example.mytrip.ui.screens.itinerary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mytrip.MyTripApplication
import com.example.mytrip.data.db.entities.Activity
import com.example.mytrip.data.db.entities.ActivityStatus
import com.example.mytrip.data.db.entities.Day
import com.example.mytrip.data.db.entities.Trip
import com.example.mytrip.data.repository.TripRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ItineraryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TripRepository =
        (application as MyTripApplication).repository

    // ── Internal trip ID trigger ──────────────────────────────────────
    private val _tripId = MutableStateFlow<Long?>(null)

    // ── Public state ──────────────────────────────────────────────────
    val trip: StateFlow<Trip?> = _tripId.flatMapLatest { id ->
        if (id != null) repository.getTripById(id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val days: StateFlow<List<Day>> = _tripId.flatMapLatest { id ->
        if (id != null) repository.getDays(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Map of dayId -> activities list, rebuilt whenever days or any activity list changes
    private val _activitiesMap = MutableStateFlow<Map<Long, List<Activity>>>(emptyMap())
    val activitiesMap: StateFlow<Map<Long, List<Activity>>> = _activitiesMap.asStateFlow()

    val expandedDays = MutableStateFlow<Set<Long>>(emptySet())
    val expandedClusters = MutableStateFlow<Set<Long>>(emptySet())

    // ── Load data ─────────────────────────────────────────────────────
    fun loadData(tripId: Long) {
        _tripId.value = tripId
        // Observe days and for each day observe activities
        viewModelScope.launch {
            days.collect { dayList ->
                if (dayList.isEmpty()) return@collect
                // Initialize expanded state for all days
                expandedDays.value = dayList.map { it.id }.toSet()
                // Start collecting activities for each day
                dayList.forEach { day ->
                    launch {
                        repository.getActivities(day.id).collect { activities ->
                            val current = _activitiesMap.value.toMutableMap()
                            current[day.id] = activities
                            _activitiesMap.value = current
                        }
                    }
                }
            }
        }
    }

    // ── Toggle expand ─────────────────────────────────────────────────
    fun toggleDayExpanded(dayId: Long) {
        val current = expandedDays.value.toMutableSet()
        if (current.contains(dayId)) current.remove(dayId) else current.add(dayId)
        expandedDays.value = current
    }

    fun toggleClusterExpanded(clusterId: Long) {
        val current = expandedClusters.value.toMutableSet()
        if (current.contains(clusterId)) current.remove(clusterId) else current.add(clusterId)
        expandedClusters.value = current
    }

    fun expandAll() {
        expandedDays.value = days.value.map { it.id }.toSet()
    }

    fun collapseAll() {
        expandedDays.value = emptySet()
    }

    // ── Activity CRUD ─────────────────────────────────────────────────
    fun addActivity(activity: Activity) {
        viewModelScope.launch {
            val dayActivities = _activitiesMap.value[activity.dayId] ?: emptyList()
            val ordered = activity.copy(orderIndex = dayActivities.size)
            repository.insertActivity(ordered)
        }
    }

    fun updateActivity(activity: Activity) {
        viewModelScope.launch {
            repository.updateActivity(activity)
        }
    }

    fun deleteActivity(activity: Activity) {
        viewModelScope.launch {
            repository.deleteActivity(activity)
        }
    }

    fun updateActivityStatus(id: Long, status: ActivityStatus) {
        viewModelScope.launch {
            repository.updateActivityStatus(id, status)
        }
    }

    fun reorderActivities(dayId: Long, fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val list = (_activitiesMap.value[dayId] ?: return@launch).toMutableList()
            if (fromIndex !in list.indices || toIndex !in list.indices) return@launch
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            val reordered = list.mapIndexed { idx, act -> act.copy(orderIndex = idx) }
            reordered.forEach { repository.updateActivity(it) }
        }
    }

    // ── Factory ───────────────────────────────────────────────────────
    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(
                modelClass: Class<T>,
                extras: androidx.lifecycle.viewmodel.CreationExtras
            ): T {
                val application =
                    extras[androidx.lifecycle.viewmodel.MutableCreationExtras.APPLICATION_KEY]!!
                return ItineraryViewModel(application) as T
            }
        }
    }
}
