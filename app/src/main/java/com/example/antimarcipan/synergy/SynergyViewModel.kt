package com.example.antimarcipan.synergy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.antimarcipan.data.DeckRepository
import com.example.antimarcipan.data.SynergyRecommendation
import com.example.antimarcipan.data.SynergyRepository
import com.example.antimarcipan.data.advisor.AdvisorEngine
import com.example.antimarcipan.data.advisor.AdvisorTip
import com.example.antimarcipan.data.local.Deck
import com.example.antimarcipan.data.synergy.DeckSynergyReport
import com.example.antimarcipan.data.synergy.SynergyEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SynergyUiState(
    val deckName: String? = null,
    val commanderName: String? = null,
    val loading: Boolean = false,
    val report: DeckSynergyReport? = null,
    val suggestions: List<SynergyRecommendation> = emptyList(),
    val suggestionsSource: String = "",
    /** Náhrady pro nevhodné karty: scryfallId → názvy navrhovaných karet. */
    val replacements: Map<String, List<String>> = emptyMap(),
    /** Obecné rady k balíku (dříve „Anti Marcipán“). */
    val tips: List<AdvisorTip> = emptyList(),
    /** URL obrázků navržených karet: jméno (malými) → URL. */
    val images: Map<String, String> = emptyMap(),
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

    private val mainTypes = listOf(
        "Creature", "Planeswalker", "Instant", "Sorcery",
        "Artifact", "Enchantment", "Battle", "Land",
    )

    private fun primaryType(typeLine: String?): String =
        mainTypes.firstOrNull { typeLine?.contains(it, ignoreCase = true) == true } ?: ""

    fun analyzeDeck(deckId: Long) {
        viewModelScope.launch {
            try {
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

                // Těžší výpočet mimo hlavní vlákno.
                val report = withContext(Dispatchers.Default) { SynergyEngine.analyze(cards) }

                // Obecné rady k balíku (rules-based rádce).
                val commanderCard = cards.firstOrNull { it.scryfallId == deck?.commanderScryfallId }
                val tips = withContext(Dispatchers.Default) { AdvisorEngine.analyze(cards, commanderCard) }

                // Návrhy: velitel → EDHREC, jinak Scryfall podle barev + tématu.
                // Síť je už uvnitř ošetřená (Result), ale pro jistotu i tady.
                val commander = deck?.commanderName
                val exclude = cards.map { it.name }.toSet()
                val (suggestions, source) = try {
                    if (!commander.isNullOrBlank()) {
                        val recs = synergyRepo.recommendationsFor(commander).getOrElse { emptyList() }
                        recs to "EDHREC · velitel $commander"
                    } else {
                        val frag = report.topThemes.firstNotNullOfOrNull { themeQuery[it] }
                        val recs = synergyRepo.suggestFromScryfall(report.colors, frag, exclude)
                            .getOrElse { emptyList() }
                        recs to "Scryfall · barvy + téma"
                    }
                } catch (e: Exception) {
                    emptyList<SynergyRecommendation>() to "Návrhy se nenačetly"
                }

                _state.value = SynergyUiState(
                    deckName = deck?.name,
                    commanderName = commander,
                    report = report,
                    suggestions = suggestions.take(40),
                    suggestionsSource = source,
                    tips = tips,
                )

                // Náhrady pro nevhodné karty — doplní se po zobrazení reportu.
                try {
                    val frag = report.topThemes.firstNotNullOfOrNull { themeQuery[it] }
                    val deckNames = cards.map { it.name }.toSet()
                    val map = mutableMapOf<String, List<String>>()
                    for (misfit in report.misfits.take(6)) {
                        val type = primaryType(misfit.card.typeLine)
                        val cmc = (misfit.card.cmc ?: 0.0).toInt()
                        val repl = synergyRepo.replacementsFor(
                            primaryType = type,
                            cmc = cmc,
                            colors = report.colors,
                            themeFragment = frag,
                            exclude = deckNames + misfit.card.name,
                        ).getOrElse { emptyList() }
                        if (repl.isNotEmpty()) map[misfit.card.scryfallId] = repl
                    }
                    if (map.isNotEmpty()) {
                        _state.value = _state.value.copy(replacements = map)
                    }
                } catch (_: Exception) { /* náhrady jsou volitelné */ }

                // Obrázky návrhů a náhrad — skutečné URL přes Scryfall API.
                try {
                    val st = _state.value
                    val names = st.suggestions.map { it.name } + st.replacements.values.flatten()
                    if (names.isNotEmpty()) {
                        val imgs = synergyRepo.imageUrls(names)
                        if (imgs.isNotEmpty()) _state.value = _state.value.copy(images = imgs)
                    }
                } catch (_: Exception) { /* obrázky jsou volitelné */ }
            } catch (e: Exception) {
                _state.value = SynergyUiState(
                    error = "Analýza selhala: ${e.message ?: e.javaClass.simpleName}",
                )
            }
        }
    }
}
