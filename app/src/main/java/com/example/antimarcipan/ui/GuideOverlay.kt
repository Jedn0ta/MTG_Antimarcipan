package com.example.antimarcipan.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// =====================================================================
// Průvodce aplikací: strom témat. Každá stránka má ilustraci (zjednodušený
// „screenshot“ obrazovky s vyznačeným gestem), text a odkazy „Víc o…“
// na podrobnější stránky. Zpět se vrací o úroveň výš.
// =====================================================================

private val Gold = Color(0xFFE2BC62)
private val GoldLight = Color(0xFFF6DE9A)
private val Ink = Color(0xFF1E1633)
private val Panel = Color(0xFF2B2252)
private val Muted = Color(0xFFB8AEDB)
private val Green = Color(0xFF8FD6A0)
private val Lav = Color(0xFFB9A7EC)

private class GuideNode(
    val id: String,
    val title: String,
    val body: String,
    val mock: (@Composable () -> Unit)? = null,
    val children: List<String> = emptyList(),
)

private val NODES: Map<String, GuideNode> = listOf(
    GuideNode(
        "root", "Průvodce aplikací",
        "Vyber téma. Každé má ilustraci obrazovky, popis a odkazy na podrobnosti.",
        mock = { MockHome() },
        children = listOf("gestures", "scan", "collection", "decks", "advisor", "life", "settings"),
    ),
    // ---------- Gesta
    GuideNode(
        "gestures", "Základní gesta",
        "Appka se ovládá hlavně gesty na kartách:\n• Ťuk na náhled → detail karty.\n• Podržení náhledu → velká karta.\n• Swipe řádku doprava → do balíku.\n• Dlouhé podržení s kolečkem → akce, která se nemá stát omylem.",
        mock = { MockCardRow(highlightThumb = true, label = "ťukni / podrž") },
        children = listOf("g_tap", "g_hold", "g_swipe", "g_long"),
    ),
    GuideNode(
        "g_tap", "Ťuknutí na náhled",
        "Otevře detail: jméno, mana cost, edice, typ, textace se symboly many a vysvětlivky vlastností (anglicky, s tlačítkem pro český překlad). Dole poznámka a tlačítko pro odebrání/smazání.",
        mock = { MockDetailDialog() },
    ),
    GuideNode(
        "g_hold", "Podržení náhledu",
        "Podrž náhled — po chvilce se ukáže kolečko a pak se otevře celá karta ve velkém se zlatým rámečkem. Swipem doleva/doprava listuješ celým seznamem, ve kterém jsi.",
        mock = { MockBigCard() },
    ),
    GuideNode(
        "g_swipe", "Swipe řádku",
        "Ve sbírce chyť řádek za levý okraj a táhni doprava. Až přejedeš půlku displeje, pusť — vybereš balík a karta se přidá (pokud tam je, přidá se kus). Ve Wishlistu, Draftu a Playgroundu swipe přesune kartu do sbírky.",
        mock = { MockSwipe() },
    ),
    GuideNode(
        "g_long", "Dlouhé podržení",
        "U akcí, které nechceš spustit omylem, se plní kolečko (2–3 s). Např. podržení ikony knížky otevře Gatherer. Mazání karet je vždy jen z detailu karty a s potvrzením.",
        mock = { MockHoldRing() },
    ),
    // ---------- Sken
    GuideNode(
        "scan", "Skenování",
        "Záložka Sken. Nahoře přepínáš režim: Jméno, Roh nebo Multisken. Vpravo nahoře ✕ skener vypne, pod tím blesk zapne světlo.",
        mock = { MockScanner(mode = 0) },
        children = listOf("s_name", "s_found", "s_corner", "s_multi", "s_manual"),
    ),
    GuideNode(
        "s_name", "Režim Jméno",
        "Namiř na jméno karty (horní řádek). Appka ho přečte a najde kartu ve Scryfallu. Drž telefon rovně, při slabém světle zapni blesk.",
        mock = { MockScanner(mode = 0) },
    ),
    GuideNode(
        "s_found", "Nalezená karta",
        "Karta zůstane na obrazovce, dokud nerozhodneš:\n• Do sbírky / Do balíku\n• Wishlist / Playground (odložit k prozkoumání)\n• Skenovat dál\nŤukem na edici vybereš jiný tisk (obrázek i cena).",
        mock = { MockFoundButtons() },
    ),
    GuideNode(
        "s_corner", "Režim Roh — cizojazyčné karty",
        "Německé, japonské a jiné karty najdeš podle levého spodního rohu: číslo karty (123/280) a kód edice s jazykem (DMU • DE). Namiř roh do zlatého rámečku. Funguje u karet zhruba od roku 2015.",
        mock = { MockScanner(mode = 1) },
    ),
    GuideNode(
        "s_multi", "Multisken z fotky",
        "Rozlož karty lícem nahoru vedle sebe a ťukni „Vyfotit karty“ (nebo vyber fotku z galerie). Appka přečte všechny názvy, ukáže průběh a nalezené karty potvrdíš najednou.",
        mock = { MockMulti() },
    ),
    GuideNode(
        "s_manual", "Ruční přidání",
        "Ikona + v horní liště: napiš jméno, vyber kartu (ťuk na náhled = detail) a přidej do sbírky, na wishlist nebo do balíku.",
        mock = { MockTopBar() },
    ),
    // ---------- Sbírka
    GuideNode(
        "collection", "Sbírka",
        "Nahoře záložky Sbírka / Wishlist / Draft / Playground, pod nimi počet kusů a cena. Vpravo přepínač seznam/mřížka a filtr.",
        mock = { MockCollection() },
        children = listOf("c_tabs", "c_filter", "c_edition", "c_gatherer", "c_notes", "c_delete"),
    ),
    GuideNode(
        "c_tabs", "Záložky",
        "• Sbírka — co vlastníš.\n• Wishlist — co chceš (s poznámkou, např. ke kterému kombu).\n• Draft — karty z poolu draftu/sealedu, ještě nejsou ve sbírce.\n• Playground — karty odložené ze skeneru k prozkoumání.",
        mock = { MockChips(listOf("Sbírka", "Wishlist", "Draft", "Playground"), 1) },
    ),
    GuideNode(
        "c_filter", "Filtr a řazení",
        "Ikona filtru otevře panel: jméno, text, barvy, mana value, typy, tribal, vlastnosti (flying, lifelink…), edice, rarita. Řádek Vše/Land/Spell a vpravo řazení (jméno, cena, MV…).",
        mock = { MockChips(listOf("Vše", "Land", "Spell"), 0, trailing = "Jméno ↑") },
    ),
    GuideNode(
        "c_edition", "Změna edice",
        "Ikona ⇄ u karty: vybereš jiný tisk. Změní se obrázek i cena a karta se přepíše i v balících.",
        mock = { MockCardRow(highlightIcon = 0, label = "⇄ edice") },
    ),
    GuideNode(
        "c_gatherer", "Gatherer a rulings",
        "Ikona knížky:\n• ťuk → oficiální rulings (výklady pravidel),\n• podržení 3 s → otevře kartu na Gathereru (oficiální web Wizards of the Coast).",
        mock = { MockCardRow(highlightIcon = 1, label = "📖 rulings") },
    ),
    GuideNode(
        "c_notes", "Poznámky",
        "V detailu karty „Přidat poznámku“. Poznámka se ukáže fialově pod jménem v seznamu. Karty přidané z Rádce na wishlist dostanou poznámku automaticky (např. „Kombo: …“).",
        mock = { MockCardRow(note = "📝 Kombo: A + B → infinite mana") },
    ),
    GuideNode(
        "c_delete", "Smazání karty",
        "Jen z detailu karty: ťuk na náhled → červené „Odstranit ze sbírky“ → potvrdit. Ve Wishlistu/Playgroundu se karta jen odebere ze záložky.",
        mock = { MockDetailDialog(showDelete = true) },
    ),
    // ---------- Balíky
    GuideNode(
        "decks", "Balíky",
        "„+ Nový balík“ a formát: Commander, Jednokarťák, Standard, Draft, Sealed, Volný. V detailu balíku: panel základních zemí, Vše/Land/Spell, mana curve, cena.",
        mock = { MockDeck() },
        children = listOf("d_basics", "d_curve", "d_remove", "d_limited"),
    ),
    GuideNode(
        "d_basics", "Základní země",
        "Panel nahoře: ťuk na symbol many přidá zemi, „−“ ubere. Basicy se jinde v balíku nezobrazují.",
        mock = { MockBasics() },
    ),
    GuideNode(
        "d_curve", "Mana curve",
        "Ikona grafu v liště balíku zobrazí/skryje nízký graf křivky: počty karet podle MV, tvorové zlatě, ostatní fialově, vpravo průměr.",
        mock = { MockCurve() },
    ),
    GuideNode(
        "d_remove", "Odebrání karty",
        "Ťuk na náhled → „Odebrat z balíku“, nebo podržení řádku. Vždy s potvrzením; karta zůstane ve sbírce.",
        mock = { MockDetailDialog(showDelete = true, deleteLabel = "Odebrat z balíku") },
    ),
    GuideNode(
        "d_limited", "Draft a Sealed",
        "Limited balík se staví z poolu: v rádci balíku „Vyfotit karty“ → karty jdou do poolu. Rádce hlídá 40 karet a 17 zemí, doporučí barvy a nejlepší karty. Na konci „Pool do sbírky“.",
        mock = { MockPool() },
    ),
    // ---------- Rádce
    GuideNode(
        "advisor", "Rádce",
        "Rozcestník nástrojů — ťukni na dlaždici.",
        mock = { MockHub() },
        children = listOf("a_analyze", "a_build", "a_combos", "a_hand"),
    ),
    GuideNode(
        "a_analyze", "Analýza balíku",
        "Vyber balík: mana curve, zdraví (země, ramp, dobírání, removal), témata, karty, které nesedí, i s náhradami a návrhy. U návrhů „+ Wishlist“.",
        mock = { MockCurve() },
    ),
    GuideNode(
        "a_build", "Postavit balík ze sbírky",
        "Formát, barvy (nebo velitel z mřížky s hledáním) → „Navrhnout balík“. Uvidíš počty, křivku a seznam; „Vytvořit balík a otevřít“ ho uloží.",
        mock = { MockCommanderGrid() },
    ),
    GuideNode(
        "a_combos", "Komba",
        "Najde komba v celé sbírce nebo v balíku (databáze Commander Spellbook, potřebuje internet). Kompletní komba a komba, kterým chybí jedna karta — tu přidáš na wishlist s poznámkou.",
        mock = { MockCombo() },
    ),
    GuideNode(
        "a_hand", "Zkušební ruka",
        "Zamíchá balík a lízne 7 karet. Ukáže počet zemí a radu (drž / mulligan). Tlačítka Mulligan, Líznout, Nová.",
        mock = { MockHand() },
    ),
    // ---------- Počítadlo
    GuideNode(
        "life", "Počítadlo životů",
        "Ikona počítadla v horní liště. Levá půlka dlaždice −1, pravá +1, podržení ±5. ☠ = jed. Kulaté tlačítko uprostřed = nastavení.",
        mock = { MockLife() },
        children = listOf("l_menu", "l_turns", "l_history"),
    ),
    GuideNode(
        "l_menu", "Nastavení hry",
        "Hráči 2–6, životy 20/30/40, den/noc (swipem kolečka přepneš), nezhasínání + úspora baterie, celá obrazovka, nová hra.",
        mock = { MockLifeMenu() },
    ),
    GuideNode(
        "l_turns", "Režim tahů",
        "Hráč na tahu má zlatý rámeček, v rohu čas a dole „Konec tahu“. Křivka doporučeného času: po překročení dlaždice bliká a telefon zavibruje.",
        mock = { MockTurnTile() },
    ),
    GuideNode(
        "l_history", "Historie životů",
        "Graf a seznam dávek změn i konců tahů. ↶ u záznamu vrátí hru přesně do toho stavu.",
        mock = { MockHistory() },
    ),
    // ---------- Nastavení
    GuideNode(
        "settings", "Nastavení",
        "Aktualizace appky z GitHubu, záloha a obnova dat, O aplikaci (verze, průvodce, log pádů — tlačítkem ho zkopíruješ a pošleš).",
        mock = { MockSettings() },
    ),
).associateBy { it.id }

@Composable
fun GuideOverlay(onClose: () -> Unit) {
    val stack = remember { mutableStateListOf("root") }
    val node = NODES.getValue(stack.last())
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            Modifier.fillMaxWidth(0.94f).heightIn(max = 720.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF231C45),
            border = BorderStroke(1.5.dp, GoldBorderBrush),
        ) {
            Column(Modifier.padding(18.dp)) {
                // Drobečková navigace
                Text(
                    stack.joinToString("  ›  ") { NODES.getValue(it).title },
                    style = MaterialTheme.typography.labelSmall, color = Muted, maxLines = 1,
                )
                Spacer(Modifier.height(6.dp))
                AnimatedContent(targetState = node, label = "guide", modifier = Modifier.weight(1f, fill = false)) { n ->
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        Text(n.title, style = MaterialTheme.typography.titleLarge, color = GoldLight)
                        Spacer(Modifier.height(10.dp))
                        n.mock?.let {
                            PhoneFrame { it() }
                            Spacer(Modifier.height(12.dp))
                        }
                        Text(n.body, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
                        if (n.children.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                if (n.id == "root") "Témata" else "Víc informací",
                                style = MaterialTheme.typography.labelLarge, color = Gold,
                            )
                            n.children.forEach { id ->
                                val c = NODES.getValue(id)
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color.Black.copy(alpha = 0.22f))
                                        .border(1.dp, Color(0x44F6DE9A), RoundedCornerShape(14.dp))
                                        .clickable { stack.add(id) }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(c.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                    if (c.children.isNotEmpty()) Text("${c.children.size}  ", style = MaterialTheme.typography.labelSmall, color = Muted)
                                    Text("›", color = Gold, fontSize = 20.sp)
                                }
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    if (stack.size > 1) TextButton(onClick = { stack.removeAt(stack.lastIndex) }) { Text("‹ Zpět") }
                    else Spacer(Modifier.width(1.dp))
                    TextButton(onClick = onClose) { Text("Zavřít") }
                }
            }
        }
    }
}

// ============================================================== Ilustrace

/** Rámeček „telefonu“ — v něm je zjednodušený výřez obrazovky. */
@Composable
private fun PhoneFrame(content: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(210.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF1A1536), Color(0xFF120E22))))
            .border(2.dp, Color(0xFF4A3F7A), RoundedCornerShape(20.dp))
            .padding(10.dp),
        content = content,
    )
}

/** Pulzující kroužek = „sem ťukni“. */
@Composable
private fun TapHint(modifier: Modifier, label: String? = null) {
    val t by rememberInfiniteTransition(label = "tap").animateFloat(
        0.6f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "tapA",
    )
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size((30 * t).dp).clip(CircleShape)
                .background(GoldLight.copy(alpha = 0.35f))
                .border(2.dp, GoldLight, CircleShape)
        )
        label?.let {
            Text(
                it, Modifier.background(Ink.copy(alpha = 0.85f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall, color = GoldLight,
            )
        }
    }
}

@Composable
private fun MiniCard(w: Int = 34, h: Int = 47, hue: Color = Color(0xFF6B4E2A)) {
    Box(
        Modifier.size(w.dp, h.dp).clip(RoundedCornerShape(4.dp))
            .background(Brush.verticalGradient(listOf(hue, Color(0xFF2E2A20))))
            .border(1.5.dp, GoldBorderBrush, RoundedCornerShape(4.dp))
    ) {
        Box(Modifier.padding(4.dp).fillMaxWidth().height(5.dp).background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(2.dp)))
    }
}

@Composable
private fun Line(w: Float, color: Color = Color.White.copy(alpha = 0.75f), h: Int = 7) {
    Box(Modifier.fillMaxWidth(w).height(h.dp).clip(RoundedCornerShape(3.dp)).background(color))
}

@Composable
private fun MockCardRow(
    highlightThumb: Boolean = false,
    highlightIcon: Int = -1,
    label: String? = null,
    note: String? = null,
) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().align(Alignment.Center), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) { i ->
                Row(Modifier.fillMaxWidth().background(Panel.copy(alpha = if (i == 1) 1f else 0.5f), RoundedCornerShape(8.dp)).padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        MiniCard()
                        if (i == 1 && highlightThumb) TapHint(Modifier.align(Alignment.Center).offset(y = 4.dp), label)
                    }
                    Column(Modifier.weight(1f).padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Line(0.7f)
                        Row { Box(Modifier.weight(1f)) { Line(0.8f, Gold.copy(alpha = 0.8f), 6) }; Text(" 3,25 €", fontSize = 9.sp, color = Green) }
                        if (i == 1 && note != null) Text(note, fontSize = 9.sp, color = Lav, maxLines = 1)
                    }
                    Box { Text("⇄", color = Color.White, fontSize = 14.sp); if (i == 1 && highlightIcon == 0) TapHint(Modifier.offset((-8).dp, (-6).dp), label) }
                    Spacer(Modifier.width(10.dp))
                    Box { Text("📖", fontSize = 13.sp); if (i == 1 && highlightIcon == 1) TapHint(Modifier.offset((-8).dp, (-6).dp), label) }
                }
            }
        }
    }
}

@Composable
private fun MockDetailDialog(showDelete: Boolean = false, deleteLabel: String = "Odstranit ze sbírky") {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth(0.85f).clip(RoundedCornerShape(14.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF34295F), Color(0xFF1A1536))))
                .border(1.dp, GoldBorderBrush, RoundedCornerShape(14.dp)).padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text("Llanowar Elves", fontSize = 12.sp, color = Gold, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) { Pip(Color(0xFF3E8E4F)) }
            Line(0.6f, Muted, 5)
            Text("{T}: Add {G}.", fontSize = 10.sp, color = Color.White)
            Text("Flying (can't be blocked except by…)", fontSize = 9.sp, color = Lav, fontWeight = FontWeight.Light)
            Text("Přeložit vysvětlivky do češtiny", fontSize = 9.sp, color = Gold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (showDelete) Box {
                    Text(deleteLabel, fontSize = 10.sp, color = Color(0xFFFF8A8A))
                    TapHint(Modifier.offset(20.dp, (-4).dp))
                }
                Spacer(Modifier.width(14.dp)); Text("Zavřít", fontSize = 10.sp, color = Gold)
            }
        }
    }
}

@Composable
private fun Pip(c: Color) = Box(Modifier.size(12.dp).clip(CircleShape).background(c).border(1.dp, Color.Black.copy(alpha = 0.4f), CircleShape))

@Composable
private fun MockBigCard() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(30.dp, 140.dp).clip(RoundedCornerShape(8.dp)).background(Color(0x553A2E66)))
            MiniCard(110, 154, Color(0xFF365A3A))
            Box(Modifier.size(30.dp, 140.dp).clip(RoundedCornerShape(8.dp)).background(Color(0x553A2E66)))
        }
        Text("‹  swipe  ›", Modifier.align(Alignment.BottomCenter), color = GoldLight, fontSize = 11.sp)
    }
}

@Composable
private fun MockSwipe() {
    val t by rememberInfiniteTransition(label = "sw").animateFloat(0f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Restart), label = "swA")
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().height(60.dp).align(Alignment.Center).background(Color(0x332E9E5B), RoundedCornerShape(8.dp)))
        Box(Modifier.fillMaxWidth(0.5f).fillMaxHeight().align(Alignment.CenterEnd).border(1.dp, Color(0x55FFFFFF), RoundedCornerShape(4.dp)))
        Text("půlka displeje", Modifier.align(Alignment.TopCenter), fontSize = 9.sp, color = Muted)
        Row(
            Modifier.fillMaxWidth().align(Alignment.Center).offset(x = (150 * t).dp)
                .background(Panel, RoundedCornerShape(8.dp)).padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MiniCard(); Spacer(Modifier.width(8.dp)); Column(Modifier.weight(1f)) { Line(0.6f); Spacer(Modifier.height(4.dp)); Line(0.4f, Gold, 5) }
        }
        Text("→ do balíku", Modifier.align(Alignment.BottomEnd), color = Green, fontSize = 11.sp)
    }
}

@Composable
private fun MockHoldRing() {
    val t by rememberInfiniteTransition(label = "hr").animateFloat(0f, 1f, infiniteRepeatable(tween(2200), RepeatMode.Restart), label = "hrA")
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        androidx.compose.material3.CircularProgressIndicator(progress = { t }, Modifier.size(70.dp), color = Gold, strokeWidth = 5.dp)
        Text("📖", fontSize = 26.sp)
        Text("drž 3 s → Gatherer", Modifier.align(Alignment.BottomCenter), color = GoldLight, fontSize = 11.sp)
    }
}

@Composable
private fun MockChips(labels: List<String>, selected: Int, trailing: String? = null) {
    Box(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(top = 30.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            labels.forEachIndexed { i, l ->
                Box {
                    Text(
                        l,
                        Modifier.clip(RoundedCornerShape(8.dp)).background(if (i == selected) Gold else Color.Transparent)
                            .border(1.dp, Gold, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 5.dp),
                        fontSize = 11.sp, color = if (i == selected) Ink else GoldLight,
                    )
                    if (i == selected) TapHint(Modifier.align(Alignment.Center).offset(y = 18.dp))
                }
            }
            trailing?.let { Spacer(Modifier.weight(1f)); Text(it, fontSize = 11.sp, color = Gold) }
        }
        Column(Modifier.fillMaxWidth().align(Alignment.BottomCenter), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(2) { Row(Modifier.fillMaxWidth().background(Panel.copy(alpha = 0.6f), RoundedCornerShape(8.dp)).padding(6.dp)) { MiniCard(); Spacer(Modifier.width(8.dp)); Column { Line(0.5f); Spacer(Modifier.height(4.dp)); Line(0.3f, Gold, 5) } } }
        }
    }
}

@Composable
private fun MockScanner(mode: Int) {
    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(Color(0xFF2A2A2A))) {
        Row(Modifier.fillMaxWidth().padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf("Jméno", "Roh", "Multisken").forEachIndexed { i, l ->
                Text(l, Modifier.clip(RoundedCornerShape(6.dp)).background(if (i == mode) Gold else Color.Transparent).border(1.dp, Gold, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 3.dp),
                    fontSize = 9.sp, color = if (i == mode) Ink else GoldLight)
            }
            Spacer(Modifier.weight(1f))
            Text("✕", Modifier.size(22.dp).clip(CircleShape).background(Color(0xAA1E1633)), color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
        Text("⚡", Modifier.align(Alignment.TopEnd).padding(top = 38.dp, end = 8.dp).size(24.dp).clip(CircleShape).background(Color(0xAA1E1633)), textAlign = TextAlign.Center, color = Color.White)
        // karta v záběru
        Box(Modifier.align(Alignment.Center).size(96.dp, 134.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF5B4A2E)).border(1.dp, Color(0xFF222222), RoundedCornerShape(6.dp))) {
            if (mode == 0) Box(Modifier.padding(5.dp).fillMaxWidth().height(12.dp).border(2.dp, GoldLight, RoundedCornerShape(3.dp)))
            if (mode == 1) Box(Modifier.align(Alignment.BottomStart).padding(3.dp).size(54.dp, 20.dp).border(2.dp, GoldLight, RoundedCornerShape(3.dp))) {
                Text("123/280 R\nDMU • DE", fontSize = 6.sp, color = Color.White, lineHeight = 7.sp, modifier = Modifier.padding(2.dp))
            }
        }
        Text(if (mode == 0) "namiř na jméno" else "namiř na levý spodní roh", Modifier.align(Alignment.BottomCenter).padding(6.dp), fontSize = 10.sp, color = GoldLight)
    }
}

@Composable
private fun MockFoundButtons() {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { MiniCard(44, 61); Spacer(Modifier.width(8.dp)); Column { Text("Llanowar Elves", fontSize = 11.sp, color = Gold); Line(0.5f, Muted, 5) } }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { MockBtn("Do sbírky", true); MockBtn("Do balíku") }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { MockBtn("Wishlist"); MockBtn("Playground") }
        Text("Skenovat dál", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 11.sp, color = Gold)
    }
}

@Composable
private fun RowScope.MockBtn(label: String, filled: Boolean = false) {
    Text(
        label,
        Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(if (filled) Gold else Color.Transparent)
            .border(1.dp, Gold, RoundedCornerShape(16.dp)).padding(vertical = 6.dp),
        textAlign = TextAlign.Center, fontSize = 11.sp, color = if (filled) Ink else GoldLight,
    )
}

@Composable
private fun MockMulti() {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { repeat(4) { MiniCard(40, 56, listOf(Color(0xFF365A3A), Color(0xFF5B2E2E), Color(0xFF2E3E5B), Color(0xFF5B4A2E))[it]) } }
        Text("Hledám karty 3/4… (nalezeno 3)", fontSize = 11.sp, color = Gold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Vyfotit karty", Modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFF6A4BC4)).padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 11.sp, color = Color.White)
            Text("Z galerie", Modifier.clip(RoundedCornerShape(16.dp)).border(1.dp, Gold, RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 11.sp, color = GoldLight)
        }
    }
}

@Composable
private fun MockTopBar() {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(8.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Sbírka", fontSize = 16.sp, color = Gold, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("⊞", fontSize = 18.sp, color = Color.White); Spacer(Modifier.width(18.dp))
            Box { Text("+", fontSize = 22.sp, color = Color.White); TapHint(Modifier.offset((-6).dp, (-2).dp)) }
        }
        Spacer(Modifier.height(10.dp))
        Text("⊞ = počítadlo životů   + = ruční přidání", fontSize = 10.sp, color = Muted)
    }
}

@Composable
private fun MockCollection() {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("Sbírka", "Wishlist", "Draft", "Playground").forEachIndexed { i, l ->
                Text(l, Modifier.clip(RoundedCornerShape(6.dp)).background(if (i == 0) Gold else Color.Transparent).border(1.dp, Gold, RoundedCornerShape(6.dp)).padding(horizontal = 5.dp, vertical = 3.dp), fontSize = 9.sp, color = if (i == 0) Ink else GoldLight)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("102 ks · 299,13 €", fontSize = 10.sp, color = Green, modifier = Modifier.weight(1f))
            Text("▦", color = Color.White, fontSize = 13.sp); Spacer(Modifier.width(10.dp))
            Box { Text("⚙", color = Color.White, fontSize = 13.sp); TapHint(Modifier.offset((-8).dp, (-8).dp)) }
        }
        repeat(2) { Row(Modifier.fillMaxWidth().background(Panel.copy(alpha = 0.6f), RoundedCornerShape(8.dp)).padding(6.dp)) { MiniCard(); Spacer(Modifier.width(8.dp)); Column { Line(0.5f); Spacer(Modifier.height(4.dp)); Line(0.3f, Gold, 5) } } }
    }
}

@Composable
private fun MockDeck() {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Elfové · 60/60 · 84,20 €", fontSize = 11.sp, color = Gold, modifier = Modifier.weight(1f))
            Text("📊", fontSize = 12.sp); Spacer(Modifier.width(8.dp)); Text("⚙", fontSize = 12.sp, color = Color.White)
        }
        MockBasicsRow()
        repeat(2) { Row(Modifier.fillMaxWidth().background(Panel.copy(alpha = 0.6f), RoundedCornerShape(8.dp)).padding(6.dp)) { MiniCard(28, 39); Spacer(Modifier.width(8.dp)); Column { Line(0.5f); Spacer(Modifier.height(4.dp)); Line(0.3f, Gold, 5) } } }
    }
}

@Composable
private fun MockBasicsRow() {
    Row(Modifier.fillMaxWidth().border(1.dp, GoldBorderBrush, RoundedCornerShape(10.dp)).padding(6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        listOf(Color(0xFFF3EBC9) to "4", Color(0xFF4A86C8) to "0", Color(0xFF555555) to "0", Color(0xFFD0563C) to "6", Color(0xFF3E8E4F) to "12").forEach { (c, n) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(18.dp).clip(CircleShape).background(c)); Text(n, fontSize = 10.sp, color = Color.White); Text("−  +", fontSize = 9.sp, color = Gold) }
        }
    }
}

@Composable
private fun MockBasics() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box { MockBasicsRow(); TapHint(Modifier.align(Alignment.CenterEnd).offset((-14).dp, (-10).dp), "+1") }
    }
}

@Composable
private fun MockCurve() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Row(Modifier.fillMaxWidth().height(110.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(0 to 0, 2 to 1, 6 to 3, 5 to 4, 3 to 3, 2 to 1, 1 to 1, 0 to 1).forEachIndexed { i, (cr, ot) ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    val tot = cr + ot
                    if (tot > 0) Text("$tot", fontSize = 9.sp, color = Color.White)
                    Box(Modifier.fillMaxWidth().height((ot * 9).dp).background(Color(0xFF8C73D9)))
                    Box(Modifier.fillMaxWidth().height((cr * 9).dp).background(Gold))
                    Text(if (i == 7) "7+" else "$i", fontSize = 9.sp, color = Muted)
                }
            }
            Text("Ø 2.9", fontSize = 10.sp, color = Gold)
        }
    }
}

@Composable
private fun MockPool() {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Rádce · Sealed deck", fontSize = 11.sp, color = Gold)
        Text("✓ Karet 40/40   ! Removal 2 (aspoň 3)", fontSize = 10.sp, color = Color.White)
        Text("Doporučení: hraj G + R", fontSize = 10.sp, color = Gold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box { Text("Vyfotit karty", Modifier.clip(RoundedCornerShape(14.dp)).background(Gold).padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 10.sp, color = Ink); TapHint(Modifier.align(Alignment.Center)) }
            Text("Pool do sbírky", Modifier.clip(RoundedCornerShape(14.dp)).border(1.dp, Gold, RoundedCornerShape(14.dp)).padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 10.sp, color = GoldLight)
        }
    }
}

@Composable
private fun MockHub() {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("📈 Analýza balíku", "🛠 Postavit balík ze sbírky", "⇄ Průvodce Draft / Sealed", "✨ Komba ze sbírky").forEachIndexed { i, t ->
            Box(Modifier.fillMaxWidth()) {
                Text(t, Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, GoldBorderBrush, RoundedCornerShape(12.dp)).background(Gold.copy(alpha = 0.08f)).padding(9.dp), fontSize = 11.sp, color = Color.White)
                if (i == 0) TapHint(Modifier.align(Alignment.CenterEnd).padding(end = 10.dp))
            }
        }
    }
}

@Composable
private fun MockCommanderGrid() {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Hledat velitele…", Modifier.fillMaxWidth().border(1.dp, Muted, RoundedCornerShape(6.dp)).padding(5.dp), fontSize = 10.sp, color = Muted)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            repeat(3) { i ->
                Box { MiniCard(52, 72, listOf(Color(0xFF365A3A), Color(0xFF5B2E2E), Color(0xFF2E3E5B))[i])
                    if (i == 1) Text("✓", Modifier.align(Alignment.TopEnd).padding(2.dp).size(16.dp).clip(CircleShape).background(Gold), textAlign = TextAlign.Center, fontSize = 10.sp, color = Ink) }
            }
        }
        Text("Navrhnout balík", Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Gold).padding(6.dp), textAlign = TextAlign.Center, fontSize = 11.sp, color = Ink)
    }
}

@Composable
private fun MockCombo() {
    Column(Modifier.fillMaxSize().border(1.dp, Color(0x55B9A7EC), RoundedCornerShape(12.dp)).padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { MiniCard(44, 61, Color(0xFF2E3E5B)); MiniCard(44, 61, Color(0xFF3A2A3A)); Box { MiniCard(44, 61, Color(0xFF444444)); Text("✗", Modifier.align(Alignment.Center), color = Color(0xFFFFA0A0), fontSize = 20.sp) } }
        Text("Výsledek: Infinite mana", fontSize = 10.sp, color = Green)
        Box { Text("Na wishlist", Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(1.dp, Gold, RoundedCornerShape(14.dp)).padding(5.dp), textAlign = TextAlign.Center, fontSize = 10.sp, color = GoldLight); TapHint(Modifier.align(Alignment.Center)) }
    }
}

@Composable
private fun MockHand() {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("V ruce 7 karet · zemí 3", fontSize = 10.sp, color = Color.White)
        Text("Hratelná ruka — drž.", fontSize = 11.sp, color = Gold, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { repeat(7) { MiniCard(30, 42, if (it < 3) Color(0xFF6B6B4A) else Color(0xFF365A3A)) } }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { MockBtn("Mulligan"); MockBtn("Líznout"); MockBtn("Nová", true) }
    }
}

@Composable
private fun MockLife() {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(Color(0xFF8E2C3A), Color(0xFF2B4F8E)).forEachIndexed { i, c ->
                Row(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c).border(1.dp, GoldBorderBrush, RoundedCornerShape(10.dp)), verticalAlignment = Alignment.CenterVertically) {
                    Text("−", Modifier.weight(1f), textAlign = TextAlign.Center, color = Color.White.copy(alpha = 0.6f), fontSize = 18.sp)
                    Text(if (i == 0) "18" else "20", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text("+", Modifier.weight(1f), textAlign = TextAlign.Center, color = Color.White.copy(alpha = 0.6f), fontSize = 18.sp)
                }
            }
        }
        Box(Modifier.align(Alignment.Center).size(40.dp).clip(CircleShape).background(Color(0xFF15102A)).border(2.dp, GoldBorderBrush, CircleShape), contentAlignment = Alignment.Center) { Text("⚙", color = GoldLight) }
        TapHint(Modifier.align(Alignment.Center))
    }
}

@Composable
private fun MockLifeMenu() {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Hra", fontSize = 10.sp, color = GoldLight)
        Row(Modifier.fillMaxWidth().border(1.dp, GoldBorderBrush, RoundedCornerShape(8.dp))) {
            (2..6).forEach { n -> Text("$n", Modifier.weight(1f).background(if (n == 4) Gold else Color.Transparent).padding(4.dp), textAlign = TextAlign.Center, fontSize = 10.sp, color = if (n == 4) Ink else GoldLight) }
        }
        Text("Den a noc", fontSize = 10.sp, color = GoldLight)
        Row(Modifier.fillMaxWidth().border(1.dp, GoldBorderBrush, RoundedCornerShape(8.dp))) {
            listOf("Vypnuto", "☀ Den", "☾ Noc").forEachIndexed { i, l -> Text(l, Modifier.weight(1f).background(if (i == 1) Gold else Color.Transparent).padding(4.dp), textAlign = TextAlign.Center, fontSize = 10.sp, color = if (i == 1) Ink else GoldLight) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) { Text("Nezhasínat obrazovku", Modifier.weight(1f), fontSize = 10.sp, color = Color.White); Box(Modifier.size(28.dp, 14.dp).clip(CircleShape).background(Gold)) }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { MockBtn("Historie"); MockBtn("Nová hra", true) }
    }
}

@Composable
private fun MockTurnTile() {
    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(Color(0xFF8E2C3A)).border(3.dp, GoldBorderBrush, RoundedCornerShape(12.dp))) {
        Text("⏱ 1:23 / 2:00", Modifier.align(Alignment.TopStart).padding(8.dp).background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 11.sp, color = GoldLight)
        Text("Tah 3", Modifier.align(Alignment.TopEnd).padding(10.dp), fontSize = 10.sp, color = GoldLight)
        Text("20", Modifier.align(Alignment.Center), fontSize = 40.sp, color = Color.White, fontWeight = FontWeight.Bold)
        Box(Modifier.align(Alignment.BottomCenter).padding(8.dp)) {
            Text("Konec tahu", Modifier.clip(RoundedCornerShape(16.dp)).background(Gold).padding(horizontal = 14.dp, vertical = 6.dp), fontSize = 11.sp, color = Ink, fontWeight = FontWeight.Bold)
            TapHint(Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun MockHistory() {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(Triple("start", "20  20", Color.White), Triple("H2 −3", "20  17", Color(0xFFFFB4B4)), Triple("Tah 1 H1 0:52", "20  17", GoldLight), Triple("H1 +2", "22  17", Color(0xFFB6F2C4))).forEachIndexed { i, (a, b, c) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(a, Modifier.weight(1f), fontSize = 10.sp, color = c); Text(b, Modifier.weight(1f), fontSize = 10.sp, color = Color.White)
                Box { Text("↶", fontSize = 14.sp, color = GoldLight); if (i == 1) TapHint(Modifier.offset((-8).dp, (-6).dp)) }
            }
        }
    }
}

@Composable
private fun MockSettings() {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("O aplikaci", fontSize = 12.sp, color = Gold)
        Text("Antimarcipán · verze …", fontSize = 10.sp, color = Muted)
        Text("Spustit průvodce", Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Gold).padding(6.dp), textAlign = TextAlign.Center, fontSize = 11.sp, color = Ink)
        Text("Log pádů: zaznamenáno 1 pádů.", fontSize = 10.sp, color = Color(0xFFFF9A9A))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { MockBtn("Zkopírovat log", true); MockBtn("Smazat log") }
    }
}

@Composable
private fun MockHome() {
    Column(Modifier.fillMaxSize()) {
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(10.dp)).padding(8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("Sken", "Sbírka", "Balíky", "Rádce", "Nastavení").forEachIndexed { i, l ->
                Text(l, Modifier.clip(RoundedCornerShape(10.dp)).background(if (i == 1) Gold else Color.Transparent).padding(horizontal = 5.dp, vertical = 3.dp), fontSize = 9.sp, color = if (i == 1) Ink else Color.White)
            }
        }
        Text("Spodní lišta = hlavní části appky", Modifier.fillMaxWidth().padding(top = 6.dp), textAlign = TextAlign.Center, fontSize = 10.sp, color = Muted)
    }
}
