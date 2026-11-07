package com.example.antimarcipan.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/** Jednoduchý potvrzovací dialog, používaný hlavně před mazáním. */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "Potvrdit",
    dismissText: String = "Zrušit",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    com.example.antimarcipan.ui.GoldDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmText) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismissText) } },
    )
}
