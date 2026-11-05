package com.example.antimarcipan.data.edhrec

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Unofficial EDHREC JSON endpoint used by community tools. There is no public
 * API contract, so treat this as best-effort and tolerate schema drift.
 *
 * Example: https://json.edhrec.com/pages/commanders/atraxa-praetors-voice.json
 *
 * The slug is the commander name, lowercased, apostrophes/commas removed and
 * spaces turned into hyphens — see Slug.commanderSlug().
 */
interface EdhrecApi {

    @GET("pages/commanders/{slug}.json")
    suspend fun commanderPage(
        @Path("slug") slug: String,
    ): EdhrecPage
}

@Serializable
data class EdhrecPage(
    val container: EdhrecContainer? = null,
)

@Serializable
data class EdhrecContainer(
    @SerialName("json_dict") val jsonDict: EdhrecJsonDict? = null,
)

@Serializable
data class EdhrecJsonDict(
    val cardlists: List<EdhrecCardList> = emptyList(),
)

@Serializable
data class EdhrecCardList(
    val header: String? = null,
    val tag: String? = null,
    val cardviews: List<EdhrecCardView> = emptyList(),
)

@Serializable
data class EdhrecCardView(
    val name: String? = null,
    val sanitized: String? = null,
    /** How well this card synergises with the commander (−1.0 … 1.0). */
    val synergy: Double? = null,
    @SerialName("num_decks") val numDecks: Int? = null,
    @SerialName("potential_decks") val potentialDecks: Int? = null,
)
