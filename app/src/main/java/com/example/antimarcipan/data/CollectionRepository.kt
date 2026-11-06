package com.example.antimarcipan.data

import com.example.antimarcipan.data.local.CollectionCard
import com.example.antimarcipan.data.local.CollectionDao
import com.example.antimarcipan.data.scryfall.ScryfallApi
import com.example.antimarcipan.data.scryfall.ScryfallCard
import com.example.antimarcipan.scan.OcrParse
import kotlinx.coroutines.flow.Flow
import java.util.Locale

/** Resolves scanned text into cards and manages the local collection. */
class CollectionRepository(
    private val dao: CollectionDao,
    private val scryfall: ScryfallApi = Network.scryfall,
) {
    val cards: Flow<List<CollectionCard>> = dao.observeAll()
    val count: Flow<Int> = dao.observeCount()

    /**
     * Identifikuje kartu z OCR textu. Nejdřív najde kartu podle jména (fuzzy),
     * a pokud se z fotky podařilo přečíst edici (set kód + číslo) a sedí na
     * stejnou kartu, vrátí rovnou ten konkrétní tisk — tedy správnou edici.
     */
    suspend fun lookup(rawText: String): Result<ScryfallCard> = runCatching {
        val parsed = OcrParse.parse(rawText)
        require(parsed.nameGuess.length >= 3) { "Text příliš krátký na jméno karty" }

        val byName = scryfall.cardByFuzzyName(parsed.nameGuess)

        // Zkus upřesnit edici podle set kódu + čísla z fotky.
        val set = parsed.setCode
        val number = parsed.collectorNumber
        if (set != null && number != null) {
            runCatching {
                scryfall.cardBySetAndNumber(set.lowercase(Locale.ROOT), number)
            }.getOrNull()?.let { print ->
                if (sameCard(print.name, byName.name)) return@runCatching print
            }
        }
        byName
    }

    /** Všechny tisky (edice) dané karty — pro ruční výběr správné edice. */
    suspend fun printsOf(cardName: String): Result<List<ScryfallCard>> = runCatching {
        scryfall.search(
            query = "!\"$cardName\"",
            order = "released",
            unique = "prints",
        ).data
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

    private fun sameCard(a: String, b: String): Boolean {
        fun norm(s: String) = s.lowercase(Locale.ROOT).substringBefore("//").trim()
        return norm(a) == norm(b)
    }
}
