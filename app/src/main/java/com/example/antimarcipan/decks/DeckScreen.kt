package com.example.antimarcipan.decks

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Style
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.antimarcipan.data.local.CollectionCard
import com.example.antimarcipan.data.thumbModel
import com.example.antimarcipan.data.local.Deck
import com.example.antimarcipan.ui.AppViewModelFactory
import com.example.antimarcipan.ui.CardThumb
import com.example.antimarcipan.ui.ConfirmDialog
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
        ) {
            Text("Moje balíky", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            Button(onClick = { showNewDeck = true }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(" Nový")
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
                        onOpen = { vm.select(deck.id) },
                        onDelete = { pendingDeleteDeck = deck },
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (showNewDeck) {
        DeckNameDialog(
            title = "Nový balík",
            initial = "",
            onDismiss = { showNewDeck = false },
            onConfirm = { name ->
                showNewDeck = false
                vm.createDeck(name)
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
private fun DeckListRow(deck: Deck, onOpen: () -> Unit, onDelete: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onOpen() }.padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DeckCover(deck.coverImagePath, size = 56.dp)
        Column(Modifier.weight(1f)) {
            Text(deck.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                deck.commanderName?.let { "Velitel: $it" } ?: "Bez velitele",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
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

@Composable
private fun DeckDetailView(vm: DeckViewModel, deck: Deck) {
    val context = LocalContext.current
    val deckCards by vm.selectedDeckCards.collectAsStateWithLifecycle()
    val collection by vm.collection.collectAsStateWithLifecycle()

    var showRename by remember { mutableStateOf(false) }
    var pendingRemove by remember { mutableStateOf<CollectionCard?>(null) }

    val coverPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) vm.setCover(deck.id, uri) }

    Column(Modifier.fillMaxSize()) {
        // Hlavička balíku
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = vm::closeDeck) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět na seznam")
            }
            DeckCover(deck.coverImagePath, size = 64.dp)
            Column(Modifier.weight(1f)) {
                Text(deck.name, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    "Velitel: ${deck.commanderName ?: "— nenastaven —"} · ${deckCards.size} karet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
        }

        // Akce
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { showRename = true },
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(" Název", maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelLarge)
            }
            OutlinedButton(
                onClick = {
                    coverPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
            ) {
                Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(" Náhled", maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelLarge)
            }
            OutlinedButton(
                onClick = { shareDecklist(context, deck.name, deckCards) },
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(" Export", maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelLarge)
            }
        }

        val duplicates = DeckDuplicates.duplicateNames(deckCards)
        if (duplicates.isNotEmpty()) {
            DuplicateWarning(duplicates)
        }

        LazyColumn(Modifier.fillMaxSize()) {
            item { SectionHeader("Karty v balíku — swipe doleva odebere") }
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
                SwipeToRemove(onRequestRemove = { pendingRemove = card }) {
                    DeckCardRow(
                        card = card,
                        isCommander = card.scryfallId == deck.commanderScryfallId,
                        onCommander = { vm.setCommander(card) },
                        onRemove = { pendingRemove = card },
                    )
                }
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
                    CardThumb(card.thumbModel())
                    Text(card.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
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
        .map { (name, group) -> "${group.sumOf { it.quantity }} $name" }
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
