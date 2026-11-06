package com.example.antimarcipan.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {

    @Query("SELECT * FROM collection_cards ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<CollectionCard>>

    @Query("SELECT * FROM collection_cards WHERE scryfallId = :id")
    suspend fun findById(id: String): CollectionCard?

    @Upsert
    suspend fun upsert(card: CollectionCard)

    @Query("UPDATE collection_cards SET quantity = quantity + :delta WHERE scryfallId = :id")
    suspend fun bumpQuantity(id: String, delta: Int)

    @Query("DELETE FROM collection_cards WHERE scryfallId = :id")
    suspend fun delete(id: String)

    @Query("SELECT COUNT(*) FROM collection_cards")
    fun observeCount(): Flow<Int>

    @Query("SELECT name FROM collection_cards")
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
}
