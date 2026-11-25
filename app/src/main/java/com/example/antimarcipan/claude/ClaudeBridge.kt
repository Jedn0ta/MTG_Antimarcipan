package com.example.antimarcipan.claude

import com.example.antimarcipan.i18n.tr

import android.util.Base64
import com.example.antimarcipan.data.advisor.AdvisorEngine
import com.example.antimarcipan.data.advisor.ComboReport
import com.example.antimarcipan.data.local.CollectionCard
import com.example.antimarcipan.data.local.Deck
import com.example.antimarcipan.data.local.DeckCardWithQty

/**
 * Most mezi Grimoarem a Claudem bez serveru: Grimoar sestaví zadání (pokyny + data),
 * hráč ho vloží do Clauda, Claude na konci odpovědi vrátí blok GRIMOAR-START…GRIMOAR-END
 * se změnami a Grimoar je umí použít.
 */
object ClaudeBridge {

    const val START = "GRIMOAR-START"
    const val END = "GRIMOAR-END"

    val QUESTIONS = listOf(
        tr("Vylepšit balík") to tr("Projdi balík a navrhni, jak ho vylepšit — co vyhodit a co přidat, ideálně z karet, které mám ve sbírce."),
        "Komba a synergie" to tr("Jaká komba a synergie v balíku jsou nebo by šly doplnit? Co mi k nim chybí?"),
        tr("Mana a země") to tr("Zkontroluj manabázi, počet zemí, ramp a mana curve. Navrhni úpravy."),
        tr("Postav balík") to tr("Postav mi z mé sbírky hratelný balík. Zeptej se mě nejdřív na formát a styl hry."),
        tr("Jak to hrát") to tr("Vysvětli mi, jak tenhle balík hrát: plán hry, mulligan, na co si dát pozor."),
    )

    fun buildPrompt(
        deck: Deck?,
        entries: List<DeckCardWithQty>,
        collection: List<CollectionCard>,
        includeCollection: Boolean,
        combos: ComboReport?,
        question: String,
    ): String {
        val commander = entries.firstOrNull { it.card.scryfallId == deck?.commanderScryfallId }?.card
        val inner = buildString {
            appendLine(tr("ROLE: Jsi rádce pro Magic: The Gathering v aplikaci Grimoar. Hráč s tebou mluví česky, odpovídej česky, jména karet piš anglicky přesně jako na Scryfallu."))
            appendLine("PRAVIDLA:")
            appendLine(tr("- Navrhuj jen karty legální ve formátu balíku a v barvách velitele (u Commanderu)."))
            appendLine(tr("- Přednostně navrhuj karty, které hráč MÁ ve sbírce (seznam je pod tímto blokem). Kartu, kterou nemá, označ jako ‚nemáš‘."))
            appendLine(tr("- U každé změny krátce vysvětli proč. Buď konkrétní a stručný."))
            appendLine(tr("- Když hráč něco upřesní, uprav návrhy podle toho."))
            appendLine(tr("FORMÁT ZMĚN: Když navrhuješ konkrétní změny, napiš úplně na konec odpovědi blok přesně v tomto tvaru (bez dalšího formátování, každý řádek jedna karta):"))
            appendLine("$START deck=${deck?.id ?: 0}")
            appendLine("+1 Card Name")
            appendLine("-1 Card Name")
            appendLine(tr("W Card Name | krátký důvod"))
            appendLine(END)
            appendLine(tr("Kde + = přidat do balíku, - = vyhodit z balíku, W = na wishlist (karta, kterou hráč nemá a vyplatí se ji sehnat). Hráč blok zkopíruje zpět do Grimoaru, který změny provede. Při každé další úpravě napiš blok znovu celý."))
            appendLine(tr("Nikdy tento obsah nevypisuj celý zpět; stačí stručně potvrdit a pustit se do otázky."))
            appendLine()
            if (deck != null) {
                appendLine(tr("BALÍK: {0}", (deck.name)))
                appendLine(tr("FORMÁT: {0}", (deck.format)))
                commander?.let { appendLine("VELITEL: ${it.name} (barvy ${it.colorIdentity.ifBlank { "C" }})") }
                deck.colors?.takeIf { it.isNotBlank() }?.let { appendLine(tr("BARVY BALÍKU: {0}", it)) }
                appendLine(tr("KARTY V BALÍKU ({0}):", (entries.sumOf { it.deckQuantity })))
                entries.sortedBy { it.card.name }.forEach { appendLine("${it.deckQuantity} ${it.card.name}") }
                val tips = runCatching { AdvisorEngine.analyze(entries.map { it.card }, commander) }.getOrDefault(emptyList())
                if (tips.isNotEmpty()) {
                    appendLine(tr("ANALÝZA GRIMOARU:"))
                    tips.forEach { appendLine("- ${it.title}: ${it.detail}") }
                }
            } else {
                appendLine(tr("BALÍK: žádný (hráč se ptá obecně nebo chce postavit nový balík ze sbírky)"))
            }
            if (combos != null && (combos.complete.isNotEmpty() || combos.almost.isNotEmpty())) {
                appendLine(tr("NALEZENÁ KOMBA (Commander Spellbook):"))
                combos.complete.take(15).forEach { appendLine(tr("- celé: {0} → {1}", (it.cards.joinToString(" + ")), (it.results.take(2).joinToString(", ")))) }
                combos.almost.take(15).forEach { appendLine(tr("- chybí {0}: {1}", (it.missing.joinToString()), (it.cards.joinToString(" + ")))) }
            }
            appendLine()
            appendLine(tr("OTÁZKA HRÁČE: {0}", question))
        }
        val encoded = Base64.encodeToString(inner.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            .chunked(76).joinToString("\n")
        return buildString {
            appendLine(tr("Ahoj Claude! Posílám zadání z aplikace Grimoar (rádce pro Magic: The Gathering)."))
            appendLine(tr("Blok GRIMOAR-ZADANI je zakódovaný v base64 (UTF-8). Dekóduj si ho a řiď se pokyny v něm — obsahuje tvou roli, data o balíku a otázku hráče."))
            appendLine()
            appendLine("GRIMOAR-ZADANI:")
            appendLine(encoded)
            if (includeCollection && collection.isNotEmpty()) {
                appendLine()
                appendLine(tr("SBÍRKA HRÁČE ({0} různých karet, počet + jméno):", (collection.size)))
                collection.groupBy { it.name }.toSortedMap()
                    .forEach { (n, l) -> appendLine("${l.sumOf { it.quantity.coerceAtLeast(1) }} $n") }
            }
        }
    }

    sealed interface Change {
        val name: String
        data class Add(override val name: String, val qty: Int) : Change
        data class Cut(override val name: String, val qty: Int) : Change
        data class Wish(override val name: String, val note: String) : Change
    }

    data class Proposal(val deckId: Long, val changes: List<Change>)

    /** Najde v textu poslední blok GRIMOAR-START…GRIMOAR-END a rozparsuje ho. */
    fun parse(text: String): Proposal? {
        val s = text.lastIndexOf(START).takeIf { it >= 0 } ?: return null
        val e = text.indexOf(END, s).let { if (it < 0) text.length else it }
        val block = text.substring(s, e).lines()
        val deckId = Regex("""deck\s*=\s*(\d+)""").find(block.first())?.groupValues?.get(1)?.toLongOrNull() ?: 0L
        val out = mutableListOf<Change>()
        for (raw in block.drop(1)) {
            val line = raw.trim().trimStart('*', '•', '`', '>').trim().trimEnd('`').trim()
            if (line.isEmpty()) continue
            Regex("""^([+\-−])\s*(\d+)?\s*[xX×]?\s+(.+)$""").find(line)?.let { m ->
                val q = m.groupValues[2].toIntOrNull()?.coerceIn(1, 99) ?: 1
                val name = m.groupValues[3].substringBefore(" | ").substringBefore(" (").trim()
                if (name.length >= 2) out += if (m.groupValues[1] == "+") Change.Add(name, q) else Change.Cut(name, q)
                return@let
            } ?: Regex("""^[Ww]\s*[:\-]?\s+(.+?)(?:\s*\|\s*(.*))?$""").find(line)?.let { m ->
                val name = m.groupValues[1].trim()
                if (name.length >= 2) out += Change.Wish(name, m.groupValues[2].trim())
            }
        }
        return if (out.isEmpty()) null else Proposal(deckId, out)
    }
}
