package com.example.mytrip.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.mytrip.data.db.entities.Trip

@Composable
fun TripThemeProvider(
    trip: Trip?,
    content: @Composable () -> Unit
) {
    if (trip == null || trip.themeColor.isBlank()) {
        content()
        return
    }

    val parsedColor = try {
        Color(android.graphics.Color.parseColor(trip.themeColor))
    } catch (e: Exception) {
        null
    }

    if (parsedColor != null) {
        val customColorScheme = MaterialTheme.colorScheme.copy(
            primary = parsedColor,
            primaryContainer = parsedColor.copy(alpha = 0.2f),
            onPrimaryContainer = parsedColor
        )
        MaterialTheme(
            colorScheme = customColorScheme,
            typography = MaterialTheme.typography,
            content = content
        )
    } else {
        content()
    }
}
