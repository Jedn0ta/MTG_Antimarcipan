package com.example.antimarcipan.data.advisor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class Combo(
    val id: String,
    val cards: List<String>,
    val results: List<String>,
    val steps: String,
    /** Karty kombinace, které ve sbírce chybí. */
    val missing: List<String>,
)

data class ComboReport(val complete: List<Combo>, val almost: List<Combo>)

/**
 * Hledá komba mezi kartami ve sbírce přes veřejnou databázi Commander Spellbook
 * (endpoint find-my-combos). Vrací komba, která máš celá, a komba, kterým chybí jedna karta.
 */
object ComboFinder {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { ch -> ch.proceed(ch.request().newBuilder().header("User-Agent", "Antimarcipan/0.1 (personal hobby app)").build()) }
        .build()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun find(cardNames: Collection<String>): Result<ComboReport> = withContext(Dispatchers.IO) {
        runCatching {
            val owned = cardNames.map { it.substringBefore(" //").trim() }.filter { it.isNotEmpty() }.distinct()
            val body = buildJsonObject {
                put("commanders", buildJsonArray { })
                put("main", buildJsonArray { owned.forEach { add(buildJsonObject { put("card", it); put("quantity", 1) }) } })
            }.toString()
            val req = Request.Builder()
                .url("https://backend.commanderspellbook.com/find-my-combos")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()
            val text = client.newCall(req).execute().use { r ->
                if (!r.isSuccessful) error("Commander Spellbook odpověděl ${r.code}")
                r.body?.string() ?: error("Prázdná odpověď")
            }
            val root = json.parseToJsonElement(text) as JsonObject
            val results = (root["results"] as? JsonObject) ?: root
            val ownedSet = owned.map { it.lowercase() }.toSet()
            fun parse(key: String): List<Combo> =
                ((results[key] as? JsonArray) ?: JsonArray(emptyList())).mapNotNull { parseVariant(it, ownedSet) }
            ComboReport(
                complete = parse("included"),
                almost = parse("almostIncluded").filter { it.missing.size == 1 },
            )
        }
    }

    private fun JsonElement.str(vararg path: String): String? {
        var e: JsonElement? = this
        for (p in path) e = (e as? JsonObject)?.get(p)
        return runCatching { e?.jsonPrimitive?.contentOrNull }.getOrNull()
    }

    private fun parseVariant(v: JsonElement, owned: Set<String>): Combo? {
        val o = v as? JsonObject ?: return null
        val cards = ((o["uses"] as? JsonArray) ?: return null).mapNotNull { it.str("card", "name") }
        if (cards.isEmpty()) return null
        val produces = ((o["produces"] as? JsonArray) ?: JsonArray(emptyList()))
            .mapNotNull { it.str("feature", "name") ?: it.str("name") }
        return Combo(
            id = o.str("id") ?: cards.joinToString("+"),
            cards = cards,
            results = produces,
            steps = o.str("description") ?: "",
            missing = cards.filter { it.lowercase() !in owned && it.substringBefore(" //").lowercase() !in owned },
        )
    }
}
