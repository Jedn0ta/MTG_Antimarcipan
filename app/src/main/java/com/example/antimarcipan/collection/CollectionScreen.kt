package com.example.antimarcipan.collection

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.antimarcipan.R
import com.example.antimarcipan.data.local.CollectionCard
import com.example.antimarcipan.data.thumbModel
import com.example.antimarcipan.ui.AppViewModelFactory
import com.example.antimarcipan.ui.CardThumb
import com.example.antimarcipan.ui.ConfirmDialog
import com.example.antimarcipan.ui.DeckPickerDialog
import com.example.antimarcipan.ui.EmptyState
import com.example.antimarcipan.ui.ManaCost

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
    val selection by vm.selection.collectAsStateWithLifecycle()
    val decks by vm.decks.collectAsStateWithLifecycle()
    val sets by vm.availableSets.collectAsStateWithLifecycle()

    var showAdvanced by remember { mutableStateOf(false) }
    var showDeckPicker by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<CollectionCard?>(null) }

    Column(Modifier.fillMaxSize()) {
        // Hledání
        OutlinedTextField(
            value = filter.query,
            onValueChange = vm::setQuery,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            placeholder = { Text("Hledat podle jména nebo textu…") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
        )

        // Barvy — vždy viditelné, ikony many
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
            Box(Modifier.weight(1f))
            TextButton(onClick = { showAdvanced = !showAdvanced }) {
                Icon(
                    Icons.Filled.FilterList,
                    contentDescription = null,
                    tint = if (filter.isActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                )
                Text(if (showAdvanced) " Méně" else " Filtry")
            }
        }

        AnimatedVisibility(visible = showAdvanced) {
            AdvancedFilters(filter, vm, sets)
        }

        SortRow(filter, vm)

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
                    SwipeToDelete(onRequestDelete = { pendingDelete = card }) {
                        CollectionRow(
                            card = card,
                            selected = selection.contains(card.scryfallId),
                            onToggleSelect = { vm.toggleSelect(card.scryfallId) },
                            onRemove = { pendingDelete = card },
                        )
                    }
                    HorizontalDivider()
                }
            }
        }

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
                    Toast.makeText(context, "Přidáno $n karet do „${deck.name}“", Toast.LENGTH_SHORT).show()
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

@Composable
private fun SwipeToDelete(onRequestDelete: () -> Unit, content: @Composable () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onRequestDelete()
            }
            false // nikdy nemizí rovnou — čeká na potvrzení v dialogu
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier.fillMaxWidth().padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, tint = Color(0xFFD95C5C))
                    Text("Smazat", color = Color(0xFFD95C5C))
                }
            }
        },
        content = { content() },
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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

        // Náhled + tooltip s textem karty na podržení
        val tooltipState = rememberTooltipState(isPersistent = true)
        TooltipBox(
            positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
            tooltip = {
                RichTooltip(
                    title = { Text(card.name) },
                    text = {
                        Text(
                            buildString {
                                card.manaCost?.takeIf { it.isNotBlank() }?.let { appendLine(it) }
                                card.typeLine?.let { appendLine(it) }
                                card.oracleText?.let { append(it) }
                            }.ifBlank { "Bez textu." }
                        )
                    },
                )
            },
            state = tooltipState,
        ) {
            CardThumb(card.thumbModel(), width = 60.dp, height = 84.dp)
        }

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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    card.typeLine ?: "",
                    Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                ManaCost(card.manaCost, symbolSize = 15)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                card.rarity?.let {
                    Text(it.replaceFirstChar { ch -> ch.uppercase() }, style = MaterialTheme.typography.labelSmall)
                }
                card.priceUsd?.let { Text("$$it", style = MaterialTheme.typography.labelSmall) }
                Text("ks: ${card.quantity}", style = MaterialTheme.typography.labelSmall)
            }
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Delete, contentDescription = "Odebrat")
        }
    }
}
