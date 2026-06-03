package com.example.mytrip.data.db.dao

import androidx.room.*
import com.example.mytrip.data.db.entities.Expense
import com.example.mytrip.data.db.entities.ExpenseRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    // Ngân sách dự kiến
    @Query("SELECT * FROM expenses WHERE tripId = :tripId")
    fun getExpensesForTrip(tripId: Long): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE tripId = :tripId")
    suspend fun getExpensesForTripOnce(tripId: Long): List<Expense>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<Expense>)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("DELETE FROM expenses WHERE tripId = :tripId")
    suspend fun deleteAllForTrip(tripId: Long)

    // Chi tiêu thực tế
    @Query("SELECT * FROM expense_records WHERE tripId = :tripId ORDER BY timestamp DESC")
    fun getRecordsForTrip(tripId: Long): Flow<List<ExpenseRecord>>

    @Query("SELECT * FROM expense_records WHERE tripId = :tripId ORDER BY timestamp DESC")
    suspend fun getRecordsForTripOnce(tripId: Long): List<ExpenseRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: ExpenseRecord): Long

    @Update
    suspend fun updateRecord(record: ExpenseRecord)

    @Delete
    suspend fun deleteRecord(record: ExpenseRecord)

    @Query("SELECT * FROM expense_records WHERE noteId = :noteId LIMIT 1")
    suspend fun getRecordByNoteId(noteId: Long): ExpenseRecord?

    @Query("DELETE FROM expense_records WHERE noteId = :noteId")
    suspend fun deleteRecordByNoteId(noteId: Long)

    @Query("SELECT SUM(amount) FROM expense_records WHERE tripId = :tripId")
    fun getTotalActual(tripId: Long): Flow<Long?>

    @Query("SELECT SUM(planned) FROM expenses WHERE tripId = :tripId")
    fun getTotalPlanned(tripId: Long): Flow<Long?>
}
