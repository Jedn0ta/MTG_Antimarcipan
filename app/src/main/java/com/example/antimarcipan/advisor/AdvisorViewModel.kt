package com.example.antimarcipan.advisor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.antimarcipan.data.CollectionRepository
import com.example.antimarcipan.data.DeckRepository
import com.example.antimarcipan.data.advisor.AdvisorEngine
import com.example.antimarcipan.data.advisor.AdvisorTip
import com.example.antimarcipan.data.advisor.BuildFormat
import com.example.antimarcipan.data.advisor.BuildResult
import com.example.antimarcipan.data.advisor.DeckBuilder
import com.example.antimarcipan.data.local.CollectionCard
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

/** Rádce: analýza balíku, stavba balíku ze sbírky, zkušební ruka. */
class AdvisorViewModel(
    private val deckRepo: DeckRepository,
    private val collectionRepo: CollectionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AdvisorUiState())
    val state: StateFlow<AdvisorUiState> = _state.asStateFlow()

    val decks: StateFlow<List<Deck>> = deckRepo.decks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val owned: StateFlow<List<CollectionCard>> = collectionRepo.cards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun analyzeDeck(deckId: Long) {
        viewModelScope.launch {
            val deck = deckRepo.deck(deckId)
            val cards = deckRepo.deckCards(deckId).first()
            val commander = cards.firstOrNull { it.scryfallId == deck?.commanderScryfallId }
            val tips = AdvisorEngine.analyze(cards, commander)
            val (curve, avg, lands) = com.example.antimarcipan.ui.computeCurve(deckRepo.deckCardsWithQty(deckId).first())
            _state.value = AdvisorUiState(deck?.name, tips, true, curve, avg, lands)
        }
    }

    // ---- Stavba ze sbírky ----
    private val _build = MutableStateFlow<BuildResult?>(null)
    val build: StateFlow<BuildResult?> = _build

    fun buildDeck(format: BuildFormat, colors: List<String>?, commander: CollectionCard?) {
        _build.value = DeckBuilder.build(owned.value, format, colors, commander)
    }

    fun clearBuild() { _build.value = null }

    /** Uloží navržený balík a otevře ho v záložce Balíky. */
    fun saveBuild(name: String) {
        val b = _build.value ?: return
        viewModelScope.launch {
            val id = deckRepo.createDeck(name.ifBlank { "Navržený balík" }, b.format.deckFormat, b.colors.joinToString(""))
            b.commander?.let { deckRepo.addCard(id, it.scryfallId); deckRepo.setCommander(id, it) }
            b.cards.forEach { (c, q) -> deckRepo.setBasicLandCount(id, c.scryfallId, q) }
            b.basics.forEach { (n, q) ->
                val bid = collectionRepo.addBasicLandToCollection(n)
                if (bid != null && q > 0) deckRepo.setBasicLandCount(id, bid, q)
            }
            _build.value = null
            AppNav.openDeck.value = id
        }
    }

    /** Založí limited balík (průvodce draftem/sealedem) a otevře ho. */
    fun createLimitedDeck(name: String, format: String) {
        viewModelScope.launch {
            val id = deckRepo.createDeck(name, format, null)
            AppNav.openDeck.value = id
        }
    }

    // ---- Komba ----
    data class ComboState(val loading: Boolean = false, val report: com.example.antimarcipan.data.advisor.ComboReport? = null, val error: String? = null)
    private val _combos = MutableStateFlow(ComboState())
    val combos: StateFlow<ComboState> = _combos

    /** Najde komba mezi kartami sbírky, nebo jen v balíku [deckId]. */
    fun findCombos(deckId: Long?) {
        _combos.value = ComboState(loading = true)
        viewModelScope.launch {
            val names = if (deckId != null) deckRepo.deckCards(deckId).first().map { it.name }
            else owned.value.map { it.name }
            com.example.antimarcipan.data.advisor.ComboFinder.find(names)
                .onSuccess { _combos.value = ComboState(report = it) }
                .onFailure { _combos.value = ComboState(error = "Nepodařilo se načíst komba (${it.message ?: "chyba sítě"}).") }
        }
    }

    // ---- Zkušební ruka ----
    suspend fun deckLibrary(deckId: Long): List<CollectionCard> {
        val deck = deckRepo.deck(deckId)
        return deckRepo.deckCardsWithQty(deckId).first()
            .filter { it.card.scryfallId != deck?.commanderScryfallId }
            .flatMap { e -> List(e.deckQuantity.coerceAtLeast(1)) { e.card } }
    }
}
