package com.example.antimarcipan.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Gold = Color(0xFFE9C46A)
private val Night = Color(0xFF1B1B2F)
private val Plum = Color(0xFF3A2E5C)

private val DarkColors = darkColorScheme(
    primary = Gold,
    onPrimary = Night,
    secondary = Color(0xFF9D86D6),
    background = Night,
    surface = Color(0xFF232343),
    onSurface = Color(0xFFECECF5),
)

private val LightColors = lightColorScheme(
    primary = Plum,
    onPrimary = Color.White,
    secondary = Color(0xFF7A5C2E),
    background = Color(0xFFFAF7F0),
    surface = Color(0xFFFFFFFF),
)

@Composable
fun AntimarcipanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
