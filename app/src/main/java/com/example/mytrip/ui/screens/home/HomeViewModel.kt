package com.example.mytrip.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mytrip.MyTripApplication
import com.example.mytrip.data.db.entities.Trip
import com.example.mytrip.data.db.entities.TripStatus
import com.example.mytrip.data.repository.TripRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TripRepository =
        (application as MyTripApplication).repository

    // Raw list from DB
    private val _allTripsRaw: StateFlow<List<Trip>> = repository.getAllTrips()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Active filter (null = show all)
    private val _filter = MutableStateFlow<TripStatus?>(null)
    val filter: StateFlow<TripStatus?> = _filter.asStateFlow()

    // Filtered trips exposed to UI
    val allTrips: StateFlow<List<Trip>> = combine(_allTripsRaw, _filter) { trips, status ->
        if (status == null) trips else trips.filter { it.status == status }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun filterByStatus(status: TripStatus?) {
        _filter.value = if (_filter.value == status) null else status
    }

    fun deleteTrip(trip: Trip) {
        viewModelScope.launch {
            repository.deleteTrip(trip)
        }
    }

    fun startTrip(tripId: Long) {
        viewModelScope.launch {
            repository.updateTripStatus(tripId, TripStatus.ONGOING)
        }
    }

    fun finishTrip(tripId: Long) {
        viewModelScope.launch {
            repository.updateTripStatus(tripId, TripStatus.DONE)
        }
    }

    fun importSampleTrip() {
        viewModelScope.launch {
            repository.importSeedTrip()
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
