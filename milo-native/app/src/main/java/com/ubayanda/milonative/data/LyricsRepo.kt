package com.ubayanda.milonative.data

import android.content.Context
import com.ubayanda.milonative.model.LyricLine
import com.ubayanda.milonative.model.LyricWord
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
        // lyrics saved together with a downloaded song, so they work with no connection
        val pinned = if (cached == null && t.uri.startsWith("yt:")) try { File(context.filesDir, "offline/" + t.uri.removePrefix("yt:") + ".lrc").takeIf { it.exists() }?.readText() } catch (e: Exception) { null } else null
        val body = cached ?: pinned ?: fetch(t)?.also { try { f.writeText(it) } catch (_: Exception) {} }
        body?.let { parse(it) }
    }

    /** Songs whose saved lyrics contain [query]: (track id, the matching line). Only songs whose lyrics were opened before are searchable. */
    suspend fun searchCached(query: String): List<Pair<Long, String>> = withContext(Dispatchers.IO) {
        val needle = query.trim().lowercase()
        if (needle.length < 3) return@withContext emptyList()
        val out = ArrayList<Pair<Long, String>>()
        for (f in dir.listFiles().orEmpty()) {
            val id = f.nameWithoutExtension.toLongOrNull() ?: continue
            try {
                val j = org.json.JSONObject(f.readText())
                val text = j.optString("plainLyrics").ifEmpty { j.optString("syncedLyrics").replace(Regex("\\[[^\\]]*\\]"), "") }
                val line = text.lines().firstOrNull { it.lowercase().contains(needle) }?.trim()
                if (line != null) out.add(id to line)
            } catch (_: Exception) {}
            if (out.size >= 30) break
        }
        out
    }

    /** Saves the lyrics of [t] into [f] (used when a song is downloaded). */
    fun fetchTo(f: File, t: Track) { try { fetch(t)?.let { f.writeText(it) } } catch (_: Exception) {} }

    private fun q(s: String) = URLEncoder.encode(s, "UTF-8")

    /** Throws away the saved copy and looks the song up again (the Rescan button). */
    suspend fun rescan(t: Track): Lyrics? = withContext(Dispatchers.IO) {
        try { File(dir, "${t.id}.json").delete() } catch (_: Exception) {}
        val body = fetch(t)?.also { try { File(dir, "${t.id}.json").writeText(it) } catch (_: Exception) {} }
        body?.let { parse(it) }
    }

    private val tags = Regex("""\s*[\(\[][^)\]]*\b(official|music video|lyric|lyrics|video|audio|visuali[sz]er|live|hd|hq|4k|remaster(ed)?|explicit|clip|performance)\b[^)\]]*[\)\]]""", RegexOption.IGNORE_CASE)

    /** True when the title says it is a video, live or lyric-video upload rather than the studio recording. */
    fun looksLikeVideo(title: String) = tags.containsMatchIn(title) || Regex("""\b(official (music )?video|lyric video|live at|live from)\b""", RegexOption.IGNORE_CASE).containsMatchIn(title)

    /** "Artist - Song (Official Video)" becomes "Song", so the lookup finds the real song. */
    fun cleanTitle(title: String, artist: String): String {
        var t = title.replace(tags, "")
        t = t.replace(Regex("""\s*[-|]\s*(official.*|lyrics?.*|video.*|audio.*)$""", RegexOption.IGNORE_CASE), "")
        if (artist.isNotBlank() && artist != "Unknown artist" && t.startsWith("$artist - ", true)) t = t.substring(artist.length + 3)
        else if (t.contains(" - ") && artist.isBlank()) t = t.substringAfter(" - ")
        return t.trim().ifEmpty { title }
    }

    private fun open(url: String): String? = try {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 8000
        c.readTimeout = 8000
        c.setRequestProperty("User-Agent", "milo-native/1.0")
        if (c.responseCode == 200) c.inputStream.bufferedReader().use { it.readText() } else null
    } catch (e: Exception) { null }

    private fun fetch(t: Track): String? {
        val artist = if (t.artist == "Unknown artist") "" else t.artist
        val title = cleanTitle(t.title, artist)
        val secs = (t.durationMs / 1000).toInt()
        // 1) exact studio match (needs the length to agree within 2 seconds, so a longer video version misses on purpose)
        open("https://lrclib.net/api/get?track_name=${q(title)}&artist_name=${q(artist)}&album_name=${q(t.album)}&duration=$secs")?.let { return it }
        open("https://lrclib.net/api/get?track_name=${q(title)}&artist_name=${q(artist)}&duration=$secs")?.let { return it }
        // 2) search, then take the version whose length is closest to this one
        return bestOf(open("https://lrclib.net/api/search?q=${q("$title $artist".trim())}"), secs, looksLikeVideo(t.title))
    }

    private fun bestOf(body: String?, secs: Int, video: Boolean): String? = try {
        val arr = org.json.JSONArray(body ?: return null)
        var best: JSONObject? = null; var bestD = Int.MAX_VALUE
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            if (o.optString("syncedLyrics").isBlank() && o.optString("plainLyrics").isBlank()) continue
            // a synced version wins over a plain one unless it is much further off in length
            val d = Math.abs(o.optInt("duration", secs) - secs) + (if (o.optString("syncedLyrics").isBlank()) 6 else 0)
            if (d < bestD) { best = o; bestD = d }
        }
        best?.let { o ->
            val delta = o.optInt("duration", secs) - secs
            if (Math.abs(delta) > 3) o.put("miloNote",
                (if (video) "You are playing a video or live version. " else "") + "These lyrics are from a version that is ${Math.abs(delta)}s ${if (delta > 0) "longer" else "shorter"}. If the lines drift, nudge the timing with the \u22C5\u22C5\u22C5 menu.")
            o.toString()
        }
    } catch (e: Exception) { null }

    private val previews = HashMap<String, List<String>>()

    /** A few lines of a song, for the quote on a friend's post. Remembered while the app is open. */
    suspend fun preview(title: String, artist: String, durSec: Int): List<String> = withContext(Dispatchers.IO) {
        val key = "$title|$artist"
        previews[key]?.let { return@withContext it }
        val clean = cleanTitle(title, artist)
        val body = open("https://lrclib.net/api/get?track_name=${q(clean)}&artist_name=${q(artist)}&duration=$durSec")
            ?: bestOf(open("https://lrclib.net/api/search?q=${q("$clean $artist".trim())}"), if (durSec > 0) durSec else 200, false)
        val lines = try {
            val o = JSONObject(body ?: "{}")
            val synced = o.optString("syncedLyrics")
            val src = if (synced.isNotBlank()) synced.lines().map { it.replace(Regex("""\[[^\]]*]"""), "").replace(Regex("<[^>]*>"), "").trim() } else o.optString("plainLyrics").lines().map { it.trim() }
            src.filter { it.isNotBlank() }.drop(2).take(4)
        } catch (_: Exception) { emptyList() }
        previews[key] = lines
        lines
    }

    private fun parse(body: String): Lyrics? {
        return try {
            val o = JSONObject(body)
            val synced = o.optString("syncedLyrics", "")
            val plain = o.optString("plainLyrics", "")
            val lines = ArrayList<LyricLine>()
            val re = Regex("""\[(\d+):(\d+)(?:[.:](\d+))?](.*)""")
            val wordRe = Regex("""<(\d+):(\d+)(?:[.:](\d+))?>([^<]*)""")
            synced.lineSequence().forEach { raw ->
                val m = re.find(raw.trim()) ?: return@forEach
                val mm = m.groupValues[1].toLong()
                val ss = m.groupValues[2].toLong()
                val frac = m.groupValues[3]
                val ms = if (frac.isEmpty()) 0L else (frac.padEnd(3, '0').take(3)).toLong()
                val body = m.groupValues[4]
                // enhanced LRC: <mm:ss.xx>word gives word-by-word timing
                val words = wordRe.findAll(body).mapNotNull { w ->
                    val txt = w.groupValues[4].trim()
                    if (txt.isEmpty()) null else {
                        val f = w.groupValues[3]
                        LyricWord(w.groupValues[1].toLong() * 60_000 + w.groupValues[2].toLong() * 1000 + (if (f.isEmpty()) 0L else f.padEnd(3, '0').take(3).toLong()), txt)
                    }
                }.toList()
                val clean = body.replace(Regex("<[^>]*>"), "").replace(Regex("\\s+"), " ").trim()
                lines.add(LyricLine(mm * 60_000 + ss * 1000 + ms, clean, if (words.size >= 2) words else emptyList()))
            }
            if (lines.isEmpty() && plain.isBlank()) null else Lyrics(lines, plain, o.optString("miloNote", ""))
        } catch (e: Exception) {
            null
        }
    }
}
