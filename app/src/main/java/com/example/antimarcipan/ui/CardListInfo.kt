package com.example.antimarcipan.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.antimarcipan.data.local.CollectionCard

/**
 * Jednotné zobrazení informací o kartě (interaktivní náhled, jméno,
 * edice, manacost) — používá se ve sbírce i v balíku.
 */
@Composable
fun CardListInfo(
    card: CollectionCard,
    modifier: Modifier = Modifier,
    deckQuantity: Int? = null,
    thumbWidth: Int = 60,
    thumbHeight: Int = 84,
    onLongHold: (() -> Unit)? = null,
) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InteractiveThumb(card, width = thumbWidth.dp, height = thumbHeight.dp)

        val colMod = if (onLongHold != null) Modifier.weight(1f).longHold(2500L, onLongHold)
        else Modifier.weight(1f)
        Column(colMod) {
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    card.setName ?: card.setCode?.uppercase() ?: "",
                    Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                card.priceLabel()?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color(0xFF8FD6A0),
                    )
                }
            }
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
