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
    /** Zvýší se po každém úspěšném přidání — spouští potvrzovací animaci. */
    val confirmTick: Int = 0,
    val confirmText: String? = null,
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

    /** Called by the camera analyzer with each OCR result. */
    fun onTextRecognized(text: String) {
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
                        shop = null,
                        shopUrl = shopRepo.searchUrl(card.name),
                    )
                    loadShopPrice(card.name)
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
