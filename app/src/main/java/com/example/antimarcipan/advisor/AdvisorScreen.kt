package com.example.antimarcipan.advisor

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.antimarcipan.data.advisor.AdvisorTip
import com.example.antimarcipan.data.advisor.TipLevel
import com.example.antimarcipan.ui.AppViewModelFactory
import com.example.antimarcipan.ui.EmptyState

@Composable
fun AdvisorScreen(
    vm: AdvisorViewModel = viewModel(factory = AppViewModelFactory),
) {
    val decks by vm.decks.collectAsStateWithLifecycle()
    val state by vm.state.collectAsStateWithLifecycle()
    var selectedDeckId by remember { mutableStateOf<Long?>(null) }

    Column(Modifier.fillMaxSize()) {
        Text(
            "Rozbor balíku a rady při hře.",
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            decks.forEach { deck ->
                FilterChip(
                    selected = selectedDeckId == deck.id,
                    onClick = {
                        selectedDeckId = deck.id
                        vm.analyzeDeck(deck.id)
                    },
                    label = { Text(deck.name) },
                )
            }
        }

        HorizontalDivider(Modifier.padding(bottom = 8.dp))

        when {
            decks.isEmpty() -> EmptyState(
                "Žádné balíky",
                "Vytvoř balík v záložce Balíky a Anti Marcipán ho rozebere.",
            )
            selectedDeckId == null -> EmptyState(
                "Vyber balík",
                "Anti Marcipán zkontroluje zem, ramp, draw, removal, křivku a dá rady do hry.",
            )
            !state.analyzed -> EmptyState("Počítám…", "Chvilku strpení.")
            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(state.tips) { tip ->
                    TipCard(tip)
                }
            }
        }
    }
}

@Composable
private fun TipCard(tip: AdvisorTip) {
    val (icon, tint) = when (tip.level) {
        TipLevel.GOOD -> Icons.Filled.CheckCircle to Color(0xFF2E9E5B)
        TipLevel.WARN -> Icons.Filled.Warning to Color(0xFFD98324)
        TipLevel.TIP -> Icons.Filled.Lightbulb to MaterialTheme.colorScheme.primary
    }
    Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(icon, contentDescription = null, tint = tint)
            Column {
                Text(tip.title, style = MaterialTheme.typography.titleSmall)
                Text(
                    tip.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                )
            }
        }
    }
}
