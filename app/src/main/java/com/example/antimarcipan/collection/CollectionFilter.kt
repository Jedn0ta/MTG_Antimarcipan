package com.example.antimarcipan.collection

import com.example.antimarcipan.data.colorIdentityList
import com.example.antimarcipan.data.local.CollectionCard

enum class CardColor(val code: String, val label: String) {
    W("W", "Bílá"), U("U", "Modrá"), B("B", "Černá"),
    R("R", "Červená"), G("G", "Zelená"), C("C", "Bezbarvá"),
}

enum class CardType(val needle: String, val label: String) {
    CREATURE("Creature", "Creature"),
    INSTANT("Instant", "Instant"),
    SORCERY("Sorcery", "Sorcery"),
    ARTIFACT("Artifact", "Artifact"),
    ENCHANTMENT("Enchantment", "Enchantment"),
    PLANESWALKER("Planeswalker", "Planeswalker"),
    LAND("Land", "Land"),
    BATTLE("Battle", "Battle"),
}

enum class Rarity(val code: String, val label: String) {
    COMMON("common", "Common"),
    UNCOMMON("uncommon", "Uncommon"),
    RARE("rare", "Rare"),
    MYTHIC("mythic", "Mythic"),
}

enum class SortBy(val label: String) {
    ADDED("Nejnovější"),
    NAME("Jméno"),
    CMC("Mana value"),
    PRICE("Cena"),
    RARITY("Rarita"),
}

/** Sada filtrů nad sbírkou. Prázdná skupina = bez omezení v dané kategorii. */
data class CollectionFilter(
    val query: String = "",
    val colors: Set<CardColor> = emptySet(),
    val types: Set<CardType> = emptySet(),
    val rarities: Set<Rarity> = emptySet(),
    /** Vybrané mana value (0..6, 7 = 7+). Prázdné = všechny. */
    val cmcBuckets: Set<Int> = emptySet(),
    /** Vybrané edice (set kódy). Prázdné = všechny. */
    val sets: Set<String> = emptySet(),
    val sortBy: SortBy = SortBy.NAME,
    val sortDesc: Boolean = false,
) {
    val isActive: Boolean
        get() = query.isNotBlank() || colors.isNotEmpty() || types.isNotEmpty() ||
            rarities.isNotEmpty() || cmcBuckets.isNotEmpty() || sets.isNotEmpty()
}

/** Edice dostupná ve sbírce pro filtr. */
data class SetOption(val code: String, val label: String)

object CollectionFiltering {

    fun apply(cards: List<CollectionCard>, f: CollectionFilter): List<CollectionCard> {
        val filtered = cards.filter { card ->
            matchesQuery(card, f.query) &&
                matchesColors(card, f.colors) &&
                matchesTypes(card, f.types) &&
                matchesRarity(card, f.rarities) &&
                matchesCmc(card, f.cmcBuckets) &&
                matchesSet(card, f.sets)
        }
        val sorted = when (f.sortBy) {
            SortBy.ADDED -> filtered.sortedBy { it.addedAt }
            SortBy.NAME -> filtered.sortedBy { it.name.lowercase() }
            SortBy.CMC -> filtered.sortedBy { it.cmc ?: 0.0 }
            SortBy.PRICE -> filtered.sortedBy { it.priceUsd?.toDoubleOrNull() ?: 0.0 }
            SortBy.RARITY -> filtered.sortedBy { rarityRank(it.rarity) }
        }
        return if (f.sortDesc) sorted.reversed() else sorted
    }

    private fun matchesQuery(card: CollectionCard, q: String): Boolean {
        if (q.isBlank()) return true
        val needle = q.trim().lowercase()
        return card.name.lowercase().contains(needle) ||
            (card.oracleText?.lowercase()?.contains(needle) == true) ||
            (card.typeLine?.lowercase()?.contains(needle) == true)
    }

    private fun matchesColors(card: CollectionCard, colors: Set<CardColor>): Boolean {
        if (colors.isEmpty()) return true
        val ci = card.colorIdentityList()
        return colors.any { sel ->
            if (sel == CardColor.C) ci.isEmpty() else ci.contains(sel.code)
        }
    }

    private fun matchesTypes(card: CollectionCard, types: Set<CardType>): Boolean {
        if (types.isEmpty()) return true
        val line = card.typeLine?.lowercase() ?: return false
        return types.any { line.contains(it.needle.lowercase()) }
    }

    private fun matchesRarity(card: CollectionCard, rarities: Set<Rarity>): Boolean {
        if (rarities.isEmpty()) return true
        val r = card.rarity?.lowercase() ?: return false
        return rarities.any { it.code == r }
    }

    private fun matchesCmc(card: CollectionCard, buckets: Set<Int>): Boolean {
        if (buckets.isEmpty()) return true
        val cmc = (card.cmc ?: 0.0).toInt()
        val bucket = if (cmc >= 7) 7 else cmc
        return buckets.contains(bucket)
    }

    private fun matchesSet(card: CollectionCard, sets: Set<String>): Boolean {
        if (sets.isEmpty()) return true
        return card.setCode != null && sets.contains(card.setCode)
    }

    private fun rarityRank(r: String?): Int = when (r?.lowercase()) {
        "common" -> 0
        "uncommon" -> 1
        "rare" -> 2
        "mythic" -> 3
        else -> -1
    }
}
