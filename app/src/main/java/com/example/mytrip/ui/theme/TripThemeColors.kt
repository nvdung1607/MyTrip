package com.example.mytrip.ui.theme

object TripThemeColors {
    val vibrantColors = listOf(
        "#E53935", // Red
        "#D81B60", // Pink
        "#8E24AA", // Purple
        "#3949AB", // Indigo
        "#1E88E5", // Blue
        "#00ACC1", // Cyan
        "#43A047", // Green
        "#F4511E", // Deep Orange
        "#FF8F00"  // Amber/Orange
    )

    fun getRandomColor(): String {
        return vibrantColors.random()
    }

    fun getThemeGradient(themeColorHex: String): androidx.compose.ui.graphics.Brush {
        if (themeColorHex.isBlank()) {
            return androidx.compose.ui.graphics.Brush.linearGradient(
                listOf(androidx.compose.ui.graphics.Color(0xFF1565C0), androidx.compose.ui.graphics.Color(0xFF42A5F5))
            )
        }
        return try {
            val baseColor = androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(themeColorHex))
            androidx.compose.ui.graphics.Brush.linearGradient(
                listOf(baseColor, baseColor.copy(alpha = 0.8f))
            )
        } catch (e: Exception) {
            androidx.compose.ui.graphics.Brush.linearGradient(
                listOf(androidx.compose.ui.graphics.Color(0xFF1565C0), androidx.compose.ui.graphics.Color(0xFF42A5F5))
            )
        }
    }
}
