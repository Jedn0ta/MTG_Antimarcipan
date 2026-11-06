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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (!granted) deniedOnce = true
    }

    val state by vm.state.collectAsStateWithLifecycle()
    val decks by vm.decks.collectAsStateWithLifecycle()

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
        } else {
            CameraPreview(
                onText = vm::onTextRecognized,
                modifier = Modifier.fillMaxSize(),
            )

            // Status / candidate overlay
            Column(
                Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val candidate = state.candidate
                if (candidate != null) {
                    CandidateCard(
                        state = state,
                        decks = decks,
                        onDismiss = vm::dismissCandidate,
                        onAddCollection = vm::addToCollection,
                        onAddDeck = vm::addToDeck,
                        onLoadPrints = vm::loadPrints,
                        onSelectPrint = vm::selectPrint,
                    )
                } else {
                    Card {
                        Text(
                            state.status,
                            Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            // Zelené potvrzení po úspěšném přidání
            AddedConfirmation(
                tick = state.confirmTick,
                text = state.confirmText,
                modifier = Modifier.align(Alignment.Center),
            )
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
private fun CandidateCard(
    state: ScanUiState,
    decks: List<Deck>,
    onDismiss: () -> Unit,
    onAddCollection: () -> Unit,
    onAddDeck: (Long) -> Unit,
    onLoadPrints: () -> Unit,
    onSelectPrint: (com.example.antimarcipan.data.scryfall.ScryfallCard) -> Unit,
) {
    val candidate = state.candidate ?: return
    var showDeckPicker by remember { mutableStateOf(false) }
    var showEditions by remember { mutableStateOf(false) }
    var showEnglish by remember { mutableStateOf(false) }

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
                    // Překlad: anglická textace (oracle je vždy anglicky)
                    androidx.compose.material3.IconButton(
                        onClick = { showEnglish = !showEnglish },
                        modifier = Modifier.size(32.dp),
                    ) {
                        androidx.compose.material3.Icon(
                            androidx.compose.material.icons.Icons.Filled.Translate,
                            contentDescription = "Anglická textace",
                            tint = if (showEnglish) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                        )
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
                if (showEnglish) {
                    Text(
                        buildString {
                            append("EN: ${candidate.name}\n")
                            candidate.typeLine?.let { append("$it\n") }
                            candidate.oracleText?.let { append(it) }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    )
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

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp),
            ) {
                Text("Zahodit", maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelLarge)
            }
            OutlinedButton(
                onClick = { showDeckPicker = true },
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp),
            ) {
                Text("Do balíku", maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelLarge)
            }
            Button(
                onClick = onAddCollection,
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp),
            ) {
                Text("Do sbírky", maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelLarge)
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
