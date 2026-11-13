package com.example.antimarcipan.friends

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.antimarcipan.data.local.DeckCardWithQty
import com.example.antimarcipan.ui.AppViewModelFactory
import com.example.antimarcipan.ui.GoldBorderBrush
import com.example.antimarcipan.ui.GoldDialog
import kotlinx.coroutines.launch

/** Přátelé: sdílení balíků (bez serveru — přes Quick Share/Wi-Fi, messenger, schránku) a rádce proti jejich balíkům. */
@Composable
fun FriendsScreen() {
    var tab by rememberSaveable { mutableStateOf(0) }
    val boardInbox by com.example.antimarcipan.board.BoardInbox.pending.collectAsStateWithLifecycle()
    LaunchedEffect(boardInbox) { if (boardInbox != null) tab = 1 }
    val ctx = LocalContext.current
    val vm: FriendsViewModel = viewModel(factory = AppViewModelFactory)
    val scope = rememberCoroutineScope()
    // Hledání na Wi-Fi běží, dokud jsou otevření Přátelé (i při přepnutí na Nástěnku)
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { LanShare.stop() } }
    val incoming by LanShare.incoming.collectAsStateWithLifecycle()
    val ack by LanShare.acks.collectAsStateWithLifecycle()
    LaunchedEffect(ack) { ack?.let { Toast.makeText(ctx, it, Toast.LENGTH_LONG).show(); LanShare.acks.value = null } }
    incoming.firstOrNull()?.let { inc ->
        val deck = remember(inc) { DeckShareCodec.decode(inc.text) }
        if (deck == null) LaunchedEffect(inc) { LanShare.respond(inc, false, "?") }
        else GoldDialog(
            onDismissRequest = {},
            title = { Text("Příchozí balík") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${inc.from} ti posílá balík:", style = MaterialTheme.typography.bodyMedium)
                    Text("„${deck.name}“ · ${deck.total} karet", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Text("Po přijetí se uloží k příteli ${inc.from}.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.importDeck(deck, inc.from.takeIf { it != "?" } ?: deck.owner) { n -> Toast.makeText(ctx, "Balík uložen k: $n", Toast.LENGTH_SHORT).show() }
                    scope.launch { LanShare.respond(inc, true, deck.name) }
                }) { Text("Přijmout") }
            },
            dismissButton = { TextButton(onClick = { scope.launch { LanShare.respond(inc, false, deck.name) } }) { Text("Odmítnout") } },
        )
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            com.example.antimarcipan.ui.GoldChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("Přátelé") })
            com.example.antimarcipan.ui.GoldChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("Nástěnka") })
        }
        Box(Modifier.weight(1f)) {
            if (tab == 0) FriendsList() else com.example.antimarcipan.board.BoardScreen()
        }
    }
}

@Composable
private fun FriendsList(vm: FriendsViewModel = viewModel(factory = AppViewModelFactory)) {
    val ctx = LocalContext.current
    val friends by vm.friends.collectAsStateWithLifecycle()
    val decks by vm.friendDecks.collectAsStateWithLifecycle()
    val myDecks by vm.myDecks.collectAsStateWithLifecycle()
    val myName by vm.myName.collectAsStateWithLifecycle()
    val inbox by FriendsInbox.pending.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var openDeck by remember { mutableStateOf<FriendDeck?>(null) }
    var showShare by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var editName by remember { mutableStateOf(false) }
    var addFriend by remember { mutableStateOf(false) }
    var deleteFriend by remember { mutableStateOf<Friend?>(null) }
    val lanOn by LanShare.running.collectAsStateWithLifecycle()
    val peers by LanShare.peers.collectAsStateWithLifecycle()
    var sendTo by remember { mutableStateOf<LanPeer?>(null) }


    // Přijatý sdílený text (z jiné appky) → rovnou import
    LaunchedEffect(inbox) {
        inbox?.let { importText = it; showImport = true; FriendsInbox.pending.value = null }
    }

    openDeck?.let { fd ->
        FriendDeckView(fd, vm, onBack = { openDeck = null })
        return
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Moje jméno: ", style = MaterialTheme.typography.bodyMedium)
                Text(myName.ifBlank { "nenastaveno" }, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                TextButton(onClick = { editName = true }) { Text("Změnit") }
            }
            Text(
                "Balíky si posíláte přes sdílení telefonu — Quick Share (Wi-Fi/Bluetooth nablízko), messenger, e-mail nebo schránku. Žádný účet ani server.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            Row(Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showShare = true }, modifier = Modifier.weight(1f)) { Text("Poslat balík") }
                OutlinedButton(onClick = {
                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    importText = cm.primaryClip?.getItemAt(0)?.coerceToText(ctx)?.toString().orEmpty()
                    showImport = true
                }, modifier = Modifier.weight(1f)) { Text("Přijmout balík") }
            }
        }
        item {
            Card(
                Modifier.fillMaxWidth().padding(bottom = 10.dp),
                border = BorderStroke(1.dp, GoldBorderBrush),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Na této Wi-Fi", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        androidx.compose.material3.Switch(
                            checked = lanOn,
                            onCheckedChange = { on -> if (on) LanShare.start(ctx, myName) else LanShare.stop() },
                        )
                    }
                    if (!lanOn) Text(
                        "Zapni a uvidíš kamarády, kteří mají Antimarcipán otevřený na Přátelích a jsou na stejné Wi-Fi. Balík pošleš přímo, bez internetu.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    ) else if (peers.isEmpty()) Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("  Hledám… (kamarád musí mít otevřené Přátele a zapnuté hledání)", style = MaterialTheme.typography.bodySmall)
                    }
                    peers.forEach { p ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📶 ${p.name}", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            if (friends.none { it.name.equals(p.name, true) }) TextButton(onClick = { vm.addFriend(p.name) }) { Text("+ Přítel") }
                            Button(onClick = { sendTo = p }) { Text("Poslat") }
                        }
                    }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Přátelé (${friends.size})", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                TextButton(onClick = { addFriend = true }) { Text("+ Přidat") }
            }
        }
        if (friends.isEmpty()) item {
            Text("Zatím žádní přátelé. Přijmi balík od kamaráda a přidá se sám.", Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall)
        }
        items(friends, key = { it.id }) { f ->
            val fDecks = decks.filter { it.friendId == f.id }
            Card(
                Modifier.fillMaxWidth().padding(vertical = 5.dp),
                border = BorderStroke(1.dp, GoldBorderBrush),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                            Text(f.name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Text("  ${f.name}", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                        Text("${fDecks.size} balíků", style = MaterialTheme.typography.labelSmall)
                        IconButton(onClick = { deleteFriend = f }) { Icon(Icons.Filled.Delete, contentDescription = "Odebrat přítele") }
                    }
                    fDecks.forEach { d ->
                        val sd = remember(d.id) { DeckShareCodec.parseJson(d.json) }
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { openDeck = d }.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(d.name, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${com.example.antimarcipan.decks.DeckFormat.from(d.format).label} · ${sd?.total ?: 0} karet",
                                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                )
                            }
                            Text("›", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth().padding(top = 14.dp), colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.2f))) {
                Column(Modifier.padding(12.dp)) {
                    Text("Nástěnka", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "Zprávy a domlouvání schůzek najdeš v záložce Nástěnka nahoře — skupiny s pozvánkou, bez účtu a bez serveru.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }

    sendTo?.let { peer ->
        GoldDialog(
            onDismissRequest = { sendTo = null },
            title = { Text("Poslat ${peer.name}") },
            text = {
                Column {
                    if (myDecks.isEmpty()) Text("Nemáš žádný balík.")
                    myDecks.forEach { d ->
                        Text(
                            d.name,
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable {
                                scope.launch {
                                    val res = LanShare.send(peer, vm.shareText(d.id))
                                    Toast.makeText(ctx, if (res.isSuccess) "Odesláno: ${d.name} — čeká na potvrzení" else "Nepodařilo se odeslat — jste na stejné Wi-Fi?", Toast.LENGTH_SHORT).show()
                                    sendTo = null
                                }
                            }.padding(12.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        HorizontalDivider()
                    }
                }
            },
            confirmButton = { TextButton(onClick = { sendTo = null }) { Text("Zavřít") } },
        )
    }
    if (editName) {
        var n by remember { mutableStateOf(myName) }
        GoldDialog(
            onDismissRequest = { editName = false },
            title = { Text("Moje jméno") },
            text = { OutlinedTextField(n, { n = it }, singleLine = true, label = { Text("Jak tě uvidí kamarádi") }) },
            confirmButton = { TextButton(onClick = { vm.setMyName(n); editName = false }) { Text("Uložit") } },
            dismissButton = { TextButton(onClick = { editName = false }) { Text("Zrušit") } },
        )
    }
    if (addFriend) {
        var n by remember { mutableStateOf("") }
        GoldDialog(
            onDismissRequest = { addFriend = false },
            title = { Text("Nový přítel") },
            text = { OutlinedTextField(n, { n = it }, singleLine = true, label = { Text("Jméno") }) },
            confirmButton = { TextButton(onClick = { vm.addFriend(n); addFriend = false }) { Text("Přidat") } },
            dismissButton = { TextButton(onClick = { addFriend = false }) { Text("Zrušit") } },
        )
    }
    deleteFriend?.let { f ->
        GoldDialog(
            onDismissRequest = { deleteFriend = null },
            title = { Text("Odebrat přítele?") },
            text = { Text("Odebrat ${f.name} i všechny jeho uložené balíky?") },
            confirmButton = { TextButton(onClick = { vm.deleteFriend(f.id); deleteFriend = null }) { Text("Odebrat") } },
            dismissButton = { TextButton(onClick = { deleteFriend = null }) { Text("Zrušit") } },
        )
    }
    if (showShare) {
        GoldDialog(
            onDismissRequest = { showShare = false },
            title = { Text("Poslat balík") },
            text = {
                Column {
                    if (myName.isBlank()) Text("Tip: nastav si jméno, ať kamarád ví, od koho balík je.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    if (myDecks.isEmpty()) Text("Nemáš žádný balík.")
                    myDecks.forEach { d ->
                        Text(
                            d.name,
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable {
                                scope.launch {
                                    val text = vm.shareText(d.id)
                                    val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                                        .putExtra(Intent.EXTRA_SUBJECT, "Balík ${d.name}")
                                        .putExtra(Intent.EXTRA_TEXT, text)
                                    ctx.startActivity(Intent.createChooser(send, "Poslat balík").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                    showShare = false
                                }
                            }.padding(12.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        HorizontalDivider()
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showShare = false }) { Text("Zavřít") } },
        )
    }
    if (showImport) {
        val parsed = remember(importText) { DeckShareCodec.decode(importText) }
        var friendName by remember(parsed) { mutableStateOf(parsed?.owner ?: "") }
        GoldDialog(
            onDismissRequest = { showImport = false },
            title = { Text("Přijmout balík") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (parsed == null) {
                        Text("Vlož zprávu s balíkem (obsahuje kód AMDECK1:…). Zkopíruj ji v messengeru a ťukni Přijmout — schránka se načte sama.", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(importText, { importText = it }, label = { Text("Text / kód") }, modifier = Modifier.fillMaxWidth(), maxLines = 4)
                    } else {
                        Text("„${parsed.name}“ · ${parsed.total} karet", style = MaterialTheme.typography.titleSmall)
                        Text("Od: ${parsed.owner}", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(friendName, { friendName = it }, singleLine = true, label = { Text("Uložit k příteli") })
                        if (friends.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            friends.forEach { f -> com.example.antimarcipan.ui.GoldChip(selected = f.name.equals(friendName, true), onClick = { friendName = f.name }, label = { Text(f.name) }) }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = parsed != null, onClick = {
                    parsed?.let { vm.importDeck(it, friendName) { n -> Toast.makeText(ctx, "Balík uložen k: $n", Toast.LENGTH_SHORT).show() } }
                    showImport = false
                }) { Text("Uložit") }
            },
            dismissButton = { TextButton(onClick = { showImport = false }) { Text("Zrušit") } },
        )
    }
}

@Composable
private fun FriendDeckView(fd: FriendDeck, vm: FriendsViewModel, onBack: () -> Unit) {
    val deck = remember(fd.id) { DeckShareCodec.parseJson(fd.json) }
    val myDecks by vm.myDecks.collectAsStateWithLifecycle()
    var myDeckId by rememberSaveable { mutableStateOf<Long?>(null) }
    var report by remember { mutableStateOf<MatchupReport?>(null) }
    var confirmDel by remember { mutableStateOf(false) }
    androidx.activity.compose.BackHandler(onBack = onBack)
    LaunchedEffect(myDeckId) { report = myDeckId?.let { id -> deck?.let { vm.matchup(it, id) } } }
    val cards = deck?.toCards().orEmpty()

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět") }
            Column(Modifier.weight(1f)) {
                Text(fd.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text("od ${deck?.owner ?: "?"} · ${deck?.total ?: 0} karet", style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = { confirmDel = true }) { Icon(Icons.Filled.Delete, contentDescription = "Smazat balík") }
        }
        if (deck == null) { Text("Balík se nepodařilo načíst.", Modifier.padding(16.dp)); return@Column }
        com.example.antimarcipan.ui.CurveBlind(cards.map { DeckCardWithQty(it.first, it.second) })
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
            item {
                Text("Rádce: jak hrát proti tomuhle balíku", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Text("Vyber svůj balík:", style = MaterialTheme.typography.labelSmall)
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    myDecks.forEach { d -> com.example.antimarcipan.ui.GoldChip(selected = myDeckId == d.id, onClick = { myDeckId = d.id }, label = { Text(d.name) }) }
                }
            }
            report?.let { r ->
                item { Text(r.oppSummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)) }
                items(r.tips) { t ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), border = BorderStroke(1.dp, GoldBorderBrush)) {
                        Column(Modifier.padding(10.dp)) {
                            Text(t.title, style = MaterialTheme.typography.titleSmall)
                            Text(t.text, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (r.sideboard.isNotEmpty()) {
                    item { Text("Z tvé sbírky by pomohly:", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
                    items(r.sideboard, key = { "sb" + it.first.scryfallId }) { (c, why) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            com.example.antimarcipan.ui.InteractiveThumb(c, width = 36.dp, height = 50.dp)
                            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                                Text(c.name, style = MaterialTheme.typography.bodyMedium)
                                Text(why, style = MaterialTheme.typography.labelSmall, color = Color(0xFF8FD6A0))
                            }
                        }
                    }
                }
            }
            item { Text("Karty (${deck.total})", Modifier.padding(top = 12.dp, bottom = 4.dp), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary) }
            items(cards.sortedWith(compareBy({ com.example.antimarcipan.decks.LimitedAdvisor.isLand(it.first) }, { it.first.cmc ?: 0.0 }, { it.first.name })), key = { "c" + it.first.scryfallId }) { (c, q) ->
                androidx.compose.runtime.CompositionLocalProvider(com.example.antimarcipan.ui.LocalCardList provides cards.map { it.first }) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        com.example.antimarcipan.ui.InteractiveThumb(c, width = 36.dp, height = 50.dp)
                        Text("  ${q}× ${c.name}", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text(c.typeLine?.substringBefore("—")?.trim() ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
    if (confirmDel) GoldDialog(
        onDismissRequest = { confirmDel = false },
        title = { Text("Smazat balík?") },
        text = { Text("Smazat uložený balík „${fd.name}“?") },
        confirmButton = { TextButton(onClick = { vm.deleteDeck(fd.id); confirmDel = false; onBack() }) { Text("Smazat") } },
        dismissButton = { TextButton(onClick = { confirmDel = false }) { Text("Zrušit") } },
    )
}
