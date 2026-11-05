package com.example.antimarcipan.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.antimarcipan.data.CollectionRepository
import com.example.antimarcipan.data.local.CollectionCard
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CollectionViewModel(
    private val repo: CollectionRepository,
) : ViewModel() {

    val cards: StateFlow<List<CollectionCard>> = repo.cards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun remove(scryfallId: String) {
        viewModelScope.launch { repo.remove(scryfallId) }
    }
}
