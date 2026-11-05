package com.example.antimarcipan.data.scryfall

import retrofit2.http.GET
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
    ): ScryfallList
}
