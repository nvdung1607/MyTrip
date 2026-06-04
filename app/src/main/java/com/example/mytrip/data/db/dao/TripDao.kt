package com.example.mytrip.data.db.dao

import androidx.room.*
import com.example.mytrip.data.db.entities.Trip
import com.example.mytrip.data.db.entities.TripStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Query("SELECT * FROM trips ORDER BY startDate DESC")
    fun getAllTrips(): Flow<List<Trip>>

    @Query("SELECT * FROM trips WHERE status = :status ORDER BY startDate DESC")
    fun getTripsByStatus(status: TripStatus): Flow<List<Trip>>

    @Query("SELECT * FROM trips WHERE status = :status ORDER BY startDate ASC")
    suspend fun getTripsByStatusOnce(status: TripStatus): List<Trip>

    @Query("SELECT * FROM trips WHERE id = :id")
    fun getTripById(id: Long): Flow<Trip?>

    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun getTripByIdOnce(id: Long): Trip?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: Trip): Long

    @Update
    suspend fun updateTrip(trip: Trip)

    @Delete
    suspend fun deleteTrip(trip: Trip)

    @Query("UPDATE trips SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: TripStatus)

    @Query("UPDATE trips SET status = :newStatus WHERE status = :currentStatus AND id != :exceptId")
    suspend fun updateTripsWithStatusExcept(
        currentStatus: TripStatus,
        newStatus: TripStatus,
        exceptId: Long
    )

    @Transaction
    suspend fun insertTripEnforcingSingleOngoing(trip: Trip): Long {
        if (trip.status == TripStatus.ONGOING) {
            updateTripsWithStatusExcept(
                currentStatus = TripStatus.ONGOING,
                newStatus = TripStatus.PLANNING,
                exceptId = -1L
            )
        }
        return insertTrip(trip)
    }

    @Transaction
    suspend fun updateTripEnforcingSingleOngoing(trip: Trip) {
        if (trip.status == TripStatus.ONGOING) {
            updateTripsWithStatusExcept(
                currentStatus = TripStatus.ONGOING,
                newStatus = TripStatus.PLANNING,
                exceptId = trip.id
            )
        }
        updateTrip(trip)
    }

    @Transaction
    suspend fun updateStatusEnforcingSingleOngoing(id: Long, status: TripStatus) {
        if (status == TripStatus.ONGOING) {
            updateTripsWithStatusExcept(
                currentStatus = TripStatus.ONGOING,
                newStatus = TripStatus.PLANNING,
                exceptId = id
            )
        }
        updateStatus(id, status)
    }
}
