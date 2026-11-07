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

    suspend fun translate(text: String): String {
        cache[text]?.let { return it }
        ensureModel()
        // Po odstavcích, ať se zachová členění textu.
        val out = text.split("\n").map { line ->
            if (line.isBlank()) line else translateLine(line)
        }.joinToString("\n")
        cache[text] = out
        return out
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

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(if (showCz && translated != null) translated!! else text, style = style)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                onClick = {
                    if (showCz) { showCz = false; return@TextButton }
                    if (translated != null) { showCz = true; return@TextButton }
                    loading = true; error = null
                    scope.launch {
                        runCatching { CzechTranslator.translate(text) }
                            .onSuccess { translated = it; showCz = true }
                            .onFailure { error = "Překlad se nepovedl (napoprvé je potřeba internet pro stažení jazyka)." }
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
                Text(if (showCz) "  Originál" else "  Přeložit do češtiny")
            }
        }
        error?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
        }
    }
}
