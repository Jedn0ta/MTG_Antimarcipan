package com.example.antimarcipan.data

import com.example.antimarcipan.data.local.CollectionCard
import com.example.antimarcipan.data.local.Deck
import com.example.antimarcipan.data.local.DeckCard
import com.example.antimarcipan.data.local.DeckCardWithQty
import com.example.antimarcipan.data.local.DeckDao
import kotlinx.coroutines.flow.Flow

class DeckRepository(private val dao: DeckDao) {

    val decks: Flow<List<Deck>> = dao.observeDecks()

    fun deckCards(deckId: Long): Flow<List<CollectionCard>> = dao.observeDeckCards(deckId)

    fun deckCardsWithQty(deckId: Long): Flow<List<DeckCardWithQty>> =
        dao.observeDeckCardsWithQty(deckId)

    suspend fun setDeckCardQuantity(deckId: Long, scryfallId: String, qty: Int) =
        dao.setDeckCardQuantity(deckId, scryfallId, qty)

    suspend fun createDeck(name: String, format: String = "FREEFORM", colors: String? = null, poolSet: String? = null): Long =
        dao.insertDeck(Deck(name = name, format = format, colors = colors, poolSet = poolSet))

    fun pool(deckId: Long) = dao.observePool(deckId)

    suspend fun addToPool(deckId: Long, scryfallId: String) {
        val q = dao.poolQty(deckId, scryfallId) ?: 0
        dao.upsertPool(com.example.antimarcipan.data.local.DeckPoolCard(deckId, scryfallId, q + 1))
    }

    suspend fun setPoolQty(deckId: Long, scryfallId: String, qty: Int) {
        if (qty <= 0) dao.deletePool(deckId, scryfallId)
        else dao.upsertPool(com.example.antimarcipan.data.local.DeckPoolCard(deckId, scryfallId, qty))
    }

    suspend fun poolOf(deckId: Long) = dao.poolOf(deckId)
    suspend fun setPoolImported(deckId: Long, v: Boolean) = dao.setPoolImported(deckId, v)

    suspend fun setPoolSet(deckId: Long, set: String?) = dao.setPoolSet(deckId, set)

    suspend fun setColors(deckId: Long, colors: String?) = dao.setColors(deckId, colors)

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

    /**
     * Barvy balíku odvozené z many, kterou produkují země v balíku
     * (tj. jaké barvy reálně umíš zahrát). Jen W/U/B/R/G.
     */
    suspend fun deckColors(deckId: Long): Set<String> =
        dao.deckLandProducedMana(deckId)
            .flatMap { it.split(",") }
            .map { it.trim().uppercase() }
            .filter { it in setOf("W", "U", "B", "R", "G") }
            .toSet()

    /** Nastaví počet základní země v balíku (karta už musí být přidaná). */
    suspend fun setBasicLandCount(deckId: Long, scryfallId: String, count: Int) {
        if (count <= 0) {
            dao.removeDeckCard(deckId, scryfallId)
        } else {
            dao.upsertDeckCard(DeckCard(deckId = deckId, scryfallId = scryfallId, quantity = count))
        }
    }
}
