package com.example.antimarcipan.decks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.antimarcipan.data.local.CollectionCard
import com.example.antimarcipan.ui.GoldBorderBrush
import com.example.antimarcipan.ui.InteractiveThumb

/**
 * Rádce pro limited formáty (Draft, Sealed): hodnocení karet poolu,
 * doporučení barev, kontrola struktury balíku 40 karet a rozpis zemí.
 */
object LimitedAdvisor {
    private val BASIC = setOf("plains", "island", "swamp", "mountain", "forest", "wastes")
    val WUBRG = listOf("W", "U", "B", "R", "G")
    val BASIC_FOR = mapOf("W" to "Plains", "U" to "Island", "B" to "Swamp", "R" to "Mountain", "G" to "Forest")

    fun isBasic(c: CollectionCard): Boolean =
        c.name.lowercase().removePrefix("snow-covered ").trim() in BASIC

    fun isLand(c: CollectionCard) = c.typeLine?.contains("Land", true) == true
    fun isCreature(c: CollectionCard) = c.typeLine?.contains("Creature", true) == true

    private val removalRx = Regex(
        "destroy target|exile target|deals? (\\d+|x) damage to (any target|target creature|target attacking|target .*creature)|" +
            "target creature (gets|gains) -|fights? (another )?target|target creature can't block|tap target creature.*doesn't untap|" +
            "return target (nonland )?(creature|permanent) to its owner's hand|put target creature .* on (the )?(top|bottom)",
        RegexOption.IGNORE_CASE,
    )

    fun isRemoval(c: CollectionCard) = c.oracleText?.let { removalRx.containsMatchIn(it) } == true

    fun colors(c: CollectionCard): List<String> =
        c.colorIdentity.split(",").map { it.trim() }.filter { it in WUBRG }

    /** Hrubé hodnocení síly karty v limited (0–10). Pořadí: bomby > removal > evasivní tvorové > zbytek. */
    fun rating(c: CollectionCard): Double {
        if (isBasic(c)) return 0.0
        var r = when (c.rarity?.lowercase()) {
            "mythic" -> 4.0; "rare" -> 3.2; "uncommon" -> 2.0; else -> 1.4
        }
        val text = (c.oracleText ?: "")
        val kw = c.keywords.lowercase()
        if (isRemoval(c)) r += 2.6
        if (isCreature(c)) {
            r += 1.0
            if ("flying" in kw || "menace" in kw || "trample" in kw || "can't be blocked" in text.lowercase()) r += 0.8
            if ("deathtouch" in kw || "lifelink" in kw || "first strike" in kw) r += 0.4
        }
        if (Regex("draw (a|two|three) cards?", RegexOption.IGNORE_CASE).containsMatchIn(text)) r += 0.6
        if (text.contains("counter target", true)) r += 0.3
        val cmc = c.cmc ?: 0.0
        if (cmc >= 7) r -= 1.2 else if (cmc >= 6) r -= 0.5
        if (colors(c).size >= 3) r -= 0.8
        if (isLand(c)) r = if (c.producedMana.split(",").count { it.isNotBlank() } >= 2) 1.5 else 0.5
        return r.coerceIn(0.0, 10.0)
    }

    /** Skóre barev poolu (součet hodnocení hratelných karet). */
    fun colorScores(pool: List<CollectionCard>): Map<String, Double> {
        val scores = WUBRG.associateWith { 0.0 }.toMutableMap()
        pool.filter { !isLand(it) }.forEach { c ->
            val cols = colors(c)
            val q = c.quantity.coerceAtLeast(1)
            cols.forEach { col -> scores[col] = scores[col]!! + rating(c) * q / cols.size }
        }
        return scores
    }

    fun bestPair(scores: Map<String, Double>): List<String> =
        scores.entries.sortedByDescending { it.value }.take(2).map { it.key }

    /** Doporučený počet zemí pro 40 karet podle průměrné MV. */
    fun landTarget(spells: List<CollectionCard>): Int {
        val avg = spells.mapNotNull { it.cmc }.average().takeIf { !it.isNaN() } ?: 3.0
        return when {
            avg < 2.5 -> 16
            avg > 3.6 -> 18
            else -> 17
        }
    }

    /** Rozdělení základních zemí podle počtu symbolů many na kouzlech. */
    fun basicSplit(spells: List<CollectionCard>, basicsNeeded: Int): Map<String, Int> {
        val pips = WUBRG.associateWith { col ->
            spells.sumOf { s -> Regex("\\{([^}]*)\\}").findAll(s.manaCost ?: "").count { col in it.groupValues[1] } }
        }.filterValues { it > 0 }
        val total = pips.values.sum()
        if (total == 0 || basicsNeeded <= 0) return emptyMap()
        val raw = pips.mapValues { it.value.toDouble() * basicsNeeded / total }
        val res = raw.mapValues { kotlin.math.floor(it.value).toInt() }.toMutableMap()
        var left = basicsNeeded - res.values.sum()
        raw.entries.sortedByDescending { it.value - kotlin.math.floor(it.value) }.forEach {
            if (left > 0) { res[it.key] = res[it.key]!! + 1; left-- }
        }
        return res.mapKeys { BASIC_FOR[it.key]!! }
    }
}

@Composable
fun LimitedAdvisorPanel(
    format: DeckFormat,
    deckCards: List<CollectionCard>,
    pool: List<CollectionCard>,
    poolImported: Boolean,
    onScanPool: () -> Unit,
    onImportPool: () -> Unit,
    onAddCard: (CollectionCard) -> Unit,
    onApplyLands: (Map<String, Int>) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    var showGuide by rememberSaveable { mutableStateOf(false) }
    var confirmImport by remember { mutableStateOf(false) }
    val spells = deckCards.filter { !LimitedAdvisor.isLand(it) }
    val nonBasicLands = deckCards.count { LimitedAdvisor.isLand(it) && !LimitedAdvisor.isBasic(it) }
    val basics = deckCards.count { LimitedAdvisor.isBasic(it) }
    val creatures = spells.count { LimitedAdvisor.isCreature(it) }
    val removal = spells.count { LimitedAdvisor.isRemoval(it) }
    val twoDrops = spells.count { (it.cmc ?: 0.0) <= 2.0 && LimitedAdvisor.isCreature(it) }
    val top = spells.count { (it.cmc ?: 0.0) >= 6.0 }
    val landTarget = LimitedAdvisor.landTarget(spells)
    val scores = LimitedAdvisor.colorScores(pool.ifEmpty { deckCards })
    val pair = LimitedAdvisor.bestPair(scores)
    val deckCols = spells.flatMap { LimitedAdvisor.colors(it) }.toSet()

    Card(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, GoldBorderBrush),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Rádce · ${format.label}",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(if (expanded) "▲" else "▼", color = MaterialTheme.colorScheme.primary)
            }
            if (!expanded) return@Column

            // Pool
            Text(
                "Pool: ${pool.sumOf { it.quantity }} karet" + if (poolImported) " · přidáno do sbírky ✓" else "",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = onScanPool) { Text("Skenovat pool") }
                OutlinedButton(onClick = { confirmImport = true }, enabled = pool.isNotEmpty() && !poolImported) {
                    Text("Pool do sbírky")
                }
            }
            TextButton(onClick = { showGuide = true }) { Text("Jak se ${format.label} hraje") }

            // Kontrola struktury
            Check("Karet v balíku", "${deckCards.size} / 40", deckCards.size >= 40)
            Check("Země", "${nonBasicLands + basics} (doporučeno $landTarget)", nonBasicLands + basics in (landTarget - 1)..(landTarget + 1))
            Check("Tvorové", "$creatures (ideál 15–17)", creatures in 14..18)
            Check("Removal", "$removal (čím víc, tím líp, aspoň 3)", removal >= 3)
            Check("Levní tvorové (MV ≤ 2)", "$twoDrops (aspoň 4–5)", twoDrops >= 4)
            Check("Drahá kouzla (MV ≥ 6)", "$top (max 2–3)", top <= 3)
            Check("Barvy", if (deckCols.isEmpty()) "—" else deckCols.joinToString(""), deckCols.size in 1..2)

            HorizontalDivider(Modifier.padding(vertical = 2.dp))
            Text(
                "Nejsilnější barvy ${if (pool.isEmpty()) "balíku" else "poolu"}: " +
                    scores.entries.sortedByDescending { it.value }
                        .joinToString("  ") { "${it.key} ${"%.0f".format(it.value)}" },
                style = MaterialTheme.typography.bodySmall,
            )
            if (pair.isNotEmpty() && scores.values.any { it > 0 }) {
                Text(
                    "Doporučení: hraj ${pair.joinToString(" + ")}" +
                        if (format == DeckFormat.DRAFT) " — ale v draftu sleduj, které barvy ti chodí (jsou „otevřené“)." else ".",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            val basicsNeeded = landTarget - nonBasicLands
            val split = LimitedAdvisor.basicSplit(spells, basicsNeeded)
            if (split.isNotEmpty()) {
                OutlinedButton(onClick = { onApplyLands(split) }) {
                    Text("Nastavit země: " + split.entries.joinToString(", ") { "${it.value}× ${it.key}" })
                }
            }

            // Návrhy z poolu
            if (pool.isNotEmpty()) {
                val inDeck = deckCards.map { it.scryfallId }.toSet()
                val suggestions = pool
                    .filter { it.scryfallId !in inDeck && !LimitedAdvisor.isLand(it) }
                    .filter { c -> LimitedAdvisor.colors(c).all { it in pair } }
                    .sortedByDescending { LimitedAdvisor.rating(it) }
                    .take(8)
                if (suggestions.isNotEmpty()) {
                    Text("Nejlepší karty z poolu v barvách ${pair.joinToString("")}:", style = MaterialTheme.typography.labelLarge)
                    suggestions.forEach { c ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            InteractiveThumb(c, width = 36.dp, height = 50.dp)
                            Column(Modifier.weight(1f)) {
                                Text(c.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    buildString {
                                        append("hodnocení %.1f".format(LimitedAdvisor.rating(c)))
                                        if (LimitedAdvisor.isRemoval(c)) append(" · removal")
                                        c.cmc?.let { append(" · MV ${it.toInt()}") }
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                )
                            }
                            TextButton(onClick = { onAddCard(c) }) { Text("+ Přidat") }
                        }
                    }
                }
            }
        }
    }

    if (showGuide) LimitedGuideDialog(format) { showGuide = false }
    if (confirmImport) {
        com.example.antimarcipan.ui.GoldDialog(
            onDismissRequest = { confirmImport = false },
            title = { Text("Přidat pool do sbírky?") },
            text = { Text("Všech ${pool.sumOf { it.quantity }} karet z poolu se přidá do sbírky (počty se přičtou).") },
            confirmButton = { TextButton(onClick = { onImportPool(); confirmImport = false }) { Text("Přidat") } },
            dismissButton = { TextButton(onClick = { confirmImport = false }) { Text("Zrušit") } },
        )
    }
}

@Composable
private fun Modifier.androidxScroll(): Modifier =
    this.heightIn(max = 460.dp).verticalScroll(rememberScrollState())

@Composable
private fun Check(label: String, value: String, ok: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (ok) "✓" else "!",
            Modifier.width(18.dp),
            color = if (ok) Color(0xFF2E9E5B) else Color(0xFFD98324),
            style = MaterialTheme.typography.labelLarge,
        )
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
    }
}

/** Text vysvětlující, jak daný limited formát probíhá a jak stavět balík. */
fun limitedGuide(format: DeckFormat): String = when (format) {
    DeckFormat.SEALED -> """
Jak probíhá Sealed:
• Každý hráč otevře 6 boosterů (play boostery) — to je tvůj pool, cca 84 karet.
• Z poolu postavíš balík o min. 40 kartách. Základní země dostaneš zdarma v libovolném počtu.
• Zbytek poolu je sideboard — mezi hrami ho můžeš do balíku vyměňovat.
• Hraje se obvykle na 2 vítězné hry (Bo3), 20 životů.

Postup stavby:
1. Ťukni na „Skenovat pool“ a naskenuj všechny karty z boosterů (jdou do poolu, ne do sbírky).
2. Projdi bomby (silné rare/mythic) a removal — ty určují barvy.
3. Vyber 2 nejsilnější a nejhlubší barvy (rádce je spočítá). Třetí barvu jen „splashni“ pro 1–2 bomby, pokud máš fixing.
4. Cíl: 17 zemí + 23 kouzel, z toho 15–17 tvorů a co nejvíc removalu.
5. Křivka: 4–6 tvorů za 2, 3–5 za 3, pár za 4–5, max 2–3 karty za 6+.
6. Země rozděl podle symbolů many — tlačítko „Nastavit země“.
""".trimIndent()
    DeckFormat.DRAFT -> """
Jak probíhá Draft (booster draft):
• U stolu sedí obvykle 8 hráčů, každý má 3 boostery.
• Otevřeš 1. booster, vybereš si 1 kartu a zbytek pošleš hráči vlevo. Od souseda dostaneš jeho booster, zase vybereš 1 a pošleš dál — dokud se boostery nevyčerpají.
• 2. booster se posílá doprava, 3. zase doleva.
• Získáš pool cca 42 karet, z něj stavíš balík min. 40 karet + libovolně základních zemí.

Jak vybírat (pick order):
1. Bomby — karty, které samy vyhrají hru.
2. Removal — zničení/odstranění tvora.
3. Evasivní a efektivní tvorové (flying, menace, dobrý poměr síla/cena).
4. Kombinační karty a triky až nakonec.
• Prvních ~5 picků ber nejsilnější kartu bez ohledu na barvu.
• Pak sleduj, které barvy ti chodí pozdě (silné karty v 6.–9. picku) — ta barva je „otevřená“, sousedé ji neberou.
• Do konce 1. boosteru se snaž ustálit na 2 barvách.

Stavba balíku: 17 zemí + 23 kouzel, 15–17 tvorů, co nejvíc removalu, rozumná křivka.
Tip: v balíku ťukni na „Skenovat pool“ a naskenuj vydraftované karty — rádce je ohodnotí a doporučí barvy. Po dohrání je můžeš hromadně přidat do sbírky.
""".trimIndent()
    else -> ""
}

@Composable
fun LimitedGuideDialog(format: DeckFormat, onDismiss: () -> Unit) {
    com.example.antimarcipan.ui.GoldDialog(
        onDismissRequest = onDismiss,
        title = { Text(format.label) },
        text = {
            Column(Modifier.androidx()) {
                Text(limitedGuide(format), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Rozumím") } },
    )
}

@Composable
private fun Modifier.androidx(): Modifier = androidxScroll()
