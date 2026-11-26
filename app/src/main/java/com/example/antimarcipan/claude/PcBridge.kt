package com.example.antimarcipan.claude

import com.example.antimarcipan.i18n.tr

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket

/**
 * Malý webový server v telefonu: na PC v prohlížeči otevřeš adresu, zkopíruješ zadání
 * pro Clauda a jeho odpověď pošleš zpět do telefonu. Běží jen v místní Wi-Fi a chrání ho
 * náhodný kód v adrese.
 */
object PcBridge {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var server: ServerSocket? = null
    private var token = ""

    @Volatile var prompt: String = ""
    /** Adresa pro PC, null = neběží. */
    val url = MutableStateFlow<String?>(null)
    /** Odpověď poslaná z PC. */
    val incoming = MutableStateFlow<String?>(null)

    fun start(text: String) {
        prompt = text
        if (server != null) return
        val ip = localIp() ?: run { url.value = tr("Telefon není připojený k Wi-Fi"); return }
        token = (1..6).map { "abcdefghjkmnpqrstuvwxyz23456789".random() }.joinToString("")
        val ss = runCatching { ServerSocket(8765) }.getOrElse { ServerSocket(0) }
        server = ss
        url.value = "http://$ip:${ss.localPort}/$token"
        scope.launch {
            while (!ss.isClosed) {
                val s = runCatching { ss.accept() }.getOrNull() ?: break
                launch { runCatching { handle(s) }; runCatching { s.close() } }
            }
        }
    }

    fun stop() {
        runCatching { server?.close() }
        server = null
        url.value = null
    }

    private fun handle(s: Socket) {
        s.soTimeout = 15000
        val ins = s.getInputStream()
        val head = java.io.ByteArrayOutputStream()
        var last4 = 0
        while (true) {
            val b = ins.read(); if (b < 0) break
            head.write(b)
            last4 = (last4 shl 8) or b
            if (last4 == 0x0D0A0D0A || head.size() > 64_000) break
        }
        val lines = head.toString("UTF-8").split("\r\n")
        val req = lines.firstOrNull() ?: return
        val len = lines.firstOrNull { it.lowercase().startsWith("content-length:") }
            ?.substringAfter(":")?.trim()?.toIntOrNull()?.coerceIn(0, 4_000_000) ?: 0
        val parts = req.split(" ")
        val method = parts.getOrNull(0) ?: ""
        val path = parts.getOrNull(1) ?: ""
        val out = s.getOutputStream()
        fun reply(code: String, type: String, body: String) {
            val b = body.toByteArray(Charsets.UTF_8)
            out.write("HTTP/1.1 $code\r\nContent-Type: $type; charset=utf-8\r\nContent-Length: ${b.size}\r\nCache-Control: no-store\r\nConnection: close\r\n\r\n".toByteArray())
            out.write(b); out.flush()
        }
        when {
            method == "GET" && path == "/$token" -> reply("200 OK", "text/html", page())
            method == "GET" && path == "/$token/prompt" -> reply("200 OK", "text/plain", prompt)
            method == "POST" && path == "/$token/reply" -> {
                val buf = ByteArray(len)
                var read = 0
                while (read < len) { val n = ins.read(buf, read, len - read); if (n < 0) break; read += n }
                incoming.value = String(buf, 0, read, Charsets.UTF_8)
                reply("200 OK", "text/plain", "ok")
            }
            else -> reply("404 Not Found", "text/plain", tr("Grimoar: neplatná adresa"))
        }
    }

    private fun localIp(): String? = runCatching {
        NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback }
            .sortedBy { if (it.name.startsWith("wlan")) 0 else 1 }
            .flatMap { it.inetAddresses.toList() }
            .firstOrNull { it is Inet4Address && it.isSiteLocalAddress }?.hostAddress
    }.getOrNull()

    private fun page(): String = """<!doctype html><html lang="${if (com.example.antimarcipan.i18n.I18n.english) "en" else "cs"}"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1"><title>Grimoar · Claude</title>
<style>
body{margin:0;background:#140f24;color:#eee;font-family:system-ui,Segoe UI,Roboto,sans-serif}
main{max-width:900px;margin:0 auto;padding:28px 18px}
h1{color:#e9c46a;font-family:Georgia,serif;margin:0 0 4px}
p{color:#bbb;margin:6px 0 14px}
h2{color:#e9c46a;font-size:1.05rem;margin:26px 0 8px}
textarea{width:100%;box-sizing:border-box;background:#1e1640;color:#eee;border:1px solid #6b5a2e;border-radius:10px;padding:12px;font:13px/1.45 ui-monospace,Consolas,monospace}
button{background:#e9c46a;color:#1e1633;border:0;border-radius:22px;padding:11px 22px;font-weight:700;font-size:15px;cursor:pointer;margin:10px 8px 0 0}
button.o{background:transparent;color:#e9c46a;border:1px solid #e9c46a}
#st,#st2{color:#8fd6a0;margin-left:6px}
</style></head><body><main>
<h1>Grimoar · Claude</h1>
<p>${tr("1) Zkopíruj zadání a vlož ho do Clauda (claude.ai). 2) Až Claude navrhne změny, zkopíruj celou jeho odpověď (nebo jen blok GRIMOAR-START…END) a pošli ji do telefonu.")}</p>
<h2>${tr("Zadání pro Clauda")}</h2>
<textarea id="p" rows="12" readonly></textarea>
<button onclick="cp()">${tr("Kopírovat zadání")}</button><button class="o" onclick="window.open('https://claude.ai/new','_blank')">${tr("Otevřít claude.ai")}</button><button class="o" onclick="load()">${tr("Načíst znovu")}</button><span id="st"></span>
<h2>${tr("Odpověď od Clauda")}</h2>
<textarea id="r" rows="10" placeholder="${tr("Sem vlož odpověď od Clauda…")}"></textarea>
<button onclick="send()">${tr("Poslat do telefonu")}</button><span id="st2"></span>
<script>
const base=location.pathname;
async function load(){const t=await (await fetch(base+'/prompt')).text();document.getElementById('p').value=t;}
function cp(){const p=document.getElementById('p');p.select();
 if(navigator.clipboard&&window.isSecureContext){navigator.clipboard.writeText(p.value)}else{document.execCommand('copy')}
 document.getElementById('st').textContent='${tr("Zkopírováno ✓")}';}
async function send(){const v=document.getElementById('r').value;if(!v.trim())return;
 const r=await fetch(base+'/reply',{method:'POST',headers:{'Content-Type':'text/plain;charset=utf-8'},body:v});
 document.getElementById('st2').textContent=r.ok?'${tr("Odesláno — potvrď změny v telefonu ✓")}':'${tr("Nepodařilo se odeslat")}';}
load();
</script></main></body></html>"""
}
