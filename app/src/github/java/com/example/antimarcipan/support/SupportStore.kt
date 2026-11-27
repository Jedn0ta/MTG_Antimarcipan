package com.example.antimarcipan.support

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Verze z GitHubu nákupy nemá. */
object SupportStore {
    const val AVAILABLE = false
    val price: StateFlow<String?> = MutableStateFlow(null)
    val message: MutableStateFlow<String?> = MutableStateFlow(null)
    fun connect(ctx: Context) { Supporter.load(ctx) }
    fun buy(activity: Activity) {}
    fun restore() {}
}
