package com.example.mytrip.ui.screens.notes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mytrip.MyTripApplication
import com.example.mytrip.data.db.entities.Note
import com.example.mytrip.widget.MyTripWidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as MyTripApplication).repository

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _savedNoteId = MutableStateFlow<Long?>(null)
    val savedNoteId: StateFlow<Long?> = _savedNoteId.asStateFlow()

    private val _noteToEdit = MutableStateFlow<Note?>(null)
    val noteToEdit: StateFlow<Note?> = _noteToEdit.asStateFlow()

    fun loadNote(noteId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _noteToEdit.value = repository.getNoteById(noteId)
            _isLoading.value = false
        }
    }

    fun saveNote(note: Note) {
        viewModelScope.launch {
            _isLoading.value = true
            val id = if (note.id == 0L) {
                repository.insertNote(note)
            } else {
                repository.updateNote(note)
                note.id
            }
            MyTripWidgetUpdater.update(getApplication())
            _savedNoteId.value = id
            _isLoading.value = false
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
            MyTripWidgetUpdater.update(getApplication())
        }
    }

    fun resetSavedNoteId() { _savedNoteId.value = null }

    companion object {
        fun factory(app: Application) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(c: Class<T>) = NoteViewModel(app) as T
        }
    }
}
