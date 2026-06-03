package com.example.mytrip.data.db.dao

import androidx.room.*
import com.example.mytrip.data.db.entities.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE tripId = :tripId ORDER BY timestamp DESC")
    fun getNotesForTrip(tripId: Long): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE dayId = :dayId ORDER BY timestamp DESC")
    fun getNotesForDay(dayId: Long): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE tripId = :tripId ORDER BY timestamp DESC")
    suspend fun getNotesForTripOnce(tripId: Long): List<Note>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)
}
