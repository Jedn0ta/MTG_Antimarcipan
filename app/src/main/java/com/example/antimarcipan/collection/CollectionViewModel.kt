package com.example.antimarcipan.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.antimarcipan.data.CollectionRepository
import com.example.antimarcipan.data.DeckRepository
import com.example.antimarcipan.data.local.CollectionCard
import com.example.antimarcipan.data.local.Deck
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CollectionViewModel(
    private val repo: CollectionRepository,
    private val deckRepo: DeckRepository,
) : ViewModel() {

    private val _filter = MutableStateFlow(CollectionFilter())
    val filter: StateFlow<CollectionFilter> = _filter

    /** Vybrané karty (multi-výběr) podle scryfallId. */
    private val _selection = MutableStateFlow<Set<String>>(emptySet())
    val selection: StateFlow<Set<String>> = _selection

    val decks: StateFlow<List<Deck>> = deckRepo.decks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch { repo.refreshPrices() }
        // Dostáhne náhledy u karet, které je ještě nemají lokálně uložené.
        viewModelScope.launch { repo.backfillImages() }
    }

    /** Výsledný, filtrovaný a seřazený seznam. */
    val cards: StateFlow<List<CollectionCard>> =
        combine(repo.cards, _filter) { cards, f ->
            CollectionFiltering.apply(cards, f)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Draft — nezařazené (rozpracované) karty. */
    val drafts: StateFlow<List<CollectionCard>> =
        combine(repo.unsorted, _filter) { cards, f ->
            CollectionFiltering.apply(cards, f)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Wishlist — po stejném filtru jako sbírka. */
    val wishlist: StateFlow<List<CollectionCard>> =
        combine(repo.wishlist, _filter) { cards, f ->
            CollectionFiltering.apply(cards, f)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Edice dostupné ve sbírce (pro filtr podle edice). */
    val availableSets: StateFlow<List<SetOption>> =
        repo.cards.map { list ->
            list.filter { it.setCode != null }
                .groupBy { it.setCode!! }
                .map { (code, group) ->
                    SetOption(code, group.first().setName ?: code.uppercase())
                }
                .sortedBy { it.label }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- filtr ---
    fun setQuery(q: String) { _filter.value = _filter.value.copy(query = q) }

    fun toggleColor(c: CardColor) = _filter.update {
        it.copy(colors = it.colors.toggle(c))
    }

    fun toggleType(t: CardType) = _filter.update {
        it.copy(types = it.types.toggle(t))
    }

    fun toggleRarity(r: Rarity) = _filter.update {
        it.copy(rarities = it.rarities.toggle(r))
    }

    fun toggleCmc(bucket: Int) = _filter.update {
        it.copy(cmcBuckets = it.cmcBuckets.toggle(bucket))
    }

    fun toggleSet(code: String) = _filter.update {
        it.copy(sets = it.sets.toggle(code))
    }

    fun setSort(sort: SortBy) = _filter.update {
        if (it.sortBy == sort) it.copy(sortDesc = !it.sortDesc) else it.copy(sortBy = sort)
    }

    fun clearFilters() {
        _filter.value = CollectionFilter()
    }

    // --- výběr ---
    fun toggleSelect(id: String) = _selection.update {
        if (it.contains(id)) it - id else it + id
    }

    fun clearSelection() { _selection.value = emptySet() }

    fun selectAllVisible() {
        _selection.value = cards.value.map { it.scryfallId }.toSet()
    }

    val selectionActive: Boolean get() = _selection.value.isNotEmpty()

    fun addSelectionToDeck(deckId: Long, onDone: (Int) -> Unit) {
        val ids = _selection.value.toList()
        viewModelScope.launch {
            ids.forEach { deckRepo.addCard(deckId, it) }
            _selection.value = emptySet()
            onDone(ids.size)
        }
    }

    fun addCardToDeck(deckId: Long, scryfallId: String, onDone: () -> Unit) {
        viewModelScope.launch {
            deckRepo.addCard(deckId, scryfallId)
            onDone()
        }
    }

    /** Kompletní filtr karet (společný model s Balíky). */
    private val _cardFilter = kotlinx.coroutines.flow.MutableStateFlow(com.example.antimarcipan.ui.filter.CardFilterState())
    val cardFilter: StateFlow<com.example.antimarcipan.ui.filter.CardFilterState> = _cardFilter

    fun setCardFilter(f: com.example.antimarcipan.ui.filter.CardFilterState) { _cardFilter.value = f }

    /** Všechny tisky karty (pro změnu edice). */
    suspend fun printsOf(name: String): List<com.example.antimarcipan.data.scryfall.ScryfallCard> =
        repo.printsOf(name).getOrElse { emptyList() }

    fun changePrinting(
        old: CollectionCard,
        newCard: com.example.antimarcipan.data.scryfall.ScryfallCard,
        onDone: () -> Unit = {},
    ) {
        viewModelScope.launch {
            repo.changePrinting(old, newCard)
            onDone()
        }
    }

    fun removeFromWishlist(scryfallId: String) {
        viewModelScope.launch { repo.removeFromWishlist(scryfallId) }
    }

    /** Přesune wishlist kartu do sbírky (vlastním ji). */
    fun wishlistToCollection(scryfallId: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repo.markOwned(scryfallId)
            onDone()
        }
    }

    fun remove(scryfallId: String) {
        viewModelScope.launch { repo.remove(scryfallId) }
        _selection.update { it - scryfallId }
    }

    suspend fun rulingsFor(scryfallId: String): List<String> =
        repo.rulings(scryfallId).getOrElse { emptyList() }

    private fun <T> Set<T>.toggle(item: T): Set<T> =
        if (contains(item)) this - item else this + item
}
