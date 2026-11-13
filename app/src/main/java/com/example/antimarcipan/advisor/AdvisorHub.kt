package com.example.antimarcipan.advisor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.antimarcipan.data.advisor.BuildFormat
import com.example.antimarcipan.data.advisor.DeckBuilder
import com.example.antimarcipan.data.local.CollectionCard
import com.example.antimarcipan.decks.DeckFormat
import com.example.antimarcipan.decks.LimitedAdvisor
import com.example.antimarcipan.ui.AppViewModelFactory
import com.example.antimarcipan.ui.GoldBorderBrush
import kotlinx.coroutines.launch
import com.example.antimarcipan.data.thumbModel
import androidx.compose.foundation.verticalScroll

private enum class Tool { HUB, ANALYZE, BUILD, LIMITED, HAND, COMBOS, GLOSSARY }

/** Rádce: rozcestník nástrojů. */
@Composable
fun AdvisorHubScreen(vm: AdvisorViewModel = viewModel(factory = AppViewModelFactory)) {
    var tool by rememberSaveable { mutableStateOf(Tool.HUB) }
    androidx.activity.compose.BackHandler(enabled = tool != Tool.HUB) { tool = Tool.HUB }
    val back = { tool = Tool.HUB }
    when (tool) {
        Tool.HUB -> Hub { tool = it }
        Tool.ANALYZE -> com.example.antimarcipan.synergy.SynergyScreen(advisorVm = vm, onBack = back)
        Tool.BUILD -> BuildTool(vm, back)
        Tool.LIMITED -> LimitedTool(vm, back)
        Tool.HAND -> SampleHandTool(vm, back)
        Tool.COMBOS -> CombosTool(vm, back)
        Tool.GLOSSARY -> GlossaryTool(back)
    }
}

@Composable
fun HubBackRow(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět do Rádce") }
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun Hub(open: (Tool) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
        item { HubTile(Icons.Filled.Insights, "Analýza balíku", "Mana curve, země, ramp, dobírání, removal, synergie, co nesedí a co doplnit.") { open(Tool.ANALYZE) } }
        item { HubTile(Icons.Filled.Construction, "Postavit balík ze sbírky", "Vyber formát a barvy (nebo velitele) — navrhnu balík jen z karet, které máš.") { open(Tool.BUILD) } }
        item { HubTile(Icons.Filled.SwapHoriz, "Průvodce Draft / Sealed", "Krok za krokem: jak formát probíhá, pick order, focení poolu a stavba 40 karet.") { open(Tool.LIMITED) } }
        item { HubTile(Icons.Filled.Casino, "Zkušební ruka", "Zamíchej balík, lízni 7 karet, zkus mulligan a líznutí — poznáš, jak balík startuje.") { open(Tool.HAND) } }
        item { HubTile(Icons.Filled.AutoAwesome, "Komba ze sbírky", "Najde známá komba mezi tvými kartami (nebo v balíku) a komba, kterým chybí jediná karta.") { open(Tool.COMBOS) } }
        item { HubTile(Icons.AutoMirrored.Filled.MenuBook, "Slovníček vlastností", "Hledej klíčová slova (Flying, Ward, Dredge…) s českým vysvětlením.") { open(Tool.GLOSSARY) } }
    }
}

@Composable
private fun HubTile(icon: ImageVector, title: String, desc: String, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, GoldBorderBrush),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        }
    }
}

// ---------------------------------------------------------------- Stavba ze sbírky

@Composable
private fun BuildTool(vm: AdvisorViewModel, onBack: () -> Unit) {
    val owned by vm.owned.collectAsStateWithLifecycle()
    val result by vm.build.collectAsStateWithLifecycle()
    var format by rememberSaveable { mutableStateOf(BuildFormat.CASUAL60) }
    var colors by remember { mutableStateOf(setOf<String>()) }
    var commander by remember { mutableStateOf<CollectionCard?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var cmdQuery by rememberSaveable { mutableStateOf("") }
    val commanders = remember(owned) { DeckBuilder.commanderCandidates(owned) }
    val suggested = remember(owned) { DeckBuilder.bestColors(owned) }

    Column(Modifier.fillMaxSize()) {
        HubBackRow("Postavit balík ze sbírky") { vm.clearBuild(); onBack() }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
            item {
                Text("Formát", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    BuildFormat.entries.forEach { f ->
                        com.example.antimarcipan.ui.GoldChip(selected = format == f, onClick = { format = f; vm.clearBuild() }, label = { Text(f.label) })
                    }
                }
            }
            if (format == BuildFormat.COMMANDER) {
                item {
                    Text("Velitel (${commanders.size} legendárních tvorů)", style = MaterialTheme.typography.labelLarge)
                    if (commanders.isEmpty()) Text("Ve sbírce nemáš žádného legendárního tvora.", style = MaterialTheme.typography.bodySmall)
                    else if (commanders.size > 6) OutlinedTextField(
                        value = cmdQuery, onValueChange = { cmdQuery = it }, singleLine = true,
                        label = { Text("Hledat velitele") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    )
                }
                val shownCmd = commanders.filter { cmdQuery.isBlank() || it.name.contains(cmdQuery.trim(), true) }
                // Mřížka po 3 (bez vnořeného lazy gridu)
                items(shownCmd.chunked(3), key = { row -> row.joinToString { it.scryfallId } }) { row ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { c ->
                            val sel = commander?.scryfallId == c.scryfallId
                            Column(
                                Modifier.weight(1f).clickable { commander = c; vm.clearBuild() },
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box {
                                    com.example.antimarcipan.ui.CardThumb(c.thumbModel(), width = 100.dp, height = 140.dp)
                                    if (sel) Box(
                                        Modifier.align(Alignment.TopEnd).padding(4.dp).size(26.dp).clip(CircleShape)
                                            .background(Color(0xFFE2BC62)),
                                        contentAlignment = Alignment.Center,
                                    ) { Text("✓", color = Color(0xFF1E1633), fontWeight = FontWeight.Bold) }
                                }
                                Text(
                                    c.name, style = MaterialTheme.typography.labelSmall, maxLines = 2,
                                    color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                )
                            }
                        }
                        repeat(3 - row.size) { Box(Modifier.weight(1f)) }
                    }
                }
            } else {
                item {
                    Text("Barvy (prázdné = vyberu sám: ${suggested.joinToString("")})", style = MaterialTheme.typography.labelLarge)
                    Row(Modifier.padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf("W", "U", "B", "R", "G").forEach { col ->
                            val sel = col in colors
                            Box(
                                Modifier.size(44.dp).clip(CircleShape)
                                    .border(if (sel) 3.dp else 1.dp, if (sel) GoldBorderBrush else androidx.compose.ui.graphics.SolidColor(Color(0x55FFFFFF)), CircleShape)
                                    .clickable { colors = if (sel) colors - col else colors + col; vm.clearBuild() }
                                    .padding(4.dp),
                            ) {
                                androidx.compose.foundation.Image(
                                    androidx.compose.ui.res.painterResource(manaSymbolRes(col)),
                                    contentDescription = col,
                                    modifier = Modifier.fillMaxSize().then(if (sel) Modifier else Modifier.background(Color.Transparent)),
                                    alpha = if (sel) 1f else 0.45f,
                                )
                            }
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = { vm.buildDeck(format, colors.toList().ifEmpty { null }, if (format == BuildFormat.COMMANDER) commander else null) },
                    enabled = owned.isNotEmpty() && (format != BuildFormat.COMMANDER || commander != null),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                ) { Text(if (result == null) "Navrhnout balík" else "Navrhnout znovu") }
                if (owned.isEmpty()) Text("Sbírka je prázdná — nejdřív naskenuj karty.", style = MaterialTheme.typography.bodySmall)
            }
            result?.let { r ->
                item {
                    Card(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        border = BorderStroke(1.dp, GoldBorderBrush),
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "${r.total} karet · ${r.spellCount} kouzel · ${r.landCount} zemí · barvy ${r.colors.joinToString("")}",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            r.notes.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                            if (r.basics.isNotEmpty()) Text(
                                "Základní země: " + r.basics.entries.joinToString(", ") { "${it.value}× ${it.key}" },
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                item {
                    val (curve, avg, lands) = com.example.antimarcipan.ui.computeCurve(
                        r.cards.map { com.example.antimarcipan.data.local.DeckCardWithQty(it.first, it.second) }
                    )
                    com.example.antimarcipan.ui.MiniManaCurve(curve, avg)
                }
                r.commander?.let { c -> item { BuildRow(c, 1, "velitel") } }
                items(r.cards, key = { it.first.scryfallId }) { (c, q) -> BuildRow(c, q, null) }
                item {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        label = { Text("Název balíku") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    )
                    Button(onClick = { vm.saveBuild(name) }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text("Vytvořit balík a otevřít")
                    }
                }
            }
        }
    }
}

@Composable
private fun BuildRow(c: CollectionCard, q: Int, tag: String?) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        com.example.antimarcipan.ui.InteractiveThumb(c, width = 36.dp, height = 50.dp)
        Column(Modifier.weight(1f)) {
            Text((if (q > 1) "${q}× " else "") + c.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            Text(
                listOfNotNull(tag, c.typeLine, c.cmc?.let { "MV ${it.toInt()}" }).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall, maxLines = 1,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            )
        }
    }
}

// ---------------------------------------------------------------- Průvodce Draft / Sealed

@Composable
private fun LimitedTool(vm: AdvisorViewModel, onBack: () -> Unit) {
    var format by rememberSaveable { mutableStateOf<String?>(null) }
    var step by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        HubBackRow("Průvodce Draft / Sealed", onBack)
        Column(
            Modifier.fillMaxSize().androidx_verticalScroll().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (format == null) {
                Text("Co budeš hrát?", style = MaterialTheme.typography.titleMedium)
                HubTile(Icons.Filled.SwapHoriz, "Booster Draft", "8 hráčů, 3 boostery, karty si vybíráš po jedné a posíláš dál.") { format = "DRAFT"; step = 0 }
                HubTile(Icons.Filled.AutoAwesome, "Sealed deck", "Otevřeš 6 boosterů a z nich postavíš balík.") { format = "SEALED"; step = 0 }
            } else {
                val f = DeckFormat.from(format)
                val steps = limitedSteps(f)
                val (title, body) = steps[step]
                Text("${f.label} · krok ${step + 1}/${steps.size}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(body, style = MaterialTheme.typography.bodyMedium)
                if (step == steps.lastIndex) {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        label = { Text("Název balíku") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { if (step == 0) format = null else step-- }, modifier = Modifier.weight(1f)) { Text("Zpět") }
                    if (step < steps.lastIndex) Button(onClick = { step++ }, modifier = Modifier.weight(1f)) { Text("Dál") }
                    else Button(onClick = { vm.createLimitedDeck(name.ifBlank { f.label }, f.code) }, modifier = Modifier.weight(1f)) {
                        Text("Založit a začít")
                    }
                }
            }
        }
    }
}

private fun limitedSteps(f: DeckFormat): List<Pair<String, String>> = if (f == DeckFormat.SEALED) listOf(
    "Otevři boostery" to "Každý hráč dostane 6 boosterů. Otevři je — všech ~84 karet je tvůj pool. Základní země dostaneš zdarma, kolik chceš.",
    "Vyfoť pool" to "Po založení balíku se otevře jeho detail. V rádci nahoře ťukni „Vyfotit karty“ a fofť karty po skupinách lícem nahoru. Jdou do poolu, ne do sbírky.",
    "Najdi barvy" to "Rádce v balíku spočítá nejsilnější barvy podle bomb (silné rare/mythic) a removalu. Hraj 2 barvy; třetí jen na 1–2 bomby, pokud máš fixing.",
    "Postav 40 karet" to "Cíl: 17 zemí + 23 kouzel, 15–17 tvorů, co nejvíc removalu. Křivka: 4–6 tvorů za 2, 3–5 za 3, max 2–3 karty za 6+. Tlačítko „Nastavit země“ rozdělí basicy podle symbolů many.",
    "Hraj a ulož" to "Hraje se obvykle na 2 vítězné hry; mezi hrami můžeš měnit karty s poolem (sideboard). Po turnaji dej „Pool do sbírky“ a karty se přidají do sbírky naráz.",
) else listOf(
    "Jak draft probíhá" to "U stolu je obvykle 8 hráčů, každý má 3 boostery. Otevřeš booster, vezmeš si 1 kartu a zbytek pošleš doleva. Od souseda dostaneš jeho booster, zase vybereš 1… 2. booster jde doprava, 3. zase doleva. Skončíš s ~42 kartami.",
    "Pick order" to "1. Bomby — karty, které samy vyhrají hru.\n2. Removal.\n3. Evasivní a efektivní tvorové (flying, menace, dobrý poměr síla/cena).\n4. Kombinační karty a triky nakonec.",
    "Čti signály" to "Prvních ~5 picků ber nejsilnější kartu bez ohledu na barvu. Pak sleduj, která barva ti chodí pozdě (silná karta v 6.–9. picku) — ta je „otevřená“, sousedé ji neberou. Do konce 1. boosteru se ustal na 2 barvách.",
    "Vyfoť karty" to "Po draftu otevři balík (vytvoříme ho teď) a v rádci ťukni „Vyfotit karty“. Karty jdou do poolu; rádce je ohodnotí a doporučí barvy a nejlepší karty.",
    "Postav 40 karet" to "17 zemí + 23 kouzel, 15–17 tvorů, co nejvíc removalu, rozumná křivka. Po hraní „Pool do sbírky“ přidá vydraftované karty do sbírky.",
)

@Composable
private fun Modifier.androidx_verticalScroll(): Modifier =
    this.then(Modifier.verticalScroll(rememberScrollState()))

private fun manaSymbolRes(c: String): Int = when (c) {
    "W" -> com.example.antimarcipan.R.drawable.mana_w
    "U" -> com.example.antimarcipan.R.drawable.mana_u
    "B" -> com.example.antimarcipan.R.drawable.mana_b
    "R" -> com.example.antimarcipan.R.drawable.mana_r
    else -> com.example.antimarcipan.R.drawable.mana_g
}

// ---------------------------------------------------------------- Zkušební ruka

@Composable
private fun SampleHandTool(vm: AdvisorViewModel, onBack: () -> Unit) {
    val decks by vm.decks.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var deckId by rememberSaveable { mutableStateOf<Long?>(null) }
    var library by remember { mutableStateOf<List<CollectionCard>>(emptyList()) }
    var hand by remember { mutableStateOf<List<CollectionCard>>(emptyList()) }
    var drawn by remember { mutableIntStateOf(0) }
    var mulligans by remember { mutableIntStateOf(0) }

    fun newHand(mull: Boolean) {
        val shuffled = library.shuffled()
        mulligans = if (mull) mulligans + 1 else 0
        hand = shuffled.take(7)
        library = shuffled
        drawn = 7
    }
    LaunchedEffect(deckId) {
        val id = deckId ?: return@LaunchedEffect
        library = vm.deckLibrary(id)
        mulligans = 0
        newHand(false)
    }

    Column(Modifier.fillMaxSize()) {
        HubBackRow("Zkušební ruka", onBack)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            decks.forEach { d -> com.example.antimarcipan.ui.GoldChip(selected = deckId == d.id, onClick = { deckId = d.id }, label = { Text(d.name) }) }
        }
        HorizontalDivider()
        if (deckId == null) {
            Text("Vyber balík.", Modifier.padding(16.dp))
            return@Column
        }
        if (library.size < 7) {
            Text("Balík má méně než 7 karet.", Modifier.padding(16.dp))
            return@Column
        }
        val lands = hand.count { LimitedAdvisor.isLand(it) }
        val verdict = when {
            hand.size < 7 -> ""
            lands <= 1 -> "Málo zemí — spíš mulligan."
            lands >= 5 -> "Moc zemí — spíš mulligan."
            hand.none { !LimitedAdvisor.isLand(it) && (it.cmc ?: 9.0) <= 3 } -> "Žádný levný tah — zvaž mulligan."
            else -> "Hratelná ruka — drž."
        }
        Text(
            "V ruce ${hand.size} karet · zemí $lands" + (if (mulligans > 0) " · mulligan $mulligans× (dej $mulligans ${if (mulligans == 1) "kartu" else "karty"} dospod)" else ""),
            Modifier.padding(horizontal = 16.dp, vertical = 6.dp), style = MaterialTheme.typography.labelLarge,
        )
        if (verdict.isNotEmpty()) Text(verdict, Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        androidx.compose.runtime.CompositionLocalProvider(com.example.antimarcipan.ui.LocalCardList provides hand) {
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(96.dp),
                modifier = Modifier.weight(1f).padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(hand.size) { i -> com.example.antimarcipan.ui.InteractiveThumb(hand[i], width = 96.dp, height = 134.dp) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { newHand(true) }, modifier = Modifier.weight(1f)) { Text("Mulligan") }
            OutlinedButton(
                onClick = { if (drawn < library.size) { hand = hand + library[drawn]; drawn++ } },
                enabled = drawn < library.size, modifier = Modifier.weight(1f),
            ) { Text("Líznout") }
            Button(onClick = { scope.launch { newHand(false) } }, modifier = Modifier.weight(1f)) { Text("Nová") }
        }
    }
}

// ---------------------------------------------------------------- Slovníček

@Composable
private fun GlossaryTool(onBack: () -> Unit) {
    var q by rememberSaveable { mutableStateOf("") }
    var czech by rememberSaveable { mutableStateOf(false) }
    val all = if (czech) com.example.antimarcipan.data.KeywordGlossary.entriesCz else com.example.antimarcipan.data.KeywordGlossary.entries
    val shown = all.entries.filter { q.isBlank() || it.key.contains(q.trim(), true) || it.value.contains(q.trim(), true) }
        .sortedBy { it.key }
    Column(Modifier.fillMaxSize()) {
        HubBackRow("Slovníček vlastností", onBack)
        OutlinedTextField(
            value = q, onValueChange = { q = it }, singleLine = true,
            label = { Text("Hledat (např. trample, ward)") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        )
        Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            com.example.antimarcipan.ui.GoldChip(selected = !czech, onClick = { czech = false }, label = { Text("English") })
            com.example.antimarcipan.ui.GoldChip(selected = czech, onClick = { czech = true }, label = { Text("Přeložit do češtiny") })
        }
        LazyColumn(Modifier.fillMaxSize().padding(top = 6.dp)) {
            items(shown, key = { it.key }) { e ->
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(e.key.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Text(e.value.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall)
                }
                HorizontalDivider()
            }
        }
    }
}


// ---------------------------------------------------------------- Komba

@Composable
private fun CombosTool(vm: AdvisorViewModel, onBack: () -> Unit) {
    val decks by vm.decks.collectAsStateWithLifecycle()
    val st by vm.combos.collectAsStateWithLifecycle()
    var source by rememberSaveable { mutableStateOf<Long?>(-1L) } // -1 = celá sbírka
    Column(Modifier.fillMaxSize()) {
        HubBackRow("Komba ze sbírky", onBack)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            com.example.antimarcipan.ui.GoldChip(selected = source == -1L, onClick = { source = -1L; vm.findCombos(null) }, label = { Text("Celá sbírka") })
            decks.forEach { d -> com.example.antimarcipan.ui.GoldChip(selected = source == d.id, onClick = { source = d.id; vm.findCombos(d.id) }, label = { Text(d.name) }) }
        }
        HorizontalDivider()
        when {
            st.loading -> Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.CircularProgressIndicator(Modifier.size(22.dp)); Text("  Hledám komba…")
            }
            st.error != null -> Text(st.error!!, Modifier.padding(16.dp))
            st.report == null -> Text(
                "Vyber celou sbírku nebo balík. Komba hledám v databázi Commander Spellbook (potřebuje internet).",
                Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall,
            )
            else -> {
                val r = st.report!!
                LazyColumn(Modifier.fillMaxSize()) {
                    item { ComboHeader("Kompletní komba (${r.complete.size})", "Všechny karty máš.") }
                    if (r.complete.isEmpty()) item { Text("Žádné kompletní kombo.", Modifier.padding(16.dp, 4.dp), style = MaterialTheme.typography.bodySmall) }
                    items(r.complete.take(100), key = { "c" + it.id }) { ComboCard(it) }
                    item { ComboHeader("Chybí jedna karta (${r.almost.size})", "Stačí sehnat jednu kartu — vhodné na wishlist.") }
                    items(r.almost.take(150), key = { "a" + it.id }) { ComboCard(it) }
                }
            }
        }
    }
}

@Composable
private fun ComboHeader(title: String, sub: String) {
    Column(Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

@Composable
private fun ComboCard(c: com.example.antimarcipan.data.advisor.Combo) {
    var open by remember { mutableStateOf(false) }
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).clickable { open = !open },
        border = BorderStroke(1.dp, if (c.missing.isEmpty()) GoldBorderBrush else androidx.compose.ui.graphics.SolidColor(Color(0x55B9A7EC))),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                c.cards.forEach { n ->
                    val miss = n in c.missing
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.size(width = 76.dp, height = 140.dp)) {
                        var big by remember { mutableStateOf(false) }
                        Box(Modifier.clickable { big = true }) {
                            com.example.antimarcipan.ui.CardThumb(
                                com.example.antimarcipan.ui.scryfallImageByName(n, "small"), width = 70.dp, height = 98.dp,
                            )
                        }
                        if (big) androidx.compose.ui.window.Dialog(onDismissRequest = { big = false }) {
                            coil.compose.AsyncImage(
                                model = com.example.antimarcipan.ui.scryfallImageByName(n), contentDescription = n,
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(2.dp, GoldBorderBrush, RoundedCornerShape(14.dp)),
                            )
                        }
                        Text(
                            (if (miss) "✗ " else "✓ ") + n,
                            style = MaterialTheme.typography.labelSmall, maxLines = 2,
                            color = if (miss) Color(0xFFFFA0A0) else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            c.missing.forEach { m ->
                com.example.antimarcipan.ui.WishlistButton(
                    m,
                    "Kombo: " + c.cards.joinToString(" + ") + (if (c.results.isNotEmpty()) " → " + c.results.joinToString(", ") else ""),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (c.results.isNotEmpty()) Text(
                "Výsledek: " + c.results.joinToString(", "),
                style = MaterialTheme.typography.bodySmall, color = Color(0xFF8FD6A0),
            )
            if (open && c.steps.isNotBlank()) Text(c.steps, style = MaterialTheme.typography.bodySmall)
            else if (c.steps.isNotBlank()) Text("Ťukni pro postup", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}
