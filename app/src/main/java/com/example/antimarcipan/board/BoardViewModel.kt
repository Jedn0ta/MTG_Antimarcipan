package com.example.antimarcipan.board

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Obsah příspěvku (šifrovaný v Nostr události). */
@Serializable
data class Payload(
    /** msg | meet | rsvp | poll | vote | meta | del */
    val y: String,
    val n: String = "",
    val x: String = "",
    val ti: String = "",
    val w: Long = 0,
    val pl: String = "",
    val r: String = "",
    val g: String = "",
    /** Možnosti ankety. */
    val o: List<String> = emptyList(),
    /** Anketa s více odpověďmi. */
    val mu: Boolean = false,
)

data class Post(val id: String, val author: String, val createdAt: Long, val p: Payload, val mine: Boolean)

/** Přijatá pozvánka do skupiny (ze sdílení) — čeká na potvrzení na Nástěnce. */
object BoardInbox {
    val pending = MutableStateFlow<String?>(null)
    /** Pozvánka už potvrzená (např. z Wi-Fi dialogu) — připojit bez dalšího ptaní. */
    val accepted = MutableStateFlow<String?>(null)
}

/**
 * Nástěnka — běží na úrovni aplikace (dokud je appka v popředí), aby se počítaly
 * nepřečtené zprávy i mimo obrazovku Nástěnky.
 */
object Board {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var ctx: Context
    private val prefs get() = ctx.getSharedPreferences("board", Context.MODE_PRIVATE)
    private var identity: NostrIdentity? = null
    val myPub: String get() = identity?.pubHex ?: ""

    val groups = MutableStateFlow<List<BoardGroup>>(emptyList())
    val current = MutableStateFlow<BoardGroup?>(null)
    /** Příspěvky podle štítku skupiny. */
    private val byGroup = MutableStateFlow<Map<String, List<Post>>>(emptyMap())
    val posts = MutableStateFlow<List<Post>>(emptyList())
    val relayStatus = MutableStateFlow(0 to DEFAULT_RELAYS.size)
    /** Nepřečtené podle štítku skupiny. */
    val unread = MutableStateFlow<Map<String, Int>>(emptyMap())
    /** Nástěnka je právě na obrazovce (pak se vše hned značí jako přečtené). */
    @Volatile var visible = false

    private val seen = HashSet<String>()
    private var pool: RelayPool? = null
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        ctx = context.applicationContext
        identity = NostrIdentity(ctx)
        groups.value = loadGroups()
        groups.value.forEach { loadCache(it) }
        current.value = groups.value.firstOrNull { it.secretHex == prefs.getString("current", null) } ?: groups.value.firstOrNull()
        refreshCurrent(); recountUnread()
    }

    private fun myName(): String =
        ctx.getSharedPreferences("friends", Context.MODE_PRIVATE).getString("myName", "")?.ifBlank { null } ?: "Hráč"

    private fun loadGroups(): List<BoardGroup> =
        (prefs.getString("groups", "") ?: "").lines().mapNotNull { l ->
            val i = l.lastIndexOf('|'); if (i <= 0) null else BoardGroup(l.substring(0, i), l.substring(i + 1))
        }

    private fun saveGroups() = prefs.edit().putString("groups", groups.value.joinToString("\n") { "${it.name.replace("\n", " ")}|${it.secretHex}" }).apply()

    private fun cacheKey(g: BoardGroup) = "cache_" + g.tag
    private fun loadCache(g: BoardGroup) {
        val list = (prefs.getString(cacheKey(g), "") ?: "").lines().mapNotNull { l ->
            runCatching {
                val parts = l.split("\t", limit = 4)
                Post(parts[0], parts[1], parts[2].toLong(), json.decodeFromString(Payload.serializer(), parts[3]), parts[1] == myPub)
            }.getOrNull()
        }
        synchronized(seen) { list.forEach { seen += it.id } }
        byGroup.value = byGroup.value + (g.tag to list.sortedBy { it.createdAt })
    }
    private fun saveCache(g: BoardGroup) {
        val keep = byGroup.value[g.tag].orEmpty().takeLast(400)
        prefs.edit().putString(cacheKey(g), keep.joinToString("\n") {
            "${it.id}\t${it.author}\t${it.createdAt}\t${json.encodeToString(Payload.serializer(), it.p)}"
        }).apply()
    }

    private fun refreshCurrent() { posts.value = current.value?.let { byGroup.value[it.tag] }.orEmpty() }

    // ---- nepřečtené ----
    private fun lastRead(g: BoardGroup) = prefs.getLong("read_" + g.tag, 0L)
    fun markRead(g: BoardGroup) {
        val last = byGroup.value[g.tag]?.maxOfOrNull { it.createdAt } ?: (System.currentTimeMillis() / 1000)
        prefs.edit().putLong("read_" + g.tag, last).apply()
        recountUnread()
    }
    private fun recountUnread() {
        unread.value = groups.value.associate { g ->
            val lr = lastRead(g)
            g.tag to byGroup.value[g.tag].orEmpty().count { !it.mine && it.createdAt > lr && it.p.y != "rsvp" }
        }
    }

    // ---- připojení ----
    fun start() {
        if (!initialized || pool != null) return
        pool = RelayPool(DEFAULT_RELAYS, onEvent = { onEvent(it) }, onStatus = { c, t -> relayStatus.value = c to t }).also {
            it.connect()
            resubscribe()
        }
    }

    fun stop() { pool?.close(); pool = null; relayStatus.value = 0 to DEFAULT_RELAYS.size }

    private fun resubscribe() { pool?.subscribe(groups.value.map { it.tag }) }

    fun select(g: BoardGroup) {
        current.value = g
        prefs.edit().putString("current", g.secretHex).apply()
        refreshCurrent()
        if (visible) markRead(g)
    }

    private fun onEvent(ev: NostrEvent) {
        if (ev.kind != AM_KIND) return
        val tag = ev.tags.firstOrNull { it.size >= 2 && it[0] == "t" }?.get(1) ?: return
        val g = groups.value.firstOrNull { it.tag == tag } ?: return
        synchronized(seen) { if (!seen.add(ev.id)) return }
        val plain = g.decrypt(ev.content) ?: return
        val p = runCatching { json.decodeFromString(Payload.serializer(), plain) }.getOrNull() ?: return
        val post = Post(ev.id, ev.pubkey, ev.createdAt, p, ev.pubkey == myPub)
        if (p.y == "meta" && p.ti.isNotBlank()) applyRename(g, p.ti, ev.createdAt)
        byGroup.value = byGroup.value + (g.tag to (byGroup.value[g.tag].orEmpty() + post).sortedBy { it.createdAt })
        saveCache(g)
        if (current.value?.tag == g.tag) {
            refreshCurrent()
            if (visible) markRead(g)
        }
        recountUnread()
    }

    private fun publish(p: Payload) {
        val g = current.value ?: return
        val id = identity ?: return
        scope.launch {
            val ev = NostrEvent.create(id, AM_KIND, listOf(listOf("t", g.tag)), g.encrypt(json.encodeToString(Payload.serializer(), p)))
            onEvent(ev)
            pool?.publish(ev)
        }
    }

    private fun applyRename(g: BoardGroup, name: String, at: Long) {
        val key = "meta_" + g.tag
        if (at < prefs.getLong(key, 0L)) return
        prefs.edit().putLong(key, at).apply()
        groups.value = groups.value.map { if (it.secretHex == g.secretHex) it.copy(name = name) else it }
        saveGroups()
        if (current.value?.secretHex == g.secretHex) current.value = groups.value.first { it.secretHex == g.secretHex }
    }

    /** Přejmenuje skupinu pro všechny členy. */
    fun renameGroup(name: String) { if (name.isNotBlank()) publish(Payload(y = "meta", n = myName(), ti = name.trim())) }

    fun createPoll(question: String, options: List<String>, multi: Boolean) {
        val opts = options.map { it.trim() }.filter { it.isNotEmpty() }
        if (question.isNotBlank() && opts.size >= 2) publish(Payload(y = "poll", n = myName(), ti = question.trim(), o = opts, mu = multi))
    }

    /** Hlas v anketě: indexy zvolených možností (prázdné = zrušit hlas). */
    fun vote(pollId: String, choices: Set<Int>) = publish(Payload(y = "vote", n = myName(), r = pollId, g = choices.sorted().joinToString(",")))

    /** Smaže můj příspěvek (u všech se skryje). */
    fun deletePost(id: String) = publish(Payload(y = "del", n = myName(), r = id))

    fun sendMessage(text: String) { if (text.isNotBlank()) publish(Payload(y = "msg", n = myName(), x = text.trim())) }
    fun createMeetup(title: String, whenMs: Long, place: String, note: String) =
        publish(Payload(y = "meet", n = myName(), ti = title.trim(), w = whenMs, pl = place.trim(), x = note.trim()))
    fun rsvp(meetId: String, going: String) = publish(Payload(y = "rsvp", n = myName(), r = meetId, g = going))

    fun createGroup(name: String) {
        val g = BoardGroup.new(name.ifBlank { "Herna" })
        groups.value = groups.value + g; saveGroups(); resubscribe(); select(g)
    }

    fun joinGroup(invite: String): Boolean {
        val g = BoardGroup.fromInvite(invite) ?: return false
        if (groups.value.none { it.secretHex == g.secretHex }) { groups.value = groups.value + g; saveGroups(); resubscribe() }
        select(groups.value.first { it.secretHex == g.secretHex })
        return true
    }

    fun leaveGroup(g: BoardGroup) {
        groups.value = groups.value.filterNot { it.secretHex == g.secretHex }; saveGroups()
        prefs.edit().remove(cacheKey(g)).remove("read_" + g.tag).apply()
        byGroup.value = byGroup.value - g.tag
        resubscribe(); recountUnread()
        groups.value.firstOrNull()?.let { select(it) } ?: run { current.value = null; posts.value = emptyList() }
    }
}
