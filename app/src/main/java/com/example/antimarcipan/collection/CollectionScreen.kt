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
    val playgroundCards by vm.playground.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(0) } // 0 sbírka, 1 wishlist, 2 draft
    val showWishlist = tab == 1
    val showDraft = tab == 2
    val showPlayground = tab == 3
    val fstate by vm.cardFilter.collectAsStateWithLifecycle()
    val base = when (tab) { 1 -> wishlist; 2 -> drafts; 3 -> playgroundCards; else -> cards }
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
            com.example.antimarcipan.ui.GoldChip(
                selected = tab == 3,
                onClick = { tab = 3 },
                label = { Text("Playground") },
            )
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
            // Řazení zabere zbylé místo; ikony vpravo mají vždy plnou velikost
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                androidx.compose.material3.TextButton(
                    onClick = { showSortMenu = true },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" " + filter.sortBy.label + if (filter.sortDesc) " ↓" else " ↑", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
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
            IconButton(onClick = { settings.setCollectionGrid(!gridMode) }, modifier = Modifier.size(44.dp)) {
                Icon(if (gridMode) Icons.AutoMirrored.Filled.ViewList else Icons.Filled.GridView, contentDescription = "Zobrazení", modifier = Modifier.size(24.dp))
            }
            androidx.compose.material3.BadgedBox(badge = {
                if (fstate.isActive) androidx.compose.material3.Badge { Text("${fstate.activeCount}") }
            }) {
                IconButton(onClick = { showControls = true }, modifier = Modifier.size(44.dp)) {
                    Icon(
                        Icons.Filled.Tune, contentDescription = "Filtr", modifier = Modifier.size(24.dp),
                        tint = if (fstate.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        // Informační lišta: počet, cena a ruční aktualizace cen (ťuknutí)
        val refreshing by vm.priceRefreshing.collectAsStateWithLifecycle()
        val updatedAt by vm.priceUpdatedAt.collectAsStateWithLifecycle()
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.18f))
                .clickable(enabled = !refreshing) {
                    vm.refreshPricesNow { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("$totalQty ks", style = MaterialTheme.typography.labelLarge)
            com.example.antimarcipan.ui.totalPriceLabel(source.map { it to it.quantity.coerceAtLeast(1) })?.let {
                Text("  ·  $it", style = MaterialTheme.typography.labelLarge, color = androidx.compose.ui.graphics.Color(0xFF8FD6A0), maxLines = 1)
            }
            Spacer(Modifier.weight(1f))
            Text(
                when {
                    refreshing -> "aktualizuji…"
                    updatedAt == 0L -> "ceny neaktuální"
                    else -> "ceny " + android.text.format.DateUtils.getRelativeTimeSpanString(updatedAt, System.currentTimeMillis(), android.text.format.DateUtils.MINUTE_IN_MILLIS)
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            if (refreshing) androidx.compose.material3.CircularProgressIndicator(Modifier.padding(start = 6.dp).size(14.dp), strokeWidth = 2.dp)
            else Icon(Icons.Filled.Refresh, contentDescription = "Aktualizovat ceny", Modifier.padding(start = 6.dp).size(16.dp), tint = MaterialTheme.colorScheme.primary)
        }

        HorizontalDivider()

        // Akce podle režimu: ve sbírce swipe = do balíku, dlouhé podržení = smazat;
        // na wishlistu swipe = přesun do sbírky, dlouhé podržení = pryč z wishlistu.
        val onSwipe: (CollectionCard) -> Unit = if (showWishlist || showDraft || showPlayground) {
            { card -> vm.wishlistToCollection(card.scryfallId) {
                Toast.makeText(context, "„${card.name}“ přesunuto do sbírky", Toast.LENGTH_SHORT).show()
            } }
        } else {
            { card -> pendingAddDeck = card }
        }
        val onDelete: (CollectionCard) -> Unit = if (showPlayground) {
            { card -> vm.removeFromPlayground(card) }
        } else if (showDraft) {
            { card -> pendingDelete = card }
        } else if (showWishlist) {
            { card -> vm.removeFromWishlist(card.scryfallId) }
        } else {
            { card -> pendingDelete = card }
        }

        if (source.isEmpty()) {
            EmptyState(
                if (showPlayground && !fstate.isActive) "Playground je prázdný"
                else if (showDraft && !fstate.isActive) "Žádné rozpracované karty"
                else if (showWishlist && !fstate.isActive) "Wishlist je prázdný" else if (fstate.isActive) "Nic nenalezeno" else "Zatím prázdno",
                if (showPlayground) "Při skenování ťukni na „Playground“ — karta se sem uloží k prozkoumání."
                else if (showDraft) "Sem patří karty naskenované do poolu draftu / sealed balíku, dokud je nepřidáš do sbírky."
                else if (showWishlist) "Přidej karty na wishlist ze skenu nebo ručně."
                else if (fstate.isActive) "Zkus upravit filtr." else "Naskenuj první kartu v záložce Sken.",
            )
        } else if (gridMode) {
            androidx.compose.runtime.CompositionLocalProvider(com.example.antimarcipan.ui.LocalCardList provides (if (section == 0) source else lands + spells), com.example.antimarcipan.ui.LocalCardRemoveAction provides (when { showPlayground -> "Odebrat z playgroundu"; showWishlist -> "Odebrat z wishlistu"; showDraft -> "Smazat z draftu"; else -> "Odstranit ze sbírky" } to { c: CollectionCard -> pendingDelete = c })) {
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive((90 * com.example.antimarcipan.ui.LocalThumbScale.current.grid).dp),
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (section == 0) collectionGridSection("Vše (${source.size})", source)
                else {
                    collectionGridSection("Land (${lands.size})", lands)
                    collectionGridSection("Spell (${spells.size})", spells)
                }
            }
            }
        } else {
            androidx.compose.runtime.CompositionLocalProvider(com.example.antimarcipan.ui.LocalCardList provides (if (section == 0) source else lands + spells), com.example.antimarcipan.ui.LocalCardRemoveAction provides (when { showPlayground -> "Odebrat z playgroundu"; showWishlist -> "Odebrat z wishlistu"; showDraft -> "Smazat z draftu"; else -> "Odstranit ze sbírky" } to { c: CollectionCard -> pendingDelete = c })) {
            LazyColumn(Modifier.weight(1f)) {
                if (section == 0) collectionListSection("Vše (${source.size})", source, context, vm, onSwipe, onDelete) { pendingEdition = it }
                else {
                    collectionListSection("Land (${lands.size})", lands, context, vm, onSwipe, onDelete) { pendingEdition = it }
                    collectionListSection("Spell (${spells.size})", spells, context, vm, onSwipe, onDelete) { pendingEdition = it }
                }
            }
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
            title = if (showPlayground) "Odebrat z playgroundu?" else if (showWishlist) "Odebrat z wishlistu?" else "Smazat kartu?",
            message = if (showPlayground) "Odebrat „${card.name}“ z playgroundu?"
            else if (showWishlist) "Odebrat „${card.name}“ z wishlistu?"
            else "Opravdu odstranit „${card.name}“ ze sbírky (všechny kusy)? Tohle nejde vrátit.",
            confirmText = if (showPlayground || showWishlist) "Odebrat" else "Smazat",
            onConfirm = {
                if (showPlayground) vm.removeFromPlayground(card) else vm.remove(card.scryfallId)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}
