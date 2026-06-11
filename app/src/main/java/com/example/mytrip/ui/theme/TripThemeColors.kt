package com.example.mytrip.ui.theme

object TripThemeColors {
    val vibrantColors = listOf(
        "#0D47A1", // Navy Blue
        "#1976D2", // Primary Blue
        "#1E88E5", // Bright Blue
        "#039BE5", // Light Blue Accent
        "#00ACC1", // Cyan
        "#0097A7", // Deep Cyan
        "#00897B", // Teal
        "#00796B", // Deep Teal
        "#3949AB", // Indigo
        "#5C6BC0"  // Soft Indigo
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
