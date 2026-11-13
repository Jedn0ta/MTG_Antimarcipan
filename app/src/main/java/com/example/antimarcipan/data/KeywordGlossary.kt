package com.example.antimarcipan.data

/**
 * Stručná česká vysvětlení klíčových slov MTG. Používá se tam, kde karta
 * uvádí jen samotné heslo bez připomínkového textu (např. „Dredge 3").
 */
object KeywordGlossary {

    /** Všechna hesla — anglicky (výchozí). */
    val entries: Map<String, String> get() = GLOSSARY_EN

    /** Všechna hesla — česky (jen na vyžádání překladu). */
    val entriesCz: Map<String, String> get() = GLOSSARY

    private val GLOSSARY_EN: Map<String, String> = linkedMapOf(
        "flying" to "can't be blocked except by creatures with flying or reach",
        "first strike" to "deals combat damage before creatures without first strike",
        "double strike" to "deals both first-strike and regular combat damage",
        "deathtouch" to "any amount of damage it deals to a creature is enough to destroy it",
        "haste" to "can attack and {T} as soon as it comes under your control",
        "hexproof" to "can't be the target of spells or abilities your opponents control",
        "shroud" to "can't be the target of spells or abilities",
        "indestructible" to "damage and effects that say \"destroy\" don't destroy it",
        "lifelink" to "damage dealt by this also causes you to gain that much life",
        "menace" to "can't be blocked except by two or more creatures",
        "reach" to "can block creatures with flying",
        "trample" to "excess combat damage beyond lethal to blockers is dealt to the player",
        "vigilance" to "attacking doesn't cause it to tap",
        "defender" to "can't attack",
        "flash" to "you may cast it any time you could cast an instant",
        "ward" to "whenever it becomes the target of a spell or ability an opponent controls, counter it unless that player pays the ward cost",
        "protection" to "can't be blocked, targeted, dealt damage, enchanted or equipped by anything with the stated quality",
        "prowess" to "whenever you cast a noncreature spell, it gets +1/+1 until end of turn",
        "dredge" to "if you would draw a card, you may mill N cards instead and return this card from your graveyard to your hand",
        "cycling" to "pay the cost, discard this card: draw a card",
        "kicker" to "you may pay an additional cost as you cast this spell for an extra effect",
        "multikicker" to "you may pay the kicker cost any number of times",
        "flashback" to "you may cast this card from your graveyard for its flashback cost, then exile it",
        "equip" to "pay the cost: attach to target creature you control; equip only as a sorcery",
        "enchant" to "defines what this Aura can be attached to",
        "convoke" to "each creature you tap while casting this spell pays for {1} or one mana of its color",
        "delve" to "each card you exile from your graveyard while casting this spell pays for {1}",
        "cascade" to "when you cast this, exile cards from the top of your library until you hit a cheaper nonland card; you may cast it free",
        "storm" to "when you cast this, copy it for each spell cast before it this turn",
        "affinity" to "this spell costs {1} less for each permanent of the stated type you control",
        "persist" to "when it dies, if it had no -1/-1 counters, return it with a -1/-1 counter",
        "undying" to "when it dies, if it had no +1/+1 counters, return it with a +1/+1 counter",
        "evoke" to "you may cast it for its evoke cost; if you do, it's sacrificed when it enters",
        "madness" to "if you discard this card, you may cast it for its madness cost",
        "morph" to "you may cast it face down as a 2/2 for {3}; turn it face up any time for its morph cost",
        "megamorph" to "like morph, and it gets a +1/+1 counter when turned face up",
        "unearth" to "return it from your graveyard to the battlefield with haste; exile it at the next end step (sorcery speed)",
        "exalted" to "whenever a creature you control attacks alone, it gets +1/+1 until end of turn",
        "infect" to "deals damage to creatures as -1/-1 counters and to players as poison counters",
        "wither" to "deals damage to creatures in the form of -1/-1 counters",
        "annihilator" to "whenever it attacks, defending player sacrifices N permanents",
        "mentor" to "whenever it attacks, put a +1/+1 counter on target attacking creature with lesser power",
        "riot" to "enters with your choice of a +1/+1 counter or haste",
        "afterlife" to "when it dies, create N 1/1 white and black Spirit tokens with flying",
        "escape" to "you may cast it from your graveyard for its escape cost, exiling other cards from your graveyard",
        "mutate" to "cast it onto a non-Human creature you own to merge them",
        "embalm" to "exile it from your graveyard and pay: create a white Zombie token copy of it",
        "eternalize" to "exile it from your graveyard and pay: create a 4/4 black Zombie token copy of it",
        "exploit" to "when it enters, you may sacrifice a creature",
        "fabricate" to "when it enters, put N +1/+1 counters on it or create N 1/1 Servo tokens",
        "crew" to "tap creatures with total power N or more: it becomes an artifact creature until end of turn",
        "partner" to "you can have two commanders if both have partner",
        "toxic" to "players dealt combat damage by it also get N poison counters",
        "bargain" to "you may sacrifice an artifact, enchantment or token as you cast this spell for a bonus",
        "disturb" to "you may cast it transformed from your graveyard for its disturb cost",
        "ninjutsu" to "pay and return an unblocked attacker you control to hand: put this onto the battlefield tapped and attacking",
        "buyback" to "you may pay an additional cost; if you do, return this spell to your hand as it resolves",
        "echo" to "at the beginning of your next upkeep, sacrifice it unless you pay its echo cost",
        "fading" to "enters with N fade counters; remove one each upkeep, sacrifice it when you can't",
        "vanishing" to "enters with N time counters; remove one each upkeep, sacrifice it when the last is removed",
        "suspend" to "exile it from your hand with N time counters; remove one each upkeep, cast it free when the last is removed",
        "rebound" to "if cast from your hand, exile it as it resolves; cast it free at your next upkeep",
        "overload" to "pay the overload cost to change \"target\" to \"each\"",
        "bestow" to "cast it as an Aura granting its P/T; it becomes a creature again if the enchanted creature leaves",
        "dash" to "cast it for its dash cost: it gains haste and returns to your hand at end of turn",
        "changeling" to "this object is every creature type",
        "devoid" to "this card has no color",
        "emerge" to "you may cast it by sacrificing a creature and paying the emerge cost reduced by that creature's mana value",
        "improvise" to "each artifact you tap while casting this spell pays for {1}",
        "training" to "whenever it attacks with another creature with greater power, put a +1/+1 counter on it",
        "blitz" to "cast it for its blitz cost: haste and \"when it dies, draw a card\"; sacrifice it at end of turn",
        "casualty" to "as you cast this spell, you may sacrifice a creature with power N or greater to copy it",
        "plot" to "pay the plot cost and exile it from your hand; cast it free on a later turn as a sorcery",
        "offspring" to "pay an additional cost: when it enters, create a 1/1 token copy of it",
        "enlist" to "as it attacks, you may tap a non-attacking creature to add its power",
        "backup" to "when it enters, put N +1/+1 counters on target creature; if that's another creature, it gains these abilities until end of turn",
        "living weapon" to "when it enters, create a 0/0 black Phyrexian Germ token and attach this to it",
        "reconfigure" to "pay: attach to target creature you control or unattach; attached, it isn't a creature",
        "split second" to "as long as this spell is on the stack, players can't cast spells or activate non-mana abilities",
        "landwalk" to "can't be blocked as long as defending player controls a land of the stated type",
        "islandwalk" to "can't be blocked as long as defending player controls an Island",
        "swampwalk" to "can't be blocked as long as defending player controls a Swamp",
        "forestwalk" to "can't be blocked as long as defending player controls a Forest",
        "mountainwalk" to "can't be blocked as long as defending player controls a Mountain",
        "plainswalk" to "can't be blocked as long as defending player controls a Plains",
        "horsemanship" to "can't be blocked except by creatures with horsemanship",
        "shadow" to "can block or be blocked only by creatures with shadow",
        "fear" to "can't be blocked except by artifact and/or black creatures",
        "intimidate" to "can't be blocked except by artifact creatures and/or creatures that share a color with it",
        "skulk" to "can't be blocked by creatures with greater power",
        "daybound" to "if it's night, this transforms; follows the day/night cycle",
        "nightbound" to "if it's day, this transforms; follows the day/night cycle",
        "decayed" to "can't block; when it attacks, sacrifice it at end of combat",
        "explore" to "reveal the top card: a land goes to your hand, otherwise put a +1/+1 counter on it and you may put that card into your graveyard",
        "surveil" to "look at the top N cards, put any number into your graveyard and the rest back on top",
        "scry" to "look at the top N cards, put any on the bottom and the rest on top in any order",
    )

    private val GLOSSARY: Map<String, String> = linkedMapOf(
        "flying" to "může být blokována jen stvořeními s flying nebo reach",
        "first strike" to "v boji uděluje zranění dřív než stvoření bez first strike",
        "double strike" to "v boji uděluje zranění dvakrát – v kroku first strike i normálně",
        "deathtouch" to "jakékoli zranění od tohoto zdroje stvoření zničí",
        "haste" to "může útočit a používat {T} hned v tahu, kdy přišlo",
        "hexproof" to "nemůže být cílem kouzel a schopností soupeřů",
        "shroud" to "nemůže být cílem žádných kouzel ani schopností",
        "indestructible" to "nemůže být zničena (zranění ani efekty „destroy“ ji nezničí)",
        "lifelink" to "zranění, které udělí, ti přidá stejně životů",
        "menace" to "může být blokována jen dvěma nebo více stvořeními",
        "reach" to "může blokovat stvoření s flying",
        "trample" to "přebytečné zranění nad smrtelné blokujícím projde na hráče",
        "vigilance" to "útočení ji netapuje",
        "defender" to "nemůže útočit",
        "flash" to "lze sesílat kdykoli, kdy bys mohl sesílat instant",
        "ward" to "když se stane cílem soupeřova kouzla/schopnosti, zruš ho, pokud soupeř nezaplatí uvedenou cenu",
        "protection" to "nemůže být blokována, cílena, zraněna ani očarována tím, od čeho má protection",
        "prowess" to "když sešleš nestvořeníové kouzlo, dostane do konce tahu +1/+1",
        "dredge" to "místo líznutí můžeš millnout N karet a vrátit tuto kartu ze hřbitova do ruky",
        "cycling" to "zaplať cenu, zahoď kartu a lízni si novou",
        "kicker" to "při sesílání můžeš zaplatit navíc pro silnější efekt",
        "multikicker" to "kicker lze zaplatit libovolně krát",
        "flashback" to "lze ji sesílat ze hřbitova za uvedenou cenu, pak jde do exilu",
        "equip" to "zaplať cenu: připoj toto vybavení ke svému stvoření (jen jako sorcery)",
        "enchant" to "určuje, k čemu lze tuto auru připojit",
        "convoke" to "každé tvé stvoření, které tapneš, zaplatí {1} nebo jednu manu jeho barvy",
        "delve" to "každá karta, kterou vyexiluješ ze svého hřbitova, zaplatí {1}",
        "cascade" to "při seslání odkrývej knihovnu, dokud nenajdeš levnější nezemi; tu můžeš zdarma seslat",
        "storm" to "při seslání zkopíruj kouzlo za každé kouzlo seslané dříve v tomto tahu",
        "affinity" to "stojí o {1} méně za každou tvou permanentní kartu daného typu",
        "persist" to "když zemře bez -1/-1 žetonu, vrátí se do hry s -1/-1 žetonem",
        "undying" to "když zemře bez +1/+1 žetonu, vrátí se do hry s +1/+1 žetonem",
        "evoke" to "můžeš ji seslat levněji, ale hned po příchodu ji obětuješ",
        "madness" to "když ji zahodíš, můžeš ji seslat za cenu madness",
        "morph" to "můžeš ji seslat lícem dolů jako 2/2 za {3} a otočit za cenu morph",
        "megamorph" to "jako morph, a při otočení dostane +1/+1 žeton",
        "unearth" to "ze hřbitova ji vrať do hry s haste, na konci tahu jde do exilu",
        "exalted" to "když tvé stvoření útočí samo, dostane +1/+1 do konce tahu",
        "infect" to "zranění uděluje stvořením jako -1/-1 žetony a hráčům jako poison žetony",
        "wither" to "zranění stvořením uděluje jako -1/-1 žetony",
        "annihilator" to "když útočí, bránící hráč obětuje N permanentů",
        "mentor" to "když útočí, dej +1/+1 žeton útočícímu stvoření s menší silou",
        "riot" to "přijde buď s +1/+1 žetonem, nebo s haste",
        "afterlife" to "když zemře, vytvoř N bílo-černých 1/1 duchů s flying",
        "escape" to "lze ji seslat ze hřbitova za cenu escape a vyexilování dalších karet",
        "mutate" to "můžeš ji seslat na své ne-lidské stvoření a sloučit je",
        "embalm" to "vyexiluj ze hřbitova a zaplať: vytvoř bílý token-kopii (zombie)",
        "eternalize" to "vyexiluj ze hřbitova a zaplať: vytvoř černý 4/4 token-kopii (zombie)",
        "exploit" to "když přijde, můžeš obětovat stvoření",
        "fabricate" to "když přijde, dej mu N +1/+1 žetonů, nebo vytvoř N 1/1 servo tokenů",
        "crew" to "tapni stvoření se součtem síly N: vozidlo se do konce tahu stane stvořením",
        "partner" to "můžeš mít dva velitele, pokud oba mají partner",
        "toxic" to "když udělí bojové zranění hráči, dostane i N poison žetonů",
        "bargain" to "při sesílání můžeš obětovat artefakt, enchantment nebo token pro bonus",
        "disturb" to "můžeš ji ze hřbitova seslat přetočenou za cenu disturb",
        "ninjutsu" to "zaplať a vrať neblokované útočící stvoření do ruky: tuto kartu dej do hry tapnutou a útočící",
        "buyback" to "zaplať navíc a kouzlo se po vyhodnocení vrátí do ruky",
        "echo" to "na začátku tvé příští údržby zaplať echo, jinak ji obětuj",
        "fading" to "přijde s N fade žetony; každou údržbu jeden odeber, když nemůžeš, obětuj ji",
        "vanishing" to "přijde s N time žetony; každou údržbu jeden odeber, po posledním ji obětuj",
        "suspend" to "vyexiluj ji s N time žetony; každou údržbu jeden odeber, po posledním ji zdarma sešli",
        "rebound" to "seslaná z ruky jde do exilu a v příští údržbě ji můžeš zdarma seslat znovu",
        "overload" to "za cenu overload zasáhne místo „target“ všechno odpovídající",
        "bestow" to "můžeš ji seslat jako auru, která dává svou sílu/odolnost; když hostitel odejde, je to stvoření",
        "dash" to "sešli za cenu dash s haste; na konci tahu se vrátí do ruky",
        "changeling" to "je to všech typů stvoření najednou",
        "devoid" to "tato karta nemá barvu",
        "emerge" to "můžeš ji seslat obětováním stvoření; cena se sníží o jeho mana value",
        "improvise" to "každý artefakt, který tapneš, zaplatí {1}",
        "training" to "když útočí s silnějším stvořením, dostane +1/+1 žeton",
        "blitz" to "sešli za cenu blitz s haste; na konci tahu ji obětuj a lízni si",
        "casualty" to "při sesílání můžeš obětovat stvoření se sílou N a víc a kouzlo zkopírovat",
        "plot" to "zaplať a vyexiluj z ruky; v pozdějším tahu ji můžeš zdarma seslat jako sorcery",
        "offspring" to "zaplať navíc: když přijde, vytvoř 1/1 token-kopii",
        "enlist" to "když útočí, můžeš tapnout neútočící stvoření a přičíst si jeho sílu",
        "backup" to "když přijde, dej N +1/+1 žetonů cílovému stvoření; pokud je to jiné, získá i schopnosti",
        "living weapon" to "když přijde, vytvoř 0/0 černý germ token a připoj k němu toto vybavení",
        "reconfigure" to "zaplať: připoj ke stvoření nebo odpoj (připojené přestává být stvořením)",
        "split second" to "dokud je kouzlo na stacku, nelze sesílat kouzla ani aktivovat schopnosti",
        "landwalk" to "nemůže být blokována, pokud bránící hráč ovládá zemi daného typu",
        "islandwalk" to "nemůže být blokována, pokud bránící hráč ovládá Island",
        "swampwalk" to "nemůže být blokována, pokud bránící hráč ovládá Swamp",
        "forestwalk" to "nemůže být blokována, pokud bránící hráč ovládá Forest",
        "mountainwalk" to "nemůže být blokována, pokud bránící hráč ovládá Mountain",
        "plainswalk" to "nemůže být blokována, pokud bránící hráč ovládá Plains",
        "horsemanship" to "může být blokována jen stvořeními s horsemanship",
        "shadow" to "blokuje a je blokována jen stvořeními se shadow",
        "fear" to "může být blokována jen artefaktovými nebo černými stvořeními",
        "intimidate" to "může být blokována jen artefaktovými stvořeními nebo stvořeními sdílejícími barvu",
        "skulk" to "nemůže být blokována stvořeními s větší silou",
        "daybound" to "pokud je noc, otočí se; mění se podle cyklu den/noc",
        "nightbound" to "pokud je den, otočí se; mění se podle cyklu den/noc",
        "decayed" to "nemůže blokovat; když útočí, na konci boje ji obětuj",
        "explore" to "odkryj vrchní kartu: země jde do ruky, jinak +1/+1 žeton a kartu můžeš dát do hřbitova",
        "surveil" to "podívej se na vrchních N karet, libovolné dej do hřbitova a zbytek zpět navrch",
        "scry" to "podívej se na vrchních N karet a dej je navrch nebo dospod v libovolném pořadí",
    )

    /** Vysvětlení pro dané heslo (bez parametru), nebo null. */
    fun explain(keyword: String, czech: Boolean = false): String? =
        (if (czech) GLOSSARY else GLOSSARY_EN)[keyword.trim().lowercase()]

    /**
     * Doplní vysvětlení do závorky k řádkům, které obsahují jen klíčová slova
     * bez připomínkového textu („Flying, vigilance“, „Dredge 3“, „Ward {2}“).
     */
    fun annotate(oracle: String?, czech: Boolean = false): String? {
        if (oracle.isNullOrBlank()) return oracle
        return oracle.lines().joinToString("\n") { line -> annotateLine(line, czech) }
    }

    private fun annotateLine(line: String, czech: Boolean): String {
        val t = line.trim()
        if (t.isEmpty() || t.contains('(') || t.contains('.') || t.contains(':')) return line
        val tokens = t.split(Regex("[,;]\\s*")).map { it.trim() }.filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return line
        val found = tokens.map { matchKeyword(it, czech) ?: return line }
        val parts = found.map { (name, expl) ->
            if (found.size == 1) expl else "${name.replaceFirstChar { it.uppercase() }}: $expl"
        }
        // \uE000…\uE001 = doplněk od aplikace (vykreslí se jinou barvou)
        return "$line \uE000(${parts.joinToString("; ")})\uE001"
    }

    /** Nejdelší shoda hesla na začátku tokenu (např. „First strike“, „Protection from red“). */
    private fun matchKeyword(token: String, czech: Boolean): Pair<String, String>? {
        val dict = if (czech) GLOSSARY else GLOSSARY_EN
        val low = token.lowercase()
        val key = GLOSSARY.keys
            .filter { low == it || low.startsWith("$it ") || low.startsWith("$it—") || low.startsWith("$it-") }
            .maxByOrNull { it.length } ?: return null
        return key to dict.getValue(key)
    }
}
