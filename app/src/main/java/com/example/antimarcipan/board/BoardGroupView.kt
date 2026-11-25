package com.example.antimarcipan.board

import com.example.antimarcipan.i18n.tr

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

// ───────────────────────────────────────────── Detail skupiny (Chat / Schůzky / Ankety)

@Composable
internal fun GroupView(g: BoardGroup, status: Pair<Int, Int>, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val vm = Board
    val posts by vm.posts.collectAsStateWithLifecycle()
    LaunchedEffect(posts.size) { vm.markRead(g) }

    var tab by androidx.compose.runtime.saveable.rememberSaveable(g.secretHex) { mutableStateOf(0) }
    var showMeet by remember { mutableStateOf(false) }
    var showPoll by remember { mutableStateOf(false) }
    var showInvite by remember { mutableStateOf(false) }
    var showMembers by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var confirmLeave by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Post?>(null) }
    var replyTo by remember { mutableStateOf<Post?>(null) }
    var msgMenu by remember { mutableStateOf<Post?>(null) }
    var text by remember { mutableStateOf("") }

    val deleted = posts.filter { it.p.y == "del" }.map { it.p.r to it.author }.toSet()
    val vis = posts.filter { p -> (p.id to p.author) !in deleted }
    val byId = vis.associateBy { it.id }
    val now = System.currentTimeMillis()
    val rsvps = vis.filter { it.p.y == "rsvp" }.groupBy { it.p.r }
        .mapValues { (_, l) -> l.groupBy { it.author }.mapValues { it.value.maxBy { p -> p.createdAt } }.values.toList() }
    val votes = vis.filter { it.p.y == "vote" }.groupBy { it.p.r }
        .mapValues { (_, l) -> l.groupBy { it.author }.mapValues { it.value.maxBy { p -> p.createdAt } }.values.toList() }
    val meets = vis.filter { it.p.y == "meet" }
    val upcoming = meets.filter { it.p.w > now - 3 * 3600_000 }.sortedBy { it.p.w }
    val past = meets.filter { it.p.w <= now - 3 * 3600_000 }.sortedByDescending { it.p.w }
    val polls = vis.filter { it.p.y == "poll" }.sortedByDescending { it.createdAt }
    val chat = vis.filter { it.p.y in setOf("msg", "meet", "poll", "meta") }
    val members = vis.groupBy { it.author }.map { (k, l) -> Triple(k, l.maxBy { it.createdAt }.p.n, l.maxOf { it.createdAt }) }
        .sortedByDescending { it.third }

    Column(Modifier.fillMaxSize()) {
        // Hlavička
        Row(Modifier.fillMaxWidth().padding(end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Zpět na skupiny")) }
            Box(Modifier.size(36.dp).clip(CircleShape).background(nameColor(g.tag).copy(alpha = 0.9f)), contentAlignment = Alignment.Center) {
                Text(g.name.take(1).uppercase(), color = Color(0xFF1E1633), fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f).padding(start = 10.dp).clickable { showMembers = true }) {
                Text(g.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, maxLines = 1)
                Text(
                    "${members.size} ${if (members.size == 1) "člen" else if (members.size in 2..4) "členové" else "členů"}" +
                        if (status.first == 0) tr(" · připojuji…") else "",
                    style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f),
                )
            }
            IconButton(onClick = { showInvite = true }) { Icon(Icons.Filled.PersonAdd, contentDescription = tr("Přidat přítele"), tint = MaterialTheme.colorScheme.primary) }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = tr("Možnosti skupiny")) }
                androidx.compose.material3.DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    androidx.compose.material3.DropdownMenuItem(text = { Text(tr("Přejmenovat skupinu")) }, onClick = { menu = false; showRename = true }, leadingIcon = { Icon(Icons.Filled.Edit, null) })
                    androidx.compose.material3.DropdownMenuItem(text = { Text(tr("Členové")) }, onClick = { menu = false; showMembers = true }, leadingIcon = { Icon(Icons.Filled.Group, null) })
                    androidx.compose.material3.DropdownMenuItem(text = { Text(tr("Zkopírovat pozvánku")) }, onClick = {
                        menu = false
                        (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(android.content.ClipData.newPlainText(tr("Pozvánka"), g.invite))
                        Toast.makeText(ctx, tr("Pozvánka zkopírována"), Toast.LENGTH_SHORT).show()
                    }, leadingIcon = { Icon(Icons.Filled.ContentCopy, null) })
                    androidx.compose.material3.DropdownMenuItem(text = { Text(tr("Opustit skupinu"), color = Color(0xFFFF8A8A)) }, onClick = { menu = false; confirmLeave = true }, leadingIcon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = Color(0xFFFF8A8A)) })
                }
            }
        }
        // Záložky
        androidx.compose.material3.TabRow(
            selectedTabIndex = tab, containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.primary,
        ) {
            listOf("Chat", tr("Schůzky ({0})", (upcoming.size)), "Ankety (${polls.size})").forEachIndexed { i, t ->
                androidx.compose.material3.Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t, maxLines = 1) })
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (tab) {
                0 -> ChatTab(chat, byId, votes, onLong = { msgMenu = it }, onVote = { id, c -> vm.vote(id, c) }, myPub = vm.myPub, nextMeet = upcoming.firstOrNull(), onOpenMeets = { tab = 1 })
                1 -> LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp)) {
                    if (upcoming.isEmpty() && past.isEmpty()) item { EmptyHint(tr("Žádné schůzky"), tr("Naplánuj první — vyber den, čas a místo.")) }
                    if (upcoming.isNotEmpty()) item { SectionLabel(tr("Nadcházející")) }
                    items(upcoming, key = { "u" + it.id }) { m -> MeetCard(m, rsvps[m.id].orEmpty(), vm.myPub) { vm.rsvp(m.id, it) } }
                    if (past.isNotEmpty()) item { SectionLabel(tr("Proběhlé")) }
                    items(past, key = { "p" + it.id }) { m -> Box(Modifier.alpha(0.6f)) { MeetCard(m, rsvps[m.id].orEmpty(), vm.myPub) { vm.rsvp(m.id, it) } } }
                    item { Box(Modifier.size(80.dp)) }
                }
                else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp)) {
                    if (polls.isEmpty()) item { EmptyHint(tr("Žádné ankety"), tr("Zeptej se party — formát, den, kdo co přinese…")) }
                    items(polls, key = { it.id }) { p -> PollCard(p, votes[p.id].orEmpty(), vm.myPub, onVote = { vm.vote(p.id, it) }, onLongPress = { if (p.mine) deleting = p }) }
                    item { Box(Modifier.size(80.dp)) }
                }
            }
            if (tab != 0) androidx.compose.material3.ExtendedFloatingActionButton(
                onClick = { if (tab == 1) showMeet = true else showPoll = true },
                icon = { Icon(if (tab == 1) Icons.Filled.Event else Icons.Filled.AddCircle, null) },
                text = { Text(if (tab == 1) tr("Naplánovat") else tr("Nová anketa")) },
                containerColor = MaterialTheme.colorScheme.primary, contentColor = Color(0xFF1E1633),
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            )
        }

        // Psaní (jen v chatu)
        if (tab == 0) {
            replyTo?.let { r ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp).clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(alpha = 0.3f)).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(3.dp, 28.dp).background(MaterialTheme.colorScheme.primary))
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Text(tr("Odpověď pro {0}", (r.p.n)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(r.p.x.ifBlank { r.p.ti }, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                    IconButton(onClick = { replyTo = null }) { Icon(Icons.Filled.Close, contentDescription = tr("Zrušit odpověď")) }
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp), verticalAlignment = Alignment.Bottom) {
                var attach by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { attach = true }) {
                        Icon(Icons.Filled.AddCircle, contentDescription = tr("Schůzka nebo anketa"), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                    }
                    androidx.compose.material3.DropdownMenu(expanded = attach, onDismissRequest = { attach = false }) {
                        androidx.compose.material3.DropdownMenuItem(text = { Text(tr("📅  Schůzka")) }, onClick = { attach = false; showMeet = true })
                        androidx.compose.material3.DropdownMenuItem(text = { Text("📊  Anketa") }, onClick = { attach = false; showPoll = true })
                    }
                }
                OutlinedTextField(text, { text = it }, Modifier.weight(1f), placeholder = { Text(tr("Zpráva…")) }, maxLines = 5, shape = RoundedCornerShape(22.dp))
                IconButton(onClick = { vm.sendMessage(text, replyTo?.id ?: ""); text = ""; replyTo = null }, enabled = text.isNotBlank()) {
                    Box(
                        Modifier.size(42.dp).clip(CircleShape).background(if (text.isNotBlank()) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = tr("Odeslat"), tint = Color(0xFF1E1633)) }
                }
            }
        }
    }

    // Akce nad zprávou (podržení)
    msgMenu?.let { p ->
        GoldDialog(
            onDismissRequest = { msgMenu = null },
            title = { Text(p.p.n) },
            text = {
                Column {
                    Text(p.p.x.ifBlank { p.p.ti }, style = MaterialTheme.typography.bodySmall, maxLines = 3)
                    androidx.compose.material3.HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    MenuRow(tr("↩  Odpovědět")) { replyTo = p; msgMenu = null }
                    MenuRow(tr("⧉  Kopírovat text")) {
                        (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(android.content.ClipData.newPlainText(tr("Zpráva"), p.p.x))
                        Toast.makeText(ctx, tr("Zkopírováno"), Toast.LENGTH_SHORT).show(); msgMenu = null
                    }
                    if (p.mine) MenuRow(tr("🗑  Smazat u všech"), Color(0xFFFF8A8A)) { deleting = p; msgMenu = null }
                }
            },
            confirmButton = { TextButton(onClick = { msgMenu = null }) { Text(tr("Zavřít")) } },
        )
    }
    if (showRename) {
        var n by remember { mutableStateOf(g.name) }
        GoldDialog(
            onDismissRequest = { showRename = false },
            title = { Text(tr("Přejmenovat skupinu")) },
            text = { Column { OutlinedTextField(n, { n = it }, singleLine = true, label = { Text(tr("Nový název")) }); Text(tr("Změna se projeví všem členům."), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 6.dp)) } },
            confirmButton = { TextButton(enabled = n.isNotBlank() && n != g.name, onClick = { vm.renameGroup(n); showRename = false }) { Text(tr("Uložit")) } },
            dismissButton = { TextButton(onClick = { showRename = false }) { Text(tr("Zrušit")) } },
        )
    }
    if (showMembers) GoldDialog(
        onDismissRequest = { showMembers = false },
        title = { Text(tr("Členové ({0})", (members.size))) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (members.isEmpty()) Text(tr("Zatím nikdo nic nenapsal."), style = MaterialTheme.typography.bodySmall)
                members.forEach { (pub, name, last) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(pub, name, 30)
                        Column(Modifier.padding(start = 10.dp)) {
                            Text(name + if (pub == vm.myPub) " (ty)" else "", style = MaterialTheme.typography.bodyMedium)
                            Text(tr("aktivní ") + timeFmt.format(Date(last * 1000)), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                        }
                    }
                }
                Text(tr("Členem je každý s pozvánkou; v seznamu jsou ti, kdo už něco napsali."), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
            }
        },
        confirmButton = { TextButton(onClick = { showMembers = false; showInvite = true }) { Text(tr("Přidat přítele")) } },
        dismissButton = { TextButton(onClick = { showMembers = false }) { Text(tr("Zavřít")) } },
    )
    if (confirmLeave) GoldDialog(
        onDismissRequest = { confirmLeave = false },
        title = { Text(tr("Opustit skupinu?")) },
        text = { Text(tr("Skupina „{0}“ zmizí z telefonu. Vrátit se můžeš přes pozvánku.", (g.name))) },
        confirmButton = { TextButton(onClick = { vm.leaveGroup(g); confirmLeave = false; onBack() }) { Text(tr("Opustit")) } },
        dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text(tr("Zrušit")) } },
    )
    deleting?.let { p ->
        GoldDialog(
            onDismissRequest = { deleting = null },
            title = { Text(tr("Smazat příspěvek?")) },
            text = { Text(tr("Příspěvek zmizí u všech členů.")) },
            confirmButton = { TextButton(onClick = { vm.deletePost(p.id); deleting = null }) { Text(tr("Smazat")) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(tr("Zrušit")) } },
        )
    }
    if (showInvite) InviteDialog(g) { showInvite = false }
    if (showMeet) MeetDialog(onDismiss = { showMeet = false }) { title, whenMs, place, note -> vm.createMeetup(title, whenMs, place, note); showMeet = false; tab = 1 }
    if (showPoll) PollDialog(onDismiss = { showPoll = false }) { q, opts, multi -> vm.createPoll(q, opts, multi); showPoll = false; tab = 2 }
}

@Composable
internal fun MenuRow(label: String, color: Color = Color.Unspecified, onClick: () -> Unit) {
    Text(label, Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(vertical = 12.dp, horizontal = 4.dp), color = color, style = MaterialTheme.typography.bodyLarge)
}

@Composable
internal fun SectionLabel(t: String) = Text(t, Modifier.padding(4.dp, 10.dp, 4.dp, 4.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

@Composable
internal fun EmptyHint(title: String, sub: String) {
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(sub, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
internal fun ChatTab(
    chat: List<Post>,
    byId: Map<String, Post>,
    votes: Map<String, List<Post>>,
    onLong: (Post) -> Unit,
    onVote: (String, Set<Int>) -> Unit,
    myPub: String,
    nextMeet: Post?,
    onOpenMeets: () -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(chat.size) { if (chat.isNotEmpty()) listState.scrollToItem(listState.layoutInfo.totalItemsCount.coerceAtLeast(1) - 1) }
    Column(Modifier.fillMaxSize()) {
        // Připnutá nejbližší schůzka
        nextMeet?.let { m ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp).clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)).clickable(onClick = onOpenMeets).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("📌", Modifier.padding(end = 8.dp))
                Column(Modifier.weight(1f)) {
                    Text(m.p.ti, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    Text(dateFmt.format(Date(m.p.w)) + if (m.p.pl.isNotBlank()) " · ${m.p.pl}" else "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, maxLines = 1)
                }
                Text("›", color = MaterialTheme.colorScheme.primary)
            }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
            if (chat.isEmpty()) item { EmptyHint(tr("Zatím je tu ticho"), tr("Napiš první zprávu. Tlačítkem ⊕ přidáš schůzku nebo anketu.")) }
            var lastDay = ""
            var lastAuthor = ""
            chat.forEach { p ->
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
                        "msg" -> MessageBubble(p, showName, quoted = byId[p.p.q], onLongPress = { onLong(p) })
                        "meet" -> SystemLine(tr("📅 {0} naplánoval/a: {1} · {2}", (p.p.n), (p.p.ti), (dateFmt.format(Date(p.p.w)))))
                        "meta" -> SystemLine(tr("✏️ {0} přejmenoval/a skupinu na „{1}“", (p.p.n), (p.p.ti)))
                        "poll" -> PollCard(p, votes[p.id].orEmpty(), myPub, onVote = { onVote(p.id, it) }, onLongPress = { onLong(p) })
                    }
                }
            }
        }
    }
}
