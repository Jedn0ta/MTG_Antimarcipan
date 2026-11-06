package com.example.antimarcipan.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private data class GuideStep(
    val icon: ImageVector,
    val title: String,
    val body: String,
)

private val GUIDE_STEPS = listOf(
    GuideStep(
        Icons.Filled.CameraAlt,
        "Skenování karet",
        "Namiř foťák na jméno karty a appka ji najde ve Scryfallu i se správnou edicí. " +
            "V režimu Multisken projedeš víc karet po sobě a potvrdíš je naráz. " +
            "Ikonou překladu zobrazíš textaci v jazyce zvoleném v Nastavení.",
    ),
    GuideStep(
        Icons.Filled.ViewModule,
        "Sbírka",
        "Všechny naskenované i ručně přidané karty. Přepínáš seznam/mřížku, filtruješ a hledáš. " +
            "Ťuknutím zobrazíš detail, podržením celou kartu, swipem vpravo přidáš do balíku. " +
            "Dlouhé podržení textu kartu smaže.",
    ),
    GuideStep(
        Icons.Filled.Style,
        "Balíky",
        "Postav si balík — Commander i „jednokarták“. Země jsou zvlášť, u základních měníš počet " +
            "přes + a −. Barvy balíku se počítají ze zemí. Filtr typů nabízí i tribaly (Elf, Knight…). " +
            "Decklist jde exportovat.",
    ),
    GuideStep(
        Icons.Filled.Hub,
        "Synergie",
        "Vyhodnotí, jak se karty hodí do daného balíku, označí ty, co nesedí, a navrhne výměny " +
            "i další karty — podle velitele (EDHREC) nebo podle barev a témat balíku.",
    ),
    GuideStep(
        Icons.Filled.AutoAwesome,
        "Anti Marcipán",
        "Rádce při hře. Pomůže s rozhodnutím nad balíkem a pravidly.",
    ),
    GuideStep(
        Icons.Filled.Settings,
        "Nastavení",
        "Jazyk překladu karet, aktualizace aplikace přímo z GitHubu a záloha/obnova dat. " +
            "Tohoto průvodce si tu můžeš kdykoli pustit znovu.",
    ),
)

/**
 * Jednoduchý průvodce aplikací — krokovaný přehled funkcí. Zobrazí se při
 * prvním spuštění a dá se kdykoli vyvolat z horní lišty nebo z Nastavení.
 */
@Composable
fun GuideOverlay(onClose: () -> Unit) {
    var index by remember { mutableIntStateOf(0) }
    val step = GUIDE_STEPS[index]
    val last = index == GUIDE_STEPS.lastIndex

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = true),
    ) {
        androidx.compose.material3.Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
        ) {
            Column(
                Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.size(72.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        step.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(36.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    step.title,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    step.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))

                // Tečky kroků
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GUIDE_STEPS.indices.forEach { i ->
                        Box(
                            Modifier.size(if (i == index) 9.dp else 7.dp).clip(CircleShape)
                                .background(
                                    if (i == index) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                                )
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onClose) { Text("Přeskočit") }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (index > 0) {
                            TextButton(onClick = { index-- }) { Text("Zpět") }
                        }
                        Button(onClick = { if (last) onClose() else index++ }) {
                            Text(if (last) "Hotovo" else "Dál")
                        }
                    }
                }
            }
        }
    }
}
