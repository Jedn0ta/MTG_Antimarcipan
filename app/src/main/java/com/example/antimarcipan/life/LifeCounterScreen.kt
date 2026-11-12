package com.example.antimarcipan.life

import android.content.Context
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
import androidx.compose.foundation.layout.size
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
    /** 0 = nic, 1 = den, 2 = noc */
    var dayNight by mutableIntStateOf(0)
    val players = List(6) { LifePlayer(20) }
    /** Celá obrazovka — skryje horní lištu aplikace i systémové lišty. */
    var fullscreen by mutableStateOf(false)
    private var loaded = false

    fun load(ctx: Context) {
        if (loaded) return
        loaded = true
        val p = ctx.getSharedPreferences("life_counter", Context.MODE_PRIVATE)
        count = p.getInt("count", 4)
        startLife = p.getInt("start", 20)
        dayNight = p.getInt("dayNight", 0)
        players.forEachIndexed { i, pl ->
            pl.life = p.getInt("life$i", startLife)
            pl.poison = p.getInt("poison$i", 0)
        }
    }

    fun save(ctx: Context) {
        val e = ctx.getSharedPreferences("life_counter", Context.MODE_PRIVATE).edit()
        e.putInt("count", count).putInt("start", startLife).putInt("dayNight", dayNight)
        players.forEachIndexed { i, pl -> e.putInt("life$i", pl.life).putInt("poison$i", pl.poison) }
        e.apply()
    }

    fun reset() {
        players.forEach { it.life = startLife; it.poison = 0; it.delta = 0 }
        dayNight = 0
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
    val fs = LifeState.fullscreen
    DisposableEffect(fs) {
        val window = (ctx as? android.app.Activity)?.window
        val ctl = window?.let { androidx.core.view.WindowCompat.getInsetsController(it, view) }
        if (fs) {
            ctl?.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            ctl?.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        } else ctl?.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        onDispose { ctl?.show(androidx.core.view.WindowInsetsCompat.Type.systemBars()) }
    }
    androidx.activity.compose.BackHandler(enabled = fs) { LifeState.fullscreen = false }
    val n = LifeState.count
    val save = { LifeState.save(ctx) }

    Column(Modifier.fillMaxSize().background(Color(0xFF0E0B1C))) {
        // Rozložení hráčů kolem stolu + kulaté tlačítko nastavení uprostřed
        var showMenu by remember { mutableStateOf(false) }
        Box(Modifier.fillMaxSize()) {
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
        CenterButton(Modifier.align(Alignment.Center)) { showMenu = true }
        }
        if (showMenu) LifeMenu(onDismiss = { showMenu = false }, save = save)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Tile(index: Int, rotation: Float, modifier: Modifier, save: () -> Unit) {
    val p = LifeState.players[index]
    val color = PLAYER_COLORS[index]
    val dead = p.life <= 0 || p.poison >= 10
    val isStarter = false

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
                    androidx.compose.animation.AnimatedVisibility(visible = p.delta != 0, enter = fadeIn(), exit = fadeOut()) {
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
                    "Hráč ${index + 1}",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelMedium,
                )
                // Jed: ťuk +1, podržet −1
                Text(
                    "☠ ${p.poison}",
                    Modifier
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.35f))
                        .combinedClickable(
                            onClick = { p.poison++; save() },
                            onLongClick = { if (p.poison > 0) { p.poison--; save() } },
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 22.dp, vertical = 10.dp),
                    color = if (p.poison >= 10) Color(0xFFB6F2C4) else Color.White.copy(alpha = 0.9f),
                    fontSize = 22.sp, fontWeight = FontWeight.Bold,
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

@Composable
private fun <T> Dropdown(label: String, options: List<Pair<String, T>>, modifier: Modifier, onPick: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        androidx.compose.material3.OutlinedButton(
            onClick = { open = true },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, com.example.antimarcipan.ui.GoldBorderBrush),
        ) {
            Text("$label ▾", color = Color(0xFFEAD9A8), maxLines = 1)
        }
        androidx.compose.material3.DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (text, v) ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(text) },
                    onClick = { onPick(v); open = false },
                )
            }
        }
    }
}

/** Kulaté zlaté tlačítko uprostřed mezi hráči — otevře nastavení hry. */
@Composable
private fun CenterButton(modifier: Modifier, onClick: () -> Unit) {
    val dn = LifeState.dayNight
    Box(
        modifier
            .size(62.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(
                Brush.radialGradient(listOf(Color(0xFF3A2E66), Color(0xFF15102A)))
            )
            .border(2.5.dp, com.example.antimarcipan.ui.GoldBorderBrush, androidx.compose.foundation.shape.CircleShape)
            .combinedClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        when (dn) {
            1 -> Text("☀", fontSize = 28.sp, color = Color(0xFFFFD86B))
            2 -> Text("☾", fontSize = 28.sp, color = Color(0xFFAFC3FF))
            else -> Icon(
                androidx.compose.material.icons.Icons.Filled.Settings,
                contentDescription = "Nastavení hry",
                tint = Color(0xFFF3E7C4),
                modifier = Modifier.size(30.dp),
            )
        }
    }
}

@Composable
private fun LifeMenu(onDismiss: () -> Unit, save: () -> Unit) {
    com.example.antimarcipan.ui.GoldDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nastavení hry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Počet hráčů", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (2..6).forEach { k ->
                        com.example.antimarcipan.ui.GoldChip(
                            selected = LifeState.count == k,
                            onClick = { LifeState.count = k; save() },
                            label = { Text("$k") },
                        )
                    }
                }
                Text("Počáteční životy (spustí novou hru)", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(20, 30, 40).forEach { l ->
                        com.example.antimarcipan.ui.GoldChip(
                            selected = LifeState.startLife == l,
                            onClick = { LifeState.startLife = l; LifeState.reset(); save() },
                            label = { Text("$l") },
                        )
                    }
                }
                Text("Den / noc", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Vypnuto" to 0, "☀ Den" to 1, "☾ Noc" to 2).forEach { (t, v) ->
                        com.example.antimarcipan.ui.GoldChip(
                            selected = LifeState.dayNight == v,
                            onClick = { LifeState.dayNight = v; save() },
                            label = { Text(t) },
                        )
                    }
                }
                com.example.antimarcipan.ui.GoldChip(
                    selected = LifeState.fullscreen,
                    onClick = { LifeState.fullscreen = !LifeState.fullscreen; onDismiss() },
                    label = { Text(if (LifeState.fullscreen) "Ukončit celou obrazovku" else "Celá obrazovka") },
                )
                androidx.compose.material3.Button(
                    onClick = { LifeState.reset(); save(); onDismiss() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Text("  Reset — nová hra")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Zavřít") } },
    )
}
