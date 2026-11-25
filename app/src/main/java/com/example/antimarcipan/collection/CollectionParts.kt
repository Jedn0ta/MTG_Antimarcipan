package com.example.antimarcipan.collection

import com.example.antimarcipan.i18n.tr

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import com.example.antimarcipan.ui.filter.matches
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.antimarcipan.R
import com.example.antimarcipan.data.gathererLink
import com.example.antimarcipan.data.local.CollectionCard
import com.example.antimarcipan.data.thumbModel
import com.example.antimarcipan.ui.AppViewModelFactory
import com.example.antimarcipan.ui.CardListInfo
import com.example.antimarcipan.ui.CardThumb
import com.example.antimarcipan.ui.ConfirmDialog
import com.example.antimarcipan.ui.DeckPickerDialog
import com.example.antimarcipan.ui.EmptyState
import com.example.antimarcipan.ui.Holdable
import com.example.antimarcipan.ui.ManaCost
import kotlinx.coroutines.withTimeoutOrNull

internal fun CardColor.drawableRes(): Int = when (this) {
    CardColor.W -> R.drawable.mana_w
    CardColor.U -> R.drawable.mana_u
    CardColor.B -> R.drawable.mana_b
    CardColor.R -> R.drawable.mana_r
    CardColor.G -> R.drawable.mana_g
    CardColor.C -> R.drawable.mana_c
}

internal fun isLandCard(card: CollectionCard): Boolean =
    card.typeLine?.contains("Land", ignoreCase = true) == true

@Composable
internal fun SectionHeaderC(text: String) {
    com.example.antimarcipan.ui.OrnamentHeader(text)
}

internal fun LazyListScope.collectionListSection(
    title: String,
    cards: List<CollectionCard>,
    context: android.content.Context,
    vm: CollectionViewModel,
    onSwipe: (CollectionCard) -> Unit,
    onDelete: (CollectionCard) -> Unit,
    onEdition: (CollectionCard) -> Unit = {},
) {
    if (cards.isEmpty()) return
    item(key = "h-$title") { SectionHeaderC(title) }
    items(cards, key = { it.scryfallId }) { card ->
        Column {
            SwipeToAddDeck(onRequestAddToDeck = { onSwipe(card) }) {
                CollectionRow(
                    card = card,
                    onLongDelete = { onDelete(card) },
                    loadRulings = { vm.rulingsFor(it) },
                    onChangeEdition = { onEdition(card) },
                    onOpenGatherer = {
                        runCatching {
                            context.startActivity(
                                android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(card.gathererLink()),
                                ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    },
                )
            }
            HorizontalDivider()
        }
    }
}

internal fun LazyGridScope.collectionGridSection(title: String, cards: List<CollectionCard>) {
    if (cards.isEmpty()) return
    item(span = { GridItemSpan(maxLineSpan) }, key = "gh-$title") { SectionHeaderC(title) }
    items(cards, key = { it.scryfallId }) { card ->
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            ScaledGridThumb(card)
        }
    }
}

@Composable
internal fun ScaledGridThumb(card: CollectionCard) {
    val g = com.example.antimarcipan.ui.LocalThumbScale.current.grid
    com.example.antimarcipan.ui.InteractiveThumb(card, width = (88 * g).dp, height = (123 * g).dp)
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun SwipeToAddDeck(
    onRequestAddToDeck: () -> Unit,
    content: @Composable () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) onRequestAddToDeck()
            false // nemizí — jen vyvolá akci
        },
        // Akce se spustí až když levý okraj karty přejede přes polovinu displeje.
        positionalThreshold = { totalDistance -> totalDistance * 0.5f },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromEndToStart = false,
        backgroundContent = {
            // Jemná zpětná vazba: zlatý přechod sílí s tahem, bez ikon.
            if (dismissState.dismissDirection == SwipeToDismissBoxValue.Settled) return@SwipeToDismissBox
            val p = dismissState.progress.coerceIn(0f, 1f)
            val armed = dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd
            Box(
                Modifier.fillMaxSize().background(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = if (armed) 0.35f else 0.12f * p),
                            Color.Transparent,
                        )
                    )
                )
            )
        },
        content = { content() },
    )
}

/** Podržení prstem po zadaný čas → akce (bez reakce na swipe/zrušení). */
internal fun Modifier.longHold(durationMs: Long, onHold: () -> Unit): Modifier =
    this.pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            val completed = withTimeoutOrNull(durationMs) {
                waitForUpOrCancellation()
                true // prst zvednut nebo gesto zrušeno před časem
            }
            if (completed == null) onHold() // stále drženo → long hold
        }
    }

@Composable
internal fun CollectionRow(
    card: CollectionCard,
    onOpenGatherer: () -> Unit,
    onLongDelete: () -> Unit,
    loadRulings: suspend (String) -> List<String>,
    onChangeEdition: () -> Unit = {},
) {
    var ringProgress by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    Box(
        Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CardListInfo(
                card = card,
                modifier = Modifier.weight(1f),
                onLongHold = null, // mazání jen z detailu karty (ochrana před omylem)
            )
            IconButton(onClick = onChangeEdition) {
                Icon(
                    Icons.Filled.SwapHoriz,
                    contentDescription = tr("Změnit edici"),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                )
            }
            GathererButton(
                cardName = card.name,
                loadRulings = { loadRulings(card.scryfallId) },
                onOpenGatherer = onOpenGatherer,
                onProgress = { ringProgress = it },
            )
        }
        if (ringProgress > 0.18f && ringProgress < 1f) {
            androidx.compose.material3.CircularProgressIndicator(
                progress = { ringProgress },
                strokeWidth = 3.dp,
                modifier = Modifier.size(46.dp),
            )
        }
    }
}

/**
 * Ikona Gathereru: ťuknutí zobrazí rulings ve scrollovatelném dialogu,
 * podržení 3 s (s plnícím kolečkem uprostřed řádku) otevře kartu na Gathereru.
 */
@Composable
internal fun GathererButton(
    cardName: String,
    loadRulings: suspend () -> List<String>,
    onOpenGatherer: () -> Unit,
    onProgress: (Float) -> Unit,
) {
    var showRulings by remember { mutableStateOf(false) }
    var rulings by remember { mutableStateOf<List<String>?>(null) }

    Holdable(
        holdMs = 3000,
        onTap = { showRulings = true },
        onHold = onOpenGatherer,
        onProgress = onProgress,
    ) {
        Icon(
            Icons.AutoMirrored.Filled.MenuBook,
            contentDescription = tr("Rulings (ťuk) / Gatherer (podržení)"),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(12.dp),
        )
    }

    if (showRulings) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            if (rulings == null) rulings = loadRulings()
        }
        com.example.antimarcipan.ui.GoldDialog(
            onDismissRequest = { showRulings = false },
            title = { Text("Rulings — $cardName") },
            text = {
                Box(
                    Modifier
                        .heightIn(max = 380.dp)
                        .verticalScroll(androidx.compose.foundation.rememberScrollState())
                ) {
                    when {
                        rulings == null -> Text(tr("Načítám…"))
                        rulings!!.isEmpty() -> Text(tr("Žádné rulings k této kartě."))
                        else -> com.example.antimarcipan.ui.SymbolText(
                            rulings!!.joinToString("\n\n"),
                            MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRulings = false }) { Text(tr("Zavřít")) }
            },
        )
    }
}

/** Výběr jiného tisku (edice) karty — mřížka náhledů se symbolem edice. */
@Composable
internal fun EditionPickerDialog(
    card: CollectionCard,
    loadPrints: suspend () -> List<com.example.antimarcipan.data.scryfall.ScryfallCard>,
    onDismiss: () -> Unit,
    onPick: (com.example.antimarcipan.data.scryfall.ScryfallCard) -> Unit,
) {
    var prints by remember { mutableStateOf<List<com.example.antimarcipan.data.scryfall.ScryfallCard>?>(null) }
    androidx.compose.runtime.LaunchedEffect(card.name) { prints = loadPrints() }

    com.example.antimarcipan.ui.GoldDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edice — ${card.name}") },
        text = {
            val list = prints
            when {
                list == null -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
                list.isEmpty() -> Text(tr("Nepodařilo se načíst edice (je potřeba internet)."))
                else -> androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(92.dp),
                    modifier = Modifier.heightIn(max = 460.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(list, key = { it.id }) { p ->
                        val current = p.id == card.scryfallId
                        Column(
                            Modifier
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                .background(
                                    if (current) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                    else Color.Transparent
                                )
                                .clickable { onPick(p) }
                                .padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            com.example.antimarcipan.ui.CardThumb(p.bestImageUrl, width = 84.dp, height = 117.dp)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(top = 4.dp),
                            ) {
                                com.example.antimarcipan.ui.SetSymbol(p.set, size = 14.dp)
                                Text(
                                    (p.set ?: "").uppercase() + (p.collectorNumber?.let { " #$it" } ?: ""),
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                )
                            }
                            Text(
                                p.setName ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Zavřít")) } },
    )
}
