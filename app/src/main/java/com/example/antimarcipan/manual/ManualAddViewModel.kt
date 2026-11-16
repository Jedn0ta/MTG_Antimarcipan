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

    fun addToWishlist(card: ScryfallCard, onDone: (String) -> Unit) {
        viewModelScope.launch {
            repo.addToWishlist(card)
            onDone("Na wishlist: ${card.name}")
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

    // ---- Hromadný import seznamu ----
    private val _importBusy = MutableStateFlow(false)
    val importBusy: StateFlow<Boolean> = _importBusy.asStateFlow()

    enum class Target { COLLECTION, WISHLIST, DECK }

    /**
     * Naimportuje seznam ve formátu „4 Lightning Bolt“, „4x Bolt“, „Bolt (M10) 146“.
     * Prázdné řádky, komentáře a nadpisy (Sideboard, Deck…) přeskočí.
     */
    fun importList(text: String, target: Target, deckId: Long?, onDone: (String) -> Unit) {
        val entries = parseCardList(text)
        if (entries.isEmpty()) { onDone("Seznam je prázdný"); return }
        viewModelScope.launch {
            _importBusy.value = true
            val (found, missing) = repo.resolveNames(entries.map { it.second })
            var added = 0
            for ((qty, name) in entries) {
                val card = found[name.lowercase()] ?: continue
                repeat(qty) {
                    when (target) {
                        Target.COLLECTION -> repo.addToCollection(card)
                        Target.WISHLIST -> if (it == 0) repo.addToWishlist(card)
                        Target.DECK -> { repo.addToCollection(card); deckId?.let { d -> deckRepo.addCard(d, card.id) } }
                    }
                }
                added += qty
            }
            _importBusy.value = false
            onDone(
                "Přidáno $added karet" +
                    if (missing.isEmpty()) "." else ". Nenalezeno (${missing.size}): " + missing.joinToString(", ")
            )
        }
    }
}

/** Parsuje řádky seznamu na (počet, jméno). */
fun parseCardList(text: String): List<Pair<Int, String>> {
    val headers = setOf("deck", "sideboard", "commander", "companion", "maybeboard", "main", "mainboard")
    val out = LinkedHashMap<String, Pair<Int, String>>()
    for (raw in text.lines()) {
        var line = raw.trim().removePrefix("SB:").trim()
        if (line.isEmpty() || line.startsWith("//") || line.startsWith("#")) continue
        if (line.lowercase().trimEnd(':') in headers) continue
        var qty = 1
        Regex("""^(\d+)\s*[xX×]?\s+(.+)$""").find(line)?.let {
            qty = it.groupValues[1].toInt().coerceIn(1, 99); line = it.groupValues[2]
        }
        // odříznout edici „(M10) 146“, foil značky „*F*“
        line = line.replace(Regex("""\s*\([A-Za-z0-9]{2,6}\)(\s+\S+)?\s*$"""), "")
            .replace(Regex("""\s*\*[A-Z]+\*\s*$"""), "").trim()
        if (line.length < 2) continue
        val key = line.lowercase()
        val prev = out[key]
        out[key] = (prev?.first ?: 0) + qty to line
    }
    return out.values.toList()
}
