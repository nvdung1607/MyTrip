package com.example.mytrip.ui.screens.trip

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mytrip.MyTripApplication
import com.example.mytrip.data.db.entities.Trip
import com.example.mytrip.data.db.entities.TripStatus
import com.example.mytrip.data.repository.TripRepository
import com.example.mytrip.util.CsvImportUtils
import com.example.mytrip.widget.MyTripWidgetUpdater
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

    // CSV import state
    private val _importedTripId = MutableStateFlow<Long?>(null)
    val importedTripId: StateFlow<Long?> = _importedTripId.asStateFlow()

    private val _importError = MutableStateFlow<String?>(null)
    val importError: StateFlow<String?> = _importError.asStateFlow()

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
        val tripToSave = if (trip.themeColor.isEmpty()) trip.copy(themeColor = com.example.mytrip.ui.theme.TripThemeColors.getRandomColor()) else trip
        val tripId = if (tripToSave.id == 0L) {
            repository.createTrip(tripToSave)
        } else {
            repository.updateTrip(tripToSave)
            tripToSave.id
        }
        MyTripWidgetUpdater.update(getApplication())
        return tripId
    }

    fun deleteTrip(trip: Trip) {
        viewModelScope.launch {
            try {
                repository.deleteTrip(trip)
                MyTripWidgetUpdater.update(getApplication())
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
                MyTripWidgetUpdater.update(getApplication())
            } catch (e: Exception) {
                _uiState.value = TripUiState.Error(e.message ?: "Không thể cập nhật trạng thái")
            }
        }
    }

    /** Import trip from a user-picked Excel file URI */
    fun importFromExcelUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = TripUiState.Loading
            when (val result = com.example.mytrip.util.ExcelImportUtils.parseFromUri(context, uri)) {
                is com.example.mytrip.util.ExcelImportUtils.ImportResult.Error -> {
                    _importError.value = result.message
                    _uiState.value = TripUiState.Success
                }
                is com.example.mytrip.util.ExcelImportUtils.ImportResult.Success -> {
                    try {
                        val tripId = repository.importFromExcel(result.data)
                        _importedTripId.value = tripId
                        _uiState.value = TripUiState.Success
                    } catch (e: Exception) {
                        _importError.value = "Lỗi lưu dữ liệu: ${e.message}"
                        _uiState.value = TripUiState.Success
                    }
                }
            }
        }
    }

    /** Import sample trip */
    fun importSampleTrip() {
        viewModelScope.launch {
            _uiState.value = TripUiState.Loading
            try {
                // To trigger navigation after import, we can fetch the last inserted ID.
                // Or simply we can get the latest trip. But repository.importSeedTrip doesn't return ID.
                val tripId = repository.importSeedTrip()
                _importedTripId.value = tripId
                _uiState.value = TripUiState.Success
            } catch (e: Exception) {
                _importError.value = "Lỗi lưu dữ liệu mẫu: ${e.message}"
                _uiState.value = TripUiState.Success
            }
        }
    }

    /** Save the template Excel to Downloads folder using ExcelUtils */
    fun downloadTemplateExcel(context: Context): String? {
        return try {
            val trip = com.example.mytrip.data.seed.TripSeedData.trip.copy(name = "Lịch trình mẫu")
            val days = com.example.mytrip.data.seed.TripSeedData.days.map { daySeed ->
                com.example.mytrip.data.db.entities.Day(
                    id = 0L,
                    tripId = trip.id,
                    dayNumber = daySeed.dayNumber,
                    date = trip.startDate + (daySeed.dayNumber - 1) * 86_400_000L,
                    title = daySeed.title
                )
            }
            val uri = com.example.mytrip.util.ExcelUtils.exportTripToExcel(
                context = context,
                trip = trip,
                days = days,
                activitiesMap = emptyMap(),
                expenses = emptyList(),
                records = emptyList(),
                notes = emptyList(),
                memberNames = listOf("Tôi")
            )
            uri.lastPathSegment
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun clearImportError() { _importError.value = null }
}
