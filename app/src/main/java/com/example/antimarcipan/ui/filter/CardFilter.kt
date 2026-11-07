package com.example.antimarcipan.ui.filter

import com.example.antimarcipan.data.colorIdentityList
import com.example.antimarcipan.data.local.CollectionCard

/** Jak se mají vybrané barvy vyhodnotit. */
enum class ColorMode(val label: String) {
    ANY("Obsahuje některou"),
    ALL("Obsahuje všechny"),
    EXACT("Přesně tyto"),
}

/**
 * Kompletní filtr karet — společný pro Sbírku i Balíky.
 * Prázdná množina = bez omezení v dané kategorii.
 */
data class CardFilterState(
    val name: String = "",
    val text: String = "",
    val colors: Set<String> = emptySet(),          // W U B R G C
    val colorMode: ColorMode = ColorMode.ANY,
    val cmcMin: Int = 0,
    val cmcMax: Int = CMC_CAP,                     // CMC_CAP = „7+“
    val types: Set<String> = emptySet(),
    val supertypes: Set<String> = emptySet(),
    val tribes: Set<String> = emptySet(),          // podtypy stvoření
    val subtypes: Set<String> = emptySet(),        // ostatní podtypy (Equipment, Aura, Forest…)
    val keywords: Set<String> = emptySet(),        // vlastnosti (Deathtouch, Lifelink…)
    val sets: Set<String> = emptySet(),            // kódy edic
    val rarities: Set<String> = emptySet(),
) {
    /** Počet aktivních kritérií (pro odznak na ikoně). */
    val activeCount: Int
        get() = listOf(
            name.isNotBlank(), text.isNotBlank(), colors.isNotEmpty(),
            cmcMin > 0 || cmcMax < CMC_CAP, types.isNotEmpty(), supertypes.isNotEmpty(),
            tribes.isNotEmpty(), subtypes.isNotEmpty(), keywords.isNotEmpty(),
            sets.isNotEmpty(), rarities.isNotEmpty(),
        ).count { it }

    val isActive: Boolean get() = activeCount > 0

    companion object { const val CMC_CAP = 7 }
}

/** Rozbor typové řádky karty (i oboustranné „A // B“). */
data class ParsedType(
    val supertypes: Set<String>,
    val types: Set<String>,
    val creatureSubtypes: Set<String>,
    val otherSubtypes: Set<String>,
)

private val SUPERTYPES = setOf("Legendary", "Basic", "Snow", "World", "Ongoing", "Elite", "Host")

fun parseType(typeLine: String?): ParsedType {
    val sup = mutableSetOf<String>()
    val typ = mutableSetOf<String>()
    val cre = mutableSetOf<String>()
    val oth = mutableSetOf<String>()
    typeLine?.split("//")?.forEach { face ->
        val left = face.substringBefore("—").trim()
        val right = face.substringAfter("—", "").trim()
        val leftWords = left.split(" ").filter { it.isNotBlank() }
        leftWords.forEach { w -> if (w in SUPERTYPES) sup += w else typ += w }
        val subs = right.split(" ").map { it.trim() }.filter { it.isNotBlank() }
        val isCreature = leftWords.any { it == "Creature" || it == "Kindred" || it == "Tribal" }
        subs.forEach { if (isCreature) cre += it else oth += it }
    }
    return ParsedType(sup, typ, cre, oth)
}

fun CollectionCard.keywordSet(): Set<String> =
    keywords.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()

/** Možnosti filtru odvozené z daných karet (co se v nich opravdu vyskytuje). */
data class CardFilterOptions(
    val types: List<String>,
    val supertypes: List<String>,
    val tribes: List<String>,
    val subtypes: List<String>,
    val keywords: List<String>,
    val sets: List<Pair<String, String>>,   // kód → název
    val rarities: List<String>,
    val colors: List<String>,
)

private val TYPE_ORDER = listOf(
    "Creature", "Instant", "Sorcery", "Artifact", "Enchantment", "Planeswalker", "Land", "Battle", "Kindred", "Tribal",
)
private val RARITY_ORDER = listOf("common", "uncommon", "rare", "mythic", "special", "bonus")
private val COLOR_ORDER = listOf("W", "U", "B", "R", "G", "C")

fun buildFilterOptions(cards: List<CollectionCard>): CardFilterOptions {
    val types = mutableSetOf<String>()
    val sup = mutableSetOf<String>()
    val tribes = sortedSetOf<String>()
    val subs = sortedSetOf<String>()
    val kws = sortedSetOf<String>()
    val sets = sortedMapOf<String, String>()
    val rar = mutableSetOf<String>()
    val cols = mutableSetOf<String>()
    cards.forEach { c ->
        val p = parseType(c.typeLine)
        types += p.types; sup += p.supertypes; tribes += p.creatureSubtypes; subs += p.otherSubtypes
        kws += c.keywordSet()
        c.setCode?.let { code -> sets[code.lowercase()] = c.setName ?: code.uppercase() }
        c.rarity?.let { rar += it.lowercase() }
        val ci = c.colorIdentityList()
        if (ci.isEmpty()) cols += "C" else cols += ci
    }
    return CardFilterOptions(
        types = types.sortedBy { TYPE_ORDER.indexOf(it).let { i -> if (i < 0) 99 else i } },
        supertypes = sup.sorted(),
        tribes = tribes.toList(),
        subtypes = subs.toList(),
        keywords = kws.toList(),
        sets = sets.entries.map { it.key to it.value }.sortedBy { it.second },
        rarities = rar.sortedBy { RARITY_ORDER.indexOf(it).let { i -> if (i < 0) 99 else i } },
        colors = COLOR_ORDER.filter { it in cols },
    )
}

/** Projde-li karta filtrem. */
fun CardFilterState.matches(c: CollectionCard): Boolean {
    if (name.isNotBlank() && !c.name.contains(name.trim(), ignoreCase = true)) return false
    if (text.isNotBlank()) {
        val hay = (c.oracleText ?: "") + " " + (c.typeLine ?: "")
        if (!hay.contains(text.trim(), ignoreCase = true)) return false
    }
    if (colors.isNotEmpty()) {
        val ci = c.colorIdentityList().toSet().ifEmpty { setOf("C") }
        val ok = when (colorMode) {
            ColorMode.ANY -> ci.any { it in colors }
            ColorMode.ALL -> colors.all { it in ci }
            ColorMode.EXACT -> ci == colors
        }
        if (!ok) return false
    }
    val cmc = (c.cmc ?: 0.0).toInt()
    if (cmc < cmcMin) return false
    if (cmcMax < CardFilterState.CMC_CAP && cmc > cmcMax) return false

    val p = parseType(c.typeLine)
    if (types.isNotEmpty() && p.types.none { it in types }) return false
    if (supertypes.isNotEmpty() && p.supertypes.none { it in supertypes }) return false
    if (tribes.isNotEmpty() && p.creatureSubtypes.none { it in tribes }) return false
    if (subtypes.isNotEmpty() && p.otherSubtypes.none { it in subtypes }) return false
    if (keywords.isNotEmpty() && c.keywordSet().none { it in keywords }) return false
    if (sets.isNotEmpty() && (c.setCode?.lowercase() !in sets)) return false
    if (rarities.isNotEmpty() && (c.rarity?.lowercase() !in rarities)) return false
    return true
}
