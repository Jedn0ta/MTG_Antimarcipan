package com.example.antimarcipan.collection

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.antimarcipan.data.local.CollectionCard
import com.example.antimarcipan.ui.AppViewModelFactory
import com.example.antimarcipan.ui.CardThumb
import com.example.antimarcipan.ui.DeckPickerDialog
import com.example.antimarcipan.ui.EmptyState

@Composable
fun CollectionScreen(
    vm: CollectionViewModel = viewModel(factory = AppViewModelFactory),
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val cards by vm.cards.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val selection by vm.selection.collectAsStateWithLifecycle()
    val decks by vm.decks.collectAsStateWithLifecycle()

    var showFilters by remember { mutableStateOf(false) }
    var showDeckPicker by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        // Hledání
        OutlinedTextField(
            value = filter.query,
            onValueChange = vm::setQuery,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            placeholder = { Text("Hledat podle jména nebo textu…") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { showFilters = !showFilters }) {
                    Icon(
                        Icons.Filled.FilterList,
                        contentDescription = "Filtry",
                        tint = if (filter.isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                    )
                }
            },
        )

        AnimatedVisibility(visible = showFilters) {
            FilterPanel(filter, vm)
        }

        // Řádek řazení + počet
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Řadit:", style = MaterialTheme.typography.labelMedium)
            SortBy.entries.forEach { s ->
                val active = filter.sortBy == s
                FilterChip(
                    selected = active,
                    onClick = { vm.setSort(s) },
                    label = {
                        Text(s.label + if (active) (if (filter.sortDesc) " ↓" else " ↑") else "")
                    },
                )
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
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(cards, key = { it.scryfallId }) { card ->
                    SwipeToDelete(onDelete = { vm.remove(card.scryfallId) }) {
                        CollectionRow(
                            card = card,
                            selected = selection.contains(card.scryfallId),
                            onToggleSelect = { vm.toggleSelect(card.scryfallId) },
                            onRemove = { vm.remove(card.scryfallId) },
                        )
                    }
                    HorizontalDivider()
                }
            }
        }

        // Akční lišta výběru
        if (selection.isNotEmpty()) {
            Surface(tonalElevation = 3.dp) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Vybráno ${selection.size}", Modifier.weight(1f))
                    TextButton(onClick = vm::clearSelection) { Text("Zrušit") }
                    TextButton(onClick = { showDeckPicker = true }) { Text("Do balíku") }
                }
            }
        }
    }

    if (showDeckPicker) {
        DeckPickerDialog(
            decks = decks,
            onDismiss = { showDeckPicker = false },
            onPick = { deck ->
                showDeckPicker = false
                vm.addSelectionToDeck(deck.id) { n ->
                    Toast.makeText(
                        context,
                        "Přidáno $n karet do „${deck.name}“",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            },
        )
    }
}

@Composable
private fun FilterPanel(filter: CollectionFilter, vm: CollectionViewModel) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        FilterRow("Barvy") {
            CardColor.entries.forEach { c ->
                FilterChip(
                    selected = filter.colors.contains(c),
                    onClick = { vm.toggleColor(c) },
                    label = { Text(c.code) },
                )
            }
        }
        FilterRow("Typ") {
            CardType.entries.forEach { t ->
                FilterChip(
                    selected = filter.types.contains(t),
                    onClick = { vm.toggleType(t) },
                    label = { Text(t.label) },
                )
            }
        }
        FilterRow("Rarita") {
            Rarity.entries.forEach { r ->
                FilterChip(
                    selected = filter.rarities.contains(r),
                    onClick = { vm.toggleRarity(r) },
                    label = { Text(r.label) },
                )
            }
        }
        FilterRow("Mana value") {
            (0..7).forEach { b ->
                FilterChip(
                    selected = filter.cmcBuckets.contains(b),
                    onClick = { vm.toggleCmc(b) },
                    label = { Text(if (b == 7) "7+" else b.toString()) },
                )
            }
        }
        if (filter.isActive) {
            TextButton(onClick = vm::clearFilters) { Text("Vymazat filtry") }
        }
    }
}

@Composable
private fun FilterRow(label: String, content: @Composable () -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) { content() }
    }
}

@Composable
private fun SwipeToDelete(onDelete: () -> Unit, content: @Composable () -> Unit) {
    val dismissState = androidx.compose.material3.rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == androidx.compose.material3.SwipeToDismissBoxValue.EndToStart) {
                onDelete(); true
            } else false
        },
    )
    androidx.compose.material3.SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            androidx.compose.foundation.layout.Box(
                Modifier.fillMaxWidth().padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color(0xFFD95C5C),
                    )
                    Text("Smazat", color = androidx.compose.ui.graphics.Color(0xFFD95C5C))
                }
            }
        },
        content = { content() },
    )
}

@Composable
private fun CollectionRow(
    card: CollectionCard,
    selected: Boolean,
    onToggleSelect: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = selected, onCheckedChange = { onToggleSelect() })
        CardThumb(card.imageUrl, width = 60.dp, height = 84.dp)
        Column(Modifier.weight(1f)) {
            Text(
                card.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                card.setName ?: card.setCode?.uppercase() ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                card.typeLine ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                card.rarity?.let {
                    Text(
                        it.replaceFirstChar { ch -> ch.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                card.priceUsd?.let {
                    Text("$$it", style = MaterialTheme.typography.labelSmall)
                }
                Text("ks: ${card.quantity}", style = MaterialTheme.typography.labelSmall)
            }
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Delete, contentDescription = "Odebrat")
        }
    }
}
