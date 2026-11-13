package com.example.antimarcipan.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import com.example.antimarcipan.MtgApp
import kotlinx.coroutines.launch

private fun repo(ctx: android.content.Context) =
    (ctx.applicationContext as MtgApp).container.collectionRepository

/** Obrázek karty podle jména (Scryfall). */
fun scryfallImageByName(name: String, version: String = "normal"): String =
    "https://api.scryfall.com/cards/named?exact=" +
        java.net.URLEncoder.encode(name.substringBefore(" //").trim(), "UTF-8") + "&format=image&version=$version"

/** Tlačítko „Na wishlist“ — otevře dialog s poznámkou (předvyplněnou např. kombem). */
@Composable
fun WishlistButton(name: String, defaultNote: String, modifier: Modifier = Modifier, compact: Boolean = false) {
    var open by remember { mutableStateOf(false) }
    if (compact) TextButton(onClick = { open = true }, modifier = modifier) { Text("+ Wishlist", style = MaterialTheme.typography.labelMedium) }
    else OutlinedButton(onClick = { open = true }, modifier = modifier) { Text("Na wishlist") }
    if (open) WishlistNoteDialog(name, defaultNote) { open = false }
}

@Composable
fun WishlistNoteDialog(name: String, defaultNote: String, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var note by remember { mutableStateOf(defaultNote) }
    var busy by remember { mutableStateOf(false) }
    GoldDialog(
        onDismissRequest = onDismiss,
        title = { Text("Na wishlist: $name") },
        text = {
            Column {
                Text("Poznámka (proč ji chceš, ke kterému kombu…)", style = MaterialTheme.typography.labelMedium)
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp), minLines = 2,
                )
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                busy = true
                scope.launch {
                    repo(ctx).addToWishlistByName(name, note)
                        .onSuccess { Toast.makeText(ctx, "Na wishlistu: $it", Toast.LENGTH_SHORT).show() }
                        .onFailure { Toast.makeText(ctx, "Kartu se nepodařilo najít (internet?)", Toast.LENGTH_SHORT).show() }
                    onDismiss()
                }
            }) { Text(if (busy) "Ukládám…" else "Přidat") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Zrušit") } },
    )
}

/** Úprava poznámky karty (ve sbírce/wishlistu). */
@Composable
fun EditNoteDialog(scryfallId: String, current: String?, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var note by remember { mutableStateOf(current ?: "") }
    GoldDialog(
        onDismissRequest = onDismiss,
        title = { Text("Poznámka") },
        text = {
            OutlinedTextField(value = note, onValueChange = { note = it }, modifier = Modifier.fillMaxWidth(), minLines = 3)
        },
        confirmButton = {
            TextButton(onClick = { scope.launch { repo(ctx).setNote(scryfallId, note); onDismiss() } }) { Text("Uložit") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Zrušit") } },
    )
}
