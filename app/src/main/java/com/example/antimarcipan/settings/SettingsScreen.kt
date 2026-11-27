package com.example.antimarcipan.settings

import com.example.antimarcipan.i18n.tr

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.TextButton
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
        LanguageCard()
        SupportCard()
        if (BuildConfig.SELF_UPDATE) Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(tr("Aktualizace aplikace"), style = MaterialTheme.typography.titleMedium)
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
                    UpdatePhase.CHECKING -> RowProgress(tr("Zjišťuji dostupnou verzi…"))
                    UpdatePhase.DOWNLOADING -> {
                        val p = state.downloadProgress
                        Text(
                            if (p >= 0f) "Stahuji… ${(p * 100).roundToInt()} %" else tr("Stahuji…"),
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
                        tr("Nová verze: {0} ", (status.versionName ?: status.latestSha)) +
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
                                        tr("Povol instalaci z neznámých zdrojů a zkus to znovu."),
                                        Toast.LENGTH_LONG,
                                    ).show()
                                    context.startActivity(vm.unknownSourcesSettingsIntent())
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(tr("Nainstalovat")) }
                    }
                    state.status is UpdateStatus.Available &&
                        state.phase != UpdatePhase.DOWNLOADING -> {
                        Button(
                            onClick = vm::downloadUpdate,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(tr("Stáhnout a aktualizovat")) }
                    }
                    state.phase == UpdatePhase.IDLE -> {
                        Button(
                            onClick = vm::checkForUpdate,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(tr("Zkontrolovat aktualizaci")) }
                    }
                }
            }
        }

        Text(
            tr("Aktualizace stahuje APK přímo z GitHubu a spustí systémový instalátor. ") +
                tr("Při prvním použití Android vyzve k povolení instalace z této aplikace."),
            Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Start,
        )

        AppearanceCard(vm)

        ThumbSizeCard()



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
            Text(tr("Pozadí aplikace"), style = MaterialTheme.typography.titleMedium)
            Text(
                tr("Krajina za celou aplikací. Vyber si podle nálady — nebo žádné."),
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
                        tr("Bez\npozadí"),
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
            Text(tr("O aplikaci"), style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Image(
                    painter = painterResource(R.drawable.splash_icon),
                    contentDescription = tr("Ikona aplikace"),
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(14.dp))
                        .background(androidx.compose.ui.graphics.Brush.radialGradient(listOf(androidx.compose.ui.graphics.Color(0xFF3A2C72), androidx.compose.ui.graphics.Color(0xFF140F28)))),
                )
                Text(
                    "Grimoar · verze ${BuildConfig.VERSION_NAME} · build $sha",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
            Text(
                tr("Průvodce aplikací: krátký přehled, jak jednotlivé části fungují."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            Button(
                onClick = { showGuide = true },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(tr("Spustit průvodce")) }

            // Log pádů
            val ctx = androidx.compose.ui.platform.LocalContext.current
            val logFile = java.io.File(ctx.filesDir, com.example.antimarcipan.MtgApp.CRASH_LOG)
            var logText by remember { mutableStateOf(logFile.takeIf { it.exists() }?.readText()) }
            if (logText.isNullOrBlank()) {
                Text(
                    tr("Log pádů: žádný pád nezaznamenán."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            } else {
                Text(
                    tr("Log pádů: zaznamenáno {0} pádů. Zkopíruj ho a pošli vývojáři.", (logText!!.split("=== ").count { it.isNotBlank() })),
                    style = MaterialTheme.typography.bodySmall,
                    color = androidx.compose.ui.graphics.Color(0xFFFF9A9A),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val cm = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            cm.setPrimaryClip(android.content.ClipData.newPlainText(tr("Grimoar crash log"), logText))
                            android.widget.Toast.makeText(ctx, tr("Log zkopírován do schránky"), android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text(tr("Zkopírovat log")) }
                    androidx.compose.material3.OutlinedButton(
                        onClick = { logFile.delete(); logText = null },
                        modifier = Modifier.weight(1f),
                    ) { Text(tr("Smazat log")) }
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
            Text(tr("Záloha dat"), style = MaterialTheme.typography.titleMedium)
            Text(
                tr("Sbírka i balíky se ukládají automaticky a přežijí aktualizaci. ") +
                    tr("Android je navíc zálohuje do cloudu (obnoví se po přeinstalaci). ") +
                    tr("Tady si můžeš udělat vlastní zálohu do souboru."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            Button(
                onClick = { exportLauncher.launch(vm.suggestedBackupName()) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(tr("Zálohovat do souboru")) }
            Button(
                onClick = { importLauncher.launch(arrayOf("application/octet-stream", "*/*")) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(tr("Obnovit ze zálohy")) }
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


/** Velikost náhledů karet + živý náhled seznamu a mřížky. */
@Composable
private fun ThumbSizeCard() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val container = remember { (ctx.applicationContext as com.example.antimarcipan.MtgApp).container }
    val settings = container.appSettings
    val listScale by settings.listThumb.collectAsStateWithLifecycle()
    val gridScale by settings.gridThumb.collectAsStateWithLifecycle()
    val cards by container.collectionRepository.cards.collectAsStateWithLifecycle(emptyList())
    val sample = cards.take(6)
    Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(tr("Velikost náhledů karet"), style = MaterialTheme.typography.titleMedium)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Seznam"), Modifier.width(70.dp), style = MaterialTheme.typography.labelLarge)
                androidx.compose.material3.Slider(
                    value = listScale, onValueChange = { settings.setListThumb((it * 10).toInt() / 10f) },
                    valueRange = 0.6f..1.8f, steps = 11, modifier = Modifier.weight(1f),
                )
                Text("${(listScale * 100).toInt()} %", Modifier.width(48.dp), style = MaterialTheme.typography.labelMedium)
            }
            // Náhled seznamu
            androidx.compose.runtime.CompositionLocalProvider(
                com.example.antimarcipan.ui.LocalThumbScale provides com.example.antimarcipan.ui.ThumbScale(listScale, gridScale)
            ) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.2f)).padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (sample.isEmpty()) Text(tr("Náhled se ukáže, až budeš mít karty ve sbírce."), style = MaterialTheme.typography.bodySmall)
                    sample.take(2).forEach { com.example.antimarcipan.ui.CardListInfo(card = it) }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Mřížka"), Modifier.width(70.dp), style = MaterialTheme.typography.labelLarge)
                androidx.compose.material3.Slider(
                    value = gridScale, onValueChange = { settings.setGridThumb((it * 10).toInt() / 10f) },
                    valueRange = 0.6f..1.8f, steps = 11, modifier = Modifier.weight(1f),
                )
                Text("${(gridScale * 100).toInt()} %", Modifier.width(48.dp), style = MaterialTheme.typography.labelMedium)
            }
            // Náhled mřížky
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.2f)).padding(8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                sample.forEach { c ->
                    com.example.antimarcipan.ui.InteractiveThumb(c, width = (88 * gridScale).dp, height = (123 * gridScale).dp)
                }
            }
            TextButton(onClick = { settings.setListThumb(1f); settings.setGridThumb(1f) }) { Text(tr("Výchozí velikost")) }
        }
    }
}

/** Grimoar Plus — jednorázové odemknutí (jen verze z Google Play). */
@Composable
private fun SupportCard() {
    if (!com.example.antimarcipan.support.SupportStore.AVAILABLE) return
    val unlocked = com.example.antimarcipan.support.rememberPlusUnlocked()
    Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        if (unlocked) Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("✦ Grimoar Plus", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(tr("Díky! Máš Grimoar Plus — počítadlo životů i zakládání anket jsou odemčené."), style = MaterialTheme.typography.bodySmall)
        } else com.example.antimarcipan.support.PlusOffer("", Modifier.fillMaxWidth())
    }
}

/** Jazyk aplikace: podle telefonu / čeština / angličtina. Změna restartuje aplikaci. */
@Composable
private fun LanguageCard() {
    val ctx = LocalContext.current
    val mode = com.example.antimarcipan.i18n.I18n.mode
    Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Jazyk / Language", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("auto" to tr("Podle telefonu"), "cs" to tr("Čeština"), "en" to "English").forEach { (m, label) ->
                    com.example.antimarcipan.ui.GoldChip(selected = mode == m, onClick = {
                        if (m != mode) {
                            com.example.antimarcipan.i18n.I18n.setMode(ctx, m)
                            // Restart, ať se přeloží i texty načtené při startu.
                            val i = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
                                ?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            ctx.startActivity(i)
                            Runtime.getRuntime().exit(0)
                        }
                    }, label = { Text(label) })
                }
            }
        }
    }
}
