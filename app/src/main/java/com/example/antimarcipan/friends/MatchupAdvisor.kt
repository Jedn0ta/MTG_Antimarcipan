package com.example.antimarcipan.friends

import com.example.antimarcipan.data.local.CollectionCard
import com.example.antimarcipan.decks.LimitedAdvisor

data class MatchupTip(val title: String, val text: String)

data class MatchupReport(
    val oppSummary: String,
    val tips: List<MatchupTip>,
    /** Karty z mé sbírky, které proti soupeři pomohou (s důvodem). */
    val sideboard: List<Pair<CollectionCard, String>>,
)

/** Rádce „jak hrát proti kamarádovu balíku“ — profil soupeře a karty z mé sbírky. */
object MatchupAdvisor {

    private fun txt(c: CollectionCard) = ((c.oracleText ?: "") + " " + c.keywords).lowercase()
    private fun has(c: CollectionCard, vararg w: String) = w.any { it in txt(c) }

    fun analyze(
        opp: List<Pair<CollectionCard, Int>>,
        mine: List<Pair<CollectionCard, Int>>,
        myCollection: List<CollectionCard>,
    ): MatchupReport {
        val oSpells = opp.filter { !LimitedAdvisor.isLand(it.first) }
        val n = oSpells.sumOf { it.second }.coerceAtLeast(1)
        fun count(p: (CollectionCard) -> Boolean) = oSpells.filter { p(it.first) }.sumOf { it.second }
        val creatures = count { LimitedAdvisor.isCreature(it) }
        val flyers = count { LimitedAdvisor.isCreature(it) && has(it, "flying") }
        val artifacts = count { (it.typeLine ?: "").contains("Artifact", true) }
        val enchants = count { (it.typeLine ?: "").contains("Enchantment", true) }
        val removal = count { LimitedAdvisor.isRemoval(it) }
        val counters = count { has(it, "counter target spell", "counter target") }
        val gy = count { has(it, "from your graveyard", "flashback", "escape", "dredge", "unearth", "reanimate", "return target creature card from your graveyard") }
        val tokens = count { has(it, "create a", "create two", "token") }
        val lifegain = count { has(it, "gain ", "lifelink") }
        val avg = oSpells.sumOf { (it.first.cmc ?: 0.0) * it.second } / n
        val colors = oSpells.flatMap { LimitedAdvisor.colors(it.first) }.toSet()

        val tips = mutableListOf<MatchupTip>()
        when {
            avg < 2.6 && creatures > n * 0.45 -> tips += MatchupTip("Agresivní balík", "Nízká křivka (Ø MV %.1f) a hodně tvorů — přežij první tahy: levní blokaři, lifegain, plošné odstranění malých tvorů. Nehraj pomalé karty brzy.".format(avg))
            avg > 3.6 -> tips += MatchupTip("Pomalý / rampový balík", "Vysoká křivka (Ø MV %.1f) — tlač tempo od začátku, ať nestihne zahrát drahé hrozby. Counterspelly a odstranění na velké hrozby.".format(avg))
            else -> tips += MatchupTip("Midrange", "Vyrovnaná křivka (Ø MV %.1f) — rozhodne výměna karet a hodnota. Šetři removal na nejlepší hrozby.".format(avg))
        }
        if (flyers >= 5) tips += MatchupTip("Hodně létajících ($flyers)", "Potřebuješ reach / flying blokaře nebo odstranění létajících.")
        if (removal >= 6) tips += MatchupTip("Hodně removalu ($removal)", "Nespoléhej na jednu velkou hrozbu; víc menších hrozeb, hexproof/ward, nebo karty s hodnotou při vstupu.")
        if (counters >= 4) tips += MatchupTip("Counterspelly ($counters)", "Hraj dvě kouzla za tah, nastražuj návnady, hrozby s flashem nebo „can't be countered“.")
        if (artifacts >= 8) tips += MatchupTip("Artefakty ($artifacts)", "Vyplatí se artifact removal.")
        if (enchants >= 6) tips += MatchupTip("Enchantmenty ($enchants)", "Vyplatí se enchantment removal.")
        if (gy >= 4) tips += MatchupTip("Hřbitov ($gy)", "Soupeř využívá hřbitov — graveyard hate ho výrazně oslabí.")
        if (tokens >= 6) tips += MatchupTip("Tokeny ($tokens)", "Plošné odstranění malých tvorů a blokaři s first strike.")
        if (lifegain >= 6) tips += MatchupTip("Lifegain ($lifegain)", "Počítej s delší hrou; „players can't gain life“ efekty pomůžou.")

        // Moje slabiny
        val mSpells = mine.filter { !LimitedAdvisor.isLand(it.first) }
        val myRemoval = mSpells.filter { LimitedAdvisor.isRemoval(it.first) }.sumOf { it.second }
        if (myRemoval < 4) tips += MatchupTip("Můj balík: málo removalu", "Máš jen $myRemoval removal — proti jeho hrozbám zvaž doplnění.")

        // Hate karty z mé sbírky
        val needs = mutableListOf<Pair<(CollectionCard) -> Boolean, String>>()
        if (flyers >= 5) needs += ({ c: CollectionCard -> LimitedAdvisor.isCreature(c) && has(c, "reach") } to "reach proti letcům")
        if (artifacts >= 8) needs += ({ c: CollectionCard -> has(c, "destroy target artifact", "exile target artifact", "destroy all artifacts") } to "artifact removal")
        if (enchants >= 6) needs += ({ c: CollectionCard -> has(c, "destroy target enchantment", "exile target enchantment", "destroy all enchantments") } to "enchantment removal")
        if (gy >= 4) needs += ({ c: CollectionCard -> has(c, "exile all cards from", "exile target card from a graveyard", "exile all graveyards", "exile each opponent's graveyard", "graveyard") && has(c, "exile") } to "graveyard hate")
        if (tokens >= 6 || (avg < 2.6 && creatures > n * 0.45)) needs += ({ c: CollectionCard -> has(c, "each creature", "all creatures") && has(c, "damage", "destroy", "-1/-1", "gets -") } to "plošné odstranění")
        if (counters >= 4) needs += ({ c: CollectionCard -> has(c, "can't be countered") } to "nejde counternout")
        if (removal >= 6) needs += ({ c: CollectionCard -> has(c, "hexproof", "ward") && LimitedAdvisor.isCreature(c) } to "odolná hrozba (hexproof/ward)")
        val myIds = mine.map { it.first.scryfallId }.toSet()
        val side = needs.flatMap { (p, why) ->
            myCollection.filter { it.owned && it.scryfallId !in myIds && p(it) }
                .sortedByDescending { LimitedAdvisor.rating(it) }.take(4).map { it to why }
        }.distinctBy { it.first.name }

        val summary = "${opp.sumOf { it.second }} karet · barvy ${colors.joinToString("").ifEmpty { "C" }} · tvorů $creatures · removal $removal · Ø MV %.1f".format(avg)
        return MatchupReport(summary, tips, side)
    }
}
