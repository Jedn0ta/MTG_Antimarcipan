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

/**
 * Hledání přátel na stejné Wi-Fi (Android NSD / mDNS) a posílání balíků
 * přímo telefon → telefon přes lokální síť. Bez internetu a bez serveru.
 */
object LanShare {
    private const val TYPE = "_antimarcipan._tcp."
    private const val PREFIX = "Antimarcipan-"

    val peers = MutableStateFlow<List<LanPeer>>(emptyList())
    val running = MutableStateFlow(false)

    private var server: ServerSocket? = null
    private var nsd: NsdManager? = null
    private var regListener: NsdManager.RegistrationListener? = null
    private var discListener: NsdManager.DiscoveryListener? = null
    private var myServiceName: String? = null
    private val resolveQueue = ConcurrentLinkedQueue<NsdServiceInfo>()
    @Volatile private var resolving = false

    fun start(context: Context, myName: String) {
        if (running.value) return
        running.value = true
        peers.value = emptyList()
        val mgr = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        nsd = mgr

        // 1) Server: přijímá balíky od ostatních
        val ss = ServerSocket(0)
        server = ss
        thread(name = "lan-share-server", isDaemon = true) {
            while (!ss.isClosed) {
                val s = runCatching { ss.accept() }.getOrNull() ?: break
                thread(isDaemon = true) {
                    runCatching {
                        s.soTimeout = 15000
                        val text = s.getInputStream().readBytes().toString(Charsets.UTF_8)
                        if ("AMDECK1:" in text) FriendsInbox.pending.value = text
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

    suspend fun send(peer: LanPeer, text: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching<Unit> {
            Socket().use { s ->
                s.connect(InetSocketAddress(peer.host, peer.port), 5000)
                s.getOutputStream().apply { write(text.toByteArray(Charsets.UTF_8)); flush() }
            }
            Unit
        }
    }
}
