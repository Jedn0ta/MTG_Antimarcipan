package com.example.antimarcipan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.antimarcipan.data.local.DeckCardWithQty

/** Spočítá mana curve: index 0..7 (7 = 7+), pár (tvorové, ostatní kouzla). */
fun computeCurve(cards: List<DeckCardWithQty>): Triple<List<Pair<Int, Int>>, Double, Int> {
    val spells = cards.filter { it.card.typeLine?.contains("Land", true) != true }
    val curve = (0..7).map { b ->
        val inB = spells.filter { minOf(7, (it.card.cmc ?: 0.0).toInt()) == b }
        inB.filter { it.card.typeLine?.contains("Creature", true) == true }.sumOf { it.deckQuantity } to
            inB.filter { it.card.typeLine?.contains("Creature", true) != true }.sumOf { it.deckQuantity }
    }
    val total = spells.sumOf { it.deckQuantity }
    val avg = if (total == 0) 0.0 else spells.sumOf { (it.card.cmc ?: 0.0) * it.deckQuantity } / total
    val lands = cards.filter { it.card.typeLine?.contains("Land", true) == true }.sumOf { it.deckQuantity }
    return Triple(curve, avg, lands)
}

/** Mana curve balíku: sloupce podle mana value, tvorové vs. ostatní kouzla. */
@Composable
fun ManaCurveCard(curve: List<Pair<Int, Int>>, avgMv: Double, lands: Int, compact: Boolean = false) {
    val maxV = curve.maxOf { it.first + it.second }.coerceAtLeast(1)
    val creature = androidx.compose.ui.graphics.Color(0xFFE2BC62)
    val other = androidx.compose.ui.graphics.Color(0xFF8C73D9)
    androidx.compose.material3.Card(
        Modifier.fillMaxWidth().padding(horizontal = if (compact) 8.dp else 12.dp, vertical = 4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, com.example.antimarcipan.ui.GoldBorderBrush),
    ) {
        Column(Modifier.padding(if (compact) 10.dp else 14.dp)) {
            Text("Mana curve", style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(
                "Průměrná MV %.2f · kouzel %d · zemí %d".format(
                    avgMv, curve.sumOf { it.first + it.second }, lands,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            )
            androidx.compose.foundation.layout.Row(
                Modifier.fillMaxWidth().height(if (compact) 90.dp else 150.dp).padding(top = if (compact) 6.dp else 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                curve.forEachIndexed { i, (cr, ot) ->
                    val total = cr + ot
                    Column(
                        Modifier.weight(1f).fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        if (total > 0) Text("$total", style = MaterialTheme.typography.labelSmall)
                        val frac = total.toFloat() / maxV
                        if (total > 0) Column(
                            Modifier.fillMaxWidth().fillMaxHeight(frac * 0.82f)
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)),
                        ) {
                            if (ot > 0) androidx.compose.foundation.layout.Box(
                                Modifier.fillMaxWidth().weight(ot.toFloat()).background(other)
                            )
                            if (cr > 0) androidx.compose.foundation.layout.Box(
                                Modifier.fillMaxWidth().weight(cr.toFloat()).background(creature)
                            )
                        }
                    }
                }
            }
            androidx.compose.foundation.layout.Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                (0..7).forEach { i ->
                    Text(
                        if (i == 7) "7+" else "$i",
                        Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            androidx.compose.foundation.layout.Row(
                Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("■ Tvorové", color = creature, style = MaterialTheme.typography.labelMedium)
                Text("■ Ostatní kouzla", color = other, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}


/** Úsporná mana curve: nízké sloupce s počtem, popisky MV pod nimi, vpravo průměr. */
@Composable
fun MiniManaCurve(curve: List<Pair<Int, Int>>, avgMv: Double) {
    val maxV = curve.maxOf { it.first + it.second }.coerceAtLeast(1)
    val creature = androidx.compose.ui.graphics.Color(0xFFE2BC62)
    val other = androidx.compose.ui.graphics.Color(0xFF8C73D9)
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        curve.forEachIndexed { i, (cr, ot) ->
            val total = cr + ot
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (total > 0) "$total" else "", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                Column(
                    Modifier.fillMaxWidth().height((34f * total / maxV).dp.coerceAtLeast(if (total > 0) 3.dp else 0.dp))
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)),
                ) {
                    if (ot > 0) androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().weight(ot.toFloat()).background(other))
                    if (cr > 0) androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().weight(cr.toFloat()).background(creature))
                }
                Text(
                    if (i == 7) "7+" else "$i",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
        Text(
            "Ø %.1f".format(avgMv),
            Modifier.padding(start = 6.dp, bottom = 14.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
