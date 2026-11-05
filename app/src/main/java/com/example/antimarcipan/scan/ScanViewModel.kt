package com.example.antimarcipan.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.antimarcipan.data.CollectionRepository
import com.example.antimarcipan.data.scryfall.ScryfallCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ScanUiState(
    val recognizedText: String = "",
    val candidate: ScryfallCard? = null,
    val status: String = "Namiř foťák na jméno karty",
    val looking: Boolean = false,
    val lastAdded: String? = null,
)

class ScanViewModel(
    private val repo: CollectionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ScanUiState())
    val state: StateFlow<ScanUiState> = _state.asStateFlow()

    private var lastQuery: String = ""

    /** Called by the camera analyzer with each OCR result. */
    fun onTextRecognized(text: String) {
        val firstLine = text.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.length >= 3 && it.any(Char::isLetter) } ?: return

        // Ignore if we're already showing this candidate or mid-lookup.
        if (firstLine.equals(lastQuery, ignoreCase = true) || _state.value.looking) return
        lastQuery = firstLine
        _state.value = _state.value.copy(recognizedText = firstLine, looking = true, status = "Hledám „$firstLine“…")

        viewModelScope.launch {
            repo.lookup(text)
                .onSuccess { card ->
                    _state.value = _state.value.copy(
                        candidate = card,
                        looking = false,
                        status = "Nalezeno: ${card.name}",
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        candidate = null,
                        looking = false,
                        status = "Nenašlo se — zkus lepší světlo / úhel",
                    )
                }
        }
    }

    fun addCandidate() {
        val card = _state.value.candidate ?: return
        viewModelScope.launch {
            repo.addToCollection(card)
            _state.value = _state.value.copy(
                candidate = null,
                lastAdded = card.name,
                status = "Přidáno: ${card.name}",
            )
            lastQuery = ""
        }
    }

    fun dismissCandidate() {
        _state.value = _state.value.copy(candidate = null, status = "Namiř foťák na jméno karty")
        lastQuery = ""
    }
}
