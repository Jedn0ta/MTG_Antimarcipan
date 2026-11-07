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

    val settings = remember { (context.applicationContext as com.example.antimarcipan.MtgApp).container.appSettings }
    val gridMode by settings.collectionGrid.collectAsStateWithLifecycle()

    var showControls by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<CollectionCard?>(null) }
    var pendingAddDeck by remember { mutableStateOf<CollectionCard?>(null) }
    var pendingEdition by remember { mutableStateOf<CollectionCard?>(null) }

    val wishlist by vm.wishlist.collectAsStateWithLifecycle()
    val drafts by vm.drafts.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(0) } // 0 sbírka, 1 wishlist, 2 draft
    val showWishlist = tab == 1
    val showDraft = tab == 2
    val fstate by vm.cardFilter.collectAsStateWithLifecycle()
    val base = when (tab) { 1 -> wishlist; 2 -> drafts; else -> cards }
    val filterOptions = remember(base) { com.example.antimarcipan.ui.filter.buildFilterOptions(base) }
    val source = remember(base, fstate) { base.filter { fstate.matches(it) } }
    val totalQty = source.sumOf { it.quantity }
    var section by remember { mutableStateOf(0) } // 0 = Vše, 1 = Land, 2 = Spell
    var showSortMenu by remember { mutableStateOf(false) }
    val lands = if (section == 2) emptyList() else source.filter { isLandCard(it) }
    val spells = if (section == 1) emptyList() else source.filter { !isLandCard(it) }

    Column(Modifier.fillMaxSize()) {
        // Přepínač Sbírka / Wishlist + tlačítko s hledáním a filtry.
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 8.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            com.example.antimarcipan.ui.GoldChip(
                selected = tab == 0,
                onClick = { tab = 0 },
                label = { Text("Sbírka") },
            )
            com.example.antimarcipan.ui.GoldChip(
                selected = tab == 1,
                onClick = { tab = 1 },
                label = { Text("Wishlist") },
            )
            com.example.antimarcipan.ui.GoldChip(
                selected = tab == 2,
                onClick = { tab = 2 },
                label = { Text("Draft") },
            )
            Spacer(Modifier.weight(1f))
            Text(
                "$totalQty",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            // Zobrazení (seznam/mřížka) hned vedle filtru
            IconButton(onClick = { settings.setCollectionGrid(!gridMode) }) {
                Icon(
                    if (gridMode) Icons.AutoMirrored.Filled.ViewList else Icons.Filled.GridView,
                    contentDescription = "Zobrazení",
                )
            }
            androidx.compose.material3.BadgedBox(
                badge = {
                    if (fstate.isActive) androidx.compose.material3.Badge { Text("${fstate.activeCount}") }
                },
            ) {
                IconButton(onClick = { showControls = true }) {
                    Icon(
                        Icons.Filled.Tune,
                        contentDescription = "Filtr",
                        tint = if (fstate.isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        if (showControls) {
            com.example.antimarcipan.ui.filter.CardFilterSheet(
                options = filterOptions,
                state = fstate,
                resultCount = source.size,
                onChange = vm::setCardFilter,
                onDismiss = { showControls = false },
            )
        }

        // Sekce + řazení na jednom řádku
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf("Vše", "Land", "Spell").forEachIndexed { i, label ->
                com.example.antimarcipan.ui.GoldChip(
                    selected = section == i,
                    onClick = { section = i },
                    label = { Text(label) },
                )
            }
            Spacer(Modifier.weight(1f))
            Box {
                androidx.compose.material3.TextButton(onClick = { showSortMenu = true }) {
                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" " + filter.sortBy.label + if (filter.sortDesc) " ↓" else " ↑")
                }
                androidx.compose.material3.DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                ) {
                    SortBy.entries.forEach { s ->
                        val active = filter.sortBy == s
                        androidx.compose.material3.DropdownMenuItem(
                            text = {
                                Text(
                                    s.label + if (active) (if (filter.sortDesc) "  ↓" else "  ↑") else "",
                                    color = if (active) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface,
                                )
                            },
                            onClick = { vm.setSort(s); if (!active) showSortMenu = false },
                        )
                    }
                }
            }
        }

        HorizontalDivider()

        // Akce podle režimu: ve sbírce swipe = do balíku, dlouhé podržení = smazat;
        // na wishlistu swipe = přesun do sbírky, dlouhé podržení = pryč z wishlistu.
        val onSwipe: (CollectionCard) -> Unit = if (showWishlist || showDraft) {
            { card -> vm.wishlistToCollection(card.scryfallId) {
                Toast.makeText(context, "„${card.name}“ přesunuto do sbírky", Toast.LENGTH_SHORT).show()
            } }
        } else {
            { card -> pendingAddDeck = card }
        }
        val onDelete: (CollectionCard) -> Unit = if (showDraft) {
            { card -> pendingDelete = card }
        } else if (showWishlist) {
            { card -> vm.removeFromWishlist(card.scryfallId) }
        } else {
            { card -> pendingDelete = card }
        }

        if (source.isEmpty()) {
            EmptyState(
                if (showDraft && !fstate.isActive) "Žádné rozpracované karty"
                else if (showWishlist && !fstate.isActive) "Wishlist je prázdný" else if (fstate.isActive) "Nic nenalezeno" else "Zatím prázdno",
                if (showDraft) "Sem patří karty naskenované do poolu draftu / sealed balíku, dokud je nepřidáš do sbírky."
                else if (showWishlist) "Přidej karty na wishlist ze skenu nebo ručně."
                else if (fstate.isActive) "Zkus upravit filtr." else "Naskenuj první kartu v záložce Sken.",
            )
        } else if (gridMode) {
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(90.dp),
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                collectionGridSection("Land (${lands.size})", lands)
                collectionGridSection("Spell (${spells.size})", spells)
            }
        } else {
            LazyColumn(Modifier.weight(1f)) {
                collectionListSection("Land (${lands.size})", lands, context, vm, onSwipe, onDelete) { pendingEdition = it }
                collectionListSection("Spell (${spells.size})", spells, context, vm, onSwipe, onDelete) { pendingEdition = it }
            }
        }
    }

    pendingEdition?.let { card ->
        EditionPickerDialog(
            card = card,
            loadPrints = { vm.printsOf(card.name) },
            onDismiss = { pendingEdition = null },
            onPick = { print ->
                pendingEdition = null
                vm.changePrinting(card, print) {
                    Toast.makeText(
                        context,
                        "Edice změněna: ${print.setName ?: print.set?.uppercase()}",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            },
        )
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

private fun isLandCard(card: CollectionCard): Boolean =
    card.typeLine?.contains("Land", ignoreCase = true) == true

@Composable
private fun SectionHeaderC(text: String) {
    com.example.antimarcipan.ui.OrnamentHeader(text)
}

private fun LazyListScope.collectionListSection(
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

private fun LazyGridScope.collectionGridSection(title: String, cards: List<CollectionCard>) {
    if (cards.isEmpty()) return
    item(span = { GridItemSpan(maxLineSpan) }, key = "gh-$title") { SectionHeaderC(title) }
    items(cards, key = { it.scryfallId }) { card ->
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            com.example.antimarcipan.ui.InteractiveThumb(card, width = 88.dp, height = 123.dp)
        }
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
                com.example.antimarcipan.ui.GoldChip(
                    selected = filter.types.contains(t),
                    onClick = { vm.toggleType(t) },
                    label = { Text(t.label) },
                )
            }
        }
        Text("Rarita", style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Rarity.entries.forEach { r ->
                com.example.antimarcipan.ui.GoldChip(
                    selected = filter.rarities.contains(r),
                    onClick = { vm.toggleRarity(r) },
                    label = { Text(r.label) },
                )
            }
        }
        Text("Mana value", style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (0..7).forEach { b ->
                com.example.antimarcipan.ui.GoldChip(
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
                    com.example.antimarcipan.ui.GoldChip(
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
            com.example.antimarcipan.ui.GoldChip(
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
    onChangeEdition: () -> Unit = {},
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
            IconButton(onClick = onChangeEdition) {
                Icon(
                    Icons.Filled.SwapHoriz,
                    contentDescription = "Změnit edici",
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
                        rulings == null -> Text("Načítám…")
                        rulings!!.isEmpty() -> Text("Žádné rulings k této kartě.")
                        else -> com.example.antimarcipan.ui.TranslatableText(
                            rulings!!.joinToString("\n\n"),
                            MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRulings = false }) { Text("Zavřít") }
            },
        )
    }
}

/** Výběr jiného tisku (edice) karty — mřížka náhledů se symbolem edice. */
@Composable
private fun EditionPickerDialog(
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
                list.isEmpty() -> Text("Nepodařilo se načíst edice (je potřeba internet).")
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
        confirmButton = { TextButton(onClick = onDismiss) { Text("Zavřít") } },
    )
}
