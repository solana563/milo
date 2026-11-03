package com.ubayanda.milonative.data

import android.content.Context
import com.ubayanda.milonative.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Finds songs on YouTube with the official Data API (the same one the web milo. uses). Playing them goes through YouTube's own
 * embedded player (see YouTubeScreen), so video, ads and rules stay YouTube's. Results are [Track]s whose address is "yt:<video id>".
 */
class YouTubeRepo(context: Context) {
    private val app = context.applicationContext
    init { YtMusic.init(app) }
    private val prefs = context.getSharedPreferences("milo_yt", Context.MODE_PRIVATE)
    private val http = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(12, TimeUnit.SECONDS).build()

    /** The person's own key if they added one, otherwise the one the web milo. ships with. */
    var key: String = prefs.getString("key", "").orEmpty()
        private set

    fun setKey(k: String) { key = k.trim(); prefs.edit().putString("key", key).apply() }

    private fun idOf(videoId: String): Long = -(1L + (("yt:" + videoId).hashCode().toLong() and 0x7fffffffL))

    /** Songs from YouTube Music; if that is unreachable, the official Data API search below. */
    suspend fun search(term: String): List<Track> = withContext(Dispatchers.IO) {
        try {
            val r = YtMusic.searchSongs(term.trim()).map { YtMusic.toTrack(it) }.distinctBy { it.id }
            if (r.isNotEmpty()) return@withContext r
        } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (_: Exception) { }
        legacySearch(term)
    }

    suspend fun artists(term: String): List<YtMusic.Card> = withContext(Dispatchers.IO) { try { YtMusic.searchCards(term.trim(), "artist").take(12) } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (_: Exception) { emptyList() } }
    suspend fun albums(term: String): List<YtMusic.Card> = withContext(Dispatchers.IO) { try { YtMusic.searchCards(term.trim(), "album").take(12) } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (_: Exception) { emptyList() } }
    suspend fun page(id: String): YtMusic.Page = withContext(Dispatchers.IO) { YtMusic.page(id) }
    suspend fun radio(id: String): List<Track> = withContext(Dispatchers.IO) { YtMusic.radio(id).map { YtMusic.toTrack(it) } }
    /** True when a direct stream can be found, so the song can play in milo.'s own player. */
    suspend fun canStream(track: Track): Boolean = withContext(Dispatchers.IO) { try { YtMusic.stream(track.uri.removePrefix("yt:")); true } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (_: Exception) { false } }

    private suspend fun legacySearch(term: String): List<Track> = withContext(Dispatchers.IO) {
        val k = key.ifEmpty { "AIzaSyAFSMixowk9ptdAnT6ap3aK_gKO9fOmZ0E" }
        val url = "https://www.googleapis.com/youtube/v3/search?part=snippet&type=video&videoEmbeddable=true&videoCategoryId=10&maxResults=30&q=" +
            URLEncoder.encode(term.trim(), "UTF-8") + "&key=" + k
        val req = Request.Builder().url(url).header("User-Agent", "milo-native/1.0").build()
        val body = http.newCall(req).execute().use { r ->
            val tx = r.body?.string().orEmpty()
            if (!r.isSuccessful) {
                val msg = try { JSONObject(tx).optJSONObject("error")?.optString("message").orEmpty() } catch (e: Exception) { "" }
                throw IllegalStateException(
                    if (r.code == 403 || r.code == 400) "YouTube refused the search ($msg). You can add your own YouTube Data API key in Profile."
                    else "YouTube answered ${r.code}."
                )
            }
            tx
        }
        val arr = JSONObject(body).optJSONArray("items") ?: return@withContext emptyList()
        val out = ArrayList<Track>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val vid = o.optJSONObject("id")?.optString("videoId").orEmpty()
            if (vid.isEmpty()) continue
            val sn = o.optJSONObject("snippet") ?: continue
            val th = sn.optJSONObject("thumbnails")
            val thumb = (th?.optJSONObject("high") ?: th?.optJSONObject("medium") ?: th?.optJSONObject("default"))?.optString("url").orEmpty()
            val id = idOf(vid)
            out.add(
                Track(
                    id = id, uri = "yt:$vid", title = clean(sn.optString("title")), artist = clean(sn.optString("channelTitle")),
                    album = "YouTube", albumId = 4_000_000_000_000L + (-id), durationMs = 1L, trackNo = 0, year = 0, addedSec = 0L, artUrl = thumb,
                )
            )
        }
        out.distinctBy { it.id }
    }

    private fun clean(s: String) = android.text.Html.fromHtml(s, android.text.Html.FROM_HTML_MODE_LEGACY).toString()
}
