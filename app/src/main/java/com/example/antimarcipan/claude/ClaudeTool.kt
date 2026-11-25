package com.example.antimarcipan.claude

import com.example.antimarcipan.i18n.tr

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.antimarcipan.advisor.AdvisorViewModel
import com.example.antimarcipan.advisor.HubBackRow
import com.example.antimarcipan.ui.GoldDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Rádce → „Zeptat se Clauda“: zadání do Clauda a použití jeho návrhů. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ClaudeTool(vm: AdvisorViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val decks by vm.decks.collectAsStateWithLifecycle()
    val owned by vm.owned.collectAsStateWithLifecycle()
    val pcUrl by PcBridge.url.collectAsStateWithLifecycle()
    val pcIncoming by PcBridge.incoming.collectAsStateWithLifecycle()

    var deckId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deckMenu by remember { mutableStateOf(false) }
    var withCollection by rememberSaveable { mutableStateOf(true) }
    var preset by rememberSaveable { mutableStateOf(0) }
    var custom by rememberSaveable { mutableStateOf("") }
    var proposal by remember { mutableStateOf<ClaudeBridge.Proposal?>(null) }
    var result by remember { mutableStateOf<String?>(null) }
    var lastClip by remember { mutableStateOf("") }

    LaunchedEffect(decks) { if (deckId == null) deckId = decks.firstOrNull()?.id }
    val question = custom.ifBlank { ClaudeBridge.QUESTIONS[preset].second }

    suspend fun prompt() = vm.claudePrompt(deckId, withCollection, question)

    // Odpověď poslaná z PC přes Wi-Fi
    LaunchedEffect(pcIncoming) {
        val t = pcIncoming ?: return@LaunchedEffect
        PcBridge.incoming.value = null
        val p = ClaudeBridge.parse(t)
        if (p == null) result = tr("Odpověď z PC neobsahuje blok {0}… se změnami.", (ClaudeBridge.START))
        else proposal = p
    }
    // Po návratu do aplikace zkontrolovat schránku
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) scope.launch {
                delay(400)
                val t = readClip(ctx)
                if (t != lastClip && t.contains(ClaudeBridge.START)) {
                    lastClip = t
                    ClaudeBridge.parse(t)?.let { proposal = it }
                }
            }
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    // Při odchodu z nástroje vypnout server pro PC
    DisposableEffect(Unit) { onDispose { PcBridge.stop() } }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        HubBackRow("Zeptat se Clauda", onBack)
        Column(Modifier.padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                tr("Grimoar připraví zadání pro Clauda (tvůj balík, analýzu, komba a sbírku). Vlož ho do Clauda a ptej se. ") +
                    tr("Když Claude navrhne změny, zkopíruj jeho odpověď — Grimoar ji pozná a změny provede."),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(tr("Balík"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Box0 {
                OutlinedButton(onClick = { deckMenu = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(decks.firstOrNull { it.id == deckId }?.name ?: tr("Bez balíku (jen sbírka)"))
                }
                DropdownMenu(expanded = deckMenu, onDismissRequest = { deckMenu = false }) {
                    DropdownMenuItem(text = { Text(tr("Bez balíku (jen sbírka)")) }, onClick = { deckId = null; deckMenu = false })
                    decks.forEach { d -> DropdownMenuItem(text = { Text(d.name) }, onClick = { deckId = d.id; deckMenu = false }) }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = withCollection, onCheckedChange = { withCollection = it })
                Text(tr("Přiložit sbírku ({0} karet) — Claude radí z toho, co máš", (owned.size)), style = MaterialTheme.typography.bodySmall)
            }
            Text(tr("Otázka"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ClaudeBridge.QUESTIONS.forEachIndexed { i, (label, _) ->
                    FilterChip(selected = custom.isBlank() && preset == i, onClick = { preset = i; custom = "" }, label = { Text(label) })
                }
            }
            OutlinedTextField(
                value = custom, onValueChange = { custom = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(tr("Nebo vlastní otázka")) }, minLines = 2,
            )

            Button(onClick = {
                scope.launch {
                    copyClip(ctx, prompt())
                    Toast.makeText(ctx, tr("Zadání zkopírováno — vlož ho do Clauda"), Toast.LENGTH_LONG).show()
                    openClaude(ctx)
                }
            }, modifier = Modifier.fillMaxWidth()) { Text(tr("Kopírovat a otevřít Clauda")) }

            OutlinedButton(onClick = {
                scope.launch { PcBridge.start(prompt()) }
            }, modifier = Modifier.fillMaxWidth()) { Text(if (pcUrl == null) tr("Poslat na PC přes Wi-Fi") else tr("Aktualizovat zadání na PC")) }

            pcUrl?.let { u ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(tr("Na počítači ve stejné Wi-Fi otevři v prohlížeči:"), style = MaterialTheme.typography.bodySmall)
                        SelectionContainer { Text(u, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
                        Text(
                            tr("Tam zkopíruješ zadání pro Clauda a jeho odpověď pošleš zpět do telefonu. Spojení běží, dokud jsi v tomhle nástroji."),
                            style = MaterialTheme.typography.labelSmall,
                        )
                        TextButton(onClick = { PcBridge.stop() }) { Text(tr("Ukončit spojení s PC")) }
                    }
                }
            }

            OutlinedButton(onClick = {
                val t = readClip(ctx)
                val p = ClaudeBridge.parse(t)
                if (p == null) result = tr("Ve schránce není blok {0}… od Clauda. Zkopíruj celou jeho odpověď (nebo jen ten blok).", (ClaudeBridge.START))
                else { lastClip = t; proposal = p }
            }, modifier = Modifier.fillMaxWidth()) { Text(tr("Vložit odpověď od Clauda ze schránky")) }
            Text(
                tr("Tip: Claude píše změny na konec odpovědi do bloku GRIMOAR-START … GRIMOAR-END. Stačí zkopírovat celou odpověď."),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }

    proposal?.let { p ->
        ProposalDialog(
            p,
            deckName = decks.firstOrNull { it.id == p.deckId }?.name,
            onDismiss = { proposal = null },
            onApply = { sel ->
                proposal = null
                vm.applyClaude(p.deckId, sel) { result = it }
            },
        )
    }
    result?.let { msg ->
        GoldDialog(
            onDismissRequest = { result = null },
            title = { Text("Claude") },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = { result = null }) { Text("OK") } },
        )
    }
}

@Composable
private fun Box0(content: @Composable () -> Unit) = androidx.compose.foundation.layout.Box { content() }

@Composable
private fun ProposalDialog(
    p: ClaudeBridge.Proposal,
    deckName: String?,
    onDismiss: () -> Unit,
    onApply: (List<ClaudeBridge.Change>) -> Unit,
) {
    val checked = remember(p) { mutableStateListOf(*Array(p.changes.size) { true }) }
    GoldDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Návrh od Clauda")) },
        text = {
            Column(Modifier) {
                Text(
                    if (deckName != null) tr("Balík: {0}", deckName) else tr("Bez balíku — přidávané karty půjdou na wishlist."),
                    style = MaterialTheme.typography.bodySmall,
                )
                p.changes.forEachIndexed { i, c ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = checked[i], onCheckedChange = { checked[i] = it })
                        Text(
                            when (c) {
                                is ClaudeBridge.Change.Add -> "＋ ${c.qty}× ${c.name}"
                                is ClaudeBridge.Change.Cut -> "－ ${c.qty}× ${c.name}"
                                is ClaudeBridge.Change.Wish -> "☆ ${c.name}" + if (c.note.isNotBlank()) " — ${c.note}" else ""
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                Text(
                    tr("Karty, které nemáš ve sbírce, se místo do balíku dají na wishlist."),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(p.changes.filterIndexed { i, _ -> checked[i] }) }) { Text(tr("Provést")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Zrušit")) } },
    )
}

private fun copyClip(ctx: Context, text: String) {
    ctx.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("Grimoar → Claude", text))
}

private fun readClip(ctx: Context): String = runCatching {
    ctx.getSystemService(ClipboardManager::class.java)?.primaryClip?.getItemAt(0)?.coerceToText(ctx)?.toString()
}.getOrNull() ?: ""

private fun openClaude(ctx: Context) {
    val app = ctx.packageManager.getLaunchIntentForPackage("com.anthropic.claude")
    runCatching {
        ctx.startActivity(app ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://claude.ai/new")))
    }
}
