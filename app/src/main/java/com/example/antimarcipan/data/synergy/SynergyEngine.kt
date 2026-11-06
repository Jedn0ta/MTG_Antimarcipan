package com.example.antimarcipan.data.synergy

import com.example.antimarcipan.data.colorIdentityList
import com.example.antimarcipan.data.local.CollectionCard

/** Jak dobře se karta hodí do balíku, s důvody. */
data class CardFit(
    val card: CollectionCard,
    val score: Int,             // 0..100
    val reasons: List<String>,
    val offColor: Boolean,
)

/** Výsledek analýzy balíku. */
data class DeckSynergyReport(
    val colors: Set<String>,
    val topKeywords: List<String>,
    val topTribes: List<String>,
    val topThemes: List<String>,
    val fits: List<CardFit>,         // seřazeno vzestupně dle skóre (nejhorší první)
    val misfits: List<CardFit>,      // kandidáti na výměnu (nízké skóre / mimo barvu)
)

/**
 * Offline heuristika synergie — funguje pro libovolný balík (ne jen EDH).
 * Odvodí z balíku jeho barvy, klíčová slova, tribaly a témata z textu karet
 * a každé kartě spočítá, jak moc do balíku zapadá.
 */
object SynergyEngine {

    // Témata z oracle textu: fráze → zobrazovaný název.
    private val THEMES = linkedMapOf(
        "\\+1/\\+1 counter" to "+1/+1 counters",
        "counter" to "counters",
        "sacrifice" to "sacrifice",
        "graveyard" to "graveyard",
        "token" to "tokens",
        "gain .* life" to "lifegain",
        "draw" to "card draw",
        "mill" to "mill",
        "deals? damage" to "damage/burn",
        "exile" to "exile",
        "dies" to "death triggers",
        "discard" to "discard",
        "enters the battlefield" to "ETB efekty",
        "equip" to "equipment",
        "enchant" to "auras/enchant",
        "proliferate" to "proliferate",
        "landfall|whenever a land" to "landfall",
        "cast your .* spell|instant or sorcery" to "spellslinger",
    )

    // Předkompilované regexy (bezpečně — vadný vzor se jen přeskočí).
    private val THEME_REGEX: Map<String, Regex> = THEMES.keys.mapNotNull { k ->
        runCatching { k to Regex(k) }.getOrNull()
    }.toMap()

    fun analyze(deck: List<CollectionCard>): DeckSynergyReport {
        val nonLands = deck.filter { !it.isLand() }

        // Barvy balíku
        val colors = deck.flatMap { it.colorIdentityList() }.toSet()

        // Klíčová slova (keywords), tribaly, témata přes celý balík
        val keywordCount = mutableMapOf<String, Int>()
        val tribeCount = mutableMapOf<String, Int>()
        val themeCount = mutableMapOf<String, Int>()

        deck.forEach { c ->
            c.keywordList().forEach { keywordCount.merge(it, 1, Int::plus) }
            c.creatureSubtypes().forEach { tribeCount.merge(it, 1, Int::plus) }
            val o = c.oracle()
            THEMES.forEach { (pattern, label) ->
                if (THEME_REGEX[pattern]?.containsMatchIn(o) == true) themeCount.merge(label, 1, Int::plus)
            }
        }

        val topKeywords = keywordCount.filter { it.value >= 2 }
            .entries.sortedByDescending { it.value }.map { it.key }
        val topTribes = tribeCount.filter { it.value >= 2 }
            .entries.sortedByDescending { it.value }.take(4).map { it.key }
        val topThemes = themeCount.filter { it.value >= 3 }
            .entries.sortedByDescending { it.value }.take(6).map { it.key }

        val fits = deck.map { card ->
            scoreCard(card, colors, topKeywords.toSet(), topTribes.toSet(), topThemes)
        }.sortedBy { it.score }

        val misfits = fits.filter { it.offColor || it.score < 40 }
            .filter { !it.card.isBasicLand() }

        return DeckSynergyReport(
            colors = colors.filter { it in setOf("W", "U", "B", "R", "G") }.toSet(),
            topKeywords = topKeywords.take(6),
            topTribes = topTribes,
            topThemes = topThemes,
            fits = fits,
            misfits = misfits,
        )
    }

    private fun scoreCard(
        card: CollectionCard,
        deckColors: Set<String>,
        topKeywords: Set<String>,
        topTribes: Set<String>,
        topThemes: List<String>,
    ): CardFit {
        val reasons = mutableListOf<String>()
        var score = 45

        // Barevná shoda
        val ci = card.colorIdentityList()
        val offColor = ci.any { it !in deckColors } && deckColors.isNotEmpty()
        when {
            offColor -> { score -= 35; reasons += "mimo barvu balíku" }
            ci.isNotEmpty() -> { score += 8 }
        }

        // Země se vždy hodí
        if (card.isLand()) {
            score = (score + 25).coerceAtMost(85)
            reasons += "země (mana základ)"
        }

        // Klíčová slova
        val kwHit = card.keywordList().toSet().intersect(topKeywords)
        if (kwHit.isNotEmpty()) {
            score += (kwHit.size * 7).coerceAtMost(21)
            reasons += "sdílí: ${kwHit.joinToString(", ")}"
        }

        // Tribaly
        val tribeHit = card.creatureSubtypes().toSet().intersect(topTribes)
        if (tribeHit.isNotEmpty()) {
            score += 12
            reasons += "tribal: ${tribeHit.joinToString(", ")}"
        }

        // Témata z textu
        val o = card.oracle()
        val themeHits = topThemes.filter { label ->
            THEMES.entries.firstOrNull { it.value == label }?.key
                ?.let { THEME_REGEX[it]?.containsMatchIn(o) } == true
        }
        if (themeHits.isNotEmpty()) {
            score += (themeHits.size * 8).coerceAtMost(24)
            reasons += "téma: ${themeHits.joinToString(", ")}"
        }

        if (reasons.isEmpty()) reasons += "žádná zřejmá synergie"

        return CardFit(
            card = card,
            score = score.coerceIn(0, 100),
            reasons = reasons,
            offColor = offColor,
        )
    }

    // --- pomocné ---
    private fun CollectionCard.oracle() = oracleText?.lowercase() ?: ""
    private fun CollectionCard.isLand() = typeLine?.contains("Land", true) == true
    private fun CollectionCard.isBasicLand() =
        typeLine?.contains("Basic", true) == true && isLand()

    private fun CollectionCard.keywordList(): List<String> =
        keywords.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    /** Podtypy stvoření z type line za pomlčkou. */
    private fun CollectionCard.creatureSubtypes(): List<String> {
        val line = typeLine ?: return emptyList()
        if (!line.contains("Creature", true)) return emptyList()
        val after = line.substringAfter("—", "").trim()
        return after.split(" ").map { it.trim() }.filter { it.isNotEmpty() }
    }
}
