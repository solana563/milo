package com.ubayanda.milonative.data

import android.content.Context
import com.ubayanda.milonative.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Songs saved on the phone from YouTube Music so they play with no connection.
 * Files live in the app's private folder (the system can't see them, and removing the app removes them).
 * A saved song keeps its normal "yt:<id>" address; the player swaps in the saved file when there is one.
 */
object OfflineStore {
    data class Item(val id: String, val title: String, val artist: String, val album: String, val art: String, val durationMs: Long, val file: String, val bytes: Long, val at: Long)

    private var dir: File? = null
    private var appCtx: Context? = null
    private val _items = MutableStateFlow<List<Item>>(emptyList())
    val items: StateFlow<List<Item>> = _items.asStateFlow()
    /** 0..1 while a song is downloading, -1 when it failed. */
    private val _progress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val progress: StateFlow<Map<String, Float>> = _progress.asStateFlow()

    private val http = OkHttpClient.Builder().connectTimeout(12, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()
    private const val PIECE = 4L * 1024 * 1024
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO)
    private val gate = kotlinx.coroutines.sync.Semaphore(2)

    /** Queue songs to download in the background (two at a time). [done] gets how many were saved and the first failure message. */
    fun enqueue(list: List<Track>, done: (Int, String?) -> Unit = { _, _ -> }) {
        val todo = list.filter { it.uri.startsWith("yt:") && !has(it.uri.removePrefix("yt:")) }
        if (todo.isEmpty()) { done(0, null); return }
        scope.launch {
            var ok = 0; var err: String? = null
            todo.map { t -> async { gate.acquire(); try { val e = download(t); if (e == null) ok++ else if (err == null) err = e } finally { gate.release() } } }.forEach { it.await() }
            withContext(Dispatchers.Main) { done(ok, err) }
        }
    }

    @Synchronized fun init(c: Context) {
        if (dir != null) return
        appCtx = c.applicationContext
        val d = File(c.applicationContext.filesDir, "offline").also { it.mkdirs() }
        dir = d
        try {
            val a = JSONArray(File(d, "index.json").takeIf { it.exists() }?.readText() ?: "[]")
            val out = ArrayList<Item>()
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                val f = File(d, o.getString("file"))
                if (f.exists()) out.add(Item(o.getString("id"), o.getString("title"), o.optString("artist"), o.optString("album"), o.optString("art"), o.optLong("d"), o.getString("file"), f.length(), o.optLong("at")))
            }
            _items.value = out
        } catch (_: Exception) {}
    }

    private fun save() {
        val d = dir ?: return
        val a = JSONArray()
        _items.value.forEach { a.put(JSONObject().put("id", it.id).put("title", it.title).put("artist", it.artist).put("album", it.album).put("art", it.art).put("d", it.durationMs).put("file", it.file).put("at", it.at)) }
        try { File(d, "index.json").writeText(a.toString()) } catch (_: Exception) {}
    }

    fun has(videoId: String) = _items.value.any { it.id == videoId }
    fun fileOf(videoId: String): File? = _items.value.firstOrNull { it.id == videoId }?.let { dir?.let { d -> File(d, it.file) } }?.takeIf { it.exists() }
    fun totalBytes() = _items.value.sumOf { it.bytes }

    /** The saved songs as normal tracks, newest first. */
    fun tracks(): List<Track> = _items.value.sortedByDescending { it.at }.map {
        Track(
            id = YtMusic.trackId(it.id), uri = "yt:" + it.id, title = it.title, artist = it.artist, album = it.album.ifEmpty { "Downloaded" },
            albumId = 4_000_000_000_000L + (it.album.hashCode().toLong() and 0x7fffffffL), durationMs = it.durationMs, trackNo = 0, year = 0, addedSec = it.at / 1000, artUrl = it.art,
        )
    }

    fun remove(videoId: String) {
        val it = _items.value.firstOrNull { x -> x.id == videoId } ?: return
        dir?.let { d -> File(d, it.file).delete(); File(d, videoId + ".jpg").delete(); File(d, videoId + ".lrc").delete() }
        _items.value = _items.value.filter { x -> x.id != videoId }
        save()
    }

    fun clear() { _items.value.map { it.id }.forEach { remove(it) } }

    private fun setProgress(id: String, p: Float?) {
        _progress.value = if (p == null) _progress.value - id else _progress.value + (id to p)
    }

    /** Downloads one YouTube song. Returns null when saved, otherwise a message. */
    suspend fun download(t: Track): String? = withContext(Dispatchers.IO) {
        val d = dir ?: return@withContext "Not ready yet"
        val id = t.uri.removePrefix("yt:")
        if (!t.uri.startsWith("yt:") || id.isEmpty()) return@withContext "Only YouTube songs can be downloaded"
        if (has(id)) return@withContext null
        if (_progress.value.containsKey(id) && _progress.value[id]!! >= 0f) return@withContext null
        setProgress(id, 0f)
        val part = File(d, "$id.part")
        try {
            val s = YtMusic.stream(id, force = true)
            val ext = if (s.mime.contains("webm")) "webm" else if (s.mime.contains("mp4")) "m4a" else "audio"
            var start = 0L; var total = -1L
            part.delete()
            part.outputStream().use { out ->
                while (total < 0 || start < total) {
                    val end = start + PIECE - 1
                    val req = Request.Builder().url(s.url).header("User-Agent", YtMusic.STREAM_UA).header("Range", "bytes=$start-$end").build()
                    http.newCall(req).execute().use { r ->
                        if (r.code != 206 && r.code != 200) throw IOException("YouTube answered ${r.code}")
                        val cr = r.header("Content-Range")
                        if (total < 0) total = cr?.substringAfter('/')?.toLongOrNull() ?: (if (r.code == 200) r.body?.contentLength() ?: -1L else -1L)
                        val body = r.body ?: throw IOException("empty answer")
                        val n = body.byteStream().copyTo(out)
                        start += n
                        if (r.code == 200) total = start      // server ignored the range and sent everything
                        if (n <= 0L) throw IOException("stream ended early")
                    }
                    if (total > 0) setProgress(id, (start.toFloat() / total).coerceIn(0f, 0.99f))
                    if (total < 0) break
                }
            }
            val name = "$id.$ext"
            val f = File(d, name)
            f.delete(); part.renameTo(f)
            // the cover, so it still shows offline
            var art = t.artUrl
            try {
                if (art.startsWith("http")) {
                    http.newCall(Request.Builder().url(art).header("User-Agent", "milo-native/1.0").build()).execute().use { r ->
                        val b = r.body?.bytes()
                        if (r.isSuccessful && b != null && b.isNotEmpty()) { val cf = File(d, "$id.jpg"); cf.writeBytes(b); art = cf.absolutePath }
                    }
                }
            } catch (_: Exception) {}
            _items.value = _items.value.filter { it.id != id } + Item(id, t.title, t.artist, if (t.album == "YouTube Music" || t.album == "YouTube") "" else t.album, art, t.durationMs, name, f.length(), System.currentTimeMillis())
            save()
            try { appCtx?.let { com.ubayanda.milonative.data.LyricsRepo(it).fetchTo(File(d, "$id.lrc"), t) } } catch (_: Exception) {}
            setProgress(id, null)
            null
        } catch (e: kotlinx.coroutines.CancellationException) {
            part.delete(); setProgress(id, null); throw e
        } catch (e: Exception) {
            part.delete()
            com.ubayanda.milonative.util.ErrorLog.add("download $id", e.message)
            setProgress(id, -1f)
            e.message ?: "Download failed"
        }
    }
}
