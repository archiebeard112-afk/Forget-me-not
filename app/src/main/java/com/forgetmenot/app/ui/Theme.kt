package com.forgetmenot.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Brand colours, taken from the app icon: navy, blue petals, gold check.
val Urgent = Color(0xFFE5575A)
val Warning = Color(0xFFE8A317)
val Gold = Color(0xFFF2C14E)
val Periwinkle = Color(0xFF8FB0FF)

/** The main action button's fill, same in light and dark. */
val PrimaryButtonBrush = Brush.verticalGradient(listOf(Color(0xFF5B86F5), Color(0xFF3A5FD8)))

private val Dark = darkColorScheme(
    primary = Periwinkle,
    onPrimary = Color(0xFF0E1630),
    primaryContainer = Color(0xFF2A3D7A),
    onPrimaryContainer = Color(0xFFDCE6FF),
    secondary = Gold,
    onSecondary = Color(0xFF2A1E00),
    secondaryContainer = Color(0xFF2B3354),
    onSecondaryContainer = Color(0xFFE9EDFA),
    background = Color(0xFF0E1630),
    onBackground = Color(0xFFEEF2FF),
    surface = Color(0xFF0E1630),
    onSurface = Color(0xFFEEF2FF),
    surfaceVariant = Color(0xFF1D2849),
    onSurfaceVariant = Color(0xFF98A4C6),
    surfaceContainerLowest = Color(0xFF0A1128),
    surfaceContainerLow = Color(0xFF121B36),
    surfaceContainer = Color(0xFF151E3B),
    surfaceContainerHigh = Color(0xFF1D2849),
    surfaceContainerHighest = Color(0xFF243159),
    outline = Color(0xFF3A4773),
    outlineVariant = Color(0xFF2A3660),
    error = Color(0xFFF07373),
)

private val Light = lightColorScheme(
    primary = Color(0xFF3A5FD8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE6FF),
    onPrimaryContainer = Color(0xFF0E1A45),
    secondary = Color(0xFFB98400),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8EDFB),
    onSecondaryContainer = Color(0xFF121A33),
    background = Color(0xFFF5F7FD),
    onBackground = Color(0xFF121A33),
    surface = Color(0xFFF5F7FD),
    onSurface = Color(0xFF121A33),
    surfaceVariant = Color(0xFFEEF2FB),
    onSurfaceVariant = Color(0xFF5C6787),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFAFBFE),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFEEF2FB),
    surfaceContainerHighest = Color(0xFFE5EAF7),
    outline = Color(0xFFC3CCE4),
    outlineVariant = Color(0xFFD5DCEE),
    error = Color(0xFFD64545),
)

@Composable
fun ForgetMeNotTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
