package com.example.antimarcipan.data

import com.example.antimarcipan.data.edhrec.EdhrecApi
import com.example.antimarcipan.data.edhrec.Slug
import com.example.antimarcipan.data.local.CollectionDao
import com.example.antimarcipan.data.scryfall.ScryfallApi

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
 * Pulls card recommendations from EDHREC (velitel) nebo ze Scryfallu (podle
 * barev a tématu balíku) a označí, které už hráč vlastní.
 */
class SynergyRepository(
    private val collectionDao: CollectionDao,
    private val edhrec: EdhrecApi = Network.edhrec,
    private val scryfall: ScryfallApi = Network.scryfall,
) {
    /** Návrhy karet ze Scryfallu podle barev balíku a tématu. */
    suspend fun suggestFromScryfall(
        colors: Set<String>,
        themeFragment: String?,
        exclude: Set<String>,
    ): Result<List<SynergyRecommendation>> = runCatching {
        val colorPart = if (colors.isNotEmpty())
            "id<=${colors.joinToString("").lowercase()}" else ""
        val query = listOfNotNull(
            colorPart.ifBlank { null },
            themeFragment,
            "-t:basic",
            "legal:commander",
        ).joinToString(" ")
        val owned = collectionDao.allNames().map { it.lowercase() }.toSet()
        val exclLower = exclude.map { it.lowercase() }.toSet()
        scryfall.search(query = query, order = "edhrec").data
            .asSequence()
            .map { it.name }
            .filter { it.lowercase() !in exclLower }
            .distinct()
            .take(40)
            .map { name ->
                SynergyRecommendation(
                    name = name,
                    synergy = 0.0,
                    numDecks = 0,
                    category = "Návrh (Scryfall)",
                    owned = owned.contains(name.lowercase()),
                )
            }
            .toList()
    }

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
