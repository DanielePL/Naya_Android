package com.example.menotracker.ui.theme

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = NayaPrimary,
    background = NayaBackground,
    surface = NayaSurface,
    surfaceVariant = NayaSurfaceVariant,
    onPrimary = NayaOnPrimary,
    onBackground = NayaOnBackground,
    onSurface = NayaOnSurface
)

private val LightColorScheme = lightColorScheme(
    primary = NayaPrimary,
    background = NayaBackgroundLight,
    surface = NayaSurfaceLight,
    surfaceVariant = NayaSurfaceVariantLight,
    onPrimary = NayaOnPrimaryLight,
    onBackground = NayaOnBackgroundLight,
    onSurface = NayaOnSurfaceLight
)

@Composable
fun NayaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = NayaPrimary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NayaTypography,
        shapes = NayaShapes,
        content = content
    )
}

// Legacy Alias for compatibility
@Composable
fun MenoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) = NayaTheme(darkTheme, content)

// Fallback Gradient (used if image fails to load)
private val backgroundBrush = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF1A1625),  // Leichter Violet-Tint oben
        Color(0xFF141418),  // Background mit Violet-Hint
        Color(0xFF121214)   // Solid unten
    )
)

@Composable
fun AppBackground(content: @Composable () -> Unit) {
    val isDark = MaterialTheme.colorScheme.background == NayaBackground

    Box(modifier = Modifier.fillMaxSize()) {
        if (isDark) {
            // Use Compose gradient background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundBrush)
            )
        } else {
            // Light mode: solid color
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            )
        }
        content()
    }
}
