package com.ubayanda.milonative.data

import android.content.Context
import com.ubayanda.milonative.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlinx.coroutines.Job as CoJob
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

    /** Songs still waiting or downloading, for the "Downloading…" bar. */
    private val _pending = MutableStateFlow(0)
    val pending: StateFlow<Int> = _pending.asStateFlow()

    private val http = OkHttpClient.Builder().connectTimeout(12, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()
    private const val PIECE = 4L * 1024 * 1024
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO)
    private val gate = kotlinx.coroutines.sync.Semaphore(2)

    // ---------------- download manager (queue, pause, retry, limits) ----------------
    enum class State { QUEUED, ACTIVE, PAUSED, FAILED }
    data class Job(
        val id: String, val track: Track, val state: State, val error: String = "", val attempts: Int = 0,
        val doneBytes: Long = 0L, val totalBytes: Long = 0L, val notBefore: Long = 0L, val source: String = "YouTube Music",
    ) {
        /** Rough size until the real one is known: about 20 KB per second of music (about 6 with the data saver). */
        fun estimate(saver: Boolean): Long = if (totalBytes > 0) totalBytes else (track.durationMs / 1000L) * (if (saver) 6_000L else 20_000L)
    }
    data class Settings(val wifiOnly: Boolean = false, val limitMb: Int = 0, val autoClean: Boolean = false, val saver: Boolean = false)

    private val _jobs = MutableStateFlow<List<Job>>(emptyList())
    val jobs: StateFlow<List<Job>> = _jobs.asStateFlow()
    private val _settings = MutableStateFlow(Settings())
    val settings: StateFlow<Settings> = _settings.asStateFlow()
    /** Why nothing is moving right now ("Waiting for Wi-Fi"), empty when the queue is free to run. */
    private val _waiting = MutableStateFlow("")
    val waiting: StateFlow<String> = _waiting.asStateFlow()

    private class Batch(val done: (Int, String?) -> Unit) { var left = 0; var ok = 0; var err: String? = null }
    private val batches = HashMap<String, Batch>()
    private val running = HashMap<String, CoJob>()
    private val keepPart = HashSet<String>()
    private class Fatal(msg: String) : IOException(msg)
    private const val MAX_TRIES = 3
    private const val MAX_PARALLEL = 2

    private fun updateJob(id: String, f: (Job) -> Job) { _jobs.update { l -> l.map { if (it.id == id) f(it) else it } } }
    private fun refreshPending() {
        _pending.value = _jobs.value.count { it.state == State.QUEUED || it.state == State.ACTIVE }
        // keep the app alive while there is work, so downloads continue with the app closed (the service stops itself when the queue is empty)
        if (_pending.value > 0) appCtx?.let { DownloadService.start(it) }
    }

    /** Queue songs to download in the background (two at a time). [done] gets how many were saved and the first failure message. */
    @Synchronized fun enqueue(list: List<Track>, done: (Int, String?) -> Unit = { _, _ -> }) {
        val known = _jobs.value.map { it.id }.toSet()
        val todo = list.filter { it.uri.startsWith("yt:") && !has(it.uri.removePrefix("yt:")) && it.uri.removePrefix("yt:") !in known }
        if (todo.isEmpty()) { done(0, null); return }
        val b = Batch(done).also { it.left = todo.size }
        todo.forEach { batches[it.uri.removePrefix("yt:")] = b }
        _jobs.update { l -> l + todo.map { Job(it.uri.removePrefix("yt:"), it, State.QUEUED) } }
        saveQueue(); refreshPending(); pump()
    }

    fun setSettings(s: Settings) {
        _settings.value = s
        appCtx?.getSharedPreferences("milo_dl", Context.MODE_PRIVATE)?.edit()
            ?.putBoolean("wifi", s.wifiOnly)?.putInt("limit", s.limitMb)?.putBoolean("clean", s.autoClean)?.putBoolean("saver", s.saver)?.apply()
        pump()
    }

    private fun networkState(): String {
        val c = appCtx ?: return ""
        val cm = c.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager ?: return ""
        return try {
            if (cm.activeNetwork == null) "Waiting for a connection"
            else if (_settings.value.wifiOnly && cm.isActiveNetworkMetered) "Waiting for Wi-Fi"
            else ""
        } catch (_: Exception) { "" }
    }

    /** Starts as many queued songs as there are free slots, unless the network says wait. */
    @Synchronized private fun pump() {
        if (appCtx == null) return
        val now = System.currentTimeMillis()
        val hasQueued = _jobs.value.any { it.state == State.QUEUED }
        val why = networkState()
        _waiting.value = if (why.isNotEmpty() && hasQueued) why else ""
        refreshPending()
        if (why.isNotEmpty()) return
        while (running.size < MAX_PARALLEL) {
            val next = _jobs.value.firstOrNull { it.state == State.QUEUED && it.notBefore <= now } ?: break
            start(next)
        }
    }

    private fun start(j: Job) {
        updateJob(j.id) { it.copy(state = State.ACTIVE, error = "") }
        refreshPending()
        running[j.id] = scope.launch {
            var err: Throwable? = null
            try { run(j.track) } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Throwable) { err = e }
            finish(j.id, err)
        }
    }

    @Synchronized private fun finish(id: String, err: Throwable?) {
        running.remove(id)
        val job = _jobs.value.firstOrNull { it.id == id }
        if (job == null) { pump(); return }
        if (err == null) {
            _jobs.update { l -> l.filter { it.id != id } }
            settle(id, true, null)
        } else {
            val msg = err.message ?: "Download failed"
            val n = job.attempts + 1
            if (err !is Fatal && n < MAX_TRIES) {
                // automatic retry with a growing wait: 5 s, then 20 s
                val wait = 5_000L * n * n
                updateJob(id) { it.copy(state = State.QUEUED, attempts = n, error = "Retrying… ($msg)", notBefore = System.currentTimeMillis() + wait) }
                setProgress(id, null)
                scope.launch { kotlinx.coroutines.delay(wait + 200); pump() }
            } else {
                updateJob(id) { it.copy(state = State.FAILED, attempts = n, error = msg) }
                setProgress(id, -1f)
                settle(id, false, msg)
            }
        }
        saveQueue(); refreshPending(); pump()
    }

    private fun settle(id: String, ok: Boolean, err: String?) {
        val b = batches.remove(id) ?: return
        if (ok) b.ok++ else if (b.err == null) b.err = err
        b.left--
        if (b.left <= 0) scope.launch { withContext(Dispatchers.Main) { b.done(b.ok, b.err) } }
    }

    /** Pause one song. A song already part-way keeps what it has and carries on from there when resumed. */
    @Synchronized fun pause(id: String) {
        val j = _jobs.value.firstOrNull { it.id == id } ?: return
        if (j.state == State.ACTIVE) { keepPart.add(id); running.remove(id)?.cancel() }
        if (j.state == State.ACTIVE || j.state == State.QUEUED) updateJob(id) { it.copy(state = State.PAUSED, error = "") }
        refreshPending(); saveQueue(); pump()
    }
    @Synchronized fun resume(id: String) {
        val j = _jobs.value.firstOrNull { it.id == id } ?: return
        if (j.state == State.PAUSED || j.state == State.FAILED) { updateJob(id) { it.copy(state = State.QUEUED, error = "", attempts = 0, notBefore = 0L) }; setProgress(id, null) }
        refreshPending(); saveQueue(); pump()
    }
    fun retry(id: String) = resume(id)
    fun pauseAll() { _jobs.value.filter { it.state == State.ACTIVE || it.state == State.QUEUED }.forEach { pause(it.id) } }
    fun resumeAll() { _jobs.value.filter { it.state == State.PAUSED }.forEach { resume(it.id) } }
    fun retryAllFailed() { _jobs.value.filter { it.state == State.FAILED }.forEach { resume(it.id) } }
    /** Move this song to the front of the queue. */
    @Synchronized fun prioritise(id: String) {
        val j = _jobs.value.firstOrNull { it.id == id } ?: return
        _jobs.update { l -> listOf(j) + l.filter { it.id != id } }
        if (j.state == State.PAUSED || j.state == State.FAILED) resume(id) else { saveQueue(); pump() }
    }
    /** Remove from the queue (a half-finished file is deleted too). */
    @Synchronized fun cancel(id: String) {
        running.remove(id)?.cancel()
        _jobs.update { l -> l.filter { it.id != id } }
        dir?.let { File(it, "$id.part").delete(); File(it, "$id.fmt").delete() }
        setProgress(id, null); settle(id, false, "Cancelled")
        refreshPending(); saveQueue(); pump()
    }
    fun clearFailed() { _jobs.value.filter { it.state == State.FAILED }.forEach { cancel(it.id) } }

    private fun saveQueue() {
        val d = dir ?: return
        val a = JSONArray()
        _jobs.value.forEach { a.put(JSONObject().put("t", SavedStore.toJson(it.track)).put("s", it.state.name).put("e", it.error)) }
        try { File(d, "queue.json").writeText(a.toString()) } catch (_: Exception) {}
    }

    /** Songs that were still queued when the app closed come back paused, so nothing starts by surprise. */
    private fun loadQueue(d: File) {
        try {
            val f = File(d, "queue.json"); if (!f.exists()) return
            val a = JSONArray(f.readText())
            val out = ArrayList<Job>()
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                val t = SavedStore.fromJson(o.getJSONObject("t")); val id = t.uri.removePrefix("yt:")
                if (id.isEmpty() || has(id)) continue
                val failed = o.optString("s") == State.FAILED.name
                out.add(Job(id, t, if (failed) State.FAILED else State.PAUSED, if (failed) o.optString("e") else ""))
            }
            _jobs.value = out
        } catch (_: Exception) {}
    }

    /** Drop the oldest saved songs until the saved total is at most [target] bytes. */
    private fun evictTo(target: Long, keep: String? = null) {
        for (old in _items.value.filter { x -> x.id != keep }.sortedBy { x -> x.at }) {
            if (totalBytes() <= target) break
            remove(old.id)
        }
    }

    /** Disk space used by files that are not finished songs (half-finished downloads, stray covers). */
    fun leftoverBytes(): Long = dir?.listFiles()?.filter { it.name.endsWith(".part") }?.sumOf { it.length() } ?: 0L
    fun clearLeftovers() { dir?.listFiles()?.filter { it.name.endsWith(".part") && it.name.removeSuffix(".part") !in running.keys }?.forEach { it.delete() } }
    fun freeBytes(): Long = dir?.usableSpace ?: 0L

    @Synchronized fun init(c: Context) {
        if (dir != null) return
        appCtx = c.applicationContext
        val d = File(c.applicationContext.filesDir, "offline").also { it.mkdirs() }
        dir = d
        c.applicationContext.getSharedPreferences("milo_dl", Context.MODE_PRIVATE).let { p ->
            _settings.value = Settings(p.getBoolean("wifi", false), p.getInt("limit", 0), p.getBoolean("clean", false), p.getBoolean("saver", false))
        }
        try {
            val cm = c.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            cm.registerDefaultNetworkCallback(object : android.net.ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: android.net.Network) { pump() }
                override fun onCapabilitiesChanged(network: android.net.Network, nc: android.net.NetworkCapabilities) { pump() }
                override fun onLost(network: android.net.Network) { pump() }
            })
        } catch (_: Exception) {}
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
        loadQueue(d)
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
        dir?.let { d -> File(d, it.file).delete(); File(d, videoId + ".jpg").delete(); File(d, videoId + ".lrc").delete(); File(d, videoId + ".fmt").delete() }
        _items.value = _items.value.filter { x -> x.id != videoId }
        save()
    }

    fun clear() { _items.value.map { it.id }.forEach { remove(it) } }

    private fun setProgress(id: String, p: Float?) {
        _progress.value = if (p == null) _progress.value - id else _progress.value + (id to p)
    }

    /** Downloads one YouTube song right now (outside the queue). Returns null when saved, otherwise a message. */
    suspend fun download(t: Track): String? = try { run(t); null } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { e.message ?: "Download failed" }

    /** The actual download. Throws on failure ([Fatal] means retrying won't help). Keeps a half-finished file when paused. */
    private suspend fun run(t: Track): Unit = withContext(Dispatchers.IO) {
        val d = dir ?: throw Fatal("Not ready yet")
        val id = t.uri.removePrefix("yt:")
        if (!t.uri.startsWith("yt:") || id.isEmpty()) throw Fatal("Only YouTube songs can be downloaded")
        if (has(id)) return@withContext
        val st = _settings.value
        val limit = st.limitMb * 1_048_576L
        if (limit > 0 && totalBytes() >= limit) {
            if (st.autoClean) evictTo((limit - 12L * 1_048_576L).coerceAtLeast(0L))
            else throw Fatal("Download limit of ${st.limitMb} MB reached. Raise it or remove songs.")
        }
        if (d.usableSpace < 80L * 1_048_576L) throw Fatal("Not enough free space on the phone.")
        setProgress(id, 0f)
        val part = File(d, "$id.part")
        try {
            val s = YtMusic.stream(id, force = true, low = st.saver)
            val ext = if (s.mime.contains("webm")) "webm" else if (s.mime.contains("mp4")) "m4a" else "audio"
            var start = 0L; var total = -1L
            // carry on from a half-finished file when there is one; a different quality or format starts again
            val resumeAt = if (part.exists() && File(d, "$id.fmt").takeIf { it.exists() }?.readText() == s.mime + st.saver) part.length() else 0L
            if (resumeAt <= 0L) part.delete()
            File(d, "$id.fmt").writeText(s.mime + st.saver)
            start = resumeAt
            java.io.FileOutputStream(part, resumeAt > 0L).use { out ->
                while (total < 0 || start < total) {
                    val end = start + PIECE - 1
                    val req = Request.Builder().url(s.url).header("User-Agent", YtMusic.STREAM_UA).header("Range", "bytes=$start-$end").build()
                    http.newCall(req).execute().use { r ->
                        if (r.code == 416 && total < 0) { start = 0L; throw IOException("Resume point rejected, will start again") }
                        if (r.code != 206 && r.code != 200) throw IOException("YouTube answered ${r.code}")
                        val cr = r.header("Content-Range")
                        if (total < 0) total = cr?.substringAfter('/')?.toLongOrNull() ?: (if (r.code == 200) r.body?.contentLength() ?: -1L else -1L)
                        val body = r.body ?: throw IOException("empty answer")
                        val n = body.byteStream().copyTo(out)
                        start += n
                        if (r.code == 200) total = start      // server ignored the range and sent everything
                        if (n <= 0L) throw IOException("stream ended early")
                    }
                    if (total > 0) { setProgress(id, (start.toFloat() / total).coerceIn(0f, 0.99f)); updateJob(id) { it.copy(doneBytes = start, totalBytes = total) } }
                    if (total < 0) break
                }
            }
            val name = "$id.$ext"
            val f = File(d, name)
            f.delete(); part.renameTo(f)
            File(d, "$id.fmt").delete()
            // the cover, so it still shows offline
            var art = t.artUrl
            try {
                if (art.startsWith("http")) {
                    // save the big picture so the offline cover is sharp on the Now Playing screen too
                    val urls = listOf(com.ubayanda.milonative.data.YtMusic.art(art, 1080), art).distinct()
                    for (u in urls) {
                        val ok = http.newCall(Request.Builder().url(u).header("User-Agent", "milo-native/1.0").build()).execute().use { r ->
                            val b = r.body?.bytes()
                            if (r.isSuccessful && b != null && b.isNotEmpty()) { val cf = File(d, "$id.jpg"); cf.writeBytes(b); art = cf.absolutePath; true } else false
                        }
                        if (ok) break
                    }
                }
            } catch (_: Exception) {}
            _items.value = _items.value.filter { it.id != id } + Item(id, t.title, t.artist, if (t.album == "YouTube Music" || t.album == "YouTube") "" else t.album, art, t.durationMs, name, f.length(), System.currentTimeMillis())
            save()
            if (limit > 0 && st.autoClean && totalBytes() > limit) evictTo(limit, keep = id)
            try { appCtx?.let { com.ubayanda.milonative.data.LyricsRepo(it).fetchTo(File(d, "$id.lrc"), t) } } catch (_: Exception) {}
            setProgress(id, null)
        } catch (e: kotlinx.coroutines.CancellationException) {
            if (!keepPart.remove(id)) { part.delete(); File(d, "$id.fmt").delete() }
            setProgress(id, null); throw e
        } catch (e: Exception) {
            com.ubayanda.milonative.util.ErrorLog.add("download $id", e.message)
            if (e is Fatal || e.message?.startsWith("Resume point") == true) { part.delete(); File(d, "$id.fmt").delete() }
            setProgress(id, -1f)
            throw e
        }
    }
}
