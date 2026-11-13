package com.example.antimarcipan.friends

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.thread

data class LanPeer(val name: String, val host: String, val port: Int)

/** Příchozí balík přes Wi-Fi — čeká, až ho příjemce potvrdí nebo odmítne. */
data class LanIncoming(val from: String, val host: String, val port: Int?, val text: String, val at: Long = System.currentTimeMillis())

/**
 * Hledání přátel na stejné Wi-Fi (Android NSD / mDNS) a posílání balíků
 * přímo telefon → telefon přes lokální síť. Bez internetu a bez serveru.
 */
object LanShare {
    private const val TYPE = "_antimarcipan._tcp."
    private const val PREFIX = "Antimarcipan-"

    val peers = MutableStateFlow<List<LanPeer>>(emptyList())
    val running = MutableStateFlow(false)
    val incoming = MutableStateFlow<List<LanIncoming>>(emptyList())
    /** Potvrzení od příjemce (zobrazí se odesílateli). */
    val acks = MutableStateFlow<String?>(null)
    private var myName: String = "Hráč"
    private var myPort: Int = 0

    private var server: ServerSocket? = null
    private var nsd: NsdManager? = null
    private var regListener: NsdManager.RegistrationListener? = null
    private var discListener: NsdManager.DiscoveryListener? = null
    private var myServiceName: String? = null
    private val resolveQueue = ConcurrentLinkedQueue<NsdServiceInfo>()
    @Volatile private var resolving = false

    private fun prefs(c: Context) = c.getSharedPreferences("friends", Context.MODE_PRIVATE)

    /** Viditelnost na Wi-Fi (pamatuje se; běží, dokud je appka v popředí). */
    fun enable(context: Context, on: Boolean) {
        prefs(context).edit().putBoolean("lan", on).apply()
        if (on) start(context, prefs(context).getString("myName", "") ?: "") else stop()
    }

    fun isEnabled(context: Context) = prefs(context).getBoolean("lan", false)

    /** Volá aktivita při návratu do popředí. */
    fun resume(context: Context) { if (isEnabled(context)) start(context, prefs(context).getString("myName", "") ?: "") }

    suspend fun sendInvite(peer: LanPeer, invite: String, groupName: String): Result<Unit> = raw(peer.host, peer.port, header() + invite)

    fun start(context: Context, myName: String) {
        if (running.value) return
        running.value = true
        this.myName = myName.ifBlank { "Hráč" }
        peers.value = emptyList()
        val mgr = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        nsd = mgr

        // 1) Server: přijímá balíky od ostatních
        val ss = ServerSocket(0)
        server = ss
        myPort = ss.localPort
        thread(name = "lan-share-server", isDaemon = true) {
            while (!ss.isClosed) {
                val s = runCatching { ss.accept() }.getOrNull() ?: break
                thread(isDaemon = true) {
                    runCatching {
                        s.soTimeout = 15000
                        val raw = s.getInputStream().readBytes().toString(Charsets.UTF_8)
                        val host = s.inetAddress?.hostAddress ?: ""
                        var from = "?"; var port: Int? = null; var text = raw
                        if (raw.startsWith("AMFROM1:")) {
                            val head = raw.substringBefore("\n").removePrefix("AMFROM1:").split(":")
                            from = runCatching { String(android.util.Base64.decode(head[0], android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE)) }.getOrDefault("?")
                            port = head.getOrNull(1)?.toIntOrNull()
                            text = raw.substringAfter("\n")
                        }
                        when {
                            text.startsWith("AMACK1:") -> acks.value = text.removePrefix("AMACK1:")
                            "AMDECK1:" in text || "AMGROUP1:" in text -> incoming.value = incoming.value + LanIncoming(from, host, port, text)
                        }
                    }
                    runCatching { s.close() }
                }
            }
        }

        // 2) Oznámení: „jsem tady“
        val info = NsdServiceInfo().apply {
            serviceName = PREFIX + myName.ifBlank { "Hráč" }.take(40)
            serviceType = TYPE
            port = ss.localPort
        }
        val rl = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(i: NsdServiceInfo) { myServiceName = i.serviceName }
            override fun onRegistrationFailed(i: NsdServiceInfo, e: Int) {}
            override fun onServiceUnregistered(i: NsdServiceInfo) {}
            override fun onUnregistrationFailed(i: NsdServiceInfo, e: Int) {}
        }
        regListener = rl
        runCatching { mgr.registerService(info, NsdManager.PROTOCOL_DNS_SD, rl) }

        // 3) Hledání ostatních
        val dl = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(t: String) {}
            override fun onDiscoveryStopped(t: String) {}
            override fun onStartDiscoveryFailed(t: String, e: Int) {}
            override fun onStopDiscoveryFailed(t: String, e: Int) {}
            override fun onServiceFound(s: NsdServiceInfo) {
                if (!s.serviceName.startsWith(PREFIX) || s.serviceName == myServiceName) return
                resolveQueue.add(s); resolveNext()
            }
            override fun onServiceLost(s: NsdServiceInfo) {
                val n = s.serviceName.removePrefix(PREFIX)
                peers.value = peers.value.filterNot { it.name == n }
            }
        }
        discListener = dl
        runCatching { mgr.discoverServices(TYPE, NsdManager.PROTOCOL_DNS_SD, dl) }
    }

    @Suppress("DEPRECATION")
    private fun resolveNext() {
        if (resolving) return
        val s = resolveQueue.poll() ?: return
        val mgr = nsd ?: return
        resolving = true
        runCatching {
            mgr.resolveService(s, object : NsdManager.ResolveListener {
                override fun onResolveFailed(i: NsdServiceInfo, e: Int) { resolving = false; resolveNext() }
                override fun onServiceResolved(i: NsdServiceInfo) {
                    val host = i.host?.hostAddress
                    if (host != null && i.serviceName != myServiceName) {
                        val p = LanPeer(i.serviceName.removePrefix(PREFIX), host, i.port)
                        peers.value = (peers.value.filterNot { it.name == p.name } + p).sortedBy { it.name }
                    }
                    resolving = false; resolveNext()
                }
            })
        }.onFailure { resolving = false }
    }

    fun stop() {
        runCatching { discListener?.let { nsd?.stopServiceDiscovery(it) } }
        runCatching { regListener?.let { nsd?.unregisterService(it) } }
        runCatching { server?.close() }
        discListener = null; regListener = null; server = null; myServiceName = null
        resolveQueue.clear(); resolving = false
        peers.value = emptyList()
        running.value = false
    }

    private fun header(): String =
        "AMFROM1:" + android.util.Base64.encodeToString(myName.toByteArray(), android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE) + ":" + myPort + "\n"

    private suspend fun raw(host: String, port: Int, text: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching<Unit> {
            Socket().use { s ->
                s.connect(InetSocketAddress(host, port), 5000)
                s.getOutputStream().apply { write(text.toByteArray(Charsets.UTF_8)); flush() }
            }
            Unit
        }
    }

    suspend fun send(peer: LanPeer, text: String): Result<Unit> = raw(peer.host, peer.port, header() + text)

    /** Odpověď odesílateli: přijato / odmítnuto. */
    suspend fun respond(inc: LanIncoming, accepted: Boolean, deckName: String) {
        incoming.value = incoming.value - inc
        val port = inc.port ?: return
        raw(inc.host, port, header() + "AMACK1:" + (if (accepted) "✓ $myName přijal/a balík „$deckName“" else "✗ $myName odmítl/a balík „$deckName“"))
    }
}
