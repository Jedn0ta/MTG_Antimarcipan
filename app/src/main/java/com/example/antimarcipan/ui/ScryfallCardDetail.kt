package com.example.antimarcipan.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.antimarcipan.data.KeywordGlossary
import com.example.antimarcipan.data.scryfall.ScryfallCard

/**
 * Náhled karty ze Scryfallu (není ve sbírce) — ťuknutím otevře detail
 * s velkým obrázkem a textací.
 */
@Composable
fun ScryfallThumb(card: ScryfallCard, width: Dp = 48.dp, height: Dp = 67.dp) {
    var open by remember { mutableStateOf(false) }
    CardThumb(
        model = card.imageUris?.small ?: card.cardFaces.firstOrNull()?.imageUris?.small ?: card.bestImageUrl,
        width = width,
        height = height,
        modifier = Modifier.clickable { open = true },
    )
    if (open) ScryfallCardDetailDialog(card) { open = false }
}

/** Detail karty: obrázek (u oboustranných přepínání stran) + textace všech stran. */
@Composable
fun ScryfallCardDetailDialog(card: ScryfallCard, onDismiss: () -> Unit) {
    // Strany karty: buď jedna (běžná karta), nebo víc (DFC / split / adventure).
    data class Face(val name: String, val mana: String?, val type: String?, val text: String?, val image: String?)
    val faces = remember(card.id) {
        if (card.cardFaces.isNotEmpty()) {
            card.cardFaces.map {
                Face(
                    name = it.name ?: card.name,
                    mana = it.manaCost,
                    type = it.typeLine,
                    text = it.oracleText,
                    image = it.imageUris?.normal ?: card.imageUris?.normal,
                )
            }
        } else {
            listOf(Face(card.name, card.manaCost, card.typeLine, card.oracleText, card.imageUris?.normal))
        }
    }
    var imageIndex by remember(card.id) { mutableIntStateOf(0) }
    val images = faces.mapNotNull { it.image }.distinct()

    com.example.antimarcipan.ui.GoldDialog(
        onDismissRequest = onDismiss,
        title = { Text(card.name) },
        text = {
            Column(
                Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                images.getOrNull(imageIndex)?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = card.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = images.size > 1) {
                                imageIndex = (imageIndex + 1) % images.size
                            },
                    )
                }
                if (images.size > 1) {
                    Text(
                        "Ťukni na obrázek pro otočení karty",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
                Text(
                    listOfNotNull(card.setName, card.rarity?.replaceFirstChar { it.uppercase() })
                        .joinToString("  ·  "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                faces.forEachIndexed { i, f ->
                    if (i > 0) HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    if (faces.size > 1) Text(f.name, style = MaterialTheme.typography.titleSmall)
                    f.mana?.takeIf { it.isNotBlank() }?.let { ManaCost(it, symbolSize = 18) }
                    f.type?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    KeywordGlossary.annotate(f.text)?.takeIf { it.isNotBlank() }?.let {
                        TranslatableText(it, MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Zavřít") } },
    )
}
