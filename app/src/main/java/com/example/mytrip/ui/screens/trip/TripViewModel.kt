package com.example.mytrip.ui.screens.trip

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mytrip.MyTripApplication
import com.example.mytrip.data.db.entities.Trip
import com.example.mytrip.data.db.entities.TripStatus
import com.example.mytrip.data.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed class TripUiState {
    object Loading : TripUiState()
    object Success : TripUiState()
    data class Error(val message: String) : TripUiState()
}

class TripViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TripRepository =
        (application as MyTripApplication).repository

    private val _trip = MutableStateFlow<Trip?>(null)
    val trip: StateFlow<Trip?> = _trip.asStateFlow()

    private val _uiState = MutableStateFlow<TripUiState>(TripUiState.Loading)
    val uiState: StateFlow<TripUiState> = _uiState.asStateFlow()

    private val _totalPlanned = MutableStateFlow(0L)
    val totalPlanned: StateFlow<Long> = _totalPlanned.asStateFlow()

    fun loadTrip(id: Long) {
        viewModelScope.launch {
            _uiState.value = TripUiState.Loading
            repository.getTripById(id)
                .catch { e ->
                    _uiState.value = TripUiState.Error(e.message ?: "Không thể tải chuyến đi")
                }
                .collectLatest { tripData ->
                    _trip.value = tripData
                    _uiState.value = TripUiState.Success
                }
        }
        viewModelScope.launch {
            repository.getTotalPlanned(id)
                .catch { /* ignore */ }
                .collectLatest { total ->
                    _totalPlanned.value = total ?: 0L
                }
        }
    }

    /**
     * Save trip: if trip.id == 0 → create (returns new id), else → update (returns same id).
     */
    suspend fun saveTrip(trip: Trip): Long {
        return if (trip.id == 0L) {
            repository.createTrip(trip)
        } else {
            repository.updateTrip(trip)
            trip.id
        }
    }

    fun deleteTrip(trip: Trip) {
        viewModelScope.launch {
            try {
                repository.deleteTrip(trip)
                _uiState.value = TripUiState.Success
            } catch (e: Exception) {
                _uiState.value = TripUiState.Error(e.message ?: "Không thể xóa chuyến đi")
            }
        }
    }

    fun updateStatus(id: Long, status: TripStatus) {
        viewModelScope.launch {
            try {
                repository.updateTripStatus(id, status)
            } catch (e: Exception) {
                _uiState.value = TripUiState.Error(e.message ?: "Không thể cập nhật trạng thái")
            }
        }
    }
}
