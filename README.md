# Antimarcipán

Android aplikace (Kotlin + Jetpack Compose) na skenování a digitalizaci karet
Magic: The Gathering, stavění balíků a návrhy synergie — plus mód **Anti Marcipán**,
který rozebere balík a radí při hře.

> Tohle je funkční **kostra** projektu k otevření v Android Studiu. Hlavní
> dráty (kamera → OCR → Scryfall → lokální sbírka → balíky → EDHREC → advisor)
> jsou napojené. Je to startovní bod, ne hotový produkt z obchodu.

## Co umí (MVP)

- **Sken** — živý náhled kamery (CameraX) + OCR jména karty (ML Kit, běží
  v telefonu). Jméno se pošle na **Scryfall** (fuzzy hledání, snese překlepy
  z OCR), karta se zobrazí a uloží do sbírky.
- **Sbírka** — tvoje lokální „album" karet v databázi v telefonu (Room). Funguje
  offline, počítá kusy a ceny.
- **Balíky** — zakládání balíků, přidávání karet ze sbírky, nastavení velitele.
- **Synergie** — pro velitele balíku stáhne doporučené karty z **EDHREC**
  (seřazené podle % synergie) a označí, které už máš ve sbírce.
- **Anti Marcipán** — offline advisor. Projde balík (počet zemí, ramp, card
  draw, removal, manovou křivku, barevnou identitu vs. velitel) a vypíše
  varování + rady do hry (mulligan, sekvencování).

## Stack

| Vrstva | Co |
|---|---|
| UI | Jetpack Compose, Material 3, Navigation Compose |
| Kamera / OCR | CameraX + ML Kit Text Recognition (on-device) |
| Data o kartách | Scryfall API (`api.scryfall.com`) — zdarma, bez klíče |
| Synergie | Neoficiální EDHREC JSON (`json.edhrec.com`) |
| Síť | Retrofit + OkHttp + kotlinx.serialization |
| Lokální úložiště | Room (SQLite) |
| Obrázky | Coil |

## Jak spustit

1. **Android Studio** (Koala / 2024.1 nebo novější). Otevři složku `Antimarcipan`.
2. Studio samo stáhne Gradle (8.10.2) a knihovny při prvním **Sync**.
   - Je potřeba JDK 17 (Studio ho má v sobě).
3. Připoj telefon (USB debugging) nebo spusť emulátor s kamerou.
4. **Run ▶**. Při prvním skenu appka poprosí o přístup ke kameře.

> Projekt nejde sestavit v prostředí bez přístupu na `dl.google.com` a Maven
> Central — Android Gradle Plugin a knihovny se stahují odtud. V Android Studiu
> s běžným připojením to proběhne automaticky.

## Struktura

```
app/src/main/java/com/example/antimarcipan/
├─ MainActivity.kt            vstupní bod + Compose
├─ MtgApp.kt                  Application + jednoduchý DI kontejner
├─ data/
│  ├─ scryfall/               Scryfall API + modely
│  ├─ edhrec/                 EDHREC API + slug velitele
│  ├─ local/                  Room: entity, DAO, databáze
│  ├─ advisor/AdvisorEngine   logika „Anti Marcipán" (čistý Kotlin)
│  ├─ CollectionRepository    OCR → karta, správa sbírky
│  ├─ DeckRepository          balíky
│  └─ SynergyRepository       EDHREC doporučení + průnik se sbírkou
├─ scan/                      kamera, OCR analyzer, ScanScreen + VM
├─ collection/                CollectionScreen + VM
├─ decks/                     DeckScreen + VM
├─ synergy/                   SynergyScreen + VM
├─ advisor/                   AdvisorScreen (Anti Marcipán) + VM
└─ ui/                        navigace, téma, společné UI, VM factory
```

## Kam dál (nápady)

- **Lepší rozpoznávání** — místo OCR jména přidat obrázkové matchování
  (perceptual hash proti art cropům ze Scryfall) kvůli foilům a cizím jazykům.
- **Dávkové skenování 3×5** — přes OpenCV detekovat víc karet v jednom záběru,
  každou oříznout a pustit OCR zvlášť.
- **Výběr edice/variantu** — když fuzzy hledání trefí víc tisků, nechat uživatele
  vybrat konkrétní set a sběratelské číslo.
- **Export** — decklist do Moxfieldu / Archidektu (textový i přes link).
- **Chytřejší Anti Marcipán** — komba z Commander Spellbook API, tagy archetypů,
  rady podle aktuální ruky.

## Poznámky k API

- **Scryfall**: limit ~10 požadavků/s, posílej `User-Agent` a `Accept` hlavičky
  (appka to dělá a throttluje v `data/Network.kt`). Data o kartách jde i stáhnout
  v bulku a mít offline.
- **EDHREC**: `json.edhrec.com` je komunitní, neoficiální endpoint bez záruky —
  ber ho jako best-effort, může se změnit.

## Licence / data

Karty, texty a ceny patří jejich vlastníkům; Scryfall a EDHREC jsou jen zdroje
dat. Appka je pro osobní použití.
