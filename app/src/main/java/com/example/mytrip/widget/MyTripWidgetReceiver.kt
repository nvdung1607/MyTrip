package com.example.mytrip.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * BroadcastReceiver entry point for the MyTrip home screen widget.
 * Registered in AndroidManifest.xml with APPWIDGET_UPDATE intent filter.
 */
class MyTripWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MyTripWidget()
}
