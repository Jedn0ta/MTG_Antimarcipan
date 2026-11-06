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
 * Obchod nemá veřejné API, takže se parsuje HTML stránky s výsledky hledání.
 * Je to „best-effort": hledáme cenu v blízkosti jména karty a dostupnost.
 * Když se struktura stránky změní nebo se nic nenajde, UI vždy nabídne
 * otevření hledání v prohlížeči. Výkupní ceny mají na webu zvláštní sekci.
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
                parse(html, cardName)
            }
        }.getOrElse { e -> ShopResult.Error(e.message ?: "Chyba sítě") }
    }

    /**
     * Najde cenu u řádku s danou kartou. Nejprve zkusí okno textu za jménem
     * karty, teprve pak první cenu na stránce jako nouzové řešení.
     */
    private fun parse(html: String, cardName: String): ShopResult {
        val text = html
            .replace(Regex("(?is)<script.*?</script>"), " ")
            .replace(Regex("(?is)<style.*?</style>"), " ")
            .replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ")
            .replace(Regex("\\s+"), " ")

        // Cena: "123 Kč", "1 234 Kč", "123,-", "1.234,-"
        val priceRe = Regex("""(\d[\d .]{0,8}\d|\d)\s*(?:Kč|,-)""")

        val nameIdx = text.indexOf(cardName, ignoreCase = true)
        val searchRegion = if (nameIdx >= 0) {
            text.substring(nameIdx, minOf(text.length, nameIdx + 400))
        } else {
            text
        }

        val match = priceRe.find(searchRegion) ?: priceRe.find(text) ?: return ShopResult.NotFound
        val price = match.groupValues[1].replace(Regex("[ .]"), "")
        if (price.isBlank()) return ShopResult.NotFound

        val available = Regex("(?i)skladem|naskladněno|dostupné").containsMatchIn(searchRegion)
        val note = buildString {
            append("orientačně · ")
            append(if (available) "skladem" else "ověř dostupnost na webu")
        }
        return ShopResult.Found("$price Kč", note)
    }
}
