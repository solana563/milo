package com.ubayanda.milonative.data

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.abs

/**
 * More places to look for lyrics when lrclib has nothing (or only unsynced text). Each source gives back LRC text,
 * or enhanced LRC ("[mm:ss.xx]<mm:ss.xx>word <mm:ss.xx>word") when it knows the timing of every word.
 *
 * The sources and how to talk to them come from Mochy Music (GPL-3.0, github.com/realidkroo/Mochy-Music):
 * BetterLyrics (word-by-word, Apple-sourced TTML), SimpMusic (by YouTube id) and KuGou. The code here is written for milo.
 * All of it blocks: call from a background thread (LyricsRepo does).
 */
object LyricsFallbacks {
    private fun q(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun get(url: String): String? = try {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 8000
        c.readTimeout = 10000
        c.setRequestProperty("User-Agent", "milo-native/1.0 (+https://github.com/ubayanda)")
        c.setRequestProperty("Accept", "application/json")
        if (c.responseCode == 200) c.inputStream.bufferedReader().use { it.readText() } else null
    } catch (_: Exception) { null }

    private fun stamp(ms: Long): String {
        val m = ms / 60000; val s = (ms % 60000) / 1000; val cs = (ms % 1000) / 10
        return "%02d:%02d.%02d".format(m, s, cs)
    }

    /** First source that has synced lyrics wins. Returns lrclib-shaped JSON ({"syncedLyrics": ...}) or null. */
    fun synced(title: String, artist: String, album: String, secs: Int, videoId: String?): String? {
        val lrc = runCatching { betterLyrics(title, artist, album, secs) }.getOrNull()
            ?: videoId?.let { runCatching { simpMusic(it, secs) }.getOrNull() }
            ?: runCatching { kuGou(title, artist, secs) }.getOrNull()
        return lrc?.takeIf { it.isNotBlank() }?.let { JSONObject().put("syncedLyrics", it).toString() }
    }

    // ---- BetterLyrics: TTML with word timing ----
    private fun betterLyrics(title: String, artist: String, album: String, secs: Int): String? {
        val url = "https://lyrics-api.boidu.dev/getLyrics?s=${q(title)}&a=${q(artist)}" +
            (if (secs > 0) "&d=$secs" else "") + (if (album.isNotBlank()) "&al=${q(album)}" else "")
        val ttml = JSONObject(get(url) ?: return null).optString("ttml")
        return if (ttml.isBlank()) null else ttmlToLrc(ttml)
    }

    private fun ttmlTime(s: String): Long {
        val t = s.trim().removeSuffix("s")
        val parts = t.split(":")
        val sec = when (parts.size) {
            1 -> parts[0].toDoubleOrNull() ?: 0.0
            2 -> (parts[0].toDoubleOrNull() ?: 0.0) * 60 + (parts[1].toDoubleOrNull() ?: 0.0)
            else -> (parts[0].toDoubleOrNull() ?: 0.0) * 3600 + (parts[1].toDoubleOrNull() ?: 0.0) * 60 + (parts[2].toDoubleOrNull() ?: 0.0)
        }
        return (sec * 1000).toLong()
    }

    private fun roleOf(e: Element): String {
        val a = e.attributes
        for (i in 0 until a.length) { val n = a.item(i).nodeName; if (n == "role" || n.endsWith(":role")) return a.item(i).nodeValue.orEmpty() }
        return ""
    }

    internal fun ttmlToLrc(ttml: String): String? {
        val f = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = false }
        val doc = f.newDocumentBuilder().parse(ttml.byteInputStream())
        val ps = doc.getElementsByTagName("p")
        val out = StringBuilder()
        for (i in 0 until ps.length) {
            val p = ps.item(i) as? Element ?: continue
            val begin = p.getAttribute("begin")
            if (begin.isEmpty()) continue
            // word = one or more syllable spans with no space between them
            val words = ArrayList<Pair<Long, String>>()
            var cur: StringBuilder? = null; var curAt = 0L
            val kids = p.childNodes
            for (j in 0 until kids.length) {
                val n = kids.item(j)
                if (n.nodeType == Node.TEXT_NODE) {
                    if (n.textContent.orEmpty().any { it == ' ' || it == '\t' }) { cur?.let { words.add(curAt to it.toString().trim()) }; cur = null }
                } else if (n.nodeType == Node.ELEMENT_NODE) {
                    val e = n as Element
                    if (!e.tagName.equals("span", true)) continue
                    val role = roleOf(e)
                    if (role == "x-bg" || role == "x-translation" || role == "x-roman") continue
                    val txt = e.textContent.orEmpty().trim()
                    val b = e.getAttribute("begin")
                    if (txt.isEmpty() || b.isEmpty()) continue
                    if (cur == null) { cur = StringBuilder(); curAt = ttmlTime(b) }
                    cur.append(txt)
                }
            }
            cur?.let { words.add(curAt to it.toString().trim()) }
            val start = ttmlTime(begin)
            if (words.isNotEmpty()) {
                out.append('[').append(stamp(start)).append(']')
                words.forEach { (t, w) -> if (w.isNotEmpty()) out.append('<').append(stamp(t)).append('>').append(w).append(' ') }
                out.append('\n')
            } else {
                val plain = p.textContent.orEmpty().replace(Regex("\\s+"), " ").trim()
                if (plain.isNotEmpty()) out.append('[').append(stamp(start)).append(']').append(plain).append('\n')
            }
        }
        return out.toString().takeIf { it.isNotBlank() }
    }

    // ---- SimpMusic: community lyrics keyed by YouTube video id ----
    private fun simpMusic(videoId: String, secs: Int): String? {
        val body = get("https://api-lyrics.simpmusic.org/v1/$videoId") ?: return null
        val o = JSONObject(body)
        if (o.optString("type") != "success") return null
        val arr = o.optJSONArray("data") ?: return null
        var best: String? = null; var bestD = Int.MAX_VALUE
        for (i in 0 until arr.length()) {
            val t = arr.optJSONObject(i) ?: continue
            val lrc = t.optString("syncedLyrics")
            if (lrc.isBlank()) continue
            val d = if (secs > 0 && t.has("durationSeconds")) abs(t.optInt("durationSeconds") - secs) else 0
            if (d <= 10 && d < bestD) { best = lrc; bestD = d }
        }
        return best
    }

    // ---- KuGou: large catalogue, plain LRC ----
    private fun kuGou(title: String, artist: String, secs: Int): String? {
        val kw = "${title.replace(Regex("[(（「『<《〈＜].*?[)）」』>》〉＞]"), "").trim()} - ${artist.replace(", ", "、").replace(" & ", "、").trim()}"
        val songs = JSONObject(get("https://mobileservice.kugou.com/api/v3/search/song?version=9108&plat=0&pagesize=8&showtype=0&keyword=${q(kw)}") ?: return null)
            .optJSONObject("data")?.optJSONArray("info") ?: return null
        for (i in 0 until songs.length()) {
            val s = songs.optJSONObject(i) ?: continue
            if (secs > 0 && abs(s.optInt("duration") - secs) > 8) continue
            val hash = s.optString("hash").ifEmpty { continue }
            val cands = JSONObject(get("https://lyrics.kugou.com/search?ver=1&man=yes&client=pc&hash=$hash") ?: continue).optJSONArray("candidates") ?: continue
            val c = cands.optJSONObject(0) ?: continue
            val dl = JSONObject(get("https://lyrics.kugou.com/download?fmt=lrc&charset=utf8&client=pc&ver=1&id=${c.optLong("id")}&accesskey=${q(c.optString("accesskey"))}") ?: continue)
            val text = String(Base64.decode(dl.optString("content"), Base64.DEFAULT), Charsets.UTF_8)
            val time = Regex("""\[(\d\d):(\d\d)\.(\d{2,3})].*""")
            val credit = Regex(""".+].+[:：].+""")
            val lines = text.lines().filter { time.matches(it) }
            // drop the credit lines ("Lyrics by: ...") that open and close KuGou's files
            var from = 0
            for (k in minOf(6, lines.lastIndex) downTo 0) if (credit.matches(lines[k])) { from = k + 1; break }
            val res = lines.drop(from).joinToString("\n")
            if (res.isNotBlank()) return res
        }
        return null
    }
}
