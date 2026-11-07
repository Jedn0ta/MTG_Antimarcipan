package com.example.antimarcipan.data.local

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Karta v balíku i s počtem kusů v tomto balíku. */
data class DeckCardWithQty(
    @Embedded val card: CollectionCard,
    val deckQuantity: Int,
)

@Dao
interface CollectionDao {

    @Query("SELECT * FROM collection_cards WHERE owned = 1 ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<CollectionCard>>

    @Query("SELECT * FROM collection_cards WHERE owned = 0 AND wishlist = 0 ORDER BY addedAt DESC")
    fun observeUnsorted(): Flow<List<CollectionCard>>

    @Query("SELECT * FROM collection_cards WHERE wishlist = 1 ORDER BY addedAt DESC")
    fun observeWishlist(): Flow<List<CollectionCard>>

    @Query("SELECT * FROM collection_cards WHERE scryfallId = :id")
    suspend fun findById(id: String): CollectionCard?

    @Query("UPDATE collection_cards SET owned = :owned WHERE scryfallId = :id")
    suspend fun setOwned(id: String, owned: Boolean)

    @Query("UPDATE collection_cards SET wishlist = :wish WHERE scryfallId = :id")
    suspend fun setWishlist(id: String, wish: Boolean)

    // --- změna edice: přepojení odkazů v balících ---
    @Query("UPDATE deck_cards SET scryfallId = :newId WHERE scryfallId = :oldId AND deckId NOT IN (SELECT deckId FROM deck_cards WHERE scryfallId = :newId)")
    suspend fun repointDeckCards(oldId: String, newId: String)

    @Query("DELETE FROM deck_cards WHERE scryfallId = :oldId")
    suspend fun deleteDeckRefs(oldId: String)

    @Query("UPDATE decks SET commanderScryfallId = :newId WHERE commanderScryfallId = :oldId")
    suspend fun repointCommander(oldId: String, newId: String)

    @Upsert
    suspend fun upsert(card: CollectionCard)

    @Query("UPDATE collection_cards SET quantity = quantity + :delta WHERE scryfallId = :id")
    suspend fun bumpQuantity(id: String, delta: Int)

    @Query("DELETE FROM collection_cards WHERE scryfallId = :id")
    suspend fun delete(id: String)

    @Query("SELECT COUNT(*) FROM collection_cards")
    fun observeCount(): Flow<Int>

    @Query("SELECT name FROM collection_cards WHERE owned = 1")
    suspend fun allNames(): List<String>

    @Query("UPDATE collection_cards SET localImagePath = :path WHERE scryfallId = :id")
    suspend fun setLocalImage(id: String, path: String?)

    @Query("SELECT * FROM collection_cards WHERE localImagePath IS NULL AND imageUrl IS NOT NULL")
    suspend fun cardsWithoutLocalImage(): List<CollectionCard>
}

@Dao
interface DeckDao {

    @Query("SELECT * FROM decks ORDER BY createdAt DESC")
    fun observeDecks(): Flow<List<Deck>>

    @Query("SELECT * FROM decks WHERE id = :deckId")
    suspend fun findDeck(deckId: Long): Deck?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: Deck): Long

    @Query("DELETE FROM decks WHERE id = :deckId")
    suspend fun deleteDeck(deckId: Long)

    @Query("UPDATE decks SET commanderScryfallId = :cardId, commanderName = :name WHERE id = :deckId")
    suspend fun setCommander(deckId: Long, cardId: String?, name: String?)

    @Query("UPDATE decks SET name = :name WHERE id = :deckId")
    suspend fun renameDeck(deckId: Long, name: String)

    @Query("UPDATE decks SET coverImagePath = :path WHERE id = :deckId")
    suspend fun setCover(deckId: Long, path: String?)

    @Query("UPDATE decks SET colors = :colors WHERE id = :deckId")
    suspend fun setColors(deckId: Long, colors: String?)

    @Query("UPDATE decks SET poolSet = :set WHERE id = :deckId")
    suspend fun setPoolSet(deckId: Long, set: String?)

    @Query("UPDATE decks SET format = :format WHERE id = :deckId")
    suspend fun setFormat(deckId: Long, format: String)

    @Query("SELECT COUNT(*) FROM deck_cards WHERE deckId = :deckId")
    suspend fun deckCardCount(deckId: Long): Int

    /** Barevné identity karet v balíku (pro odvození barev balíku). */
    @Query(
        """
        SELECT c.colorIdentity FROM collection_cards c
        INNER JOIN deck_cards dc ON dc.scryfallId = c.scryfallId
        WHERE dc.deckId = :deckId
        """
    )
    suspend fun deckColorIdentities(deckId: Long): List<String>

    @Upsert
    suspend fun upsertDeckCard(deckCard: DeckCard)

    @Query("DELETE FROM deck_cards WHERE deckId = :deckId AND scryfallId = :cardId")
    suspend fun removeDeckCard(deckId: Long, cardId: String)

    /** Full card rows for every card linked to a deck. */
    @Query(
        """
        SELECT c.* FROM collection_cards c
        INNER JOIN deck_cards dc ON dc.scryfallId = c.scryfallId
        WHERE dc.deckId = :deckId
        ORDER BY c.cmc, c.name
        """
    )
    fun observeDeckCards(deckId: Long): Flow<List<CollectionCard>>

    /** Karty v balíku i s počtem kusů v balíku (deck_cards.quantity). */
    @Query(
        """
        SELECT c.*, dc.quantity AS deckQuantity FROM collection_cards c
        INNER JOIN deck_cards dc ON dc.scryfallId = c.scryfallId
        WHERE dc.deckId = :deckId
        ORDER BY c.cmc, c.name
        """
    )
    fun observeDeckCardsWithQty(deckId: Long): Flow<List<DeckCardWithQty>>

    @Query(
        """
        SELECT c.*, p.quantity AS deckQuantity FROM collection_cards c
        INNER JOIN deck_pool p ON p.scryfallId = c.scryfallId
        WHERE p.deckId = :deckId
        ORDER BY c.cmc, c.name
        """
    )
    fun observePool(deckId: Long): Flow<List<DeckCardWithQty>>

    @Query("SELECT * FROM deck_pool WHERE deckId = :deckId")
    suspend fun poolOf(deckId: Long): List<DeckPoolCard>

    @Query("SELECT quantity FROM deck_pool WHERE deckId = :deckId AND scryfallId = :id")
    suspend fun poolQty(deckId: Long, id: String): Int?

    @androidx.room.Upsert
    suspend fun upsertPool(card: DeckPoolCard)

    @Query("DELETE FROM deck_pool WHERE deckId = :deckId AND scryfallId = :id")
    suspend fun deletePool(deckId: Long, id: String)

    @Query("UPDATE decks SET poolImported = :v WHERE id = :deckId")
    suspend fun setPoolImported(deckId: Long, v: Boolean)

    @Query("UPDATE deck_cards SET quantity = :qty WHERE deckId = :deckId AND scryfallId = :scryfallId")
    suspend fun setDeckCardQuantity(deckId: Long, scryfallId: String, qty: Int)

    /** produkovaná mana zemí v balíku (pro odvození barev podle zemí). */
    @Query(
        """
        SELECT c.producedMana FROM collection_cards c
        INNER JOIN deck_cards dc ON dc.scryfallId = c.scryfallId
        WHERE dc.deckId = :deckId AND c.typeLine LIKE '%Land%'
        """
    )
    suspend fun deckLandProducedMana(deckId: Long): List<String>
}
