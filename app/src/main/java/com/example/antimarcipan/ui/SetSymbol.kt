package com.example.antimarcipan.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Symbol edice (set symbol) načtený ze Scryfallu jako SVG a obarvený na
 * barvu textu, aby byl vidět ve světlém i tmavém režimu.
 */
@Composable
fun SetSymbol(setCode: String?, size: Dp = 20.dp, modifier: Modifier = Modifier) {
    if (setCode.isNullOrBlank()) return
    AsyncImage(
        model = "https://svgs.scryfall.io/sets/${setCode.lowercase()}.svg",
        contentDescription = setCode.uppercase(),
        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
        modifier = modifier.size(size),
    )
}
