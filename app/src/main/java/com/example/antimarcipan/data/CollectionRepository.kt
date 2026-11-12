package com.example.antimarcipan.data

import android.content.Context
import com.example.antimarcipan.data.local.CollectionCard
import com.example.antimarcipan.data.local.CollectionDao
import com.example.antimarcipan.data.scryfall.ScryfallApi
import com.example.antimarcipan.data.scryfall.ScryfallCard
import com.example.antimarcipan.scan.OcrParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.Locale

/** Přeložená textace karty (do jazyka zvoleného v nastavení). */
data class CardTranslation(
    val name: String,
    val typeLine: String?,
    val text: String?,
    val lang: String,
    val fallbackToEnglish: Boolean,
)

/** Resolves scanned text into cards and manages the local collection. */
class CollectionRepository(
    private val dao: CollectionDao,
    private val appContext: Context,
    private val scryfall: ScryfallApi = Network.scryfall,
) {
    val cards: Flow<List<CollectionCard>> = dao.observeAll()
    val count: Flow<Int> = dao.observeCount()

    // Vlastní klient na stahování obrázků (bez Scryfall rate-limiteru).
    private val imageClient = OkHttpClient.Builder()
        .callTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    /**
     * Identifikuje kartu z OCR textu. Nejdřív najde kartu podle jména (fuzzy),
     * a pokud se z fotky podařilo přečíst edici (set kód + číslo) a sedí na
     * stejnou kartu, vrátí rovnou ten konkrétní tisk — tedy správnou edici.
     */
    /** Obnoví ceny všech uložených karet (nejvýš jednou za 12 h, pokud není [force]). */
    suspend fun refreshPrices(force: Boolean = false): Result<Int> = runCatching {
        val prefs = appContext.getSharedPreferences("prices", Context.MODE_PRIVATE)
        val last = prefs.getLong("last", 0L)
        if (!force && System.currentTimeMillis() - last < 12 * 3600_000L) return@runCatching 0
        val ids = dao.allIds()
        var n = 0
        ids.chunked(75).forEach { chunk ->
            val res = scryfall.collectionById(
                com.example.antimarcipan.data.scryfall.IdCollectionRequest(
                    chunk.map { com.example.antimarcipan.data.scryfall.IdIdentifier(it) }
                )
            )
            res.data.forEach { c -> dao.updatePrices(c.id, c.prices?.usd, c.prices?.eur); n++ }
            kotlinx.coroutines.delay(120)
        }
        prefs.edit().putLong("last", System.currentTimeMillis()).apply()
        n
    }

    /** Najde kartu podle levého spodního rohu (číslo + kód edice) — i pro cizojazyčné karty. */
    suspend fun lookupByCorner(rawText: String): Result<ScryfallCard> = runCatching {
        val (numbers, sets) = OcrParser.parseCorner(rawText)
        require(numbers.isNotEmpty() && sets.isNotEmpty()) { "V rohu není čitelné číslo a edice" }
        var tries = 0
        for (set in sets) for (n in numbers) {
            if (tries++ >= 8) break
            runCatching { scryfall.cardBySetAndNumber(set.lowercase(Locale.ROOT), n) }
                .getOrNull()?.let { return@runCatching it }
        }
        error("Kombinace edice a čísla nenalezena")
    }

    suspend fun lookup(rawText: String): Result<ScryfallCard> = runCatching {
        val parsed = OcrParser.parse(rawText)
        require(parsed.nameGuess.length >= 3) { "Text příliš krátký na jméno karty" }

        val byName = runCatching { scryfall.cardByFuzzyName(parsed.nameGuess) }.getOrElse { e ->
            // Cizojazyčná karta: jméno nenajdeme, zkusíme roh (edice + číslo)
            return@runCatching lookupByCorner(rawText).getOrElse { throw e }
        }

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

    /**
     * Přeloží textaci karty do zvoleného jazyka přes lokalizované tisky
     * Scryfallu (printed_name / printed_type_line / printed_text). Když karta
     * v daném jazyce nevyšla, vrátí anglický originál s příznakem fallbacku.
     */
    suspend fun translate(card: ScryfallCard, lang: String): Result<CardTranslation> = runCatching {
        // Čeština: Scryfall karty v češtině nemá → ukážeme originál (anglicky).
        if (lang == "en" || lang == "cs") {
            return@runCatching CardTranslation(
                name = card.name,
                typeLine = card.typeLine,
                text = card.oracleText,
                lang = "en",
                fallbackToEnglish = false,
            )
        }

        val set = card.set
        val number = card.collectorNumber
        val localized = if (set != null && number != null) {
            runCatching {
                scryfall.cardBySetNumberLang(set.lowercase(Locale.ROOT), number, lang)
            }.getOrNull()
        } else null

        val found = localized ?: runCatching {
            scryfall.search(
                query = "!\"${card.name}\" lang:$lang",
                order = "released",
                unique = "prints",
            ).data.firstOrNull()
        }.getOrNull()

        if (found != null && (found.printedName != null || found.printedText != null)) {
            CardTranslation(
                name = found.printedName ?: found.name,
                typeLine = found.printedTypeLine ?: found.typeLine,
                text = found.printedText ?: found.oracleText,
                lang = lang,
                fallbackToEnglish = false,
            )
        } else {
            CardTranslation(
                name = card.name,
                typeLine = card.typeLine,
                text = card.oracleText,
                lang = "en",
                fallbackToEnglish = true,
            )
        }
    }

    /** Oficiální rulings ke kartě (ze Scryfallu). */
    suspend fun rulings(scryfallId: String): Result<List<String>> = runCatching {
        scryfall.rulings(scryfallId).data.mapNotNull { r ->
            val c = r.comment ?: return@mapNotNull null
            if (r.publishedAt != null) "• $c  (${r.publishedAt})" else "• $c"
        }
    }

    /** Vyhledání karet podle jména (pro ruční přidání). */
    suspend fun searchByName(query: String): Result<List<ScryfallCard>> = runCatching {
        require(query.trim().length >= 2) { "Zadej aspoň 2 znaky" }
        scryfall.search(
            query = query.trim(),
            order = "name",
            unique = "cards",
        ).data
    }

    /** Všechny tisky (edice) dané karty — pro ruční výběr správné edice. */
    suspend fun printsOf(cardName: String): Result<List<ScryfallCard>> = runCatching {
        scryfall.search(
            query = "!\"$cardName\"",
            order = "released",
            unique = "prints",
        ).data
    }

    /** Stáhne základní zemi podle jména a přidá do sbírky; vrátí její id. */
    suspend fun addBasicLandToCollection(landName: String): String? = runCatching {
        val card = scryfall.cardByExactName(landName)
        val existing = dao.findById(card.id)
        if (existing == null) {
            val local = card.bestImageUrl?.let { downloadImage(it, card.id) }
            dao.upsert(card.toCollectionCard().copy(localImagePath = local))
        }
        card.id
    }.getOrNull()

    /** Nezařazené karty (draft / rozpracované, např. pool limited balíku). */
    val unsorted: Flow<List<CollectionCard>> = dao.observeUnsorted()

    /** Karty na wishlistu (chci je). */
    val wishlist: Flow<List<CollectionCard>> = dao.observeWishlist()

    suspend fun addToCollection(card: ScryfallCard) {
        val existing = dao.findById(card.id)
        when {
            existing == null -> {
                // Stáhni náhled lokálně, ať se zobrazuje spolehlivě i offline.
                val local = card.bestImageUrl?.let { downloadImage(it, card.id) }
                dao.upsert(card.toCollectionCard().copy(localImagePath = local, owned = true))
            }
            !existing.owned -> dao.setOwned(card.id, true)
            else -> dao.bumpQuantity(card.id, 1)
        }
    }

    /** Uloží kartu jako „nezařazenou“ (není ve sbírce ani na wishlistu) — např. pool draftu. */
    suspend fun ensureUnsorted(card: ScryfallCard) {
        val e = dao.findById(card.id)
        if (e != null) {
            if (!e.owned && !e.wishlist) dao.bumpQuantity(card.id, 1)
            return
        }
        val local = card.bestImageUrl?.let { downloadImage(it, card.id) }
        dao.upsert(card.toCollectionCard().copy(localImagePath = local, owned = false, wishlist = false, quantity = 1))
    }

    /** Přidá [qty] kusů karty do sbírky (nezařazená → vlastněná). */
    suspend fun addOwned(scryfallId: String, qty: Int) {
        val e = dao.findById(scryfallId) ?: return
        if (e.owned) dao.bumpQuantity(scryfallId, qty)
        else dao.upsert(e.copy(owned = true, quantity = qty))
    }

    suspend fun addToWishlist(card: ScryfallCard) {
        val existing = dao.findById(card.id)
        if (existing == null) {
            val local = card.bestImageUrl?.let { downloadImage(it, card.id) }
            dao.upsert(card.toCollectionCard().copy(localImagePath = local, owned = false, wishlist = true))
        } else {
            dao.setWishlist(card.id, true)
        }
    }

    suspend fun remove(scryfallId: String) {
        // Odebrání ze sbírky: když je i na wishlistu, jen zrušíme vlastnění.
        val existing = dao.findById(scryfallId)
        if (existing?.wishlist == true) {
            dao.setOwned(scryfallId, false)
        } else {
            File(appContext.filesDir, "card_images/$scryfallId.jpg").delete()
            dao.delete(scryfallId)
        }
    }

    /**
     * Změní tisk (edici) karty ve sbírce: nová karta převezme počet kusů
     * i příznaky, odkazy v balících se přepojí a starý záznam zmizí.
     */
    suspend fun changePrinting(old: CollectionCard, newCard: ScryfallCard) {
        if (newCard.id == old.scryfallId) return
        val existingNew = dao.findById(newCard.id)
        if (existingNew == null) {
            val local = newCard.bestImageUrl?.let { downloadImage(it, newCard.id) }
            dao.upsert(
                newCard.toCollectionCard().copy(
                    localImagePath = local,
                    quantity = old.quantity,
                    owned = old.owned,
                    wishlist = old.wishlist,
                    addedAt = old.addedAt,
                )
            )
        } else {
            dao.upsert(
                existingNew.copy(
                    quantity = existingNew.quantity + old.quantity,
                    owned = existingNew.owned || old.owned,
                    wishlist = existingNew.wishlist || old.wishlist,
                )
            )
        }
        dao.repointDeckCards(old.scryfallId, newCard.id)
        dao.deleteDeckRefs(old.scryfallId)
        dao.repointCommander(old.scryfallId, newCard.id)
        File(appContext.filesDir, "card_images/${old.scryfallId}.jpg").delete()
        dao.delete(old.scryfallId)
    }

    /** Označí kartu jako vlastněnou (přesun z wishlistu do sbírky). */
    suspend fun markOwned(scryfallId: String) = dao.setOwned(scryfallId, true)

    suspend fun removeFromWishlist(scryfallId: String) {
        val existing = dao.findById(scryfallId)
        if (existing?.owned == true) {
            dao.setWishlist(scryfallId, false)
        } else {
            File(appContext.filesDir, "card_images/$scryfallId.jpg").delete()
            dao.delete(scryfallId)
        }
    }

    /** Dostáhne chybějící náhledy u karet, které je ještě nemají lokálně. */
    suspend fun backfillImages() {
        val missing = dao.cardsWithoutLocalImage()
        for (c in missing) {
            val url = c.imageUrl ?: continue
            val path = downloadImage(url, c.scryfallId) ?: continue
            dao.setLocalImage(c.scryfallId, path)
        }
    }

    private suspend fun downloadImage(url: String, id: String): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(appContext.filesDir, "card_images").apply { mkdirs() }
                val out = File(dir, "$id.jpg")
                if (out.exists() && out.length() > 0) return@runCatching out.absolutePath
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Antimarcipan/0.1")
                    .build()
                imageClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@runCatching null
                    val body = resp.body ?: return@runCatching null
                    body.byteStream().use { input -> out.outputStream().use { input.copyTo(it) } }
                }
                out.absolutePath
            }.getOrNull()
        }

    private fun sameCard(a: String, b: String): Boolean {
        fun norm(s: String) = s.lowercase(Locale.ROOT).substringBefore("//").trim()
        return norm(a) == norm(b)
    }
}
