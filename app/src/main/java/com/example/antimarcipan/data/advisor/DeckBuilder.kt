package com.example.antimarcipan.data.advisor

import com.example.antimarcipan.data.local.CollectionCard
import com.example.antimarcipan.decks.LimitedAdvisor

/** Typ balíku, který Rádce staví ze sbírky. */
enum class BuildFormat(val label: String, val size: Int, val lands: Int, val maxCopies: Int, val deckFormat: String) {
    COMMANDER("Commander (100)", 100, 37, 1, "COMMANDER"),
    CASUAL60("Běžný 60 karet", 60, 24, 4, "FREEFORM"),
    STANDARD("Standard 60", 60, 24, 4, "STANDARD"),
    LIMITED40("Limited 40", 40, 17, 99, "FREEFORM"),
}

data class BuildResult(
    val format: BuildFormat,
    val colors: List<String>,
    val commander: CollectionCard?,
    /** Karty ze sbírky s počtem kusů. */
    val cards: List<Pair<CollectionCard, Int>>,
    /** Základní země: jméno → počet. */
    val basics: Map<String, Int>,
    val notes: List<String>,
) {
    val spellCount get() = cards.filter { !LimitedAdvisor.isLand(it.first) }.sumOf { it.second }
    val landCount get() = cards.filter { LimitedAdvisor.isLand(it.first) }.sumOf { it.second } + basics.values.sum()
    val total get() = cards.sumOf { it.second } + basics.values.sum() + (if (commander != null) 1 else 0)
}

/**
 * Postaví balík z vlastněných karet: vybere barvy (nebo podle velitele),
 * seřadí karty podle hodnocení a rolí, hlídá křivku a doplní země.
 */
object DeckBuilder {

    private val rampRx = Regex("add \\{|search your library for (a|up to \\w+) .*land", RegexOption.IGNORE_CASE)
    private val drawRx = Regex("draw (a|two|three|x) cards?|draws? cards", RegexOption.IGNORE_CASE)

    fun isRamp(c: CollectionCard) = !LimitedAdvisor.isLand(c) && rampRx.containsMatchIn(c.oracleText ?: "")
    fun isDraw(c: CollectionCard) = drawRx.containsMatchIn(c.oracleText ?: "")

    fun commanderCandidates(owned: List<CollectionCard>): List<CollectionCard> =
        owned.filter {
            val t = it.typeLine ?: ""
            t.contains("Legendary", true) && t.contains("Creature", true) ||
                (it.oracleText ?: "").contains("can be your commander", true)
        }.distinctBy { it.name }.sortedByDescending { LimitedAdvisor.rating(it) }

    /** Doporučené barvy podle síly karet ve sbírce. */
    fun bestColors(owned: List<CollectionCard>, n: Int = 2): List<String> =
        LimitedAdvisor.colorScores(owned).entries.sortedByDescending { it.value }.take(n).map { it.key }

    fun build(
        owned: List<CollectionCard>,
        format: BuildFormat,
        colorsIn: List<String>?,
        commander: CollectionCard?,
    ): BuildResult {
        val notes = mutableListOf<String>()
        val colors = when {
            commander != null -> LimitedAdvisor.colors(commander).ifEmpty { listOf("C") }
            !colorsIn.isNullOrEmpty() -> colorsIn
            else -> bestColors(owned)
        }
        val allowed = colors.toSet()
        fun fits(c: CollectionCard) = LimitedAdvisor.colors(c).all { it in allowed }

        val pool = owned.filter { !LimitedAdvisor.isBasic(it) && fits(it) && it.scryfallId != commander?.scryfallId }
            .distinctBy { it.name.lowercase() } // stejné jméno z různých edic = jedna karta

        // Kolik kusů karty smím (dle sbírky a formátu)
        val ownedByName = owned.groupBy { it.name.lowercase() }.mapValues { e -> e.value.sumOf { it.quantity.coerceAtLeast(1) } }
        fun copies(c: CollectionCard) = minOf(format.maxCopies, ownedByName[c.name.lowercase()] ?: 1).coerceAtLeast(1)

        val spellSlots = format.size - format.lands - (if (commander != null) 1 else 0)

        // Skóre: síla + role + synergie s velitelem (sdílené typy tvorů / klíčová slova)
        val cmdWords = commander?.let { (it.typeLine + " " + it.keywords + " " + (it.oracleText ?: "")).lowercase() } ?: ""
        fun score(c: CollectionCard): Double {
            var s = LimitedAdvisor.rating(c)
            if (format == BuildFormat.COMMANDER) {
                if (isRamp(c)) s += 2.0
                if (isDraw(c)) s += 1.2
            }
            if (cmdWords.isNotEmpty()) {
                val sub = (c.typeLine ?: "").substringAfter("—", "").trim().lowercase().split(" ").filter { it.length > 2 }
                if (sub.any { it in cmdWords }) s += 1.5
                c.keywords.lowercase().split(",").map { it.trim() }.filter { it.length > 3 }.forEach { if (it in cmdWords) s += 0.6 }
            }
            return s
        }

        // Cílová křivka (podíl kouzel na MV)
        val curveShare = when (format) {
            BuildFormat.COMMANDER -> mapOf(0 to .02, 1 to .1, 2 to .2, 3 to .22, 4 to .18, 5 to .12, 6 to .16)
            BuildFormat.LIMITED40 -> mapOf(0 to .0, 1 to .08, 2 to .26, 3 to .26, 4 to .2, 5 to .12, 6 to .08)
            else -> mapOf(0 to .0, 1 to .14, 2 to .28, 3 to .26, 4 to .18, 5 to .08, 6 to .06)
        }
        val bucketCap = curveShare.mapValues { (kotlin.math.ceil(it.value * spellSlots) + 1).toInt() }
        val bucketCount = mutableMapOf<Int, Int>()

        val chosen = mutableListOf<Pair<CollectionCard, Int>>()
        var filled = 0
        val spells = pool.filter { !LimitedAdvisor.isLand(it) }.sortedByDescending { score(it) }
        // 1. průchod s hlídáním křivky, 2. průchod doplní zbytek
        for (pass in 0..1) {
            for (c in spells) {
                if (filled >= spellSlots) break
                if (chosen.any { it.first.scryfallId == c.scryfallId }) continue
                val b = minOf(6, (c.cmc ?: 0.0).toInt())
                if (pass == 0 && (bucketCount[b] ?: 0) >= (bucketCap[b] ?: 99)) continue
                val q = minOf(copies(c), spellSlots - filled)
                chosen += c to q
                filled += q
                bucketCount[b] = (bucketCount[b] ?: 0) + q
            }
        }
        if (filled < spellSlots) notes += "Ve sbírce nemáš dost karet v barvách ${colors.joinToString("")}: chybí ${spellSlots - filled} kouzel."

        // Země: nebasic duální/užitečné až třetina slotů
        val nonBasic = pool.filter { LimitedAdvisor.isLand(it) }
            .filter { l -> l.producedMana.split(",").map { it.trim() }.filter { it.isNotEmpty() }.let { p -> p.isEmpty() || p.any { it in allowed } } }
            .sortedByDescending { it.producedMana.split(",").count { p -> p.trim() in allowed } }
            .take(format.lands / 3)
        nonBasic.forEach { chosen += it to minOf(copies(it), if (format.maxCopies == 1) 1 else 2) }
        val nbCount = nonBasic.sumOf { minOf(copies(it), if (format.maxCopies == 1) 1 else 2) }
        val basicsNeeded = format.lands - nbCount
        var basics = LimitedAdvisor.basicSplit(chosen.map { it.first }.filter { !LimitedAdvisor.isLand(it) }, basicsNeeded)
        if (basics.isEmpty() && basicsNeeded > 0) {
            basics = if (colors == listOf("C")) mapOf("Wastes" to basicsNeeded)
            else colors.mapNotNull { LimitedAdvisor.BASIC_FOR[it] }.let { names ->
                names.mapIndexed { i, n -> n to (basicsNeeded / names.size + if (i < basicsNeeded % names.size) 1 else 0) }.toMap()
            }
        }

        // Poznámky k rolím
        val sp = chosen.filter { !LimitedAdvisor.isLand(it.first) }
        val removal = sp.filter { LimitedAdvisor.isRemoval(it.first) }.sumOf { it.second }
        val creatures = sp.filter { LimitedAdvisor.isCreature(it.first) }.sumOf { it.second }
        val ramp = sp.filter { isRamp(it.first) }.sumOf { it.second }
        val draw = sp.filter { isDraw(it.first) }.sumOf { it.second }
        notes += "Tvorové $creatures · removal $removal · ramp $ramp · dobírání $draw."
        if (removal < (if (format == BuildFormat.COMMANDER) 6 else 4)) notes += "Málo removalu — doplň ho, až nějaký získáš."
        if (format == BuildFormat.COMMANDER && ramp < 8) notes += "Commander chce ~10 rampu; máš $ramp."
        if (format == BuildFormat.STANDARD) notes += "Legalitu ve Standardu appka nekontroluje — ověř si edice."

        return BuildResult(format, colors, commander, chosen.sortedWith(compareBy({ LimitedAdvisor.isLand(it.first) }, { it.first.cmc ?: 0.0 }, { it.first.name })), basics, notes)
    }
}
