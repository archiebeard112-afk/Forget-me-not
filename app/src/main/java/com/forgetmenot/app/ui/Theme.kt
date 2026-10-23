package com.forgetmenot.app.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val Urgent = Color(0xFFE5484D)
val Warning = Color(0xFFE8A317)

private val Light = lightColorScheme(
    primary = Color(0xFF3B6FD4),
    secondary = Color(0xFFF5C542),
)
private val Dark = darkColorScheme(
    primary = Color(0xFF9DB9F5),
    secondary = Color(0xFFF5C542),
)

@Composable
fun ForgetMeNotTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> Dark
        else -> Light
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
