package com.example.antimarcipan.board

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Obsah příspěvku (šifrovaný v Nostr události). */
@Serializable
data class Payload(
    /** msg | meet | rsvp */
    val y: String,
    val n: String = "",
    val x: String = "",
    val ti: String = "",
    val w: Long = 0,
    val pl: String = "",
    val r: String = "",
    val g: String = "",
)

data class Post(val id: String, val author: String, val createdAt: Long, val p: Payload, val mine: Boolean)

/** Přijatá pozvánka do skupiny (ze sdílení) — čeká na potvrzení. */
object BoardInbox { val pending = MutableStateFlow<String?>(null) }

class BoardViewModel(private val ctx: Context) : ViewModel() {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }
    private val prefs = ctx.getSharedPreferences("board", Context.MODE_PRIVATE)
    private val identity by lazy { NostrIdentity(ctx) }
    val myPub: String get() = identity.pubHex

    val groups = MutableStateFlow(loadGroups())
    val current = MutableStateFlow(groups.value.firstOrNull { it.secretHex == prefs.getString("current", null) } ?: groups.value.firstOrNull())
    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts
    val relayStatus = MutableStateFlow(0 to DEFAULT_RELAYS.size)

    private val seen = HashSet<String>()
    private var pool: RelayPool? = null

    private fun myName(): String =
        ctx.getSharedPreferences("friends", Context.MODE_PRIVATE).getString("myName", "")?.ifBlank { null } ?: "Hráč"

    private fun loadGroups(): List<BoardGroup> =
        (prefs.getString("groups", "") ?: "").lines().mapNotNull { l ->
            val i = l.lastIndexOf('|'); if (i <= 0) null else BoardGroup(l.substring(0, i), l.substring(i + 1))
        }

    private fun saveGroups() = prefs.edit().putString("groups", groups.value.joinToString("\n") { "${it.name.replace("\n", " ")}|${it.secretHex}" }).apply()

    // ---- cache (offline zobrazení) ----
    private fun cacheKey(g: BoardGroup) = "cache_" + g.tag
    private fun loadCache(g: BoardGroup) {
        seen.clear()
        val list = (prefs.getString(cacheKey(g), "") ?: "").lines().mapNotNull { l ->
            runCatching {
                val parts = l.split("\t", limit = 4)
                Post(parts[0], parts[1], parts[2].toLong(), json.decodeFromString(Payload.serializer(), parts[3]), parts[1] == myPub)
            }.getOrNull()
        }
        list.forEach { seen += it.id }
        _posts.value = list.sortedBy { it.createdAt }
    }
    private fun saveCache(g: BoardGroup) {
        val keep = _posts.value.takeLast(400)
        prefs.edit().putString(cacheKey(g), keep.joinToString("\n") {
            "${it.id}\t${it.author}\t${it.createdAt}\t${json.encodeToString(Payload.serializer(), it.p)}"
        }).apply()
    }

    fun start() {
        if (pool != null) return
        pool = RelayPool(
            DEFAULT_RELAYS,
            onEvent = { ev -> onEvent(ev) },
            onStatus = { c, t -> relayStatus.value = c to t },
        ).also { it.connect() }
        current.value?.let { select(it) }
    }

    fun stop() { pool?.close(); pool = null; relayStatus.value = 0 to DEFAULT_RELAYS.size }

    fun select(g: BoardGroup) {
        current.value = g
        prefs.edit().putString("current", g.secretHex).apply()
        loadCache(g)
        pool?.subscribe(g.tag)
    }

    private fun onEvent(ev: NostrEvent) {
        val g = current.value ?: return
        if (ev.kind != AM_KIND || ev.tags.none { it.size >= 2 && it[0] == "t" && it[1] == g.tag }) return
        synchronized(seen) { if (!seen.add(ev.id)) return }
        val plain = g.decrypt(ev.content) ?: return // ne-členové / poškozené
        val p = runCatching { json.decodeFromString(Payload.serializer(), plain) }.getOrNull() ?: return
        val post = Post(ev.id, ev.pubkey, ev.createdAt, p, ev.pubkey == myPub)
        _posts.value = (_posts.value + post).sortedBy { it.createdAt }
        saveCache(g)
    }

    private fun publish(p: Payload) {
        val g = current.value ?: return
        viewModelScope.launch(Dispatchers.Default) {
            val ev = NostrEvent.create(identity, AM_KIND, listOf(listOf("t", g.tag)), g.encrypt(json.encodeToString(Payload.serializer(), p)))
            onEvent(ev) // hned zobrazit u mě
            pool?.publish(ev)
        }
    }

    fun sendMessage(text: String) { if (text.isNotBlank()) publish(Payload(y = "msg", n = myName(), x = text.trim())) }
    fun createMeetup(title: String, whenMs: Long, place: String, note: String) =
        publish(Payload(y = "meet", n = myName(), ti = title.trim(), w = whenMs, pl = place.trim(), x = note.trim()))
    fun rsvp(meetId: String, going: String) = publish(Payload(y = "rsvp", n = myName(), r = meetId, g = going))

    fun createGroup(name: String) {
        val g = BoardGroup.new(name.ifBlank { "Herna" })
        groups.value = groups.value + g; saveGroups(); select(g)
    }

    fun joinGroup(invite: String): Boolean {
        val g = BoardGroup.fromInvite(invite) ?: return false
        if (groups.value.none { it.secretHex == g.secretHex }) { groups.value = groups.value + g; saveGroups() }
        select(groups.value.first { it.secretHex == g.secretHex })
        return true
    }

    fun leaveGroup(g: BoardGroup) {
        groups.value = groups.value.filterNot { it.secretHex == g.secretHex }; saveGroups()
        prefs.edit().remove(cacheKey(g)).apply()
        groups.value.firstOrNull()?.let { select(it) } ?: run { current.value = null; _posts.value = emptyList() }
    }

    override fun onCleared() { stop() }
}
