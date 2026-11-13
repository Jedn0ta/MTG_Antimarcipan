package com.example.antimarcipan.synergy

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.antimarcipan.data.SynergyRecommendation
import com.example.antimarcipan.data.synergy.CardFit
import com.example.antimarcipan.ui.AppViewModelFactory
import com.example.antimarcipan.ui.EmptyState

@Composable
fun SynergyScreen(
    vm: SynergyViewModel = viewModel(factory = AppViewModelFactory),
    advisorVm: com.example.antimarcipan.advisor.AdvisorViewModel = viewModel(factory = AppViewModelFactory),
    onBack: (() -> Unit)? = null,
) {
    val adv by advisorVm.state.collectAsStateWithLifecycle()
    val decks by vm.decks.collectAsStateWithLifecycle()
    val state by vm.state.collectAsStateWithLifecycle()
    var selectedDeckId by remember { mutableStateOf<Long?>(null) }

    Column(Modifier.fillMaxSize()) {
        if (onBack != null) com.example.antimarcipan.advisor.HubBackRow("Analýza balíku", onBack)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            decks.forEach { deck ->
                com.example.antimarcipan.ui.GoldChip(
                    selected = selectedDeckId == deck.id,
                    onClick = { selectedDeckId = deck.id; vm.analyzeDeck(deck.id); advisorVm.analyzeDeck(deck.id) },
                    label = { Text(deck.name) },
                )
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        when {
            decks.isEmpty() -> EmptyState("Žádné balíky", "Nejdřív si vytvoř balík v záložce Balíky.")
            selectedDeckId == null -> EmptyState(
                "Vyber balík",
                "Poradím s balíkem: rady, jak karty sedí, co nesedí a co doplnit.",
            )
            state.loading -> Row(
                Modifier.fillMaxWidth().padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) { CircularProgressIndicator(); Text("Analyzuji „${state.deckName}“…") }
            state.error != null -> EmptyState("Nepovedlo se", state.error!!)
            state.report != null -> {
                val report = state.report!!
                LazyColumn(Modifier.fillMaxSize()) {
                    if (adv.curve.any { it.first + it.second > 0 }) item {
                        com.example.antimarcipan.ui.ManaCurveCard(adv.curve, adv.avgMv, adv.lands)
                    }
                    item { ThemeSummary(report.colors, report.topThemes, report.topTribes, report.topKeywords) }
                    if (adv.tips.isNotEmpty()) {
                        item { SectionTitle("Zdraví balíku") }
                        items(adv.tips) { TipCard(it) }
                    }

                    if (state.tips.isNotEmpty()) {
                        item { SectionTitle("Rady k balíku") }
                        items(state.tips) { TipCard(it) }
                    }

                    if (report.misfits.isNotEmpty()) {
                        item { SectionTitle("Zvaž výměnu (${report.misfits.size})") }
                        items(report.misfits) { FitRow(it, state.replacements[it.card.scryfallId], state.images) }
                    }

                    item { SectionTitle("Všechny karty podle vhodnosti") }
                    items(report.fits) { FitRow(it) }

                    if (state.suggestions.isNotEmpty()) {
                        item { SectionTitle("Návrhy karet · ${state.suggestionsSource}") }
                        items(state.suggestions) { RecRow(it, state.images) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeSummary(colors: Set<String>, themes: List<String>, tribes: List<String>, keywords: List<String>) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        val parts = buildList {
            if (themes.isNotEmpty()) add("Témata: ${themes.joinToString(", ")}")
            if (tribes.isNotEmpty()) add("Tribaly: ${tribes.joinToString(", ")}")
            if (keywords.isNotEmpty()) add("Klíčová slova: ${keywords.joinToString(", ")}")
        }
        if (parts.isEmpty()) {
            Text("Balík nemá výrazné téma — návrhy budou spíš podle barev.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        } else {
            parts.forEach {
                Text(it, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
            }
        }
    }
    HorizontalDivider()
}

@Composable
private fun SectionTitle(text: String) {
    com.example.antimarcipan.ui.OrnamentHeader(text)
}

@Composable
private fun FitRow(fit: CardFit, replacements: List<String>? = null, images: Map<String, com.example.antimarcipan.data.scryfall.ScryfallCard> = emptyMap()) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val color = when {
                fit.offColor -> Color(0xFFD95C5C)
                fit.score >= 65 -> Color(0xFF2E9E5B)
                fit.score >= 45 -> Color(0xFFD9A441)
                else -> Color(0xFFD98324)
            }
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(color.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("${fit.score}", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold, color = color, textAlign = TextAlign.Center)
            }
            // Náhled + info/celá karta (ťuk = info, podržení = celá karta)
            com.example.antimarcipan.ui.InteractiveThumb(card = fit.card, width = 40.dp, height = 56.dp)
            Column(Modifier.weight(1f)) {
                Text(fit.card.name, style = MaterialTheme.typography.bodyMedium)
                Text(
                    fit.reasons.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
        }

        if (!replacements.isNullOrEmpty()) {
            Text(
                "Vhodné náhrady:",
                Modifier.padding(start = 48.dp, top = 6.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                Modifier.fillMaxWidth().padding(start = 48.dp, top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                replacements.take(3).forEach { name ->
                    Column(
                        Modifier.width(64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        SuggestionImage(name, images[name.lowercase()])
                        Text(
                            name,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 2,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        )
                    }
                }
            }
        }
    }
    HorizontalDivider()
}

/** Náhled navrhované karty podle jména (obrázek ze Scryfallu), ťuk = zvětšení. */
@Composable
private fun SuggestionImage(name: String, card: com.example.antimarcipan.data.scryfall.ScryfallCard?) {
    if (card != null) {
        // ťuk = detail karty s obrázkem a textací
        com.example.antimarcipan.ui.ScryfallThumb(card, width = 40.dp, height = 56.dp)
    } else {
        com.example.antimarcipan.ui.CardThumb(null, width = 40.dp, height = 56.dp)
    }
}

private fun scryfallImageUrl(name: String, version: String): String {
    val enc = java.net.URLEncoder.encode(name, "UTF-8")
    return "https://api.scryfall.com/cards/named?exact=$enc&format=image&version=$version"
}

@Composable
private fun TipCard(tip: com.example.antimarcipan.data.advisor.AdvisorTip) {
    val icon = when (tip.level) {
        com.example.antimarcipan.data.advisor.TipLevel.GOOD -> Icons.Filled.CheckCircle
        com.example.antimarcipan.data.advisor.TipLevel.WARN -> Icons.Filled.Warning
        com.example.antimarcipan.data.advisor.TipLevel.TIP -> Icons.Filled.Lightbulb
    }
    val tint = when (tip.level) {
        com.example.antimarcipan.data.advisor.TipLevel.GOOD -> Color(0xFF2E9E5B)
        com.example.antimarcipan.data.advisor.TipLevel.WARN -> Color(0xFFD98324)
        com.example.antimarcipan.data.advisor.TipLevel.TIP -> MaterialTheme.colorScheme.primary
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Column(Modifier.weight(1f)) {
            Text(tip.title, style = MaterialTheme.typography.titleSmall)
            Text(
                tip.detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            )
        }
    }
    HorizontalDivider()
}

@Composable
private fun RecRow(rec: SynergyRecommendation, images: Map<String, com.example.antimarcipan.data.scryfall.ScryfallCard>) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SuggestionImage(rec.name, images[rec.name.lowercase()])
        Column(Modifier.weight(1f)) {
            Text(rec.name, style = MaterialTheme.typography.bodyMedium)
            Text(
                (if (rec.synergy != 0.0) "synergie ${(rec.synergy * 100).toInt()}% · " else "") +
                    rec.category + (if (rec.owned) "  ·  máš ve sbírce ✓" else ""),
                style = MaterialTheme.typography.bodySmall,
                color = if (rec.owned) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
    HorizontalDivider()
}
