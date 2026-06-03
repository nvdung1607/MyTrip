package com.example.mytrip.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary          = Primary80,
    onPrimary        = Primary20,
    primaryContainer = Primary30,
    onPrimaryContainer = Primary90,
    secondary        = Secondary80,
    onSecondary      = Secondary20,
    secondaryContainer = Secondary30,
    onSecondaryContainer = Secondary90,
    tertiary         = Tertiary80,
    onTertiary       = Tertiary40,
    tertiaryContainer = Color(0xFF57395C),
    onTertiaryContainer = Tertiary90,
    error            = Error80,
    onError          = Error40,
    errorContainer   = Color(0xFF93000A),
    onErrorContainer = Error90,
    background       = Neutral10,
    onBackground     = Neutral90,
    surface          = Neutral10,
    onSurface        = Neutral90,
    surfaceVariant   = NeutralVariant30,
    onSurfaceVariant = NeutralVariant80,
    outline          = NeutralVariant60,
)

private val LightColorScheme = lightColorScheme(
    primary          = Primary40,
    onPrimary        = Color.White,
    primaryContainer = Primary90,
    onPrimaryContainer = Primary10,
    secondary        = Secondary40,
    onSecondary      = Color.White,
    secondaryContainer = Secondary90,
    onSecondaryContainer = Secondary10,
    tertiary         = Tertiary40,
    onTertiary       = Color.White,
    tertiaryContainer = Tertiary90,
    onTertiaryContainer = Color(0xFF29132E),
    error            = Error40,
    onError          = Color.White,
    errorContainer   = Error90,
    onErrorContainer = Color(0xFF410002),
    background       = Neutral99,
    onBackground     = Neutral10,
    surface          = Neutral99,
    onSurface        = Neutral10,
    surfaceVariant   = NeutralVariant90,
    onSurfaceVariant = NeutralVariant30,
    outline          = NeutralVariant50,
)

@Composable
fun MyTripTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MyTripTypography,
        content = content
    )
}
