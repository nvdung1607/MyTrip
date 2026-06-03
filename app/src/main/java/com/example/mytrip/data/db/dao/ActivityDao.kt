package com.example.mytrip.data.db.dao

import androidx.room.*
import com.example.mytrip.data.db.entities.Activity
import com.example.mytrip.data.db.entities.ActivityStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities WHERE dayId = :dayId ORDER BY orderIndex ASC")
    fun getActivitiesForDay(dayId: Long): Flow<List<Activity>>

    @Query("SELECT * FROM activities WHERE dayId = :dayId ORDER BY orderIndex ASC")
    suspend fun getActivitiesForDayOnce(dayId: Long): List<Activity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: Activity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivities(activities: List<Activity>)

    @Update
    suspend fun updateActivity(activity: Activity)

    @Delete
    suspend fun deleteActivity(activity: Activity)

    @Query("UPDATE activities SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: ActivityStatus)

    @Query("DELETE FROM activities WHERE dayId = :dayId")
    suspend fun deleteAllForDay(dayId: Long)
}
