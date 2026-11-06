package com.example.antimarcipan.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.antimarcipan.BuildConfig
import com.example.antimarcipan.data.update.UpdateStatus
import com.example.antimarcipan.ui.AppViewModelFactory
import com.example.antimarcipan.ui.ScreenTitle
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    vm: SettingsViewModel = viewModel(factory = AppViewModelFactory),
) {
    val context = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ScreenTitle("Nastavení")

        // --- Verze / aktualizace ---
        Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Aktualizace aplikace", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Verze ${BuildConfig.VERSION_NAME} · build ${state.currentSha}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                Text(
                    "Zdroj: GitHub (${BuildConfig.UPDATE_REPO}, release „${BuildConfig.UPDATE_TAG}“).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )

                when (state.phase) {
                    UpdatePhase.CHECKING -> RowProgress("Zjišťuji dostupnou verzi…")
                    UpdatePhase.DOWNLOADING -> {
                        val p = state.downloadProgress
                        Text(
                            if (p >= 0f) "Stahuji… ${(p * 100).roundToInt()} %" else "Stahuji…",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (p >= 0f) {
                            LinearProgressIndicator(
                                progress = { p },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                        }
                    }
                    else -> Unit
                }

                val status = state.status
                if (status is UpdateStatus.Available && state.phase != UpdatePhase.DOWNLOADING) {
                    Text(
                        "Nová verze: ${status.versionName ?: status.latestSha} " +
                            "(${(status.sizeBytes / 1_000_000.0).roundToInt()} MB)",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    status.notes?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                }

                state.message?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }

                // Tlačítka podle fáze
                when {
                    state.phase == UpdatePhase.READY_TO_INSTALL -> {
                        Button(
                            onClick = {
                                val ok = vm.tryInstall()
                                if (!ok) {
                                    Toast.makeText(
                                        context,
                                        "Povol instalaci z neznámých zdrojů a zkus to znovu.",
                                        Toast.LENGTH_LONG,
                                    ).show()
                                    context.startActivity(vm.unknownSourcesSettingsIntent())
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Nainstalovat") }
                    }
                    state.status is UpdateStatus.Available &&
                        state.phase != UpdatePhase.DOWNLOADING -> {
                        Button(
                            onClick = vm::downloadUpdate,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Stáhnout a aktualizovat") }
                    }
                    state.phase == UpdatePhase.IDLE -> {
                        Button(
                            onClick = vm::checkForUpdate,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Zkontrolovat aktualizaci") }
                    }
                }
            }
        }

        Text(
            "Aktualizace stahuje APK přímo z GitHubu a spustí systémový instalátor. " +
                "Při prvním použití Android vyzve k povolení instalace z této aplikace.",
            Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Start,
        )

        BackupCard(vm)
    }
}

@Composable
private fun BackupCard(vm: SettingsViewModel) {
    val context = LocalContext.current

    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) vm.exportBackup(uri) { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }
    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) vm.importBackup(uri) { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Záloha dat", style = MaterialTheme.typography.titleMedium)
            Text(
                "Sbírka i balíky se ukládají automaticky a přežijí aktualizaci. " +
                    "Android je navíc zálohuje do cloudu (obnoví se po přeinstalaci). " +
                    "Tady si můžeš udělat vlastní zálohu do souboru.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            Button(
                onClick = { exportLauncher.launch(vm.suggestedBackupName()) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Zálohovat do souboru") }
            Button(
                onClick = { importLauncher.launch(arrayOf("application/octet-stream", "*/*")) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Obnovit ze zálohy") }
        }
    }
}

@Composable
private fun RowProgress(text: String) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CircularProgressIndicator()
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}
