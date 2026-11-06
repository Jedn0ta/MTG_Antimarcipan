package com.example.antimarcipan.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.antimarcipan.data.local.CollectionCard
import com.example.antimarcipan.data.thumbModel

/**
 * Jednotné zobrazení informací o kartě (náhled s tooltipem textu, jméno,
 * edice, manacost, rarita) — používá se ve sbírce i v balíku.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardListInfo(
    card: CollectionCard,
    modifier: Modifier = Modifier,
    deckQuantity: Int? = null,
    thumbWidth: Int = 60,
    thumbHeight: Int = 84,
) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val tooltipState = rememberTooltipState(isPersistent = true)
        TooltipBox(
            positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
            tooltip = {
                RichTooltip(
                    title = { Text(card.name) },
                    text = {
                        Text(
                            buildString {
                                card.manaCost?.takeIf { it.isNotBlank() }?.let { appendLine(it) }
                                card.typeLine?.let { appendLine(it) }
                                card.oracleText?.let { append(it) }
                            }.ifBlank { "Bez textu." }
                        )
                    },
                )
            },
            state = tooltipState,
        ) {
            CardThumb(card.thumbModel(), width = thumbWidth.dp, height = thumbHeight.dp)
        }

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    card.name,
                    Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (deckQuantity != null && deckQuantity > 1) {
                    Text(
                        "×$deckQuantity",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Text(
                card.setName ?: card.setCode?.uppercase() ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    card.typeLine ?: "",
                    Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                ManaCost(card.manaCost, symbolSize = 15)
            }
        }
    }
}
