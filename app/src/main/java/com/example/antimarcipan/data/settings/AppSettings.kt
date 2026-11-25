package com.example.antimarcipan.data.settings

import com.example.antimarcipan.i18n.tr

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Krajinná pozadí aplikace (5 barev MTG + pustina), volitelná v nastavení. */
enum class AppBackground(val code: String, val label: String) {
    NONE("none", tr("Žádné")),
    PLAINS("plains", tr("Pláně")),
    ISLAND("island", "Ostrov"),
    SWAMP("swamp", tr("Bažina")),
    MOUNTAIN("mountain", "Hory"),
    FOREST("forest", "Les"),
    WASTES("wastes", "Pustina");

    companion object {
        fun fromCode(code: String?): AppBackground =
            entries.firstOrNull { it.code == code } ?: WASTES
    }
}

/**
 * Jednoduché uživatelské předvolby přes SharedPreferences, vystavené jako
 * StateFlow, aby na ně obrazovky reagovaly. Singleton v AppContainer.
 */
class AppSettings(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _guideSeen = MutableStateFlow(prefs.getBoolean(KEY_GUIDE_SEEN, false))
    val guideSeen: StateFlow<Boolean> = _guideSeen.asStateFlow()

    private val _background =
        MutableStateFlow(AppBackground.fromCode(prefs.getString(KEY_BACKGROUND, null)))
    val background: StateFlow<AppBackground> = _background.asStateFlow()

    // Zapamatované zobrazení (mřížka/seznam) a sekce balíku.
    private val _collectionGrid = MutableStateFlow(prefs.getBoolean(KEY_COLLECTION_GRID, false))
    val collectionGrid: StateFlow<Boolean> = _collectionGrid.asStateFlow()

    private val _listThumb = MutableStateFlow(prefs.getFloat("thumb_list", 1f))
    /** Měřítko náhledů v seznamech (0.6–1.8). */
    val listThumb: StateFlow<Float> = _listThumb.asStateFlow()
    private val _gridThumb = MutableStateFlow(prefs.getFloat("thumb_grid", 1f))
    /** Měřítko náhledů v mřížce (0.6–1.8). */
    val gridThumb: StateFlow<Float> = _gridThumb.asStateFlow()
    fun setListThumb(v: Float) { prefs.edit().putFloat("thumb_list", v).apply(); _listThumb.value = v }
    fun setGridThumb(v: Float) { prefs.edit().putFloat("thumb_grid", v).apply(); _gridThumb.value = v }

    private val _deckGrid = MutableStateFlow(prefs.getBoolean(KEY_DECK_GRID, false))
    val deckGrid: StateFlow<Boolean> = _deckGrid.asStateFlow()

    private val _deckSection = MutableStateFlow(prefs.getString(KEY_DECK_SECTION, "ALL") ?: "ALL")
    val deckSection: StateFlow<String> = _deckSection.asStateFlow()

    fun setBackground(bg: AppBackground) {
        prefs.edit().putString(KEY_BACKGROUND, bg.code).apply()
        _background.value = bg
    }

    fun setCollectionGrid(on: Boolean) {
        prefs.edit().putBoolean(KEY_COLLECTION_GRID, on).apply()
        _collectionGrid.value = on
    }

    fun setDeckGrid(on: Boolean) {
        prefs.edit().putBoolean(KEY_DECK_GRID, on).apply()
        _deckGrid.value = on
    }

    fun setDeckSection(mode: String) {
        prefs.edit().putString(KEY_DECK_SECTION, mode).apply()
        _deckSection.value = mode
    }

    fun markGuideSeen() {
        if (_guideSeen.value) return
        prefs.edit().putBoolean(KEY_GUIDE_SEEN, true).apply()
        _guideSeen.value = true
    }

    private companion object {
        const val PREFS = "antimarcipan_settings"
        const val KEY_GUIDE_SEEN = "guide_seen"
        const val KEY_BACKGROUND = "app_background"
        const val KEY_COLLECTION_GRID = "collection_grid"
        const val KEY_DECK_GRID = "deck_grid"
        const val KEY_DECK_SECTION = "deck_section"
    }
}
