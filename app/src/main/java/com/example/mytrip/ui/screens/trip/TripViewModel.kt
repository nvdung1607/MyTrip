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
        val tripId = if (trip.id == 0L) {
            repository.createTrip(trip)
        } else {
            repository.updateTrip(trip)
            trip.id
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

    /** Import trip from a user-picked CSV file URI */
    fun importFromCsvUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = TripUiState.Loading
            when (val result = CsvImportUtils.parseFromUri(context, uri)) {
                is CsvImportUtils.ImportResult.Error -> {
                    _importError.value = result.message
                    _uiState.value = TripUiState.Success
                }
                is CsvImportUtils.ImportResult.Success -> {
                    try {
                        val tripId = repository.importFromCsv(result.data)
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

    /** Save the template CSV to Downloads folder so user can open/edit it */
    fun downloadTemplateCsv(context: Context): String? {
        return try {
            val assetContent = context.assets.open("trip_template.csv").use { it.readBytes() }
            val fileName = "MyTrip_Mau_Lich_Trinh.csv"

            // Save to Downloads via MediaStore
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.Downloads.MIME_TYPE, "text/csv")
                put(android.provider.MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { it.write(assetContent) }
                values.clear()
                values.put(android.provider.MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                fileName
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun clearImportError() { _importError.value = null }
}
