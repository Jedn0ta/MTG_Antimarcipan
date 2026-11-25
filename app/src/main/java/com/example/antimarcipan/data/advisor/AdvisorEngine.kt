package com.example.antimarcipan.data.advisor

import com.example.antimarcipan.i18n.tr

import com.example.antimarcipan.data.colorIdentityList
import com.example.antimarcipan.data.local.CollectionCard

enum class TipLevel { GOOD, WARN, TIP }

data class AdvisorTip(
    val title: String,
    val detail: String,
    val level: TipLevel,
)

/**
 * "Grimoar" — a rules-based advisor. It inspects a deck's composition
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
                    tr("Prázdný balík"),
                    tr("Přidej karty do balíku a Grimoar ti řekne, co doladit."),
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
                tr("Málo zemí ({0})", landCount),
                tr("Cílíš kolem {0} zemí. S tímhle počtem budeš často bez many — přidej {1} zemí nebo ramp.", TARGET_LANDS, (TARGET_LANDS - landCount)),
                TipLevel.WARN,
            )
            landCount > TARGET_LANDS + 4 -> AdvisorTip(
                tr("Hodně zemí ({0})", landCount),
                tr("Můžeš pár zemí vyměnit za ramp nebo card draw, ať neflodíš prázdné tahy."),
                TipLevel.WARN,
            )
            else -> AdvisorTip(
                tr("Zem v pohodě ({0})", landCount),
                tr("Počet zemí sedí kolem doporučených {0}.", TARGET_LANDS),
                TipLevel.GOOD,
            )
        }

        // 2) Ramp
        val ramp = nonLands.count { it.isRamp() }
        tips += healthTip(
            count = ramp, target = TARGET_RAMP, label = "ramp",
            lowMsg = tr("Jen {0} zdrojů rampu. Přidej mana rocky/dorky (cíl ~{1}), ať rozjedeš velitele dřív.", ramp, TARGET_RAMP),
            okMsg = tr("Rampu máš dost ({0}).", ramp),
        )

        // 3) Card draw
        val draw = nonLands.count { it.drawsCards() }
        tips += healthTip(
            count = draw, target = TARGET_DRAW, label = "card draw",
            lowMsg = tr("Málo dobírání ({0}). Bez karet dojdeš dechem — cíl je ~{1} zdrojů.", draw, TARGET_DRAW),
            okMsg = tr("Dobírání karet je OK ({0}).", draw),
        )

        // 4) Removal / interaction
        val removal = nonLands.count { it.isRemoval() }
        tips += healthTip(
            count = removal, target = TARGET_REMOVAL, label = "removal",
            lowMsg = tr("Jen {0} removalů. V multiplayeru potřebuješ odpovědi — přidej spoty a board wipy (cíl ~{1}).", removal, TARGET_REMOVAL),
            okMsg = tr("Removalu máš rozumně ({0}).", removal),
        )

        // 5) Mana curve
        val avgCmc = nonLands.filter { (it.cmc ?: 0.0) > 0 }
            .map { it.cmc ?: 0.0 }
            .average()
            .takeIf { !it.isNaN() } ?: 0.0
        tips += if (avgCmc > 3.6) AdvisorTip(
            tr("Těžká křivka (prům. CMC %.1f)").format(avgCmc),
            tr("Balík je drahý. Přidej levné karty (1–2 many) nebo víc rampu, ať nejsi v early game bezbranný."),
            TipLevel.WARN,
        ) else AdvisorTip(
            tr("Křivka v pohodě (prům. CMC %.1f)").format(avgCmc),
            tr("Rozložení ceny karet je zdravé."),
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
                    tr("V Commanderu nesmí být karty mimo barevnou identitu velitele. Zkontroluj: {0}{1}.", (offColor.take(5).joinToString { it.name }), (if (offColor.size > 5) "…" else "")),
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
        if (count < target - 2) AdvisorTip(tr("Málo: {0} ({1})", label, count), lowMsg, TipLevel.WARN)
        else AdvisorTip("$label OK ($count)", okMsg, TipLevel.GOOD)

    private fun inGameTips(
        landCount: Int, total: Int, commander: CollectionCard?,
    ): AdvisorTip {
        val idealOpener = when {
            landCount <= 34 -> tr("2–4 zemí")
            landCount >= 40 -> tr("3–5 zemí")
            else -> tr("3–4 zemí")
        }
        val cmdLine = commander?.let { tr(" Veliteli ({0}) nachystej ramp, ať ho zahraješ včas.", (it.name)) } ?: ""
        return AdvisorTip(
            tr("Rádce Grimoaru radí při hře"),
            tr("Mulligan drž ruku s {0} a aspoň jedním raným playem. Sekvencuj: nejdřív ramp, pak card draw, interakci si drž na klíčové hrozby.{1}", idealOpener, cmdLine),
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
