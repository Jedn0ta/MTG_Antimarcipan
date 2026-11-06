package com.example.antimarcipan.synergy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.antimarcipan.data.DeckRepository
import com.example.antimarcipan.data.SynergyRecommendation
import com.example.antimarcipan.data.SynergyRepository
import com.example.antimarcipan.data.local.Deck
import com.example.antimarcipan.data.synergy.DeckSynergyReport
import com.example.antimarcipan.data.synergy.SynergyEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SynergyUiState(
    val deckName: String? = null,
    val commanderName: String? = null,
    val loading: Boolean = false,
    val report: DeckSynergyReport? = null,
    val suggestions: List<SynergyRecommendation> = emptyList(),
    val suggestionsSource: String = "",
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

    // Téma balíku → Scryfall oracle fragment pro návrhy
    private val themeQuery = mapOf(
        "+1/+1 counters" to "o:\"+1/+1 counter\"",
        "counters" to "o:counter",
        "sacrifice" to "o:sacrifice",
        "graveyard" to "o:graveyard",
        "tokens" to "o:token",
        "lifegain" to "o:\"gain\" o:\"life\"",
        "card draw" to "o:draw",
        "mill" to "o:mill",
        "damage/burn" to "o:\"deals\" o:\"damage\"",
        "death triggers" to "o:dies",
        "landfall" to "o:landfall",
        "spellslinger" to "o:\"instant or sorcery\"",
        "proliferate" to "o:proliferate",
        "equipment" to "t:equipment",
        "auras/enchant" to "t:aura",
    )

    fun analyzeDeck(deckId: Long) {
        viewModelScope.launch {
            val deck = deckRepo.deck(deckId)
            val cards = deckRepo.deckCards(deckId).first()
            if (cards.isEmpty()) {
                _state.value = SynergyUiState(
                    deckName = deck?.name,
                    error = "Balík je prázdný. Přidej karty v záložce Balíky.",
                )
                return@launch
            }
            _state.value = SynergyUiState(deckName = deck?.name, loading = true)

            val report = SynergyEngine.analyze(cards)

            // Návrhy: velitel → EDHREC, jinak Scryfall podle barev + tématu
            val commander = deck?.commanderName
            val exclude = cards.map { it.name }.toSet()
            val (suggestions, source) = if (!commander.isNullOrBlank()) {
                val recs = synergyRepo.recommendationsFor(commander).getOrElse { emptyList() }
                recs to "EDHREC · velitel $commander"
            } else {
                val frag = report.topThemes.firstNotNullOfOrNull { themeQuery[it] }
                val recs = synergyRepo.suggestFromScryfall(report.colors, frag, exclude)
                    .getOrElse { emptyList() }
                recs to "Scryfall · barvy + téma"
            }

            _state.value = SynergyUiState(
                deckName = deck?.name,
                commanderName = commander,
                report = report,
                suggestions = suggestions.filter { !it.owned || true }.take(40),
                suggestionsSource = source,
            )
        }
    }
}
