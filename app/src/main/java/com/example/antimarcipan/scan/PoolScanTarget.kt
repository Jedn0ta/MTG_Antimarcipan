package com.example.antimarcipan.scan

import kotlinx.coroutines.flow.MutableStateFlow

/** Balík (draft/sealed), do jehož poolu se právě skenují nové karty. */
data class PoolTarget(val deckId: Long, val deckName: String)

object PoolScanTarget {
    val target = MutableStateFlow<PoolTarget?>(null)
}
