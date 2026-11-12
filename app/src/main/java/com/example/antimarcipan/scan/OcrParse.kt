package com.example.antimarcipan.scan

/** Výsledek rozboru OCR textu z karty. */
data class OcrParse(
    val nameGuess: String,
    val collectorNumber: String?,
    val setCode: String?,
)

/**
 * Z OCR textu celé karty vytáhne odhad jména (horní řádek) a — u moderních
 * karet (cca od 2015) — sběratelské číslo a kód edice ze spodního okraje.
 *
 * Spodní řádky moderní karty vypadají typicky takto:
 *   123/264 C
 *   NEO • EN • Jméno Autora
 */
object OcrParser {

    // Slova, která nejsou kódy edic, i když vypadají jako 3písmenné tokeny.
    private val STOPWORDS = setOf(
        "THE", "AND", "FOR", "YOU", "ADD", "END", "EACH", "WHEN", "THIS",
        "WITH", "INTO", "FROM", "THAN", "THEN", "EN", "DE", "FR", "IT", "ES",
        "PT", "JP", "KO", "RU", "ZH",
    )

    fun parse(raw: String): OcrParse {
        val lines = raw.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()

        val name = lines.firstOrNull { it.length >= 3 && it.any(Char::isLetter) }
            ?.let { clean(it) }
            ?: clean(raw)

        val collector = findCollectorNumber(lines)
        val set = findSetCode(lines)

        return OcrParse(nameGuess = name, collectorNumber = collector, setCode = set)
    }

    /** Hledá vzor "123/264" a vrací levou část (číslo karty). */
    private fun findCollectorNumber(lines: List<String>): String? {
        val re = Regex("""\b(\d{1,4})\s*/\s*\d{1,4}\b""")
        for (line in lines) {
            re.find(line)?.let { return it.groupValues[1].trimStart('0').ifEmpty { "0" } }
        }
        return null
    }

    /** Hledá pravděpodobný 3–5písmenný kód edice na spodních řádcích. */
    private fun findSetCode(lines: List<String>): String? {
        // Edice bývá dole; projdeme řádky od konce.
        val tokenRe = Regex("""\b([A-Z0-9]{3,5})\b""")
        for (line in lines.asReversed()) {
            for (m in tokenRe.findAll(line)) {
                val tok = m.groupValues[1]
                if (tok in STOPWORDS) continue
                if (tok.all { it.isDigit() }) continue   // čistě číselné nejsou kódy
                return tok
            }
        }
        return null
    }

    private val LANGS = setOf("EN", "DE", "FR", "IT", "ES", "PT", "JP", "JA", "KO", "RU", "ZH", "ZHS", "ZHT", "CS", "PH")

    /**
     * Rozbor levého spodního rohu karty (funguje pro karty v jakémkoli jazyce):
     *   „123/280 R“ nebo „0123 R“  → sběratelské číslo
     *   „DMU • EN“                  → kód edice
     * Vrátí kandidáty seřazené od nejpravděpodobnějších.
     */
    fun parseCorner(raw: String): Pair<List<String>, List<String>> {
        val lines = raw.lineSequence().map { it.trim().uppercase() }.filter { it.isNotEmpty() }.toList()
        val numbers = LinkedHashSet<String>()
        val sets = LinkedHashSet<String>()
        val slash = Regex("""\b(\d{1,4})\s*/\s*\d{1,4}\b""")
        val numRarity = Regex("""(?:^|\s)(\d{1,4}[A-Z]?)\s*[CURMLSTP]?\b""")
        val rarityNum = Regex("""\b[CURMLSTP]\s+(\d{1,4})\b""")
        val setLang = Regex("""\b([A-Z0-9]{3,5})\s*[•·*.,:\-]+\s*([A-Z]{2,3})\b""")
        lines.forEach { l -> slash.find(l)?.let { numbers += it.groupValues[1].trimStart('0').ifEmpty { "0" } } }
        lines.forEach { l -> rarityNum.find(l)?.let { numbers += it.groupValues[1].trimStart('0').ifEmpty { "0" } } }
        lines.forEach { l ->
            numRarity.findAll(l).forEach { m ->
                val n = m.groupValues[1]
                if (n.any { it.isDigit() }) numbers += n.trimStart('0').ifEmpty { "0" }.lowercase()
            }
        }
        lines.forEach { l ->
            setLang.findAll(l).forEach { m ->
                val s = m.groupValues[1]; val lang = m.groupValues[2]
                if (lang in LANGS && s.any { it.isLetter() } && s !in STOPWORDS) sets += s
            }
        }
        // Záložně: samostatné 3–5znakové tokeny s písmenem
        lines.forEach { l ->
            Regex("""\b([A-Z][A-Z0-9]{2,4})\b""").findAll(l).forEach { m ->
                val s = m.groupValues[1]
                if (s !in STOPWORDS && s !in LANGS) sets += s
            }
        }
        return numbers.take(3) to sets.take(4)
    }

    private fun clean(s: String): String =
        s.replace(Regex("[^\\p{L}\\p{N} ,'\\-]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
}
