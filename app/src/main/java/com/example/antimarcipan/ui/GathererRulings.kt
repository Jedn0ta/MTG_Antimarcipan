package com.example.antimarcipan.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.antimarcipan.MtgApp

/** Odkaz na kartu na Gathereru podle jména. */
fun gathererUrlFor(name: String): String =
    "https://gatherer.wizards.com/Pages/Search/Default.aspx?name=" +
        java.net.URLEncoder.encode("[\"${name.substringBefore(" //").trim()}\"]", "UTF-8")

/**
 * Ikona knížky: ťuk = rulings (výklady pravidel) v okně, podržení 3 s = otevře Gatherer.
 * Funguje kdekoli — ve sbírce, v balíku i u naskenované karty.
 */
@Composable
fun GathererRulingsButton(scryfallId: String, name: String, gathererUrl: String? = null, iconSize: Dp = 24.dp) {
    val ctx = LocalContext.current
    var show by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    val url = gathererUrl?.takeIf { it.isNotBlank() } ?: gathererUrlFor(name)
    fun openGatherer() {
        runCatching {
            ctx.startActivity(
                android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
    Box(contentAlignment = Alignment.Center) {
        Holdable(holdMs = 3000, onTap = { show = true }, onHold = { openGatherer() }, onProgress = { progress = it }) {
            Icon(
                Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = "Rulings (ťuk) / Gatherer (podržení)",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(10.dp).size(iconSize),
            )
        }
        if (progress > 0.18f && progress < 1f) CircularProgressIndicator(
            progress = { progress }, strokeWidth = 3.dp, modifier = Modifier.size(iconSize + 20.dp),
        )
    }
    if (show) {
        var rulings by remember { mutableStateOf<List<String>?>(null) }
        LaunchedEffect(scryfallId) {
            rulings = (ctx.applicationContext as MtgApp).container.collectionRepository
                .rulings(scryfallId).getOrElse { emptyList() }
        }
        GoldDialog(
            onDismissRequest = { show = false },
            title = { Text("Rulings — $name") },
            text = {
                Box(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                    val r = rulings
                    when {
                        r == null -> Text("Načítám…")
                        r.isEmpty() -> Text("Žádné rulings k této kartě.")
                        else -> SymbolText(r.joinToString("\n\n"), MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            dismissButton = { TextButton(onClick = { openGatherer() }) { Text("Otevřít Gatherer") } },
            confirmButton = { TextButton(onClick = { show = false }) { Text("Zavřít") } },
        )
    }
}
