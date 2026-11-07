package com.example.antimarcipan.ui.filter

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.antimarcipan.R

private val COLOR_LABEL = mapOf(
    "W" to "Bílá", "U" to "Modrá", "B" to "Černá", "R" to "Červená", "G" to "Zelená", "C" to "Bezbarvá",
)
private val RARITY_LABEL = mapOf(
    "common" to "Common", "uncommon" to "Uncommon", "rare" to "Rare", "mythic" to "Mythic",
    "special" to "Special", "bonus" to "Bonus",
)

private fun manaRes(c: String): Int = when (c) {
    "W" -> R.drawable.mana_w
    "U" -> R.drawable.mana_u
    "B" -> R.drawable.mana_b
    "R" -> R.drawable.mana_r
    "G" -> R.drawable.mana_g
    else -> R.drawable.mana_c
}

/**
 * Spodní panel s kompletním filtrem karet. Rolovatelný (LazyColumn).
 * [resultCount] = kolik karet aktuálně projde filtrem.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CardFilterSheet(
    options: CardFilterOptions,
    state: CardFilterState,
    resultCount: Int,
    onChange: (CardFilterState) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        LazyColumn(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Filtr", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Odpovídá: $resultCount",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (state.isActive) {
                        TextButton(onClick = { onChange(CardFilterState()) }) {
                            Text("Zrušit vše (${state.activeCount})")
                        }
                    }
                }
            }

            // --- Hledání ---
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = { onChange(state.copy(name = it)) },
                        label = { Text("Název karty") },
                        leadingIcon = { Icon(Icons.Filled.Search, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = state.text,
                        onValueChange = { onChange(state.copy(text = it)) },
                        label = { Text("Text karty (např. „draw a card“, „sacrifice“)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // --- Barvy ---
            if (options.colors.isNotEmpty()) item {
                Section("Barva") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        options.colors.forEach { c ->
                            val on = c in state.colors
                            Image(
                                painter = painterResource(manaRes(c)),
                                contentDescription = COLOR_LABEL[c],
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .border(
                                        if (on) 2.dp else 0.dp,
                                        if (on) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        CircleShape,
                                    )
                                    .alpha(if (on) 1f else 0.4f)
                                    .clickable { onChange(state.copy(colors = state.colors.toggle(c))) },
                            )
                        }
                    }
                    if (state.colors.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ColorMode.entries.forEach { m ->
                                com.example.antimarcipan.ui.GoldChip(
                                    selected = state.colorMode == m,
                                    onClick = { onChange(state.copy(colorMode = m)) },
                                    label = { Text(m.label) },
                                )
                            }
                        }
                    }
                }
            }

            // --- Mana value ---
            item {
                Section(
                    "Mana value: ${state.cmcMin}–" +
                        if (state.cmcMax >= CardFilterState.CMC_CAP) "${CardFilterState.CMC_CAP}+" else "${state.cmcMax}"
                ) {
                    RangeSlider(
                        value = state.cmcMin.toFloat()..state.cmcMax.toFloat(),
                        onValueChange = { r ->
                            onChange(state.copy(cmcMin = r.start.toInt(), cmcMax = r.endInclusive.toInt()))
                        },
                        valueRange = 0f..CardFilterState.CMC_CAP.toFloat(),
                        steps = CardFilterState.CMC_CAP - 1,
                    )
                }
            }

            // --- Typ (málo hodnot → dlaždice) ---
            if (options.types.isNotEmpty()) item {
                Section("Typ") {
                    ChipGroup(options.types.map { it to it }, state.types) {
                        onChange(state.copy(types = state.types.toggle(it)))
                    }
                }
            }

            // --- Rarita ---
            if (options.rarities.isNotEmpty()) item {
                Section("Rarita") {
                    ChipGroup(options.rarities.map { it to (RARITY_LABEL[it] ?: it) }, state.rarities) {
                        onChange(state.copy(rarities = state.rarities.toggle(it)))
                    }
                }
            }

            // --- Select-listy (mnoho hodnot) ---
            if (options.tribes.isNotEmpty()) item {
                MultiSelectField("Tribal (typ stvoření)", options.tribes.map { it to it }, state.tribes) {
                    onChange(state.copy(tribes = it))
                }
            }
            if (options.keywords.isNotEmpty()) item {
                MultiSelectField("Vlastnosti (Deathtouch, Lifelink…)", options.keywords.map { it to it }, state.keywords) {
                    onChange(state.copy(keywords = it))
                }
            }
            if (options.sets.isNotEmpty()) item {
                MultiSelectField("Edice", options.sets.map { it.first to "${it.second} (${it.first.uppercase()})" }, state.sets) {
                    onChange(state.copy(sets = it))
                }
            }
            if (options.subtypes.isNotEmpty()) item {
                MultiSelectField("Podtyp (Equipment, Aura, Forest…)", options.subtypes.map { it to it }, state.subtypes) {
                    onChange(state.copy(subtypes = it))
                }
            }
            if (options.supertypes.isNotEmpty()) item {
                MultiSelectField("Nadtyp (Legendary, Basic, Snow…)", options.supertypes.map { it to it }, state.supertypes) {
                    onChange(state.copy(supertypes = it))
                }
            }

            item {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 24.dp),
                ) { Text("Zobrazit $resultCount") }
            }
        }
    }
}

private fun Set<String>.toggle(v: String): Set<String> = if (v in this) this - v else this + v

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipGroup(values: List<Pair<String, String>>, selected: Set<String>, onToggle: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        values.forEach { (key, label) ->
            com.example.antimarcipan.ui.GoldChip(selected = key in selected, onClick = { onToggle(key) }, label = { Text(label) })
        }
    }
}

/**
 * Select-list s vyhledáváním a výběrem více položek. Vybrané hodnoty se
 * ukazují jako odebíratelné štítky pod polem.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MultiSelectField(
    label: String,
    options: List<Pair<String, String>>,
    selected: Set<String>,
    onChange: (Set<String>) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val labelOf = options.toMap()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Surface(
            onClick = { open = true },
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (selected.isEmpty()) "Vše (${options.size} možností)" else "Vybráno: ${selected.size}",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Icon(Icons.Filled.ArrowDropDown, null)
            }
        }
        if (selected.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                selected.forEach { key ->
                    InputChip(
                        selected = true,
                        onClick = { onChange(selected - key) },
                        label = { Text(labelOf[key] ?: key) },
                        trailingIcon = { Icon(Icons.Filled.Close, null, Modifier.size(16.dp)) },
                    )
                }
            }
        }
    }

    if (open) {
        var query by remember { mutableStateOf("") }
        val shown = options.filter { query.isBlank() || it.second.contains(query.trim(), ignoreCase = true) }
        com.example.antimarcipan.ui.GoldDialog(
            onDismissRequest = { open = false },
            title = { Text(label) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Hledat…") },
                        leadingIcon = { Icon(Icons.Filled.Search, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    LazyColumn(Modifier.heightIn(max = 380.dp)) {
                        items(shown, key = { it.first }) { (key, text) ->
                            val on = key in selected
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onChange(if (on) selected - key else selected + key) }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(checked = on, onCheckedChange = { onChange(if (on) selected - key else selected + key) })
                                Text(text, style = MaterialTheme.typography.bodyMedium)
                            }
                            HorizontalDivider()
                        }
                        if (shown.isEmpty()) item {
                            Text("Nic neodpovídá.", Modifier.padding(8.dp))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { open = false }) { Text("Hotovo") } },
            dismissButton = {
                if (selected.isNotEmpty()) TextButton(onClick = { onChange(emptySet()) }) { Text("Vymazat") }
            },
        )
    }
}

