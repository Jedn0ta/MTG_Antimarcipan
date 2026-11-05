package com.example.antimarcipan.decks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.antimarcipan.data.CollectionRepository
import com.example.antimarcipan.data.DeckRepository
import com.example.antimarcipan.data.local.CollectionCard
import com.example.antimarcipan.data.local.Deck
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class DeckViewModel(
    private val deckRepo: DeckRepository,
    collectionRepo: CollectionRepository,
) : ViewModel() {

    val decks: StateFlow<List<Deck>> = deckRepo.decks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Whole collection, so the user can pick cards to add to a deck. */
    val collection: StateFlow<List<CollectionCard>> = collectionRepo.cards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val selectedDeckId = MutableStateFlow<Long?>(null)

    val selectedDeck: StateFlow<Deck?> = selectedDeckId
        .flatMapLatest { id ->
            if (id == null) flowOf(null) else flowOf(deckRepo.deck(id))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedDeckCards: StateFlow<List<CollectionCard>> = selectedDeckId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else deckRepo.deckCards(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun select(deckId: Long) { selectedDeckId.value = deckId }

    fun createDeck(name: String) {
        viewModelScope.launch {
            val id = deckRepo.createDeck(name.ifBlank { "Nový balík" })
            selectedDeckId.value = id
        }
    }

    fun deleteDeck(deckId: Long) {
        viewModelScope.launch {
            deckRepo.deleteDeck(deckId)
            if (selectedDeckId.value == deckId) selectedDeckId.value = null
        }
    }

    fun addCard(card: CollectionCard) {
        val deckId = selectedDeckId.value ?: return
        viewModelScope.launch { deckRepo.addCard(deckId, card.scryfallId) }
    }

    fun removeCard(card: CollectionCard) {
        val deckId = selectedDeckId.value ?: return
        viewModelScope.launch { deckRepo.removeCard(deckId, card.scryfallId) }
    }

    fun setCommander(card: CollectionCard) {
        val deckId = selectedDeckId.value ?: return
        viewModelScope.launch {
            deckRepo.setCommander(deckId, card)
            // Ensure the commander is also in the deck list.
            deckRepo.addCard(deckId, card.scryfallId)
            selectedDeckId.value = deckId // refresh
        }
    }
}
