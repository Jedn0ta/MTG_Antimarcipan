package com.example.antimarcipan.data.scryfall

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Minimal subset of the Scryfall card object.
 * Full schema: https://scryfall.com/docs/api/cards
 */
@Serializable
data class ScryfallCard(
    val id: String,
    val name: String,
    @SerialName("mana_cost") val manaCost: String? = null,
    val cmc: Double? = null,
    @SerialName("type_line") val typeLine: String? = null,
    @SerialName("oracle_text") val oracleText: String? = null,
    @SerialName("printed_name") val printedName: String? = null,
    @SerialName("printed_type_line") val printedTypeLine: String? = null,
    @SerialName("printed_text") val printedText: String? = null,
    @SerialName("color_identity") val colorIdentity: List<String> = emptyList(),
    @SerialName("produced_mana") val producedMana: List<String> = emptyList(),
    val keywords: List<String> = emptyList(),
    val set: String? = null,
    @SerialName("set_name") val setName: String? = null,
    @SerialName("collector_number") val collectorNumber: String? = null,
    val rarity: String? = null,
    val lang: String? = null,
    @SerialName("image_uris") val imageUris: ImageUris? = null,
    @SerialName("card_faces") val cardFaces: List<CardFace> = emptyList(),
    val prices: Prices? = null,
    @SerialName("scryfall_uri") val scryfallUri: String? = null,
    @SerialName("related_uris") val relatedUris: RelatedUris? = null,
) {
    /** Best available normal-size image, falling back to the front face. */
    val bestImageUrl: String?
        get() = imageUris?.normal
            ?: cardFaces.firstOrNull()?.imageUris?.normal
            ?: imageUris?.small
}

@Serializable
data class CardFace(
    val name: String? = null,
    @SerialName("mana_cost") val manaCost: String? = null,
    @SerialName("type_line") val typeLine: String? = null,
    @SerialName("oracle_text") val oracleText: String? = null,
    @SerialName("image_uris") val imageUris: ImageUris? = null,
)

@Serializable
data class ImageUris(
    val small: String? = null,
    val normal: String? = null,
    @SerialName("art_crop") val artCrop: String? = null,
)

@Serializable
data class RelatedUris(
    val gatherer: String? = null,
)

@Serializable
data class Prices(
    val usd: String? = null,
    @SerialName("usd_foil") val usdFoil: String? = null,
    val eur: String? = null,
)

/** Response wrapper for /cards/search (a list of cards). */
@Serializable
data class ScryfallList(
    val data: List<ScryfallCard> = emptyList(),
    @SerialName("total_cards") val totalCards: Int = 0,
    @SerialName("has_more") val hasMore: Boolean = false,
)

@Serializable
data class RulingList(
    val data: List<Ruling> = emptyList(),
)

@Serializable
data class Ruling(
    val source: String? = null,
    @SerialName("published_at") val publishedAt: String? = null,
    val comment: String? = null,
)
