package com.ubayanda.milonative.data

import android.content.Context
import com.ubayanda.milonative.model.Track
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * YouTube Music, read the way its own web client reads it: search, artist / album / playlist pages and direct audio streams.
 * A song plays in milo.'s own player (Media3), so background playback, the lock screen, crossfade, speed and the sleep timer
 * all work, and YouTube's embedded player (which refuses to start inside apps, error 153) is not needed.
 *
 * Everything here blocks, so call it from a background thread (the player's loader thread does, and the screens use Dispatchers.IO).
 * Written for milo. from how the public web API behaves; approach credited in CREDITS.md (Limusic, Metrolist).
 */
object YtMusic {
    // ---- client identities. Versions rot, so the app can be handed new ones in prefs ("ymc" JSON) without a new build ----
    private data class Client(
        val name: String, val version: String, val id: String, val ua: String,
        val os: String? = null, val osVersion: String? = null, val make: String? = null, val model: String? = null,
    )

    private var remix = Client(
        "WEB_REMIX", "1.20260213.01.00", "67",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0",
    )
    private var vision = Client(
        "VISIONOS", "0.1", "101",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15",
        "visionOS", "1.3.21O771", "Apple", "RealityDevice14,1",
    )

    const val STREAM_UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15"

    private const val BASE = "https://music.youtube.com/youtubei/v1/"
    private val F_SONG = "EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D"
    private val F_ALBUM = "EgWKAQIYAWoKEAkQChAFEAMQBA%3D%3D"
    private val F_ARTIST = "EgWKAQIgAWoKEAkQChAFEAMQBA%3D%3D"
    private val F_PLAYLIST = "EgeKAQQoAEABagoQAxAEEAoQCRAF"

    private val http = OkHttpClient.Builder().connectTimeout(12, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build()
    private val JSON = "application/json".toMediaType()
    private var prefs: android.content.SharedPreferences? = null
    @Volatile private var visitor = ""
    @Volatile private var visitorAt = 0L

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences("milo_ytm", Context.MODE_PRIVATE)
        prefs = p
        visitor = p.getString("vd", "").orEmpty()
        visitorAt = p.getLong("vdAt", 0L)
        try {
            val o = JSONObject(p.getString("ymc", "") ?: "")
            o.optJSONObject("REMIX")?.let { remix = remix.copy(version = it.optString("v", remix.version), ua = it.optString("ua", remix.ua)) }
            o.optJSONObject("VISION")?.let { vision = vision.copy(version = it.optString("v", vision.version)) }
        } catch (_: Exception) {}
    }

    // ---- transport ----
    private fun locale(): Pair<String, String> {
        val l = Locale.getDefault()
        return (l.language.ifEmpty { "en" }.lowercase()) to (l.country.ifEmpty { "US" }.uppercase())
    }

    private fun ctx(c: Client): JSONObject {
        val (hl, gl) = locale()
        val cl = JSONObject().put("clientName", c.name).put("clientVersion", c.version).put("gl", gl).put("hl", hl)
        if (c.os != null) cl.put("osName", c.os).put("osVersion", c.osVersion).put("deviceMake", c.make).put("deviceModel", c.model)
        if (visitor.isNotEmpty()) cl.put("visitorData", visitor)
        return JSONObject().put("client", cl)
            .put("request", JSONObject().put("internalExperimentFlags", JSONArray()).put("useSsl", true))
            .put("user", JSONObject().put("lockedSafetyMode", false))
    }

    private fun ensureVisitor() {
        if (visitor.isNotEmpty() && System.currentTimeMillis() - visitorAt < 6 * 3_600_000L) return
        try {
            val req = Request.Builder().url("https://music.youtube.com/sw.js_data").header("User-Agent", remix.ua).header("Accept-Language", "en-US,en;q=0.9").build()
            val txt = http.newCall(req).execute().use { it.body?.string().orEmpty() }
            val i = txt.indexOf('[')
            if (i < 0) return
            val arr = JSONArray(txt.substring(i)).optJSONArray(0)?.optJSONArray(2) ?: return
            for (k in 0 until arr.length()) {
                val s = arr.optString(k)
                if (s.startsWith("Cgt") || s.startsWith("Cgs")) {
                    visitor = s; visitorAt = System.currentTimeMillis()
                    prefs?.edit()?.putString("vd", s)?.putLong("vdAt", visitorAt)?.apply()
                    return
                }
            }
        } catch (_: Exception) {}
    }

    private fun post(path: String, body: JSONObject, c: Client): JSONObject {
        ensureVisitor()
        body.put("context", ctx(c))
        val (hl, _) = locale()
        val rb = Request.Builder().url(BASE + path + "?prettyPrint=false")
            .header("Content-Type", "application/json").header("Accept", "application/json")
            .header("X-Goog-Api-Format-Version", "1")
            .header("X-YouTube-Client-Name", c.id).header("X-YouTube-Client-Version", c.version)
            .header("X-Origin", "https://music.youtube.com").header("Referer", "https://music.youtube.com/")
            .header("User-Agent", c.ua)
            .header("Accept-Language", if (hl == "en") "en-US,en;q=0.9" else "$hl,en;q=0.5")
        if (visitor.isNotEmpty()) rb.header("X-Goog-Visitor-Id", visitor)
        val req = rb.post(body.toString().toRequestBody(JSON)).build()
        http.newCall(req).execute().use { r ->
            val tx = r.body?.string().orEmpty()
            if (!r.isSuccessful) throw IllegalStateException("YouTube answered ${r.code}")
            return JSONObject(tx)
        }
    }

    // ---- reading the answers ----
    private fun findAll(n: Any?, key: String, out: MutableList<Any?> = ArrayList()): MutableList<Any?> {
        when (n) {
            is JSONObject -> {
                val it = n.keys()
                while (it.hasNext()) {
                    val k = it.next()
                    val v = n.opt(k)
                    if (k == key) out.add(v)
                    findAll(v, key, out)
                }
            }
            is JSONArray -> for (i in 0 until n.length()) findAll(n.opt(i), key, out)
        }
        return out
    }

    private fun first(n: Any?, key: String): JSONObject? = findAll(n, key).firstOrNull { it is JSONObject } as? JSONObject

    private fun runs(v: JSONObject?): String {
        val a = v?.optJSONArray("runs") ?: return ""
        val sb = StringBuilder()
        for (i in 0 until a.length()) sb.append(a.optJSONObject(i)?.optString("text").orEmpty())
        return sb.toString()
    }

    private fun thumbOf(n: Any?): String {
        for (t in findAll(n, "thumbnails")) {
            if (t is JSONArray && t.length() > 0) return t.optJSONObject(t.length() - 1)?.optString("url").orEmpty()
        }
        return ""
    }

    private fun durSec(s: String): Int {
        val p = s.trim().split(":")
        if (p.isEmpty() || p.any { it.toIntOrNull() == null }) return 0
        return p.fold(0) { a, x -> a * 60 + x.toInt() }
    }

    private val DUR = Regex("^\\d+(:\\d{2}){1,2}$")
    private val TYPES = Regex("^(song|video|album|single|ep|playlist|artist|podcast|episode|profile|station)$", RegexOption.IGNORE_CASE)

    private class Group(var text: String = "", var artist: String = "", var album: String = "")

    private fun groups(a: JSONArray?): MutableList<Group> {
        val out = ArrayList<Group>()
        var cur = Group()
        if (a != null) for (i in 0 until a.length()) {
            val r = a.optJSONObject(i) ?: continue
            val t = r.optString("text")
            if (t.trim() == "•") { out.add(cur); cur = Group() }
            else {
                cur.text += t
                val b = r.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")
                val id = b?.optString("browseId").orEmpty()
                if (id.startsWith("UC") && cur.artist.isEmpty()) cur.artist = id
                if (id.startsWith("MPRE")) cur.album = id
            }
        }
        out.add(cur)
        out.forEach { it.text = it.text.trim() }
        return out
    }

    // ---- what the screens get ----
    data class Song(
        val id: String, val title: String, val artist: String, val album: String, val albumId: String,
        val artistId: String, val durationSec: Int, val thumb: String,
    )

    /** An artist, album or playlist (or song) card. */
    data class Shelf(val title: String, val cards: List<Card>)
    data class Card(val kind: String, val id: String, val title: String, val sub: String, val thumb: String)

    data class Page(
        val kind: String, val id: String, val title: String, val subtitle: String, val description: String,
        val cover: String, val banner: String, val avatar: String, val songs: List<Song>, val sections: List<Pair<String, List<Card>>>,
        val radioId: String, val isCollage: Boolean,
    )

    data class Stream(val url: String, val mime: String, val expiresAt: Long, val durationSec: Int, val loudnessDb: Double = 0.0, val title: String = "", val author: String = "")

    private fun songOf(n: JSONObject): Song? {
        val cols = n.optJSONArray("flexColumns")
        fun runsAt(i: Int): JSONArray? = cols?.optJSONObject(i)?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")?.optJSONObject("text")?.optJSONArray("runs")
        val vid = n.optJSONObject("playlistItemData")?.optString("videoId").orEmpty().ifEmpty {
            n.optJSONObject("navigationEndpoint")?.optJSONObject("watchEndpoint")?.optString("videoId").orEmpty()
        }.ifEmpty { (findAll(n.opt("overlay"), "videoId").firstOrNull { it is String } as? String).orEmpty() }
        val title = run { val a = runsAt(0); val sb = StringBuilder(); if (a != null) for (i in 0 until a.length()) sb.append(a.optJSONObject(i)?.optString("text").orEmpty()); sb.toString() }
        if (vid.isEmpty() || title.isEmpty()) return null
        var g = groups(runsAt(1))
        if (g.size > 1 && g[0].artist.isEmpty() && (TYPES.matches(g[0].text) || g.drop(1).any { it.artist.isNotEmpty() } || (g.size == 2 && DUR.matches(g[1].text)))) g = g.drop(1).toMutableList()
        var dur = g.lastOrNull { DUR.matches(it.text) }?.text.orEmpty()
        if (dur.isEmpty()) {
            val fc = n.optJSONArray("fixedColumns")
            if (fc != null) for (i in 0 until fc.length()) {
                val t = runs(fc.optJSONObject(i)?.optJSONObject("musicResponsiveListItemFixedColumnRenderer")?.optJSONObject("text"))
                if (DUR.matches(t)) { dur = t; break }
            }
        }
        val artist = g.firstOrNull()?.text?.takeIf { !DUR.matches(it) }.orEmpty()
        val albG = g.drop(1).firstOrNull { it.album.isNotEmpty() }
        var album = albG?.text.orEmpty()
        var albumId = albG?.album.orEmpty()
        if (albumId.isEmpty()) albumId = (findAll(n, "browseId").firstOrNull { it is String && it.startsWith("MPRE") } as? String).orEmpty()
        if (album.isEmpty() && cols != null) for (i in 2 until cols.length()) {
            val a = runsAt(i) ?: continue
            for (k in 0 until a.length()) {
                val r = a.optJSONObject(k) ?: continue
                val id = r.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")?.optString("browseId").orEmpty()
                if (id.startsWith("MPRE")) { album = r.optString("text"); if (albumId.isEmpty()) albumId = id; break }
            }
            if (album.isNotEmpty()) break
        }
        val arId = groups(runsAt(1)).firstOrNull { it.artist.isNotEmpty() }?.artist.orEmpty()
        return Song(vid, title, artist, album, albumId, arId, durSec(dur), thumbOf(n))
    }

    private fun songs(root: Any?): List<Song> = findAll(root, "musicResponsiveListItemRenderer").mapNotNull { (it as? JSONObject)?.let(::songOf) }

    private fun kindOf(id: String) = when {
        id.startsWith("UC") -> "artist"
        id.startsWith("MPRE") -> "album"
        id.startsWith("VL") || id.startsWith("PL") || id.startsWith("RD") || id.startsWith("OLAK") -> "playlist"
        else -> "other"
    }

    private fun browseIdOf(n: JSONObject): String = n.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")?.optString("browseId").orEmpty()

    private fun cardOfRow(n: JSONObject): Card? {
        val id = browseIdOf(n)
        val cols = n.optJSONArray("flexColumns") ?: return null
        fun runsAt(i: Int) = cols.optJSONObject(i)?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")?.optJSONObject("text")
        val title = runs(runsAt(0))
        if (id.isEmpty() || title.isEmpty()) return null
        var g = groups(runsAt(1)?.optJSONArray("runs"))
        if (g.size > 1 && TYPES.matches(g[0].text)) g = g.drop(1).toMutableList()
        return Card(kindOf(id), id, title, g.map { it.text }.filter { it.isNotEmpty() }.joinToString(" · "), thumbOf(n))
    }

    private fun cardOfTwoRow(n: JSONObject): Card? {
        val title = runs(n.optJSONObject("title"))
        if (title.isEmpty()) return null
        val sub = runs(n.optJSONObject("subtitle"))
        val img = thumbOf(n)
        val nv = n.optJSONObject("navigationEndpoint")
        val vid = nv?.optJSONObject("watchEndpoint")?.optString("videoId").orEmpty()
        if (vid.isNotEmpty()) {
            val g = groups(n.optJSONObject("subtitle")?.optJSONArray("runs"))
            val a = g.firstOrNull { it.artist.isNotEmpty() } ?: g.firstOrNull()
            val who = if (a != null && TYPES.matches(a.text) && g.size > 1) g[1].text else a?.text.orEmpty()
            return Card("song", vid, title, who, img)
        }
        val pl = nv?.optJSONObject("watchPlaylistEndpoint")?.optString("playlistId").orEmpty()
        if (pl.isNotEmpty()) return Card("playlist", "VL$pl", title, sub, img)
        val id = browseIdOf(n)
        if (id.isEmpty()) return null
        return Card(kindOf(id), id, title, sub, img)
    }

    // ---- artwork: ask for the size a slot really shows ----
    /** Google image URLs carry their size (=w544-h544 / =s400); a video thumbnail has a bigger file by name. */
    fun art(url: String, px: Int): String {
        if (url.isEmpty()) return ""
        val v = Regex("^https://i\\.ytimg\\.com/vi/([\\w-]{11})/").find(url)
        if (v != null && px >= 720) return "https://i.ytimg.com/vi/${v.groupValues[1]}/maxresdefault.jpg"
        if (Regex("=w\\d+-h\\d+").containsMatchIn(url)) return url.replace(Regex("=w\\d+-h\\d+"), "=w$px-h$px")
        if (Regex("=s\\d+").containsMatchIn(url)) return url.replace(Regex("=s\\d+"), "=s$px")
        return url
    }

    /** YouTube's automatic 2x2 collage of a playlist's first four songs: not a cover anybody chose. */
    private val COLLAGE = Regex("yt3\\.(ggpht|googleusercontent)\\.com/.*=s\\d+")

    // ---- search ----
    fun searchSongs(q: String): List<Song> = songs(post("search", JSONObject().put("query", q).put("params", java.net.URLDecoder.decode(F_SONG, "UTF-8")), remix))

    fun searchCards(q: String, kind: String): List<Card> {
        val f = when (kind) { "artist" -> F_ARTIST; "album" -> F_ALBUM; else -> F_PLAYLIST }
        val j = post("search", JSONObject().put("query", q).put("params", java.net.URLDecoder.decode(f, "UTF-8")), remix)
        return findAll(j, "musicResponsiveListItemRenderer").mapNotNull { (it as? JSONObject)?.let(::cardOfRow) }.filter { it.kind == kind }
    }

    // ---- pages ----
    fun page(id: String): Page {
        val j = post("browse", JSONObject().put("browseId", id), remix)
        val kind = kindOf(id)
        val h = first(j, "musicImmersiveHeaderRenderer") ?: first(j, "musicResponsiveHeaderRenderer") ?: first(j, "musicDetailHeaderRenderer")
            ?: first(j, "musicEditablePlaylistDetailHeaderRenderer") ?: first(j, "musicVisualHeaderRenderer") ?: first(j, "musicHeaderRenderer") ?: JSONObject()
        val title = runs(h.optJSONObject("title"))
        val desc = runs(h.optJSONObject("description"))
        if (kind == "artist") {
            var top: List<Song> = emptyList()
            val secs = ArrayList<Pair<String, List<Card>>>()
            val list = findAll(j, "sectionListRenderer").mapNotNull { (it as? JSONObject)?.optJSONArray("contents") }.firstOrNull()
            if (list != null) for (i in 0 until list.length()) {
                val n = list.optJSONObject(i) ?: continue
                n.optJSONObject("musicShelfRenderer")?.let { if (top.isEmpty()) top = songs(it) }
                n.optJSONObject("musicCarouselShelfRenderer")?.let { c ->
                    val hd = first(c.optJSONObject("header"), "title")
                    val items = ArrayList<Card>()
                    val cs = c.optJSONArray("contents")
                    if (cs != null) for (k in 0 until cs.length()) {
                        val x = cs.optJSONObject(k) ?: continue
                        x.optJSONObject("musicTwoRowItemRenderer")?.let { cardOfTwoRow(it) }?.let(items::add)
                        x.optJSONObject("musicResponsiveListItemRenderer")?.let { songOf(it) }?.let { items.add(Card("song", it.id, it.title, it.artist, it.thumb)) }
                    }
                    if (items.isNotEmpty()) secs.add((runs(hd).ifEmpty { "More" }) to items)
                }
            }
            val sb = first(h, "subscribeButtonRenderer")
            val subs = runs(sb?.optJSONObject("longSubscriberCountText")).ifEmpty { runs(sb?.optJSONObject("subscriberCountText")) }.ifEmpty { runs(h.optJSONObject("monthlyListenerCount")) }
            val radio = findAll(h, "playlistId").firstOrNull { it is String && it.startsWith("RD") } as? String ?: ""
            return Page(
                "artist", id, title.ifEmpty { "Artist" }, subs, desc, "", thumbOf(h.opt("thumbnail") ?: h),
                thumbOf(h.opt("foregroundThumbnail")), if (top.isEmpty()) songs(j).take(10) else top, secs, radio, false,
            )
        }
        val cover = thumbOf(h.opt("thumbnail") ?: h.opt("thumbnails") ?: h)
        val sg = groups(h.optJSONObject("subtitle")?.optJSONArray("runs")).map { it.text }.filter { it.isNotEmpty() }
        val strap = runs(h.optJSONObject("straplineTextOne"))
        val year = sg.firstOrNull { Regex("^(19|20)\\d{2}$").matches(it) }.orEmpty()
        val artist = strap.ifEmpty { sg.firstOrNull { !TYPES.matches(it) && !Regex("^(19|20)\\d{2}$").matches(it) && !Regex("(?i)song|minute|hour").containsMatchIn(it) }.orEmpty() }
        val meta = runs(h.optJSONObject("secondSubtitle")).ifEmpty { sg.filter { Regex("(?i)song|minute|hour|view|track").containsMatchIn(it) }.joinToString(" · ") }
        val label = if (sg.firstOrNull()?.let { TYPES.matches(it) } == true) sg.first() else if (kind == "album") "Album" else "Playlist"
        val subtitle = listOf(label, artist, year).filter { it.isNotEmpty() }.joinToString(" · ")
        return Page(kind, id, title.ifEmpty { if (kind == "album") "Album" else "Playlist" }, subtitle.ifEmpty { meta }, desc, cover, "", "", songs(j), emptyList(), "", COLLAGE.containsMatchIn(cover))
    }

    /** An artist's radio: songs YouTube picks to follow on from the artist. */
    fun radio(playlistId: String): List<Song> {
        val j = post("next", JSONObject().put("playlistId", playlistId).put("isAudioOnly", true), remix)
        val out = ArrayList<Song>()
        for (n in findAll(j, "playlistPanelVideoRenderer")) {
            val o = n as? JSONObject ?: continue
            val vid = o.optString("videoId")
            if (vid.isEmpty()) continue
            val by = runs(o.optJSONObject("longBylineText") ?: o.optJSONObject("shortBylineText")).split(" • ")
            out.add(Song(vid, runs(o.optJSONObject("title")), by.firstOrNull().orEmpty(), "", "", "", durSec(runs(o.optJSONObject("lengthText"))), thumbOf(o)))
        }
        return out
    }

    // ---- direct audio streams ----
    private val streams = ConcurrentHashMap<String, Stream>()
    private val rank = mapOf("AUDIO_QUALITY_HIGH" to 3, "AUDIO_QUALITY_MEDIUM" to 2, "AUDIO_QUALITY_LOW" to 1)

    private fun pick(sd: JSONObject): JSONObject? {
        fun list(key: String): List<JSONObject> {
            val a = sd.optJSONArray(key) ?: return emptyList()
            return (0 until a.length()).mapNotNull { a.optJSONObject(it) }.filter { !it.has("width") && it.has("url") && !it.has("signatureCipher") && !it.has("cipher") }
        }
        var a = list("adaptiveFormats").ifEmpty { list("formats") }
        if (a.isEmpty()) return null
        val orig = a.filter { f -> val t = f.optJSONObject("audioTrack"); t == null || (!t.optBoolean("isAutoDubbed", false) && t.optBoolean("audioIsDefault", false)) }
        if (orig.isNotEmpty()) a = orig
        // ExoPlayer plays Opus (WebM) and AAC (MP4); prefer Opus, then the higher bitrate
        fun score(f: JSONObject): Long {
            val m = f.optString("mimeType")
            val codec = if (m.contains("opus")) 2L else if (m.contains("mp4a")) 1L else 0L
            return (rank[f.optString("audioQuality")] ?: 0) * 1_000_000_000_000L + f.optInt("audioChannels", 2) * 100_000_000_000L + codec * 10_000_000_000L + f.optLong("bitrate", 0L).coerceAtMost(9_999_999_999L)
        }
        return a.maxByOrNull(::score)
    }

    fun stream(videoId: String, force: Boolean = false): Stream {
        streams[videoId]?.let { if (!force && it.expiresAt > System.currentTimeMillis() + 90_000L) return it }
        val j = post("player", JSONObject().put("videoId", videoId).put("contentCheckOk", true).put("racyCheckOk", true), vision)
        val ps = j.optJSONObject("playabilityStatus")
        val status = ps?.optString("status").orEmpty()
        if (status.isNotEmpty() && status != "OK") throw java.io.IOException(ps?.optString("reason").orEmpty().ifEmpty { status })
        val sd = j.optJSONObject("streamingData") ?: throw java.io.IOException("no stream data")
        val f = pick(sd) ?: throw java.io.IOException("no direct audio stream")
        val secs = sd.optString("expiresInSeconds").toLongOrNull() ?: 18_000L
        val s = Stream(f.getString("url"), f.optString("mimeType"), System.currentTimeMillis() + secs * 1000L, j.optJSONObject("videoDetails")?.optString("lengthSeconds")?.toIntOrNull() ?: 0, j.optJSONObject("playerConfig")?.optJSONObject("audioConfig")?.optDouble("loudnessDb", 0.0) ?: 0.0, j.optJSONObject("videoDetails")?.optString("title").orEmpty(), j.optJSONObject("videoDetails")?.optString("author").orEmpty().removeSuffix(" - Topic"))
        streams[videoId] = s
        return s
    }

    fun forget(videoId: String) { streams.remove(videoId) }

    /** How much louder than YouTube's reference this song is (dB, 0 when unknown). Used to even out volume between songs. */
    fun loudness(videoId: String): Double = streams[videoId]?.loudnessDb ?: 0.0

    /** Look the stream up ahead of time so the next song starts without a pause. */
    fun warm(videoId: String) { try { stream(videoId) } catch (_: Exception) {} }

    // ---- into the app's own model ----
    private fun hashId(s: String): Long = (s.hashCode().toLong() and 0x7fffffffL)

    /** Tracks use negative ids so they never clash with MediaStore songs. */
    fun trackId(videoId: String): Long = -(1L + hashId("yt:$videoId"))

    fun toTrack(s: Song, coverFallback: String = ""): Track {
        val id = trackId(s.id)
        val cover = art(s.thumb.ifEmpty { coverFallback }, 544)
        // songs of one album share a cover, so give them one album id and the cover is loaded once
        val aid = 4_000_000_000_000L + (if (s.albumId.isNotEmpty()) hashId(s.albumId) else -id)
        return Track(
            id = id, uri = "yt:${s.id}", title = s.title, artist = s.artist.ifEmpty { "YouTube Music" },
            album = s.album.ifEmpty { "YouTube Music" }, albumId = aid, durationMs = if (s.durationSec > 0) s.durationSec * 1000L else 1L,
            trackNo = 0, year = 0, addedSec = 0L, artUrl = cover,
        )
    }

    /** A card as a stand-in "track" so the existing cover loader can draw its picture. */
    fun cardTrack(c: Card, px: Int = 400): Track {
        val id = -(1L + hashId("card:" + c.id))
        return Track(id = id, uri = "", title = c.title, artist = c.sub, album = "", albumId = 4_000_000_000_000L + hashId("c:" + c.id), durationMs = 0L, trackNo = 0, year = 0, addedSec = 0L, artUrl = art(c.thumb, px))
    }

    // ---- home: recommended playlists, trending artists, charts ----
    private fun shelvesOf(j: JSONObject): List<Shelf> {
        val out = ArrayList<Shelf>()
        for (key in listOf("musicCarouselShelfRenderer", "musicImmersiveCarouselShelfRenderer")) {
            for (n in findAll(j, key)) {
                if (n !is JSONObject) continue
                val title = runs(first(n.optJSONObject("header"), "title"))
                if (title.isEmpty()) continue
                val cards = ArrayList<Card>()
                val cs = n.optJSONArray("contents") ?: continue
                for (i in 0 until cs.length()) {
                    val c = cs.optJSONObject(i) ?: continue
                    c.optJSONObject("musicTwoRowItemRenderer")?.let { r -> cardOfTwoRow(r)?.let { cards.add(it) } }
                    c.optJSONObject("musicResponsiveListItemRenderer")?.let { r ->
                        val sg = songOf(r)
                        if (sg != null) cards.add(Card("song", sg.id, sg.title, sg.artist, sg.thumb)) else cardOfRow(r)?.let { cards.add(it) }
                    }
                }
                if (cards.isNotEmpty()) out.add(Shelf(title, cards.take(14)))
            }
        }
        return out
    }

    private fun browseShelves(id: String): List<Shelf> {
        return try {
            var j = post("browse", JSONObject().put("browseId", id), remix)
            val all = ArrayList(shelvesOf(j))
            repeat(2) {
                val tok = (findAll(j, "nextContinuationData").firstOrNull { it is JSONObject } as? JSONObject)?.optString("continuation").orEmpty()
                if (tok.isEmpty()) return@repeat
                try { j = post("browse", JSONObject().put("continuation", tok), remix); all.addAll(shelvesOf(j)) } catch (_: Exception) { return all }
            }
            all
        } catch (_: Exception) { emptyList() }
    }

    /** What the Home screen shows below your own music while you are online. */
    fun home(): List<Shelf> {
        val seen = HashSet<String>()
        return (browseShelves("FEmusic_home") + browseShelves("FEmusic_charts")).filter { seen.add(it.title) }.take(10)
    }
}
