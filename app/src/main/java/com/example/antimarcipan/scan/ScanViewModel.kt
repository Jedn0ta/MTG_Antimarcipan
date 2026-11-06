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
    val pending: List<PendingScan> = emptyList(),
    /** Když != null, další naskenovaná karta nahradí tento slot (přenačtení). */
    val rescanSlot: Long? = null,
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

    /** Zapne/vypne multisken (víc karet po sobě, potvrzení hromadně). */
    fun setMultiScan(on: Boolean) {
        _state.value = _state.value.copy(
            multiScan = on,
            candidate = null,
            prints = emptyList(),
            rescanSlot = null,
            status = if (on) "Multisken: skenuj karty jednu po druhé"
            else "Namiř foťák na jméno karty",
        )
        lastQuery = ""
    }

    /** Called by the camera analyzer with each OCR result. */
    fun onTextRecognized(text: String) {
        val firstLine = text.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.length >= 3 && it.any(Char::isLetter) } ?: return

        if (firstLine.equals(lastQuery, ignoreCase = true) || _state.value.looking) return
        lastQuery = firstLine

        if (_state.value.multiScan) {
            onMultiScanText(text, firstLine)
            return
        }

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

    private fun onMultiScanText(text: String, firstLine: String) {
        _state.value = _state.value.copy(
            recognizedText = firstLine,
            looking = true,
            status = "Hledám „$firstLine“…",
        )
        viewModelScope.launch {
            repo.lookup(text)
                .onSuccess { card ->
                    val st = _state.value
                    val rescan = st.rescanSlot
                    if (rescan != null) {
                        val updated = st.pending.map {
                            if (it.slotId == rescan) it.copy(card = card) else it
                        }
                        _state.value = st.copy(
                            pending = updated,
                            rescanSlot = null,
                            looking = false,
                            status = "Opraveno na „${card.name}“ — skenuj dál",
                        )
                    } else {
                        val slot = nextSlot++
                        val newList = st.pending + PendingScan(slot, card)
                        _state.value = st.copy(
                            pending = newList,
                            looking = false,
                            status = "K potvrzení: ${newList.size} — naposled „${card.name}“",
                        )
                    }
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        looking = false,
                        status = "Nenašlo se — zkus lepší světlo / úhel",
                    )
                }
        }
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
            status = "Přenačítám — namiř na správnou kartu",
        )
        lastQuery = ""
    }

    fun cancelRescan() {
        if (_state.value.rescanSlot == null) return
        _state.value = _state.value.copy(
            rescanSlot = null,
            status = "Multisken: skenuj karty jednu po druhé",
        )
    }

    /** Hromadně uloží všechny potvrzené karty do sbírky. */
    fun confirmAllToCollection() {
        val items = _state.value.pending
        if (items.isEmpty()) return
        viewModelScope.launch {
            items.forEach { repo.addToCollection(it.card) }
            _state.value = _state.value.copy(
                pending = emptyList(),
                rescanSlot = null,
                status = "Multisken: skenuj karty jednu po druhé",
                confirmTick = _state.value.confirmTick + 1,
                confirmText = "Uloženo ${items.size} karet do sbírky",
            )
            lastQuery = ""
        }
    }

    fun clearPending() {
        _state.value = _state.value.copy(pending = emptyList(), rescanSlot = null)
    }

    fun addToCollection() {
        val card = _state.value.candidate ?: return
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
            status = if (_state.value.multiScan) "Multisken: skenuj karty jednu po druhé"
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
