package com.example.antimarcipan.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A card the user owns, stored in their local collection ("album"). */
@Entity(tableName = "collection_cards")
data class CollectionCard(
    @PrimaryKey val scryfallId: String,
    val name: String,
    val setCode: String?,
    val setName: String?,
    val collectorNumber: String?,
    val rarity: String?,
    val manaCost: String?,
    val cmc: Double?,
    val typeLine: String?,
    val oracleText: String?,
    /** WUBRG letters stored as a comma-separated string, e.g. "W,U,B". */
    val colorIdentity: String,
    /** Barvy many, kterou karta produkuje (hlavně země), oddělené čárkou. */
    val producedMana: String = "",
    val keywords: String,
    val imageUrl: String?,
    /** Lokálně stažený náhled karty (aby se zobrazoval offline a spolehlivě). */
    val localImagePath: String? = null,
    /** Odkaz na tuto konkrétní kartu na Gathereru (oficiální pravidla). */
    val gathererUrl: String? = null,
    val priceUsd: String?,
    val priceEur: String? = null,
    val quantity: Int = 1,
    /** Karta je ve sbírce (vlastněná). */
    val owned: Boolean = true,
    /** Karta je na wishlistu (chci ji). */
    val wishlist: Boolean = false,
    /** Playground — prozkoumané karty (např. špatně rozpoznané nebo k prohlédnutí). */
    val playground: Boolean = false,
    val addedAt: Long = System.currentTimeMillis(),
)

/** A deck the user is building. */
@Entity(tableName = "decks")
data class Deck(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val commanderScryfallId: String? = null,
    val commanderName: String? = null,
    /** Lokální cesta k náhledovému obrázku balíku (např. rub obalu). */
    val coverImagePath: String? = null,
    /** Ručně zvolené barvy balíku, např. "WUB" (znaky many W/U/B/R/G/C). */
    val colors: String? = null,
    /** Formát: COMMANDER, SINGLETON (jednokarťák), FREEFORM. */
    val format: String = "FREEFORM",
    /** Limited (draft/sealed): kód edice, ze které je pool karet. */
    val poolSet: String? = null,
    /** Pool už byl hromadně přidán do sbírky. */
    val poolImported: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

/** Many-to-many link between a deck and the cards in it. */
@Entity(
    tableName = "deck_cards",
    primaryKeys = ["deckId", "scryfallId"],
    indices = [Index("scryfallId")],
)
data class DeckCard(
    val deckId: Long,
    val scryfallId: String,
    val quantity: Int = 1,
)

/** Pool limited balíku (draft/sealed): karty z otevřených boosterů. */
@Entity(
    tableName = "deck_pool",
    primaryKeys = ["deckId", "scryfallId"],
    indices = [Index("scryfallId")],
)
data class DeckPoolCard(
    val deckId: Long,
    val scryfallId: String,
    val quantity: Int = 1,
)
