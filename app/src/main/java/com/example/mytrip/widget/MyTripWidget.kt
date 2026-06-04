package com.example.mytrip.widget

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import com.example.mytrip.widget.ui.EmptyWidget
import com.example.mytrip.widget.ui.LargeWidget
import com.example.mytrip.widget.ui.MediumWidget
import com.example.mytrip.widget.ui.SmallWidget

/**
 * Main GlanceAppWidget for MyTrip.
 * Uses [SizeMode.Responsive] to switch between Small, Medium, and Large
 * layouts at runtime based on the actual widget size on the home screen.
 */
class MyTripWidget : GlanceAppWidget() {

    companion object {
        private val SMALL  = DpSize(110.dp, 110.dp)
        private val MEDIUM = DpSize(220.dp, 110.dp)
        private val LARGE  = DpSize(220.dp, 220.dp)
    }

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM, LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Fetch all data up-front on a background coroutine (Glance handles this)
        val state = WidgetRepository(context).buildWidgetState()

        provideContent {
            if (!state.hasActiveTrip) {
                EmptyWidget()
                return@provideContent
            }

            val size = LocalSize.current
            when {
                size.width  >= LARGE.width  && size.height >= LARGE.height  -> LargeWidget(state)
                size.width  >= MEDIUM.width                                  -> MediumWidget(state)
                else                                                          -> SmallWidget(state)
            }
        }
    }
}
