package com.example.mytrip.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mytrip.MyTripApplication
import com.example.mytrip.data.db.entities.Trip
import com.example.mytrip.data.db.entities.TripStatus
import com.example.mytrip.data.repository.TripRepository
import com.example.mytrip.data.db.entities.TripType
import com.example.mytrip.widget.MyTripWidgetUpdater
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.onEach

/**
 * TripType cycle used to vary the gradient color on each sample import.
 * CAR → MOTORBIKE → PUBLIC → TREKKING → CAMPING → OTHER → CAR → …
 */
private val SEED_TYPE_CYCLE = TripType.values()

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TripRepository =
        (application as MyTripApplication).repository

    init {
        viewModelScope.launch {
            repository.autoUpdateTripStatuses()
        }
    }

    // One-shot event: emitted after a sample trip is imported so UI can switch to 'Sắp đi'
    private val _sampleImportedEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sampleImportedEvent: SharedFlow<Unit> = _sampleImportedEvent.asSharedFlow()

    // Active filter (null = show all)
    private val _filter = MutableStateFlow<TripStatus?>(null)
    val filter: StateFlow<TripStatus?> = _filter.asStateFlow()

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setSearchQuery(q: String) { _searchQuery.value = q }

    private var lastRawTripsSize: Int? = null

    // Raw list from DB
    private val _allTripsRaw: StateFlow<List<Trip>> = repository.getAllTrips()
        .onEach { trips ->
            val prev = lastRawTripsSize
            if (prev != null && trips.size > prev) {
                _filter.value = TripStatus.PLANNING
            }
            lastRawTripsSize = trips.size
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Filtered trips exposed to UI
    val allTrips: StateFlow<List<Trip>> = combine(_allTripsRaw, _filter, _searchQuery) { trips, status, query ->
        trips
            .filter { if (status == null) true else it.status == status }
            .filter { if (query.isBlank()) true else it.name.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun filterByStatus(status: TripStatus?) {
        _filter.value = if (_filter.value == status) null else status
    }

    /** Force-set the filter without toggling. Used by event handlers. */
    fun setFilter(status: TripStatus?) {
        _filter.value = status
    }

    fun deleteTrip(trip: Trip) {
        viewModelScope.launch {
            repository.deleteTrip(trip)
            MyTripWidgetUpdater.update(getApplication())
        }
    }

    fun startTrip(tripId: Long) {
        viewModelScope.launch {
            repository.updateTripStatus(tripId, TripStatus.ONGOING)
            MyTripWidgetUpdater.update(getApplication())
        }
    }

    fun finishTrip(tripId: Long) {
        viewModelScope.launch {
            repository.updateTripStatus(tripId, TripStatus.DONE)
            MyTripWidgetUpdater.update(getApplication())
        }
    }

    fun importSampleTrip() {
        viewModelScope.launch {
            // Pick TripType cycling on each press to change the gradient color
            val tripType = SEED_TYPE_CYCLE[_allTripsRaw.value.size % SEED_TYPE_CYCLE.size]
            repository.importSeedTrip(overrideType = tripType)
            // Signal UI to switch to 'Sắp đi' tab
            _sampleImportedEvent.tryEmit(Unit)
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
                return HomeViewModel(application) as T
            }
        }
    }
}
