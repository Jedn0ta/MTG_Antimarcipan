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
    val inbox by BoardInbox.pending.collectAsStateWithLifecycle()
    val status by vm.relayStatus.collectAsStateWithLifecycle()
    DisposableEffect(Unit) { vm.start(); onDispose { vm.visible = false } }

    // null = přehled skupin, jinak otevřená skupina
    var openId by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    var showNew by remember { mutableStateOf(false) }
    var showJoin by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(inbox) { inbox?.let { showJoin = it; BoardInbox.pending.value = null } }
    // Po založení / připojení otevřít skupinu
    LaunchedEffect(current?.secretHex) { if (openId != null && current != null) openId = current!!.secretHex }

    val open = groups.firstOrNull { it.secretHex == openId }
    if (open != null) {
        androidx.activity.compose.BackHandler { openId = null }
        LaunchedEffect(open.secretHex) { if (current?.secretHex != open.secretHex) vm.select(open) }
        DisposableEffect(open.secretHex) { vm.visible = true; vm.markRead(open); onDispose { vm.visible = false } }
        GroupView(open, status, onBack = { openId = null })
    } else {
        GroupList(
            groups, status,
            onOpen = { vm.select(it); openId = it.secretHex },
            onNew = { showNew = true },
            onJoin = {
                val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                showJoin = cm.primaryClip?.getItemAt(0)?.coerceToText(ctx)?.toString().orEmpty()
            },
        )
    }

    if (showNew) {
        var n by remember { mutableStateOf("") }
        GoldDialog(
            onDismissRequest = { showNew = false },
            title = { Text(tr("Nová skupina")) },
            text = { OutlinedTextField(n, { n = it }, singleLine = true, label = { Text(tr("Název (např. Herna u Petra)")) }) },
            confirmButton = { TextButton(onClick = { vm.createGroup(n); showNew = false; openId = vm.current.value?.secretHex }) { Text(tr("Založit")) } },
            dismissButton = { TextButton(onClick = { showNew = false }) { Text(tr("Zrušit")) } },
        )
    }
    showJoin?.let { init ->
        var t by remember(init) { mutableStateOf(init) }
        val inv = BoardGroup.fromInvite(t)
        GoldDialog(
            onDismissRequest = { showJoin = null },
            title = { Text(tr("Připojit ke skupině")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (inv != null) Text("Skupina: ${inv.name}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    else Text(tr("Vlož pozvánku (text obsahující AMGROUP1:…)."), style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(t, { t = it }, label = { Text(tr("Pozvánka")) }, maxLines = 3, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(enabled = inv != null, onClick = {
                    if (vm.joinGroup(t)) { Toast.makeText(ctx, tr("Připojeno"), Toast.LENGTH_SHORT).show(); openId = vm.current.value?.secretHex }
                    showJoin = null
                }) { Text(tr("Připojit")) }
            },
            dismissButton = { TextButton(onClick = { showJoin = null }) { Text(tr("Zrušit")) } },
        )
    }
}

// ───────────────────────────────────────────── Přehled skupin

@Composable
internal fun GroupList(
    groups: List<BoardGroup>,
    status: Pair<Int, Int>,
    onOpen: (BoardGroup) -> Unit,
    onNew: () -> Unit,
    onJoin: () -> Unit,
) {
    val all by Board.byGroup.collectAsStateWithLifecycle()
    val unread by Board.unread.collectAsStateWithLifecycle()
    androidx.compose.material3.Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        floatingActionButton = {
            androidx.compose.material3.ExtendedFloatingActionButton(
                onClick = onNew,
                icon = { Icon(Icons.Filled.AddCircle, null) },
                text = { Text(tr("Nová skupina")) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color(0xFF1E1633),
            )
        },
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(if (status.first > 0) Color(0xFF5FD38A) else Color(0xFFD98324)))
                    Text(
                        if (status.first > 0) "  Online (${status.first}/${status.second} relaye)" else tr("  Připojuji…"),
                        Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f),
                    )
                    TextButton(onClick = onJoin) { Text(tr("Mám pozvánku")) }
                }
            }
            if (groups.isEmpty()) item {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("💬", fontSize = androidx.compose.ui.unit.TextUnit(48f, androidx.compose.ui.unit.TextUnitType.Sp))
                    Text(tr("Zatím žádná skupina"), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text(
                        tr("Založ skupinu pro svou partu a pozvi přátele. Píšete si, plánujete schůzky a hlasujete — šifrovaně, bez účtu."),
                        style = MaterialTheme.typography.bodySmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
            items(groups, key = { it.secretHex }) { g ->
                val posts = all[g.tag].orEmpty()
                val last = posts.lastOrNull { it.p.y in setOf("msg", "meet", "poll", "meta") }
                val u = unread[g.tag] ?: 0
                val nextMeet = posts.filter { it.p.y == "meet" && it.p.w > System.currentTimeMillis() }.minByOrNull { it.p.w }
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF2B2252)).clickable { onOpen(g) }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(48.dp).clip(CircleShape).background(nameColor(g.tag).copy(alpha = 0.9f)), contentAlignment = Alignment.Center) {
                        Text(g.name.take(1).uppercase(), color = Color(0xFF1E1633), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    }
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(g.name, style = MaterialTheme.typography.titleSmall, fontWeight = if (u > 0) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
                        Text(
                            last?.let { preview(it) } ?: tr("Zatím žádné zprávy"),
                            style = MaterialTheme.typography.bodySmall, maxLines = 1,
                            color = if (u > 0) Color.White else Color.White.copy(alpha = 0.6f),
                        )
                        nextMeet?.let {
                            Text("📅 ${it.p.ti} · ${dateFmt.format(Date(it.p.w))}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, maxLines = 1)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        last?.let { Text(shortTime(it.createdAt), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f)) }
                        if (u > 0) Box(
                            Modifier.padding(top = 4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary).padding(horizontal = 7.dp, vertical = 2.dp),
                        ) { Text("$u", color = Color(0xFF1E1633), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                    }
                }
            }
            item { Box(Modifier.size(80.dp)) }
        }
    }
}

internal fun preview(p: Post): String = when (p.p.y) {
    "msg" -> (if (p.mine) tr("Ty: ") else "${p.p.n}: ") + p.p.x
    "meet" -> "📅 ${p.p.n}: ${p.p.ti}"
    "poll" -> "📊 ${p.p.n}: ${p.p.ti}"
    "meta" -> tr("✏️ přejmenováno na {0}", (p.p.ti))
    else -> ""
}

internal fun shortTime(sec: Long): String {
    val d = Date(sec * 1000)
    val today = SimpleDateFormat("yyyyMMdd", Locale.ROOT).format(Date())
    return if (SimpleDateFormat("yyyyMMdd", Locale.ROOT).format(d) == today) hourFmt.format(d) else SimpleDateFormat("d. M.", Locale("cs")).format(d)
}
