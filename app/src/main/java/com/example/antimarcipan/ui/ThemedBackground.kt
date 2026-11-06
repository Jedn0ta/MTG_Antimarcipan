package com.example.antimarcipan.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.antimarcipan.MtgApp
import com.example.antimarcipan.R
import com.example.antimarcipan.data.settings.AppBackground

/** Drawable pro dané pozadí (null = bez obrázku). */
private fun AppBackground.drawableRes(): Int? = when (this) {
    AppBackground.NONE -> null
    AppBackground.PLAINS -> R.drawable.bg_plains
    AppBackground.ISLAND -> R.drawable.bg_island
    AppBackground.SWAMP -> R.drawable.bg_swamp
    AppBackground.MOUNTAIN -> R.drawable.bg_mountain
    AppBackground.FOREST -> R.drawable.bg_forest
    AppBackground.WASTES -> R.drawable.bg_wastes
}

/**
 * Celoobrazovkové pozadí aplikace podle volby v nastavení. Pod obrázkem je
 * plná barva pozadí a nad ním ztmavovací přechod, aby byl obsah čitelný.
 */
@Composable
fun ThemedBackground(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settings = remember { (context.applicationContext as MtgApp).container.appSettings }
    val bg by settings.background.collectAsStateWithLifecycle()
    val res = bg.drawableRes()

    Box(modifier.fillMaxSize().background(Color(0xFF0E0B1A))) {
        if (res != null) {
            Image(
                painter = painterResource(res),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            // Ztmavovací přechod — nahoře i dole tmavší kvůli lištám.
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color(0xF20E0B1A),
                        0.28f to Color(0xCC0E0B1A),
                        0.72f to Color(0xCC0E0B1A),
                        1f to Color(0xF20E0B1A),
                    )
                )
            )
        }
    }
}
