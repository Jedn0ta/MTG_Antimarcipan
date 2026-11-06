package com.example.antimarcipan.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.antimarcipan.data.CollectionRepository
import com.example.antimarcipan.data.DeckRepository
import com.example.antimarcipan.data.local.Deck
import com.example.antimarcipan.data.scryfall.ScryfallCard
import com.example.antimarcipan.data.shop.CernyRytirRepository
import com.example.antimarcipan.data.shop.ShopResult
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
    val lastAdded: String? = null,
    /** Dostupné edice kandidáta (pro ruční přepnutí). */
    val prints: List<ScryfallCard> = emptyList(),
    val loadingPrints: Boolean = false,
    val shop: ShopResult? = null,
    val shopLoading: Boolean = false,
    val shopUrl: String? = null,
    val najadaUrl: String? = null,
    /** Zvýší se po každém úspěšném přidání — spouští potvrzovací animaci. */
    val confirmTick: Int = 0,
    val confirmText: String? = null,
    /** Multisken: skenuje víc karet po sobě, potvrzení hromadně. */
    val multiScan: Boolean = false,
    val pending: List<PendingScan> = emptyList(),
    /** Když != null, další naskenovaná karta nahradí tento slot (přenačtení). */
    val rescanSlot: Long? = null,
)

class ScanViewModel(
    private val repo: CollectionRepository,
    private val deckRepo: DeckRepository,
    private val shopRepo: CernyRytirRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ScanUiState())
    val state: StateFlow<ScanUiState> = _state.asStateFlow()

    val decks: StateFlow<List<Deck>> = deckRepo.decks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
            flashAdded(card, "Přidáno do sbírky: ${card.name}")
        }
    }

    fun addToDeck(deckId: Long) {
        val card = _state.value.candidate ?: return
        viewModelScope.launch {
            // Do balíku musí jít karta, kterou známe — ulož ji i do sbírky.
            repo.addToCollection(card)
            deckRepo.addCard(deckId, card.id)
            val deckName = deckRepo.deck(deckId)?.name ?: "balíku"
            flashAdded(card, "Přidáno do „$deckName“: ${card.name}")
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
        _state.value = _state.value.copy(candidate = card, status = editionLabel(card))
    }

    private fun loadShopPrice(name: String) {
        _state.value = _state.value.copy(shopLoading = true)
        viewModelScope.launch {
            val result = shopRepo.priceFor(name)
            // Jen pokud se mezitím nezměnil kandidát.
            if (_state.value.candidate?.name == name) {
                _state.value = _state.value.copy(shop = result, shopLoading = false)
            }
        }
    }

    fun dismissCandidate() {
        _state.value = _state.value.copy(
            candidate = null,
            status = "Namiř foťák na jméno karty",
            prints = emptyList(),
        )
        lastQuery = ""
    }

    private fun flashAdded(card: ScryfallCard, msg: String) {
        _state.value = _state.value.copy(
            candidate = null,
            lastAdded = card.name,
            status = msg,
            prints = emptyList(),
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
