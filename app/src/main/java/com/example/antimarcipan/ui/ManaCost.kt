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
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.text.appendInlineContent
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

// ---------------------------------------------------------------------------
// Jednotlivý MTG symbol ({W}, {2}, {X}, {T}, {Q}, {W/U}, {G/P}, {S}, {E}…)
// ---------------------------------------------------------------------------

private val SYMBOL_BG = mapOf(
    "W" to Color(0xFFF8F6D8), "U" to Color(0xFFC1D7E9), "B" to Color(0xFFBAB1AB),
    "R" to Color(0xFFE49977), "G" to Color(0xFFA3C095), "C" to Color(0xFFCCC2C0),
)

/** Vykreslí jeden symbol many / akce jako ikonu. [token] je obsah závorek, např. "2", "T", "W/U". */
@Composable
fun ManaSymbol(token: String, size: androidx.compose.ui.unit.Dp) {
    val sym = token.uppercase()
    val px = size.value.toInt()
    val pip = pipRes(sym)
    when {
        pip != null -> Image(painterResource(pip), contentDescription = sym, modifier = Modifier.size(size))
        sym == "T" -> Image(painterResource(R.drawable.ic_sym_tap), contentDescription = "tap", modifier = Modifier.size(size))
        sym == "Q" -> Image(painterResource(R.drawable.ic_sym_untap), contentDescription = "untap", modifier = Modifier.size(size))
        sym.contains("/") -> {
            // Hybridní ({W/U}, {2/W}) nebo phyrexian ({G/P})
            val (a, b) = sym.split("/").let { it[0] to it.getOrElse(1) { "" } }
            if (b == "P") {
                Box(
                    Modifier.size(size).clip(CircleShape).background(SYMBOL_BG[a] ?: Color(0xFFCDC6BB)),
                    contentAlignment = Alignment.Center,
                ) { GlyphText("Φ", px) }
            } else {
                val ca = SYMBOL_BG[a] ?: Color(0xFFCDC6BB)
                val cb = SYMBOL_BG[b] ?: Color(0xFFCDC6BB)
                Box(
                    Modifier.size(size).clip(CircleShape).drawBehind {
                        drawRect(ca, size = androidx.compose.ui.geometry.Size(this.size.width / 2f, this.size.height))
                        drawRect(
                            cb,
                            topLeft = androidx.compose.ui.geometry.Offset(this.size.width / 2f, 0f),
                            size = androidx.compose.ui.geometry.Size(this.size.width / 2f, this.size.height),
                        )
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    // čísla u {2/W} ukážeme, barvy jsou poznat z půlek
                    if (a.toIntOrNull() != null) GlyphText(a, px)
                }
            }
        }
        sym == "S" -> Box(
            Modifier.size(size).clip(CircleShape).background(Color(0xFFDDE7EE)),
            contentAlignment = Alignment.Center,
        ) { GlyphText("❄", px) }
        else -> Box(
            Modifier.size(size).clip(CircleShape).background(Color(0xFFCDC6BB)),
            contentAlignment = Alignment.Center,
        ) { GlyphText(sym, px) }
    }
}

/** Začátek / konec doplňku od aplikace (vysvětlivky hesel) — kreslí se jinou barvou. */
const val NOTE_START = ''
const val NOTE_END = ''

/**
 * Text karty s MTG symboly místo „{T}“, „{2}{G}“ a s odlišně obarvenými
 * doplňky od aplikace (mezi [NOTE_START] a [NOTE_END]).
 */
@Composable
fun SymbolText(
    text: String,
    style: TextStyle = androidx.compose.material3.MaterialTheme.typography.bodySmall,
    noteColor: Color = Color(0xFFB9A7EC),
) {
    val tokenRegex = remember { Regex("\\{([^}]{1,6})\\}") }
    val tokens = remember(text) { tokenRegex.findAll(text).map { it.groupValues[1] }.distinct().toList() }
    val annotated = remember(text, noteColor) {
        androidx.compose.ui.text.buildAnnotatedString {
            var inNote = false
            var i = 0
            while (i < text.length) {
                val ch = text[i]
                when {
                    ch == NOTE_START -> { inNote = true; i++ }
                    ch == NOTE_END -> { inNote = false; i++ }
                    ch == '{' -> {
                        val m = tokenRegex.find(text, i)?.takeIf { it.range.first == i }
                        if (m != null) {
                            appendInlineContent("sym:${m.groupValues[1]}", m.value)
                            i += m.value.length
                        } else { append(ch); i++ }
                    }
                    else -> {
                        if (inNote) {
                            withStyle(
                                androidx.compose.ui.text.SpanStyle(
                                    color = noteColor,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                )
                            ) { append(ch) }
                        } else append(ch)
                        i++
                    }
                }
            }
        }
    }
    val em = 1.15f
    val inline = tokens.associate { t ->
        "sym:$t" to androidx.compose.foundation.text.InlineTextContent(
            androidx.compose.ui.text.Placeholder(
                width = androidx.compose.ui.unit.TextUnit(em, androidx.compose.ui.unit.TextUnitType.Em),
                height = androidx.compose.ui.unit.TextUnit(em, androidx.compose.ui.unit.TextUnitType.Em),
                placeholderVerticalAlign = androidx.compose.ui.text.PlaceholderVerticalAlign.TextCenter,
            )
        ) {
            val sizeDp = with(androidx.compose.ui.platform.LocalDensity.current) {
                (style.fontSize.takeIf { it != androidx.compose.ui.unit.TextUnit.Unspecified }
                    ?: 14.sp).toDp() * em
            }
            ManaSymbol(t, sizeDp)
        }
    }
    androidx.compose.material3.Text(annotated, style = style, inlineContent = inline)
}
