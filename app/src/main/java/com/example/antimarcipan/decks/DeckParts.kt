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

/** Dlouhé podržení → akce (ignoruje swipe/zrušení). */
internal fun Modifier.longHold(durationMs: Long, onHold: () -> Unit): Modifier =
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
internal val MAIN_TYPES = listOf(
    "Creature", "Instant", "Sorcery", "Artifact", "Enchantment",
    "Planeswalker", "Land", "Battle",
)

internal enum class DeckSectionMode(val label: String) {
    ALL(tr("Vše")), LANDS("Land"), OTHERS("Spell")
}

// ---- Barvy balíku a název barevné kombinace ----

internal val WUBRG = listOf("W", "U", "B", "R", "G")

internal val COLOR_CZ = mapOf("W" to tr("bílé"), "U" to tr("modré"), "B" to tr("černé"), "R" to tr("červené"), "G" to tr("zelené"))

internal val COMBO_NAMES = mapOf(
    "W" to tr("Bílá"), "U" to tr("Modrá"), "B" to tr("Černá"), "R" to tr("Červená"), "G" to tr("Zelená"), "C" to tr("Bezbarvá"),
    "WU" to "Azorius", "WB" to "Orzhov", "WR" to "Boros", "WG" to "Selesnya",
    "UB" to "Dimir", "UR" to "Izzet", "UG" to "Simic",
    "BR" to "Rakdos", "BG" to "Golgari", "RG" to "Gruul",
    "WUB" to "Esper", "WUR" to "Jeskai", "WUG" to "Bant", "WBR" to "Mardu", "WBG" to "Abzan",
    "WRG" to "Naya", "UBR" to "Grixis", "UBG" to "Sultai", "URG" to "Temur", "BRG" to "Jund",
    "WUBRG" to tr("Pětibarevný (WUBRG)"),
)

/** "WUB" → {W,U,B}; filtruje jen platné znaky many. */
internal fun parseDeckColors(colors: String?): Set<String> =
    colors?.uppercase()?.toCharArray()?.map { it.toString() }
        ?.filter { it in WUBRG || it == "C" }?.toSet() ?: emptySet()

/** Název barevné kombinace (Azorius, Jeskai…), nebo null když se nehodí. */
internal fun comboName(colors: Set<String>): String? {
    val cols = WUBRG.filter { it in colors }
    if (cols.isEmpty()) return if ("C" in colors) tr("Bezbarvá") else null
    val key = cols.joinToString("")
    COMBO_NAMES[key]?.let { return it }
    if (cols.size == 4) {
        val missing = WUBRG.first { it !in colors }
        return tr("Čtyřbarevný (bez {0})", (COLOR_CZ[missing]))
    }
    return null
}

internal fun formatIcon(format: DeckFormat): androidx.compose.ui.graphics.vector.ImageVector =
    when (format) {
        DeckFormat.COMMANDER -> Icons.Filled.Star
        DeckFormat.SINGLETON -> Icons.Filled.LooksOne
        DeckFormat.STANDARD -> Icons.Filled.EmojiEvents
        DeckFormat.DRAFT -> Icons.Filled.SwapHoriz
        DeckFormat.SEALED -> Icons.Filled.Inventory2
        DeckFormat.FREEFORM -> Icons.Filled.AllInclusive
    }

// ---- Kontextový filtr balíku (select-list) ----

internal val COLOR_LABELS = linkedMapOf(
    "W" to tr("Bílá"), "U" to tr("Modrá"), "B" to tr("Černá"), "R" to tr("Červená"), "G" to tr("Zelená"), "C" to tr("Bezbarvá"),
)

internal val MANA_BUCKETS = listOf("0–1", "2–3", "4–5", "6+")

internal fun creatureSubtypesOf(card: CollectionCard): List<String> {
    val line = card.typeLine ?: return emptyList()
    if (!line.contains("Creature", ignoreCase = true)) return emptyList()
    return line.substringAfter("—", "").trim().split(" ")
        .map { it.trim() }.filter { it.length > 2 }
}

internal fun manaBucketOf(cmc: Double?): String {
    val v = (cmc ?: 0.0)
    return when {
        v <= 1 -> "0–1"
        v <= 3 -> "2–3"
        v <= 5 -> "4–5"
        else -> "6+"
    }
}

internal fun matchDeckValue(card: CollectionCard, cat: String, value: String): Boolean = when (cat) {
    tr("Typ") -> card.typeLine?.contains(value, ignoreCase = true) == true
    "Tribal" -> creatureSubtypesOf(card).any { it.equals(value, ignoreCase = true) }
    tr("Barva") -> card.colorIdentityList().contains(value)
    "Mana" -> manaBucketOf(card.cmc) == value
    else -> true
}

@Composable
internal fun DeckGrid(
    lands: List<com.example.antimarcipan.data.local.DeckCardWithQty>,
    others: List<com.example.antimarcipan.data.local.DeckCardWithQty>,
    emptyDeck: Boolean,
    othersLabel: String = "Spell",
) {
    if (emptyDeck) {
        Text(
            tr("Zatím prázdný — přepni na seznam a přidej karty ze sbírky."),
            Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodySmall,
        )
        return
    }
    val orderedLands = lands.sortedBy { if (isBasicLand(it.card)) 0 else 1 }
    LazyVerticalGrid(
        columns = GridCells.Adaptive((84 * com.example.antimarcipan.ui.LocalThumbScale.current.grid).dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (orderedLands.isNotEmpty()) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                SectionHeader(tr("Ostatní země ({0})", (orderedLands.sumOf { it.deckQuantity })))
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
internal fun DeckGridCell(entry: com.example.antimarcipan.data.local.DeckCardWithQty) {
    Box(contentAlignment = Alignment.TopEnd) {
        val g = com.example.antimarcipan.ui.LocalThumbScale.current.grid
        com.example.antimarcipan.ui.InteractiveThumb(card = entry.card, width = (84 * g).dp, height = (117 * g).dp)
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
internal val BASIC_TYPES = listOf(
    "Plains" to "W", "Island" to "U", "Swamp" to "B", "Mountain" to "R", "Forest" to "G", "Wastes" to "C",
)

/** „Snow-Covered Forest“ → „Forest“. */
internal fun basicBaseName(name: String): String =
    name.removePrefix("Snow-Covered ").removePrefix("snow-covered ").trim()

/**
 * Panel základních zemí: dlaždice se symbolem many, počtem a −/+.
 * Ťuknutí na symbol přidá jeden kus.
 */
@Composable
internal fun BasicLandPanel(
    counts: Map<String, Int>,
    onInc: (String) -> Unit,
    onDec: (String) -> Unit,
    expanded: Boolean = true,
    onToggle: () -> Unit = {},
    deckColors: Set<String> = emptySet(),
) {
    var showAll by remember { mutableStateOf(false) }
    // Chytré zobrazení: jen země v barvách balíku nebo ty, které už v balíku jsou
    val relevant = BASIC_TYPES.filter { (name, mana) -> (counts[name] ?: 0) > 0 || mana in deckColors }
    val types = if (showAll || relevant.isEmpty()) BASIC_TYPES else relevant
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, com.example.antimarcipan.ui.GoldBorderBrush),
    ) {
        Column(Modifier.padding(horizontal = 6.dp, vertical = 10.dp)) {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(start = 6.dp, end = 6.dp, bottom = if (expanded) 6.dp else 0.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    tr("Základní země · {0}", (counts.values.sum())),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (!expanded) {
                    // Sbalené: jen nenulové počty se symboly
                    BASIC_TYPES.filter { (counts[it.first] ?: 0) > 0 }.forEach { (name, mana) ->
                        androidx.compose.foundation.Image(
                            androidx.compose.ui.res.painterResource(manaRes(mana)), contentDescription = name,
                            modifier = Modifier.padding(start = 8.dp).size(16.dp),
                        )
                        Text(" ${counts[name]}", style = MaterialTheme.typography.labelMedium)
                    }
                }
                Spacer(Modifier.weight(1f))
                Text(if (expanded) "▲" else "▼", color = MaterialTheme.colorScheme.primary)
            }
            if (expanded) Row(Modifier.fillMaxWidth()) {
                types.forEach { (name, mana) ->
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
            if (expanded && relevant.isNotEmpty() && relevant.size < BASIC_TYPES.size) {
                Text(
                    if (showAll) tr("Jen barvy balíku") else tr("Ukázat všechny základní země"),
                    Modifier.padding(start = 6.dp, top = 4.dp).clickable { showAll = !showAll },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

internal val BASIC_LANDS = setOf(
    "plains", "island", "swamp", "mountain", "forest", "wastes",
    "snow-covered plains", "snow-covered island", "snow-covered swamp",
    "snow-covered mountain", "snow-covered forest",
)

internal fun isLandCard(card: CollectionCard): Boolean =
    card.typeLine?.contains("Land", ignoreCase = true) == true

internal fun isBasicLand(card: CollectionCard): Boolean {
    val n = card.name.lowercase().substringBefore("//").trim()
    if (n in BASIC_LANDS) return true
    return card.typeLine?.contains("Basic", ignoreCase = true) == true &&
        card.typeLine.contains("Land", ignoreCase = true)
}

@Composable
internal fun DeckEntryRow(
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
        if (showCommander && !isBasic) {
            IconButton(onClick = onCommander, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = tr("Velitel"),
                    tint = if (isCommander) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                )
            }
        }
        // Rulings (ťuk) / Gatherer (podržení)
        com.example.antimarcipan.ui.GathererRulingsButton(entry.card.scryfallId, entry.card.name, entry.card.gathererUrl, iconSize = 20.dp)
        // Akce s kartou
        var menu by remember { mutableStateOf(false) }
        var detail by remember { mutableStateOf(false) }
        Box {
            IconButton(onClick = { menu = true }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.MoreVert, contentDescription = tr("Možnosti karty"))
            }
            androidx.compose.material3.DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                Text(
                    "${entry.card.name} · ${entry.deckQuantity}×",
                    Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
                )
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(tr("Přidat kus (+1)")) },
                    leadingIcon = { Icon(Icons.Filled.Add, null) },
                    onClick = { onChangeQty(entry.deckQuantity + 1) },
                )
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text("Ubrat kus (−1)") },
                    leadingIcon = { Icon(Icons.Filled.Remove, null) },
                    enabled = entry.deckQuantity > 1,
                    onClick = { onChangeQty(entry.deckQuantity - 1) },
                )
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(tr("Detail karty")) },
                    leadingIcon = { Icon(Icons.Filled.Info, null) },
                    onClick = { menu = false; detail = true },
                )
                if (showCommander && !isBasic) androidx.compose.material3.DropdownMenuItem(
                    text = { Text(if (isCommander) "Je velitel ✓" else tr("Nastavit jako velitele")) },
                    leadingIcon = { Icon(Icons.Filled.Star, null) },
                    enabled = !isCommander,
                    onClick = { menu = false; onCommander() },
                )
                androidx.compose.material3.HorizontalDivider()
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(tr("Odebrat z balíku"), color = Color(0xFFFF8A8A)) },
                    leadingIcon = { Icon(Icons.Filled.Delete, null, tint = Color(0xFFFF8A8A)) },
                    onClick = { menu = false; onLongRemove() },
                )
            }
        }
        if (detail) com.example.antimarcipan.ui.CardInfoDialog(entry.card) { detail = false }
    }
}

@Composable
internal fun BasicLandsDialog(
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
        title = { Text(tr("Přidat základní země")) },
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
                    tr("Nastavuje celkový počet dané základní země v balíku."),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(counts.toMap()) }) { Text(tr("Přidat")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Zrušit")) } },
    )
}

internal fun manaRes(c: String): Int = when (c) {
    "W" -> com.example.antimarcipan.R.drawable.mana_w
    "U" -> com.example.antimarcipan.R.drawable.mana_u
    "B" -> com.example.antimarcipan.R.drawable.mana_b
    "R" -> com.example.antimarcipan.R.drawable.mana_r
    "G" -> com.example.antimarcipan.R.drawable.mana_g
    else -> com.example.antimarcipan.R.drawable.mana_c
}

@Composable
internal fun SectionHeader(text: String) {
    com.example.antimarcipan.ui.OrnamentHeader(text)
}
