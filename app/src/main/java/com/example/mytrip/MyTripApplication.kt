package com.example.mytrip

import android.app.Application
import com.example.mytrip.data.db.AppDatabase
import com.example.mytrip.data.repository.TripRepository

class MyTripApplication : Application() {
    val database by lazy { AppDatabase.getInstance(this) }
    val repository by lazy {
        TripRepository(
            context      = this,
            tripDao      = database.tripDao(),
            clusterDao   = database.clusterDao(),
            dayDao       = database.dayDao(),
            activityDao  = database.activityDao(),
            noteDao      = database.noteDao(),
            expenseDao   = database.expenseDao()
        )
    }
}
