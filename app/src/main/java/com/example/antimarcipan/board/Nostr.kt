package com.example.antimarcipan.board

import android.content.Context
import android.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** Druh událostí Antimarcipánu (vlastní, aby nezaplevelily běžné Nostr klienty). */
const val AM_KIND = 7373

val DEFAULT_RELAYS = listOf(
    "wss://relay.damus.io",
    "wss://nos.lol",
    "wss://relay.primal.net",
    "wss://relay.nostr.band",
)

/** Moje Nostr identita (klíč vznikne v telefonu, nikam se neposílá). */
class NostrIdentity(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("nostr", Context.MODE_PRIVATE)
    val secret: ByteArray
    val pubHex: String

    init {
        val stored = prefs.getString("sk", null)
        secret = if (stored != null) Schnorr.unhex(stored) else {
            var k: ByteArray
            do { k = ByteArray(32).also { SecureRandom().nextBytes(it) } } while (Schnorr.num(k).let { it.signum() == 0 || it >= Schnorr.N })
            prefs.edit().putString("sk", Schnorr.hex(k)).apply()
            k
        }
        pubHex = Schnorr.hex(Schnorr.publicKey(secret))
    }
}

data class NostrEvent(
    val id: String,
    val pubkey: String,
    val createdAt: Long,
    val kind: Int,
    val tags: List<List<String>>,
    val content: String,
    val sig: String,
) {
    fun toJson(): JsonObject = buildJsonObject {
        put("id", id); put("pubkey", pubkey); put("created_at", createdAt); put("kind", kind)
        put("tags", buildJsonArray { tags.forEach { t -> add(buildJsonArray { t.forEach { add(JsonPrimitive(it)) } }) } })
        put("content", content); put("sig", sig)
    }

    companion object {
        fun create(id: NostrIdentity, kind: Int, tags: List<List<String>>, content: String): NostrEvent {
            val created = System.currentTimeMillis() / 1000
            val ser = buildJsonArray {
                add(JsonPrimitive(0)); add(JsonPrimitive(id.pubHex)); add(JsonPrimitive(created)); add(JsonPrimitive(kind))
                add(buildJsonArray { tags.forEach { t -> add(buildJsonArray { t.forEach { add(JsonPrimitive(it)) } }) } })
                add(JsonPrimitive(content))
            }.toString()
            val hash = Schnorr.sha256(ser.toByteArray(Charsets.UTF_8))
            val aux = ByteArray(32).also { SecureRandom().nextBytes(it) }
            val sig = Schnorr.sign(hash, id.secret, aux)
            return NostrEvent(Schnorr.hex(hash), id.pubHex, created, kind, tags, content, Schnorr.hex(sig))
        }

        fun parse(o: JsonObject): NostrEvent? = runCatching {
            NostrEvent(
                id = o["id"]!!.jsonPrimitive.content,
                pubkey = o["pubkey"]!!.jsonPrimitive.content,
                createdAt = o["created_at"]!!.jsonPrimitive.long,
                kind = o["kind"]!!.jsonPrimitive.content.toInt(),
                tags = o["tags"]!!.jsonArray.map { t -> t.jsonArray.map { it.jsonPrimitive.contentOrNull ?: "" } },
                content = o["content"]!!.jsonPrimitive.content,
                sig = o["sig"]!!.jsonPrimitive.content,
            )
        }.getOrNull()
    }
}

/**
 * Skupina = sdílené tajemství. Z něj se odvodí veřejný „štítek“ (podle něj relaye
 * vracejí naše zprávy) a šifrovací klíč — obsah si přečtou jen členové skupiny.
 */
data class BoardGroup(val name: String, val secretHex: String) {
    val tag: String get() = "am-" + Schnorr.hex(Schnorr.sha256("antimarcipan-tag:$secretHex".toByteArray())).take(20)
    private val key: SecretKeySpec get() = SecretKeySpec(Schnorr.sha256("antimarcipan-key:$secretHex".toByteArray()), "AES")

    fun encrypt(plain: String): String {
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv)) }
        return Base64.encodeToString(iv + c.doFinal(plain.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
    }

    fun decrypt(b64: String): String? = runCatching {
        val all = Base64.decode(b64, Base64.NO_WRAP)
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, all.copyOfRange(0, 12)))
        }
        String(c.doFinal(all.copyOfRange(12, all.size)), Charsets.UTF_8)
    }.getOrNull()

    /** Pozvánka ke sdílení. */
    val invite: String get() = "AMGROUP1:" + Base64.encodeToString(name.toByteArray(), Base64.NO_WRAP or Base64.URL_SAFE) + ":" + secretHex

    companion object {
        fun new(name: String): BoardGroup =
            BoardGroup(name, Schnorr.hex(ByteArray(32).also { SecureRandom().nextBytes(it) }))

        fun fromInvite(text: String): BoardGroup? = runCatching {
            val i = text.indexOf("AMGROUP1:")
            if (i < 0) return null
            val parts = text.substring(i + 9).trim().split(":")
            val name = String(Base64.decode(parts[0], Base64.NO_WRAP or Base64.URL_SAFE))
            val secret = parts[1].takeWhile { it.isLetterOrDigit() }.lowercase()
            require(secret.length == 64)
            BoardGroup(name, secret)
        }.getOrNull()
    }
}

/** Připojení k několika relayům najednou (WebSocket). */
class RelayPool(
    private val relays: List<String>,
    private val onEvent: (NostrEvent) -> Unit,
    private val onStatus: (connected: Int, total: Int) -> Unit,
) {
    private val client = OkHttpClient.Builder().pingInterval(30, TimeUnit.SECONDS).readTimeout(0, TimeUnit.MILLISECONDS).build()
    private val sockets = mutableMapOf<String, WebSocket>()
    private val open = mutableSetOf<String>()
    private var subscription: Pair<String, String>? = null // (subId, filterJson)

    fun connect() {
        relays.forEach { url ->
            if (sockets.containsKey(url)) return@forEach
            val ws = client.newWebSocket(Request.Builder().url(url).build(), object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    synchronized(open) { open += url }
                    report()
                    subscription?.let { (id, f) -> webSocket.send("[\"REQ\",\"$id\",$f]") }
                }
                override fun onMessage(webSocket: WebSocket, text: String) {
                    runCatching {
                        val arr = Json.parseToJsonElement(text) as JsonArray
                        if (arr[0].jsonPrimitive.content == "EVENT") NostrEvent.parse(arr[2] as JsonObject)?.let(onEvent)
                    }
                }
                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { drop(url) }
                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { drop(url) }
            })
            sockets[url] = ws
        }
    }

    private fun drop(url: String) {
        synchronized(open) { open -= url }
        sockets.remove(url)
        report()
    }

    private fun report() = onStatus(synchronized(open) { open.size }, relays.size)

    /** Odběr událostí se štítkem skupiny (za posledních 60 dní). */
    fun subscribe(tag: String) {
        val since = System.currentTimeMillis() / 1000 - 60L * 24 * 3600
        val filter = buildJsonObject {
            put("kinds", buildJsonArray { add(JsonPrimitive(AM_KIND)) })
            put("#t", buildJsonArray { add(JsonPrimitive(tag)) })
            put("since", since)
            put("limit", 500)
        }.toString()
        subscription?.let { (old, _) -> sockets.values.forEach { it.send("[\"CLOSE\",\"$old\"]") } }
        val id = "am" + System.nanoTime().toString(36)
        subscription = id to filter
        sockets.values.forEach { it.send("[\"REQ\",\"$id\",$filter]") }
    }

    /** Odešle událost na všechny připojené relaye; vrací počet, kam odešla. */
    fun publish(ev: NostrEvent): Int {
        val msg = "[\"EVENT\",${ev.toJson()}]"
        return sockets.values.count { it.send(msg) }
    }

    fun close() {
        sockets.values.forEach { runCatching { it.close(1000, null) } }
        sockets.clear(); synchronized(open) { open.clear() }
    }
}
