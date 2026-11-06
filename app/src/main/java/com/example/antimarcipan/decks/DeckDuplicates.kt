package com.example.antimarcipan.decks

import com.example.antimarcipan.data.local.CollectionCard

object DeckDuplicates {

    private val BASICS = setOf(
        "plains", "island", "swamp", "mountain", "forest", "wastes",
        "snow-covered plains", "snow-covered island", "snow-covered swamp",
        "snow-covered mountain", "snow-covered forest",
    )

    /**
     * Vrátí jména karet, která jsou v balíku víckrát (různé edice téže karty).
     * Základní země se nepočítají — těch smí být libovolně.
     *
     * Pozn.: Commander/singleton povoluje jen 1 kopii každé nezákladní karty.
     */
    fun duplicateNames(cards: List<CollectionCard>): List<String> {
        return cards
            .filter { !isBasicLand(it) }
            .groupBy { normalize(it.name) }
            .filter { it.value.size > 1 }
            .map { it.value.first().name }
            .sorted()
    }

    private fun isBasicLand(card: CollectionCard): Boolean {
        val name = normalize(card.name)
        if (name in BASICS) return true
        // „Basic ... Land" v type line
        return card.typeLine?.contains("Basic", ignoreCase = true) == true &&
            card.typeLine.contains("Land", ignoreCase = true)
    }

    private fun normalize(name: String): String =
        name.lowercase().substringBefore("//").trim()
}
