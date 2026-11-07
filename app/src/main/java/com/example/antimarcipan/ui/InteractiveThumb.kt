package com.example.antimarcipan.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.antimarcipan.data.local.CollectionCard
import com.example.antimarcipan.data.thumbModel

/**
 * Náhled karty: ťuknutí → info o kartě, podržení → celý obrázek karty.
 * Během podržení ukazuje plnící se kolečko.
 */
@Composable
fun InteractiveThumb(
    card: CollectionCard,
    width: Dp = 60.dp,
    height: Dp = 84.dp,
) {
    var showInfo by remember { mutableStateOf(false) }
    var showFull by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    Box(contentAlignment = Alignment.Center) {
        Holdable(
            holdMs = 650,
            onTap = { showInfo = true },
            onHold = { showFull = true },
            onProgress = { progress = it },
        ) {
            CardThumb(card.thumbModel(), width = width, height = height)
        }
        // Kolečko až po krátkém podržení (ne při obyčejném ťuknutí)
        if (progress > 0.18f && progress < 1f) {
            CircularProgressIndicator(
                progress = { progress },
                strokeWidth = 3.dp,
                modifier = Modifier.size(width * 0.7f),
            )
        }
    }

    if (showInfo) CardInfoDialog(card) { showInfo = false }
    if (showFull) {
        val list = LocalCardList.current.filter { it.thumbModel() != null }
        val start = list.indexOfFirst { it.scryfallId == card.scryfallId }
        if (start >= 0 && list.size > 1) {
            FullCardPager(list, start) { showFull = false }
        } else {
            val model = card.thumbModel()
            if (model != null) FullCardDialog(model) { showFull = false }
            else showFull = false
        }
    }
}

@Composable
fun CardInfoDialog(card: CollectionCard, onDismiss: () -> Unit) {
    com.example.antimarcipan.ui.GoldDialog(
        onDismissRequest = onDismiss,
        title = { Text(card.name) },
        text = {
            Column(
                Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                card.manaCost?.takeIf { it.isNotBlank() }?.let { ManaCost(it, symbolSize = 18) }
                Text(
                    card.setName ?: card.setCode?.uppercase() ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                card.typeLine?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                // Textace s vysvětlivkami samotných hesel + možnost překladu do češtiny
                com.example.antimarcipan.data.KeywordGlossary.annotate(card.oracleText)
                    ?.takeIf { it.isNotBlank() }
                    ?.let { SymbolText(it, MaterialTheme.typography.bodySmall) }
                Text(
                    buildString {
                        card.rarity?.let { append(it.replaceFirstChar { c -> c.uppercase() }) }
                        card.priceUsd?.let { append("  ·  $$it") }
                        append("  ·  ks: ${card.quantity}")
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Zavřít") } },
    )
}

/** Seznam karet, ve kterém se nachází náhled — pro listování swipem v plném náhledu. */
val LocalCardList = androidx.compose.runtime.staticCompositionLocalOf<List<CollectionCard>> { emptyList() }

/** Plný náhled s listováním: swipe doleva/doprava = další/předchozí karta seznamu. */
@Composable
private fun FullCardPager(cards: List<CollectionCard>, start: Int, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val pager = androidx.compose.foundation.pager.rememberPagerState(initialPage = start) { cards.size }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.compose.foundation.pager.HorizontalPager(
                state = pager,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 28.dp),
                pageSpacing = 12.dp,
            ) { i ->
                AsyncImage(
                    model = cards[i].thumbModel(),
                    contentDescription = cards[i].name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 560.dp)
                        .border(2.dp, GoldBorderBrush, androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
                        .padding(4.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp)),
                )
            }
            Text(
                "${pager.currentPage + 1} / ${cards.size}  ·  ${cards[pager.currentPage].name}",
                Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.labelLarge,
                color = androidx.compose.ui.graphics.Color.White,
            )
        }
    }
}

@Composable
private fun FullCardDialog(model: Any, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .padding(8.dp)
                .border(2.dp, GoldBorderBrush, androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
                .padding(4.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp)),
        )
    }
}
