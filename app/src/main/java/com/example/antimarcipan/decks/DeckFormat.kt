package com.example.antimarcipan.decks

import com.example.antimarcipan.data.local.CollectionCard

enum class DeckFormat(val code: String, val label: String) {
    COMMANDER("COMMANDER", "Commander"),
    SINGLETON("SINGLETON", "Jednokarťák"),
    FREEFORM("FREEFORM", "Volný");

    companion object {
        fun from(code: String?): DeckFormat =
            entries.firstOrNull { it.code == code } ?: FREEFORM
    }
}

data class DeckLegality(
    val format: DeckFormat,
    val totalCards: Int,
    val minCards: Int,
    val duplicates: List<String>,
    val needsCommander: Boolean,
    val hasCommander: Boolean,
) {
    val sizeOk: Boolean get() = minCards == 0 || totalCards >= minCards
    val singletonOk: Boolean get() = duplicates.isEmpty()
    val commanderOk: Boolean get() = !needsCommander || hasCommander
    val isLegal: Boolean get() = sizeOk && singletonOk && commanderOk
}

object DeckRules {
    fun legality(
        format: DeckFormat,
        cards: List<CollectionCard>,
        hasCommander: Boolean,
    ): DeckLegality {
        val total = cards.size
        val dups = DeckDuplicates.duplicateNames(cards)
        val minCards = when (format) {
            DeckFormat.COMMANDER -> 100
            DeckFormat.SINGLETON -> 100
            DeckFormat.FREEFORM -> 0
        }
        val singletonFormat = format == DeckFormat.COMMANDER || format == DeckFormat.SINGLETON
        return DeckLegality(
            format = format,
            totalCards = total,
            minCards = minCards,
            duplicates = if (singletonFormat) dups else emptyList(),
            needsCommander = format == DeckFormat.COMMANDER,
            hasCommander = hasCommander,
        )
    }
}
