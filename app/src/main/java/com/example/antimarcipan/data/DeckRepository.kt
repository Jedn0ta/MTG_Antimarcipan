package com.example.antimarcipan.data

import com.example.antimarcipan.data.local.CollectionCard
import com.example.antimarcipan.data.local.Deck
import com.example.antimarcipan.data.local.DeckCard
import com.example.antimarcipan.data.local.DeckDao
import kotlinx.coroutines.flow.Flow

class DeckRepository(private val dao: DeckDao) {

    val decks: Flow<List<Deck>> = dao.observeDecks()

    fun deckCards(deckId: Long): Flow<List<CollectionCard>> = dao.observeDeckCards(deckId)

    suspend fun createDeck(name: String, format: String = "FREEFORM"): Long =
        dao.insertDeck(Deck(name = name, format = format))

    suspend fun deleteDeck(deckId: Long) = dao.deleteDeck(deckId)

    suspend fun deck(deckId: Long): Deck? = dao.findDeck(deckId)

    suspend fun addCard(deckId: Long, scryfallId: String) =
        dao.upsertDeckCard(DeckCard(deckId = deckId, scryfallId = scryfallId))

    suspend fun removeCard(deckId: Long, scryfallId: String) =
        dao.removeDeckCard(deckId, scryfallId)

    suspend fun setCommander(deckId: Long, card: CollectionCard) =
        dao.setCommander(deckId, card.scryfallId, card.name)

    suspend fun rename(deckId: Long, name: String) =
        dao.renameDeck(deckId, name.ifBlank { "Balík" })

    suspend fun setCover(deckId: Long, path: String?) = dao.setCover(deckId, path)

    suspend fun setFormat(deckId: Long, format: String) = dao.setFormat(deckId, format)

    suspend fun cardCount(deckId: Long): Int = dao.deckCardCount(deckId)

    /** Vrátí množinu barev (W/U/B/R/G), které balík hraje. */
    suspend fun deckColors(deckId: Long): Set<String> =
        dao.deckColorIdentities(deckId)
            .flatMap { it.split(",") }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
}
