package com.example.antimarcipan.data.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Jazyk aplikace (a cíl překladu karet ve skenu). Čeština je výchozí. */
enum class TranslationLang(val code: String, val label: String) {
    CS("cs", "Čeština"),
    EN("en", "Angličtina"),
    DE("de", "Němčina"),
    IT("it", "Italština"),
    ES("es", "Španělština"),
    KO("ko", "Korejština"),
    JA("ja", "Japonština");

    companion object {
        fun fromCode(code: String?): TranslationLang =
            entries.firstOrNull { it.code == code } ?: CS
    }
}

/** Krajinná pozadí aplikace (5 barev MTG + pustina), volitelná v nastavení. */
enum class AppBackground(val code: String, val label: String) {
    NONE("none", "Žádné"),
    PLAINS("plains", "Pláně"),
    ISLAND("island", "Ostrov"),
    SWAMP("swamp", "Bažina"),
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

    private val _translationLang =
        MutableStateFlow(TranslationLang.fromCode(prefs.getString(KEY_LANG, null)))
    val translationLang: StateFlow<TranslationLang> = _translationLang.asStateFlow()

    private val _guideSeen = MutableStateFlow(prefs.getBoolean(KEY_GUIDE_SEEN, false))
    val guideSeen: StateFlow<Boolean> = _guideSeen.asStateFlow()

    private val _background =
        MutableStateFlow(AppBackground.fromCode(prefs.getString(KEY_BACKGROUND, null)))
    val background: StateFlow<AppBackground> = _background.asStateFlow()

    // Zapamatované zobrazení (mřížka/seznam) a sekce balíku.
    private val _collectionGrid = MutableStateFlow(prefs.getBoolean(KEY_COLLECTION_GRID, false))
    val collectionGrid: StateFlow<Boolean> = _collectionGrid.asStateFlow()

    private val _deckGrid = MutableStateFlow(prefs.getBoolean(KEY_DECK_GRID, false))
    val deckGrid: StateFlow<Boolean> = _deckGrid.asStateFlow()

    private val _deckSection = MutableStateFlow(prefs.getString(KEY_DECK_SECTION, "ALL") ?: "ALL")
    val deckSection: StateFlow<String> = _deckSection.asStateFlow()

    fun setTranslationLang(lang: TranslationLang) {
        prefs.edit().putString(KEY_LANG, lang.code).apply()
        _translationLang.value = lang
    }

    fun setBackground(bg: AppBackground) {
        prefs.edit().putString(KEY_BACKGROUND, bg.code).apply()
        _background.value = bg
    }

    // API klíč pro kvalitní AI překlad pravidel (Claude). Prázdný = offline překlad.
    private val _aiKey = MutableStateFlow(prefs.getString(KEY_AI_KEY, "") ?: "")
    val aiKey: StateFlow<String> = _aiKey.asStateFlow()

    fun setAiKey(key: String) {
        prefs.edit().putString(KEY_AI_KEY, key.trim()).apply()
        _aiKey.value = key.trim()
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
        const val KEY_LANG = "translation_lang"
        const val KEY_GUIDE_SEEN = "guide_seen"
        const val KEY_BACKGROUND = "app_background"
        const val KEY_COLLECTION_GRID = "collection_grid"
        const val KEY_DECK_GRID = "deck_grid"
        const val KEY_DECK_SECTION = "deck_section"
        const val KEY_AI_KEY = "ai_translate_key"
    }
}
