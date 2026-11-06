package com.example.antimarcipan.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.antimarcipan.R

private val ORDER = listOf("W", "U", "B", "R", "G")

/** Zobrazí barvy balíku jako ikony many (W/U/B/R/G), bezbarvé jako diamant. */
@Composable
fun ColorIdentityPips(colors: Set<String>, symbolSize: Int = 18) {
    val present = ORDER.filter { colors.contains(it) }
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        if (present.isEmpty()) {
            Image(
                painter = painterResource(R.drawable.mana_c),
                contentDescription = "bezbarvé",
                modifier = Modifier.size(symbolSize.dp),
            )
        } else {
            present.forEach { c ->
                Image(
                    painter = painterResource(pipRes(c)),
                    contentDescription = c,
                    modifier = Modifier.size(symbolSize.dp),
                )
            }
        }
    }
}

private fun pipRes(c: String): Int = when (c) {
    "W" -> R.drawable.mana_w
    "U" -> R.drawable.mana_u
    "B" -> R.drawable.mana_b
    "R" -> R.drawable.mana_r
    "G" -> R.drawable.mana_g
    else -> R.drawable.mana_c
}
