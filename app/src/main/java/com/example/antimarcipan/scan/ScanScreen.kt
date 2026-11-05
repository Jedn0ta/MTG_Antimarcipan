package com.example.antimarcipan.scan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import com.example.antimarcipan.ui.AppViewModelFactory
import com.example.antimarcipan.ui.CardThumb

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
                    Card {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CardThumb(candidate.bestImageUrl)
                            Column(Modifier.weight(1f)) {
                                Text(candidate.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    candidate.typeLine ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                candidate.prices?.usd?.let { Text("$$it", style = MaterialTheme.typography.bodySmall) }
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(onClick = vm::dismissCandidate, modifier = Modifier.weight(1f)) {
                                Text("Zahodit")
                            }
                            Button(onClick = vm::addCandidate, modifier = Modifier.weight(1f)) {
                                Text("Přidat do sbírky")
                            }
                        }
                    }
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
        }
    }
}
