package com.example.antimarcipan.advisor

import kotlinx.coroutines.flow.MutableStateFlow

/** Jednoduché „přejdi na balík“ napříč záložkami (Rádce → Balíky). */
object AppNav {
    val openDeck = MutableStateFlow<Long?>(null)
}
