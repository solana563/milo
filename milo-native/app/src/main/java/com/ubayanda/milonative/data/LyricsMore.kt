package com.ubayanda.milonative.data

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.abs

/**
 * Three more lyric sources, after BetterLyrics, SimpMusic and KuGou in [LyricsFallbacks]: NetEase Cloud Music (often word-timed),
 * Deezer (word-timed when it has it) and QQ Music (line-timed). None needs an account. The sources and the way to ask each come from
 * Mochy Music (GPL-3.0, github.com/realidkroo/Mochy-Music, providers/directlyrics); the code is written for milo.
 * Blocking: call from a background thread. Each returns LRC or enhanced LRC ("[mm:ss.xx]<mm:ss.xx>word ..."), or null.
 */
object LyricsMore {
    private fun q(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun http(url: String, body: String? = null, bearer: String? = null): String? = try {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 6000
        c.readTimeout = 10000
        c.setRequestProperty("User-Agent", "milo-native/1.0 (+https://github.com/ubayanda)")
        c.setRequestProperty("Accept", "application/json")
        if (bearer != null) c.setRequestProperty("Authorization", "Bearer $bearer")
        if (body != null) {
            c.requestMethod = "POST"
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json")
            c.outputStream.use { it.write(body.toByteArray()) }
        }
        if (c.responseCode == 200) c.inputStream.bufferedReader().use { it.readText() } else null
    } catch (_: Exception) { null }

    private fun stamp(ms: Long): String {
        val m = ms / 60000; val s = (ms % 60000) / 1000; val cs = (ms % 1000) / 10
        return "%02d:%02d.%02d".format(m, s, cs)
    }

    private class Cand<T>(val titles: List<String>, val artists: List<String>, val secs: Int, val ref: T)

    private val deco = Regex("""\s*[(（\[【「『《].*?[)）\]】」』》]""")
    private fun norm(s: String) = s.replace(deco, "").lowercase().filter { it.isLetterOrDigit() }

    /** the result that is this song: about the same length, same title, and the artist when it matches (the top hit is often a cover) */
    private fun <T> List<Cand<T>>.best(title: String, artist: String, secs: Int): T? {
        val wt = norm(title); val wa = norm(artist)
        if (wt.isEmpty()) return null
        return asSequence().filter { secs <= 0 || abs(it.secs - secs) <= 8 }
            .map { c ->
                val ts = c.titles.map(::norm).filter { it.isNotEmpty() }.maxOfOrNull { t ->
                    when { t == wt -> 2; t.contains(wt) || wt.contains(t) -> 1; else -> 0 }
                } ?: 0
                val ar = if (wa.isNotEmpty() && c.artists.map(::norm).any { it.isNotEmpty() && (wa.contains(it) || it.contains(wa)) }) 2 else 0
                Triple(c, ts, ar)
            }.filter { it.second > 0 }.maxByOrNull { it.second + it.third }?.first?.ref
    }

    private fun JSONArray?.names(key: String): List<String> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it)?.optString(key)?.takeIf { s -> s.isNotBlank() } }

    private fun hasLines(lrc: String) = lrc.lines().count { it.trimStart().startsWith("[") } >= 3

    // ---- NetEase ----
    fun netEase(title: String, artist: String, secs: Int): String? {
        val res = JSONObject(http("https://music.163.com/api/search/get?s=${q("$title $artist")}&type=1&limit=10") ?: return null)
        val songs = res.optJSONObject("result")?.optJSONArray("songs") ?: return null
        val cands = (0 until songs.length()).map { songs.getJSONObject(it) }.map { s ->
            Cand(listOf(s.optString("name")) + (s.optJSONArray("alias")?.let { a -> (0 until a.length()).map { a.optString(it) } } ?: emptyList()),
                s.optJSONArray("artists").names("name"), (s.optLong("duration") / 1000).toInt(), s.optLong("id"))
        }
        val id = cands.best(title, artist, secs) ?: return null
        val l = JSONObject(http("https://music.163.com/api/song/lyric?id=$id&lv=1&yv=1") ?: return null)
        l.optJSONObject("yrc")?.optString("lyric")?.takeIf { it.isNotBlank() }?.let { yrcToLrc(it) }?.let { return it }
        return l.optJSONObject("lrc")?.optString("lyric")?.takeIf { hasLines(it) }
    }

    private val yrcLine = Regex("""^\[(\d+),\d+](.*)$""")
    private val yrcWord = Regex("""\((\d+),(\d+),-?\d+\)(.*?)(?=\(\d+,\d+,-?\d+\)|$)""")

    /** NetEase word format "[lineStart,len](wordStart,len,0)word" in ms; json credit lines have no words and are skipped */
    private fun yrcToLrc(yrc: String): String? {
        val sb = StringBuilder(); var n = 0
        for (raw in yrc.lineSequence()) {
            val m = yrcLine.matchEntire(raw.trim()) ?: continue
            val words = yrcWord.findAll(m.groupValues[2]).toList()
            if (words.isEmpty()) continue
            sb.append('[').append(stamp(m.groupValues[1].toLong())).append(']')
            for (w in words) sb.append('<').append(stamp(w.groupValues[1].toLong())).append('>').append(w.groupValues[3])
            sb.append('\n'); n++
        }
        return if (n >= 3) sb.toString() else null
    }

    // ---- QQ Music ----
    private fun musicu(module: String, method: String, param: JSONObject): JSONObject? {
        val body = JSONObject().put("req", JSONObject().put("module", module).put("method", method).put("param", param))
        return http("https://u.y.qq.com/cgi-bin/musicu.fcg", body.toString())?.let { JSONObject(it) }
    }

    fun qqMusic(title: String, artist: String, secs: Int): String? {
        val r = musicu("music.search.SearchCgiService", "DoSearchForQQMusicDesktop",
            JSONObject().put("query", "$title $artist").put("num_per_page", 10).put("page_num", 1).put("search_type", 0)) ?: return null
        val list = r.optJSONObject("req")?.optJSONObject("data")?.optJSONObject("body")?.optJSONObject("song")?.optJSONArray("list") ?: return null
        val cands = (0 until list.length()).map { list.getJSONObject(it) }.map { s ->
            Cand(listOf(s.optString("name")), s.optJSONArray("singer").names("name"), s.optInt("interval"), s)
        }
        val song = cands.best(title, artist, secs) ?: return null
        val l = musicu("music.musichallSong.PlayLyricInfo", "GetPlayLyricInfo",
            JSONObject().put("songMID", song.optString("mid")).put("songID", song.optLong("id"))) ?: return null
        val enc = l.optJSONObject("req")?.optJSONObject("data")?.optString("lyric").orEmpty()
        if (enc.isBlank()) return null
        val lrc = String(Base64.decode(enc, Base64.DEFAULT), Charsets.UTF_8)
        return lrc.takeIf { hasLines(it) }
    }

    // ---- Deezer ----
    @Volatile private var dzToken: String? = null
    @Volatile private var dzUntil = 0L

    private fun dzJwt(): String? {
        dzToken?.takeIf { System.currentTimeMillis() < dzUntil }?.let { return it }
        val jwt = JSONObject(http("https://auth.deezer.com/login/anonymous?jo=p&rto=c") ?: return null).optString("jwt").takeIf { it.isNotBlank() } ?: return null
        val exp = runCatching {
            val p = String(Base64.decode(jwt.split('.')[1], Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP), Charsets.UTF_8)
            JSONObject(p).getLong("exp") * 1000
        }.getOrNull() ?: (System.currentTimeMillis() + 5 * 60_000L)
        dzToken = jwt; dzUntil = exp - 30_000L
        return jwt
    }

    fun deezer(title: String, artist: String, secs: Int): String? {
        val r = JSONObject(http("https://api.deezer.com/search?q=${q("$title $artist")}&limit=10") ?: return null)
        val data = r.optJSONArray("data") ?: return null
        val cands = (0 until data.length()).map { data.getJSONObject(it) }.map { t ->
            Cand(listOf(t.optString("title"), t.optString("title_short")), listOfNotNull(t.optJSONObject("artist")?.optString("name")), t.optInt("duration"), t.optLong("id"))
        }
        val id = cands.best(title, artist, secs) ?: return null
        val gql = "query { track(trackId: \"$id\") { lyrics { text synchronizedLines { milliseconds line } synchronizedWordByWordLines { start words { start end word } } } } }"
        val jwt = dzJwt() ?: return null
        val resp = http("https://pipe.deezer.com/api", JSONObject().put("query", gql).toString(), jwt)
        if (resp == null) { dzToken = null; return null }
        val ly = JSONObject(resp).optJSONObject("data")?.optJSONObject("track")?.optJSONObject("lyrics") ?: return null
        val wl = ly.optJSONArray("synchronizedWordByWordLines")
        if (wl != null && wl.length() >= 3) {
            val sb = StringBuilder()
            for (i in 0 until wl.length()) {
                val line = wl.getJSONObject(i); val ws = line.optJSONArray("words") ?: continue
                if (ws.length() == 0) continue
                sb.append('[').append(stamp(line.optLong("start"))).append(']')
                for (j in 0 until ws.length()) {
                    val w = ws.getJSONObject(j); val txt = w.optString("word").trim()
                    if (txt.isEmpty()) continue
                    sb.append('<').append(stamp(w.optLong("start"))).append('>').append(txt).append(' ')
                }
                sb.append('\n')
            }
            return sb.toString()
        }
        val sl = ly.optJSONArray("synchronizedLines")
        if (sl != null && sl.length() >= 3) {
            val sb = StringBuilder()
            for (i in 0 until sl.length()) {
                val o = sl.getJSONObject(i)
                sb.append('[').append(stamp(o.optLong("milliseconds"))).append(']').append(o.optString("line").trim()).append('\n')
            }
            return sb.toString()
        }
        return null
    }
}
