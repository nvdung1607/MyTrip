package com.example.mytrip.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.mytrip.data.db.dao.*
import com.example.mytrip.data.db.entities.*

@Database(
    entities = [
        Trip::class,
        Cluster::class,
        Day::class,
        Activity::class,
        Note::class,
        Expense::class,
        ExpenseRecord::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun clusterDao(): ClusterDao
    abstract fun dayDao(): DayDao
    abstract fun activityDao(): ActivityDao
    abstract fun noteDao(): NoteDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mytrip.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
