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
import kotlinx.coroutines.launch

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
    val vm = Board
    Board.init(ctx)
    val groups by vm.groups.collectAsStateWithLifecycle()
    val current by vm.current.collectAsStateWithLifecycle()
    val posts by vm.posts.collectAsStateWithLifecycle()
    val status by vm.relayStatus.collectAsStateWithLifecycle()
    val inbox by BoardInbox.pending.collectAsStateWithLifecycle()
    val unread by vm.unread.collectAsStateWithLifecycle()
    DisposableEffect(Unit) { vm.start(); vm.visible = true; onDispose { vm.visible = false } }
    LaunchedEffect(current, posts.size) { current?.let { vm.markRead(it) } }

    var showNew by remember { mutableStateOf(false) }
    var showJoin by remember { mutableStateOf<String?>(null) }
    var showMeet by remember { mutableStateOf(false) }
    var showPoll by remember { mutableStateOf(false) }
    var showInvite by remember { mutableStateOf(false) }
    var showMembers by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var confirmLeave by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var attach by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Post?>(null) }
    var text by remember { mutableStateOf("") }

    LaunchedEffect(inbox) { inbox?.let { showJoin = it; BoardInbox.pending.value = null } }

    // Odvozená data
    val deleted = posts.filter { it.p.y == "del" }.map { it.p.r to it.author }.toSet()
    val visiblePosts = posts.filter { p -> (p.id to p.author) !in deleted }
    val now = System.currentTimeMillis()
    val rsvps = visiblePosts.filter { it.p.y == "rsvp" }.groupBy { it.p.r }
        .mapValues { (_, l) -> l.groupBy { it.author }.mapValues { it.value.maxBy { p -> p.createdAt } }.values.toList() }
    val votes = visiblePosts.filter { it.p.y == "vote" }.groupBy { it.p.r }
        .mapValues { (_, l) -> l.groupBy { it.author }.mapValues { it.value.maxBy { p -> p.createdAt } }.values.toList() }
    val upcoming = visiblePosts.filter { it.p.y == "meet" && it.p.w > now - 3 * 3600_000 }.sortedBy { it.p.w }
    val feed = visiblePosts.filter { it.p.y in setOf("msg", "meet", "poll", "meta") }
    val members = visiblePosts.groupBy { it.author }.map { (k, l) -> Triple(k, l.maxBy { it.createdAt }.p.n, l.maxOf { it.createdAt }) }
        .sortedByDescending { it.third }

    val listState = rememberLazyListState()
    LaunchedEffect(feed.size, current) { if (feed.isNotEmpty()) listState.scrollToItem(listState.layoutInfo.totalItemsCount.coerceAtLeast(1) - 1) }

    Column(Modifier.fillMaxSize()) {
        // ── Výběr skupiny
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 4.dp, top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                groups.forEach { g ->
                    val u = unread[g.tag] ?: 0
                    val sel = current?.secretHex == g.secretHex
                    androidx.compose.material3.BadgedBox(badge = { if (u > 0 && !sel) androidx.compose.material3.Badge { Text("$u") } }) {
                        com.example.antimarcipan.ui.GoldChip(selected = sel, onClick = { vm.select(g) }, label = { Text(g.name) })
                    }
                }
                com.example.antimarcipan.ui.GoldChip(selected = false, onClick = { showNew = true }, label = { Text("+ Nová") })
                com.example.antimarcipan.ui.GoldChip(selected = false, onClick = {
                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    showJoin = cm.primaryClip?.getItemAt(0)?.coerceToText(ctx)?.toString().orEmpty()
                }, label = { Text("Mám pozvánku") })
            }
            Box(Modifier.padding(start = 6.dp).size(8.dp).clip(CircleShape).background(if (status.first > 0) Color(0xFF5FD38A) else Color(0xFFD98324)))
        }

        val g = current
        if (g == null) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("💬", fontSize = androidx.compose.ui.unit.TextUnit(48f, androidx.compose.ui.unit.TextUnitType.Sp))
                Text("Nástěnka", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                Text(
                    "Skupina pro vaši partu: zprávy, schůzky a ankety. Bez účtu a bez našeho serveru — šifrované, přečtou je jen pozvaní.",
                    style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                androidx.compose.material3.Button(onClick = { showNew = true }, modifier = Modifier.fillMaxWidth()) { Text("Založit skupinu") }
                OutlinedButton(onClick = {
                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    showJoin = cm.primaryClip?.getItemAt(0)?.coerceToText(ctx)?.toString().orEmpty()
                }, modifier = Modifier.fillMaxWidth()) { Text("Připojit se pozvánkou") }
            }
        } else {
            // ── Hlavička skupiny
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                border = BorderStroke(1.dp, GoldBorderBrush),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
            ) {
                Row(Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).clickable { showMembers = true }) {
                        Text(g.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, maxLines = 1)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            members.take(5).forEach { (pub, name, _) -> Avatar(pub, name, 18) }
                            Text(
                                "  ${members.size} ${if (members.size == 1) "člen" else if (members.size in 2..4) "členové" else "členů"} · ${status.first}/${status.second} relaye",
                                style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f),
                            )
                        }
                    }
                    androidx.compose.material3.FilledTonalButton(
                        onClick = { showInvite = true },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
                    ) { Icon(Icons.Filled.PersonAdd, null, Modifier.size(18.dp)); Text(" Přidat") }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Možnosti skupiny") }
                        androidx.compose.material3.DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            androidx.compose.material3.DropdownMenuItem(text = { Text("Přejmenovat skupinu") }, onClick = { menu = false; showRename = true }, leadingIcon = { Icon(Icons.Filled.Edit, null) })
                            androidx.compose.material3.DropdownMenuItem(text = { Text("Členové") }, onClick = { menu = false; showMembers = true }, leadingIcon = { Icon(Icons.Filled.Group, null) })
                            androidx.compose.material3.DropdownMenuItem(text = { Text("Zkopírovat pozvánku") }, onClick = {
                                menu = false
                                (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                                    .setPrimaryClip(android.content.ClipData.newPlainText("Pozvánka", g.invite))
                                Toast.makeText(ctx, "Pozvánka zkopírována", Toast.LENGTH_SHORT).show()
                            }, leadingIcon = { Icon(Icons.Filled.ContentCopy, null) })
                            androidx.compose.material3.DropdownMenuItem(text = { Text("Opustit skupinu", color = Color(0xFFFF8A8A)) }, onClick = { menu = false; confirmLeave = true }, leadingIcon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = Color(0xFFFF8A8A)) })
                        }
                    }
                }
            }

            // ── Obsah
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                if (upcoming.isNotEmpty()) item(key = "upcoming") {
                    Text("📌 Nadcházející", Modifier.padding(4.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    upcoming.forEach { m -> MeetCard(m, rsvps[m.id].orEmpty(), vm.myPub) { vm.rsvp(m.id, it) } }
                }
                if (feed.isEmpty()) item(key = "empty") {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Zatím je tu ticho.", style = MaterialTheme.typography.bodyMedium)
                        Text("Napiš první zprávu, naplánuj schůzku nebo založ anketu ＋", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                    }
                }
                var lastDay = ""
                var lastAuthor = ""
                feed.forEach { p ->
                    val day = dayFmt.format(Date(p.createdAt * 1000))
                    if (day != lastDay) {
                        lastDay = day; lastAuthor = ""
                        item(key = "d$day") {
                            Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(day, Modifier.clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(alpha = 0.3f)).padding(horizontal = 10.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.75f))
                            }
                        }
                    }
                    val showName = p.author != lastAuthor
                    lastAuthor = if (p.p.y == "msg") p.author else ""
                    item(key = p.id) {
                        when (p.p.y) {
                            "msg" -> MessageBubble(p, showName, onLongPress = { if (p.mine) deleting = p })
                            "meet" -> SystemLine("📅 ${p.p.n} naplánoval/a: ${p.p.ti} · ${dateFmt.format(Date(p.p.w))}")
                            "meta" -> SystemLine("✏️ ${p.p.n} přejmenoval/a skupinu na „${p.p.ti}“")
                            "poll" -> PollCard(p, votes[p.id].orEmpty(), vm.myPub, onVote = { vm.vote(p.id, it) }, onLongPress = { if (p.mine) deleting = p })
                        }
                    }
                }
            }

            // ── Psaní
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp), verticalAlignment = Alignment.Bottom) {
                Box {
                    IconButton(onClick = { attach = true }) {
                        Icon(Icons.Filled.AddCircle, contentDescription = "Přidat schůzku nebo anketu", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                    }
                    androidx.compose.material3.DropdownMenu(expanded = attach, onDismissRequest = { attach = false }) {
                        androidx.compose.material3.DropdownMenuItem(text = { Text("📅  Schůzka") }, onClick = { attach = false; showMeet = true })
                        androidx.compose.material3.DropdownMenuItem(text = { Text("📊  Anketa") }, onClick = { attach = false; showPoll = true })
                    }
                }
                OutlinedTextField(
                    text, { text = it }, Modifier.weight(1f),
                    placeholder = { Text("Zpráva pro ${g.name}…") }, maxLines = 5,
                    shape = RoundedCornerShape(22.dp),
                )
                IconButton(onClick = { vm.sendMessage(text); text = "" }, enabled = text.isNotBlank()) {
                    Box(
                        Modifier.size(42.dp).clip(CircleShape).background(if (text.isNotBlank()) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Odeslat", tint = Color(0xFF1E1633)) }
                }
            }
        }
    }

    // ── Dialogy
    if (showNew) {
        var n by remember { mutableStateOf("") }
        GoldDialog(
            onDismissRequest = { showNew = false },
            title = { Text("Nová skupina") },
            text = { OutlinedTextField(n, { n = it }, singleLine = true, label = { Text("Název (např. Herna u Petra)") }) },
            confirmButton = { TextButton(onClick = { vm.createGroup(n); showNew = false; showInvite = true }) { Text("Založit a pozvat") } },
            dismissButton = { TextButton(onClick = { showNew = false }) { Text("Zrušit") } },
        )
    }
    showJoin?.let { init ->
        var t by remember(init) { mutableStateOf(init) }
        val inv = BoardGroup.fromInvite(t)
        GoldDialog(
            onDismissRequest = { showJoin = null },
            title = { Text("Připojit ke skupině") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (inv != null) Text("Skupina: ${inv.name}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    else Text("Vlož pozvánku (text obsahující AMGROUP1:…).", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(t, { t = it }, label = { Text("Pozvánka") }, maxLines = 3, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(enabled = inv != null, onClick = { if (vm.joinGroup(t)) Toast.makeText(ctx, "Připojeno", Toast.LENGTH_SHORT).show(); showJoin = null }) { Text("Připojit") }
            },
            dismissButton = { TextButton(onClick = { showJoin = null }) { Text("Zrušit") } },
        )
    }
    if (showRename) current?.let { g ->
        var n by remember { mutableStateOf(g.name) }
        GoldDialog(
            onDismissRequest = { showRename = false },
            title = { Text("Přejmenovat skupinu") },
            text = {
                Column {
                    OutlinedTextField(n, { n = it }, singleLine = true, label = { Text("Nový název") })
                    Text("Změna se projeví všem členům.", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 6.dp))
                }
            },
            confirmButton = { TextButton(enabled = n.isNotBlank() && n != g.name, onClick = { vm.renameGroup(n); showRename = false }) { Text("Uložit") } },
            dismissButton = { TextButton(onClick = { showRename = false }) { Text("Zrušit") } },
        )
    }
    if (showMembers) GoldDialog(
        onDismissRequest = { showMembers = false },
        title = { Text("Členové (${members.size})") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (members.isEmpty()) Text("Zatím nikdo nic nenapsal.", style = MaterialTheme.typography.bodySmall)
                members.forEach { (pub, name, last) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(pub, name, 30)
                        Column(Modifier.padding(start = 10.dp)) {
                            Text(name + if (pub == vm.myPub) " (ty)" else "", style = MaterialTheme.typography.bodyMedium)
                            Text("aktivní " + timeFmt.format(Date(last * 1000)), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                        }
                    }
                }
                Text("Členem je každý, kdo má pozvánku; v seznamu jsou ti, kdo už něco napsali.", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
            }
        },
        confirmButton = { TextButton(onClick = { showMembers = false; showInvite = true }) { Text("Přidat přítele") } },
        dismissButton = { TextButton(onClick = { showMembers = false }) { Text("Zavřít") } },
    )
    if (confirmLeave) current?.let { g ->
        GoldDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text("Opustit skupinu?") },
            text = { Text("Skupina „${g.name}“ zmizí z telefonu. Vrátit se můžeš znovu přes pozvánku.") },
            confirmButton = { TextButton(onClick = { vm.leaveGroup(g); confirmLeave = false }) { Text("Opustit") } },
            dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("Zrušit") } },
        )
    }
    deleting?.let { p ->
        GoldDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Smazat příspěvek?") },
            text = { Text("Příspěvek zmizí u všech členů.") },
            confirmButton = { TextButton(onClick = { vm.deletePost(p.id); deleting = null }) { Text("Smazat") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Zrušit") } },
        )
    }
    if (showInvite) current?.let { g -> InviteDialog(g) { showInvite = false } }
    if (showMeet) MeetDialog(onDismiss = { showMeet = false }) { title, whenMs, place, note ->
        vm.createMeetup(title, whenMs, place, note); showMeet = false
    }
    if (showPoll) PollDialog(onDismiss = { showPoll = false }) { q, opts, multi -> vm.createPoll(q, opts, multi); showPoll = false }
}

private val dayFmt = SimpleDateFormat("EEEE d. MMMM", Locale("cs"))

@Composable
private fun Avatar(pub: String, name: String, size: Int) {
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
private fun SystemLine(t: String) {
    Text(
        t, Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 24.dp),
        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(p: Post, showName: Boolean, onLongPress: () -> Unit) {
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
            if (!p.mine && showName) Text(p.p.n, style = MaterialTheme.typography.labelMedium, color = nameColor(p.author), fontWeight = FontWeight.Bold)
            Text(p.p.x, style = MaterialTheme.typography.bodyMedium)
            Text(
                hourFmt.format(Date(p.createdAt * 1000)), Modifier.align(Alignment.End),
                style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.45f),
            )
        }
    }
}

private val hourFmt = SimpleDateFormat("H:mm", Locale("cs"))

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun PollCard(poll: Post, answers: List<Post>, myPub: String, onVote: (Set<Int>) -> Unit, onLongPress: () -> Unit) {
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
private fun PollDialog(onDismiss: () -> Unit, onCreate: (String, List<String>, Boolean) -> Unit) {
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


/** Pozvat přátele: přímo přes Wi-Fi (potvrdí ve své appce) nebo poslat pozvánku sdílením. */
@Composable
private fun InviteDialog(g: BoardGroup, onDismiss: () -> Unit) {
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
                        "Pozvánka do skupiny „${g.name}“ v appce Antimarcipán (sdílej do appky nebo zkopíruj a otevři Nástěnku):\n\n${g.invite}",
                    )
                    ctx.startActivity(Intent.createChooser(send, "Pozvat do skupiny").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }, modifier = Modifier.fillMaxWidth()) { Text("Poslat pozvánku zprávou") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Hotovo") } },
    )
}
