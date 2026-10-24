package com.ubayanda.milonative.data

import android.content.Context
import com.ubayanda.milonative.model.LyricLine
import com.ubayanda.milonative.model.Lyrics
import com.ubayanda.milonative.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Fetches lyrics from lrclib.net (same source as the web app) and keeps a copy on the phone so a song is only looked up once. */
class LyricsRepo(private val context: Context) {
    private val dir = File(context.cacheDir, "lyrics").also { it.mkdirs() }

    suspend fun get(t: Track): Lyrics? = withContext(Dispatchers.IO) {
        val f = File(dir, "${t.id}.json")
        val cached = try { if (f.exists()) f.readText() else null } catch (e: Exception) { null }
        val body = cached ?: fetch(t)?.also { try { f.writeText(it) } catch (_: Exception) {} }
        body?.let { parse(it) }
    }

    private fun q(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun fetch(t: Track): String? {
        val artist = if (t.artist == "Unknown artist") "" else t.artist
        val url = "https://lrclib.net/api/get?track_name=${q(t.title)}&artist_name=${q(artist)}&album_name=${q(t.album)}&duration=${t.durationMs / 1000}"
        return try {
            val c = URL(url).openConnection() as HttpURLConnection
            c.connectTimeout = 8000
            c.readTimeout = 8000
            c.setRequestProperty("User-Agent", "milo-native/1.0")
            if (c.responseCode == 200) c.inputStream.bufferedReader().use { it.readText() } else search(t)
        } catch (e: Exception) {
            null
        }
    }

    /** Exact lookup missed: try the looser search and take the first hit. */
    private fun search(t: Track): String? = try {
        val url = "https://lrclib.net/api/search?q=${q(t.title + " " + t.artist)}"
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 8000
        c.readTimeout = 8000
        c.setRequestProperty("User-Agent", "milo-native/1.0")
        if (c.responseCode != 200) null else {
            val arr = org.json.JSONArray(c.inputStream.bufferedReader().use { it.readText() })
            if (arr.length() == 0) null else arr.getJSONObject(0).toString()
        }
    } catch (e: Exception) {
        null
    }

    private fun parse(body: String): Lyrics? {
        return try {
            val o = JSONObject(body)
            val synced = o.optString("syncedLyrics", "")
            val plain = o.optString("plainLyrics", "")
            val lines = ArrayList<LyricLine>()
            val re = Regex("""\[(\d+):(\d+)(?:[.:](\d+))?](.*)""")
            synced.lineSequence().forEach { raw ->
                val m = re.find(raw.trim()) ?: return@forEach
                val mm = m.groupValues[1].toLong()
                val ss = m.groupValues[2].toLong()
                val frac = m.groupValues[3]
                val ms = if (frac.isEmpty()) 0L else (frac.padEnd(3, '0').take(3)).toLong()
                lines.add(LyricLine(mm * 60_000 + ss * 1000 + ms, m.groupValues[4].trim()))
            }
            if (lines.isEmpty() && plain.isBlank()) null else Lyrics(lines, plain)
        } catch (e: Exception) {
            null
        }
    }
}
