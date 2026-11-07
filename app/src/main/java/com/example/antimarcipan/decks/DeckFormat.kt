package com.example.antimarcipan.decks

import com.example.antimarcipan.data.local.CollectionCard

enum class DeckFormat(val code: String, val label: String) {
    COMMANDER("COMMANDER", "Commander"),
    SINGLETON("SINGLETON", "Jednokarťák"),
    STANDARD("STANDARD", "Standard"),
    DRAFT("DRAFT", "Draft"),
    SEALED("SEALED", "Sealed deck"),
    FREEFORM("FREEFORM", "Volný");

    /** Minimální počet karet pro formát (0 = bez omezení). */
    val minCards: Int
        get() = when (this) {
            COMMANDER, SINGLETON -> 100
            STANDARD -> 60
            DRAFT, SEALED -> 40
            FREEFORM -> 0
        }

    /** Max. kopií jedné nezákladní karty (0 = bez omezení). */
    val maxCopies: Int
        get() = when (this) {
            COMMANDER, SINGLETON -> 1
            STANDARD -> 4
            else -> 0 // limited: kolik máš v poolu, tolik smíš
        }

    /** Limited formát — balík se staví z otevřených boosterů (poolu). */
    val isLimited: Boolean get() = this == DRAFT || this == SEALED

    val rules: String
        get() = when (this) {
            COMMANDER -> "100 karet, singleton, 1 velitel"
            SINGLETON -> "jednokarťák: 100+ karet, singleton, bez velitele"
            STANDARD -> "min. 60 karet, max. 4 kopie, jen karty z aktuálních edic Standardu"
            DRAFT -> "limited: 3 boostery, výběr karet po jedné, balík min. 40 karet"
            SEALED -> "limited: 6 boosterů do poolu, balík min. 40 karet"
            FREEFORM -> "bez omezení"
        }

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
        val dups = when (format.maxCopies) {
            0 -> emptyList()
            1 -> DeckDuplicates.duplicateNames(cards)
            else -> cards.filter { !LimitedAdvisor.isBasic(it) }
                .groupBy { it.name.lowercase() }
                .filter { it.value.size > format.maxCopies }
                .map { it.value.first().name }
        }
        return DeckLegality(
            format = format,
            totalCards = cards.size,
            minCards = format.minCards,
            duplicates = dups,
            needsCommander = format == DeckFormat.COMMANDER,
            hasCommander = hasCommander,
        )
    }
}
