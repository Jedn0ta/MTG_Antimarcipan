package com.example.antimarcipan.ui

import androidx.compose.runtime.staticCompositionLocalOf

/** Měřítko náhledů karet v seznamech a v mřížce (z Nastavení). */
data class ThumbScale(val list: Float = 1f, val grid: Float = 1f)

val LocalThumbScale = staticCompositionLocalOf { ThumbScale() }
