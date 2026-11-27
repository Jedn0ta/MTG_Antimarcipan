package com.example.antimarcipan.support

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.antimarcipan.i18n.tr

/**
 * Grimoar Plus: jednorázové odemknutí počítadla životů a zakládání anket.
 * Verze z GitHubu nemá nákupy → vše odemčené.
 */
object Plus {
    val unlocked: Boolean get() = !SupportStore.AVAILABLE || Supporter.isSupporter
}

@Composable
fun rememberPlusUnlocked(): Boolean {
    val active by Supporter.active.collectAsStateWithLifecycle()
    return !SupportStore.AVAILABLE || active
}

/** Obsah nabídky odemknutí (celá obrazovka nebo dialog). */
@Composable
fun PlusOffer(feature: String, modifier: Modifier = Modifier, onClose: (() -> Unit)? = null) {
    val price by SupportStore.price.collectAsStateWithLifecycle()
    val msg by SupportStore.message.collectAsStateWithLifecycle()
    val activity = LocalContext.current as? android.app.Activity
    Column(
        modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("✦", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
        Text("Grimoar Plus", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(feature, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            tr("Jednorázové odemknutí zakládání anket na srazy. Všechno ostatní — sken, sbírka, balíky, Rádce, počítadlo životů i hlasování v anketách — zůstává zdarma. Jako poděkování dostaneš i zlatou hvězdičku ✦."),
            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
        )
        Button(onClick = { activity?.let { SupportStore.buy(it) } }, modifier = Modifier.fillMaxWidth()) {
            Text(tr("Odemknout") + (price?.let { " · $it" } ?: ""))
        }
        TextButton(onClick = { SupportStore.restore() }) { Text(tr("Obnovit nákup")) }
        msg?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center) }
        onClose?.let { TextButton(onClick = it) { Text(tr("Zavřít")) } }
    }
}

@Composable
fun PlusScreen(feature: String) = PlusOffer(feature, Modifier.fillMaxSize())
