package com.example.mytrip.ui.screens.photoexport

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mytrip.MyTripApplication
import com.example.mytrip.data.db.entities.Day
import com.example.mytrip.data.db.entities.Note
import com.example.mytrip.data.db.entities.Trip
import com.example.mytrip.data.repository.TripRepository
import com.example.mytrip.util.PhotoExportUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

data class PhotoGroup(
    val day: Day,
    val note: Note,
    val photos: List<String>
)

class PhotoExportViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TripRepository = (application as MyTripApplication).repository

    private val _trip = MutableStateFlow<Trip?>(null)
    val trip: StateFlow<Trip?> = _trip.asStateFlow()

    private val _photoGroups = MutableStateFlow<List<PhotoGroup>>(emptyList())
    val photoGroups: StateFlow<List<PhotoGroup>> = _photoGroups.asStateFlow()

    private val _selectedPhotos = MutableStateFlow<Set<String>>(emptySet())
    val selectedPhotos: StateFlow<Set<String>> = _selectedPhotos.asStateFlow()

    private val _includeWatermark = MutableStateFlow(true)
    val includeWatermark: StateFlow<Boolean> = _includeWatermark.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    fun loadData(tripId: Long, selectAllByDefault: Boolean = true) {
        viewModelScope.launch {
            combine(
                repository.getTripById(tripId),
                repository.getDays(tripId),
                repository.getNotes(tripId)
            ) { t, days, allNotes ->
                _trip.value = t
                if (t != null) {
                    val groups = mutableListOf<PhotoGroup>()
                    val allPhotoPaths = mutableSetOf<String>()

                    val daysMap = days.associateBy { it.id }

                    for (note in allNotes) {
                        val notePhotos = if (note.photoPaths.isNotEmpty()) note.photoPaths else if (note.photoPath != null) listOf(note.photoPath!!) else emptyList()
                        val validPhotos = notePhotos.filter { File(it).exists() }

                        if (validPhotos.isNotEmpty()) {
                            val day = note.dayId?.let { daysMap[it] } ?: continue
                            groups.add(PhotoGroup(day, note, validPhotos))
                            allPhotoPaths.addAll(validPhotos)
                        }
                    }

                    // Sort by day number then by note timestamp
                    groups.sortBy { it.day.dayNumber * 10000000L + it.note.timestamp }
                    
                    _photoGroups.value = groups
                    
                    if (selectAllByDefault && _selectedPhotos.value.isEmpty()) {
                        _selectedPhotos.value = allPhotoPaths
                    }
                }
            }.collect()
        }
    }

    fun togglePhoto(path: String) {
        val current = _selectedPhotos.value.toMutableSet()
        if (current.contains(path)) {
            current.remove(path)
        } else {
            current.add(path)
        }
        _selectedPhotos.value = current
    }

    fun toggleGroup(group: PhotoGroup) {
        val current = _selectedPhotos.value.toMutableSet()
        val allSelected = group.photos.all { current.contains(it) }
        
        if (allSelected) {
            current.removeAll(group.photos.toSet())
        } else {
            current.addAll(group.photos)
        }
        _selectedPhotos.value = current
    }

    fun toggleWatermark(enabled: Boolean) {
        _includeWatermark.value = enabled
    }

    fun exportPhotos(context: Context, onComplete: (Int) -> Unit) {
        val t = _trip.value ?: return
        val currentSelection = _selectedPhotos.value
        val watermark = _includeWatermark.value
        
        val selectedPairs = mutableListOf<Pair<Note, String>>()
        for (group in _photoGroups.value) {
            for (photo in group.photos) {
                if (currentSelection.contains(photo)) {
                    selectedPairs.add(Pair(group.note, photo))
                }
            }
        }

        if (selectedPairs.isEmpty()) {
            onComplete(0)
            return
        }

        viewModelScope.launch {
            _isExporting.value = true
            val count = PhotoExportUtils.exportSelectedPhotos(context, t.name, selectedPairs, watermark)
            _isExporting.value = false
            onComplete(count)
        }
    }

    companion object {
        fun factory(app: Application) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(c: Class<T>) = PhotoExportViewModel(app) as T
        }
    }
}
