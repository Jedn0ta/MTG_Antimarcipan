package com.example.antimarcipan.advisor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.antimarcipan.data.DeckRepository
import com.example.antimarcipan.data.advisor.AdvisorEngine
import com.example.antimarcipan.data.advisor.AdvisorTip
import com.example.antimarcipan.data.local.Deck
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AdvisorUiState(
    val deckName: String? = null,
    val tips: List<AdvisorTip> = emptyList(),
    val analyzed: Boolean = false,
    /** Mana curve: index 0..7 (7 = 7+), pár (tvorové, ostatní kouzla). */
    val curve: List<Pair<Int, Int>> = emptyList(),
    val avgMv: Double = 0.0,
    val lands: Int = 0,
)

/** Drives the "Anti Marcipán" advisor screen. */
class AdvisorViewModel(
    private val deckRepo: DeckRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AdvisorUiState())
    val state: StateFlow<AdvisorUiState> = _state.asStateFlow()

    val decks: StateFlow<List<Deck>> = deckRepo.decks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun analyzeDeck(deckId: Long) {
        viewModelScope.launch {
            val deck = deckRepo.deck(deckId)
            val cards = deckRepo.deckCards(deckId).first()
            val commander = cards.firstOrNull { it.scryfallId == deck?.commanderScryfallId }
            val tips = AdvisorEngine.analyze(cards, commander)
            val withQty = deckRepo.deckCardsWithQty(deckId).first()
            val spells = withQty.filter { it.card.typeLine?.contains("Land", true) != true }
            val curve = (0..7).map { b ->
                val inB = spells.filter { minOf(7, (it.card.cmc ?: 0.0).toInt()) == b }
                inB.filter { it.card.typeLine?.contains("Creature", true) == true }.sumOf { it.deckQuantity } to
                    inB.filter { it.card.typeLine?.contains("Creature", true) != true }.sumOf { it.deckQuantity }
            }
            val totalSpells = spells.sumOf { it.deckQuantity }
            val avg = if (totalSpells == 0) 0.0 else spells.sumOf { (it.card.cmc ?: 0.0) * it.deckQuantity } / totalSpells
            _state.value = AdvisorUiState(
                deckName = deck?.name,
                tips = tips,
                analyzed = true,
                curve = curve,
                avgMv = avg,
                lands = withQty.filter { it.card.typeLine?.contains("Land", true) == true }.sumOf { it.deckQuantity },
            )
        }
    }
}
