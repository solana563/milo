package com.ubayanda.milonative.data

import android.content.Context
import com.ubayanda.milonative.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Sends what you play to ListenBrainz (token) and Last.fm (API key + secret + your login).
 * A play is sent once it has counted (30 seconds, or half of a short song). Plays that fail are kept and retried with the next one.
 */
class Scrobbler(ctx: Context) {
    private val p = ctx.getSharedPreferences("milo_scrobble", 0)
    private val http = OkHttpClient.Builder().connectTimeout(12, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build()
    private val json = "application/json".toMediaType()
    private val queue = ArrayList<JSONObject>()
    private var nowKey = ""

    val lbToken: String get() = p.getString("lb", "") ?: ""
    val fmUser: String get() = p.getString("fmUser", "") ?: ""
    val fmKey: String get() = p.getString("fmKey", "") ?: ""
    val fmSecret: String get() = p.getString("fmSecret", "") ?: ""
    private val fmSession: String get() = p.getString("fmSk", "") ?: ""
    val lbOn get() = lbToken.isNotBlank()
    val fmOn get() = fmSession.isNotBlank()

    init {
        try { val a = JSONArray(p.getString("q", "[]")); for (i in 0 until a.length()) queue.add(a.getJSONObject(i)) } catch (_: Exception) {}
    }

    fun setListenBrainz(token: String) { p.edit().putString("lb", token.trim()).apply() }
    fun disconnectLastFm() { p.edit().remove("fmSk").remove("fmUser").apply() }

    /** Signs in to Last.fm once; the password is not kept. Returns null when it worked, otherwise a message. */
    suspend fun connectLastFm(key: String, secret: String, user: String, pass: String): String? = withContext(Dispatchers.IO) {
        try {
            val j = fm(key, secret, "auth.getMobileSession", mapOf("username" to user, "password" to pass))
            val s = j.getJSONObject("session")
            p.edit().putString("fmKey", key).putString("fmSecret", secret).putString("fmUser", s.getString("name")).putString("fmSk", s.getString("key")).apply()
            null
        } catch (e: Exception) { e.message ?: "Could not connect to Last.fm" }
    }

    private fun md5(s: String) = MessageDigest.getInstance("MD5").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }

    private fun fm(key: String, secret: String, method: String, extra: Map<String, String>): JSONObject {
        val m = HashMap(extra); m["method"] = method; m["api_key"] = key
        val sig = md5(m.toSortedMap().entries.joinToString("") { it.key + it.value } + secret)
        val body = FormBody.Builder().apply { m.forEach { (k, v) -> add(k, v) }; add("api_sig", sig); add("format", "json") }.build()
        http.newCall(Request.Builder().url("https://ws.audioscrobbler.com/2.0/").post(body).build()).execute().use { r ->
            val j = try { JSONObject(r.body?.string().orEmpty()) } catch (_: Exception) { JSONObject() }
            if (j.has("error")) throw IllegalStateException(j.optString("message", "Last.fm error"))
            if (!r.isSuccessful) throw IllegalStateException("Last.fm answered ${r.code}")
            return j
        }
    }

    private fun artistOf(t: Track) = t.artist.split(Regex(""",\s*|\s+&\s+|\s+feat\.?\s+""", RegexOption.IGNORE_CASE)).firstOrNull()?.trim().orEmpty()
    private fun ok(t: Track) = t.title.isNotBlank() && artistOf(t).isNotBlank() && !t.artist.equals("<unknown>", true)

    private fun lbCall(type: String, t: JSONObject, ts: Long) {
        val meta = JSONObject().put("track_name", t.getString("t")).put("artist_name", t.getString("a"))
            .put("additional_info", JSONObject().put("media_player", "milo.").put("submission_client", "milo.").apply { if (t.optLong("d") > 0) put("duration_ms", t.getLong("d")) })
        if (t.optString("al").isNotBlank()) meta.put("release_name", t.getString("al"))
        val item = JSONObject().put("track_metadata", meta).apply { if (type != "playing_now") put("listened_at", ts) }
        val body = JSONObject().put("listen_type", type).put("payload", JSONArray().put(item)).toString().toRequestBody(json)
        http.newCall(Request.Builder().url("https://api.listenbrainz.org/1/submit-listens").header("Authorization", "Token $lbToken").post(body).build()).execute().use {
            if (it.code == 401) throw IllegalStateException("ListenBrainz token was refused")
            if (!it.isSuccessful) throw IllegalStateException("ListenBrainz answered ${it.code}")
        }
    }

    private fun fmCall(type: String, t: JSONObject, ts: Long) {
        val m = hashMapOf("artist" to t.getString("a"), "track" to t.getString("t"), "sk" to fmSession)
        if (t.optLong("d") > 0) m["duration"] = (t.getLong("d") / 1000).toString()
        if (type == "playing_now") fm(fmKey, fmSecret, "track.updateNowPlaying", m)
        else { m["timestamp"] = ts.toString(); fm(fmKey, fmSecret, "track.scrobble", m) }
    }

    private fun item(t: Track) = JSONObject().put("t", t.title).put("a", artistOf(t)).put("al", if (t.album == "YouTube" || t.album.isBlank()) "" else t.album).put("d", t.durationMs)

    /** Call when a song starts playing. */
    suspend fun nowPlaying(t: Track) = withContext(Dispatchers.IO) {
        if (!ok(t) || (!lbOn && !fmOn) || nowKey == t.uri) return@withContext
        nowKey = t.uri
        val it = item(t)
        try { if (lbOn) lbCall("playing_now", it, 0) } catch (_: Exception) {}
        try { if (fmOn) fmCall("playing_now", it, 0) } catch (_: Exception) {}
    }

    /** Call when a play has counted. */
    suspend fun scrobble(t: Track) = withContext(Dispatchers.IO) {
        if (!ok(t) || (!lbOn && !fmOn)) return@withContext
        synchronized(queue) {
            queue.add(item(t).put("ts", System.currentTimeMillis() / 1000 - 30).put("lb", lbOn).put("fm", fmOn))
            while (queue.size > 60) queue.removeAt(0)
        }
        flush()
    }

    private fun flush() {
        synchronized(queue) {
            val it = queue.iterator()
            while (it.hasNext()) {
                val x = it.next()
                try { if (x.optBoolean("lb")) { lbCall("single", x, x.getLong("ts")); x.put("lb", false) } } catch (_: Exception) {}
                try { if (x.optBoolean("fm")) { fmCall("single", x, x.getLong("ts")); x.put("fm", false) } } catch (_: Exception) {}
                if (!x.optBoolean("lb") && !x.optBoolean("fm")) it.remove()
            }
            p.edit().putString("q", JSONArray(queue).toString()).apply()
        }
    }
}
