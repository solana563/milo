package com.ubayanda.milonative.data

import android.content.Context
import com.ubayanda.milonative.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Your own music server. Works with Navidrome and anything else that speaks the Subsonic API
 * (Airsonic, Gonic, Jellyfin with a Subsonic plugin...). Songs stream straight from your server and nothing is copied.
 */
class SubsonicRepo(context: Context) {
    private val prefs = context.getSharedPreferences("milo_server", Context.MODE_PRIVATE)
    private val http = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).build()

    var url: String = prefs.getString("url", "").orEmpty()
        private set
    var user: String = prefs.getString("user", "").orEmpty()
        private set
    private var pass: String = SecureStore.get(prefs, "pass").orEmpty()

    fun password(): String = pass

    val configured: Boolean get() = url.isNotEmpty() && user.isNotEmpty()

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
    private fun md5(s: String) = MessageDigest.getInstance("MD5").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }

    fun save(rawUrl: String, u: String, p: String) {
        var b = rawUrl.trim().trimEnd('/')
        if (b.isNotEmpty() && !b.startsWith("http://") && !b.startsWith("https://")) b = "http://$b"
        url = b; user = u.trim(); pass = p
        SecureStore.put(prefs.edit().putString("url", url).putString("user", user), "pass", pass).apply()
    }

    fun clear() {
        url = ""; user = ""; pass = ""
        prefs.edit().clear().apply()
    }

    /** Full address for one API call, signed with a fresh salt (the password itself is never sent). */
    private fun endpoint(method: String, extra: String = ""): String {
        val salt = java.util.UUID.randomUUID().toString().replace("-", "").take(12)
        return "$url/rest/$method.view?u=${enc(user)}&t=${md5(pass + salt)}&s=$salt&v=1.16.1&c=milo&f=json" + if (extra.isNotEmpty()) "&$extra" else ""
    }

    private suspend fun call(method: String, extra: String = ""): JSONObject = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(endpoint(method, extra)).header("User-Agent", "milo-native/1.0").build()
        val body = http.newCall(req).execute().use { r ->
            if (!r.isSuccessful) throw IllegalStateException("The server answered ${r.code}.")
            r.body?.string().orEmpty()
        }
        val root = JSONObject(body).optJSONObject("subsonic-response") ?: throw IllegalStateException("That doesn't look like a music server.")
        if (root.optString("status") != "ok") throw IllegalStateException(root.optJSONObject("error")?.optString("message").orEmpty().ifEmpty { "The server said no." })
        root
    }

    /** Null when the server answers and the login works, otherwise a message to show. */
    suspend fun check(): String? = try { call("ping"); null } catch (e: Exception) {
        when {
            e is java.io.IOException -> "Couldn't reach the server. Check the address and that it is running."
            e is org.json.JSONException -> "That doesn't look like a Subsonic or Navidrome server."
            else -> e.message ?: "Couldn't connect."
        }
    }

    private fun idOf(key: String): Long = -(1L + (key.hashCode().toLong() and 0x7fffffffL))

    private fun songs(arr: org.json.JSONArray?): List<Track> {
        if (arr == null) return emptyList()
        val out = ArrayList<Track>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val sid = o.optString("id")
            if (sid.isEmpty()) continue
            val id = idOf("sub:$sid")
            val cover = o.optString("coverArt")
            out.add(
                Track(
                    id = id, uri = endpoint("stream", "id=${enc(sid)}"),
                    title = o.optString("title", "Untitled"), artist = o.optString("artist", "Unknown artist"),
                    album = o.optString("album", "Unknown album"), albumId = 4_000_000_000_000L + (-id),
                    durationMs = o.optLong("duration") * 1000, trackNo = o.optInt("track"), year = o.optInt("year"), addedSec = 0L,
                    artUrl = if (cover.isNotEmpty()) endpoint("getCoverArt", "id=${enc(cover)}&size=600") else "",
                )
            )
        }
        return out.distinctBy { it.id }
    }

    suspend fun random(n: Int = 50): List<Track> =
        songs(call("getRandomSongs", "size=$n").optJSONObject("randomSongs")?.optJSONArray("song"))

    suspend fun starred(): List<Track> =
        songs(call("getStarred2").optJSONObject("starred2")?.optJSONArray("song"))

    suspend fun search(q: String): List<Track> =
        songs(call("search3", "query=${enc(q.trim())}&songCount=60&albumCount=0&artistCount=0").optJSONObject("searchResult3")?.optJSONArray("song"))
}
