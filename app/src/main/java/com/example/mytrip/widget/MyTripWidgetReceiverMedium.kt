package com.example.mytrip.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/** Receiver for the 4×2 Medium widget entry in the widget picker. */
class MyTripWidgetReceiverMedium : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MyTripWidget()
}
