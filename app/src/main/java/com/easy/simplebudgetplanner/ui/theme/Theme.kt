package com.easy.simplebudgetplanner.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = DuneSage,
    onPrimary = DuneMidnight,
    secondary = DuneClay,
    onSecondary = DuneMidnight,
    tertiary = DuneSunset,
    onTertiary = DuneMidnight,
    background = DuneMidnight,
    onBackground = DuneMist,
    surface = DuneSlate,
    onSurface = DuneMist,
    surfaceVariant = Color(0xFF0E3641),
    onSurfaceVariant = DuneMist.copy(alpha = 0.85f)
)

private val LightColorScheme = lightColorScheme(
    primary = DuneSunset,
    onPrimary = DuneMidnight,
    secondary = DuneSage,
    onSecondary = Color.White,
    tertiary = DuneClay,
    onTertiary = Color.White,
    background = DuneSand,
    onBackground = DuneMidnight,
    surface = Color.White,
    onSurface = DuneMidnight,
    surfaceVariant = DuneMist,
    onSurfaceVariant = DuneSlate
)

@Composable
fun SimpleBudgetPlannerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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
        typography = Typography,
        content = content
    )
}