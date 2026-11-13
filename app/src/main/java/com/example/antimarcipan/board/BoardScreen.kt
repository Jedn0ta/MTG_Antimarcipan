package com.example.antimarcipan.board

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.antimarcipan.ui.GoldBorderBrush
import com.example.antimarcipan.ui.GoldDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val dateFmt = SimpleDateFormat("EEE d. M. H:mm", Locale("cs"))
private val timeFmt = SimpleDateFormat("d. M. H:mm", Locale("cs"))

private fun nameColor(pub: String): Color {
    val palette = listOf(0xFFE2BC62, 0xFF8FD6A0, 0xFF8EC5FF, 0xFFFFA0A0, 0xFFB9A7EC, 0xFFFFC98A, 0xFF6FD2B0)
    return Color(palette[(pub.hashCode() and 0x7fffffff) % palette.size])
}

/**
 * Nástěnka přes Nostr: soukromé skupiny (pozvánkou), zprávy a schůzky s „jdu / možná / nejdu“.
 * Zprávy jsou šifrované klíčem skupiny — relaye ani cizí lidé je nepřečtou.
 */
@Composable
fun BoardScreen() {
    val ctx = LocalContext.current
    val vm: BoardViewModel = viewModel(factory = viewModelFactory { initializer { BoardViewModel(ctx.applicationContext) } })
    val groups by vm.groups.collectAsStateWithLifecycle()
    val current by vm.current.collectAsStateWithLifecycle()
    val posts by vm.posts.collectAsStateWithLifecycle()
    val status by vm.relayStatus.collectAsStateWithLifecycle()
    val inbox by BoardInbox.pending.collectAsStateWithLifecycle()

    DisposableEffect(Unit) { vm.start(); onDispose { vm.stop() } }

    var showNew by remember { mutableStateOf(false) }
    var showJoin by remember { mutableStateOf<String?>(null) }
    var showMeet by remember { mutableStateOf(false) }
    var confirmLeave by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }

    LaunchedEffect(inbox) { inbox?.let { showJoin = it; BoardInbox.pending.value = null } }

    val now = System.currentTimeMillis()
    val meets = posts.filter { it.p.y == "meet" }
    val rsvps = posts.filter { it.p.y == "rsvp" }
        .groupBy { it.p.r }
        .mapValues { (_, l) -> l.groupBy { it.author }.mapValues { it.value.maxBy { p -> p.createdAt } }.values.toList() }
    val upcoming = meets.filter { it.p.w > now - 3 * 3600_000 }.sortedBy { it.p.w }
    val feed = posts.filter { it.p.y == "msg" || it.p.y == "meet" }
    val listState = rememberLazyListState()
    LaunchedEffect(feed.size) { if (feed.isNotEmpty()) listState.animateScrollToItem(feed.size - 1 + if (upcoming.isNotEmpty()) 1 else 0) }

    Column(Modifier.fillMaxSize()) {
        // Skupiny + stav
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                groups.forEach { g ->
                    com.example.antimarcipan.ui.GoldChip(selected = current?.secretHex == g.secretHex, onClick = { vm.select(g) }, label = { Text(g.name) })
                }
                com.example.antimarcipan.ui.GoldChip(selected = false, onClick = { showNew = true }, label = { Text("+ Skupina") })
            }
            Box(Modifier.size(10.dp).clip(CircleShape).background(if (status.first > 0) Color(0xFF5FD38A) else Color(0xFFD98324)))
            Text(" ${status.first}/${status.second}", style = MaterialTheme.typography.labelSmall)
        }
        if (current == null) {
            Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Nástěnka", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Text(
                    "Založ skupinu (třeba vaše herna) a pošli kamarádům pozvánku. Ve skupině si píšete a domlouváte schůzky. " +
                        "Běží přes síť Nostr — žádný účet ani náš server; zprávy jsou šifrované a přečtou je jen členové.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                androidx.compose.material3.Button(onClick = { showNew = true }) { Text("Založit skupinu") }
                OutlinedButton(onClick = {
                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    showJoin = cm.primaryClip?.getItemAt(0)?.coerceToText(ctx)?.toString().orEmpty()
                }) { Text("Mám pozvánku") }
            }
        } else {
            val g = current!!
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(g.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                IconButton(onClick = {
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(
                        Intent.EXTRA_TEXT,
                        "Pozvánka do skupiny „${g.name}“ v appce Antimarcipán (otevři v appce → Přátelé → Nástěnka):\n\n${g.invite}",
                    )
                    ctx.startActivity(Intent.createChooser(send, "Pozvat do skupiny").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }) { Icon(Icons.Filled.Share, contentDescription = "Pozvat") }
                TextButton(onClick = {
                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    showJoin = cm.primaryClip?.getItemAt(0)?.coerceToText(ctx)?.toString().orEmpty()
                }) { Text("Připojit") }
                TextButton(onClick = { confirmLeave = true }) { Text("Opustit") }
            }
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState, contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp)) {
                if (upcoming.isNotEmpty()) item {
                    Text("Nadcházející schůzky", Modifier.padding(4.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    upcoming.forEach { m -> MeetCard(m, rsvps[m.id].orEmpty(), vm.myPub) { vm.rsvp(m.id, it) } }
                }
                if (feed.isEmpty()) item {
                    Text("Zatím tu nic není. Napiš první zprávu nebo naplánuj schůzku 📅.", Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall)
                }
                items(feed, key = { it.id }) { p ->
                    if (p.p.y == "meet") Text(
                        "${p.p.n} naplánoval/a schůzku: ${p.p.ti} · ${dateFmt.format(Date(p.p.w))}",
                        Modifier.fillMaxWidth().padding(6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    ) else MessageBubble(p)
                }
            }
            // Psaní
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { showMeet = true }) { Icon(Icons.Filled.Event, contentDescription = "Naplánovat schůzku", tint = MaterialTheme.colorScheme.primary) }
                OutlinedTextField(text, { text = it }, Modifier.weight(1f), placeholder = { Text("Zpráva pro skupinu…") }, maxLines = 4)
                IconButton(onClick = { vm.sendMessage(text); text = "" }, enabled = text.isNotBlank()) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Odeslat", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    if (showNew) {
        var n by remember { mutableStateOf("") }
        GoldDialog(
            onDismissRequest = { showNew = false },
            title = { Text("Nová skupina") },
            text = { OutlinedTextField(n, { n = it }, singleLine = true, label = { Text("Název (např. Herna u Petra)") }) },
            confirmButton = { TextButton(onClick = { vm.createGroup(n); showNew = false }) { Text("Založit") } },
            dismissButton = { TextButton(onClick = { showNew = false }) { Text("Zrušit") } },
        )
    }
    showJoin?.let { init ->
        var t by remember(init) { mutableStateOf(init) }
        val g = BoardGroup.fromInvite(t)
        GoldDialog(
            onDismissRequest = { showJoin = null },
            title = { Text("Připojit ke skupině") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (g != null) Text("Skupina: ${g.name}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    else Text("Vlož pozvánku (text obsahující AMGROUP1:…).", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(t, { t = it }, label = { Text("Pozvánka") }, maxLines = 3, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(enabled = g != null, onClick = { if (vm.joinGroup(t)) Toast.makeText(ctx, "Připojeno", Toast.LENGTH_SHORT).show(); showJoin = null }) { Text("Připojit") }
            },
            dismissButton = { TextButton(onClick = { showJoin = null }) { Text("Zrušit") } },
        )
    }
    if (confirmLeave) current?.let { g ->
        GoldDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text("Opustit skupinu?") },
            text = { Text("Skupina „${g.name}“ zmizí z telefonu. Vrátit se můžeš znovu přes pozvánku.") },
            confirmButton = { TextButton(onClick = { vm.leaveGroup(g); confirmLeave = false }) { Text("Opustit") } },
            dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("Zrušit") } },
        )
    }
    if (showMeet) MeetDialog(onDismiss = { showMeet = false }) { title, whenMs, place, note ->
        vm.createMeetup(title, whenMs, place, note); showMeet = false
    }
}

@Composable
private fun MessageBubble(p: Post) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = if (p.mine) Arrangement.End else Arrangement.Start) {
        Column(
            Modifier.widthIn(max = 300.dp).clip(RoundedCornerShape(14.dp))
                .background(if (p.mine) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.28f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            if (!p.mine) Text(p.p.n, style = MaterialTheme.typography.labelMedium, color = nameColor(p.author), fontWeight = FontWeight.Bold)
            Text(p.p.x, style = MaterialTheme.typography.bodyMedium)
            Text(timeFmt.format(Date(p.createdAt * 1000)), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun MeetCard(m: Post, answers: List<Post>, myPub: String, onRsvp: (String) -> Unit) {
    val mine = answers.firstOrNull { it.author == myPub }?.p?.g
    Card(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        border = BorderStroke(1.dp, GoldBorderBrush),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("📅 ${m.p.ti}", style = MaterialTheme.typography.titleSmall)
            Text(dateFmt.format(Date(m.p.w)) + if (m.p.pl.isNotBlank()) " · 📍 ${m.p.pl}" else "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            if (m.p.x.isNotBlank()) Text(m.p.x, style = MaterialTheme.typography.bodySmall)
            Text("Pořádá ${m.p.n}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
            listOf("yes" to "Jdu ✓", "maybe" to "Možná", "no" to "Nejdu").let { opts ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    opts.forEach { (k, l) ->
                        val cnt = answers.count { it.p.g == k }
                        com.example.antimarcipan.ui.GoldChip(selected = mine == k, onClick = { onRsvp(k) }, label = { Text("$l $cnt") })
                    }
                }
                val going = answers.filter { it.p.g == "yes" }.map { it.p.n }
                if (going.isNotEmpty()) Text("Jdou: " + going.joinToString(", "), style = MaterialTheme.typography.labelSmall, color = Color(0xFF8FD6A0))
            }
        }
    }
}

@Composable
private fun MeetDialog(onDismiss: () -> Unit, onCreate: (String, Long, String, String) -> Unit) {
    val ctx = LocalContext.current
    var title by remember { mutableStateOf("Hraní Magicu") }
    var place by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var whenMs by remember {
        mutableLongStateOf(Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 18); set(Calendar.MINUTE, 0) }.timeInMillis)
    }
    GoldDialog(
        onDismissRequest = onDismiss,
        title = { Text("Naplánovat schůzku") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, singleLine = true, label = { Text("Co") })
                OutlinedButton(onClick = {
                    val c = Calendar.getInstance().apply { timeInMillis = whenMs }
                    android.app.DatePickerDialog(ctx, { _, y, mo, d ->
                        c.set(y, mo, d)
                        android.app.TimePickerDialog(ctx, { _, h, mi ->
                            c.set(Calendar.HOUR_OF_DAY, h); c.set(Calendar.MINUTE, mi); whenMs = c.timeInMillis
                        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
                    }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                }) { Text("Kdy: " + dateFmt.format(Date(whenMs))) }
                OutlinedTextField(place, { place = it }, singleLine = true, label = { Text("Kde") })
                OutlinedTextField(note, { note = it }, label = { Text("Poznámka (formát, co přinést…)") }, maxLines = 3)
            }
        },
        confirmButton = { TextButton(enabled = title.isNotBlank(), onClick = { onCreate(title, whenMs, place, note) }) { Text("Zveřejnit") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Zrušit") } },
    )
}
