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
    // Otevření balíku z Rádce (navržený / limited balík)
    val openReq by com.example.antimarcipan.advisor.AppNav.openDeck.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(openReq) {
        openReq?.let { vm.select(it); com.example.antimarcipan.advisor.AppNav.openDeck.value = null }
    }

    if (selected == null) {
        DeckListView(vm)
    } else {
        DeckDetailView(vm, selected!!)
    }
}

// ---------------------------------------------------------------- list / load

@Composable
internal fun DeckListView(vm: DeckViewModel) {
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
                Text(tr(" Nový balík"))
            }
        }
        HorizontalDivider()

        if (decks.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    tr("Zatím žádný balík. Vytvoř první tlačítkem „Nový“."),
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
            title = tr("Smazat balík?"),
            message = tr("Opravdu smazat balík „{0}“? Karty zůstanou ve sbírce.", (deck.name)),
            confirmText = tr("Smazat"),
            onConfirm = { vm.deleteDeck(deck.id); pendingDeleteDeck = null },
            onDismiss = { pendingDeleteDeck = null },
        )
    }
}

@Composable
internal fun produceDeckColors(vm: DeckViewModel, deckId: Long): Set<String> {
    val state = androidx.compose.runtime.produceState(initialValue = emptySet<String>(), deckId) {
        value = vm.deckColors(deckId)
    }
    return state.value
}

@Composable
internal fun DeckListRow(
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
            Icon(Icons.Filled.Delete, contentDescription = tr("Smazat balík"))
        }
    }
}

@Composable
internal fun FormatDialog(
    current: DeckFormat,
    onDismiss: () -> Unit,
    onPick: (DeckFormat) -> Unit,
) {
    com.example.antimarcipan.ui.GoldDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Formát balíku")) },
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
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Zavřít")) } },
    )
}

@Composable
internal fun NewDeckDialog(
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
        title = { Text(tr("Nový balík")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(tr("Název balíku")) },
                    singleLine = true,
                )
                Text(tr("Formát"), style = MaterialTheme.typography.labelMedium)
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
                            tr("Sealed: otevři 6 boosterů a v balíku je přes „Vyfotit karty“ vyfoť do poolu (ne do sbírky). Rádce v balíku pak doporučí barvy, nejlepší karty a rozpis 17 zemí.")
                        else
                            tr("Draft: z každého boosteru si vybereš 1 kartu a zbytek posíláš dál. Vydraftované karty vyfoť přímo v balíku přes „Vyfotit karty“, rádce pomůže se stavbou. Do sbírky je přidáš hromadně až na konci."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    TextButton(onClick = { guideFor = format }) { Text(tr("Jak se {0} hraje a staví", (format.label))) }
                }
                Text(tr("Barvy balíku"), style = MaterialTheme.typography.labelMedium)
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
            TextButton(onClick = { onConfirm(name, format, serializeColors(colors)) }) { Text(tr("Vytvořit")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Zrušit")) } },
    )
}

internal fun serializeColors(colors: List<String>): String? =
    WUBRG.filter { it in colors }.joinToString("").ifEmpty { null }

@Composable
internal fun ColorPicker(selected: Set<String>, onToggle: (String) -> Unit) {
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
internal fun DeckNameDialog(
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
                    label = { Text(tr("Název balíku")) },
                    singleLine = true,
                )
                Text(tr("Barvy balíku"), style = MaterialTheme.typography.labelMedium)
                ColorPicker(colors.toSet()) { c ->
                    if (colors.contains(c)) colors.remove(c) else colors.add(c)
                }
                comboName(colors.toSet())?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(name, serializeColors(colors)) }) { Text(tr("Uložit")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Zrušit")) } },
    )
}
