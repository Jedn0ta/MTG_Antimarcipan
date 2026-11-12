package com.example.antimarcipan.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.antimarcipan.data.CardTranslation
import com.example.antimarcipan.data.CollectionRepository
import com.example.antimarcipan.data.DeckRepository
import com.example.antimarcipan.data.local.Deck
import com.example.antimarcipan.data.scryfall.ScryfallCard
import com.example.antimarcipan.data.settings.AppSettings
import com.example.antimarcipan.data.settings.TranslationLang
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Jedna naskenovaná karta čekající na potvrzení v multiskenu. */
data class PendingScan(
    val slotId: Long,
    val card: ScryfallCard,
)

/** Začátky řádků, které nejsou názvem karty (typová řádka apod.). */
private val TYPE_LINE_HINTS = listOf(
    "creature", "instant", "sorcery", "artifact", "enchantment", "planeswalker",
    "land", "basic land", "legendary", "token", "battle",
)

data class ScanUiState(
    val recognizedText: String = "",
    val candidate: ScryfallCard? = null,
    val status: String = "Namiř foťák na jméno karty",
    val looking: Boolean = false,
    /** Dostupné edice kandidáta (pro ruční přepnutí). */
    val prints: List<ScryfallCard> = emptyList(),
    val loadingPrints: Boolean = false,
    /** Zvýší se po každém úspěšném přidání — spouští potvrzovací animaci. */
    val confirmTick: Int = 0,
    val confirmText: String? = null,
    /** Multisken: skenuje víc karet po sobě, potvrzení hromadně. */
    val multiScan: Boolean = false,
    /** Hledání podle levého spodního rohu (číslo + edice) — pro cizojazyčné karty. */
    val cornerMode: Boolean = false,
    val pending: List<PendingScan> = emptyList(),
    /** Když != null, další naskenovaná karta nahradí tento slot (přenačtení). */
    val rescanSlot: Long? = null,
    /** Probíhá hromadné sejmutí stolu. */
    val capturing: Boolean = false,
    /** Překlad textace aktuálního kandidáta do zvoleného jazyka. */
    val translation: CardTranslation? = null,
    val translating: Boolean = false,
)

class ScanViewModel(
    private val repo: CollectionRepository,
    private val deckRepo: DeckRepository,
    private val settings: AppSettings,
) : ViewModel() {

    private val _state = MutableStateFlow(ScanUiState())
    val state: StateFlow<ScanUiState> = _state.asStateFlow()

    val decks: StateFlow<List<Deck>> = deckRepo.decks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Aktuálně zvolený cílový jazyk překladu (z nastavení). */
    val translationLang: StateFlow<TranslationLang> = settings.translationLang

    private var lastQuery: String = ""
    private var nextSlot: Long = 1

    /** Poslední řádky z OCR (pro multisken „sejmi stůl“). */
    @Volatile private var latestLines: List<String> = emptyList()

    /** Zapne/vypne multisken (víc rozprostřených karet, sejmutí stolu). */
    fun setCornerMode(on: Boolean) {
        lastQuery = ""
        _state.value = _state.value.copy(
            multiScan = false,
            cornerMode = on,
            candidate = null,
            prints = emptyList(),
            status = if (on) "Namiř na levý spodní roh karty — číslo a kód edice (např. 123/280 · DMU • DE)"
            else "Namiř foťák na jméno karty",
        )
    }

    fun setMultiScan(on: Boolean) {
        _state.value = _state.value.copy(
            cornerMode = false,
            multiScan = on,
            candidate = null,
            prints = emptyList(),
            rescanSlot = null,
            status = if (on) "Rozlož karty na stůl a vyfoť je"
            else "Namiř foťák na jméno karty",
        )
        lastQuery = ""
        latestLines = emptyList()
    }

    /** Průběžně ukládá rozpoznané řádky (využije se při sejmutí stolu). */
    fun onFrameLines(lines: List<String>) {
        if (_state.value.multiScan) latestLines = lines
    }

    /** Called by the camera analyzer with each OCR result. */
    fun onTextRecognized(text: String) {
        // V multiskenu nic nepřidáváme automaticky — čeká se na „Sejmout stůl“.
        if (_state.value.multiScan) return
        // Nalezená karta zůstává na obrazovce, dokud s ní uživatel něco neudělá.
        if (_state.value.candidate != null) return
        if (_state.value.cornerMode) { onCornerText(text); return }

        val firstLine = text.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.length >= 3 && it.any(Char::isLetter) } ?: return

        if (firstLine.equals(lastQuery, ignoreCase = true) || _state.value.looking) return
        lastQuery = firstLine

        _state.value = _state.value.copy(
            recognizedText = firstLine,
            looking = true,
            status = "Hledám „$firstLine“…",
            prints = emptyList(),
        )

        viewModelScope.launch {
            repo.lookup(text)
                .onSuccess { card ->
                    _state.value = _state.value.copy(
                        candidate = card,
                        looking = false,
                        status = editionLabel(card),
                        translation = null,
                        translating = false,
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        candidate = null,
                        looking = false,
                        status = "Nenašlo se — zkus lepší světlo / úhel",
                    )
                }
        }
    }

    private fun onCornerText(text: String) {
        val (numbers, sets) = OcrParser.parseCorner(text)
        if (numbers.isEmpty() || sets.isEmpty() || _state.value.looking) return
        val key = numbers.first() + "|" + sets.first()
        if (key == lastQuery) return
        lastQuery = key
        _state.value = _state.value.copy(
            looking = true,
            status = "Hledám ${sets.first()} #${numbers.first()}…",
            prints = emptyList(),
        )
        viewModelScope.launch {
            repo.lookupByCorner(text)
                .onSuccess { card ->
                    _state.value = _state.value.copy(candidate = card, looking = false, status = editionLabel(card))
                }
                .onFailure {
                    _state.value = _state.value.copy(looking = false, status = "Roh nečitelný — přibliž a přisviť (číslo + kód edice)")
                }
        }
    }

    /** Přeloží textaci kandidáta do jazyka zvoleného v nastavení. */
    fun translateCandidate() {
        val card = _state.value.candidate ?: return
        if (_state.value.translating) return
        val lang = settings.translationLang.value.code
        _state.value = _state.value.copy(translating = true)
        viewModelScope.launch {
            repo.translate(card, lang)
                .onSuccess { tr -> _state.value = _state.value.copy(translation = tr, translating = false) }
                .onFailure { _state.value = _state.value.copy(translating = false) }
        }
    }

    fun hideTranslation() {
        _state.value = _state.value.copy(translation = null)
    }

    /**
     * Multisken z fotky: rozpozná text na celé fotce (v plném rozlišení),
     * vybere řádky s názvy karet (velké písmo nahoře karty) a vyhledá je.
     */
    fun processPhoto(context: android.content.Context, uri: android.net.Uri) {
        if (_state.value.capturing) return
        _state.value = _state.value.copy(capturing = true, status = "Fotka pořízena · čtu text…")
        viewModelScope.launch {
            val cards = runCatching {
                PhotoCardRecognizer(repo).recognize(context, uri) { d, t, f ->
                    _state.value = _state.value.copy(status = "Hledám karty $d/$t… (nalezeno $f)")
                }
            }.getOrElse {
                _state.value = _state.value.copy(capturing = false, status = "Fotku se nepodařilo načíst — zkus to znovu")
                return@launch
            }
            val rescan = _state.value.rescanSlot
            if (rescan != null && cards.isNotEmpty()) {
                val card = cards.first()
                _state.value = _state.value.copy(
                    pending = _state.value.pending.map { if (it.slotId == rescan) it.copy(card = card) else it },
                    rescanSlot = null, capturing = false, status = "Opraveno na „${card.name}“",
                )
                return@launch
            }
            // Každá nalezená karta jednou (podle tisku); stejné jméno víckrát = víc kusů.
            val unique = cards
            unique.forEach { c -> _state.value = _state.value.copy(pending = _state.value.pending + PendingScan(nextSlot++, c)) }
            _state.value = _state.value.copy(
                capturing = false,
                rescanSlot = null,
                status = if (unique.isNotEmpty()) "Hotovo: rozpoznáno ${unique.size} karet · celkem ${_state.value.pending.size} k potvrzení"
                else "Žádnou kartu se nepodařilo rozpoznat — zkus ostřejší fotku",
            )
        }
    }

    /** Z řádků OCR vybere věrohodné názvy karet (horní řádek každé karty). */
    private fun candidateNames(lines: List<String>): List<String> {
        val seen = LinkedHashSet<String>()
        for (raw in lines) {
            val name = raw.trim().trim('·', '-', '—', '"', '\'', '.', ',')
            if (name.length < 3 || name.length > 40) continue
            val letters = name.count { it.isLetter() }
            if (letters < 3) continue
            // Hodně číslic / symbolů → nejspíš řádek s cenou/kolekcí, přeskoč.
            if (name.count { it.isDigit() } > letters) continue
            // Musí začínat písmenem (názvy karet začínají velkým písmenem).
            if (!name.first().isLetter()) continue
            // Přeskoč zjevné typové řádky a klíčová slova.
            val low = name.lowercase()
            if (TYPE_LINE_HINTS.any { low.startsWith(it) }) continue
            seen += name
            if (seen.size >= 40) break
        }
        return seen.toList()
    }

    fun removePending(slotId: Long) {
        _state.value = _state.value.copy(
            pending = _state.value.pending.filterNot { it.slotId == slotId },
            rescanSlot = if (_state.value.rescanSlot == slotId) null else _state.value.rescanSlot,
        )
    }

    /** Označí slot k přenačtení — další naskenovaná karta ho nahradí. */
    fun rescanPending(slotId: Long) {
        _state.value = _state.value.copy(
            rescanSlot = slotId,
            status = "Přenačítám — vyfoť správnou kartu",
        )
        lastQuery = ""
    }

    fun cancelRescan() {
        if (_state.value.rescanSlot == null) return
        _state.value = _state.value.copy(
            rescanSlot = null,
            status = "Rozlož karty na stůl a vyfoť je",
        )
    }

    /** Hromadně uloží všechny potvrzené karty do sbírky. */
    fun confirmAllToCollection() {
        val items = _state.value.pending
        if (items.isEmpty()) return
        PoolScanTarget.target.value?.let { t ->
            viewModelScope.launch {
                items.forEach { repo.ensureUnsorted(it.card); deckRepo.addToPool(t.deckId, it.card.id) }
                finishBatch(items.size, "Do poolu „${t.deckName}“: ${items.size} karet")
            }
            return
        }
        viewModelScope.launch {
            items.forEach { repo.addToCollection(it.card) }
            finishBatch(items.size, "Uloženo ${items.size} karet do sbírky")
        }
    }

    /** Hromadně přidá všechny potvrzené karty na wishlist. */
    fun confirmAllToWishlist() {
        val items = _state.value.pending
        if (items.isEmpty()) return
        viewModelScope.launch {
            items.forEach { repo.addToWishlist(it.card) }
            finishBatch(items.size, "Na wishlist: ${items.size} karet")
        }
    }

    /** Hromadně přidá všechny potvrzené karty do balíku (i do sbírky). */
    fun confirmAllToDeck(deckId: Long) {
        val items = _state.value.pending
        if (items.isEmpty()) return
        viewModelScope.launch {
            items.forEach {
                repo.addToCollection(it.card)
                deckRepo.addCard(deckId, it.card.id)
            }
            val deckName = deckRepo.deck(deckId)?.name ?: "balíku"
            finishBatch(items.size, "Do „$deckName“: ${items.size} karet")
        }
    }

    private fun finishBatch(count: Int, message: String) {
        _state.value = _state.value.copy(
            pending = emptyList(),
            rescanSlot = null,
            status = "Rozlož karty na stůl a vyfoť je",
            confirmTick = _state.value.confirmTick + 1,
            confirmText = message,
        )
        lastQuery = ""
    }

    fun clearPending() {
        _state.value = _state.value.copy(pending = emptyList(), rescanSlot = null)
    }

    fun addToCollection() {
        val card = _state.value.candidate ?: return
        PoolScanTarget.target.value?.let { t ->
            viewModelScope.launch {
                repo.ensureUnsorted(card)
                deckRepo.addToPool(t.deckId, card.id)
                flashAdded("Do poolu: ${card.name}")
            }
            return
        }
        viewModelScope.launch {
            repo.addToCollection(card)
            flashAdded("Přidáno do sbírky: ${card.name}")
        }
    }

    fun addToDeck(deckId: Long) {
        val card = _state.value.candidate ?: return
        viewModelScope.launch {
            // Do balíku musí jít karta, kterou známe — ulož ji i do sbírky.
            repo.addToCollection(card)
            deckRepo.addCard(deckId, card.id)
            val deckName = deckRepo.deck(deckId)?.name ?: "balíku"
            flashAdded("Přidáno do „$deckName“: ${card.name}")
        }
    }

    fun addToPlayground() {
        val card = _state.value.candidate ?: return
        viewModelScope.launch {
            repo.addToPlayground(card)
            flashAdded("Do playgroundu: ${card.name}")
        }
    }

    fun addToWishlist() {
        val card = _state.value.candidate ?: return
        viewModelScope.launch {
            repo.addToWishlist(card)
            flashAdded("Na wishlist: ${card.name}")
        }
    }

    /** Načte dostupné edice aktuálního kandidáta. */
    fun loadPrints() {
        val card = _state.value.candidate ?: return
        if (_state.value.prints.isNotEmpty() || _state.value.loadingPrints) return
        _state.value = _state.value.copy(loadingPrints = true)
        viewModelScope.launch {
            repo.printsOf(card.name)
                .onSuccess { list ->
                    _state.value = _state.value.copy(prints = list, loadingPrints = false)
                }
                .onFailure {
                    _state.value = _state.value.copy(loadingPrints = false)
                }
        }
    }

    fun selectPrint(card: ScryfallCard) {
        _state.value = _state.value.copy(
            candidate = card,
            status = editionLabel(card),
            translation = null,
            translating = false,
        )
    }

    fun dismissCandidate() {
        _state.value = _state.value.copy(
            candidate = null,
            status = if (_state.value.multiScan) "Rozlož karty na stůl a vyfoť je"
            else "Namiř foťák na jméno karty",
            prints = emptyList(),
            translation = null,
            translating = false,
        )
        lastQuery = ""
    }

    private fun flashAdded(msg: String) {
        _state.value = _state.value.copy(
            candidate = null,
            status = msg,
            prints = emptyList(),
            translation = null,
            translating = false,
            confirmTick = _state.value.confirmTick + 1,
            confirmText = msg,
        )
        lastQuery = ""
    }

    private fun editionLabel(card: ScryfallCard): String {
        val set = card.setName ?: card.set?.uppercase() ?: "?"
        return "${card.name} · $set"
    }
}
