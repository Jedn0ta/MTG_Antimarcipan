package com.example.antimarcipan.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta inspirovaná noční fantasy krajinou — hluboká indigová, fialová, zlatá.
private val Gold = Color(0xFFE9C46A)
private val Night = Color(0xFF0E0B1A)        // skoro černé indigo (pozadí)
private val Surface = Color(0xFF1A1630)       // fialově laděný povrch karet
private val SurfaceHi = Color(0xFF241D44)     // zvýrazněný povrch
private val Lavender = Color(0xFFB9A7EC)      // světle fialová (akcenty)
private val Violet = Color(0xFF7C5CD6)

private val DarkColors = darkColorScheme(
    primary = Gold,
    onPrimary = Night,
    primaryContainer = Violet,
    onPrimaryContainer = Color(0xFFF3EEFF),
    secondary = Lavender,
    onSecondary = Night,
    secondaryContainer = SurfaceHi,
    onSecondaryContainer = Color(0xFFECE6FF),
    tertiary = Color(0xFF8FB3FF),
    background = Night,
    onBackground = Color(0xFFECECF5),
    surface = Surface,
    onSurface = Color(0xFFECECF5),
    surfaceVariant = SurfaceHi,
    onSurfaceVariant = Color(0xFFC7BEE6),
    outline = Color(0xFF4A3F6E),
    error = Color(0xFFF2777A),
)

@Composable
fun AntimarcipanTheme(
    content: @Composable () -> Unit,
) {
    // Appka má jen tmavý režim — odpovídá noční fantasy estetice.
    MaterialTheme(
        colorScheme = DarkColors,
        content = content,
    )
}
