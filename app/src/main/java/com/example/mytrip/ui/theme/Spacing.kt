package com.example.mytrip.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class Spacing(
    val unit: Dp = 4.dp,
    val gutterSm: Dp = 16.dp,
    val marginMobile: Dp = 20.dp,
    val marginDesktop: Dp = 64.dp,
    val widgetPadding: Dp = 16.dp,
    val widgetGap: Dp = 12.dp,
    
    // Additional helpful spaces
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp
)

val LocalSpacing = compositionLocalOf { Spacing() }

val androidx.compose.material3.MaterialTheme.spacing: Spacing
    @androidx.compose.runtime.Composable
    @androidx.compose.runtime.ReadOnlyComposable
    get() = LocalSpacing.current
