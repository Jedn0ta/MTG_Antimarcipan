package com.example.antimarcipan.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Překlad textu karet a rulings z angličtiny do češtiny přímo v telefonu
 * (ML Kit). Jazykový model (~30 MB) se stáhne při prvním použití.
 */
object CzechTranslator {
    private val translator: Translator by lazy {
        Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(TranslateLanguage.CZECH)
                .build()
        )
    }

    private val cache = mutableMapOf<String, String>()

    /** Model pro AI překlad pravidel (rychlý a levný, na překlad textu karet stačí). */
    private const val AI_MODEL = "claude-haiku-5-5"

    /** Zadání pro AI: překlad textů MTG s ustálenou českou terminologií. */
    private val SYSTEM_PROMPT = """
        Jsi zkušený rozhodčí a překladatel Magic: The Gathering. Překládáš oracle texty karet
        a oficiální rulings z angličtiny do češtiny pro české hráče.

        Pravidla překladu:
        - Překládej věcně přesně podle herních pravidel; význam nesmí být posunutý.
        - Názvy karet, klíčová slova a schopnosti (Flying, Trample, Dredge, Ward, Equip, Cascade…)
          ponech v angličtině, jak je zvykem v české komunitě.
        - Herní pojmy překládej ustáleně: creature = stvoření, permanent = permanent,
          spell = kouzlo, ability = schopnost, battlefield = bojiště, graveyard = hřbitov,
          library = knihovna, hand = ruka, exile = exil (exile/exiluj), token = token,
          counter (+1/+1 apod.) = žeton, counter target spell = zruš cílové kouzlo,
          tap/untap = tapni/odtapni, sacrifice = obětuj, destroy = znič, draw = lízni,
          discard = zahoď, mill = millni, target = cílový, controller = ovladatel,
          owner = vlastník, upkeep = údržba, end step = konec tahu, combat = boj,
          mana value = mana value, stack = stack.
        - Symboly many a ceny ve složených závorkách ({T}, {2}{G}, {X}) ponech beze změny.
        - Zachovej členění na řádky a odstavce; text v závorkách přelož také.
        - Neviditelné řídicí znaky U+E000 a U+E001 (označují vysvětlivku od aplikace) ponech
          přesně na jejich místech kolem přeloženého textu závorky.
        - Vrať pouze překlad, bez komentářů a bez uvozovek.
    """.trimIndent()

    private val aiClient by lazy {
        com.example.antimarcipan.data.Network.httpClient.newBuilder()
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .callTimeout(90, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    /**
     * Přeloží text do češtiny. S API klíčem přes AI (kvalitní překlad pravidel),
     * jinak offline strojovým překladem v telefonu.
     */
    suspend fun translate(text: String, aiKey: String? = null): String {
        val useAi = !aiKey.isNullOrBlank()
        val cacheKey = (if (useAi) "ai:" else "mt:") + text
        cache[cacheKey]?.let { return it }
        val out = if (useAi) translateWithAi(text, aiKey!!) else translateOffline(text)
        cache[cacheKey] = out
        return out
    }

    private suspend fun translateWithAi(text: String, key: String): String =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val body = org.json.JSONObject()
                .put("model", AI_MODEL)
                .put("max_tokens", 2048)
                .put("system", SYSTEM_PROMPT)
                .put(
                    "messages",
                    org.json.JSONArray().put(
                        org.json.JSONObject().put("role", "user").put("content", text)
                    ),
                )
                .toString()
            val request = okhttp3.Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .header("x-api-key", key)
                .header("anthropic-version", "2023-06-01")
                .post(
                    body.toRequestBody("application/json".toMediaType())
                )
                .build()
            aiClient.newCall(request).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    val msg = runCatching {
                        org.json.JSONObject(raw).getJSONObject("error").getString("message")
                    }.getOrDefault("HTTP ${resp.code}")
                    error(msg)
                }
                val content = org.json.JSONObject(raw).getJSONArray("content")
                buildString {
                    for (i in 0 until content.length()) {
                        val part = content.getJSONObject(i)
                        if (part.optString("type") == "text") append(part.getString("text"))
                    }
                }.trim()
            }
        }

    private suspend fun translateOffline(text: String): String {
        ensureModel()
        // Po odstavcích, ať se zachová členění textu.
        return text.split("\n").map { line ->
            if (line.isBlank()) line else translateLine(line)
        }.joinToString("\n")
    }

    private suspend fun ensureModel() = suspendCancellableCoroutine<Unit> { cont ->
        translator.downloadModelIfNeeded()
            .addOnSuccessListener { cont.resume(Unit) }
            .addOnFailureListener { cont.resumeWithException(it) }
    }

    private suspend fun translateLine(line: String) = suspendCancellableCoroutine<String> { cont ->
        translator.translate(line)
            .addOnSuccessListener { cont.resume(it) }
            .addOnFailureListener { cont.resumeWithException(it) }
    }
}

/**
 * Text s tlačítkem „Přeložit do češtiny" / „Originál".
 */
@Composable
fun TranslatableText(text: String, style: TextStyle = MaterialTheme.typography.bodySmall) {
    var translated by remember(text) { mutableStateOf<String?>(null) }
    var showCz by remember(text) { mutableStateOf(false) }
    var loading by remember(text) { mutableStateOf(false) }
    var error by remember(text) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val settings = remember {
        (context.applicationContext as com.example.antimarcipan.MtgApp).container.appSettings
    }
    val aiKey by settings.aiKey.collectAsState()
    val useAi = aiKey.isNotBlank()

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        SymbolText(if (showCz && translated != null) translated!! else text, style = style)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                onClick = {
                    if (showCz) { showCz = false; return@TextButton }
                    if (translated != null) { showCz = true; return@TextButton }
                    loading = true; error = null
                    scope.launch {
                        runCatching { CzechTranslator.translate(text, aiKey) }
                            .onSuccess { translated = it; showCz = true }
                            .onFailure {
                                error = if (useAi) "AI překlad selhal: ${it.message ?: "chyba sítě"}"
                                else "Překlad se nepovedl (napoprvé je potřeba internet pro stažení jazyka)."
                            }
                        loading = false
                    }
                },
                enabled = !loading,
            ) {
                if (loading) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.Translate, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                Text(
                    if (showCz) "  Originál"
                    else if (useAi) "  Přeložit do češtiny (AI)"
                    else "  Přeložit do češtiny"
                )
            }
        }
        if (showCz && !useAi) {
            Text(
                "Strojový překlad — pro přesný překlad pravidel zadej AI klíč v Nastavení.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
        error?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
        }
    }
}
