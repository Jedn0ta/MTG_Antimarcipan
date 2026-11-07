package com.example.antimarcipan.scan

import android.content.Context
import android.net.Uri
import com.example.antimarcipan.data.CollectionRepository
import com.example.antimarcipan.data.scryfall.ScryfallCard
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.coroutines.suspendCoroutine

/**
 * Rozpozná všechny karty na fotce: přečte text v plném rozlišení, rozdělí řádky,
 * které OCR spojilo přes víc karet vedle sebe, a každý věrohodný úsek zkusí
 * dohledat na Scryfallu. Přijme jen výsledky, jejichž jméno sedí s přečteným textem.
 */
class PhotoCardRecognizer(private val repo: CollectionRepository) {

    private data class Seg(val text: String, val height: Int, val top: Int, val left: Int)

    suspend fun recognize(
        context: Context,
        uri: Uri,
        onProgress: (done: Int, total: Int, found: Int) -> Unit,
    ): List<ScryfallCard> {
        val segs = readSegments(context, uri)
        val candidates = candidates(segs)
        if (candidates.isEmpty()) return emptyList()
        // Každý jedinečný text se hledá jen jednou, výsledek platí pro všechny výskyty.
        val unique = candidates.distinctBy { key(it) }
        var done = 0
        var found = 0
        val sem = Semaphore(3)
        val byKey = coroutineScope {
            unique.map { name ->
                async {
                    sem.withPermit {
                        val card = repo.lookup(name).getOrNull()?.takeIf { similarName(name, it.name) }
                        synchronized(this@PhotoCardRecognizer) {
                            done++; if (card != null) found++
                            onProgress(done, unique.size, found)
                        }
                        key(name) to card
                    }
                }
            }.map { it.await() }.toMap()
        }
        // Stejná karta na různých místech fotky = víc kusů.
        return candidates.mapNotNull { byKey[key(it)] }
    }

    private fun key(s: String) = s.lowercase().filter { it.isLetter() }

    private suspend fun readSegments(context: Context, uri: Uri): List<Seg> = suspendCoroutine { cont ->
        val image = InputImage.fromFilePath(context, uri)
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            .process(image)
            .addOnSuccessListener { r ->
                val out = mutableListOf<Seg>()
                r.textBlocks.forEach { b ->
                    b.lines.forEach { line ->
                        val h = line.boundingBox?.height() ?: 0
                        // Rozdělení řádku podle velkých mezer mezi slovy (dvě karty vedle sebe).
                        var cur = mutableListOf<com.google.mlkit.vision.text.Text.Element>()
                        var lastRight: Int? = null
                        fun flush() {
                            if (cur.isNotEmpty()) {
                                val box = cur.first().boundingBox
                                out += Seg(cur.joinToString(" ") { it.text }, h, box?.top ?: 0, box?.left ?: 0)
                            }
                            cur = mutableListOf()
                        }
                        line.elements.forEach { el ->
                            val l = el.boundingBox?.left
                            if (lastRight != null && l != null && l - lastRight!! > h * 1.6) flush()
                            cur += el
                            lastRight = el.boundingBox?.right
                        }
                        flush()
                    }
                }
                cont.resumeWith(Result.success(out))
            }
            .addOnFailureListener { cont.resumeWith(Result.failure(it)) }
    }

    private fun candidates(segs: List<Seg>): List<String> {
        val ok = segs.filter { plausible(it.text) }
        if (ok.isEmpty()) return emptyList()
        // Pořadí: větší písmo (názvy) první, pak zleva doprava, shora dolů.
        val kept = mutableListOf<Seg>()
        ok.sortedWith(compareByDescending<Seg> { it.height }.thenBy { it.top }.thenBy { it.left })
            .forEach { s ->
                val k = key(s.text)
                if (k.length < 3) return@forEach
                // stejný text na (skoro) stejném místě = tentýž výskyt
                val dup = kept.any { key(it.text) == k &&
                    kotlin.math.abs(it.top - s.top) < s.height * 3 && kotlin.math.abs(it.left - s.left) < s.height * 6 }
                if (!dup) kept += s
            }
        return kept.take(100).map { it.text.trim().trim('·', '-', '—', '"', '\'', '.', ',') }
    }

    private fun plausible(raw: String): Boolean {
        val name = raw.trim().trim('·', '-', '—', '"', '\'', '.', ',')
        if (name.length < 3 || name.length > 40) return false
        val letters = name.count { it.isLetter() }
        if (letters < 3 || name.count { it.isDigit() } > letters) return false
        if (!name.first().isLetter() || !name.first().isUpperCase()) return false
        val low = name.lowercase()
        if (TYPE_HINTS.any { low.startsWith(it) }) return false
        // pravidlový text obvykle obsahuje tyto slovní spojení
        if (RULE_WORDS.any { " $it " in " $low " }) return false
        return true
    }

    companion object {
        private val TYPE_HINTS = listOf(
            "creature", "instant", "sorcery", "enchantment", "artifact", "land", "planeswalker",
            "legendary", "basic", "tribal", "kindred", "battle", "token", "illus", "™", "©",
        )
        private val RULE_WORDS = listOf(
            "target", "when", "whenever", "you", "your", "each", "until", "this", "draw", "deals",
            "damage", "control", "may", "put", "into", "onto", "from", "the battlefield",
        )

        fun similarName(query: String, found: String): Boolean {
            val q = query.lowercase().filter { it.isLetter() }
            val f = found.lowercase().substringBefore(" //").filter { it.isLetter() }
            if (q.length < 3 || f.isEmpty()) return false
            if (f == q || f.startsWith(q) && q.length >= f.length * 0.6 || q.startsWith(f)) return true
            val d = IntArray(f.length + 1) { it }
            for (i in 1..q.length) {
                var prev = d[0]; d[0] = i
                for (j in 1..f.length) {
                    val tmp = d[j]
                    d[j] = minOf(d[j] + 1, d[j - 1] + 1, prev + if (q[i - 1] == f[j - 1]) 0 else 1)
                    prev = tmp
                }
            }
            return d[f.length] <= maxOf(2, f.length / 4)
        }
    }
}
