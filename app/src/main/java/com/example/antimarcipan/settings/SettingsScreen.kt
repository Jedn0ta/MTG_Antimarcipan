package com.example.antimarcipan.settings

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.antimarcipan.R
import com.example.antimarcipan.data.settings.AppBackground
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.antimarcipan.BuildConfig
import com.example.antimarcipan.data.update.UpdateStatus
import com.example.antimarcipan.ui.AppViewModelFactory
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
        Spacer(Modifier.height(4.dp))

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

        AppearanceCard(vm)



        BackupCard(vm)

        AboutCard(sha = state.currentSha)

        Spacer(Modifier.height(12.dp))
    }
}

private fun AppBackground.previewRes(): Int? = when (this) {
    AppBackground.NONE -> null
    AppBackground.PLAINS -> R.drawable.bg_plains
    AppBackground.ISLAND -> R.drawable.bg_island
    AppBackground.SWAMP -> R.drawable.bg_swamp
    AppBackground.MOUNTAIN -> R.drawable.bg_mountain
    AppBackground.FOREST -> R.drawable.bg_forest
    AppBackground.WASTES -> R.drawable.bg_wastes
}

@Composable
private fun AppearanceCard(vm: SettingsViewModel) {
    val current by vm.background.collectAsStateWithLifecycle()
    Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Pozadí aplikace", style = MaterialTheme.typography.titleMedium)
            Text(
                "Krajina za celou aplikací. Vyber si podle nálady — nebo žádné.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AppBackground.entries.forEach { bg ->
                    BackgroundSwatch(
                        bg = bg,
                        selected = current == bg,
                        onClick = { vm.setBackground(bg) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BackgroundSwatch(
    bg: AppBackground,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val border = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outline
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier
                .width(78.dp)
                .height(108.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(if (selected) 3.dp else 1.dp, border, RoundedCornerShape(10.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            val res = bg.previewRes()
            if (res != null) {
                Image(
                    painter = painterResource(res),
                    contentDescription = bg.label,
                    modifier = Modifier.fillMaxWidth().height(108.dp),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(Modifier.fillMaxWidth().height(108.dp)
                    .clip(RoundedCornerShape(10.dp))) {
                    Text(
                        "Bez\npozadí",
                        Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Text(
            bg.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        )
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun AboutCard(sha: String) {
    var showGuide by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("O aplikaci", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Image(
                    painter = painterResource(R.drawable.splash_icon),
                    contentDescription = "Ikona aplikace",
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)),
                )
                Text(
                    "Antimarcipán · verze ${BuildConfig.VERSION_NAME} · build $sha",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
            Text(
                "Průvodce aplikací: krátký přehled, jak jednotlivé části fungují.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            Button(
                onClick = { showGuide = true },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Spustit průvodce") }

            // Log pádů
            val ctx = androidx.compose.ui.platform.LocalContext.current
            val logFile = java.io.File(ctx.filesDir, com.example.antimarcipan.MtgApp.CRASH_LOG)
            var logText by remember { mutableStateOf(logFile.takeIf { it.exists() }?.readText()) }
            if (logText.isNullOrBlank()) {
                Text(
                    "Log pádů: žádný pád nezaznamenán.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            } else {
                Text(
                    "Log pádů: zaznamenáno ${logText!!.split("=== ").count { it.isNotBlank() }} pádů. Zkopíruj ho a pošli vývojáři.",
                    style = MaterialTheme.typography.bodySmall,
                    color = androidx.compose.ui.graphics.Color(0xFFFF9A9A),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val cm = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            cm.setPrimaryClip(android.content.ClipData.newPlainText("Antimarcipán crash log", logText))
                            android.widget.Toast.makeText(ctx, "Log zkopírován do schránky", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("Zkopírovat log") }
                    androidx.compose.material3.OutlinedButton(
                        onClick = { logFile.delete(); logText = null },
                        modifier = Modifier.weight(1f),
                    ) { Text("Smazat log") }
                }
            }
        }
    }
    if (showGuide) {
        com.example.antimarcipan.ui.GuideOverlay(onClose = { showGuide = false })
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
