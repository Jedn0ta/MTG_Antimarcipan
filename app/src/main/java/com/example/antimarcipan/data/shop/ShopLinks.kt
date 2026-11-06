package com.example.antimarcipan.data.shop

import java.net.URLEncoder

/**
 * Odkazy do českých MTG obchodů s vyhledáním konkrétní karty podle jména.
 *
 * Pozn.: formáty vyhledávacích URL nejde ověřit z vývojového prostředí
 * (weby obchodů nejsou dostupné). Jsou to nejlepší odhady podle struktury
 * webů; pokud některý odkaz nevede přímo na kartu, stačí upravit zde.
 */
object ShopLinks {

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    /** Černý rytíř — vyhledání karty v jednotlivkách. */
    fun cernyRytir(cardName: String): String =
        "https://www.cernyrytir.cz/index.php3?akce=3&limit=0&jmenokarty=${enc(cardName)}&submit=Vyhledej"

    /** Najáda (najada.games) — fulltext vyhledání karty. */
    fun najada(cardName: String): String =
        "https://www.najada.games/vyhledavani?q=${enc(cardName)}"
}
