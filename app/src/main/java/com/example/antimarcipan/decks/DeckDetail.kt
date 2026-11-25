package com.example.antimarcipan.decks

import com.example.antimarcipan.i18n.tr

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
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.LooksOne
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.BarChart
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
import androidx.compose.material.icons.filled.PhotoCamera
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

// ---------------------------------------------------------------- detail

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun DeckDetailView(vm: DeckViewModel, deck: Deck) {
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
    var basicsOpen by androidx.compose.runtime.saveable.rememberSaveable(deck.id) { mutableStateOf<Boolean?>(null) }
    var pendingPoolRemove by remember { mutableStateOf<Pair<CollectionCard, Int>?>(null) }
    var showAddCards by remember { mutableStateOf(false) }
    val infoPrefs = remember { context.getSharedPreferences("deck_ui", android.content.Context.MODE_PRIVATE) }
    var showInfo by remember { mutableStateOf(infoPrefs.getBoolean("info", true)) }

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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Zpět"))
                }
                Text(
                    deck.name,
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(onClick = { showInfo = !showInfo; infoPrefs.edit().putBoolean("info", showInfo).apply() }) {
                    Icon(
                        if (showInfo) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (showInfo) tr("Skrýt přehled balíku") else tr("Zobrazit přehled balíku"),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = tr("Akce"))
                    }
                    androidx.compose.material3.DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(tr("Název a barvy")) },
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                            onClick = { showMenu = false; showRename = true },
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(tr("Export decklistu")) },
                            leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                            onClick = { showMenu = false; shareDecklist(context, deck.name, flatCards) },
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(tr("Formát balíku")) },
                            leadingIcon = { Icon(Icons.Filled.Tune, contentDescription = null) },
                            onClick = { showMenu = false; showFormat = true },
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(tr("Přidat základní země")) },
                            leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                            onClick = { showMenu = false; showBasics = true },
                        )
                    }
                }
            }
            // Přehledová karta balíku: formát, plnost, cena, barvy, složení
            val min = format.minCards
            val count = flatCards.size
            val spellsN = flatCards.count { !isLandCard(it) }
            val creaturesN = flatCards.count { it.typeLine?.contains("Creature", true) == true }
            val landsN = flatCards.count { isLandCard(it) }
            if (!showInfo) Text(
                (if (min > 0) "$count / $min karet" else "$count karet") +
                    (com.example.antimarcipan.ui.totalPriceLabel(deckCards.map { it.card to it.deckQuantity })?.let { " · $it" } ?: "") +
                    " · ${format.label}",
                Modifier.padding(start = 52.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            androidx.compose.animation.AnimatedVisibility(visible = showInfo) {
            androidx.compose.material3.Card(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, com.example.antimarcipan.ui.GoldBorderBrush),
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.material3.AssistChip(
                            onClick = { showFormat = true },
                            label = { Text(format.label) },
                            leadingIcon = { Icon(formatIcon(format), null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) },
                        )
                        val shownColors = parseDeckColors(deck.colors).ifEmpty { deckColors }
                        ColorIdentityPips(shownColors, symbolSize = 16)
                        comboName(shownColors)?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, maxLines = 1) }
                        Spacer(Modifier.weight(1f))
                        com.example.antimarcipan.ui.totalPriceLabel(deckCards.map { it.card to it.deckQuantity })?.let {
                            Text(it, style = MaterialTheme.typography.titleSmall, color = androidx.compose.ui.graphics.Color(0xFF8FD6A0))
                        }
                    }
                    // Plnost balíku
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (min > 0) "$count / $min karet" else "$count karet",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (min > 0 && count >= min) androidx.compose.ui.graphics.Color(0xFF8FD6A0) else MaterialTheme.colorScheme.onSurface,
                        )
                        if (min > 0) androidx.compose.material3.LinearProgressIndicator(
                            progress = { (count.toFloat() / min).coerceIn(0f, 1f) },
                            modifier = Modifier.weight(1f).padding(start = 10.dp).height(6.dp)
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(3.dp)),
                            color = if (count >= min) androidx.compose.ui.graphics.Color(0xFF5FD38A) else MaterialTheme.colorScheme.primary,
                            trackColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f),
                        )
                    }
                    // Složení
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        DeckStat(tr("Tvorové"), creaturesN)
                        DeckStat(tr("Ostatní kouzla"), spellsN - creaturesN)
                        DeckStat(tr("Země"), landsN)
                    }
                    // Multisken fotkou rovnou do balíku
                    val deckPhotoUri = remember {
                        val dir = java.io.File(context.cacheDir, "photos").apply { mkdirs() }
                        androidx.core.content.FileProvider.getUriForFile(
                            context, context.packageName + ".fileprovider", java.io.File(dir, "deck.jpg"),
                        )
                    }
                    val deckTakePhoto = androidx.activity.compose.rememberLauncherForActivityResult(
                        androidx.activity.result.contract.ActivityResultContracts.TakePicture()
                    ) { ok -> if (ok) vm.scanDeckPhoto(deck.id, context, deckPhotoUri) }
                    // Rychlé akce
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!format.isLimited) androidx.compose.material3.Button(
                            onClick = { showAddCards = true },
                            modifier = Modifier.weight(1f),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp),
                        ) { Icon(Icons.Filled.Add, null, Modifier.size(18.dp)); Text(tr(" Přidat"), maxLines = 1) }
                        if (!format.isLimited) androidx.compose.material3.OutlinedButton(
                            onClick = { deckTakePhoto.launch(deckPhotoUri) },
                            enabled = !poolScanBusy,
                            modifier = Modifier.weight(1f),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp),
                        ) { Icon(Icons.Filled.PhotoCamera, null, Modifier.size(18.dp)); Text(" Vyfotit") }
                        androidx.compose.material3.OutlinedButton(
                            onClick = { shareDecklist(context, deck.name, flatCards) },
                            modifier = Modifier.weight(1f),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp),
                        ) { Icon(Icons.Filled.Share, null, Modifier.size(18.dp)); Text(" Export") }
                    }
                    if (!format.isLimited && poolScanStatus != null) Row(verticalAlignment = Alignment.CenterVertically) {
                        if (poolScanBusy) androidx.compose.material3.CircularProgressIndicator(Modifier.padding(end = 8.dp).size(16.dp), strokeWidth = 2.dp)
                        Text(poolScanStatus!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
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
            IconButton(onClick = { settings.setDeckGrid(!gridMode) }) {
                Icon(
                    if (gridMode) Icons.AutoMirrored.Filled.ViewList else Icons.Filled.GridView,
                    contentDescription = if (gridMode) tr("Seznam") else tr("Mřížka"),
                )
            }
            androidx.compose.material3.BadgedBox(
                badge = {
                    if (deckFilter.isActive) androidx.compose.material3.Badge { Text("${deckFilter.activeCount}") }
                },
            ) {
                IconButton(onClick = { showTypeFilter = true }) {
                    Icon(
                        Icons.Filled.FilterList,
                        contentDescription = tr("Filtr"),
                        tint = if (deckFilter.isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                    )
                }
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
        val othersLabel = if (allMode) tr("Vše") else "Spell"
        val showBasicPanel = sectionMode != DeckSectionMode.OTHERS
        // Výchozí: rozbalené, dokud v balíku nejsou žádné základní země
        val basicsExpanded = basicsOpen ?: deckCards.none { isBasicLand(it.card) }
        val deckColorSet = deckCards.filter { !isLandCard(it.card) }
            .flatMap { com.example.antimarcipan.decks.LimitedAdvisor.colors(it.card) }.toSet()
            .ifEmpty { if (deckCards.any { !isLandCard(it.card) }) setOf("C") else emptySet() }
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

        if (deckCards.any { !isLandCard(it.card) }) com.example.antimarcipan.ui.CurveBlind(deckCards)
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
            if (showBasicPanel) BasicLandPanel(basicCounts, onBasicInc, onBasicDec, basicsExpanded, { basicsOpen = !basicsExpanded }, deckColorSet)
            androidx.compose.runtime.CompositionLocalProvider(com.example.antimarcipan.ui.LocalCardList provides ((lands + others).map { it.card }), com.example.antimarcipan.ui.LocalCardRemoveAction provides (tr("Odebrat z balíku") to { c: CollectionCard -> pendingRemove = c })) {
            DeckGrid(
                lands = lands,
                others = others,
                emptyDeck = deckCards.isEmpty(),
                othersLabel = othersLabel,
            )
            }
            return@Column
        }

        androidx.compose.runtime.CompositionLocalProvider(com.example.antimarcipan.ui.LocalCardList provides ((lands + others).map { it.card } + poolRows.map { it.card }), com.example.antimarcipan.ui.LocalCardRemoveAction provides (tr("Odebrat z balíku") to { c: CollectionCard -> pendingRemove = c })) {
        LazyColumn(Modifier.fillMaxSize()) {
            if (deckCards.isEmpty()) {
                item {
                    Text(
                        tr("Zatím prázdný — přidej dole ze sbírky, nebo základní země přes menu."),
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
                item { BasicLandPanel(basicCounts, onBasicInc, onBasicDec, basicsExpanded, { basicsOpen = !basicsExpanded }, deckColorSet) }
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
                        tr("Filtru neodpovídá žádná karta."),
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
                        tr("Pool je prázdný. V rádci nahoře ťukni na „Vyfotit karty“ a vyfoť karty z boosterů."),
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
                            Icon(Icons.Filled.Delete, contentDescription = tr("Odebrat z poolu"))
                        }
                        IconButton(onClick = { addFromPool(row.card) }, enabled = free > 0) {
                            Icon(Icons.Filled.Add, contentDescription = tr("Do balíku"))
                        }
                    }
                    HorizontalDivider()
                }
            }
            if (!format.isLimited) item { SectionHeader(tr("Přidat ze sbírky")) }
            val inDeckIds = deckCards.map { it.card.scryfallId }.toSet()
            val addable = if (format.isLimited) emptyList() else collection.filter { it.scryfallId !in inDeckIds }
            if (addable.isEmpty() && !format.isLimited) {
                item {
                    Text(
                        tr("Všechny karty ze sbírky už jsou v balíku (nebo je sbírka prázdná)."),
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
                        Icon(Icons.Filled.Add, contentDescription = tr("Přidat"))
                    }
                }
                HorizontalDivider()
            }
        }
        }
    }

    if (showRename) {
        DeckNameDialog(
            title = tr("Název a barvy"),
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

    if (showAddCards) AddCardsDialog(
        collection = collection,
        inDeck = deckCards.associate { it.card.scryfallId to it.deckQuantity },
        onAdd = { vm.addCard(it) },
        onDismiss = { showAddCards = false },
    )
    pendingPoolRemove?.let { (card, q) ->
        ConfirmDialog(
            title = tr("Odebrat z poolu?"),
            message = "Odebrat 1 kus „${card.name}“ z poolu?",
            confirmText = tr("Odebrat"),
            onConfirm = { vm.setPoolQty(deck.id, card.scryfallId, q - 1); pendingPoolRemove = null },
            onDismiss = { pendingPoolRemove = null },
        )
    }
    pendingRemove?.let { card ->
        ConfirmDialog(
            title = tr("Odebrat z balíku?"),
            message = tr("Odebrat „{0}“ z balíku? Karta zůstane ve sbírce.", (card.name)),
            confirmText = tr("Odebrat"),
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

internal fun shareDecklist(context: Context, deckName: String, cards: List<CollectionCard>) {
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
        Intent.createChooser(intent, tr("Sdílet decklist")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

@Composable
internal fun DeckStat(label: String, n: Int) {
    Column {
        Text("$n", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
    }
}

/** Rychlé přidání karet ze sbírky do balíku: hledání + „+“. */
@Composable
internal fun AddCardsDialog(
    collection: List<CollectionCard>,
    inDeck: Map<String, Int>,
    onAdd: (CollectionCard) -> Unit,
    onDismiss: () -> Unit,
) {
    var q by remember { mutableStateOf("") }
    val shown = collection.filter { q.isBlank() || it.name.contains(q.trim(), true) || (it.typeLine ?: "").contains(q.trim(), true) }
        .sortedBy { it.name.lowercase() }.take(80)
    com.example.antimarcipan.ui.GoldDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Přidat kartu ze sbírky")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                androidx.compose.material3.OutlinedTextField(
                    q, { q = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    label = { Text(tr("Hledat jméno nebo typ")) },
                )
                if (shown.isEmpty()) Text(tr("Nic nenalezeno."), style = MaterialTheme.typography.bodySmall)
                shown.forEach { c ->
                    val n = inDeck[c.scryfallId] ?: 0
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        com.example.antimarcipan.ui.InteractiveThumb(c, width = 34.dp, height = 47.dp)
                        Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                            Text(c.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                (c.typeLine?.substringBefore("—")?.trim() ?: "") + if (n > 0) tr(" · v balíku {0}×", n) else "",
                                style = MaterialTheme.typography.labelSmall, maxLines = 1,
                                color = if (n > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                        }
                        IconButton(onClick = { onAdd(c) }) { Icon(Icons.Filled.Add, contentDescription = tr("Přidat"), tint = MaterialTheme.colorScheme.primary) }
                    }
                }
                if (shown.size == 80) Text(tr("Zobrazeno prvních 80 — upřesni hledání."), style = MaterialTheme.typography.labelSmall)
            }
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text(tr("Hotovo")) } },
    )
}
