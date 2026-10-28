package com.ubayanda.milonative.ui

import android.app.Application
import android.content.Context
import android.database.ContentObserver
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ubayanda.milonative.data.ArtLoader
import com.ubayanda.milonative.data.CloudApi
import com.ubayanda.milonative.data.CloudError
import com.ubayanda.milonative.data.LyricsRepo
import com.ubayanda.milonative.data.MediaRepo
import com.ubayanda.milonative.data.LibrarySync
import com.ubayanda.milonative.data.PlaylistStore
import com.ubayanda.milonative.data.SavedStore
import org.json.JSONArray
import org.json.JSONObject
import com.ubayanda.milonative.model.Lyrics
import com.ubayanda.milonative.model.Playlist
import com.ubayanda.milonative.model.Album
import com.ubayanda.milonative.model.Artist
import com.ubayanda.milonative.model.Track
import com.ubayanda.milonative.player.PlayerHolder
import com.ubayanda.milonative.util.ColorMath
import com.ubayanda.milonative.util.NaturalOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Account + welcome/sign-in flow state. [step] is "" when no gate screen should show. */
data class AccountUi(
    val signedIn: Boolean = false,
    val username: String = "",
    val email: String = "",
    val step: String = "",
    val err: String = "",
    val busy: Boolean = false,
    val name: String = "",
)

private val UN_RE = Regex("^[a-z0-9_]{3,20}$")

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = MediaRepo(app)
    val art = ArtLoader(app)
    val player = PlayerHolder(app)
    private val lyricsRepo = LyricsRepo(app)
    private val store = PlaylistStore(app)
    private val cloud = CloudApi(app)
    val social = com.ubayanda.milonative.data.SocialModel(cloud, viewModelScope)

    private val _account = MutableStateFlow(
        AccountUi(
            signedIn = cloud.session != null, email = cloud.session?.email.orEmpty(),
            step = if (cloud.session == null && !prefs0(app).getBoolean("gate_seen", false)) "welcome" else "",
        )
    )
    val account: StateFlow<AccountUi> = _account.asStateFlow()

    private fun prefs0(c: Context) = c.getSharedPreferences("milo", Context.MODE_PRIVATE)

    private fun setAcct(f: (AccountUi) -> AccountUi) { _account.value = f(_account.value) }
    private fun seen() { prefs0(getApplication()).edit().putBoolean("gate_seen", true).apply() }

    init {
        if (cloud.session != null) viewModelScope.launch { resolveProfile() }
    }

    /** After sign-in (or on launch with a saved session): find out which step comes next. */
    private suspend fun resolveProfile() {
        val pr = cloud.loadProfile()
        val email = cloud.session?.email.orEmpty()
        when {
            cloud.session == null -> setAcct { it.copy(signedIn = false, step = "", busy = false) }
            pr == null -> setAcct { it.copy(signedIn = true, email = email, step = "claim", busy = false, err = "") }
            pr.termsV < CloudApi.TERMS_V -> setAcct { it.copy(signedIn = true, username = pr.username, email = email, step = "terms", busy = false, err = "") }
            else -> {
                seen(); setAcct { it.copy(signedIn = true, username = pr.username, email = email, name = pr.name.ifEmpty { pr.username }, step = "", busy = false, err = "") }
                syncNow()
            }
        }
    }

    /** Change display name and/or username. [done] gets null on success or a message to show. */
    fun editProfile(name: String, username: String, done: (String?) -> Unit) {
        val un = username.trim().lowercase().removePrefix("@")
        val nm = name.trim().ifEmpty { un }
        if (!UN_RE.matches(un)) return done("Username: 3 to 20 letters, numbers or _.")
        viewModelScope.launch {
            try {
                if (un != _account.value.username && !cloud.usernameFree(un)) throw CloudError(0, "That username is taken. Try another.")
                cloud.updateProfile(nm, un)
                setAcct { it.copy(username = un, name = nm) }
                done(null)
            } catch (x: Exception) { done(cloud.friendly(x)) }
        }
    }

    /** Permanently removes the account and its cloud data, then returns to the welcome screen. [done] gets null or a message. */
    fun deleteAccount(done: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                if (!cloud.fresh()) throw CloudError(401, "Please log in again first.")
                libSync.deleteAccount()
                prefs.edit().putLong("sync_ts", 0L).apply()
                setAcct { AccountUi(step = "welcome") }
                done(null)
            } catch (x: CloudError) {
                done(if (x.status == 404) "Delete isn't set up yet: run supabase_native_sync.sql in Supabase." else cloud.friendly(x))
            } catch (x: Exception) { done("Couldn't delete right now. Try again.") }
        }
    }

    fun gateStep(step: String) = setAcct { it.copy(step = step, err = "") }
    fun skipGate() { seen(); setAcct { it.copy(step = "", err = "") } }

    fun signUp(email: String, username: String, password: String) {
        val un = username.trim().lowercase().removePrefix("@")
        val e = email.trim()
        if (!UN_RE.matches(un)) return setAcct { it.copy(err = "Username: 3 to 20 letters, numbers or _.") }
        if (!Regex("""^\S+@\S+\.\S+$""").matches(e)) return setAcct { it.copy(err = "Enter a valid email address.") }
        if (password.length < 6) return setAcct { it.copy(err = "Use a password with at least 6 characters.") }
        setAcct { it.copy(busy = true, err = "") }
        viewModelScope.launch {
            try {
                if (!cloud.usernameFree(un)) throw CloudError(0, "That username is taken. Try another.")
                val ok = cloud.signUp(e, password, un)
                if (!ok) { setAcct { it.copy(busy = false, step = "in", err = "Check your inbox to confirm your email, then log in.") }; return@launch }
                // the profile row may be created by the server on sign-up; if not, create it now
                var pr = cloud.loadProfile()
                if (pr == null) { try { cloud.createProfile(un) } catch (x: CloudError) {} }
                resolveProfile()
            } catch (x: Exception) {
                setAcct { it.copy(busy = false, err = cloud.friendly(x)) }
            }
        }
    }

    fun signIn(email: String, password: String) {
        val e = email.trim()
        if (!Regex("""^\S+@\S+\.\S+$""").matches(e)) return setAcct { it.copy(err = "Enter a valid email address.") }
        if (password.length < 6) return setAcct { it.copy(err = "Use a password with at least 6 characters.") }
        setAcct { it.copy(busy = true, err = "") }
        viewModelScope.launch {
            try { cloud.signIn(e, password); resolveProfile() }
            catch (x: Exception) { setAcct { it.copy(busy = false, err = cloud.friendly(x)) } }
        }
    }

    fun claimUsername(username: String) {
        val un = username.trim().lowercase().removePrefix("@")
        if (!UN_RE.matches(un)) return setAcct { it.copy(err = "Username: 3 to 20 letters, numbers or _.") }
        setAcct { it.copy(busy = true, err = "") }
        viewModelScope.launch {
            try {
                if (!cloud.usernameFree(un)) throw CloudError(0, "That username is taken. Try another.")
                cloud.createProfile(un)
                resolveProfile()
            } catch (x: Exception) { setAcct { it.copy(busy = false, err = cloud.friendly(x)) } }
        }
    }

    fun agreeTerms() {
        setAcct { it.copy(busy = true, err = "") }
        viewModelScope.launch {
            try { cloud.agreeTerms(); resolveProfile() }
            catch (x: Exception) { setAcct { it.copy(busy = false, err = cloud.friendly(x)) } }
        }
    }

    fun signOut() {
        cloud.signOutLocal()
        prefs.edit().putLong("sync_ts", 0L).apply()
        _syncMsg.value = ""
        setAcct { AccountUi(step = "welcome") }
    }

    /** Open the gate again from Profile. */
    fun openGate(step: String) = setAcct { it.copy(step = step, err = "") }

    private val prefs = app.getSharedPreferences("milo", Context.MODE_PRIVATE)

    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()
    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()
    private val _artists = MutableStateFlow<List<Artist>>(emptyList())
    val artists: StateFlow<List<Artist>> = _artists.asStateFlow()
    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()
    private val _scanned = MutableStateFlow(false)
    val scanned: StateFlow<Boolean> = _scanned.asStateFlow()

    private val _favs = MutableStateFlow(
        prefs.getStringSet("favs", emptySet()).orEmpty().mapNotNull { it.toLongOrNull() }.toSet()
    )
    val favs: StateFlow<Set<Long>> = _favs.asStateFlow()

    private val _playlists = MutableStateFlow(store.load())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private fun savePlaylists(l: List<Playlist>) { _playlists.value = l; store.save(l); touch() }

    // ---- online songs / stations the person kept ----
    private val savedStore = SavedStore(app)
    private val _saved = MutableStateFlow(savedStore.load())
    val saved: StateFlow<Map<Long, Track>> = _saved.asStateFlow()

    private fun putSaved(t: Track) {
        if (_saved.value.containsKey(t.id)) return
        _saved.value = _saved.value + (t.id to t); savedStore.save(_saved.value)
    }
    private fun ensureSaved(id: Long) {
        if (id >= 0) return
        (_remote.value[id] ?: _saved.value[id])?.let { putSaved(it) }
    }
    private fun pruneSaved(id: Long) {
        if (id >= 0 || id in _favs.value || _playlists.value.any { id in it.trackIds }) return
        if (_saved.value.containsKey(id)) { _saved.value = _saved.value - id; savedStore.save(_saved.value) }
    }

    // ---- cloud backup of likes + playlists ----
    private val libSync = LibrarySync(cloud)
    private val _syncMsg = MutableStateFlow("")
    val syncMsg: StateFlow<String> = _syncMsg.asStateFlow()
    private var pushJob: Job? = null
    private var syncing = false
    /** Remote entries that name songs this phone doesn't have (yet): kept so they aren't lost on the next upload. */
    private val pendingFavs = ArrayList<JSONObject>()
    private val pendingPl = HashMap<String, ArrayList<JSONObject>>()

    private fun entry(t: Track): JSONObject {
        val o = JSONObject().put("k", LibrarySync.key(t.title, t.artist)).put("t", t.title).put("a", t.artist).put("al", t.album)
        if (t.id < 0) o.put("i", t.id).put("u", t.uri).put("art", t.artUrl).put("d", t.durationMs)
        return o
    }
    private fun entryOf(id: Long): JSONObject? {
        val t = trackById(id) ?: return null
        if (t.id < 0 && !LibrarySync.syncable(t.uri)) return null
        return entry(t)
    }

    private fun snapshot(): JSONObject {
        val favs = JSONArray(); val seen = HashSet<String>()
        _favs.value.forEach { id -> entryOf(id)?.let { if (seen.add(it.optString("k") + it.optString("u"))) favs.put(it) } }
        pendingFavs.forEach { if (seen.add(it.optString("k") + it.optString("u"))) favs.put(it) }
        val pls = JSONArray()
        _playlists.value.forEach { p ->
            val items = JSONArray()
            p.trackIds.forEach { id -> entryOf(id)?.let { items.put(it) } }
            pendingPl[p.id]?.forEach { items.put(it) }
            pls.put(JSONObject().put("id", p.id).put("n", p.name).put("items", items))
        }
        return JSONObject().put("favs", favs).put("playlists", pls)
    }

    /** Local id for a synced entry, or null when the song isn't on this phone. Online ones are saved and always resolve. */
    private fun resolve(e: JSONObject, index: Map<String, Long>): Long? {
        val u = e.optString("u")
        if (u.isNotEmpty()) {
            val id = e.optLong("i", 0L)
            if (id >= 0) return null
            putSaved(Track(id, u, e.optString("t"), e.optString("a"), e.optString("al"), 4_000_000_000_000L + (-id), e.optLong("d"), 0, 0, 0L, e.optString("art")))
            return id
        }
        return index[e.optString("k")]
    }

    private fun localIndex(): Map<String, Long> {
        val m = HashMap<String, Long>()
        _tracks.value.forEach { m.putIfAbsent(LibrarySync.key(it.title, it.artist), it.id) }
        return m
    }

    private fun applyRemote(d: JSONObject, union: Boolean) {
        val idx = localIndex()
        pendingFavs.clear(); pendingPl.clear()
        val favs: MutableSet<Long> = if (union) _favs.value.toMutableSet() else HashSet<Long>()
        d.optJSONArray("favs")?.let { a ->
            for (i in 0 until a.length()) {
                val e = a.getJSONObject(i)
                val id = resolve(e, idx)
                if (id != null) favs.add(id) else pendingFavs.add(e)
            }
        }
        val lists = ArrayList<Playlist>()
        val local = _playlists.value.associateBy { it.id }
        d.optJSONArray("playlists")?.let { a ->
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                val pid = o.getString("id")
                val ids = ArrayList<Long>()
                val items = o.optJSONArray("items") ?: JSONArray()
                for (j in 0 until items.length()) {
                    val e = items.getJSONObject(j)
                    val id = resolve(e, idx)
                    if (id != null) { if (id !in ids) ids.add(id) } else pendingPl.getOrPut(pid) { ArrayList() }.add(e)
                }
                if (union) local[pid]?.trackIds?.forEach { if (it !in ids) ids.add(it) }
                lists.add(Playlist(pid, o.optString("n", "Playlist"), ids))
            }
        }
        if (union) _playlists.value.forEach { p -> if (lists.none { it.id == p.id }) lists.add(p) }
        _favs.value = favs
        prefs.edit().putStringSet("favs", favs.map { it.toString() }.toSet()).apply()
        _playlists.value = lists; store.save(lists)
    }

    /** Try again to match leftover synced songs after the library was scanned. */
    private fun resolvePending() {
        if (pendingFavs.isEmpty() && pendingPl.isEmpty()) return
        val idx = localIndex()
        val f = _favs.value.toMutableSet()
        val it1 = pendingFavs.iterator()
        while (it1.hasNext()) { val id = idx[it1.next().optString("k")]; if (id != null) { f.add(id); it1.remove() } }
        if (f.size != _favs.value.size) { _favs.value = f; prefs.edit().putStringSet("favs", f.map { it.toString() }.toSet()).apply() }
        var pl = _playlists.value
        for ((pid, list) in pendingPl.toMap()) {
            val found = ArrayList<Long>()
            val i2 = list.iterator()
            while (i2.hasNext()) { val id = idx[i2.next().optString("k")]; if (id != null) { found.add(id); i2.remove() } }
            if (found.isNotEmpty()) pl = pl.map { if (it.id == pid) it.copy(trackIds = it.trackIds + found.filter { x -> x !in it.trackIds }) else it }
        }
        if (pl !== _playlists.value) { _playlists.value = pl; store.save(pl) }
    }

    private fun localHasData() = _favs.value.isNotEmpty() || _playlists.value.isNotEmpty()

    /** Something changed on this phone: remember when, and upload shortly after (if signed in). */
    private fun touch() {
        prefs.edit().putLong("sync_ts", System.currentTimeMillis()).apply()
        if (cloud.session == null) return
        pushJob?.cancel()
        pushJob = viewModelScope.launch { delay(3000); syncNow() }
    }

    fun syncNow() {
        if (cloud.session == null || syncing) return
        viewModelScope.launch {
            syncing = true
            try {
                if (!cloud.fresh()) return@launch
                val remote = libSync.pull()
                val ts = prefs.getLong("sync_ts", 0L)
                when {
                    remote == null -> if (localHasData()) { val now = System.currentTimeMillis(); libSync.push(now, snapshot()); prefs.edit().putLong("sync_ts", now).apply() }
                    ts == 0L && localHasData() -> {
                        applyRemote(remote.data, union = true)
                        val now = System.currentTimeMillis(); libSync.push(now, snapshot()); prefs.edit().putLong("sync_ts", now).apply()
                    }
                    remote.ts > ts -> { applyRemote(remote.data, union = false); prefs.edit().putLong("sync_ts", remote.ts).apply() }
                    ts > remote.ts -> libSync.push(ts, snapshot())
                }
                _syncMsg.value = "Backed up just now"
            } catch (e: CloudError) {
                _syncMsg.value = if (e.status == 404 || e.status == 400) "Cloud backup needs a one-time setup: run supabase_native_sync.sql in Supabase." else e.message.orEmpty()
            } catch (e: Exception) {
                _syncMsg.value = "Couldn't back up right now."
            } finally { syncing = false }
        }
    }

    fun createPlaylist(name: String, firstTrackId: Long? = null): String {
        firstTrackId?.let { ensureSaved(it) }
        val id = "p" + System.currentTimeMillis()
        savePlaylists(_playlists.value + Playlist(id, name.trim().ifEmpty { "New playlist" }, listOfNotNull(firstTrackId)))
        return id
    }
    fun deletePlaylist(id: String) = savePlaylists(_playlists.value.filterNot { it.id == id })
    fun renamePlaylist(id: String, name: String) = savePlaylists(_playlists.value.map { if (it.id == id) it.copy(name = name.trim().ifEmpty { it.name }) else it })
    fun addToPlaylist(id: String, trackId: Long) { ensureSaved(trackId); addToPl(id, trackId) }
    private fun addToPl(id: String, trackId: Long) = savePlaylists(_playlists.value.map {
        if (it.id == id && trackId !in it.trackIds) it.copy(trackIds = it.trackIds + trackId) else it
    })
    fun removeFromPlaylist(id: String, trackId: Long) = savePlaylists(_playlists.value.map {
        if (it.id == id) it.copy(trackIds = it.trackIds.filterNot { x -> x == trackId }) else it
    })

    suspend fun lyricsFor(t: Track): Lyrics? = lyricsRepo.get(t)

    /** RGB (0xAARRGGBB) of the colour the UI leans toward, taken from the playing song's art. */
    private val _accent = MutableStateFlow(0xFF8A8A8A.toInt())
    val accent: StateFlow<Int> = _accent.asStateFlow()

    /** The song's raw dominant colour (0xAARRGGBB) that tints the whole app, like the web milo. -1 = no art, stay neutral. */
    private val _tint = MutableStateFlow(0)
    val tint: StateFlow<Int> = _tint.asStateFlow()

    private var started = false
    private var observer: ContentObserver? = null
    private var rescan: Job? = null

    /** Call once the audio permission is granted. */
    fun start() {
        if (started) return
        started = true
        player.connect()
        scan()
        val o = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                // songs were added/removed (USB plugged in, download finished...): refresh after things settle
                rescan?.cancel()
                rescan = viewModelScope.launch { delay(1500); scan() }
            }
        }
        getApplication<Application>().contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, o
        )
        observer = o
        viewModelScope.launch {
            player.state.map { it.currentId }.distinctUntilChanged().collect { id ->
                val t = trackById(id)
                if (t != null) computeColours(t) else _tint.value = 0
            }
        }
    }

    fun scan() {
        viewModelScope.launch {
            _scanning.value = true
            val list = repo.scan()
            val (albums, artists) = withContext(Dispatchers.Default) {
                val al = list.groupBy { it.albumId to it.album }.map { (k, v) ->
                    val sorted = v.sortedWith(compareBy({ it.trackNo }, { NaturalOrder.key(it.title) }))
                    Album(k.first, k.second, sorted.first().artist, sorted.maxOf { it.year }, sorted)
                }.sortedWith { a, b -> NaturalOrder.compare(a.title, b.title) }
                val ar = list.groupBy { it.artist }.map { (k, v) -> Artist(k, v) }
                    .sortedWith { a, b -> NaturalOrder.compare(a.name, b.name) }
                al to ar
            }
            _tracks.value = list
            _albums.value = albums
            _artists.value = artists
            _scanning.value = false
            _scanned.value = true
            resolvePending()
        }
    }

    private suspend fun computeColours(t: Track) {
        val bmp: Bitmap? = art.soft(t)
        if (bmp == null) { _tint.value = 0; return }
        val dom = withContext(Dispatchers.Default) {
            val px = IntArray(bmp.width * bmp.height)
            bmp.getPixels(px, 0, bmp.width, 0, 0, bmp.width, bmp.height)
            ColorMath.dominant(px)
        }
        if (dom < 0) { _tint.value = 0; return }
        _tint.value = 0xFF000000.toInt() or dom
        _accent.value = 0xFF000000.toInt() or ColorMath.accent(dom)
    }

    fun toggleFav(id: Long) {
        val n = _favs.value.toMutableSet()
        val added = n.add(id)
        if (!added) n.remove(id)
        _favs.value = n
        prefs.edit().putStringSet("favs", n.map { it.toString() }.toSet()).apply()
        if (added) ensureSaved(id) else pruneSaved(id)
        touch()
    }

    /** Online tracks (radio, previews) that were played this session, so the player screens can find them by id. */
    private val _remote = MutableStateFlow<Map<Long, Track>>(emptyMap())
    val remote: StateFlow<Map<Long, Track>> = _remote.asStateFlow()
    val discover = com.ubayanda.milonative.data.DiscoverRepo()
    val server = com.ubayanda.milonative.data.SubsonicRepo(app)

    /** Make a listed online song known (so it can be liked) without playing it. */
    fun playRemoteKeep(t: Track) { _remote.value = _remote.value + (t.id to t) }

    fun playRemote(list: List<Track>, index: Int) {
        if (list.isEmpty()) return
        _remote.value = _remote.value + list.associateBy { it.id }
        player.playQueue(list, index)
    }

    fun trackById(id: Long?): Track? =
        if (id == null) null else _tracks.value.firstOrNull { it.id == id } ?: _remote.value[id] ?: _saved.value[id]

    override fun onCleared() {
        observer?.let { getApplication<Application>().contentResolver.unregisterContentObserver(it) }
        player.release()
        super.onCleared()
    }
}
