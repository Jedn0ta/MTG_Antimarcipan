package com.example.antimarcipan.ui

import kotlinx.coroutines.flow.MutableStateFlow

/** Rychlé akce z ikony aplikace (podržení ikony v launcheru). */
object AppShortcuts {
    const val QUICK_SCAN = "com.example.antimarcipan.QUICK_SCAN"
    const val SCAN = "com.example.antimarcipan.SCAN"
    const val GLOSSARY = "com.example.antimarcipan.GLOSSARY"

    /** Čekající akce; obrazovka, která ji zpracuje, ji vynuluje. */
    val pending = MutableStateFlow<String?>(null)

    fun handle(action: String?) {
        if (action == QUICK_SCAN || action == SCAN || action == GLOSSARY) pending.value = action
    }
}
