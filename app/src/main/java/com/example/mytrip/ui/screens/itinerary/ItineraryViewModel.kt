package com.example.mytrip.ui.screens.itinerary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mytrip.MyTripApplication
import com.example.mytrip.data.db.entities.Activity
import com.example.mytrip.data.db.entities.ActivityStatus
import com.example.mytrip.data.db.entities.Day
import com.example.mytrip.data.db.entities.Cluster
import com.example.mytrip.data.db.entities.Trip
import com.example.mytrip.data.repository.TripRepository
import com.example.mytrip.widget.MyTripWidgetUpdater
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
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

    val clusters: StateFlow<List<Cluster>> = _tripId.flatMapLatest { id ->
        if (id != null) repository.getClusters(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Map of dayId -> activities list, rebuilt whenever days or any activity list changes
    private val _activitiesMap = MutableStateFlow<Map<Long, List<Activity>>>(emptyMap())
    val activitiesMap: StateFlow<Map<Long, List<Activity>>> = _activitiesMap.asStateFlow()

    val expandedDays = MutableStateFlow<Set<Long>>(emptySet())
    val expandedClusters = MutableStateFlow<Set<Long>>(emptySet())

    // ── Snackbar events ───────────────────────────────────────────────
    val snackbarEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)

    // ── Load data ─────────────────────────────────────────────────────────────
    private var loadJob: Job? = null
    private var currentActivitiesJob: Job? = null

    fun loadData(tripId: Long) {
        _tripId.value = tripId
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            // Initialize expanded clusters once
            launch {
                clusters.collect { clusterList ->
                    if (clusterList.isNotEmpty() && expandedClusters.value.isEmpty()) {
                        expandedClusters.value = clusterList.map { it.id }.toSet()
                    }
                }
            }
            // Initialize expanded days on first load
            launch {
                days.collect { dayList ->
                    if (dayList.isEmpty()) return@collect
                    if (expandedDays.value.isEmpty()) {
                        expandedDays.value = dayList.map { it.id }.toSet()
                    }
                }
            }
            // Observe days and reactively combine all activity flows
            days.collect { dayList ->
                if (dayList.isEmpty()) {
                    _activitiesMap.value = emptyMap()
                    return@collect
                }
                // Build a combined flow that maps each dayId to its activities
                val flows = dayList.map { day ->
                    repository.getActivities(day.id).map { activities -> day.id to activities }
                }
                // Cancel any previous activity observation
                currentActivitiesJob?.cancel()
                currentActivitiesJob = launch {
                    combine(flows) { pairs ->
                        pairs.toMap()
                    }.collect { map ->
                        _activitiesMap.value = map
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
            MyTripWidgetUpdater.update(getApplication())
        }
    }

    /**
     * Insert activity after a specific index in the day's list.
     * Activities after insertAfterIndex get their orderIndex bumped up.
     */
    fun insertActivityAfter(activity: Activity, insertAfterIndex: Int) {
        viewModelScope.launch {
            val dayActivities = (_activitiesMap.value[activity.dayId] ?: emptyList())
                .sortedBy { it.orderIndex }
                .toMutableList()

            val newIndex = (insertAfterIndex + 1).coerceIn(0, dayActivities.size)
            // Shift existing activities
            val toUpdate = dayActivities.mapIndexed { idx, act ->
                if (idx >= newIndex) act.copy(orderIndex = idx + 1) else act
            }
            toUpdate.forEach { repository.updateActivity(it) }
            // Insert new one
            repository.insertActivity(activity.copy(orderIndex = newIndex))
            MyTripWidgetUpdater.update(getApplication())
        }
    }

    fun updateActivity(activity: Activity) {
        viewModelScope.launch {
            repository.updateActivity(activity)
            MyTripWidgetUpdater.update(getApplication())
        }
    }

    fun deleteActivity(activity: Activity) {
        viewModelScope.launch {
            repository.deleteActivity(activity)
            MyTripWidgetUpdater.update(getApplication())
        }
    }

    fun updateActivityStatus(id: Long, status: ActivityStatus) {
        viewModelScope.launch {
            repository.updateActivityStatus(id, status)
            MyTripWidgetUpdater.update(getApplication())
        }
    }

    fun reorderActivities(dayId: Long, activities: List<Activity>) {
        viewModelScope.launch {
            val reordered = activities.mapIndexed { idx, act -> act.copy(orderIndex = idx) }
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                reordered.forEach { repository.updateActivity(it) }
            }
            // Refresh activities map immediately
            val current = _activitiesMap.value.toMutableMap()
            current[dayId] = reordered
            _activitiesMap.value = current
            MyTripWidgetUpdater.update(getApplication())
            snackbarEvent.tryEmit("Đã sắp xếp lại! Vui lòng kiểm tra lại giờ giấc của các hoạt động.")
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
                    extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                return ItineraryViewModel(application) as T
            }
        }
    }
}
