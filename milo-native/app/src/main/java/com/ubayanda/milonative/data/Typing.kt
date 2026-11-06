package com.ubayanda.milonative.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * "is typing…" between two people in a chat. It rides on Supabase Realtime "broadcast" (a live channel that stores
 * nothing), so no tables change. Both people need this version of the app; the web app doesn't send or show it.
 */
class Typing(private val cloud: CloudApi, private val scope: CoroutineScope) {
    private val client = OkHttpClient.Builder().readTimeout(0, TimeUnit.MILLISECONDS).build()
    private var ws: WebSocket? = null
    private var topic = ""
    private var me = ""
    private var ref = 1
    private var hb: Job? = null
    private var expire: Job? = null
    private var retry: Job? = null
    private var lastSent = 0L
    private var wanted = false
    @Volatile private var joined = false

    private val _theirs = MutableStateFlow(false)
    /** True while the other person is typing. */
    val theirs: StateFlow<Boolean> = _theirs.asStateFlow()

    fun join(chatId: String, myId: String) {
        leave()
        if (myId.isEmpty()) return
        wanted = true; me = myId
        topic = "realtime:typing-" + chatId.replace(':', '_')
        open()
    }

    private fun open() {
        if (!wanted) return
        val url = CloudApi.URL.replace("https://", "wss://") + "/realtime/v1/websocket?apikey=" + CloudApi.KEY + "&vsn=1.0.0"
        ws = client.newWebSocket(Request.Builder().url(url).build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val cfg = JSONObject().put("broadcast", JSONObject().put("self", false).put("ack", false))
                    .put("presence", JSONObject().put("key", "")).put("private", false)
                val payload = JSONObject().put("config", cfg)
                cloud.session?.accessToken?.let { payload.put("access_token", it) }
                webSocket.send(msg(topic, "phx_join", payload))
                joined = true
                hb?.cancel()
                hb = scope.launch { while (true) { delay(25_000); webSocket.send(msg("phoenix", "heartbeat", JSONObject())) } }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val j = JSONObject(text)
                    if (j.optString("event") != "broadcast") return
                    val p = j.optJSONObject("payload") ?: return
                    if (p.optString("event") != "typing") return
                    if (p.optJSONObject("payload")?.optString("from") == me) return
                    _theirs.value = true
                    expire?.cancel()
                    expire = scope.launch { delay(4_000); _theirs.value = false }
                } catch (_: Exception) {}
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { dropped() }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { dropped() }
        })
    }

    private fun dropped() {
        joined = false
        hb?.cancel()
        if (wanted) { retry?.cancel(); retry = scope.launch { delay(4_000); open() } }
    }

    private fun msg(t: String, event: String, payload: JSONObject): String =
        JSONObject().put("topic", t).put("event", event).put("payload", payload).put("ref", (ref++).toString()).toString()

    /** Call as the person types: tells the other side (at most once every 2.5 s). */
    fun ping() {
        val now = System.currentTimeMillis()
        if (!joined || now - lastSent < 2_500) return
        lastSent = now
        val inner = JSONObject().put("from", me)
        val p = JSONObject().put("type", "broadcast").put("event", "typing").put("payload", inner)
        ws?.send(msg(topic, "broadcast", p))
    }

    fun leave() {
        wanted = false; joined = false
        hb?.cancel(); expire?.cancel(); retry?.cancel()
        try { ws?.close(1000, null) } catch (_: Exception) {}
        ws = null
        _theirs.value = false
    }
}
