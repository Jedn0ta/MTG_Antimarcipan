package com.example.antimarcipan.synergy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.antimarcipan.data.DeckRepository
import com.example.antimarcipan.data.SynergyRecommendation
import com.example.antimarcipan.data.SynergyRepository
import com.example.antimarcipan.data.local.Deck
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SynergyUiState(
    val commanderName: String? = null,
    val loading: Boolean = false,
    val recommendations: List<SynergyRecommendation> = emptyList(),
    val error: String? = null,
)

class SynergyViewModel(
    private val synergyRepo: SynergyRepository,
    private val deckRepo: DeckRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SynergyUiState())
    val state: StateFlow<SynergyUiState> = _state.asStateFlow()

    val decks: StateFlow<List<Deck>> = deckRepo.decks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Loads EDHREC recommendations for the given deck's commander. */
    fun loadForDeck(deckId: Long) {
        viewModelScope.launch {
            val deck = deckRepo.deck(deckId)
            val commander = deck?.commanderName
            if (commander.isNullOrBlank()) {
                _state.value = SynergyUiState(
                    error = "Balík nemá velitele. Nastav velitele v záložce Balíky."
                )
                return@launch
            }
            _state.value = SynergyUiState(commanderName = commander, loading = true)
            synergyRepo.recommendationsFor(commander)
                .onSuccess { recs ->
                    _state.value = SynergyUiState(commanderName = commander, recommendations = recs)
                }
                .onFailure { e ->
                    _state.value = SynergyUiState(
                        commanderName = commander,
                        error = "EDHREC se nepodařilo načíst (${e.message ?: "neznámá chyba"}). Zkontroluj jméno velitele / připojení.",
                    )
                }
        }
    }
}
