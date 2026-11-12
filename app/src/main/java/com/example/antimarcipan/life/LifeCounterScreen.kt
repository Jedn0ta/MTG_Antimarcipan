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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.clickable
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
import androidx.compose.animation.core.animateFloat
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
    var totalMs by mutableStateOf(0L)
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
        keepOn = p.getBoolean("keepOn", true)
        curve = p.getInt("curve", 0).coerceIn(0, TIME_CURVES.lastIndex)
        players.forEachIndexed { i, pl ->
            pl.life = p.getInt("life$i", startLife)
            pl.poison = p.getInt("poison$i", 0)
        }
    }

    fun save(ctx: Context) {
        val e = ctx.getSharedPreferences("life_counter", Context.MODE_PRIVATE).edit()
        e.putInt("count", count).putInt("start", startLife).putInt("dayNight", dayNight).putBoolean("keepOn", keepOn).putInt("curve", curve)
        players.forEachIndexed { i, pl -> e.putInt("life$i", pl.life).putInt("poison$i", pl.poison) }
        e.apply()
    }

    fun reset() {
        players.forEach { it.life = startLife; it.poison = 0; it.delta = 0; it.totalMs = 0L }
        dayNight = 0
        resetTurns()
    }

    // ---- Režim tahů ----
    /** Hráči potvrzují konec tahu tlačítkem; měří se čas a zaznamenává historie životů. */
    var turnMode by mutableStateOf(false)
    /** Nevypínat obrazovku (spořič) — výchozí zapnuto. */
    var keepOn by mutableStateOf(true)
    /** Křivka doporučeného času tahu (index do TIME_CURVES). */
    var curve by mutableIntStateOf(0)
    /** Pro aktuální tah už se upozornilo (vibrace). */
    var warned by mutableStateOf(false)

    /** Doporučený čas tahu v ms pro aktuální tah (null = bez limitu). */
    fun limitMs(): Long? = TIME_CURVES[curve].limitFor(turn)
    var active by mutableIntStateOf(0)
    var turn by mutableIntStateOf(1)
    var turnStart by mutableStateOf(System.currentTimeMillis())
    /** Tiknutí hodin pro překreslení časovače. */
    var now by mutableStateOf(System.currentTimeMillis())
    /** Historie: (tah, hráč na tahu, životy všech hráčů po tahu). */
    val history = androidx.compose.runtime.mutableStateListOf<HistoryEntry>()

    fun resetTurns() {
        active = 0; turn = 1; turnStart = System.currentTimeMillis()
        history.clear()
        history += HistoryEntry(0, -1, players.take(count).map { it.life }, 0L,
            poison = players.take(count).map { it.poison }, active = 0, turnNo = 1)
    }

    /** Zapíše dávku změny životů hráče (po krátké pauze v ťukání). */
    fun recordChange(player: Int, delta: Int) {
        if (history.isEmpty()) {
            // výchozí stav = současný stav bez této dávky
            history += HistoryEntry(0, -1,
                players.take(count).mapIndexed { i, p -> if (i == player) p.life - delta else p.life }, 0L,
                poison = players.take(count).map { it.poison }, active = active, turnNo = turn)
        }
        history += HistoryEntry(turn, player, players.take(count).map { it.life }, 0L,
            delta = delta, poison = players.take(count).map { it.poison }, active = active, turnNo = turn)
    }

    /** Vrátí stav hry k záznamu [index]; pozdější záznamy zahodí. */
    fun revertTo(index: Int) {
        val e = history.getOrNull(index) ?: return
        e.lives.forEachIndexed { i, l -> players[i].life = l; players[i].delta = 0 }
        e.poison.forEachIndexed { i, v -> players[i].poison = v }
        while (history.size > index + 1) history.removeAt(history.lastIndex)
        if (e.delta == null && e.player >= 0) {
            // byl to konec tahu → na tahu je další po něm
            var next = e.player
            for (i in 1..count) {
                val c = (e.player + i) % count
                if (players[c].life > 0 && players[c].poison < 10) { next = c; break }
            }
            if (next <= e.player) turn = e.turn + 1 else turn = e.turn
            active = next
        } else { active = e.active; turn = e.turnNo }
        turnStart = System.currentTimeMillis()
        warned = false
    }

    fun endTurn() {
        val t = System.currentTimeMillis()
        val dur = t - turnStart
        players[active].totalMs += dur
        history += HistoryEntry(turn, active, players.take(count).map { it.life }, dur,
            poison = players.take(count).map { it.poison }, active = active, turnNo = turn)
        // další živý hráč
        var next = active
        for (i in 1..count) {
            val c = (active + i) % count
            val p = players[c]
            if (p.life > 0 && p.poison < 10) { next = c; break }
        }
        if (next <= active) turn++
        active = next
        turnStart = t
        warned = false
    }
}

/**
 * Křivka doporučeného času: začíná na [baseSec] a každým kolem přidá [perTurnSec],
 * až po strop [maxSec] — první kola jsou rychlá, pozdější s víc kartami delší.
 */
data class TimeCurve(val label: String, val baseSec: Int, val perTurnSec: Int, val maxSec: Int) {
    fun limitFor(turn: Int): Long? =
        if (baseSec <= 0) null else minOf(maxSec, baseSec + perTurnSec * (turn - 1)) * 1000L
}

val TIME_CURVES = listOf(
    TimeCurve("Bez limitu", 0, 0, 0),
    TimeCurve("Rychlá", 45, 10, 180),
    TimeCurve("Standardní", 60, 15, 240),
    TimeCurve("Pohodová", 90, 20, 360),
)

/**
 * Záznam historie: buď konec tahu ([delta] == null), nebo dávka změny životů hráče.
 * Ukládá stav všech hráčů po záznamu, aby se k němu dalo vrátit.
 */
data class HistoryEntry(
    val turn: Int,
    val player: Int,
    val lives: List<Int>,
    val durationMs: Long,
    val delta: Int? = null,
    val poison: List<Int> = emptyList(),
    val active: Int = 0,
    val turnNo: Int = 1,
)

fun formatDuration(ms: Long): String {
    val s = ms / 1000
    return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, s / 60 % 60, s % 60) else "%d:%02d".format(s / 60, s % 60)
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
        onDispose { view.keepScreenOn = false; LifeState.save(ctx) }
    }
    val keepOn = LifeState.keepOn
    DisposableEffect(keepOn) {
        view.keepScreenOn = keepOn
        onDispose { }
    }
    LaunchedEffect(LifeState.turnMode) {
        while (LifeState.turnMode) {
            LifeState.now = System.currentTimeMillis()
            val lim = LifeState.limitMs()
            if (lim != null && !LifeState.warned && LifeState.now - LifeState.turnStart > lim) {
                LifeState.warned = true
                vibrate(ctx)
            }
            delay(500)
        }
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
    val isStarter = LifeState.turnMode && LifeState.active == index

    fun change(d: Int) {
        p.life += d
        p.delta += d
        p.deltaTick++
        save()
    }
    LaunchedEffect(p.deltaTick) {
        delay(1600)
        if (p.delta != 0) LifeState.recordChange(index, p.delta)
        p.delta = 0
    }

    val lim = LifeState.limitMs()
    val over = isStarter && lim != null && LifeState.now - LifeState.turnStart > lim
    // Blikání při překročení doporučeného času
    val blink = if (over) {
        val tr = androidx.compose.animation.core.rememberInfiniteTransition(label = "blink")
        tr.animateFloat(
            0f, 1f,
            androidx.compose.animation.core.infiniteRepeatable(
                androidx.compose.animation.core.tween(500),
                androidx.compose.animation.core.RepeatMode.Reverse,
            ),
            label = "blinkA",
        ).value
    } else 0f
    Box(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    if (dead) listOf(Color(0xFF2A2A2A), Color(0xFF151515))
                    else listOf(color, color.copy(alpha = 0.55f).compositeOver(Color(0xFF120E22)))
                )
            )
            .background(Color(0xFFFF3B3B).copy(alpha = 0.35f * blink))
            .border(
                if (isStarter) 3.dp else 1.dp,
                if (over) Brush.linearGradient(listOf(Color(0xFFFF6B6B), Color(0xFFFFB4B4)))
                else Brush.linearGradient(listOf(Color(0xFFF6DE9A), Color(0xFFB98A35))),
                RoundedCornerShape(18.dp),
            ),
    ) {
        Box(Modifier.fillMaxSize().sideways(rotation)) {
            Row(Modifier.fillMaxSize()) {
                TapZone("−", { change(-1) }, { change(-5) })
                TapZone("+", { change(1) }, { change(5) })
            }
            if (LifeState.turnMode) {
                Text(
                    if (isStarter) "⏱ " + formatDuration(LifeState.now - LifeState.turnStart) +
                        (lim?.let { " / " + formatDuration(it) } ?: "")
                    else "Σ " + formatDuration(p.totalMs),
                    Modifier.align(Alignment.TopStart).padding(10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = if (isStarter) 0.45f else 0.25f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    color = if (over) Color(0xFFFFB4B4) else if (isStarter) Color(0xFFF6DE9A) else Color.White.copy(alpha = 0.6f),
                    fontSize = if (isStarter) 16.sp else 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (isStarter) Text(
                    "Tah ${LifeState.turn}",
                    Modifier.align(Alignment.TopEnd).padding(12.dp),
                    color = Color(0xFFF6DE9A), fontSize = 12.sp,
                )
                if (isStarter) androidx.compose.material3.Button(
                    onClick = { LifeState.endTurn() },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE2BC62), contentColor = Color(0xFF1E1633),
                    ),
                ) { Text("Konec tahu", fontWeight = FontWeight.Bold) }
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
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        when (dn) {
            1 -> Text("☀", fontSize = 28.sp, color = Color(0xFFFFD86B))
            2 -> Text("☾", fontSize = 28.sp, color = Color(0xFFAFC3FF))
            else -> Icon(
                Icons.Filled.Settings,
                contentDescription = "Nastavení hry",
                tint = Color(0xFFF3E7C4),
                modifier = Modifier.size(30.dp),
            )
        }
    }
}

@Composable
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
private fun LifeMenu(onDismiss: () -> Unit, save: () -> Unit) {
    var showHistory by remember { mutableStateOf(false) }
    if (showHistory) { HistoryDialog { showHistory = false }; return }
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
                Text("Režim tahů", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    com.example.antimarcipan.ui.GoldChip(
                        selected = LifeState.turnMode,
                        onClick = {
                            LifeState.turnMode = !LifeState.turnMode
                            if (LifeState.turnMode) LifeState.resetTurns()
                        },
                        label = { Text(if (LifeState.turnMode) "Zapnuto" else "Vypnuto") },
                    )
                    com.example.antimarcipan.ui.GoldChip(
                        selected = false,
                        onClick = { showHistory = true },
                        label = { Text("Historie životů") },
                    )
                }
                if (LifeState.turnMode) {
                    Text(
                        "Hráč na tahu ukončí tah tlačítkem na své dlaždici. V rohu běží čas tahu, u ostatních celkový čas.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text("Doporučený čas tahu", style = MaterialTheme.typography.labelLarge)
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        TIME_CURVES.forEachIndexed { i, c ->
                            com.example.antimarcipan.ui.GoldChip(
                                selected = LifeState.curve == i,
                                onClick = { LifeState.curve = i; LifeState.warned = false; save() },
                                label = { Text(c.label) },
                            )
                        }
                    }
                    val c = TIME_CURVES[LifeState.curve]
                    if (c.baseSec > 0) Text(
                        "1. kolo ${formatDuration(c.limitFor(1)!!)}, 5. kolo ${formatDuration(c.limitFor(5)!!)}, " +
                            "10. kolo ${formatDuration(c.limitFor(10)!!)} (max ${formatDuration(c.maxSec * 1000L)}). " +
                            "Po překročení dlaždice zabliká a telefon zavibruje.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                com.example.antimarcipan.ui.GoldChip(
                    selected = LifeState.keepOn,
                    onClick = { LifeState.keepOn = !LifeState.keepOn; save() },
                    label = { Text(if (LifeState.keepOn) "Obrazovka nezhasíná ✓" else "Obrazovka může zhasnout") },
                )
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

/** Historie životů: graf průběhu + tabulka po tazích. */
@Composable
private fun HistoryDialog(onDismiss: () -> Unit) {
    val h = LifeState.history.toList()
    val n = LifeState.count
    com.example.antimarcipan.ui.GoldDialog(
        onDismissRequest = onDismiss,
        title = { Text("Historie životů") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (h.size < 2) {
                    Text("Zatím žádný záznam — změň životy nebo ukonči tah.", style = MaterialTheme.typography.bodySmall)
                } else {
                    // Graf
                    val maxL = (h.flatMap { it.lives }.maxOrNull() ?: 20).coerceAtLeast(1)
                    androidx.compose.foundation.Canvas(
                        Modifier.fillMaxWidth().height(160.dp)
                            .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                            .padding(8.dp)
                    ) {
                        val stepX = size.width / (h.size - 1).coerceAtLeast(1)
                        for (pi in 0 until n) {
                            val path = androidx.compose.ui.graphics.Path()
                            h.forEachIndexed { i, e ->
                                val life = e.lives.getOrElse(pi) { 0 }.coerceAtLeast(0)
                                val x = i * stepX
                                val y = size.height * (1f - life.toFloat() / maxL)
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            drawPath(
                                path, PLAYER_COLORS[pi].copy(alpha = 1f).let { c ->
                                    Color(minOf(1f, c.red * 1.6f), minOf(1f, c.green * 1.6f), minOf(1f, c.blue * 1.6f))
                                },
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5f),
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (0 until n).forEach { pi ->
                            Text("● H${pi + 1}", color = PLAYER_COLORS[pi].let { c ->
                                Color(minOf(1f, c.red * 1.6f), minOf(1f, c.green * 1.6f), minOf(1f, c.blue * 1.6f))
                            }, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    // Tabulka
                    Row {
                        Text("Záznam", Modifier.width(92.dp), style = MaterialTheme.typography.labelMedium)
                        (0 until n).forEach { Text("H${it + 1}", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium) }
                        Text("    ", fontSize = 18.sp)
                    }
                    Text(
                        "Ťukni na ↶ u záznamu a hra se vrátí přesně do stavu po něm.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFB9A7EC),
                    )
                    h.forEachIndexed { idx, e ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                when {
                                    e.player < 0 -> "start"
                                    e.delta != null -> "H${e.player + 1} " + (if (e.delta > 0) "+" else "") + e.delta
                                    else -> "Tah ${e.turn} H${e.player + 1} ${formatDuration(e.durationMs)}"
                                },
                                Modifier.width(92.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = when {
                                    e.delta == null -> Color(0xFFF6DE9A)
                                    e.delta > 0 -> Color(0xFFB6F2C4)
                                    else -> Color(0xFFFFB4B4)
                                },
                            )
                            (0 until n).forEach { pi ->
                                Text(
                                    "${e.lives.getOrElse(pi) { 0 }}",
                                    Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (pi == e.player) FontWeight.Bold else FontWeight.Normal,
                                )
                            }
                            if (idx < h.lastIndex) Text(
                                "↶",
                                Modifier
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .clickable { LifeState.revertTo(idx); onDismiss() }
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                color = Color(0xFFF6DE9A), fontSize = 18.sp,
                            ) else Text("  ", fontSize = 18.sp, modifier = Modifier.padding(horizontal = 8.dp))
                        }
                    }
                    Text(
                        "Celkový čas: " + (0 until n).joinToString("  ") { "H${it + 1} ${formatDuration(LifeState.players[it].totalMs)}" },
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Zavřít") } },
    )
}

private fun vibrate(ctx: Context) {
    runCatching {
        val v = if (android.os.Build.VERSION.SDK_INT >= 31)
            (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager).defaultVibrator
        else @Suppress("DEPRECATION") (ctx.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator)
        val pattern = longArrayOf(0, 300, 150, 300, 150, 500)
        if (android.os.Build.VERSION.SDK_INT >= 26) v.vibrate(android.os.VibrationEffect.createWaveform(pattern, -1))
        else @Suppress("DEPRECATION") v.vibrate(pattern, -1)
    }
}
