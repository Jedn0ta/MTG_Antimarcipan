package com.example.antimarcipan.decks

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.antimarcipan.data.CollectionRepository
import com.example.antimarcipan.data.DeckRepository
import com.example.antimarcipan.data.local.CollectionCard
import com.example.antimarcipan.data.local.Deck
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class DeckViewModel(
    private val deckRepo: DeckRepository,
    private val collectionRepo: CollectionRepository,
    private val appContext: Context,
) : ViewModel() {

    val decks: StateFlow<List<Deck>> = deckRepo.decks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Whole collection, so the user can pick cards to add to a deck. */
    val collection: StateFlow<List<CollectionCard>> = collectionRepo.cards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val selectedDeckId = MutableStateFlow<Long?>(null)

    // Odvozeno ze seznamu balíků → reaguje na přejmenování, náhled i velitele.
    val selectedDeck: StateFlow<Deck?> =
        combine(decks, selectedDeckId) { list, id ->
            list.firstOrNull { it.id == id }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedDeckCards: StateFlow<List<com.example.antimarcipan.data.local.DeckCardWithQty>> =
        selectedDeckId
            .flatMapLatest { id ->
                if (id == null) flowOf(emptyList()) else deckRepo.deckCardsWithQty(id)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Změní počet konkrétní karty (např. základní země) v balíku. */
    /** Pool limited balíku (karty z boosterů), quantity v deckQuantity. */
    val selectedDeckPool: StateFlow<List<com.example.antimarcipan.data.local.DeckCardWithQty>> =
        selectedDeckId
            .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else deckRepo.pool(id) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun startPoolScan(deckId: Long, name: String) {
        com.example.antimarcipan.scan.PoolScanTarget.target.value =
            com.example.antimarcipan.scan.PoolTarget(deckId, name)
    }

    fun setPoolQty(deckId: Long, scryfallId: String, qty: Int) {
        viewModelScope.launch { deckRepo.setPoolQty(deckId, scryfallId, qty) }
    }

    /** Hromadně přidá všechny karty z poolu do sbírky. */
    fun importPoolToCollection(deckId: Long) {
        viewModelScope.launch {
            deckRepo.poolOf(deckId).forEach { collectionRepo.addOwned(it.scryfallId, it.quantity) }
            deckRepo.setPoolImported(deckId, true)
        }
    }

    fun setDeckCardQty(deckId: Long, scryfallId: String, qty: Int) {
        viewModelScope.launch { deckRepo.setBasicLandCount(deckId, scryfallId, qty) }
    }

    /** Přidá základní země hromadně: mapa jméno země → počet. */
    fun addBasicLands(deckId: Long, counts: Map<String, Int>) {
        viewModelScope.launch {
            counts.forEach { (name, count) ->
                if (count > 0) {
                    val id = collectionRepo.addBasicLandToCollection(name)
                    if (id != null) deckRepo.setBasicLandCount(deckId, id, count)
                }
            }
        }
    }

    fun select(deckId: Long) { selectedDeckId.value = deckId }

    fun closeDeck() { selectedDeckId.value = null }

    fun setPoolSet(deckId: Long, set: String?) {
        viewModelScope.launch { deckRepo.setPoolSet(deckId, set) }
    }

    fun createDeck(name: String, format: DeckFormat = DeckFormat.FREEFORM, colors: String? = null, poolSet: String? = null) {
        viewModelScope.launch {
            val id = deckRepo.createDeck(name.ifBlank { "Nový balík" }, format.code, colors, poolSet)
            selectedDeckId.value = id
        }
    }

    fun renameDeck(deckId: Long, name: String) {
        viewModelScope.launch { deckRepo.rename(deckId, name) }
    }

    fun setDeckColors(deckId: Long, colors: String?) {
        viewModelScope.launch { deckRepo.setColors(deckId, colors) }
    }

    fun setFormat(deckId: Long, format: DeckFormat) {
        viewModelScope.launch { deckRepo.setFormat(deckId, format.code) }
    }

    suspend fun deckColors(deckId: Long): Set<String> = deckRepo.deckColors(deckId)

    fun deleteDeck(deckId: Long) {
        viewModelScope.launch {
            deckRepo.deleteDeck(deckId)
            if (selectedDeckId.value == deckId) selectedDeckId.value = null
        }
    }

    /** Uloží vybraný obrázek jako náhled balíku (zkopíruje do úložiště appky). */
    fun setCover(deckId: Long, uri: Uri) {
        viewModelScope.launch {
            val path = withContext(Dispatchers.IO) { copyCover(deckId, uri) }
            if (path != null) deckRepo.setCover(deckId, path)
        }
    }

    fun clearCover(deckId: Long) {
        viewModelScope.launch { deckRepo.setCover(deckId, null) }
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
            deckRepo.addCard(deckId, card.scryfallId)
        }
    }

    private fun copyCover(deckId: Long, uri: Uri): String? = runCatching {
        val dir = File(appContext.filesDir, "deck_covers").apply { mkdirs() }
        val out = File(dir, "deck_${deckId}.jpg")
        appContext.contentResolver.openInputStream(uri)?.use { input ->
            out.outputStream().use { input.copyTo(it) }
        } ?: return null
        out.absolutePath
    }.getOrNull()
}
