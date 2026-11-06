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
    val keywords: String,
    val imageUrl: String?,
    /** Lokálně stažený náhled karty (aby se zobrazoval offline a spolehlivě). */
    val localImagePath: String? = null,
    val priceUsd: String?,
    val quantity: Int = 1,
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
