package com.example.antimarcipan.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private data class GuideStep(
    val icon: ImageVector,
    val title: String,
    val body: String,
)

private val GUIDE_STEPS = listOf(
    GuideStep(
        Icons.Filled.TouchApp,
        "Vítej v Antimarcipánu",
        "Appka na skenování karet Magicu, správu sbírky, stavbu balíků a hraní. Ovládá se hlavně gesty, tak si je projdeme:\n\n• Ťuknutí na náhled karty → detail s textací.\n• Podržení náhledu → celá karta ve velkém, swipem listuješ dál.\n• Swipe řádku doprava přes půlku displeje → přidání do balíku.\n• Dlouhé podržení (2–3 s) se ukazuje plnícím se kolečkem — např. otevření Gathereru. Mazání je vždy jen z detailu karty a s potvrzením.\n\nPrůvodce kdykoli pustíš znovu v Nastavení.",
    ),
    GuideStep(
        Icons.Filled.CameraAlt,
        "Skenování podle jména",
        "Záložka Sken → Spustit skenování (foťák se zapne až teď). Namiř na jméno karty nahoře.\n\n• Vpravo nahoře je tlačítko s bleskem — zapne světlo telefonu.\n• Nalezená karta zůstane na obrazovce, dokud nerozhodneš: Skenovat dál, Playground (uložit k prozkoumání), Wishlist, Do balíku nebo Do sbírky.\n• Ťuknutím na edici vybereš jiný tisk (obrázek, cena).\n• Pod kartou je anglická textace se symboly many; vysvětlivky vlastností (Flying, Dredge…) od appky jsou fialovou kurzívou.",
    ),
    GuideStep(
        Icons.Filled.Translate,
        "Cizojazyčné karty — režim Roh",
        "Německé, japonské a jiné karty se podle jména nenajdou. Přepni na „Roh“ a namiř do zlatého rámečku levý spodní roh karty: číslo karty (123/280 R) a kód edice s jazykem (DMU • DE).\n\nAppka podle toho najde přesný tisk a uloží ho s anglickou textací. Funguje u karet zhruba od roku 2015; starší roh nemají — ty přidej ručně.\n\nPozn.: překladač textací byl odstraněn, textace je anglicky s vysvětlivkami.",
    ),
    GuideStep(
        Icons.Filled.PhotoCamera,
        "Multisken z fotky",
        "Víc karet naráz: Sken → Multisken z fotky. Rozlož karty lícem nahoru vedle sebe, ťukni „Vyfotit karty“ (otevře se fotoaparát telefonu) nebo vyber fotku „Z galerie“.\n\nPak uvidíš průběh: čtu text → hledám karty 3/12 → hotovo. Nalezené karty zkontroluješ a potvrdíš najednou; špatnou přenačteš.\n\nTip: rovně, dobré světlo, bez odlesků na názvech.",
    ),
    GuideStep(
        Icons.Filled.Add,
        "Ruční přidání",
        "Ikona + v horní liště: napiš jméno, vyber kartu ze Scryfallu (ťuknutím na náhled vidíš detail s obrázkem a textací) a přidej ji do sbírky, na wishlist nebo do balíku.",
    ),
    GuideStep(
        Icons.Filled.ViewModule,
        "Sbírka",
        "Záložky nahoře:\n• Sbírka — co vlastníš.\n• Wishlist — co chceš.\n• Draft — rozpracované karty z poolu draftu/sealedu.\n• Playground — karty odložené k prozkoumání.\n\nPod nimi počet kusů a celková cena (€, případně \$). Vpravo přepnutí seznam/mřížka a filtr (jméno, text, barvy, MV, typy, tribal, vlastnosti, edice, rarita…). Řádek Vše/Land/Spell + řazení (jméno, cena, MV…).",
    ),
    GuideStep(
        Icons.Filled.Style,
        "Gesta na kartě ve sbírce",
        "• Ťuk na náhled → detail (textace, cena, počet kusů).\n• Podržení náhledu → velká karta se zlatým rámečkem; swipem doleva/doprava listuješ celým seznamem.\n• Swipe řádku doprava přes půlku → vybereš balík a karta se přidá.\n• Ikona ⇄ → změna edice (obrázek i cena); přepíše se i v balících.\n• Smazání jen z detailu karty (ťuk na náhled → červené „Odstranit ze sbírky“), vždy s potvrzením.\n• Ve Wishlistu/Draftu/Playgroundu swipe přesune kartu do sbírky.",
    ),
    GuideStep(
        Icons.AutoMirrored.Filled.MenuBook,
        "Gatherer a rulings",
        "Ikona knížky u karty:\n• Ťuknutí → oficiální rulings (výklady pravidel) ve scrollovacím okně.\n• Podržení 3 s (plní se kolečko) → otevře kartu na Gathereru, oficiálním webu Wizards of the Coast, s kompletními informacemi.\n\nZ okna s rulings se dá na Gatherer přejít i tlačítkem.",
    ),
    GuideStep(
        Icons.Filled.Inventory2,
        "Balíky",
        "+ Nový balík: vyber formát — Commander, Jednokarťák, Standard (60 karet, max 4 kopie), Draft, Sealed nebo Volný.\n\nV detailu balíku:\n• Panel základních zemí: ťukni na symbol many (+1) nebo −.\n• Vše = všechno podle abecedy, Land/Spell = rozdělené.\n• V hlavičce počet karet, barvy a celková cena.\n• Korunkou určíš velitele. Kartu odebereš z detailu (ťuk na náhled → „Odebrat z balíku“) nebo podržením řádku — vždy s potvrzením.\n• Menu ⋮: export decklistu, přejmenování, smazání.",
    ),
    GuideStep(
        Icons.Filled.SwapHoriz,
        "Draft a Sealed",
        "Limited balík se staví z poolu, ne ze sbírky:\n1. V rádci balíku „Vyfotit karty“ — karty z boosterů jdou rovnou do poolu.\n2. Sekce Pool: + přesune kartu do balíku, koš ji z poolu odstraní.\n3. Rádce hlídá 40 karet, ~17 zemí, 15–17 tvorů, removal, křivku; doporučí 2 barvy, nejlepší karty a tlačítkem rozdělí základní země.\n4. „Pool do sbírky“ na konci přidá vše do sbírky naráz.\n\n„Jak se hraje“ vysvětlí průběh formátu i pick order.",
    ),
    GuideStep(
        Icons.Filled.AutoAwesome,
        "Rádce",
        "Vyber balík a Anti Marcipán ho prozkoumá: mana curve (sloupce podle mana value, tvorové vs. ostatní, průměrná MV), počty zemí, rampu, dobírání, removal a dá konkrétní rady. Navržené karty mají obrázek — ťukni pro detail.",
    ),
    GuideStep(
        Icons.Filled.Favorite,
        "Počítadlo životů",
        "Ikona počítadla v horní liště. Stav se drží, i když odejdeš jinam.\n\n• Ťuk levá/pravá půlka −1/+1, podržení −5/+5. ☠ jed: ťuk +1, podržení −1.\n• Kulaté tlačítko uprostřed = nastavení: hráči 2–6, životy 20/30/40, den/noc, celá obrazovka, nezhasínání + úspora baterie, reset.\n• Režim tahů: „Konec tahu“, čas tahu v rohu, křivka doporučeného času (blikání + vibrace po překročení).\n• Historie životů: graf a záznamy; ↶ vrátí hru k libovolnému záznamu.",
    ),
    GuideStep(
        Icons.Filled.Settings,
        "Nastavení",
        "Aktualizace aplikace z GitHubu, záloha a obnova dat, vzhled a tento průvodce. Ceny karet se obnovují samy (nejvýš jednou za 12 h) ze Scryfallu.",
    ),
)

/**
 * Jednoduchý průvodce aplikací — krokovaný přehled funkcí. Zobrazí se při
 * prvním spuštění a dá se kdykoli vyvolat z horní lišty nebo z Nastavení.
 */
@Composable
fun GuideOverlay(onClose: () -> Unit) {
    var index by remember { mutableIntStateOf(0) }
    val step = GUIDE_STEPS[index]
    val last = index == GUIDE_STEPS.lastIndex

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = true),
    ) {
        androidx.compose.material3.Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
            color = androidx.compose.ui.graphics.Color(0xFF231C45),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldBorderBrush),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.size(72.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        step.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(36.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    step.title,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Column(
                    Modifier.heightIn(max = 340.dp)
                        .verticalScroll(androidx.compose.foundation.rememberScrollState(index)),
                ) {
                    Text(
                        step.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
                        textAlign = TextAlign.Start,
                    )
                }
                Text(
                    "${index + 1} / ${GUIDE_STEPS.size}",
                    Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(20.dp))

                // Tečky kroků
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GUIDE_STEPS.indices.forEach { i ->
                        Box(
                            Modifier.size(if (i == index) 9.dp else 7.dp).clip(CircleShape)
                                .background(
                                    if (i == index) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                                )
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onClose) { Text("Přeskočit") }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (index > 0) {
                            TextButton(onClick = { index-- }) { Text("Zpět") }
                        }
                        Button(onClick = { if (last) onClose() else index++ }) {
                            Text(if (last) "Hotovo" else "Dál")
                        }
                    }
                }
            }
        }
    }
}
