package com.example.mytrip.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

object MyTripWidgetUpdater {
    suspend fun update(context: Context) {
        MyTripWidget().updateAll(context.applicationContext)
    }
}
