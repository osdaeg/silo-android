package com.daniel.silo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Primary    = Color(0xFF1A73E8)
private val OnPrimary  = Color(0xFFFFFFFF)
private val Secondary  = Color(0xFF4CAF50)

private val LightColors = lightColorScheme(
    primary         = Primary,
    onPrimary       = OnPrimary,
    secondary       = Secondary,
    background      = Color(0xFFF6F8FA),
    surface         = Color(0xFFFFFFFF),
    onBackground    = Color(0xFF1C1B1F),
    onSurface       = Color(0xFF1C1B1F),
)

private val DarkColors = darkColorScheme(
    primary         = Color(0xFF90CAF9),
    onPrimary       = Color(0xFF003258),
    secondary       = Color(0xFF81C784),
    background      = Color(0xFF1C1B1F),
    surface         = Color(0xFF2B2B2B),
    onBackground    = Color(0xFFE6E1E5),
    onSurface       = Color(0xFFE6E1E5),
)

@Composable
fun SiloTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography(),
        content = content
    )
}
