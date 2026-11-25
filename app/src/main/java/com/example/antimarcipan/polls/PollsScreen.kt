package com.example.antimarcipan.polls

import com.example.antimarcipan.i18n.tr

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.antimarcipan.ui.GoldBorderBrush
import com.example.antimarcipan.ui.GoldDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Hlasování o další hře: ankety s termíny a místem, posílané kamarádům zprávou. */
@Composable
fun PollsScreen() {
    val ctx = LocalContext.current
    val polls by PollStore.polls.collectAsState()
    val notice by PollStore.notice.collectAsState()
    val openReq by PollStore.openRequest.collectAsState()
    var openId by remember { mutableStateOf<String?>(null) }
    var creating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var lastClip by remember { mutableStateOf("") }

    LaunchedEffect(openReq) { openReq?.let { openId = it; PollStore.openRequest.value = null } }
    // Po návratu do aplikace zkontrolovat schránku (pozvánka / hlas od kamaráda).
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) scope.launch {
                delay(400)
                val t = readClip(ctx)
                if (t != lastClip && ("AMPOLL1:" in t || "AMVOTE1:" in t)) { lastClip = t; PollStore.receive(t) }
            }
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    val open = polls.firstOrNull { it.id == openId }
    if (open != null) {
        LaunchedEffect(open.id) { PollStore.markRead(open.id) }
        PollDetail(open, onBack = { openId = null })
    } else Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tr("Hlasování o hře"), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Text(tr("Navrhni termíny a místo, pošli kamarádům, oni zaškrtnou, kdy můžou."), style = MaterialTheme.typography.bodySmall)
            }
        }
        Row(Modifier.padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { creating = true }) { Icon(Icons.Filled.Add, null); Text(tr(" Nová anketa")) }
            OutlinedButton(onClick = {
                val t = readClip(ctx)
                if (!PollStore.receive(t)) PollStore.notice.value = tr("Ve schránce není pozvánka ani hlas z Grimoaru.")
                else lastClip = t
            }) { Icon(Icons.Filled.ContentPaste, null); Text(tr(" Vložit")) }
        }
        if (polls.isEmpty()) Text(
            tr("Zatím žádná anketa. Vytvoř novou, nebo zkopíruj pozvánku od kamaráda a ťukni na Vložit."),
            Modifier.padding(20.dp), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
            items(polls, key = { it.id }) { p -> PollRow(p) { openId = p.id } }
        }
    }

    if (creating) CreatePollDialog(onDismiss = { creating = false }) { openId = it.id; creating = false }
    notice?.let { msg ->
        GoldDialog(
            onDismissRequest = { PollStore.notice.value = null },
            title = { Text(tr("Hlasování")) },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = { PollStore.notice.value = null }) { Text("OK") } },
        )
    }
}

@Composable
private fun PollRow(p: Poll, onClick: () -> Unit) {
    val best = bestOption(p)
    Card(
        Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(if (p.unread) 2.dp else 1.dp, GoldBorderBrush),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(p.title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = if (p.unread) FontWeight.Bold else null)
                if (p.unread) Text(tr("nové"), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
            }
            Text(
                (if (p.mine) "Tvoje anketa" else "Od ${p.owner}") + " · hlasovalo ${p.votes.size}" +
                    (if (p.people.isNotEmpty()) " z ${maxOf(p.people.size + if (p.mine) 1 else 0, p.votes.size)}" else ""),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                when {
                    p.chosen != null -> "✅ ${PollStore.label(p.options[p.chosen])}"
                    best != null -> tr("Zatím nejlíp: {0} ({1})", (PollStore.label(p.options[best.first])), (best.second))
                    else -> tr("{0} termínů", (p.options.size))
                } + if (p.place.isNotBlank()) " · 📍 ${p.place}" else "",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun bestOption(p: Poll): Pair<Int, Int>? =
    p.options.indices.map { i -> i to p.votes.values.count { i in it } }.filter { it.second > 0 }.maxByOrNull { it.second }

@Composable
private fun PollDetail(p: Poll, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }
    var askName by remember { mutableStateOf(false) }
    val me = PollStore.myName().ifBlank { if (p.mine) p.owner else "" }
    val mySel = remember(p.id, p.myVote, p.votes) {
        mutableStateListOf<Int>().apply { addAll(if (p.mine) p.votes[p.owner].orEmpty() else p.myVote.orEmpty()) }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Zpět")) }
            Text(p.title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, tr("Smazat")) }
        }
        Column(Modifier.padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                (if (p.mine) tr("Organizuješ ty") else "Organizuje ${p.owner}") + if (p.place.isNotBlank()) " · 📍 ${p.place}" else "",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (p.people.isNotEmpty()) Text(tr("Pozvaní: ") + p.people.joinToString(), style = MaterialTheme.typography.bodySmall)
            p.chosen?.let { c ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1F3B2A))) {
                    Column(Modifier.padding(12.dp)) {
                        Text("✅ Domluveno: ${PollStore.label(p.options[c])}", fontWeight = FontWeight.Bold)
                        TextButton(onClick = { addToCalendar(ctx, p, c) }) { Text(tr("Přidat do kalendáře")) }
                    }
                }
            }
            HorizontalDivider()
            Text(tr("Kdy můžeš? Zaškrtni všechny termíny, které ti vyhovují."), style = MaterialTheme.typography.labelLarge)
            p.options.forEachIndexed { i, o ->
                val who = p.votes.filter { i in it.value }.keys
                Card(
                    Modifier.fillMaxWidth(),
                    border = if (p.chosen == i) BorderStroke(2.dp, GoldBorderBrush) else null,
                ) {
                    Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = i in mySel, onCheckedChange = { if (it) mySel.add(i) else mySel.remove(i) })
                        Column(Modifier.weight(1f)) {
                            Text(PollStore.label(o), style = MaterialTheme.typography.titleSmall)
                            Text(
                                if (who.isEmpty()) tr("zatím nikdo") else "${who.size}: ${who.joinToString()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            )
                        }
                        if (p.mine) TextButton(onClick = { PollStore.upsert(p.copy(chosen = if (p.chosen == i) null else i)) }) {
                            Text(if (p.chosen == i) tr("Zrušit") else "Vybrat")
                        }
                    }
                }
            }
            val waiting = p.people.filter { it !in p.votes.keys }
            if (p.mine && waiting.isNotEmpty()) Text(tr("Ještě nehlasovali: ") + waiting.joinToString(), style = MaterialTheme.typography.bodySmall)

            if (p.mine) {
                Button(onClick = {
                    PollStore.upsert(p.copy(votes = p.votes + (p.owner to mySel.toList().sorted())))
                    share(ctx, PollStore.inviteText(p.copy(votes = p.votes + (p.owner to mySel.toList().sorted()))))
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (p.chosen != null) tr("Poslat potvrzený termín") else tr("Poslat pozvánku / aktuální stav"))
                }
                Text(
                    tr("Pošli zprávu do skupiny nebo jednotlivě. Kamarád ji zkopíruje a otevře Grimoar, zaškrtne termíny a pošle ti odpověď — tu zase zkopíruj a otevři Grimoar."),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            } else {
                Button(onClick = {
                    if (me.isBlank()) askName = true
                    else {
                        PollStore.upsert(p.copy(myVote = mySel.toList().sorted(), votes = p.votes + (me to mySel.toList().sorted())))
                        share(ctx, PollStore.voteText(p, me, mySel.toList().sorted()))
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text(if (p.myVote == null) tr("Odeslat hlas organizátorovi") else tr("Poslat změněný hlas")) }
                Text(
                    tr("Odpověď pošli {0} (třeba do stejné konverzace). Odpověď obsahuje jen tvé zaškrtnuté termíny.", (p.owner)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
            Spacer(Modifier.size(20.dp))
        }
    }
    if (askName) NameDialog(onDismiss = { askName = false }) { n ->
        PollStore.setMyName(n); askName = false
        PollStore.upsert(p.copy(myVote = mySel.toList().sorted(), votes = p.votes + (n to mySel.toList().sorted())))
        share(ctx, PollStore.voteText(p, n, mySel.toList().sorted()))
    }
    if (confirmDelete) GoldDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text(tr("Smazat anketu?")) },
        text = { Text(tr("Anketa „{0}“ zmizí jen z tvého telefonu.", (p.title))) },
        confirmButton = { TextButton(onClick = { PollStore.delete(p.id); confirmDelete = false; onBack() }) { Text(tr("Smazat")) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(tr("Zrušit")) } },
    )
}

@Composable
private fun NameDialog(onDismiss: () -> Unit, onOk: (String) -> Unit) {
    var n by remember { mutableStateOf("") }
    GoldDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Jak ti říkají?")) },
        text = { OutlinedTextField(n, { n = it }, label = { Text(tr("Tvoje jméno / přezdívka")) }, singleLine = true) },
        confirmButton = { TextButton(enabled = n.isNotBlank(), onClick = { onOk(n.trim()) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Zrušit")) } },
    )
}

@Composable
private fun CreatePollDialog(onDismiss: () -> Unit, onCreated: (Poll) -> Unit) {
    val ctx = LocalContext.current
    val friends by remember {
        (ctx.applicationContext as com.example.antimarcipan.MtgApp).container.friendDao.friends()
    }.collectAsState(emptyList())
    var title by remember { mutableStateOf("Magic u stolu") }
    var place by remember { mutableStateOf("") }
    var myName by remember { mutableStateOf(PollStore.myName()) }
    val options = remember { mutableStateListOf<Long>() }
    val people = remember { mutableStateListOf<String>() }
    var extra by remember { mutableStateOf("") }

    fun pickDateTime() {
        val c = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_MONTH, 1); set(java.util.Calendar.HOUR_OF_DAY, 18); set(java.util.Calendar.MINUTE, 0) }
        android.app.DatePickerDialog(ctx, { _, y, m, d ->
            android.app.TimePickerDialog(ctx, { _, h, min ->
                val cal = java.util.Calendar.getInstance().apply { set(y, m, d, h, min, 0); set(java.util.Calendar.MILLISECOND, 0) }
                if (cal.timeInMillis !in options) options.add(cal.timeInMillis)
                options.sort()
            }, c.get(java.util.Calendar.HOUR_OF_DAY), c.get(java.util.Calendar.MINUTE), true).show()
        }, c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH), c.get(java.util.Calendar.DAY_OF_MONTH)).show()
    }

    GoldDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Nová anketa")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text(tr("Název")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(place, { place = it }, label = { Text(tr("Místo")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (PollStore.myName().isBlank()) OutlinedTextField(myName, { myName = it }, label = { Text(tr("Tvoje jméno")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text(tr("Termíny"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                options.forEach { o ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(PollStore.label(o), Modifier.weight(1f))
                        IconButton(onClick = { options.remove(o) }) { Icon(Icons.Filled.Close, tr("Odebrat")) }
                    }
                }
                OutlinedButton(onClick = { pickDateTime() }, enabled = options.size < 8) { Icon(Icons.Filled.Add, null); Text(tr(" Přidat termín")) }
                Text(tr("Koho pozvat"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                (friends.map { it.name } + people.filter { n -> friends.none { it.name == n } }).distinct().forEach { n ->
                    Row(Modifier.fillMaxWidth().clickable { if (n in people) people.remove(n) else people.add(n) }, verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = n in people, onCheckedChange = { if (it) people.add(n) else people.remove(n) })
                        Text(n)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(extra, { extra = it }, label = { Text(tr("Další jméno")) }, singleLine = true, modifier = Modifier.weight(1f))
                    IconButton(onClick = { if (extra.isNotBlank() && extra.trim() !in people) people.add(extra.trim()); extra = "" }) { Icon(Icons.Filled.Add, tr("Přidat")) }
                }
                Text(
                    tr("Seznam pozvaných ti jen ukáže, kdo ještě nehlasoval. Pozvánku pak pošleš přes WhatsApp/SMS komu chceš."),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        },
        confirmButton = {
            TextButton(enabled = title.isNotBlank() && options.isNotEmpty(), onClick = {
                if (myName.isNotBlank()) PollStore.setMyName(myName)
                onCreated(PollStore.create(title, place, options.toList(), people.toList(), emptyList()))
            }) { Text(tr("Vytvořit")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Zrušit")) } },
    )
}

private fun share(ctx: Context, text: String) {
    val i = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    ctx.startActivity(Intent.createChooser(i, tr("Poslat přes…")))
}

private fun readClip(ctx: Context): String = runCatching {
    ctx.getSystemService(ClipboardManager::class.java)?.primaryClip?.getItemAt(0)?.coerceToText(ctx)?.toString()
}.getOrNull() ?: ""

private fun addToCalendar(ctx: Context, p: Poll, i: Int) {
    val start = p.options[i]
    val intent = Intent(Intent.ACTION_INSERT)
        .setData(android.provider.CalendarContract.Events.CONTENT_URI)
        .putExtra(android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
        .putExtra(android.provider.CalendarContract.EXTRA_EVENT_END_TIME, start + 4 * 3600_000L)
        .putExtra(android.provider.CalendarContract.Events.TITLE, p.title)
        .putExtra(android.provider.CalendarContract.Events.EVENT_LOCATION, p.place)
    runCatching { ctx.startActivity(intent) }
}
