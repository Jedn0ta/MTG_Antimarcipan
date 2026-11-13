package com.example.antimarcipan.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextStyle
import com.example.antimarcipan.data.KeywordGlossary

/**
 * Textace karty se symboly many a vysvětlivkami vlastností (anglicky).
 * Pokud appka nějakou vysvětlivku doplnila, nabídne její překlad do češtiny.
 */
@Composable
fun OracleText(oracle: String?, style: TextStyle) {
    if (oracle.isNullOrBlank()) return
    var czech by remember(oracle) { mutableStateOf(false) }
    val annotated = KeywordGlossary.annotate(oracle, czech) ?: oracle
    Column {
        SymbolText(annotated, style)
        if (annotated.contains('')) {
            TextButton(onClick = { czech = !czech }) {
                Text(
                    if (czech) "Vysvětlivky anglicky" else "Přeložit vysvětlivky do češtiny",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}
