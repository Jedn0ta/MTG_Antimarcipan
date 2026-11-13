package com.example.antimarcipan.data

/**
 * Stručná česká vysvětlení klíčových slov MTG. Používá se tam, kde karta
 * uvádí jen samotné heslo bez připomínkového textu (např. „Dredge 3").
 */
object KeywordGlossary {

    /** Všechna hesla (pro slovníček v Rádci). */
    val entries: Map<String, String> get() = GLOSSARY

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
    fun explain(keyword: String): String? = GLOSSARY[keyword.trim().lowercase()]

    /**
     * Doplní vysvětlení do závorky k řádkům, které obsahují jen klíčová slova
     * bez připomínkového textu („Flying, vigilance“, „Dredge 3“, „Ward {2}“).
     */
    fun annotate(oracle: String?): String? {
        if (oracle.isNullOrBlank()) return oracle
        return oracle.lines().joinToString("\n") { line -> annotateLine(line) }
    }

    private fun annotateLine(line: String): String {
        val t = line.trim()
        if (t.isEmpty() || t.contains('(') || t.contains('.') || t.contains(':')) return line
        val tokens = t.split(Regex("[,;]\\s*")).map { it.trim() }.filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return line
        val found = tokens.map { matchKeyword(it) ?: return line }
        val parts = found.map { (name, expl) ->
            if (found.size == 1) expl else "${name.replaceFirstChar { it.uppercase() }}: $expl"
        }
        // \uE000…\uE001 = doplněk od aplikace (vykreslí se jinou barvou)
        return "$line \uE000(${parts.joinToString("; ")})\uE001"
    }

    /** Nejdelší shoda hesla na začátku tokenu (např. „First strike“, „Protection from red“). */
    private fun matchKeyword(token: String): Pair<String, String>? {
        val low = token.lowercase()
        val key = GLOSSARY.keys
            .filter { low == it || low.startsWith("$it ") || low.startsWith("$it—") || low.startsWith("$it-") }
            .maxByOrNull { it.length } ?: return null
        return key to GLOSSARY.getValue(key)
    }
}
