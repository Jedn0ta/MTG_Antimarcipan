package com.example.antimarcipan.data

import com.example.antimarcipan.data.edhrec.EdhrecApi
import com.example.antimarcipan.data.edhrec.Slug
import com.example.antimarcipan.data.local.CollectionDao

/** One suggested card for a commander, with its EDHREC synergy score. */
data class SynergyRecommendation(
    val name: String,
    val synergy: Double,
    val numDecks: Int,
    val category: String,
    /** True if this card is already in the user's local collection. */
    val owned: Boolean,
)

/**
 * Pulls card recommendations for a commander from EDHREC and flags which ones
 * the user already owns, so they can see what to slot in for better synergy.
 */
class SynergyRepository(
    private val collectionDao: CollectionDao,
    private val edhrec: EdhrecApi = Network.edhrec,
) {
    suspend fun recommendationsFor(commanderName: String): Result<List<SynergyRecommendation>> =
        runCatching {
            val slug = Slug.commanderSlug(commanderName)
            val page = edhrec.commanderPage(slug)

            val owned = collectionDao.allNames().map { it.lowercase() }.toSet()

            val lists = page.container?.jsonDict?.cardlists.orEmpty()
            lists.flatMap { list ->
                list.cardviews.mapNotNull { cv ->
                    val name = cv.name ?: return@mapNotNull null
                    SynergyRecommendation(
                        name = name,
                        synergy = cv.synergy ?: 0.0,
                        numDecks = cv.numDecks ?: 0,
                        category = list.header ?: "Cards",
                        owned = owned.contains(name.lowercase()),
                    )
                }
            }
                .distinctBy { it.name }
                .sortedByDescending { it.synergy }
        }
}
