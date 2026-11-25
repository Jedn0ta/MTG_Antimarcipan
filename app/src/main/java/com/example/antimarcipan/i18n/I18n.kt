package com.example.antimarcipan.i18n

import android.content.Context
import java.util.Locale

/**
 * Jednoduchá lokalizace: texty v kódu jsou česky, pro ostatní jazyky telefonu
 * se zobrazí anglicky (slovník [EN]). Vzory s {0}, {1}… nahradí argumenty.
 */
object I18n {
    /** true = anglicky. Výchozí podle jazyka telefonu; lze přepsat v nastavení. */
    @Volatile var english: Boolean = Locale.getDefault().language != "cs"
        private set

    /** "auto" | "cs" | "en" */
    var mode: String = "auto"
        private set

    fun init(ctx: Context) {
        mode = ctx.getSharedPreferences("app_settings", Context.MODE_PRIVATE).getString("lang", "auto") ?: "auto"
        english = when (mode) {
            "cs" -> false
            "en" -> true
            else -> Locale.getDefault().language != "cs"
        }
    }

    fun setMode(ctx: Context, m: String) {
        ctx.getSharedPreferences("app_settings", Context.MODE_PRIVATE).edit().putString("lang", m).apply()
        init(ctx)
    }
}

/** Přeloží český text (nebo vzor s {0}…) do jazyka aplikace. */
fun tr(cs: String, vararg args: Any?): String {
    var s = if (I18n.english) EN[cs] ?: cs else cs
    args.forEachIndexed { i, a -> s = s.replace("{$i}", a.toString()) }
    return s
}
