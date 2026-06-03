package com.example.mytrip.data.db.dao

import androidx.room.*
import com.example.mytrip.data.db.entities.Day
import kotlinx.coroutines.flow.Flow

@Dao
interface DayDao {
    @Query("SELECT * FROM days WHERE tripId = :tripId ORDER BY dayNumber ASC")
    fun getDaysForTrip(tripId: Long): Flow<List<Day>>

    @Query("SELECT * FROM days WHERE clusterId = :clusterId ORDER BY dayNumber ASC")
    fun getDaysForCluster(clusterId: Long): Flow<List<Day>>

    @Query("SELECT * FROM days WHERE id = :id")
    suspend fun getDayById(id: Long): Day?

    @Query("""
        SELECT * FROM days 
        WHERE tripId = :tripId 
        AND date BETWEEN :startOfDay AND :endOfDay
        LIMIT 1
    """)
    suspend fun getDayByDate(tripId: Long, startOfDay: Long, endOfDay: Long): Day?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDay(day: Day): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDays(days: List<Day>)

    @Update
    suspend fun updateDay(day: Day)

    @Delete
    suspend fun deleteDay(day: Day)

    @Query("DELETE FROM days WHERE tripId = :tripId")
    suspend fun deleteAllDaysForTrip(tripId: Long)
}
