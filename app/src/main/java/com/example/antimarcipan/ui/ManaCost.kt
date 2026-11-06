package com.example.antimarcipan.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.example.antimarcipan.R

/**
 * Vykreslí manacost (např. "{2}{G}{G}", "{W/U}") jako řadu ikon many.
 * Karty hratelné více způsoby (split / modal / fuse) mají cenu oddělenou
 * " // " — každou část vykreslíme zvlášť a mezi ně dáme oddělovač „//“.
 */
@Composable
fun ManaCost(cost: String?, symbolSize: Int = 16) {
    if (cost.isNullOrBlank()) return
    // Rozdělíme na části podle "//" (split / oboustranné / fuse karty).
    val parts = cost.split("//").map { it.trim() }.filter { it.isNotEmpty() }
    if (parts.isEmpty()) return

    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        parts.forEachIndexed { index, part ->
            if (index > 0) {
                Text(
                    "//",
                    style = TextStyle(
                        fontSize = (symbolSize * 0.72).sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
            ManaCostPart(part, symbolSize)
        }
    }
}

@Composable
private fun ManaCostPart(cost: String, symbolSize: Int) {
    val tokens = Regex("\\{([^}]+)\\}").findAll(cost).map { it.groupValues[1] }.toList()
    if (tokens.isEmpty()) return

    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        tokens.forEach { raw ->
            val sym = raw.uppercase()
            val pip = pipRes(sym)
            when {
                pip != null -> Image(
                    painter = painterResource(pip),
                    contentDescription = sym,
                    modifier = Modifier.size(symbolSize.dp),
                )
                // Hybridní / více možností (obsahuje "/") — necháme lomítko
                raw.contains("/") -> Box(
                    Modifier
                        .height(symbolSize.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFCDC6BB))
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    GlyphText(sym, symbolSize)
                }
                // Generická mana (číslo, X) — vycentrované kolečko
                else -> Box(
                    Modifier.size(symbolSize.dp).clip(CircleShape).background(Color(0xFFCDC6BB)),
                    contentAlignment = Alignment.Center,
                ) {
                    GlyphText(sym, symbolSize)
                }
            }
        }
    }
}

@Composable
private fun GlyphText(text: String, symbolSize: Int) {
    Text(
        text = text,
        style = TextStyle(
            fontSize = (symbolSize * 0.62).sp,
            lineHeight = (symbolSize * 0.62).sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2A2A2A),
            textAlign = TextAlign.Center,
        ),
        maxLines = 1,
    )
}

private fun pipRes(sym: String): Int? = when (sym) {
    "W" -> R.drawable.mana_w
    "U" -> R.drawable.mana_u
    "B" -> R.drawable.mana_b
    "R" -> R.drawable.mana_r
    "G" -> R.drawable.mana_g
    "C" -> R.drawable.mana_c
    else -> null
}
