package com.example.antimarcipan.friends

import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.foundation.border
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

/** Příchozí balíky a žádosti o přátelství přes Wi-Fi — dialog se ukáže kdekoli v appce. */
@Composable
fun LanIncomingHost() {
    val ctx = LocalContext.current
    val vm: FriendsViewModel = viewModel(factory = AppViewModelFactory)
    val scope = rememberCoroutineScope()
    val incoming by LanShare.incoming.collectAsStateWithLifecycle()
    val ack by LanShare.acks.collectAsStateWithLifecycle()
    val accepted by LanShare.friendAccepted.collectAsStateWithLifecycle()
    LaunchedEffect(ack) { ack?.let { Toast.makeText(ctx, it, Toast.LENGTH_LONG).show(); LanShare.acks.value = null } }
    LaunchedEffect(accepted) {
        if (accepted.isEmpty()) return@LaunchedEffect
        val list = accepted
        LanShare.friendAccepted.value = emptyList()
        list.forEach { c ->
            val n = vm.addVerified(c)
            Toast.makeText(ctx, "$n tě přijal/a mezi přátele ✓", Toast.LENGTH_LONG).show()
        }
    }
    incoming.firstOrNull()?.let outer@{ inc ->
        // Žádost o přátelství
        FriendCode.decode(inc.text)?.let { card ->
            GoldDialog(
                onDismissRequest = {},
                title = { Text("Žádost o přátelství") },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FriendAvatar(card.n, card.c, card.a, 48.dp)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(card.n, style = MaterialTheme.typography.titleMedium)
                            Text("chce si tě přidat mezi přátele v Grimoaru.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch { vm.addVerified(card); LanShare.acceptFriend(inc) }
                    }) { Text("Přijmout") }
                },
                dismissButton = { TextButton(onClick = { LanShare.dismiss(inc) }) { Text("Odmítnout") } },
            )
            return@outer
        }
        val deck = remember(inc) { DeckShareCodec.decode(inc.text) }
        if (deck == null) LaunchedEffect(inc) { LanShare.respond(inc, false, "?") }
        else GoldDialog(
            onDismissRequest = {},
            title = { Text("Příchozí balík") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${inc.from} ti posílá balík:", style = MaterialTheme.typography.bodyMedium)
                    Text("„${deck.name}“ · ${deck.total} karet", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.importDeck(deck.copy(uid = deck.uid ?: inc.uid), inc.from.takeIf { it != "?" } ?: deck.owner) { n -> Toast.makeText(ctx, "Balík uložen k: $n", Toast.LENGTH_SHORT).show() }
                    scope.launch { LanShare.respond(inc, true, deck.name) }
                }) { Text("Přijmout") }
            },
            dismissButton = { TextButton(onClick = { scope.launch { LanShare.respond(inc, false, deck.name) } }) { Text("Odmítnout") } },
        )
    }
}

@Composable
fun FriendsScreen() {
    FriendsList()
}

private fun lastSeenLabel(t: Long): String {
    if (t <= 0) return ""
    val d = (System.currentTimeMillis() - t) / 86_400_000L
    return when { d < 1 -> "dnes"; d < 2 -> "včera"; d < 30 -> "před $d dny"; else -> "před ${d / 30} měs." }
}

@Composable
private fun SectionTitle(text: String, action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text.uppercase(), Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, letterSpacing = androidx.compose.ui.unit.TextUnit(1.2f, androidx.compose.ui.unit.TextUnitType.Sp))
        action?.invoke()
    }
}

@Composable
private fun FriendsList(vm: FriendsViewModel = viewModel(factory = AppViewModelFactory)) {
    val ctx = LocalContext.current
    val friends by vm.friends.collectAsStateWithLifecycle()
    val decks by vm.friendDecks.collectAsStateWithLifecycle()
    val myDecks by vm.myDecks.collectAsStateWithLifecycle()
    val me by Me.profile.collectAsStateWithLifecycle()
    val inbox by FriendsInbox.pending.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var openDeck by remember { mutableStateOf<FriendDeck?>(null) }
    var openFriend by remember { mutableStateOf<Long?>(null) }
    var showImport by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var editProfile by remember { mutableStateOf(false) }
    var addFriend by remember { mutableStateOf(false) }
    var showQr by remember { mutableStateOf(false) }
    var sendTo by remember { mutableStateOf<LanPeer?>(null) }
    var shareFor by remember { mutableStateOf<Friend?>(null) }
    var shareGeneric by remember { mutableStateOf(false) }
    val lanOn by LanShare.running.collectAsStateWithLifecycle()
    val peers by LanShare.peers.collectAsStateWithLifecycle()
    val added: (String?) -> Unit = { n ->
        Toast.makeText(ctx, if (n == null) "Kód přítele se nepodařilo přečíst" else "$n je teď tvůj přítel ✓", Toast.LENGTH_LONG).show()
    }
    val scanQr = {
        com.google.mlkit.vision.codescanner.GmsBarcodeScanning.getClient(
            ctx,
            com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE).build(),
        ).startScan()
            .addOnSuccessListener { b -> vm.addFromCode(b.rawValue.orEmpty(), added) }
            .addOnFailureListener { Toast.makeText(ctx, "Skener QR není dostupný", Toast.LENGTH_SHORT).show() }
        Unit
    }

    // Přijatý sdílený text (z jiné appky) → přítel nebo balík
    LaunchedEffect(inbox) {
        inbox?.let { t ->
            FriendsInbox.pending.value = null
            if (FriendCode.decode(t) != null) vm.addFromCode(t, added) else { importText = t; showImport = true }
        }
    }

    openDeck?.let { fd -> FriendDeckView(fd, vm, onBack = { openDeck = null }); return }
    friends.firstOrNull { it.id == openFriend }?.let { f ->
        FriendDetail(
            f, decks.filter { it.friendId == f.id }, peers.firstOrNull { p -> p.uid != null && p.uid == f.uid }, vm,
            onBack = { openFriend = null }, onOpenDeck = { openDeck = it }, onShare = { shareFor = f },
        )
        if (shareFor != null) ShareDeckDialog(myDecks, shareFor?.name, vm) { shareFor = null }
        return
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)) {
        // Profil
        item {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, GoldBorderBrush),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    FriendAvatar(Me.name, me.color, me.avatar, 56.dp)
                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                        Text(me.name.ifBlank { "Nastav si jméno" }, style = MaterialTheme.typography.titleLarge)
                        Text("Tvůj profil · ID ${me.uid.take(4)}…${me.uid.takeLast(4)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        TextButton(onClick = { editProfile = true }, contentPadding = PaddingValues(0.dp)) { Text("Upravit profil") }
                    }
                    androidx.compose.material3.FilledTonalIconButton(onClick = { showQr = true }, modifier = Modifier.size(52.dp)) {
                        Icon(Icons.Filled.QrCode2, contentDescription = "Můj QR kód")
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { addFriend = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.PersonAdd, null, Modifier.size(18.dp)); Text("  Přidat přítele")
                }
                OutlinedButton(onClick = { shareGeneric = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Share, null, Modifier.size(18.dp)); Text("  Poslat balík")
                }
            }
            TextButton(onClick = {
                val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val t = cm.primaryClip?.getItemAt(0)?.coerceToText(ctx)?.toString().orEmpty()
                if (FriendCode.decode(t) != null) vm.addFromCode(t, added) else { importText = t; showImport = true }
            }) { Icon(Icons.Filled.ContentPaste, null, Modifier.size(16.dp)); Text("  Vložit ze schránky (balík nebo pozvánka)") }
        }

        // V okolí
        item {
            SectionTitle("V okolí (Wi-Fi)") {
                androidx.compose.material3.Switch(checked = lanOn, onCheckedChange = { on -> LanShare.enable(ctx, on) })
            }
            if (!lanOn) Text(
                "Zapni, a uvidíš kamarády s Grimoarem na stejné Wi-Fi. Přidáš je jedním ťuknutím a balíky si pošlete přímo.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            ) else if (peers.isEmpty()) Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                androidx.compose.material3.CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                Text("  Hledám… kamarád musí mít Grimoar otevřený a viditelnost zapnutou.", style = MaterialTheme.typography.bodySmall)
            }
        }
        items(peers, key = { "p" + it.key }) { p ->
            val friend = friends.firstOrNull { f -> p.uid != null && f.uid == p.uid }
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                FriendAvatar(p.name, friend?.color ?: 0, friend?.avatar, 40.dp, online = true)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(p.name, style = MaterialTheme.typography.bodyLarge)
                    Text(if (friend != null) "přítel · v okolí" else "v okolí", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8FD6A0))
                }
                if (friend == null && p.uid != null) TextButton(onClick = {
                    scope.launch {
                        val r = LanShare.requestFriend(p)
                        Toast.makeText(ctx, if (r.isSuccess) "Žádost odeslána — ${p.name} ji musí potvrdit" else "Nepodařilo se odeslat", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Přidat") }
                OutlinedButton(onClick = { sendTo = p }) { Text("Balík") }
            }
        }

        // Přátelé
        item { SectionTitle("Přátelé (${friends.size})") }
        if (friends.isEmpty()) item {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.2f))) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Zatím žádní přátelé", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Přidej kamaráda naskenováním jeho QR kódu, v okolí na stejné Wi-Fi, nebo mu pošli pozvánku zprávou.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
            }
        }
        items(friends, key = { it.id }) { f ->
            val near = peers.any { p -> p.uid != null && p.uid == f.uid }
            val n = decks.count { it.friendId == f.id }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { openFriend = f.id }.padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FriendAvatar(f.name, f.color, f.avatar, 46.dp, online = near)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(f.name, style = MaterialTheme.typography.titleSmall)
                        if (f.verified) Icon(Icons.Filled.Verified, "Ověřený přítel", Modifier.padding(start = 4.dp).size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Text(
                        listOfNotNull(
                            if (near) "v okolí" else lastSeenLabel(f.lastSeen).takeIf { it.isNotEmpty() }?.let { "naposledy $it" },
                            if (n > 0) "$n ${if (n == 1) "balík" else if (n < 5) "balíky" else "balíků"}" else null,
                            if (!f.verified) "nespárováno" else null,
                        ).joinToString(" · ").ifEmpty { "přítel" },
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
                Text("›", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
        }
    }

    sendTo?.let { peer ->
        GoldDialog(
            onDismissRequest = { sendTo = null },
            title = { Text("Poslat balík: ${peer.name}") },
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
    if (shareGeneric) ShareDeckDialog(myDecks, null, vm) { shareGeneric = false }
    if (editProfile) ProfileDialog(me, onDismiss = { editProfile = false }) { n, c, a -> vm.saveProfile(n, c, a); editProfile = false }
    if (showQr) MyQrDialog(onDismiss = { showQr = false }, onScan = { showQr = false; scanQr() })
    if (addFriend) GoldDialog(
        onDismissRequest = { addFriend = false },
        title = { Text("Přidat přítele") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AddOption(Icons.Filled.QrCodeScanner, "Naskenovat QR kód", "Kamarád ti ukáže svůj kód v Přátelích") { addFriend = false; scanQr() }
                AddOption(Icons.Filled.QrCode2, "Ukázat můj QR kód", "Kamarád si tě naskenuje") { addFriend = false; showQr = true }
                AddOption(Icons.Filled.Wifi, "V okolí na Wi-Fi", "Zapni viditelnost a ťukni na Přidat") { addFriend = false; LanShare.enable(ctx, true) }
                AddOption(Icons.Filled.Send, "Poslat pozvánku zprávou", "WhatsApp, SMS, e-mail…") {
                    addFriend = false
                    val i = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, FriendCode.inviteText())
                    ctx.startActivity(Intent.createChooser(i, "Pozvat přítele").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
        },
        confirmButton = { TextButton(onClick = { addFriend = false }) { Text("Zavřít") } },
    )
    if (showImport) {
        val parsed = remember(importText) { DeckShareCodec.decode(importText) }
        var friendName by remember(parsed) { mutableStateOf(parsed?.owner ?: "") }
        GoldDialog(
            onDismissRequest = { showImport = false },
            title = { Text("Přijmout balík") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (parsed == null) {
                        Text("Vlož zprávu s balíkem (obsahuje kód AMDECK1:…). Zkopíruj ji v messengeru a ťukni na Vložit ze schránky.", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(importText, { importText = it }, label = { Text("Text / kód") }, modifier = Modifier.fillMaxWidth(), maxLines = 4)
                    } else {
                        Text("„${parsed.name}“ · ${parsed.total} karet", style = MaterialTheme.typography.titleSmall)
                        val known = parsed.uid?.let { u -> friends.firstOrNull { it.uid == u } }
                        if (known != null) Row(verticalAlignment = Alignment.CenterVertically) {
                            FriendAvatar(known.name, known.color, known.avatar, 32.dp)
                            Text("  od ${known.name}", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            Text("Od: ${parsed.owner}", style = MaterialTheme.typography.bodySmall)
                            OutlinedTextField(friendName, { friendName = it }, singleLine = true, label = { Text("Uložit k příteli") })
                            if (friends.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                friends.forEach { f -> com.example.antimarcipan.ui.GoldChip(selected = f.name.equals(friendName, true), onClick = { friendName = f.name }, label = { Text(f.name) }) }
                            }
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
private fun AddOption(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, sub: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
        Column(Modifier.padding(start = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
        }
    }
}

@Composable
private fun MyQrDialog(onDismiss: () -> Unit, onScan: () -> Unit) {
    val code = remember { FriendCode.mine() }
    val bmp = remember(code) { FriendCode.qr(code).asImageBitmap() }
    val me by Me.profile.collectAsStateWithLifecycle()
    GoldDialog(
        onDismissRequest = onDismiss,
        title = { Text("Můj kód přítele") },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FriendAvatar(Me.name, me.color, me.avatar, 36.dp)
                    Text("  ${Me.name}", style = MaterialTheme.typography.titleMedium)
                }
                androidx.compose.foundation.Image(
                    bmp, contentDescription = "QR kód",
                    modifier = Modifier.size(240.dp).clip(RoundedCornerShape(16.dp)).background(Color.White).padding(10.dp),
                )
                Text("Ať si kamarád v Grimoaru → Přátelé → Přidat přítele naskenuje tento kód.", style = MaterialTheme.typography.bodySmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        },
        confirmButton = { TextButton(onClick = onScan) { Text("Naskenovat kamaráda") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Zavřít") } },
    )
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ProfileDialog(me: Me.Profile, onDismiss: () -> Unit, onSave: (String, Int, String?) -> Unit) {
    var name by remember { mutableStateOf(me.name) }
    var color by remember { mutableStateOf(me.color) }
    var avatar by remember { mutableStateOf(me.avatar) }
    GoldDialog(
        onDismissRequest = onDismiss,
        title = { Text("Můj profil") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { FriendAvatar(name.ifBlank { "?" }, color, avatar, 72.dp) }
                OutlinedTextField(name, { name = it.take(24) }, singleLine = true, label = { Text("Jméno, jak tě uvidí kamarádi") }, modifier = Modifier.fillMaxWidth())
                Text("Barva", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FRIEND_COLORS.forEachIndexed { i, c ->
                        Box(
                            Modifier.size(34.dp).clip(CircleShape).background(c)
                                .border(if (i == color) 3.dp else 0.dp, Color.White, CircleShape)
                                .clickable { color = i },
                        )
                    }
                }
                Text("Avatar", style = MaterialTheme.typography.labelLarge)
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    (listOf<String?>(null) + FRIEND_AVATARS).forEach { a ->
                        Box(
                            Modifier.size(40.dp).clip(CircleShape)
                                .background(if (a == avatar) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.06f))
                                .clickable { avatar = a },
                            contentAlignment = Alignment.Center,
                        ) { Text(a ?: name.take(1).uppercase().ifBlank { "A" }, fontSize = androidx.compose.ui.unit.TextUnit(18f, androidx.compose.ui.unit.TextUnitType.Sp)) }
                    }
                }
            }
        },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onSave(name, color, avatar) }) { Text("Uložit") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Zrušit") } },
    )
}

@Composable
private fun ShareDeckDialog(myDecks: List<com.example.antimarcipan.data.local.Deck>, to: String?, vm: FriendsViewModel, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    GoldDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (to != null) "Poslat balík: $to" else "Poslat balík") },
        text = {
            Column {
                Text("Pošle se zprávou (WhatsApp, Quick Share, e-mail…). Kamarád ji otevře v Grimoaru.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
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
                                onDismiss()
                            }
                        }.padding(12.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    HorizontalDivider()
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Zavřít") } },
    )
}

@Composable
private fun FriendDetail(
    f: Friend, fDecks: List<FriendDeck>, peer: LanPeer?, vm: FriendsViewModel,
    onBack: () -> Unit, onOpenDeck: (FriendDeck) -> Unit, onShare: () -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val myDecks by vm.myDecks.collectAsStateWithLifecycle()
    var confirmDel by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf(false) }
    var lanSend by remember { mutableStateOf(false) }
    androidx.activity.compose.BackHandler(onBack = onBack)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zpět") }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { rename = true }) { Icon(Icons.Filled.Edit, "Přejmenovat") }
                IconButton(onClick = { confirmDel = true }) { Icon(Icons.Filled.Delete, "Odebrat přítele") }
            }
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                FriendAvatar(f.name, f.color, f.avatar, 84.dp, online = peer != null)
                Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(f.name, style = MaterialTheme.typography.headlineSmall)
                    if (f.verified) Icon(Icons.Filled.Verified, "Ověřený", Modifier.padding(start = 6.dp).size(20.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Text(
                    when {
                        peer != null -> "V okolí na stejné Wi-Fi"
                        f.lastSeen > 0 -> "Naposledy ${lastSeenLabel(f.lastSeen)}"
                        else -> "Přítel od ${java.text.SimpleDateFormat("d. M. yyyy", java.util.Locale("cs")).format(java.util.Date(f.addedAt))}"
                    },
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                )
                if (!f.verified) Text(
                    "Nespárováno — naskenujte si QR kódy nebo se přidejte v okolí, ať se balíky a hlasy přiřadí spolehlivě.",
                    Modifier.padding(top = 6.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { if (peer != null) lanSend = true else onShare() }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Share, null, Modifier.size(18.dp)); Text(if (peer != null) "  Poslat přes Wi-Fi" else "  Poslat balík")
                }
            }
            Text("BALÍKY OD ${f.name.uppercase()}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            if (fDecks.isEmpty()) Text("Zatím ti neposlal/a žádný balík.", Modifier.padding(vertical = 8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
        }
        items(fDecks, key = { it.id }) { d ->
            val sd = remember(d.id) { DeckShareCodec.parseJson(d.json) }
            Card(
                Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onOpenDeck(d) },
                border = BorderStroke(1.dp, GoldBorderBrush),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)),
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(d.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${com.example.antimarcipan.decks.DeckFormat.from(d.format).label} · ${sd?.total ?: 0} karet · rádce proti balíku",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        )
                    }
                    Text("›", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
    if (lanSend && peer != null) GoldDialog(
        onDismissRequest = { lanSend = false },
        title = { Text("Poslat ${f.name}") },
        text = {
            Column {
                myDecks.forEach { d ->
                    Text(
                        d.name,
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable {
                            scope.launch {
                                val res = LanShare.send(peer, vm.shareText(d.id))
                                Toast.makeText(ctx, if (res.isSuccess) "Odesláno — čeká na potvrzení" else "Nepodařilo se odeslat", Toast.LENGTH_SHORT).show()
                                lanSend = false
                            }
                        }.padding(12.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    HorizontalDivider()
                }
            }
        },
        confirmButton = { TextButton(onClick = { lanSend = false }) { Text("Zavřít") } },
    )
    if (rename) {
        var n by remember { mutableStateOf(f.name) }
        GoldDialog(
            onDismissRequest = { rename = false },
            title = { Text("Přejmenovat přítele") },
            text = { OutlinedTextField(n, { n = it }, singleLine = true, label = { Text("Jméno") }) },
            confirmButton = { TextButton(onClick = { vm.renameFriend(f, n); rename = false }) { Text("Uložit") } },
            dismissButton = { TextButton(onClick = { rename = false }) { Text("Zrušit") } },
        )
    }
    if (confirmDel) GoldDialog(
        onDismissRequest = { confirmDel = false },
        title = { Text("Odebrat přítele?") },
        text = { Text("Odebrat ${f.name} i všechny jeho uložené balíky?") },
        confirmButton = { TextButton(onClick = { vm.deleteFriend(f.id); confirmDel = false; onBack() }) { Text("Odebrat") } },
        dismissButton = { TextButton(onClick = { confirmDel = false }) { Text("Zrušit") } },
    )
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
