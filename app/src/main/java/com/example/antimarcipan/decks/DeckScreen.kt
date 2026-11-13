package com.example.antimarcipan.decks

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.border
import com.example.antimarcipan.ui.filter.matches
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.LooksOne
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.antimarcipan.data.colorIdentityList
import com.example.antimarcipan.data.local.CollectionCard
import com.example.antimarcipan.data.thumbModel
import com.example.antimarcipan.data.local.Deck
import com.example.antimarcipan.ui.AppViewModelFactory
import com.example.antimarcipan.ui.CardThumb
import com.example.antimarcipan.ui.ColorIdentityPips
import com.example.antimarcipan.ui.ConfirmDialog
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

@Composable
fun DeckScreen(
    vm: DeckViewModel = viewModel(factory = AppViewModelFactory),
) {
    val selected by vm.selectedDeck.collectAsStateWithLifecycle()

    if (selected == null) {
        DeckListView(vm)
    } else {
        DeckDetailView(vm, selected!!)
    }
}

// ---------------------------------------------------------------- list / load

@Composable
private fun DeckListView(vm: DeckViewModel) {
    val decks by vm.decks.collectAsStateWithLifecycle()
    var showNewDeck by remember { mutableStateOf(false) }
    var pendingDeleteDeck by remember { mutableStateOf<Deck?>(null) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            Button(onClick = { showNewDeck = true }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(" Nový balík")
            }
        }
        HorizontalDivider()

        if (decks.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Zatím žádný balík. Vytvoř první tlačítkem „Nový“.",
                    Modifier.padding(32.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(decks, key = { it.id }) { deck ->
                    DeckListRow(
                        deck = deck,
                        colors = produceDeckColors(vm, deck.id),
                        onOpen = { vm.select(deck.id) },
                        onDelete = { pendingDeleteDeck = deck },
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (showNewDeck) {
        NewDeckDialog(
            onDismiss = { showNewDeck = false },
            onConfirm = { name, format, colors ->
                showNewDeck = false
                vm.createDeck(name, format, colors)
            },
        )
    }

    pendingDeleteDeck?.let { deck ->
        ConfirmDialog(
            title = "Smazat balík?",
            message = "Opravdu smazat balík „${deck.name}“? Karty zůstanou ve sbírce.",
            confirmText = "Smazat",
            onConfirm = { vm.deleteDeck(deck.id); pendingDeleteDeck = null },
            onDismiss = { pendingDeleteDeck = null },
        )
    }
}

@Composable
private fun produceDeckColors(vm: DeckViewModel, deckId: Long): Set<String> {
    val state = androidx.compose.runtime.produceState(initialValue = emptySet<String>(), deckId) {
        value = vm.deckColors(deckId)
    }
    return state.value
}

@Composable
private fun DeckListRow(
    deck: Deck,
    colors: Set<String>,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val shown = parseDeckColors(deck.colors).ifEmpty { colors }
    Row(
        Modifier.fillMaxWidth().clickable { onOpen() }.padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(deck.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                DeckFormat.from(deck.format).label +
                    (deck.commanderName?.let { " · ${it}" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ColorIdentityPips(shown, symbolSize = 16)
                comboName(shown)?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Smazat balík")
        }
    }
}

@Composable
private fun DeckCover(path: String?, size: androidx.compose.ui.unit.Dp) {
    val shape = RoundedCornerShape(8.dp)
    if (path != null) {
        AsyncImage(
            model = File(path),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size).clip(shape),
        )
    } else {
        Box(
            Modifier.size(size).clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Style,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

// ---------------------------------------------------------------- detail

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun DeckDetailView(vm: DeckViewModel, deck: Deck) {
    val context = LocalContext.current
    val deckCards by vm.selectedDeckCards.collectAsStateWithLifecycle()
    val collection by vm.collection.collectAsStateWithLifecycle()
    val poolRows by vm.selectedDeckPool.collectAsStateWithLifecycle()
    val poolScanStatus by vm.poolScanStatus.collectAsStateWithLifecycle()
    val poolScanBusy by vm.poolScanBusy.collectAsStateWithLifecycle()

    val settings = remember { (context.applicationContext as com.example.antimarcipan.MtgApp).container.appSettings }
    val gridMode by settings.deckGrid.collectAsStateWithLifecycle()
    val sectionSaved by settings.deckSection.collectAsStateWithLifecycle()
    val sectionMode = runCatching { DeckSectionMode.valueOf(sectionSaved) }.getOrDefault(DeckSectionMode.ALL)

    var showRename by remember { mutableStateOf(false) }
    var showFormat by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showBasics by remember { mutableStateOf(false) }
    var showTypeFilter by remember { mutableStateOf(false) }
    var deckFilter by remember(deck.id) { mutableStateOf(com.example.antimarcipan.ui.filter.CardFilterState()) }
    var pendingRemove by remember { mutableStateOf<CollectionCard?>(null) }
    var showCurve by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var pendingPoolRemove by remember { mutableStateOf<Pair<CollectionCard, Int>?>(null) }

    val format = DeckFormat.from(deck.format)
    // Barvy balíku dle zemí, které produkují manu
    val deckColors by androidx.compose.runtime.produceState(emptySet<String>(), deckCards) {
        value = vm.deckColors(deck.id)
    }
    // Rozbalené karty (podle počtu kusů) pro počítání velikosti a legality
    val flatCards = deckCards.flatMap { e -> List(e.deckQuantity) { e.card } }

    // Kontext filtru: karty podle zvolené sekce (Vše / Land / Spell).
    val sectionBase = when (sectionMode) {
        DeckSectionMode.ALL -> deckCards
        DeckSectionMode.LANDS -> deckCards.filter { isLandCard(it.card) }
        DeckSectionMode.OTHERS -> deckCards.filter { !isLandCard(it.card) }
    }
    // Kategorie filtru se odvozují z kontextu — u landů se tribaly neukážou.
    val filterOpts = remember(sectionBase) {
        com.example.antimarcipan.ui.filter.buildFilterOptions(sectionBase.map { it.card })
    }

    Column(Modifier.fillMaxSize()) {
        // Hlavička — název na vlastním řádku (čitelný, snese delší názvy),
        // pod ním metadata a přepínače zobrazení.
        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = vm::closeDeck) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                }
                Text(
                    deck.name,
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Akce")
                    }
                    androidx.compose.material3.DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Název a barvy") },
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                            onClick = { showMenu = false; showRename = true },
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Export decklistu") },
                            leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                            onClick = { showMenu = false; shareDecklist(context, deck.name, flatCards) },
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Formát balíku") },
                            leadingIcon = { Icon(Icons.Filled.Tune, contentDescription = null) },
                            onClick = { showMenu = false; showFormat = true },
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Přidat základní země") },
                            leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                            onClick = { showMenu = false; showBasics = true },
                        )
                    }
                }
            }
            // Metadata: jeden symbol formátu + počet karet + barvy
            Row(
                Modifier.fillMaxWidth().padding(start = 4.dp, top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    formatIcon(format),
                    contentDescription = format.label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    deckCountLabel(flatCards.size, format.minCards),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                )
                com.example.antimarcipan.ui.totalPriceLabel(deckCards.map { it.card to it.deckQuantity })?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelLarge,
                        color = androidx.compose.ui.graphics.Color(0xFF8FD6A0),
                    )
                }
                val shownColors = parseDeckColors(deck.colors).ifEmpty { deckColors }
                ColorIdentityPips(shownColors, symbolSize = 15)
                comboName(shownColors)?.let {
                    Text(it, style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        HorizontalDivider()

        // Ovládání na jednom řádku: sekce vlevo, filtr a mřížka vpravo
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DeckSectionMode.entries.forEach { mode ->
                com.example.antimarcipan.ui.GoldChip(
                    selected = sectionMode == mode,
                    onClick = { settings.setDeckSection(mode.name) },
                    label = { Text(mode.label) },
                )
            }
            Spacer(Modifier.weight(1f))
            androidx.compose.material3.BadgedBox(
                badge = {
                    if (deckFilter.isActive) androidx.compose.material3.Badge { Text("${deckFilter.activeCount}") }
                },
            ) {
                IconButton(onClick = { showTypeFilter = true }) {
                    Icon(
                        Icons.Filled.FilterList,
                        contentDescription = "Filtr",
                        tint = if (deckFilter.isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            IconButton(onClick = { showCurve = !showCurve }) {
                Icon(
                    androidx.compose.material.icons.Icons.Filled.BarChart,
                    contentDescription = if (showCurve) "Skrýt mana curve" else "Zobrazit mana curve",
                    tint = if (showCurve) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(onClick = { settings.setDeckGrid(!gridMode) }) {
                Icon(
                    if (gridMode) Icons.AutoMirrored.Filled.ViewList else Icons.Filled.GridView,
                    contentDescription = if (gridMode) "Seznam" else "Mřížka",
                )
            }
        }

        val visible = sectionBase.filter { deckFilter.matches(it.card) }

        if (showTypeFilter) {
            com.example.antimarcipan.ui.filter.CardFilterSheet(
                options = filterOpts,
                state = deckFilter,
                resultCount = visible.size,
                onChange = { deckFilter = it },
                onDismiss = { showTypeFilter = false },
            )
        }
        val allLands = visible.filter { isLandCard(it.card) }
        val allOthers = visible.filter { !isLandCard(it.card) }
        // Basic landy se zobrazují jen v panelu základních zemí
        val allMode = sectionMode == DeckSectionMode.ALL
        // „Vše“: žádné seskupování, všechno podle abecedy
        val lands = if (allMode) emptyList() else allLands.filterNot { isBasicLand(it.card) }.sortedBy { it.card.name.lowercase() }
        val others = if (allMode) (allLands.filterNot { isBasicLand(it.card) } + allOthers).sortedBy { it.card.name.lowercase() }
        else allOthers.sortedBy { it.card.name.lowercase() }
        val othersLabel = if (allMode) "Vše" else "Spell"
        val showBasicPanel = sectionMode != DeckSectionMode.OTHERS
        val basicCounts = BASIC_TYPES.associate { (name, _) ->
            name to deckCards.filter { isBasicLand(it.card) && basicBaseName(it.card.name) == name }
                .sumOf { it.deckQuantity }
        }
        val onBasicInc: (String) -> Unit = { name ->
            val entry = deckCards.filter { isBasicLand(it.card) && basicBaseName(it.card.name) == name }
                .minByOrNull { if (it.card.name.startsWith("Snow", true)) 1 else 0 }
            if (entry != null) vm.setDeckCardQty(deck.id, entry.card.scryfallId, entry.deckQuantity + 1)
            else vm.addBasicLands(deck.id, mapOf(name to 1))
        }
        val onBasicDec: (String) -> Unit = { name ->
            deckCards.filter { isBasicLand(it.card) && basicBaseName(it.card.name) == name && it.deckQuantity > 0 }
                .maxByOrNull { it.deckQuantity }
                ?.let { vm.setDeckCardQty(deck.id, it.card.scryfallId, it.deckQuantity - 1) }
        }

        val poolCards = poolRows.map { it.card.copy(quantity = it.deckQuantity) }
        val addFromPool: (CollectionCard) -> Unit = { c ->
            val inDeck = deckCards.firstOrNull { it.card.scryfallId == c.scryfallId }
            if (inDeck == null) vm.addCard(c)
            else vm.setDeckCardQty(deck.id, c.scryfallId, inDeck.deckQuantity + 1)
        }
        val applyLimitedLands: (Map<String, Int>) -> Unit = { split ->
            // vynulovat basicy, které v rozpisu nejsou, ostatní nastavit
            deckCards.filter { isBasicLand(it.card) && basicBaseName(it.card.name) !in split.keys }
                .forEach { vm.setDeckCardQty(deck.id, it.card.scryfallId, 0) }
            vm.addBasicLands(deck.id, split)
        }

        if (showCurve) {
            val (dCurve, dAvg, _) = com.example.antimarcipan.ui.computeCurve(deckCards)
            if (dCurve.any { it.first + it.second > 0 }) com.example.antimarcipan.ui.MiniManaCurve(dCurve, dAvg)
        }
        if (gridMode) {
            if (format.isLimited) LimitedAdvisorPanel(
                format = format,
                deckCards = flatCards,
                pool = poolCards,
                poolImported = deck.poolImported,
                scanStatus = poolScanStatus,
                scanBusy = poolScanBusy,
                onPhoto = { vm.scanPoolPhoto(deck.id, context, it) },
                onImportPool = { vm.importPoolToCollection(deck.id) },
                onAddCard = addFromPool,
                onApplyLands = applyLimitedLands,
            )
            if (showBasicPanel) BasicLandPanel(basicCounts, onBasicInc, onBasicDec)
            androidx.compose.runtime.CompositionLocalProvider(com.example.antimarcipan.ui.LocalCardList provides ((lands + others).map { it.card }), com.example.antimarcipan.ui.LocalCardRemoveAction provides ("Odebrat z balíku" to { c: CollectionCard -> pendingRemove = c })) {
            DeckGrid(
                lands = lands,
                others = others,
                emptyDeck = deckCards.isEmpty(),
                othersLabel = othersLabel,
            )
            }
            return@Column
        }

        androidx.compose.runtime.CompositionLocalProvider(com.example.antimarcipan.ui.LocalCardList provides ((lands + others).map { it.card } + poolRows.map { it.card }), com.example.antimarcipan.ui.LocalCardRemoveAction provides ("Odebrat z balíku" to { c: CollectionCard -> pendingRemove = c })) {
        LazyColumn(Modifier.fillMaxSize()) {
            if (deckCards.isEmpty()) {
                item {
                    Text(
                        "Zatím prázdný — přidej dole ze sbírky, nebo základní země přes menu.",
                        Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            if (format.isLimited) item {
                LimitedAdvisorPanel(
                    format = format,
                    deckCards = flatCards,
                    pool = poolCards,
                    poolImported = deck.poolImported,
                    scanStatus = poolScanStatus,
                    scanBusy = poolScanBusy,
                    onPhoto = { vm.scanPoolPhoto(deck.id, context, it) },
                    onImportPool = { vm.importPoolToCollection(deck.id) },
                    onAddCard = addFromPool,
                    onApplyLands = applyLimitedLands,
                )
            }
            if (showBasicPanel) {
                val landTotal = lands.sumOf { it.deckQuantity } + basicCounts.values.sum()
                item { SectionHeader("Land ($landTotal)") }
                item { BasicLandPanel(basicCounts, onBasicInc, onBasicDec) }
            }
            if (lands.isNotEmpty()) {
                item {
                    Card(
                        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                    ) {
                        lands.forEachIndexed { i, entry ->
                            DeckEntryRow(
                                entry = entry,
                                isBasic = false,
                                isCommander = entry.card.scryfallId == deck.commanderScryfallId,
                                showCommander = format == DeckFormat.COMMANDER,
                                onCommander = { vm.setCommander(entry.card) },
                                onLongRemove = { pendingRemove = entry.card },
                                onChangeQty = { q -> vm.setDeckCardQty(deck.id, entry.card.scryfallId, q) },
                            )
                            if (i < lands.lastIndex) HorizontalDivider()
                        }
                    }
                }
            }

            if (others.isNotEmpty()) {
                item { SectionHeader("$othersLabel (${others.sumOf { it.deckQuantity }})") }
                item {
                    Card(
                        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                    ) {
                        others.forEachIndexed { i, entry ->
                            DeckEntryRow(
                                entry = entry,
                                isBasic = false,
                                isCommander = entry.card.scryfallId == deck.commanderScryfallId,
                                showCommander = format == DeckFormat.COMMANDER,
                                onCommander = { vm.setCommander(entry.card) },
                                onLongRemove = { pendingRemove = entry.card },
                                onChangeQty = {},
                            )
                            if (i < others.lastIndex) HorizontalDivider()
                        }
                    }
                }
            }

            if (deckFilter.isActive && allLands.isEmpty() && allOthers.isEmpty() && deckCards.isNotEmpty()) {
                item {
                    Text(
                        "Filtru neodpovídá žádná karta.",
                        Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
            }

            if (format.isLimited) {
                item { SectionHeader("Pool (${poolRows.sumOf { it.deckQuantity }})") }
                if (poolRows.isEmpty()) item {
                    Text(
                        "Pool je prázdný. V rádci nahoře ťukni na „Vyfotit karty“ a vyfoť karty z boosterů.",
                        Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall,
                    )
                }
                items(poolRows, key = { "pool-" + it.card.scryfallId }) { row ->
                    val used = deckCards.firstOrNull { it.card.scryfallId == row.card.scryfallId }?.deckQuantity ?: 0
                    val free = row.deckQuantity - used
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        com.example.antimarcipan.ui.CardListInfo(card = row.card, modifier = Modifier.weight(1f))
                        Text("$free/${row.deckQuantity}", style = MaterialTheme.typography.labelMedium)
                        IconButton(onClick = { pendingPoolRemove = row.card to row.deckQuantity }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Odebrat z poolu")
                        }
                        IconButton(onClick = { addFromPool(row.card) }, enabled = free > 0) {
                            Icon(Icons.Filled.Add, contentDescription = "Do balíku")
                        }
                    }
                    HorizontalDivider()
                }
            }
            if (!format.isLimited) item { SectionHeader("Přidat ze sbírky") }
            val inDeckIds = deckCards.map { it.card.scryfallId }.toSet()
            val addable = if (format.isLimited) emptyList() else collection.filter { it.scryfallId !in inDeckIds }
            if (addable.isEmpty() && !format.isLimited) {
                item {
                    Text(
                        "Všechny karty ze sbírky už jsou v balíku (nebo je sbírka prázdná).",
                        Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            items(addable, key = { "add-" + it.scryfallId }) { card ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    com.example.antimarcipan.ui.CardListInfo(card = card, modifier = Modifier.weight(1f))
                    IconButton(onClick = { vm.addCard(card) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Přidat")
                    }
                }
                HorizontalDivider()
            }
        }
        }
    }

    if (showRename) {
        DeckNameDialog(
            title = "Název a barvy",
            initial = deck.name,
            initialColors = deck.colors,
            onDismiss = { showRename = false },
            onConfirm = { name, colors ->
                showRename = false
                vm.renameDeck(deck.id, name)
                vm.setDeckColors(deck.id, colors)
            },
        )
    }

    pendingPoolRemove?.let { (card, q) ->
        ConfirmDialog(
            title = "Odebrat z poolu?",
            message = "Odebrat 1 kus „${card.name}“ z poolu?",
            confirmText = "Odebrat",
            onConfirm = { vm.setPoolQty(deck.id, card.scryfallId, q - 1); pendingPoolRemove = null },
            onDismiss = { pendingPoolRemove = null },
        )
    }
    pendingRemove?.let { card ->
        ConfirmDialog(
            title = "Odebrat z balíku?",
            message = "Odebrat „${card.name}“ z balíku? Karta zůstane ve sbírce.",
            confirmText = "Odebrat",
            onConfirm = { vm.removeCard(card); pendingRemove = null },
            onDismiss = { pendingRemove = null },
        )
    }

    if (showFormat) {
        FormatDialog(
            current = format,
            onDismiss = { showFormat = false },
            onPick = { f -> showFormat = false; vm.setFormat(deck.id, f) },
        )
    }

    if (showBasics) {
        BasicLandsDialog(
            onDismiss = { showBasics = false },
            onConfirm = { counts ->
                showBasics = false
                vm.addBasicLands(deck.id, counts)
            },
        )
    }
}

/** Dlouhé podržení → akce (ignoruje swipe/zrušení). */
private fun Modifier.longHold(durationMs: Long, onHold: () -> Unit): Modifier =
    this.pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            val completed = withTimeoutOrNull(durationMs) {
                waitForUpOrCancellation(); true
            }
            if (completed == null) onHold()
        }
    }

/** Hlavní typy + podtypy stvoření (tribaly) přítomné v balíku — pro filtr. */
private val MAIN_TYPES = listOf(
    "Creature", "Instant", "Sorcery", "Artifact", "Enchantment",
    "Planeswalker", "Land", "Battle",
)

private enum class DeckSectionMode(val label: String) {
    ALL("Vše"), LANDS("Land"), OTHERS("Spell")
}

// ---- Barvy balíku a název barevné kombinace ----

private val WUBRG = listOf("W", "U", "B", "R", "G")
private val COLOR_CZ = mapOf("W" to "bílé", "U" to "modré", "B" to "černé", "R" to "červené", "G" to "zelené")
private val COMBO_NAMES = mapOf(
    "W" to "Bílá", "U" to "Modrá", "B" to "Černá", "R" to "Červená", "G" to "Zelená", "C" to "Bezbarvá",
    "WU" to "Azorius", "WB" to "Orzhov", "WR" to "Boros", "WG" to "Selesnya",
    "UB" to "Dimir", "UR" to "Izzet", "UG" to "Simic",
    "BR" to "Rakdos", "BG" to "Golgari", "RG" to "Gruul",
    "WUB" to "Esper", "WUR" to "Jeskai", "WUG" to "Bant", "WBR" to "Mardu", "WBG" to "Abzan",
    "WRG" to "Naya", "UBR" to "Grixis", "UBG" to "Sultai", "URG" to "Temur", "BRG" to "Jund",
    "WUBRG" to "Pětibarevný (WUBRG)",
)

/** "WUB" → {W,U,B}; filtruje jen platné znaky many. */
private fun parseDeckColors(colors: String?): Set<String> =
    colors?.uppercase()?.toCharArray()?.map { it.toString() }
        ?.filter { it in WUBRG || it == "C" }?.toSet() ?: emptySet()

/** Název barevné kombinace (Azorius, Jeskai…), nebo null když se nehodí. */
private fun comboName(colors: Set<String>): String? {
    val cols = WUBRG.filter { it in colors }
    if (cols.isEmpty()) return if ("C" in colors) "Bezbarvá" else null
    val key = cols.joinToString("")
    COMBO_NAMES[key]?.let { return it }
    if (cols.size == 4) {
        val missing = WUBRG.first { it !in colors }
        return "Čtyřbarevný (bez ${COLOR_CZ[missing]})"
    }
    return null
}

private fun formatIcon(format: DeckFormat): androidx.compose.ui.graphics.vector.ImageVector =
    when (format) {
        DeckFormat.COMMANDER -> Icons.Filled.Star
        DeckFormat.SINGLETON -> Icons.Filled.LooksOne
        DeckFormat.STANDARD -> Icons.Filled.EmojiEvents
        DeckFormat.DRAFT -> Icons.Filled.SwapHoriz
        DeckFormat.SEALED -> Icons.Filled.Inventory2
        DeckFormat.FREEFORM -> Icons.Filled.AllInclusive
    }

private fun deckCountLabel(count: Int, min: Int): String =
    if (min > 0) "$count/$min karet" else "$count karet"

// ---- Kontextový filtr balíku (select-list) ----

private val COLOR_LABELS = linkedMapOf(
    "W" to "Bílá", "U" to "Modrá", "B" to "Černá", "R" to "Červená", "G" to "Zelená", "C" to "Bezbarvá",
)
private val MANA_BUCKETS = listOf("0–1", "2–3", "4–5", "6+")

private fun creatureSubtypesOf(card: CollectionCard): List<String> {
    val line = card.typeLine ?: return emptyList()
    if (!line.contains("Creature", ignoreCase = true)) return emptyList()
    return line.substringAfter("—", "").trim().split(" ")
        .map { it.trim() }.filter { it.length > 2 }
}

private fun manaBucketOf(cmc: Double?): String {
    val v = (cmc ?: 0.0)
    return when {
        v <= 1 -> "0–1"
        v <= 3 -> "2–3"
        v <= 5 -> "4–5"
        else -> "6+"
    }
}

/**
 * Sestaví kategorie filtru z kontextu karet. Kategorie se ukáže jen když má
 * aspoň 2 hodnoty — u landů tak odpadnou tribaly i (jednotná) mana.
 * Klíč hodnoty je "Kategorie:hodnota", popisek se bere z value.
 */
private fun buildDeckFilters(cards: List<CollectionCard>): LinkedHashMap<String, List<Pair<String, String>>> {
    val types = linkedSetOf<String>()
    val tribes = sortedSetOf<String>()
    val colors = linkedSetOf<String>()
    val buckets = linkedSetOf<String>()
    cards.forEach { c ->
        val line = c.typeLine
        MAIN_TYPES.forEach { if (line?.contains(it, ignoreCase = true) == true) types.add(it) }
        creatureSubtypesOf(c).forEach { tribes.add(it) }
        c.colorIdentityList().forEach { colors.add(it) }
        if (!(line?.contains("Land", ignoreCase = true) == true)) buckets.add(manaBucketOf(c.cmc))
    }
    val out = LinkedHashMap<String, List<Pair<String, String>>>()
    if (types.size >= 2) out["Typ"] = types.map { "Typ:$it" to it }
    if (tribes.size >= 2) out["Tribal"] = tribes.map { "Tribal:$it" to it }
    if (colors.size >= 2) out["Barva"] = COLOR_LABELS.keys
        .filter { it in colors }.map { "Barva:$it" to (COLOR_LABELS[it] ?: it) }
    if (buckets.size >= 2) out["Mana"] = MANA_BUCKETS
        .filter { it in buckets }.map { "Mana:$it" to it }
    return out
}

private fun matchesDeckFilters(card: CollectionCard, selected: List<String>): Boolean {
    if (selected.isEmpty()) return true
    val byCat = selected.groupBy({ it.substringBefore(':') }, { it.substringAfter(':') })
    return byCat.all { (cat, values) -> values.any { matchDeckValue(card, cat, it) } }
}

private fun matchDeckValue(card: CollectionCard, cat: String, value: String): Boolean = when (cat) {
    "Typ" -> card.typeLine?.contains(value, ignoreCase = true) == true
    "Tribal" -> creatureSubtypesOf(card).any { it.equals(value, ignoreCase = true) }
    "Barva" -> card.colorIdentityList().contains(value)
    "Mana" -> manaBucketOf(card.cmc) == value
    else -> true
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DeckFilterMenu(
    expanded: Boolean,
    categories: LinkedHashMap<String, List<Pair<String, String>>>,
    selected: List<String>,
    onToggle: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!expanded) return
    // Spodní panel: každá kategorie jako přehledná skupina dlaždic.
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                .verticalScroll(androidx.compose.foundation.rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Filtr", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                if (selected.isNotEmpty()) {
                    TextButton(onClick = onClear) { Text("Zrušit (${selected.size})") }
                }
            }
            categories.forEach { (title, values) ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        title.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (title == "Barva") {
                        // Barvy jako symboly many
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            values.forEach { (key, label) ->
                                val on = selected.contains(key)
                                androidx.compose.foundation.Image(
                                    painter = androidx.compose.ui.res.painterResource(manaRes(key.substringAfter(':'))),
                                    contentDescription = label,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .border(
                                            if (on) 2.dp else 0.dp,
                                            if (on) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            androidx.compose.foundation.shape.CircleShape,
                                        )
                                        .alpha(if (on) 1f else 0.4f)
                                        .clickable { onToggle(key) },
                                )
                            }
                        }
                    } else {
                        androidx.compose.foundation.layout.FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            values.forEach { (key, label) ->
                                com.example.antimarcipan.ui.GoldChip(
                                    selected = selected.contains(key),
                                    onClick = { onToggle(key) },
                                    label = { Text(label) },
                                )
                            }
                        }
                    }
                }
            }
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            ) { Text("Hotovo") }
        }
    }
}

private fun deckTypeTokens(cards: List<CollectionCard>): List<String> {
    val mains = linkedSetOf<String>()
    val subtypes = linkedSetOf<String>()
    cards.forEach { c ->
        val line = c.typeLine ?: return@forEach
        MAIN_TYPES.forEach { if (line.contains(it, ignoreCase = true)) mains.add(it) }
        // Podtypy jen pro stvoření (tribaly: Elf, Knight, …)
        if (line.contains("Creature", ignoreCase = true)) {
            line.substringAfter("—", "").trim()
                .split(" ")
                .map { it.trim() }
                .filter { it.length > 2 }
                .forEach { subtypes.add(it) }
        }
    }
    // Hlavní typy první, pak tribaly abecedně
    return mains.toList() + subtypes.toList().sorted()
}

@Composable
private fun DeckGrid(
    lands: List<com.example.antimarcipan.data.local.DeckCardWithQty>,
    others: List<com.example.antimarcipan.data.local.DeckCardWithQty>,
    emptyDeck: Boolean,
    othersLabel: String = "Spell",
) {
    if (emptyDeck) {
        Text(
            "Zatím prázdný — přepni na seznam a přidej karty ze sbírky.",
            Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodySmall,
        )
        return
    }
    val orderedLands = lands.sortedBy { if (isBasicLand(it.card)) 0 else 1 }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(84.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (orderedLands.isNotEmpty()) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                SectionHeader("Ostatní země (${orderedLands.sumOf { it.deckQuantity }})")
            }
            gridItems(orderedLands, key = { "gl-" + it.card.scryfallId }) { entry ->
                DeckGridCell(entry)
            }
        }
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            SectionHeader("$othersLabel (${others.sumOf { it.deckQuantity }})")
        }
        gridItems(others, key = { "go-" + it.card.scryfallId }) { entry ->
            DeckGridCell(entry)
        }
    }
}

@Composable
private fun DeckGridCell(entry: com.example.antimarcipan.data.local.DeckCardWithQty) {
    Box(contentAlignment = Alignment.TopEnd) {
        com.example.antimarcipan.ui.InteractiveThumb(card = entry.card, width = 84.dp, height = 117.dp)
        if (entry.deckQuantity > 1) {
            Box(
                Modifier
                    .padding(3.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 1.dp),
            ) {
                Text(
                    "×${entry.deckQuantity}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

/** Typy základních zemí a jejich mana. */
private val BASIC_TYPES = listOf(
    "Plains" to "W", "Island" to "U", "Swamp" to "B", "Mountain" to "R", "Forest" to "G", "Wastes" to "C",
)

/** „Snow-Covered Forest“ → „Forest“. */
private fun basicBaseName(name: String): String =
    name.removePrefix("Snow-Covered ").removePrefix("snow-covered ").trim()

/**
 * Panel základních zemí: dlaždice se symbolem many, počtem a −/+.
 * Ťuknutí na symbol přidá jeden kus.
 */
@Composable
private fun BasicLandPanel(
    counts: Map<String, Int>,
    onInc: (String) -> Unit,
    onDec: (String) -> Unit,
) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, com.example.antimarcipan.ui.GoldBorderBrush),
    ) {
        Column(Modifier.padding(horizontal = 6.dp, vertical = 10.dp)) {
            Text(
                "Základní země · ${counts.values.sum()}",
                Modifier.padding(start = 6.dp, bottom = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(Modifier.fillMaxWidth()) {
                BASIC_TYPES.forEach { (name, mana) ->
                    val n = counts[name] ?: 0
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(manaRes(mana)),
                            contentDescription = "$name +1",
                            modifier = Modifier
                                .size(38.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .alpha(if (n > 0) 1f else 0.45f)
                                .clickable { onInc(name) },
                        )
                        Text(
                            "$n",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (n > 0) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                        )
                        Text(
                            name,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                        Row {
                            Text(
                                "−",
                                Modifier
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .clickable(enabled = n > 0) { onDec(name) }
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (n > 0) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            )
                            Text(
                                "+",
                                Modifier
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .clickable { onInc(name) }
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

private val BASIC_LANDS = setOf(
    "plains", "island", "swamp", "mountain", "forest", "wastes",
    "snow-covered plains", "snow-covered island", "snow-covered swamp",
    "snow-covered mountain", "snow-covered forest",
)

private fun isLandCard(card: CollectionCard): Boolean =
    card.typeLine?.contains("Land", ignoreCase = true) == true

private fun isBasicLand(card: CollectionCard): Boolean {
    val n = card.name.lowercase().substringBefore("//").trim()
    if (n in BASIC_LANDS) return true
    return card.typeLine?.contains("Basic", ignoreCase = true) == true &&
        card.typeLine.contains("Land", ignoreCase = true)
}

@Composable
private fun DeckEntryRow(
    entry: com.example.antimarcipan.data.local.DeckCardWithQty,
    isBasic: Boolean,
    isCommander: Boolean,
    showCommander: Boolean,
    onCommander: () -> Unit,
    onLongRemove: () -> Unit,
    onChangeQty: (Int) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        com.example.antimarcipan.ui.CardListInfo(
            card = entry.card,
            deckQuantity = entry.deckQuantity,
            modifier = Modifier.weight(1f),
            onLongHold = onLongRemove,
        )
        if (isBasic) {
            IconButton(onClick = { onChangeQty(entry.deckQuantity - 1) }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Remove, contentDescription = "−")
            }
            Text("${entry.deckQuantity}", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { onChangeQty(entry.deckQuantity + 1) }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Add, contentDescription = "+")
            }
        } else if (showCommander) {
            IconButton(onClick = onCommander) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = "Velitel",
                    tint = if (isCommander) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                )
            }
        }
    }
}

/** Minimální info: kolik karet ještě doplnit do minimálního počtu. */
@Composable
private fun DeckNeed(format: DeckFormat, total: Int) {
    val min = format.minCards
    if (min == 0) {
        Text("$total", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        return
    }
    val need = (min - total).coerceAtLeast(0)
    val ok = need == 0
    Text(
        if (ok) "$total ✓" else "$total · +$need",
        style = MaterialTheme.typography.labelMedium,
        color = if (ok) Color(0xFF2E9E5B) else Color(0xFFD98324),
    )
}

@Composable
private fun BasicLandsDialog(
    onDismiss: () -> Unit,
    onConfirm: (Map<String, Int>) -> Unit,
) {
    // jméno země → mana ikona res
    val lands = listOf(
        "Plains" to com.example.antimarcipan.R.drawable.mana_w,
        "Island" to com.example.antimarcipan.R.drawable.mana_u,
        "Swamp" to com.example.antimarcipan.R.drawable.mana_b,
        "Mountain" to com.example.antimarcipan.R.drawable.mana_r,
        "Forest" to com.example.antimarcipan.R.drawable.mana_g,
    )
    val counts = remember { mutableStateMapOf<String, Int>() }
    com.example.antimarcipan.ui.GoldDialog(
        onDismissRequest = onDismiss,
        title = { Text("Přidat základní země") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                lands.forEach { (name, res) ->
                    val v = counts[name] ?: 0
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(res),
                            contentDescription = name,
                            modifier = Modifier.size(26.dp),
                        )
                        Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        IconButton(onClick = { counts[name] = (v - 1).coerceAtLeast(0) }) {
                            Icon(Icons.Filled.Remove, contentDescription = "−")
                        }
                        Text("$v", Modifier.widthIn(min = 24.dp), style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { counts[name] = v + 1 }) {
                            Icon(Icons.Filled.Add, contentDescription = "+")
                        }
                    }
                }
                Text(
                    "Nastavuje celkový počet dané základní země v balíku.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(counts.toMap()) }) { Text("Přidat") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Zrušit") } },
    )
}

@Composable
private fun FormatDialog(
    current: DeckFormat,
    onDismiss: () -> Unit,
    onPick: (DeckFormat) -> Unit,
) {
    com.example.antimarcipan.ui.GoldDialog(
        onDismissRequest = onDismiss,
        title = { Text("Formát balíku") },
        text = {
            Column {
                DeckFormat.entries.forEach { f ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onPick(f) }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = f == current,
                            onClick = { onPick(f) },
                        )
                        Column {
                            Text(f.label, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                f.rules,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Zavřít") } },
    )
}

@Composable
private fun NewDeckDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, DeckFormat, String?) -> Unit,
) {
    var guideFor by remember { mutableStateOf<DeckFormat?>(null) }
    guideFor?.let { LimitedGuideDialog(it) { guideFor = null } }
    var name by remember { mutableStateOf("") }
    var format by remember { mutableStateOf(DeckFormat.SINGLETON) }
    val colors = remember { androidx.compose.runtime.mutableStateListOf<String>() }
    com.example.antimarcipan.ui.GoldDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nový balík") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Název balíku") },
                    singleLine = true,
                )
                Text("Formát", style = MaterialTheme.typography.labelMedium)
                DeckFormat.entries.forEach { f ->
                    Row(
                        Modifier.fillMaxWidth().clickable { format = f }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = f == format,
                            onClick = { format = f },
                        )
                        Column {
                            Text(f.label)
                            Text(f.rules, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }
                if (format.isLimited) {
                    Text(
                        if (format == DeckFormat.SEALED)
                            "Sealed: otevři 6 boosterů a v balíku je přes „Vyfotit karty“ vyfoť do poolu (ne do sbírky). Rádce v balíku pak doporučí barvy, nejlepší karty a rozpis 17 zemí."
                        else
                            "Draft: z každého boosteru si vybereš 1 kartu a zbytek posíláš dál. Vydraftované karty vyfoť přímo v balíku přes „Vyfotit karty“, rádce pomůže se stavbou. Do sbírky je přidáš hromadně až na konci.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    TextButton(onClick = { guideFor = format }) { Text("Jak se ${format.label} hraje a staví") }
                }
                Text("Barvy balíku", style = MaterialTheme.typography.labelMedium)
                ColorPicker(colors.toSet()) { c ->
                    if (colors.contains(c)) colors.remove(c) else colors.add(c)
                }
                comboName(colors.toSet())?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, format, serializeColors(colors)) }) { Text("Vytvořit") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Zrušit") } },
    )
}

private fun serializeColors(colors: List<String>): String? =
    WUBRG.filter { it in colors }.joinToString("").ifEmpty { null }

private fun manaRes(c: String): Int = when (c) {
    "W" -> com.example.antimarcipan.R.drawable.mana_w
    "U" -> com.example.antimarcipan.R.drawable.mana_u
    "B" -> com.example.antimarcipan.R.drawable.mana_b
    "R" -> com.example.antimarcipan.R.drawable.mana_r
    "G" -> com.example.antimarcipan.R.drawable.mana_g
    else -> com.example.antimarcipan.R.drawable.mana_c
}

@Composable
private fun ColorPicker(selected: Set<String>, onToggle: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        WUBRG.forEach { c ->
            val on = c in selected
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(manaRes(c)),
                contentDescription = c,
                modifier = Modifier
                    .size(34.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .alpha(if (on) 1f else 0.3f)
                    .clickable { onToggle(c) },
            )
        }
    }
}

@Composable
private fun SwipeToRemove(onRequestRemove: () -> Unit, content: @Composable () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onRequestRemove()
            }
            false // čeká na potvrzení
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().clip(RoundedCornerShape(0.dp)),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Row(
                    Modifier.padding(end = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, tint = Color(0xFFD95C5C))
                    Text("Odebrat", color = Color(0xFFD95C5C))
                }
            }
        },
        content = { content() },
    )
}

@Composable
private fun SectionHeader(text: String) {
    com.example.antimarcipan.ui.OrnamentHeader(text)
}

@Composable
private fun DeckCardRow(
    card: CollectionCard,
    isCommander: Boolean,
    showCommander: Boolean,
    onCommander: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CardThumb(card.thumbModel())
        Column(Modifier.weight(1f)) {
            Text(card.name, style = MaterialTheme.typography.bodyMedium)
            Text(
                card.typeLine ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
        if (showCommander) {
            IconButton(onClick = onCommander) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = "Nastavit jako velitele",
                    tint = if (isCommander) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                )
            }
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Delete, contentDescription = "Odebrat z balíku")
        }
    }
}

@Composable
private fun DuplicateWarning(names: List<String>) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFD98324))
            Column {
                Text("Duplicitní karty (${names.size})", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Tyto karty jsou v balíku víckrát (různé edice). " +
                        "V Commanderu/singletonu smí být jen jedna kopie: " +
                        names.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                )
            }
        }
    }
}

private fun shareDecklist(context: Context, deckName: String, cards: List<CollectionCard>) {
    val lines = cards
        .groupBy { it.name }
        .map { (name, group) -> "${group.size} $name" }
        .sorted()
    val text = buildString {
        appendLine("// $deckName")
        lines.forEach { appendLine(it) }
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, deckName)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(
        Intent.createChooser(intent, "Sdílet decklist").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

@Composable
private fun DeckNameDialog(
    title: String,
    initial: String,
    initialColors: String?,
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    val colors = remember { androidx.compose.runtime.mutableStateListOf<String>().apply { addAll(parseDeckColors(initialColors).filter { it in WUBRG }) } }
    com.example.antimarcipan.ui.GoldDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Název balíku") },
                    singleLine = true,
                )
                Text("Barvy balíku", style = MaterialTheme.typography.labelMedium)
                ColorPicker(colors.toSet()) { c ->
                    if (colors.contains(c)) colors.remove(c) else colors.add(c)
                }
                comboName(colors.toSet())?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(name, serializeColors(colors)) }) { Text("Uložit") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Zrušit") } },
    )
}
