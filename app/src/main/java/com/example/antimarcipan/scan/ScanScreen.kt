package com.example.antimarcipan.scan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.antimarcipan.data.local.Deck
import com.example.antimarcipan.ui.AppViewModelFactory
import com.example.antimarcipan.ui.CardThumb
import com.example.antimarcipan.ui.DeckPickerDialog

@Composable
fun ScanScreen(
    vm: ScanViewModel = viewModel(factory = AppViewModelFactory),
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    // true = uživatel žádost aspoň jednou odmítl (možná natrvalo)
    var deniedOnce by remember { mutableStateOf(false) }
    // Fotoaparát se zapne až po stisku tlačítka
    var scanning by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) scanning = true
        if (!granted) deniedOnce = true
    }

    val state by vm.state.collectAsStateWithLifecycle()
    val decks by vm.decks.collectAsStateWithLifecycle()
    val transLang by vm.translationLang.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        if (!hasPermission) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Potřebuju přístup ke kameře",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                Text(
                    if (deniedOnce)
                        "Kamera je zakázaná. Povol ji tlačítkem níž, nebo přes nastavení aplikace, pokud se okno už neukazuje."
                    else
                        "Antimarcipán skenuje karty foťákem a čte jejich jména.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
                )
                Button(
                    onClick = { launcher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Povolit kameru")
                }
                // Vždy k dispozici — když je oprávnění zakázané natrvalo,
                // systémové okno se už neukáže a tohle je jediná cesta.
                OutlinedButton(
                    onClick = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    Text("Otevřít nastavení aplikace")
                }
            }
        } else if (!scanning) {
            // Fotoaparát spustíme až po stisku tlačítka
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Skenování karet",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "Fotoaparát se zapne až po stisknutí tlačítka.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
                )
                Button(
                    onClick = { scanning = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null)
                    Text("  Spustit skenování")
                }
            }
        } else {
            var showReview by remember { mutableStateOf(false) }

            CameraPreview(
                onText = vm::onTextRecognized,
                modifier = Modifier.fillMaxSize(),
                onLines = vm::onFrameLines,
            )

            // Horní lišta: přepínač režimu vlevo, Vypnout vpravo
            Row(
                Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    androidx.compose.material3.FilterChip(
                        selected = !state.multiScan,
                        onClick = { if (state.multiScan) vm.setMultiScan(false) },
                        label = { Text("Jednotlivě") },
                    )
                    androidx.compose.material3.FilterChip(
                        selected = state.multiScan,
                        onClick = { if (!state.multiScan) vm.setMultiScan(true) },
                        label = { Text("Multisken") },
                    )
                }
                androidx.compose.material3.FilledTonalButton(
                    onClick = { scanning = false; vm.setMultiScan(false); vm.dismissCandidate() },
                ) {
                    Text("Vypnout")
                }
            }

            // Spodní overlay — minimalistický, ať je vidět co fotíš
            Column(
                Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (state.multiScan) {
                    MultiScanBar(
                        state = state,
                        onCapture = vm::captureTable,
                        onReview = { showReview = true },
                        onCancelRescan = vm::cancelRescan,
                    )
                } else {
                    val candidate = state.candidate
                    if (candidate != null) {
                        CandidateCard(
                            state = state,
                            decks = decks,
                            langLabel = transLang.label,
                            onDismiss = vm::dismissCandidate,
                            onAddCollection = vm::addToCollection,
                            onAddWishlist = vm::addToWishlist,
                            onAddDeck = vm::addToDeck,
                            onLoadPrints = vm::loadPrints,
                            onSelectPrint = vm::selectPrint,
                            onTranslate = vm::translateCandidate,
                            onHideTranslation = vm::hideTranslation,
                        )
                    } else {
                        // Jen tenký poloprůhledný proužek se stavem — nezakrývá záběr
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(androidx.compose.ui.graphics.Color(0xCC14102A))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Text(
                                state.status,
                                style = MaterialTheme.typography.labelLarge,
                                color = androidx.compose.ui.graphics.Color.White,
                            )
                        }
                    }
                }
            }

            // Zelené potvrzení po úspěšném přidání
            AddedConfirmation(
                tick = state.confirmTick,
                text = state.confirmText,
                modifier = Modifier.align(Alignment.Center),
            )

            if (showReview) {
                MultiScanReviewSheet(
                    pending = state.pending,
                    decks = decks,
                    onDismiss = { showReview = false },
                    onRemove = vm::removePending,
                    onRescan = { slot -> vm.rescanPending(slot); showReview = false },
                    onToCollection = { vm.confirmAllToCollection(); showReview = false },
                    onToWishlist = { vm.confirmAllToWishlist(); showReview = false },
                    onToDeck = { deckId -> vm.confirmAllToDeck(deckId); showReview = false },
                )
            }
        }
    }
}

@Composable
private fun AddedConfirmation(tick: Int, text: String?, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(tick) {
        if (tick > 0) {
            visible = true
            kotlinx.coroutines.delay(1300)
            visible = false
        }
    }
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.scaleIn(
            androidx.compose.animation.core.spring(dampingRatio = 0.5f)
        ) + androidx.compose.animation.fadeIn(),
        exit = androidx.compose.animation.fadeOut(),
        modifier = modifier,
    ) {
        Card(
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = androidx.compose.ui.graphics.Color(0xFF2E9E5B)
            ),
        ) {
            Column(
                Modifier.padding(horizontal = 28.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                androidx.compose.material3.Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(56.dp),
                )
                Text(
                    text ?: "Uloženo do sbírky",
                    color = androidx.compose.ui.graphics.Color.White,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun MultiScanBar(
    state: ScanUiState,
    onCapture: () -> Unit,
    onReview: () -> Unit,
    onCancelRescan: () -> Unit,
) {
    Card {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(state.status, style = MaterialTheme.typography.bodyMedium)

            // Hlavní akce: sejmout rozprostřené karty na stole naráz.
            Button(
                onClick = onCapture,
                enabled = !state.capturing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.capturing) {
                    CircularProgressIndicator(
                        Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Text("  Snímám…")
                } else {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null)
                    Text(
                        if (state.rescanSlot != null) "  Sejmout správnou kartu"
                        else "  Sejmout stůl",
                    )
                }
            }

            if (state.rescanSlot != null) {
                OutlinedButton(onClick = onCancelRescan, modifier = Modifier.fillMaxWidth()) {
                    Text("Zrušit přenačtení")
                }
            } else {
                OutlinedButton(
                    onClick = onReview,
                    enabled = state.pending.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null)
                    Text("  Zkontrolovat a potvrdit (${state.pending.size})")
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun MultiScanReviewSheet(
    pending: List<PendingScan>,
    decks: List<Deck>,
    onDismiss: () -> Unit,
    onRemove: (Long) -> Unit,
    onRescan: (Long) -> Unit,
    onToCollection: () -> Unit,
    onToWishlist: () -> Unit,
    onToDeck: (Long) -> Unit,
) {
    var showDeckPicker by remember { mutableStateOf(false) }
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text(
                "Zkontroluj karty (${pending.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Text(
                "Špatně rozpoznanou kartu přenačti nebo odeber. Pak vyber, kam je uložit.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 8.dp),
            )
            LazyColumn(
                Modifier.fillMaxWidth().heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(pending, key = { it.slotId }) { item ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CardThumb(item.card.bestImageUrl, width = 46.dp, height = 64.dp)
                        Column(Modifier.weight(1f)) {
                            Text(item.card.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                com.example.antimarcipan.ui.SetSymbol(item.card.set, size = 14.dp)
                                Text(
                                    item.card.setName ?: item.card.set?.uppercase() ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                )
                            }
                        }
                        androidx.compose.material3.IconButton(onClick = { onRescan(item.slotId) }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Přenačíst")
                        }
                        androidx.compose.material3.IconButton(onClick = { onRemove(item.slotId) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Odebrat")
                        }
                    }
                    HorizontalDivider()
                }
            }
            Column(
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onToCollection,
                    enabled = pending.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Do sbírky (${pending.size})") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showDeckPicker = true },
                        enabled = pending.isNotEmpty() && decks.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                    ) { Text("Do balíku") }
                    OutlinedButton(
                        onClick = onToWishlist,
                        enabled = pending.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                    ) { Text("Na wishlist") }
                }
            }
        }
    }

    if (showDeckPicker) {
        DeckPickerDialog(
            decks = decks,
            onDismiss = { showDeckPicker = false },
            onPick = { deck -> showDeckPicker = false; onToDeck(deck.id) },
        )
    }
}

@Composable
private fun CandidateCard(
    state: ScanUiState,
    decks: List<Deck>,
    langLabel: String,
    onDismiss: () -> Unit,
    onAddCollection: () -> Unit,
    onAddWishlist: () -> Unit,
    onAddDeck: (Long) -> Unit,
    onLoadPrints: () -> Unit,
    onSelectPrint: (com.example.antimarcipan.data.scryfall.ScryfallCard) -> Unit,
    onTranslate: () -> Unit,
    onHideTranslation: () -> Unit,
) {
    val candidate = state.candidate ?: return
    var showDeckPicker by remember { mutableStateOf(false) }
    var showEditions by remember { mutableStateOf(false) }
    val translation = state.translation

    Card {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // Větší náhled nalezené karty — líp poznáš správnou edici
            CardThumb(candidate.bestImageUrl, width = 104.dp, height = 145.dp)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        candidate.name,
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    // Překlad textace do zvoleného jazyka
                    if (state.translating) {
                        CircularProgressIndicator(
                            Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        androidx.compose.material3.IconButton(
                            onClick = { if (translation != null) onHideTranslation() else onTranslate() },
                            modifier = Modifier.size(32.dp),
                        ) {
                            androidx.compose.material3.Icon(
                                androidx.compose.material.icons.Icons.Filled.Translate,
                                contentDescription = "Přeložit do: $langLabel",
                                tint = if (translation != null) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    com.example.antimarcipan.ui.SetSymbol(candidate.set, size = 16.dp)
                    Text(
                        candidate.setName ?: candidate.set?.uppercase() ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(candidate.typeLine ?: "", style = MaterialTheme.typography.bodySmall)
                if (translation != null) {
                    androidx.compose.material3.HorizontalDivider(Modifier.padding(vertical = 6.dp))
                    if (translation.fallbackToEnglish) {
                        Text(
                            "V jazyce „$langLabel“ karta nevyšla — ukazuji anglicky.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Text(
                        translation.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    translation.typeLine?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                    translation.text?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        )
                    }
                }
            }
        }

        // Výběr jiné edice — podle symbolů edic
        TextButton(
            onClick = {
                showEditions = !showEditions
                if (showEditions) onLoadPrints()
            },
            modifier = Modifier.padding(horizontal = 4.dp),
        ) {
            Text(if (showEditions) "Skrýt edice" else "Jiná edice")
        }
        if (showEditions) {
            if (state.loadingPrints) {
                Row(Modifier.padding(12.dp)) { CircularProgressIndicator() }
            } else {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    state.prints.forEach { p ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { onSelectPrint(p); showEditions = false }
                                .padding(4.dp),
                        ) {
                            com.example.antimarcipan.ui.SetSymbol(p.set, size = 26.dp)
                            Text(
                                p.set?.uppercase() ?: "?",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }
        }

        Column(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val pad = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), contentPadding = pad) {
                    Text("Zahodit", maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(onClick = onAddWishlist, modifier = Modifier.weight(1f), contentPadding = pad) {
                    Text("Wishlist", maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelLarge)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showDeckPicker = true }, modifier = Modifier.weight(1f), contentPadding = pad) {
                    Text("Do balíku", maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelLarge)
                }
                Button(onClick = onAddCollection, modifier = Modifier.weight(1f), contentPadding = pad) {
                    Text("Do sbírky", maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelLarge)
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
                onAddDeck(deck.id)
            },
        )
    }
}
