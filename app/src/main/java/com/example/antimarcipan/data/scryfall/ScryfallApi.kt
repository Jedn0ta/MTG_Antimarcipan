package com.example.antimarcipan.data.scryfall

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Scryfall REST API. No API key required.
 * Please respect the rate limit (~10 requests/second) — see ScryfallClient
 * for the enforced delay, and set a descriptive User-Agent.
 * Docs: https://scryfall.com/docs/api
 */
interface ScryfallApi {

    /**
     * Fuzzy name lookup — tolerant of OCR noise, partial names and typos.
     * Returns a single best-match card, or a 404 if ambiguous/not found.
     */
    @GET("cards/named")
    suspend fun cardByFuzzyName(
        @Query("fuzzy") fuzzy: String,
    ): ScryfallCard

    /** Exact name lookup. */
    @GET("cards/named")
    suspend fun cardByExactName(
        @Query("exact") exact: String,
    ): ScryfallCard

    /** Full-text search using Scryfall query syntax, e.g. "id<=WUB t:creature". */
    @GET("cards/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("order") order: String = "edhrec",
        @Query("unique") unique: String = "cards",
    ): ScryfallList

    /**
     * Exact printing by set code + collector number, e.g. cards/neo/123.
     * Umožní rozpoznat konkrétní edici karty z fotky.
     */
    @GET("cards/{set}/{number}")
    suspend fun cardBySetAndNumber(
        @Path("set") set: String,
        @Path("number") collectorNumber: String,
    ): ScryfallCard

    /**
     * Konkrétní tisk v daném jazyce (printed_name / printed_text), např.
     * cards/neo/123/ja. Vrátí 404, pokud karta v tom jazyce nevyšla.
     */
    @GET("cards/{set}/{number}/{lang}")
    suspend fun cardBySetNumberLang(
        @Path("set") set: String,
        @Path("number") collectorNumber: String,
        @Path("lang") lang: String,
    ): ScryfallCard

    /** Oficiální rulings (výklady pravidel) ke konkrétní kartě. */
    @GET("cards/{id}/rulings")
    suspend fun rulings(@Path("id") id: String): RulingList

    /** Hromadné dohledání karet podle jména (max. 75 na dotaz). */
    @POST("cards/collection")
    suspend fun collection(@Body body: CollectionRequest): ScryfallList
}

@Serializable
data class CollectionRequest(val identifiers: List<NameIdentifier>)

@Serializable
data class NameIdentifier(val name: String)
