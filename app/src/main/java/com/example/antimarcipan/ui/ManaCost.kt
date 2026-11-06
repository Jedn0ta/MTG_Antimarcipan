package com.example.antimarcipan.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.antimarcipan.R

/** Vykreslí manacost (např. "{2}{G}{G}") jako řadu ikon many. */
@Composable
fun ManaCost(cost: String?, symbolSize: Int = 16) {
    if (cost.isNullOrBlank()) return
    val tokens = Regex("\\{([^}]+)\\}").findAll(cost).map { it.groupValues[1] }.toList()
    if (tokens.isEmpty()) return

    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        tokens.forEach { raw ->
            val sym = raw.uppercase()
            val pip = pipRes(sym)
            if (pip != null) {
                Image(
                    painter = painterResource(pip),
                    contentDescription = sym,
                    modifier = Modifier.size(symbolSize.dp),
                )
            } else {
                // Generická mana (čísla, X) nebo hybrid → kolečko s textem.
                Box(
                    Modifier.size(symbolSize.dp).clip(CircleShape).background(Color(0xFFCDC6BB)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        raw.replace("/", ""),
                        fontSize = (symbolSize * 0.6).sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3A3A3A),
                    )
                }
            }
        }
    }
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
