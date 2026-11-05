package com.example.antimarcipan.data

import com.example.antimarcipan.data.local.CollectionCard
import com.example.antimarcipan.data.local.CollectionDao
import com.example.antimarcipan.data.scryfall.ScryfallApi
import com.example.antimarcipan.data.scryfall.ScryfallCard
import kotlinx.coroutines.flow.Flow

/** Resolves scanned text into cards and manages the local collection. */
class CollectionRepository(
    private val dao: CollectionDao,
    private val scryfall: ScryfallApi = Network.scryfall,
) {
    val cards: Flow<List<CollectionCard>> = dao.observeAll()
    val count: Flow<Int> = dao.observeCount()

    /**
     * Looks up a card from raw OCR text using Scryfall's fuzzy matcher.
     * Returns null if nothing matched (ambiguous or not a card name).
     */
    suspend fun lookup(rawText: String): Result<ScryfallCard> = runCatching {
        val cleaned = cleanOcr(rawText)
        require(cleaned.length >= 3) { "Text too short to be a card name" }
        scryfall.cardByFuzzyName(cleaned)
    }

    suspend fun addToCollection(card: ScryfallCard) {
        val existing = dao.findById(card.id)
        if (existing == null) {
            dao.upsert(card.toCollectionCard())
        } else {
            dao.bumpQuantity(card.id, 1)
        }
    }

    suspend fun remove(scryfallId: String) = dao.delete(scryfallId)

    /**
     * Keeps only the first plausible card-name line and strips obvious OCR
     * junk (mana symbols, set codes, collector numbers). The card title sits
     * on the top line of a Magic card, so the first non-empty line is usually
     * the best guess.
     */
    private fun cleanOcr(raw: String): String {
        val firstLine = raw.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.length >= 3 && it.any(Char::isLetter) }
            ?: raw.trim()
        return firstLine
            .replace(Regex("[^\\p{L}\\p{N} ,'\\-]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
