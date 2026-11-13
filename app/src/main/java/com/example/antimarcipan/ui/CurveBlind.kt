package com.example.antimarcipan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.antimarcipan.data.local.DeckCardWithQty

/** Typ karty pro barevné rozlišení v grafu. */
private enum class CType(val label: String, val color: Color) {
    CREATURE("Tvor", Color(0xFFE2BC62)),
    INSTANT("Instant", Color(0xFF5FA8E8)),
    SORCERY("Sorcery", Color(0xFFE0705A)),
    ENCHANT("Enchant.", Color(0xFFB98AE8)),
    ARTIFACT("Artefakt", Color(0xFFA9AEB8)),
    PW("Planesw.", Color(0xFF6FD2B0)),
    OTHER("Jiné", Color(0xFF7A6FA0)),
}

private fun typeOf(t: String?): CType {
    val s = t ?: ""
    return when {
        s.contains("Creature", true) -> CType.CREATURE
        s.contains("Planeswalker", true) -> CType.PW
        s.contains("Instant", true) -> CType.INSTANT
        s.contains("Sorcery", true) -> CType.SORCERY
        s.contains("Enchantment", true) -> CType.ENCHANT
        s.contains("Artifact", true) -> CType.ARTIFACT
        else -> CType.OTHER
    }
}

/**
 * „Informační roleta“ s mana curve: tažením za úchyt ji stáhneš libovolně dolů
 * (až do ~poloviny obrazovky), ťuknutím na úchyt ji sbalíš/rozbalíš.
 * Při malé výšce jen sloupce, při větší barevné rozlišení typů a legenda.
 */
@Composable
fun CurveBlind(cards: List<DeckCardWithQty>, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val maxH = (LocalConfiguration.current.screenHeightDp * 0.55f)
    var h by rememberSaveable { mutableFloatStateOf(0f) } // dp, 0 = sbaleno
    val spells = cards.filter { it.card.typeLine?.contains("Land", true) != true }
    val total = spells.sumOf { it.deckQuantity }
    val avg = if (total == 0) 0.0 else spells.sumOf { (it.card.cmc ?: 0.0) * it.deckQuantity } / total
    // [mv][typ] = počet
    val grid = Array(8) { IntArray(CType.entries.size) }
    spells.forEach { e -> grid[minOf(7, (e.card.cmc ?: 0.0).toInt())][typeOf(e.card.typeLine).ordinal] += e.deckQuantity }
    val maxCol = (0..7).maxOf { grid[it].sum() }.coerceAtLeast(1)
    val usedTypes = CType.entries.filter { t -> (0..7).any { grid[it][t.ordinal] > 0 } }

    Column(modifier.fillMaxWidth()) {
        if (h > 0f) {
            BoxWithConstraints(
                Modifier.fillMaxWidth().height(h.dp)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                val detailed = h >= 110f
                Column(Modifier.fillMaxWidth()) {
                    if (detailed) Text(
                        "Mana curve · kouzel $total · Ø MV %.2f".format(avg),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Row(
                        Modifier.fillMaxWidth().weight(1f),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        (0..7).forEach { mv ->
                            val col = grid[mv]
                            val sum = col.sum()
                            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                                if (sum > 0) Text("$sum", fontSize = 10.sp, color = Color.White)
                                if (sum > 0) Column(
                                    Modifier.fillMaxWidth().fillMaxHeight(0.82f * sum / maxCol)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)),
                                ) {
                                    // shora méně časté typy, dole tvorové
                                    CType.entries.reversed().forEach { t ->
                                        val n = if (detailed) col[t.ordinal] else 0
                                        if (n > 0) Box(Modifier.fillMaxWidth().weight(n.toFloat()).background(t.color))
                                    }
                                    if (!detailed) Box(Modifier.fillMaxWidth().weight(1f).background(CType.CREATURE.color))
                                }
                            }
                        }
                        if (!detailed) Text("Ø %.1f".format(avg), fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        (0..7).forEach {
                            Text(if (it == 7) "7+" else "$it", Modifier.weight(1f), fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.6f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                        if (!detailed) Box(Modifier.width(26.dp))
                    }
                    if (detailed && usedTypes.isNotEmpty()) Row(
                        Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        usedTypes.forEach { t ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(t.color))
                                Text(
                                    " ${t.label} ${(0..7).sumOf { grid[it][t.ordinal] }}",
                                    fontSize = 9.sp, color = Color.White.copy(alpha = 0.8f), maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
        // Úchyt rolety: táhni dolů/nahoru, ťukni pro sbalení/rozbalení
        Box(
            Modifier.fillMaxWidth().height(22.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = { if (h < 45f) h = 0f },
                        onVerticalDrag = { _, dy -> h = (h + dy / density.density).coerceIn(0f, maxH) },
                    )
                }
                .clickable { h = if (h > 0f) 0f else 90f },
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(36.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)))
                if (h == 0f) Text(
                    "  mana curve ▾",
                    fontSize = 10.sp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                )
            }
        }
    }
}
