package com.example.antimarcipan.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.antimarcipan.data.local.Deck

/**
 * Jednoduchý výběr balíku. Když žádný balík není, nabídne to uživateli vědět.
 */
@Composable
fun DeckPickerDialog(
    decks: List<Deck>,
    onDismiss: () -> Unit,
    onPick: (Deck) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Přidat do balíku") },
        text = {
            if (decks.isEmpty()) {
                Text("Zatím nemáš žádný balík. Založ ho v záložce Balíky.")
            } else {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    decks.forEach { deck ->
                        Text(
                            deck.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(deck) }
                                .padding(vertical = 14.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Zavřít") } },
    )
}
