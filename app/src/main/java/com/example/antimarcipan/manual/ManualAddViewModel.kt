package com.example.antimarcipan.manual

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.antimarcipan.data.CollectionRepository
import com.example.antimarcipan.data.DeckRepository
import com.example.antimarcipan.data.local.Deck
import com.example.antimarcipan.data.scryfall.ScryfallCard
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ManualAddUiState(
    val query: String = "",
    val results: List<ScryfallCard> = emptyList(),
    val loading: Boolean = false,
    val message: String? = null,
)

/**
 * Ruční vyhledání a přidání karty — pro karty, které nejdou naskenovat
 * (fullarty, promo, tokeny apod.). Hledá na Scryfallu podle jména/textu.
 */
class ManualAddViewModel(
    private val repo: CollectionRepository,
    private val deckRepo: DeckRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ManualAddUiState())
    val state: StateFlow<ManualAddUiState> = _state.asStateFlow()

    val decks: StateFlow<List<Deck>> = deckRepo.decks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var searchJob: Job? = null

    fun setQuery(q: String) {
        _state.value = _state.value.copy(query = q)
        searchJob?.cancel()
        if (q.trim().length < 2) {
            _state.value = _state.value.copy(results = emptyList(), loading = false)
            return
        }
        searchJob = viewModelScope.launch {
            delay(350) // debounce
            _state.value = _state.value.copy(loading = true, message = null)
            repo.searchByName(q)
                .onSuccess { list ->
                    _state.value = _state.value.copy(
                        results = list,
                        loading = false,
                        message = if (list.isEmpty()) "Nic nenalezeno" else null,
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        results = emptyList(),
                        loading = false,
                        message = "Nenalezeno / chyba hledání",
                    )
                }
        }
    }

    fun addToCollection(card: ScryfallCard, onDone: (String) -> Unit) {
        viewModelScope.launch {
            repo.addToCollection(card)
            onDone("Přidáno do sbírky: ${card.name}")
        }
    }

    fun addToDeck(card: ScryfallCard, deckId: Long, onDone: (String) -> Unit) {
        viewModelScope.launch {
            repo.addToCollection(card)
            deckRepo.addCard(deckId, card.id)
            val deckName = deckRepo.deck(deckId)?.name ?: "balíku"
            onDone("Přidáno do „$deckName“: ${card.name}")
        }
    }
}
