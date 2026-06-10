package com.example.mytrip.ui.theme

object TripThemeColors {
    val pastelColors = listOf(
        "#4D6360", // Default (Modern Vietnamese Travel System)
        "#6D8994", // Pastel Blue-Grey
        "#89946D", // Pastel Green
        "#947B6D", // Pastel Brown/Orange
        "#7B6D94", // Pastel Purple
        "#946D77", // Pastel Pink
        "#6D9489"  // Pastel Teal
    )

    fun getRandomColor(): String {
        return pastelColors.random()
    }
}
