package com.example.antimarcipan.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.antimarcipan.ui.AppViewModelFactory
import com.example.antimarcipan.ui.CardThumb
import com.example.antimarcipan.ui.EmptyState
import com.example.antimarcipan.ui.ScreenTitle

@Composable
fun CollectionScreen(
    vm: CollectionViewModel = viewModel(factory = AppViewModelFactory),
) {
    val cards by vm.cards.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        ScreenTitle("Sbírka (${cards.sumOf { it.quantity }})")
        if (cards.isEmpty()) {
            EmptyState("Zatím prázdno", "Naskenuj první kartu v záložce Sken.")
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(cards, key = { it.scryfallId }) { card ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CardThumb(card.imageUrl)
                        Column(Modifier.weight(1f)) {
                            Text(card.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${card.setName ?: card.setCode ?: ""}  ·  ${card.typeLine ?: ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            )
                            card.priceUsd?.let {
                                Text("$$it  ·  ks: ${card.quantity}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        IconButton(onClick = { vm.remove(card.scryfallId) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Odebrat")
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
