package com.example.antimarcipan.polls

import com.example.antimarcipan.i18n.tr

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Hlasování o další hře — bez serveru. Pozvánka i hlas jsou krátké kódy ve zprávě
 * (AMPOLL1 / AMVOTE1), které se posílají přes WhatsApp, SMS apod.
 */
@Serializable
data class Poll(
    val id: String,
    val title: String,
    val place: String = "",
    /** Termíny jako epoch millis. */
    val options: List<Long>,
    val owner: String,
    val mine: Boolean,
    val people: List<String> = emptyList(),
    /** Hlasy: jméno → indexy termínů, které mu vyhovují. */
    val votes: Map<String, List<Int>> = emptyMap(),
    /** Můj hlas (u cizí ankety), null = ještě nehlasoval. */
    val myVote: List<Int>? = null,
    val chosen: Int? = null,
    val updated: Long = System.currentTimeMillis(),
    val unread: Boolean = false,
)

@Serializable
private data class Invite(
    val id: String, val t: String, val p: String = "", val o: List<Long>,
    val w: String, val pp: List<String> = emptyList(), val c: Int? = null,
    val v: Map<String, List<Int>> = emptyMap(),
)

@Serializable
private data class Vote(val id: String, val n: String, val v: List<Int>)

object PollStore {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }
    private lateinit var app: Context
    private val _polls = MutableStateFlow<List<Poll>>(emptyList())
    val polls: StateFlow<List<Poll>> = _polls.asStateFlow()
    /** Zpráva po zpracování přijatého kódu (zobrazí obrazovka). */
    val notice = MutableStateFlow<String?>(null)
    /** Požadavek otevřít obrazovku hlasování (po přijetí kódu). */
    val openRequest = MutableStateFlow<String?>(null)

    fun init(ctx: Context) {
        app = ctx.applicationContext
        val raw = prefs().getString("polls", null)
        _polls.value = raw?.let { runCatching { json.decodeFromString<List<Poll>>(it) }.getOrNull() } ?: emptyList()
    }

    private fun prefs() = app.getSharedPreferences("polls", Context.MODE_PRIVATE)
    private fun persist(list: List<Poll>) {
        _polls.value = list.sortedByDescending { it.updated }
        prefs().edit().putString("polls", json.encodeToString(kotlinx.serialization.builtins.ListSerializer(Poll.serializer()), _polls.value)).apply()
    }

    fun myName(): String = app.getSharedPreferences("friends", Context.MODE_PRIVATE).getString("myName", "")?.trim().orEmpty()
    fun setMyName(n: String) = app.getSharedPreferences("friends", Context.MODE_PRIVATE).edit().putString("myName", n.trim()).apply()

    fun upsert(p: Poll) = persist(_polls.value.filter { it.id != p.id } + p.copy(updated = System.currentTimeMillis()))
    fun delete(id: String) = persist(_polls.value.filter { it.id != id })
    fun markRead(id: String) {
        val p = _polls.value.firstOrNull { it.id == id } ?: return
        if (p.unread) persist(_polls.value.map { if (it.id == id) it.copy(unread = false) else it })
    }
    val unreadCount: Int get() = _polls.value.count { it.unread }

    fun create(title: String, place: String, options: List<Long>, people: List<String>, myVote: List<Int>): Poll {
        val me = myName().ifBlank { tr("Já") }
        val p = Poll(
            id = (1..10).map { "abcdefghjkmnpqrstuvwxyz23456789".random() }.joinToString(""),
            title = title.trim(), place = place.trim(), options = options.sorted(), owner = me, mine = true,
            people = people, votes = mapOf(me to myVote),
        )
        upsert(p); return p
    }

    // ---------- kódy ----------
    private fun enc(s: String) = Base64.encodeToString(s.toByteArray(), Base64.NO_WRAP or Base64.URL_SAFE)
    private fun dec(s: String) = String(Base64.decode(s, Base64.URL_SAFE or Base64.NO_WRAP))

    fun label(ms: Long): String =
        java.text.SimpleDateFormat("EEE d. M. · HH:mm", java.util.Locale("cs")).format(java.util.Date(ms))

    /** Text pozvánky (nebo finálního termínu) pro poslání kamarádům. */
    fun inviteText(p: Poll): String {
        val code = enc(json.encodeToString(Invite.serializer(), Invite(p.id, p.title, p.place, p.options, p.owner, p.people, p.chosen, p.votes)))
        return buildString {
            if (p.chosen != null) {
                appendLine(tr("🎲 {0} — termín je domluvený!", (p.title)))
                appendLine("📅 ${label(p.options[p.chosen])}")
            } else {
                appendLine(tr("🎲 {0} — kdy můžeš? Hlasuj v aplikaci Grimoar.", (p.title)))
                p.options.forEachIndexed { i, o -> appendLine("${i + 1}) ${label(o)}") }
            }
            if (p.place.isNotBlank()) appendLine("📍 ${p.place}")
            appendLine()
            appendLine(tr("Zkopíruj tuto zprávu a otevři Grimoar (nebo ji do Grimoaru sdílej)."))
            append("AMPOLL1:$code")
        }
    }

    /** Text s mým hlasem pro organizátora. */
    fun voteText(p: Poll, me: String, v: List<Int>): String {
        val code = enc(json.encodeToString(Vote.serializer(), Vote(p.id, me, v)))
        val can = if (v.isEmpty()) tr("nemůžu ani jeden termín") else v.joinToString(", ") { label(p.options[it]) }
        return tr("🎲 {0}: {1} — {2}\nZkopíruj tuto zprávu a otevři Grimoar.\nAMVOTE1:{3}", (p.title), me, can, code)
    }

    /** Zpracuje přijatý text s kódem. Vrací true, pokud v něm kód byl. */
    fun receive(text: String): Boolean {
        Regex("""AMPOLL1:([A-Za-z0-9_\-=]+)""").find(text)?.let { m ->
            val inv = runCatching { json.decodeFromString(Invite.serializer(), dec(m.groupValues[1])) }.getOrNull() ?: return false
            val old = _polls.value.firstOrNull { it.id == inv.id }
            if (old?.mine == true) return true
            upsert(
                Poll(
                    id = inv.id, title = inv.t, place = inv.p, options = inv.o, owner = inv.w, mine = false,
                    people = inv.pp, votes = inv.v, myVote = old?.myVote, chosen = inv.c, unread = true,
                )
            )
            notice.value = if (inv.c != null) tr("{0}: termín potvrzen — {1}", (inv.t), (label(inv.o[inv.c])))
            else if (old == null) tr("Nová anketa od {0}: {1}", (inv.w), (inv.t)) else tr("Anketa {0} aktualizována", (inv.t))
            openRequest.value = inv.id
            return true
        }
        Regex("""AMVOTE1:([A-Za-z0-9_\-=]+)""").find(text)?.let { m ->
            val v = runCatching { json.decodeFromString(Vote.serializer(), dec(m.groupValues[1])) }.getOrNull() ?: return false
            val p = _polls.value.firstOrNull { it.id == v.id }
            if (p == null || !p.mine) { notice.value = tr("Hlas patří k anketě, kterou tady nemáš."); return true }
            upsert(
                p.copy(
                    votes = p.votes + (v.n to v.v.filter { it in p.options.indices }),
                    people = if (v.n in p.people) p.people else p.people + v.n,
                    unread = true,
                )
            )
            notice.value = tr("{0} hlasoval v anketě {1}", (v.n), (p.title))
            openRequest.value = p.id
            return true
        }
        return false
    }
}
