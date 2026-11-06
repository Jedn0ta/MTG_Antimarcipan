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
import androidx.compose.material.icons.filled.CheckCircle
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
            onConfirm = { name, format ->
                showNewDeck = false
                vm.createDeck(name, format)
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
    Row(
        Modifier.fillMaxWidth().clickable { onOpen() }.padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DeckCover(deck.coverImagePath, size = 56.dp)
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
            ColorIdentityPips(colors, symbolSize = 16)
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

    var showRename by remember { mutableStateOf(false) }
    var showFormat by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showBasics by remember { mutableStateOf(false) }
    var showTypeFilter by remember { mutableStateOf(false) }
    var gridMode by remember { mutableStateOf(false) }
    var sectionMode by remember { mutableStateOf(DeckSectionMode.ALL) }
    val selectedTypes = remember { androidx.compose.runtime.mutableStateListOf<String>() }
    var pendingRemove by remember { mutableStateOf<CollectionCard?>(null) }

    val coverPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) vm.setCover(deck.id, uri) }

    val format = DeckFormat.from(deck.format)
    // Barvy balíku dle zemí, které produkují manu
    val deckColors by androidx.compose.runtime.produceState(emptySet<String>(), deckCards) {
        value = vm.deckColors(deck.id)
    }
    // Rozbalené karty (podle počtu kusů) pro počítání velikosti a legality
    val flatCards = deckCards.flatMap { e -> List(e.deckQuantity) { e.card } }

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
                DeckCover(deck.coverImagePath, size = 44.dp)
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
                            text = { Text("Název") },
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                            onClick = { showMenu = false; showRename = true },
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Náhled") },
                            leadingIcon = { Icon(Icons.Filled.Image, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
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
                ColorIdentityPips(deckColors, symbolSize = 15)
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
                androidx.compose.material3.FilterChip(
                    selected = sectionMode == mode,
                    onClick = { sectionMode = mode },
                    label = { Text(mode.label) },
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { showTypeFilter = !showTypeFilter }) {
                Icon(
                    Icons.Filled.FilterList,
                    contentDescription = "Filtr typů",
                    tint = if (selectedTypes.isEmpty()) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = { gridMode = !gridMode }) {
                Icon(
                    if (gridMode) Icons.AutoMirrored.Filled.ViewList else Icons.Filled.GridView,
                    contentDescription = if (gridMode) "Seznam" else "Mřížka",
                )
            }
        }

        // Typy a tribaly odvozené přímo z balíku (hlavní typy + podtypy stvoření)
        val typeTokens = remember(deckCards) { deckTypeTokens(deckCards.map { it.card }) }

        // Systematický filtr: oddělené řádky Typ / Tribal, vodorovně rolovatelné.
        if (showTypeFilter) {
            val mains = typeTokens.filter { it in MAIN_TYPES }
            val tribes = typeTokens.filterNot { it in MAIN_TYPES }
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (typeTokens.isEmpty()) {
                    Text(
                        "Zatím není co filtrovat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
                if (mains.isNotEmpty()) {
                    FilterTokenRow("Typ", mains, selectedTypes) { toggleType(selectedTypes, it) }
                }
                if (tribes.isNotEmpty()) {
                    FilterTokenRow("Tribal", tribes, selectedTypes) { toggleType(selectedTypes, it) }
                }
                if (selectedTypes.isNotEmpty()) {
                    androidx.compose.material3.TextButton(
                        onClick = { selectedTypes.clear() },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    ) { Text("Zrušit filtr") }
                }
            }
        }

        val visible = deckCards.filter { entry ->
            selectedTypes.isEmpty() || selectedTypes.any { token ->
                entry.card.typeLine?.contains(token, ignoreCase = true) == true
            }
        }
        val allLands = visible.filter { isLandCard(it.card) }
        val allOthers = visible.filter { !isLandCard(it.card) }
        val lands = if (sectionMode != DeckSectionMode.OTHERS) allLands else emptyList()
        val others = if (sectionMode != DeckSectionMode.LANDS) allOthers else emptyList()

        if (gridMode) {
            DeckGrid(
                lands = lands,
                others = others,
                emptyDeck = deckCards.isEmpty(),
            )
            return@Column
        }

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

            if (lands.isNotEmpty()) {
                // Basicy první, pak ostatní země
                val orderedLands = lands.sortedBy { if (isBasicLand(it.card)) 0 else 1 }
                item { SectionHeader("Land (${lands.sumOf { it.deckQuantity }})") }
                item {
                    Card(
                        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                    ) {
                        orderedLands.forEachIndexed { i, entry ->
                            DeckEntryRow(
                                entry = entry,
                                isBasic = isBasicLand(entry.card),
                                isCommander = entry.card.scryfallId == deck.commanderScryfallId,
                                showCommander = format == DeckFormat.COMMANDER,
                                onCommander = { vm.setCommander(entry.card) },
                                onLongRemove = { pendingRemove = entry.card },
                                onChangeQty = { q -> vm.setDeckCardQty(deck.id, entry.card.scryfallId, q) },
                            )
                            if (i < orderedLands.lastIndex) HorizontalDivider()
                        }
                    }
                }
            }

            if (others.isNotEmpty()) {
                item { SectionHeader("Spell (${others.sumOf { it.deckQuantity }})") }
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

            if (selectedTypes.isNotEmpty() && allLands.isEmpty() && allOthers.isEmpty() && deckCards.isNotEmpty()) {
                item {
                    Text(
                        "Filtru „${selectedTypes.joinToString(", ")}“ neodpovídá žádná karta.",
                        Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
            }

            item { SectionHeader("Přidat ze sbírky") }
            val inDeckIds = deckCards.map { it.card.scryfallId }.toSet()
            val addable = collection.filter { it.scryfallId !in inDeckIds }
            if (addable.isEmpty()) {
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

    if (showRename) {
        DeckNameDialog(
            title = "Přejmenovat balík",
            initial = deck.name,
            onDismiss = { showRename = false },
            onConfirm = { name ->
                showRename = false
                vm.renameDeck(deck.id, name)
            },
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

private fun formatIcon(format: DeckFormat): androidx.compose.ui.graphics.vector.ImageVector =
    when (format) {
        DeckFormat.COMMANDER -> Icons.Filled.Star
        DeckFormat.SINGLETON -> Icons.Filled.LooksOne
        DeckFormat.FREEFORM -> Icons.Filled.AllInclusive
    }

private fun deckCountLabel(count: Int, min: Int): String =
    if (min > 0) "$count/$min karet" else "$count karet"

private fun toggleType(
    list: androidx.compose.runtime.snapshots.SnapshotStateList<String>,
    token: String,
) {
    if (list.contains(token)) list.remove(token) else list.add(token)
}

@Composable
private fun FilterTokenRow(
    title: String,
    tokens: List<String>,
    selected: List<String>,
    onToggle: (String) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            title,
            Modifier.width(52.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        Row(
            Modifier.weight(1f)
                .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            tokens.forEach { token ->
                androidx.compose.material3.FilterChip(
                    selected = selected.contains(token),
                    onClick = { onToggle(token) },
                    label = { Text(token) },
                )
            }
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
                SectionHeader("Land (${orderedLands.sumOf { it.deckQuantity }})")
            }
            gridItems(orderedLands, key = { "gl-" + it.card.scryfallId }) { entry ->
                DeckGridCell(entry)
            }
        }
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            SectionHeader("Spell (${others.sumOf { it.deckQuantity }})")
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
    val min = when (format) {
        DeckFormat.COMMANDER, DeckFormat.SINGLETON -> 100
        DeckFormat.FREEFORM -> 0
    }
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
    AlertDialog(
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
    AlertDialog(
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
                                when (f) {
                                    DeckFormat.COMMANDER -> "100 karet, singleton, 1 velitel"
                                    DeckFormat.SINGLETON -> "jednokarťák: 100+ karet, singleton, bez velitele"
                                    DeckFormat.FREEFORM -> "bez omezení"
                                },
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
    onConfirm: (String, DeckFormat) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var format by remember { mutableStateOf(DeckFormat.SINGLETON) }
    AlertDialog(
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
                        Text(f.label)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(name, format) }) { Text("Vytvořit") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Zrušit") } },
    )
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
    Text(
        text,
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
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
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Název balíku") },
                singleLine = true,
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(name) }) { Text("Uložit") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Zrušit") } },
    )
}
