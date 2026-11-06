package com.example.antimarcipan.data.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Jazyky, do kterých umí překladač ve skenu přeložit textaci karty. */
enum class TranslationLang(val code: String, val label: String) {
    EN("en", "Angličtina"),
    DE("de", "Němčina"),
    IT("it", "Italština"),
    ES("es", "Španělština"),
    KO("ko", "Korejština"),
    JA("ja", "Japonština");

    companion object {
        fun fromCode(code: String?): TranslationLang =
            entries.firstOrNull { it.code == code } ?: EN
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

    fun setTranslationLang(lang: TranslationLang) {
        prefs.edit().putString(KEY_LANG, lang.code).apply()
        _translationLang.value = lang
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
    }
}
