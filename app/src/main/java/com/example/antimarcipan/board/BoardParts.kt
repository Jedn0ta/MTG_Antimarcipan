package com.example.antimarcipan.board

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.automirrored.filled.ExitToApp
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.draw.alpha
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
import kotlinx.coroutines.launch

internal val dateFmt = SimpleDateFormat("EEE d. M. H:mm", Locale("cs"))

internal val timeFmt = SimpleDateFormat("d. M. H:mm", Locale("cs"))

internal fun nameColor(pub: String): Color {
    val palette = listOf(0xFFE2BC62, 0xFF8FD6A0, 0xFF8EC5FF, 0xFFFFA0A0, 0xFFB9A7EC, 0xFFFFC98A, 0xFF6FD2B0)
    return Color(palette[(pub.hashCode() and 0x7fffffff) % palette.size])
}

internal val dayFmt = SimpleDateFormat("EEEE d. MMMM", Locale("cs"))

@Composable
internal fun Avatar(pub: String, name: String, size: Int) {
    Box(
        Modifier.padding(end = 2.dp).size(size.dp).clip(CircleShape).background(nameColor(pub).copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.take(1).uppercase(), color = Color(0xFF1E1633), fontWeight = FontWeight.Bold,
            fontSize = androidx.compose.ui.unit.TextUnit(size * 0.5f, androidx.compose.ui.unit.TextUnitType.Sp),
        )
    }
}

@Composable
internal fun SystemLine(t: String) {
    Text(
        t, Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 24.dp),
        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun MessageBubble(p: Post, showName: Boolean, quoted: Post? = null, onLongPress: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = if (showName) 6.dp else 1.dp),
        horizontalArrangement = if (p.mine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (!p.mine) {
            if (showName) Avatar(p.author, p.p.n, 28) else Box(Modifier.size(30.dp))
            Box(Modifier.size(4.dp))
        }
        Column(
            Modifier.widthIn(max = 290.dp)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = if (p.mine) 16.dp else 4.dp, bottomEnd = if (p.mine) 4.dp else 16.dp))
                .background(if (p.mine) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color(0xFF2B2252))
                .combinedClickable(onClick = {}, onLongClick = onLongPress)
                .padding(horizontal = 12.dp, vertical = 7.dp),
        ) {
            if (showName && (!p.mine || p.p.s)) Row(verticalAlignment = Alignment.CenterVertically) {
                if (!p.mine) Text(p.p.n, style = MaterialTheme.typography.labelMedium, color = nameColor(p.author), fontWeight = FontWeight.Bold)
                if (p.p.s) MecenasBadge()
            }
            if (p.p.q.isNotBlank()) Row(
                Modifier.padding(vertical = 3.dp).clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = 0.25f)).padding(6.dp),
            ) {
                Box(Modifier.size(2.dp, 26.dp).background(MaterialTheme.colorScheme.primary))
                Column(Modifier.padding(start = 6.dp)) {
                    Text(quoted?.p?.n ?: "…", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(quoted?.let { it.p.x.ifBlank { it.p.ti } } ?: "zpráva byla smazána", style = MaterialTheme.typography.labelSmall, maxLines = 2, color = Color.White.copy(alpha = 0.75f))
                }
            }
            Text(p.p.x, style = MaterialTheme.typography.bodyMedium)
            Text(
                hourFmt.format(Date(p.createdAt * 1000)), Modifier.align(Alignment.End),
                style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.45f),
            )
        }
    }
}

internal val hourFmt = SimpleDateFormat("H:mm", Locale("cs"))

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun PollCard(poll: Post, answers: List<Post>, myPub: String, onVote: (Set<Int>) -> Unit, onLongPress: () -> Unit) {
    val choicesBy = answers.associate { it.author to it.p.g.split(",").mapNotNull { s -> s.toIntOrNull() }.toSet() }.filterValues { it.isNotEmpty() }
    val mine = choicesBy[myPub].orEmpty()
    val counts = poll.p.o.indices.map { i -> choicesBy.values.count { i in it } }
    val voters = choicesBy.size.coerceAtLeast(1)
    Card(
        Modifier.fillMaxWidth().padding(vertical = 6.dp).combinedClickable(onClick = {}, onLongClick = onLongPress),
        border = BorderStroke(1.dp, GoldBorderBrush),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2252)),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("📊 ${poll.p.ti}", style = MaterialTheme.typography.titleSmall)
            Text(
                "${poll.p.n} · ${if (poll.p.mu) "více odpovědí" else "jedna odpověď"} · hlasovalo ${choicesBy.size}",
                style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f),
            )
            poll.p.o.forEachIndexed { i, opt ->
                val sel = i in mine
                val frac = counts[i].toFloat() / voters
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .border(1.dp, if (sel) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                        .clickable {
                            val next = if (poll.p.mu) (if (sel) mine - i else mine + i) else (if (sel) emptySet() else setOf(i))
                            onVote(next)
                        },
                ) {
                    Box(Modifier.matchParentSize().fillMaxWidth(frac).background(MaterialTheme.colorScheme.primary.copy(alpha = if (sel) 0.35f else 0.15f)))
                    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (sel) "✓ " else "", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text(opt, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text("${counts[i]}", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
internal fun PollDialog(onDismiss: () -> Unit, onCreate: (String, List<String>, Boolean) -> Unit) {
    var q by remember { mutableStateOf("") }
    val opts = remember { androidx.compose.runtime.mutableStateListOf("", "") }
    var multi by remember { mutableStateOf(false) }
    GoldDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nová anketa") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(q, { q = it }, label = { Text("Otázka (např. Jaký formát v pátek?)") }, modifier = Modifier.fillMaxWidth())
                opts.forEachIndexed { i, o ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(o, { opts[i] = it }, singleLine = true, label = { Text("Možnost ${i + 1}") }, modifier = Modifier.weight(1f))
                        if (opts.size > 2) IconButton(onClick = { opts.removeAt(i) }) { Icon(Icons.Filled.Close, contentDescription = "Odebrat") }
                    }
                }
                if (opts.size < 8) TextButton(onClick = { opts.add("") }) { Text("+ Přidat možnost") }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { multi = !multi }) {
                    androidx.compose.material3.Checkbox(multi, { multi = it })
                    Text("Povolit více odpovědí")
                }
            }
        },
        confirmButton = {
            TextButton(enabled = q.isNotBlank() && opts.count { it.isNotBlank() } >= 2, onClick = { onCreate(q, opts.toList(), multi) }) { Text("Zveřejnit") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Zrušit") } },
    )
}

@Composable
internal fun MeetCard(m: Post, answers: List<Post>, myPub: String, onRsvp: (String) -> Unit) {
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
internal fun MeetDialog(onDismiss: () -> Unit, onCreate: (String, Long, String, String) -> Unit) {
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

/** Pozvat přátele: přímo přes Wi-Fi (potvrdí ve své appce) nebo poslat pozvánku sdílením. */
@Composable
internal fun InviteDialog(g: BoardGroup, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val lanOn by com.example.antimarcipan.friends.LanShare.running.collectAsStateWithLifecycle()
    val peers by com.example.antimarcipan.friends.LanShare.peers.collectAsStateWithLifecycle()
    val sent = remember { androidx.compose.runtime.mutableStateListOf<String>() }
    GoldDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pozvat do „${g.name}“") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Přátelé na této Wi-Fi", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                if (!lanOn) TextButton(onClick = { com.example.antimarcipan.friends.LanShare.enable(ctx, true) }) { Text("Zapnout viditelnost na Wi-Fi") }
                else if (peers.isEmpty()) Text("Hledám… (kamarád musí mít appku otevřenou a zapnutou viditelnost na Wi-Fi)", style = MaterialTheme.typography.bodySmall)
                peers.forEach { p ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📶 ${p.name}", Modifier.weight(1f))
                        if (p.name in sent) Text("Pozváno ✓", color = Color(0xFF8FD6A0))
                        else TextButton(onClick = {
                            scope.launch {
                                val ok = com.example.antimarcipan.friends.LanShare.sendInvite(p, g.invite, g.name).isSuccess
                                if (ok) sent += p.name else Toast.makeText(ctx, "Nepodařilo se poslat", Toast.LENGTH_SHORT).show()
                            }
                        }) { Text("Pozvat") }
                    }
                }
                androidx.compose.material3.HorizontalDivider()
                Text("Nebo pošli pozvánku zprávou (WhatsApp, Quick Share…). Kamarád ji otevře v appce a potvrdí.", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = {
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(
                        Intent.EXTRA_TEXT,
                        "Pozvánka do skupiny „${g.name}“ v appce Grimoar (sdílej do appky nebo zkopíruj a otevři Nástěnku):\n\n${g.invite}",
                    )
                    ctx.startActivity(Intent.createChooser(send, "Pozvat do skupiny").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }, modifier = Modifier.fillMaxWidth()) { Text("Poslat pozvánku zprávou") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Hotovo") } },
    )
}

/** Odznak Mecenáš — za „Kafe pro autora“. */
@Composable
internal fun MecenasBadge() {
    Text(
        "☕ Mecenáš",
        Modifier
            .padding(start = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFF6DE9A), Color(0xFFB98A35))))
            .padding(horizontal = 6.dp, vertical = 1.dp),
        color = Color(0xFF1E1633),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
    )
}
