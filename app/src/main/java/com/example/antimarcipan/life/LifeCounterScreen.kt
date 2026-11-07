package com.example.antimarcipan.life

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** Stav hráče — drží se v paměti mimo obrazovku, takže přežije přepínání v appce. */
class LifePlayer(life: Int) {
    var life by mutableIntStateOf(life)
    var poison by mutableIntStateOf(0)
    var delta by mutableIntStateOf(0)
    var deltaTick by mutableIntStateOf(0)
}

object LifeState {
    var count by mutableIntStateOf(4)
    var startLife by mutableIntStateOf(20)
    var starter by mutableStateOf<Int?>(null)
    val players = List(6) { LifePlayer(20) }
    private var loaded = false

    fun load(ctx: Context) {
        if (loaded) return
        loaded = true
        val p = ctx.getSharedPreferences("life_counter", Context.MODE_PRIVATE)
        count = p.getInt("count", 4)
        startLife = p.getInt("start", 20)
        players.forEachIndexed { i, pl ->
            pl.life = p.getInt("life$i", startLife)
            pl.poison = p.getInt("poison$i", 0)
        }
    }

    fun save(ctx: Context) {
        val e = ctx.getSharedPreferences("life_counter", Context.MODE_PRIVATE).edit()
        e.putInt("count", count).putInt("start", startLife)
        players.forEachIndexed { i, pl -> e.putInt("life$i", pl.life).putInt("poison$i", pl.poison) }
        e.apply()
    }

    fun reset() {
        players.forEach { it.life = startLife; it.poison = 0; it.delta = 0 }
        starter = null
    }
}

private val PLAYER_COLORS = listOf(
    Color(0xFF8E2C3A), Color(0xFF2B4F8E), Color(0xFF2F7A4C),
    Color(0xFF6A3E9A), Color(0xFFA8742A), Color(0xFF237A7A),
)

/** Otočí obsah o 90° i s prohozením rozměrů (hráči sedící po stranách stolu). */
private fun Modifier.sideways(deg: Float): Modifier =
    if (deg == 0f || deg == 180f) this.rotate(deg)
    else this.layout { m, c ->
        val p = m.measure(c.copy(minWidth = c.minHeight, maxWidth = c.maxHeight, minHeight = c.minWidth, maxHeight = c.maxWidth))
        layout(p.height, p.width) { p.place((p.height - p.width) / 2, (p.width - p.height) / 2) }
    }.rotate(deg)

@Composable
fun LifeCounterScreen() {
    val ctx = LocalContext.current
    LifeState.load(ctx)
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false; LifeState.save(ctx) }
    }
    val n = LifeState.count
    val save = { LifeState.save(ctx) }

    Column(Modifier.fillMaxSize().background(Color(0xFF0E0B1C))) {
        // Ovládání
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Hráči", style = MaterialTheme.typography.labelMedium)
            (2..6).forEach { k ->
                FilterChip(selected = n == k, onClick = { LifeState.count = k; LifeState.starter = null; save() }, label = { Text("$k") })
            }
            Text("  Životy", style = MaterialTheme.typography.labelMedium)
            listOf(20, 30, 40).forEach { l ->
                FilterChip(
                    selected = LifeState.startLife == l,
                    onClick = { LifeState.startLife = l; LifeState.reset(); save() },
                    label = { Text("$l") },
                )
            }
            TextButton(onClick = { LifeState.starter = (0 until n).random() }) { Text("Kdo začne?") }
            IconButton(onClick = { LifeState.reset(); save() }) {
                Icon(Icons.Filled.Refresh, contentDescription = "Nová hra")
            }
        }

        // Rozložení hráčů kolem stolu
        Column(Modifier.fillMaxSize().padding(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (n == 2) {
                Tile(0, 180f, Modifier.weight(1f).fillMaxWidth(), save)
                Tile(1, 0f, Modifier.weight(1f).fillMaxWidth(), save)
            } else {
                val pairs = n / 2
                val odd = n % 2 == 1
                if (odd) Tile(n - 1, 180f, Modifier.weight(0.8f).fillMaxWidth(), save)
                repeat(pairs) { r ->
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Tile(r * 2, 90f, Modifier.weight(1f).fillMaxHeight(), save)
                        Tile(r * 2 + 1, -90f, Modifier.weight(1f).fillMaxHeight(), save)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Tile(index: Int, rotation: Float, modifier: Modifier, save: () -> Unit) {
    val p = LifeState.players[index]
    val color = PLAYER_COLORS[index]
    val dead = p.life <= 0 || p.poison >= 10
    val isStarter = LifeState.starter == index

    fun change(d: Int) {
        p.life += d
        p.delta += d
        p.deltaTick++
        save()
    }
    LaunchedEffect(p.deltaTick) {
        delay(1600)
        p.delta = 0
    }

    Box(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    if (dead) listOf(Color(0xFF2A2A2A), Color(0xFF151515))
                    else listOf(color, color.copy(alpha = 0.55f).compositeOver(Color(0xFF120E22)))
                )
            )
            .border(
                if (isStarter) 3.dp else 1.dp,
                Brush.linearGradient(listOf(Color(0xFFF6DE9A), Color(0xFFB98A35))),
                RoundedCornerShape(18.dp),
            ),
    ) {
        Box(Modifier.fillMaxSize().sideways(rotation)) {
            Row(Modifier.fillMaxSize()) {
                TapZone("−", { change(-1) }, { change(-5) })
                TapZone("+", { change(1) }, { change(5) })
            }
            Column(
                Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.padding(bottom = 2.dp)) {
                    AnimatedVisibility(p.delta != 0, enter = fadeIn(), exit = fadeOut()) {
                        Text(
                            (if (p.delta > 0) "+" else "") + p.delta,
                            color = if (p.delta > 0) Color(0xFFB6F2C4) else Color(0xFFFFB4B4),
                            fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        )
                    }
                    if (p.delta == 0) Text(" ", fontSize = 18.sp)
                }
                Text(
                    "${p.life}",
                    color = Color.White,
                    fontSize = if (LifeState.count <= 2) 96.sp else 64.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    if (isStarter) "Hráč ${index + 1} · začíná" else "Hráč ${index + 1}",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelMedium,
                )
                // Jed: ťuk +1, podržet −1
                Text(
                    "☠ ${p.poison}",
                    Modifier
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.3f))
                        .combinedClickable(
                            onClick = { p.poison++; save() },
                            onLongClick = { if (p.poison > 0) { p.poison--; save() } },
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    color = if (p.poison >= 10) Color(0xFFB6F2C4) else Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowScope.TapZone(label: String, onTap: () -> Unit, onHold: () -> Unit) {
    Box(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .combinedClickable(onClick = onTap, onLongClick = onHold),
        contentAlignment = if (label == "+") Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Text(
            label,
            Modifier.padding(horizontal = 18.dp),
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 36.sp,
        )
    }
}

private fun Color.compositeOver(bg: Color): Color {
    val a = alpha
    return Color(red * a + bg.red * (1 - a), green * a + bg.green * (1 - a), blue * a + bg.blue * (1 - a), 1f)
}
