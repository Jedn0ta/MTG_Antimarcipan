package com.example.antimarcipan.support

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Odznak Mecenáš — poděkování za „Kafe pro autora“. Jen kosmetika na Nástěnce. */
object Supporter {
    private val _active = MutableStateFlow(false)
    val active: StateFlow<Boolean> = _active.asStateFlow()
    val isSupporter: Boolean get() = _active.value

    fun load(ctx: Context) {
        _active.value = ctx.getSharedPreferences("support", Context.MODE_PRIVATE).getBoolean("mecenas", false)
    }

    fun grant(ctx: Context) {
        ctx.getSharedPreferences("support", Context.MODE_PRIVATE).edit().putBoolean("mecenas", true).apply()
        _active.value = true
    }
}
