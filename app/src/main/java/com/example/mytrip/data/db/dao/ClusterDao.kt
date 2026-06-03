package com.example.mytrip.data.db.dao

import androidx.room.*
import com.example.mytrip.data.db.entities.Cluster
import kotlinx.coroutines.flow.Flow

@Dao
interface ClusterDao {
    @Query("SELECT * FROM clusters WHERE tripId = :tripId ORDER BY orderIndex ASC")
    fun getClustersForTrip(tripId: Long): Flow<List<Cluster>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCluster(cluster: Cluster): Long

    @Update
    suspend fun updateCluster(cluster: Cluster)

    @Delete
    suspend fun deleteCluster(cluster: Cluster)

    @Query("DELETE FROM clusters WHERE tripId = :tripId")
    suspend fun deleteAllClustersForTrip(tripId: Long)
}
