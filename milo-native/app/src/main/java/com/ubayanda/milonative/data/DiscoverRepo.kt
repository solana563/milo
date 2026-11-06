package com.ubayanda.milonative.data

import com.ubayanda.milonative.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Online listening, no account or key needed:
 *  - live radio from the free radio-browser.info directory
 *  - 30-second previews from the iTunes Search API
 * Results become ordinary [Track]s (with a web address instead of a file), so the same player plays them.
 */
class DiscoverRepo {
    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS).readTimeout(12, TimeUnit.SECONDS).build()

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    data class ChartItem(val name: String, val artist: String)

    /** A real country chart (Apple Music's "most played" list): [kind] is "songs" or "albums". Empty when the country has none or there is no connection. */
    suspend fun chart(country: String, kind: String, limit: Int = 12): List<ChartItem> = try {
        val o = org.json.JSONObject(get("https://rss.applemarketingtools.com/api/v2/${country.lowercase()}/music/most-played/$limit/$kind.json"))
        val arr = o.getJSONObject("feed").getJSONArray("results")
        (0 until arr.length()).map { arr.getJSONObject(it) }.map { ChartItem(it.optString("name"), it.optString("artistName")) }.filter { it.name.isNotBlank() }
    } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (_: Exception) { emptyList() }

    private suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(url).header("User-Agent", "milo-native/1.0").build()
        http.newCall(req).execute().use { r ->
            if (!r.isSuccessful) throw IllegalStateException("HTTP ${r.code}")
            r.body?.string().orEmpty()
        }
    }

    /** Quick check that a stream answers with audio before it is queued; a dead one would otherwise upset the player. */
    suspend fun reachable(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url(url).header("User-Agent", "milo-native/1.0").header("Icy-MetaData", "0").build()
            http.newCall(req).execute().use { r ->
                if (!r.isSuccessful) return@use false
                val type = r.header("Content-Type").orEmpty().lowercase()
                !type.startsWith("text/html")
            }
        } catch (e: java.net.ProtocolException) {
            true // Shoutcast servers answer "ICY 200 OK", which OkHttp rejects but the player accepts
        } catch (e: Exception) {
            false
        }
    }

    /** Stable, unique id for an online item: negative so it never clashes with a MediaStore id. */
    private fun idOf(key: String): Long = -(1L + (key.hashCode().toLong() and 0x7fffffffL))
    private fun albumKey(id: Long): Long = 4_000_000_000_000L + (-id)

    /** Live radio. [query] searches by station name; [tag] filters by genre/tag; both empty = the most voted stations. */
    suspend fun radio(query: String, tag: String, country: String = ""): List<Track> {
        val base = "https://de1.api.radio-browser.info/json/stations/search?limit=50&hidebroken=true&order=votes&reverse=true"
        val url = base + (if (query.isNotBlank()) "&name=" + enc(query.trim()) else "") + (if (tag.isNotBlank()) "&tag=" + enc(tag) else "") + (if (country.length == 2) "&countrycode=" + country else "")
        val arr = JSONArray(get(url))
        val out = ArrayList<Track>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val stream = o.optString("url_resolved").ifBlank { o.optString("url") }
            if (stream.isBlank() || !stream.startsWith("http")) continue
            val name = o.optString("name").trim()
            if (name.isEmpty()) continue
            val id = idOf("radio:$stream")
            val sub = listOf(o.optString("country"), o.optString("tags").split(",").firstOrNull().orEmpty()).filter { it.isNotBlank() }.joinToString(" · ")
            out.add(
                Track(
                    id = id, uri = stream, title = name, artist = sub.ifEmpty { "Live radio" }, album = "Live radio",
                    albumId = albumKey(id), durationMs = 0L, trackNo = 0, year = 0, addedSec = 0L,
                    artUrl = o.optString("favicon").takeIf { it.startsWith("http") }.orEmpty(),
                )
            )
        }
        return out.distinctBy { it.id }
    }

    /** 30-second song previews. */
    suspend fun previews(term: String, country: String = ""): List<Track> {
        val obj = JSONObject(get("https://itunes.apple.com/search?media=music&entity=song&limit=50" + (if (country.length == 2) "&country=" + country else "") + "&term=" + enc(term.trim())))
        val arr = obj.optJSONArray("results") ?: return emptyList()
        val out = ArrayList<Track>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val prev = o.optString("previewUrl")
            if (prev.isBlank()) continue
            val id = idOf("prev:" + o.optLong("trackId") + prev)
            out.add(
                Track(
                    id = id, uri = prev, title = o.optString("trackName", "Song"), artist = o.optString("artistName"),
                    album = o.optString("collectionName", "Preview"), albumId = albumKey(id), durationMs = 30_000L, trackNo = 0,
                    year = o.optString("releaseDate").take(4).toIntOrNull() ?: 0, addedSec = 0L,
                    artUrl = o.optString("artworkUrl100").replace("100x100", "600x600"),
                )
            )
        }
        return out.distinctBy { it.id }
    }
}
