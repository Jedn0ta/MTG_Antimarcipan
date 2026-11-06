package com.example.antimarcipan.data.shop

import com.example.antimarcipan.data.Network
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.net.URLEncoder

sealed interface ShopResult {
    data class Found(val priceText: String, val note: String?) : ShopResult
    data object NotFound : ShopResult
    data class Error(val message: String) : ShopResult
}

/**
 * Orientační cena z obchodu Černý rytíř (cernyrytir.cz).
 *
 * Pozn.: Obchod nemá veřejné API. Načtení ceny je „best-effort" — parsuje
 * HTML výsledků hledání a hledá první částku v Kč. Když se struktura stránky
 * změní nebo se nic nenajde, UI vždy nabídne otevření hledání v prohlížeči.
 * Výkupní ceny mají na webu zvláštní sekci, proto je sem nepočítáme a
 * uživatele odkážeme na web.
 */
class CernyRytirRepository {

    /** URL vyhledávání jednotlivých MTG karet podle jména (pro prohlížeč). */
    fun searchUrl(cardName: String): String {
        val q = URLEncoder.encode(cardName, "UTF-8")
        return "https://www.cernyrytir.cz/index.php3?akce=3&limit=0&jmenokarty=$q&submit=Vyhledej"
    }

    suspend fun priceFor(cardName: String): ShopResult = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(searchUrl(cardName))
                .header("Accept-Language", "cs,en;q=0.8")
                .build()
            Network.httpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@runCatching ShopResult.Error("HTTP ${resp.code}")
                val html = resp.body?.string().orEmpty()
                parsePrice(html)
            }
        }.getOrElse { e -> ShopResult.Error(e.message ?: "Chyba sítě") }
    }

    /** Najde první částku ve formátu "123 Kč" / "123,-" apod. */
    private fun parsePrice(html: String): ShopResult {
        // Hrubý odhad: kus HTML bez tagů a hledání částky v Kč.
        val text = html.replace(Regex("<[^>]+>"), " ")
        val priceRe = Regex("""(\d[\d\s]{0,6})\s*(?:Kč|,-)""")
        val match = priceRe.find(text) ?: return ShopResult.NotFound
        val price = match.groupValues[1].replace(Regex("\\s"), "")
        if (price.isBlank()) return ShopResult.NotFound
        return ShopResult.Found(
            priceText = "$price Kč",
            note = "orientační prodejní cena · ověř na webu",
        )
    }
}
