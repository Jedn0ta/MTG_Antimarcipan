package com.example.antimarcipan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent

@Composable
fun CardThumb(
    model: Any?,
    modifier: Modifier = Modifier,
    width: Dp = 48.dp,
    height: Dp = 67.dp,
) {
    // Periodický lesk přes kartu (krátký průjezd, pak pauza)
    val shine = androidx.compose.animation.core.rememberInfiniteTransition(label = "shine")
    val sweep by shine.animateFloat(
        initialValue = -1f,
        targetValue = 2.5f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            androidx.compose.animation.core.tween(durationMillis = 4200,
                easing = androidx.compose.animation.core.LinearEasing),
        ),
        label = "sweep",
    )
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier
            .size(width = width, height = height)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .drawWithContent {
                drawContent()
                // lesk jen v první třetině cyklu
                if (sweep in -0.5f..1.5f) {
                    val x = size.width * sweep
                    drawRect(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            0f to androidx.compose.ui.graphics.Color.Transparent,
                            0.5f to androidx.compose.ui.graphics.Color.White.copy(alpha = 0.22f),
                            1f to androidx.compose.ui.graphics.Color.Transparent,
                            start = androidx.compose.ui.geometry.Offset(x - size.width * 0.4f, 0f),
                            end = androidx.compose.ui.geometry.Offset(x + size.width * 0.4f, size.height),
                        )
                    )
                }
            }
            .border(
                1.dp,
                androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(
                        androidx.compose.ui.graphics.Color(0xFFF6DE9A),
                        androidx.compose.ui.graphics.Color(0xFF9C7A2E),
                        androidx.compose.ui.graphics.Color(0xFFE9C46A),
                    )
                ),
                shape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (model == null) {
            Icon(
                Icons.Filled.Style,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size((width.value * 0.4f).dp),
            )
        } else {
            SubcomposeAsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            ) {
                when (painter.state) {
                    is AsyncImagePainter.State.Loading ->
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size((width.value * 0.35f).dp),
                        )
                    is AsyncImagePainter.State.Error ->
                        Icon(
                            Icons.Filled.BrokenImage,
                            contentDescription = "náhled se nenačetl",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size((width.value * 0.4f).dp),
                        )
                    else -> SubcomposeAsyncImageContent()
                }
            }
        }
    }
}

@Composable
fun EmptyState(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(32.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun ScreenTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

/** Ozdobný nadpis sekce: zlatý kosočtverec, text a mizející zlatá linka. */
@Composable
fun OrnamentHeader(text: String, modifier: Modifier = Modifier) {
    val gold = MaterialTheme.colorScheme.primary
    Row(
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(8.dp)
                .rotate(45f)
                .background(
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        listOf(androidx.compose.ui.graphics.Color(0xFFF6DE9A), gold)
                    )
                ),
        )
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(
                letterSpacing = androidx.compose.ui.unit.TextUnit(1.6f, androidx.compose.ui.unit.TextUnitType.Sp),
                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
            ),
            color = gold,
        )
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(gold.copy(alpha = 0.7f), androidx.compose.ui.graphics.Color.Transparent)
                    )
                ),
        )
    }
}

