package com.example.antimarcipan.collection

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

private fun CardColor.drawableRes(): Int = when (this) {
    CardColor.W -> R.drawable.mana_w
    CardColor.U -> R.drawable.mana_u
    CardColor.B -> R.drawable.mana_b
    CardColor.R -> R.drawable.mana_r
    CardColor.G -> R.drawable.mana_g
    CardColor.C -> R.drawable.mana_c
}

@Composable
fun CollectionScreen(
    vm: CollectionViewModel = viewModel(factory = AppViewModelFactory),
) {
    val context = LocalContext.current
    val cards by vm.cards.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val decks by vm.decks.collectAsStateWithLifecycle()
    val sets by vm.availableSets.collectAsStateWithLifecycle()

    var showFilters by remember { mutableStateOf(false) }
    var gridMode by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<CollectionCard?>(null) }
    var pendingAddDeck by remember { mutableStateOf<CollectionCard?>(null) }

    Column(Modifier.fillMaxSize()) {
        // Kompaktní řádek: hledání + přepínač filtrů (filtry schované)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            OutlinedTextField(
                value = filter.query,
                onValueChange = vm::setQuery,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Hledat…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
            )
            IconButton(onClick = { gridMode = !gridMode }) {
                Icon(
                    if (gridMode) Icons.AutoMirrored.Filled.ViewList else Icons.Filled.GridView,
                    contentDescription = "Zobrazení",
                )
            }
            IconButton(onClick = { showFilters = !showFilters }) {
                Icon(
                    Icons.Filled.FilterList,
                    contentDescription = "Filtry",
                    tint = if (filter.isActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        AnimatedVisibility(visible = showFilters) {
            Column {
                // Barvy — ikony many
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CardColor.entries.forEach { c ->
                        ColorPip(
                            res = c.drawableRes(),
                            selected = filter.colors.contains(c),
                            onClick = { vm.toggleColor(c) },
                        )
                    }
                }
                AdvancedFilters(filter, vm, sets)
                SortRow(filter, vm)
            }
        }

        val totalQty = cards.sumOf { it.quantity }
        val totalValue = cards.sumOf { (it.priceUsd?.toDoubleOrNull() ?: 0.0) * it.quantity }
        Text(
            buildString {
                append("$totalQty karet (${cards.size} unikátních)")
                if (filter.isActive) append(" · filtrováno")
                if (totalValue > 0) append(" · ~$%.2f".format(totalValue))
            },
            Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )

        HorizontalDivider()

        if (cards.isEmpty()) {
            EmptyState(
                if (filter.isActive) "Nic nenalezeno" else "Zatím prázdno",
                if (filter.isActive) "Zkus upravit filtry." else "Naskenuj první kartu v záložce Sken.",
            )
        } else if (gridMode) {
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(90.dp),
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(cards, key = { it.scryfallId }) { card ->
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        com.example.antimarcipan.ui.InteractiveThumb(card, width = 88.dp, height = 123.dp)
                    }
                }
            }
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(cards, key = { it.scryfallId }) { card ->
                    SwipeToAddDeck(onRequestAddToDeck = { pendingAddDeck = card }) {
                        CollectionRow(
                            card = card,
                            onLongDelete = { pendingDelete = card },
                            loadRulings = { vm.rulingsFor(it) },
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
    }

    pendingAddDeck?.let { card ->
        DeckPickerDialog(
            decks = decks,
            onDismiss = { pendingAddDeck = null },
            onPick = { deck ->
                pendingAddDeck = null
                vm.addCardToDeck(deck.id, card.scryfallId) {
                    Toast.makeText(context, "Přidáno do „${deck.name}“", Toast.LENGTH_SHORT).show()
                }
            },
        )
    }

    pendingDelete?.let { card ->
        ConfirmDialog(
            title = "Smazat kartu?",
            message = "Opravdu odebrat „${card.name}“ ze sbírky?",
            confirmText = "Smazat",
            onConfirm = { vm.remove(card.scryfallId); pendingDelete = null },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun ColorPip(res: Int, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .then(
                if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                else Modifier
            )
            .clickable { onClick() }
            .padding(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(res),
            contentDescription = null,
            modifier = Modifier.size(34.dp).alpha(if (selected) 1f else 0.45f),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdvancedFilters(
    filter: CollectionFilter,
    vm: CollectionViewModel,
    sets: List<SetOption>,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        Text("Typ", style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CardType.entries.forEach { t ->
                FilterChip(
                    selected = filter.types.contains(t),
                    onClick = { vm.toggleType(t) },
                    label = { Text(t.label) },
                )
            }
        }
        Text("Rarita", style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Rarity.entries.forEach { r ->
                FilterChip(
                    selected = filter.rarities.contains(r),
                    onClick = { vm.toggleRarity(r) },
                    label = { Text(r.label) },
                )
            }
        }
        Text("Mana value", style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (0..7).forEach { b ->
                FilterChip(
                    selected = filter.cmcBuckets.contains(b),
                    onClick = { vm.toggleCmc(b) },
                    label = { Text(if (b == 7) "7+" else b.toString()) },
                )
            }
        }
        if (sets.isNotEmpty()) {
            Text("Edice", style = MaterialTheme.typography.labelMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                sets.forEach { s ->
                    FilterChip(
                        selected = filter.sets.contains(s.code),
                        onClick = { vm.toggleSet(s.code) },
                        label = { Text(s.label) },
                    )
                }
            }
        }
        if (filter.isActive) {
            TextButton(onClick = vm::clearFilters) { Text("Vymazat filtry") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SortRow(filter: CollectionFilter, vm: CollectionViewModel) {
    FlowRow(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SortBy.entries.forEach { s ->
            val active = filter.sortBy == s
            FilterChip(
                selected = active,
                onClick = { vm.setSort(s) },
                label = { Text(s.label + if (active) (if (filter.sortDesc) " ↓" else " ↑") else "") },
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToAddDeck(
    onRequestAddToDeck: () -> Unit,
    content: @Composable () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) onRequestAddToDeck()
            false // nemizí — jen vyvolá výběr balíku
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromEndToStart = false,
        backgroundContent = {
            Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp), contentAlignment = Alignment.CenterStart) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = Color(0xFF2E9E5B))
            }
        },
        content = { content() },
    )
}

/** Podržení prstem po zadaný čas → akce (bez reakce na swipe/zrušení). */
private fun Modifier.longHold(durationMs: Long, onHold: () -> Unit): Modifier =
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
private fun CollectionRow(
    card: CollectionCard,
    onOpenGatherer: () -> Unit,
    onLongDelete: () -> Unit,
    loadRulings: suspend (String) -> List<String>,
) {
    var ringProgress by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    Box(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface),
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
                onLongHold = onLongDelete,
            )
            GathererButton(
                cardName = card.name,
                loadRulings = { loadRulings(card.scryfallId) },
                onOpenGatherer = onOpenGatherer,
                onProgress = { ringProgress = it },
            )
        }
        if (ringProgress > 0f && ringProgress < 1f) {
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
private fun GathererButton(
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
            contentDescription = "Rulings (ťuk) / Gatherer (podržení)",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(12.dp),
        )
    }

    if (showRulings) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            if (rulings == null) rulings = loadRulings()
        }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showRulings = false },
            title = { Text("Rulings — $cardName") },
            text = {
                Box(
                    Modifier
                        .heightIn(max = 380.dp)
                        .verticalScroll(androidx.compose.foundation.rememberScrollState())
                ) {
                    Text(
                        when {
                            rulings == null -> "Načítám…"
                            rulings!!.isEmpty() -> "Žádné rulings k této kartě."
                            else -> rulings!!.joinToString("\n\n")
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showRulings = false }) { Text("Zavřít") }
            },
        )
    }
}
