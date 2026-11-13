package com.example.antimarcipan.advisor

import com.example.antimarcipan.data.local.CollectionCard
import com.example.antimarcipan.decks.LimitedAdvisor
import kotlin.random.Random

data class SimResult(
    val runs: Int,
    /** Počet zemí v úvodní sedmičce: index 0..7 → podíl. */
    val landDist: DoubleArray,
    val keep7: Double,
    val keepBy6: Double,
    val keepBy5: Double,
    /** P(≥ k zemí do tahu k), k = 1..6 (na tahu prvním, lízání od 2. tahu). */
    val landDrops: List<Pair<Int, Double>>,
    /** P(první kouzlo zahratelné nejpozději v tahu t), t = 1..6. */
    val firstPlay: List<Pair<Int, Double>>,
    val avgFirstPlay: Double,
    /** P(mám zdroj barvy do 3. tahu). */
    val colorBy3: List<Pair<String, Double>>,
    val avgLands7: Double,
    val cheapInHand: Double,
)

/** Monte Carlo simulace úvodních rukou a prvních tahů (bez vizualizace). */
object HandSimulator {
    private fun isLand(c: CollectionCard) = LimitedAdvisor.isLand(c)
    private fun mv(c: CollectionCard) = (c.cmc ?: 0.0).toInt()

    /** Stejná heuristika jako u zkušební ruky. */
    fun keepable(hand: List<CollectionCard>): Boolean {
        val l = hand.count { isLand(it) }
        return l in 2..4 && hand.any { !isLand(it) && mv(it) <= 3 }
    }

    private fun sources(c: CollectionCard): Set<String> {
        if (!isLand(c)) return emptySet()
        val p = c.producedMana.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        if (p.isNotEmpty()) return p
        val n = c.name.lowercase()
        return buildSet {
            if ("plains" in n) add("W"); if ("island" in n) add("U"); if ("swamp" in n) add("B")
            if ("mountain" in n) add("R"); if ("forest" in n) add("G")
        }
    }

    fun run(library: List<CollectionCard>, runs: Int, seed: Long = System.nanoTime()): SimResult {
        val rnd = Random(seed)
        val deck = library.toMutableList()
        val colors = library.filter { !isLand(it) }.flatMap { LimitedAdvisor.colors(it) }.toSet().sorted()
        val landHist = IntArray(8)
        var keep7 = 0; var keep6 = 0; var keep5 = 0
        val dropHits = IntArray(7)
        val playBy = IntArray(7)
        var firstPlaySum = 0.0; var firstPlayCount = 0
        val colorHits = IntArray(colors.size)
        var landsSum = 0; var cheap = 0

        repeat(runs) {
            deck.shuffle(rnd)
            val hand = deck.subList(0, minOf(7, deck.size))
            val l7 = hand.count { isLand(it) }
            landHist[minOf(7, l7)]++; landsSum += l7
            if (hand.any { !isLand(it) && mv(it) <= 2 }) cheap++
            val k7 = keepable(hand)
            if (k7) keep7++
            // London mulligan: nová sedmička, vracíš karty dospod (zjednodušeně posuzujeme sedmičku)
            var k6 = k7
            if (!k6) { deck.shuffle(rnd); k6 = keepable(deck.subList(0, minOf(7, deck.size))) }
            if (k6) keep6++
            var k5 = k6
            if (!k5) { deck.shuffle(rnd); k5 = keepable(deck.subList(0, minOf(7, deck.size))) }
            if (k5) keep5++

            // Průběh tahů s posledně zamíchaným pořadím, na tahu první (lízání od tahu 2)
            deck.shuffle(rnd)
            var landsInPlay = 0
            var firstPlay = -1
            for (t in 1..6) {
                val seen = minOf(deck.size, 7 + (t - 1))
                val cards = deck.subList(0, seen)
                val landsSeen = cards.count { isLand(it) }
                landsInPlay = minOf(t, landsSeen)
                if (landsSeen >= t) dropHits[t]++
                if (firstPlay < 0 && cards.any { !isLand(it) && mv(it) in 1..landsInPlay }) firstPlay = t
                if (firstPlay in 1..t) playBy[t]++
                if (t == 3) {
                    val src = cards.filter { isLand(it) }.flatMap { sources(it) }.toSet()
                    colors.forEachIndexed { i, c -> if (c in src) colorHits[i]++ }
                }
            }
            if (firstPlay > 0) { firstPlaySum += firstPlay; firstPlayCount++ }
        }
        val r = runs.toDouble()
        return SimResult(
            runs = runs,
            landDist = DoubleArray(8) { landHist[it] / r },
            keep7 = keep7 / r, keepBy6 = keep6 / r, keepBy5 = keep5 / r,
            landDrops = (1..6).map { it to dropHits[it] / r },
            firstPlay = (1..6).map { it to playBy[it] / r },
            avgFirstPlay = if (firstPlayCount == 0) 0.0 else firstPlaySum / firstPlayCount,
            colorBy3 = colors.mapIndexed { i, c -> c to colorHits[i] / r },
            avgLands7 = landsSum / r,
            cheapInHand = cheap / r,
        )
    }

    /** Slovní vyhodnocení. */
    fun verdict(s: SimResult, deckSize: Int): List<String> = buildList {
        add("Průměrně %.1f zemí v úvodní ruce.".format(s.avgLands7))
        when {
            s.keep7 < 0.6 -> add("Jen %.0f %% rukou je hratelných na 7 — balík často mulliganuje. Uprav počet zemí nebo zlevni křivku.".format(s.keep7 * 100))
            s.keep7 < 0.75 -> add("%.0f %% rukou hratelných na 7 — průměr, dá se vylepšit.".format(s.keep7 * 100))
            else -> add("%.0f %% rukou hratelných na 7 — výborná stabilita.".format(s.keep7 * 100))
        }
        val screw = s.landDist[0] + s.landDist[1]
        val flood = (5..7).sumOf { s.landDist[it] }
        if (screw > 0.2) add("%.0f %% rukou má 0–1 zemi (málo zemí) — zvaž přidat země.".format(screw * 100))
        if (flood > 0.2) add("%.0f %% rukou má 5+ zemí (moc zemí) — zvaž ubrat země nebo přidat dobírání.".format(flood * 100))
        s.landDrops.firstOrNull { it.first == 4 }?.let {
            if (it.second < 0.6) add("Čtvrtou zemi do 4. tahu trefíš jen v %.0f %% her.".format(it.second * 100))
        }
        s.colorBy3.filter { it.second < 0.75 }.forEach {
            add("Zdroj barvy ${it.first} do 3. tahu jen v %.0f %% her — přidej zdroje této barvy.".format(it.second * 100))
        }
        if (s.cheapInHand < 0.5) add("Kouzlo za 1–2 many má v ruce jen %.0f %% rukou — pomalý start.".format(s.cheapInHand * 100))
        add("První kouzlo zahraješ průměrně v %.1f. tahu.".format(s.avgFirstPlay))
    }
}
