package com.example.antimarcipan.decks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.antimarcipan.data.local.CollectionCard
import com.example.antimarcipan.ui.AppViewModelFactory
import com.example.antimarcipan.ui.CardThumb
import com.example.antimarcipan.ui.EmptyState
import com.example.antimarcipan.ui.ScreenTitle

@Composable
fun DeckScreen(
    vm: DeckViewModel = viewModel(factory = AppViewModelFactory),
) {
    val decks by vm.decks.collectAsStateWithLifecycle()
    val selected by vm.selectedDeck.collectAsStateWithLifecycle()
    val deckCards by vm.selectedDeckCards.collectAsStateWithLifecycle()
    val collection by vm.collection.collectAsStateWithLifecycle()

    var showNewDeck by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        ScreenTitle("Balíky")

        // Deck selector chips
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            decks.forEach { deck ->
                FilterChip(
                    selected = selected?.id == deck.id,
                    onClick = { vm.select(deck.id) },
                    label = { Text(deck.name) },
                )
            }
            AssistChip(
                onClick = { showNewDeck = true },
                label = { Text("Nový") },
                leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
            )
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        val deck = selected
        if (deck == null) {
            EmptyState(
                "Vyber nebo založ balík",
                "Pak do něj přidáš karty ze sbírky a nastavíš velitele.",
            )
        } else {
            Text(
                "Velitel: ${deck.commanderName ?: "— nenastaven —"}",
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                "V balíku: ${deckCards.sumOf { it.quantity }} karet",
                Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodySmall,
            )

            LazyColumn(Modifier.fillMaxSize()) {
                item {
                    SectionHeader("Karty v balíku")
                }
                if (deckCards.isEmpty()) {
                    item {
                        Text(
                            "Zatím prázdný — přidej dole ze sbírky.",
                            Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                items(deckCards, key = { "in-" + it.scryfallId }) { card ->
                    DeckCardRow(
                        card = card,
                        isCommander = card.scryfallId == deck.commanderScryfallId,
                        onCommander = { vm.setCommander(card) },
                        onRemove = { vm.removeCard(card) },
                    )
                    HorizontalDivider()
                }

                item { SectionHeader("Přidat ze sbírky") }
                val inDeckIds = deckCards.map { it.scryfallId }.toSet()
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
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CardThumb(card.imageUrl)
                        Text(card.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        IconButton(onClick = { vm.addCard(card) }) {
                            Icon(Icons.Filled.Add, contentDescription = "Přidat")
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }

    if (showNewDeck) {
        NewDeckDialog(
            onDismiss = { showNewDeck = false },
            onCreate = { name ->
                vm.createDeck(name)
                showNewDeck = false
            },
        )
    }
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
    onCommander: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CardThumb(card.imageUrl)
        Column(Modifier.weight(1f)) {
            Text(card.name, style = MaterialTheme.typography.bodyMedium)
            Text(
                card.typeLine ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
        IconButton(onClick = onCommander) {
            Icon(
                Icons.Filled.Star,
                contentDescription = "Nastavit jako velitele",
                tint = if (isCommander) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            )
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Delete, contentDescription = "Odebrat z balíku")
        }
    }
}

@Composable
private fun NewDeckDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nový balík") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Název balíku") },
                singleLine = true,
            )
        },
        confirmButton = { TextButton(onClick = { onCreate(name) }) { Text("Vytvořit") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Zrušit") } },
    )
}
