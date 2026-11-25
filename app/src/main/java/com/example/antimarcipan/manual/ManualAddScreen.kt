package com.example.antimarcipan.manual

import com.example.antimarcipan.i18n.tr

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.antimarcipan.data.scryfall.ScryfallCard
import com.example.antimarcipan.ui.AppViewModelFactory
import com.example.antimarcipan.ui.CardThumb
import com.example.antimarcipan.ui.DeckPickerDialog

@Composable
fun ManualAddScreen(
    vm: ManualAddViewModel = viewModel(factory = AppViewModelFactory),
) {
    val context = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()
    val decks by vm.decks.collectAsStateWithLifecycle()
    var deckPickerFor by remember { mutableStateOf<ScryfallCard?>(null) }
    var showImport by remember { mutableStateOf(false) }
    val importBusy by vm.importBusy.collectAsStateWithLifecycle()
    var importResult by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.query,
            onValueChange = vm::setQuery,
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            placeholder = { Text(tr("Jméno karty (i částečně)…")) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
        )
        OutlinedButton(
            onClick = { showImport = true },
            enabled = !importBusy,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        ) {
            if (importBusy) CircularProgressIndicator(Modifier.padding(end = 8.dp).size(18.dp), strokeWidth = 2.dp)
            Text(if (importBusy) tr("Importuji seznam…") else tr("Import seznamu karet (hromadně)"))
        }
        Text(
            tr("Pro karty, co nejdou naskenovat (fullarty, promo, tokeny)."),
            Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )

        if (state.loading) {
            Row(
                Modifier.fillMaxWidth().padding(24.dp),
                horizontalArrangement = Arrangement.Center,
            ) { CircularProgressIndicator() }
        }
        state.message?.let {
            Text(it, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
        }

        LazyColumn(Modifier.fillMaxSize()) {
            items(state.results, key = { it.id }) { card ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    com.example.antimarcipan.ui.ScryfallThumb(card, width = 54.dp, height = 75.dp)
                    Column(Modifier.weight(1f)) {
                        Text(
                            card.name,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            card.setName ?: card.set?.uppercase() ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            card.typeLine ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        OutlinedButton(onClick = {
                            vm.addToCollection(card) { msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }) { Text(tr("Sbírka")) }
                        OutlinedButton(onClick = { deckPickerFor = card }) { Text(tr("Balík")) }
                        OutlinedButton(onClick = {
                            vm.addToWishlist(card) { msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }) { Text("Wishlist") }
                    }
                }
                HorizontalDivider()
            }
        }
    }

    if (showImport) ImportListDialog(
        decks = decks,
        onDismiss = { showImport = false },
        onImport = { text, target, deckId ->
            showImport = false
            vm.importList(text, target, deckId) { importResult = it }
        },
    )
    importResult?.let { msg ->
        com.example.antimarcipan.ui.GoldDialog(
            onDismissRequest = { importResult = null },
            title = { Text(tr("Import hotov")) },
            text = { Text(msg) },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { importResult = null }) { Text("OK") } },
        )
    }

    deckPickerFor?.let { card ->
        DeckPickerDialog(
            decks = decks,
            onDismiss = { deckPickerFor = null },
            onPick = { deck ->
                deckPickerFor = null
                vm.addToDeck(card, deck.id) { msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            },
        )
    }
}

@Composable
private fun ImportListDialog(
    decks: List<com.example.antimarcipan.data.local.Deck>,
    onDismiss: () -> Unit,
    onImport: (String, ManualAddViewModel.Target, Long?) -> Unit,
) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    var target by remember { mutableStateOf(ManualAddViewModel.Target.COLLECTION) }
    var pickDeck by remember { mutableStateOf(false) }
    if (pickDeck) {
        DeckPickerDialog(decks = decks, onDismiss = { pickDeck = false }, onPick = { d ->
            pickDeck = false
            onImport(text, ManualAddViewModel.Target.DECK, d.id)
        })
        return
    }
    val count = remember(text) { parseCardList(text).sumOf { it.first } }
    com.example.antimarcipan.ui.GoldDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Import seznamu")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    tr("Jedna karta na řádek, např. „4 Lightning Bolt“, „1x Sol Ring“. Funguje i export z Moxfield/Arena."),
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = text, onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    placeholder = { Text("4 Lightning Bolt\n2 Counterspell\n1 Sol Ring") },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = {
                        val cm = context.getSystemService(android.content.ClipboardManager::class.java)
                        cm?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()?.let { text = it }
                    }) { Text(tr("Vložit ze schránky")) }
                }
                Text(tr("Kam přidat:"), style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        tr("Sbírka") to ManualAddViewModel.Target.COLLECTION,
                        "Wishlist" to ManualAddViewModel.Target.WISHLIST,
                        tr("Balík") to ManualAddViewModel.Target.DECK,
                    ).forEach { (l, t) ->
                        androidx.compose.material3.FilterChip(
                            selected = target == t, onClick = { target = t }, label = { Text(l) },
                        )
                    }
                }
                Text(tr("Rozpoznáno: {0} karet", count), style = MaterialTheme.typography.labelSmall)
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                enabled = count > 0,
                onClick = {
                    if (target == ManualAddViewModel.Target.DECK) pickDeck = true
                    else onImport(text, target, null)
                },
            ) { Text(if (target == ManualAddViewModel.Target.DECK) tr("Vybrat balík…") else tr("Importovat")) }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text(tr("Zrušit")) } },
    )
}
