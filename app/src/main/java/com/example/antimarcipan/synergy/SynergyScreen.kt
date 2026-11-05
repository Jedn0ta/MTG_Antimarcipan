package com.example.antimarcipan.synergy

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.antimarcipan.data.SynergyRecommendation
import com.example.antimarcipan.ui.AppViewModelFactory
import com.example.antimarcipan.ui.EmptyState
import com.example.antimarcipan.ui.ScreenTitle
import kotlin.math.roundToInt

@Composable
fun SynergyScreen(
    vm: SynergyViewModel = viewModel(factory = AppViewModelFactory),
) {
    val decks by vm.decks.collectAsStateWithLifecycle()
    val state by vm.state.collectAsStateWithLifecycle()
    var selectedDeckId by remember { mutableStateOf<Long?>(null) }

    Column(Modifier.fillMaxSize()) {
        ScreenTitle("Synergie (EDHREC)")

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            decks.forEach { deck ->
                FilterChip(
                    selected = selectedDeckId == deck.id,
                    onClick = {
                        selectedDeckId = deck.id
                        vm.loadForDeck(deck.id)
                    },
                    label = { Text(deck.name) },
                )
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        when {
            decks.isEmpty() -> EmptyState(
                "Žádné balíky",
                "Nejdřív si vytvoř balík a nastav velitele v záložce Balíky.",
            )
            selectedDeckId == null -> EmptyState(
                "Vyber balík",
                "Pro vybraný balík načtu doporučené karty podle velitele.",
            )
            state.loading -> Row(
                Modifier.fillMaxWidth().padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator()
                Text("Načítám synergie pro ${state.commanderName}…")
            }
            state.error != null -> EmptyState("Nepovedlo se", state.error!!)
            else -> {
                Text(
                    "Velitel: ${state.commanderName}  ·  seřazeno podle synergie",
                    Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
                LazyColumn(Modifier.fillMaxSize()) {
                    items(state.recommendations, key = { it.name }) { rec ->
                        RecRow(rec)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun RecRow(rec: SynergyRecommendation) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val pct = (rec.synergy * 100).roundToInt()
        Text(
            (if (pct >= 0) "+$pct%" else "$pct%"),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (rec.synergy >= 0) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.padding(end = 4.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(rec.name, style = MaterialTheme.typography.bodyMedium)
            Text(
                rec.category + (if (rec.owned) "  ·  máš ve sbírce ✓" else ""),
                style = MaterialTheme.typography.bodySmall,
                color = if (rec.owned) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}
