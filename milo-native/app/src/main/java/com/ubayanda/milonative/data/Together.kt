package com.ubayanda.milonative.data

import com.ubayanda.milonative.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Listen together: one person hosts, others join with a code. The host's YouTube song, position and play/pause are broadcast over the same
 * realtime channel the web app uses (Supabase Realtime, Phoenix protocol), so a web listener and a phone can share a session.
 */
class Together(private val token: suspend () -> String?, private val onState: (JSONObject) -> Unit, private val onHello: () -> Unit) {
    private val http = OkHttpClient.Builder().pingInterval(25, TimeUnit.SECONDS).readTimeout(0, TimeUnit.MILLISECONDS).build()
    private var ws: WebSocket? = null
    private var ref = 0
    private val _code = MutableStateFlow("")
    val code: StateFlow<String> = _code
    var host = false
        private set
    private val topic get() = "realtime:milo-together-" + _code.value.lowercase()

    suspend fun join(c: String, asHost: Boolean, supabaseUrl: String, anonKey: String): Boolean {
        leave()
        val at = token() ?: return false
        _code.value = c.uppercase(); host = asHost
        val url = supabaseUrl.replace("https://", "wss://") + "/realtime/v1/websocket?apikey=$anonKey&vsn=1.0.0"
        val socket = http.newWebSocket(Request.Builder().url(url).build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                send(webSocket, topic, "phx_join", JSONObject().put("config", JSONObject().put("broadcast", JSONObject().put("self", false)).put("presence", JSONObject().put("key", ""))).put("access_token", at))
                if (!asHost) webSocket.send(msg(topic, "broadcast", JSONObject().put("type", "broadcast").put("event", "hello").put("payload", JSONObject())))
            }
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val m = JSONObject(text)
                    if (m.optString("event") != "broadcast" || m.optString("topic") != topic) return
                    val p = m.getJSONObject("payload")
                    when (p.optString("event")) {
                        "st" -> if (!host) onState(p.getJSONObject("payload"))
                        "hello" -> if (host) onHello()
                    }
                } catch (_: Exception) {}
            }
        })
        ws = socket
        // keep the socket alive the way Phoenix expects
        Thread {
            while (ws === socket) { try { Thread.sleep(25_000) } catch (_: Exception) {}; if (ws === socket) send(socket, "phoenix", "heartbeat", JSONObject()) }
        }.apply { isDaemon = true }.start()
        return true
    }

    fun leave() { ws?.close(1000, null); ws = null; _code.value = ""; host = false }

    /** Host only: tells guests what is playing. */
    fun broadcast(videoId: String, t: Track, posMs: Long, playing: Boolean) {
        val w = ws ?: return
        val st = JSONObject().put("yt", videoId).put("t", t.title).put("a", t.artist).put("img", t.artUrl).put("d", t.durationMs / 1000)
            .put("pos", posMs / 1000.0).put("playing", playing).put("ts", System.currentTimeMillis())
        w.send(msg(topic, "broadcast", JSONObject().put("type", "broadcast").put("event", "st").put("payload", st)))
    }

    private fun msg(topic: String, event: String, payload: JSONObject) = JSONObject().put("topic", topic).put("event", event).put("payload", payload).put("ref", (++ref).toString()).toString()
    private fun send(w: WebSocket, topic: String, event: String, payload: JSONObject) { w.send(msg(topic, event, payload)) }
}
