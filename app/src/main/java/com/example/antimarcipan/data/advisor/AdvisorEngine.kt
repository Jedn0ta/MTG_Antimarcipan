package com.example.antimarcipan.data.advisor

import com.example.antimarcipan.data.colorIdentityList
import com.example.antimarcipan.data.local.CollectionCard

enum class TipLevel { GOOD, WARN, TIP }

data class AdvisorTip(
    val title: String,
    val detail: String,
    val level: TipLevel,
)

/**
 * "Anti Marcipán" — a rules-based advisor. It inspects a deck's composition
 * and the commander, then produces tips you can use while playing: deck-health
 * warnings (too few lands, no ramp, thin on removal) plus in-game guidance
 * (mulligan targets, sequencing). Fully offline, no network needed.
 *
 * Heuristics are tuned for 100-card Commander; tweak the constants for other
 * formats.
 */
object AdvisorEngine {

    // Commander deck targets (rough community consensus).
    private const val TARGET_LANDS = 37
    private const val TARGET_RAMP = 10
    private const val TARGET_DRAW = 10
    private const val TARGET_REMOVAL = 8

    fun analyze(
        deck: List<CollectionCard>,
        commander: CollectionCard?,
    ): List<AdvisorTip> {
        if (deck.isEmpty()) {
            return listOf(
                AdvisorTip(
                    "Prázdný balík",
                    "Přidej karty do balíku a Anti Marcipán ti řekne, co doladit.",
                    TipLevel.TIP,
                )
            )
        }

        val tips = mutableListOf<AdvisorTip>()
        val total = deck.sumOf { it.quantity }

        val lands = deck.filter { it.isLand() }
        val nonLands = deck.filter { !it.isLand() }
        val landCount = lands.sumOf { it.quantity }

        // 1) Land count
        tips += when {
            landCount < TARGET_LANDS - 3 -> AdvisorTip(
                "Málo zemí ($landCount)",
                "Cílíš kolem $TARGET_LANDS zemí. S tímhle počtem budeš často bez many — přidej ${TARGET_LANDS - landCount} zemí nebo ramp.",
                TipLevel.WARN,
            )
            landCount > TARGET_LANDS + 4 -> AdvisorTip(
                "Hodně zemí ($landCount)",
                "Můžeš pár zemí vyměnit za ramp nebo card draw, ať neflodíš prázdné tahy.",
                TipLevel.WARN,
            )
            else -> AdvisorTip(
                "Zem v pohodě ($landCount)",
                "Počet zemí sedí kolem doporučených $TARGET_LANDS.",
                TipLevel.GOOD,
            )
        }

        // 2) Ramp
        val ramp = nonLands.count { it.isRamp() }
        tips += healthTip(
            count = ramp, target = TARGET_RAMP, label = "ramp",
            lowMsg = "Jen $ramp zdrojů rampu. Přidej mana rocky/dorky (cíl ~$TARGET_RAMP), ať rozjedeš velitele dřív.",
            okMsg = "Rampu máš dost ($ramp).",
        )

        // 3) Card draw
        val draw = nonLands.count { it.drawsCards() }
        tips += healthTip(
            count = draw, target = TARGET_DRAW, label = "card draw",
            lowMsg = "Málo dobírání ($draw). Bez karet dojdeš dechem — cíl je ~$TARGET_DRAW zdrojů.",
            okMsg = "Dobírání karet je OK ($draw).",
        )

        // 4) Removal / interaction
        val removal = nonLands.count { it.isRemoval() }
        tips += healthTip(
            count = removal, target = TARGET_REMOVAL, label = "removal",
            lowMsg = "Jen $removal removalů. V multiplayeru potřebuješ odpovědi — přidej spoty a board wipy (cíl ~$TARGET_REMOVAL).",
            okMsg = "Removalu máš rozumně ($removal).",
        )

        // 5) Mana curve
        val avgCmc = nonLands.filter { (it.cmc ?: 0.0) > 0 }
            .map { it.cmc ?: 0.0 }
            .average()
            .takeIf { !it.isNaN() } ?: 0.0
        tips += if (avgCmc > 3.6) AdvisorTip(
            "Těžká křivka (prům. CMC %.1f)".format(avgCmc),
            "Balík je drahý. Přidej levné karty (1–2 many) nebo víc rampu, ať nejsi v early game bezbranný.",
            TipLevel.WARN,
        ) else AdvisorTip(
            "Křivka v pohodě (prům. CMC %.1f)".format(avgCmc),
            "Rozložení ceny karet je zdravé.",
            TipLevel.GOOD,
        )

        // 6) Color identity vs commander
        if (commander != null) {
            val ci = commander.colorIdentityList().toSet()
            val offColor = deck.filter { card ->
                card.colorIdentityList().any { it !in ci }
            }
            if (offColor.isNotEmpty()) {
                tips += AdvisorTip(
                    "Karty mimo barvu velitele (${offColor.size})",
                    "V Commanderu nesmí být karty mimo barevnou identitu velitele. Zkontroluj: ${offColor.take(5).joinToString { it.name }}${if (offColor.size > 5) "…" else ""}.",
                    TipLevel.WARN,
                )
            }
        }

        // 7) In-game guidance (always shown — this is the "advisor" voice)
        tips += inGameTips(landCount, total, commander)

        return tips
    }

    private fun healthTip(
        count: Int, target: Int, label: String, lowMsg: String, okMsg: String,
    ): AdvisorTip =
        if (count < target - 2) AdvisorTip("Málo: $label ($count)", lowMsg, TipLevel.WARN)
        else AdvisorTip("$label OK ($count)", okMsg, TipLevel.GOOD)

    private fun inGameTips(
        landCount: Int, total: Int, commander: CollectionCard?,
    ): AdvisorTip {
        val idealOpener = when {
            landCount <= 34 -> "2–4 zemí"
            landCount >= 40 -> "3–5 zemí"
            else -> "3–4 zemí"
        }
        val cmdLine = commander?.let { " Veliteli (${it.name}) nachystej ramp, ať ho zahraješ včas." } ?: ""
        return AdvisorTip(
            "Anti Marcipán radí při hře",
            "Mulligan drž ruku s $idealOpener a aspoň jedním raným playem. Sekvencuj: nejdřív ramp, pak card draw, interakci si drž na klíčové hrozby.$cmdLine",
            TipLevel.TIP,
        )
    }
}

// --- Card classification helpers (heuristics over type line + oracle text) ---

private fun CollectionCard.isLand(): Boolean =
    typeLine?.contains("Land", ignoreCase = true) == true

private fun CollectionCard.oracle(): String = oracleText?.lowercase() ?: ""

private fun CollectionCard.isRamp(): Boolean {
    val o = oracle()
    val t = typeLine?.lowercase() ?: ""
    // Mana rocks/dorks and land ramp.
    return ("add {" in o && ("artifact" in t || "creature" in t)) ||
        "search your library for a" in o && "land" in o && "onto the battlefield" in o
}

private fun CollectionCard.drawsCards(): Boolean {
    val o = oracle()
    return "draw a card" in o || "draw two" in o || "draw three" in o || "draw cards" in o
}

private fun CollectionCard.isRemoval(): Boolean {
    val o = oracle()
    return "destroy target" in o || "exile target" in o ||
        "destroy all" in o || "each player sacrifices" in o ||
        ("deal" in o && "damage to target" in o)
}
