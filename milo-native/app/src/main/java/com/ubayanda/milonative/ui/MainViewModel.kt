package com.ubayanda.milonative.ui

import android.app.Application
import android.content.Context
import android.database.ContentObserver
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
    val avatar: String = "",
    val cover: String = "",
    val shareNp: Boolean = true,
    val sharePl: Boolean = true,
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
    val typing = com.ubayanda.milonative.data.Typing(cloud, viewModelScope)

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
        if (cloud.session != null) viewModelScope.launch {
            resolveProfile()
            // started offline: quietly try again until the server answers
            var n = 0
            while (!profileOk && cloud.session != null && n++ < 40) { delay(30_000); resolveProfile() }
        }
    }

    /** After sign-in (or on launch with a saved session): find out which step comes next. */
    /** Set by whichever list screen is on top: scrolls to the playing song and returns true, or false if it isn't in that list. */
    var jumper: (() -> Boolean)? = null
    /** Set with [jumper]: true while the playing song is in the list on screen (drives the "Now playing" button). */
    var canJump: (() -> Boolean)? by androidx.compose.runtime.mutableStateOf<(() -> Boolean)?>(null)
    private var profileOk = false
    private suspend fun resolveProfile() {
        val pr = cloud.loadProfile()
        val email = cloud.session?.email.orEmpty()
        when {
            cloud.session == null -> setAcct { it.copy(signedIn = false, step = "", busy = false) }
            // offline or the server didn't answer: keep the person signed in with what we last knew, never show the username screen
            pr == null && !cloud.profileMissing -> {
                val p0 = prefs0(getApplication())
                val un = p0.getString("prof_un", "").orEmpty()
                setAcct { it.copy(signedIn = true, email = email, username = if (it.username.isEmpty()) un else it.username, name = if (it.name.isEmpty()) p0.getString("prof_nm", un).orEmpty() else it.name, step = "", busy = false, err = "") }
            }
            pr == null -> setAcct { it.copy(signedIn = true, email = email, step = "claim", busy = false, err = "") }
            pr.termsV < CloudApi.TERMS_V -> setAcct { it.copy(signedIn = true, username = pr.username, email = email, step = "terms", busy = false, err = "") }
            else -> {
                seen(); profileOk = true
                prefs0(getApplication()).edit().putString("prof_un", pr.username).putString("prof_nm", pr.name.ifEmpty { pr.username }).apply()
                setAcct { it.copy(signedIn = true, username = pr.username, email = email, name = pr.name.ifEmpty { pr.username }, avatar = pr.avatar, cover = pr.cover, shareNp = pr.shareNp, sharePl = pr.sharePl, step = "", busy = false, err = "") }
                syncNow()
                plSig = ""; schedulePlPush()
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

    /** Set (or clear, with null) the profile picture or cover photo. [done] gets null or a message. */
    fun setPhoto(cover: Boolean, data: String?, done: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                if (cover) { cloud.setCover(data); setAcct { it.copy(cover = data.orEmpty()) } }
                else { cloud.setAvatar(data); setAcct { it.copy(avatar = data.orEmpty()) } }
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

    fun signInGoogle(context: android.content.Context) {
        setAcct { it.copy(busy = true, err = "") }
        viewModelScope.launch {
            try {
                val tok = com.ubayanda.milonative.data.GoogleSignIn.idToken(context)
                cloud.signInGoogle(tok); resolveProfile()
            } catch (x: com.ubayanda.milonative.data.CancelledSignIn) { setAcct { it.copy(busy = false) } }
            catch (x: Exception) {
                val m = x.message.orEmpty()
                setAcct { it.copy(busy = false, err = if (x is CloudError && Regex("provider|disabled|not enabled", RegexOption.IGNORE_CASE).containsMatchIn(m)) "Google sign-in isn't switched on for this app yet." else if (x is CloudError && Regex("nonce", RegexOption.IGNORE_CASE).containsMatchIn(m)) "Google sign-in: in Supabase > Authentication > Providers > Google, turn on \"Skip nonce checks\" and save. ($m)" else if (x is CloudError && Regex("audience|client", RegexOption.IGNORE_CASE).containsMatchIn(m)) "Google sign-in: add the Web client ID under \"Authorized Client IDs\" in Supabase > Providers > Google. ($m)" else if (x is CloudError) cloud.friendly(x) + " (" + x.status + ")" else m.ifEmpty { "Google sign-in failed" }) }
            }
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
        if (cloud.session != null) schedulePlPush()
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
    fun removeFromPlaylist(id: String, trackId: Long) {
        val at = _playlists.value.firstOrNull { it.id == id }?.trackIds?.indexOf(trackId) ?: -1
        savePlaylists(_playlists.value.map {
            if (it.id == id) it.copy(trackIds = it.trackIds.filterNot { x -> x == trackId }) else it
        })
        if (at >= 0) offerUndo("Removed from playlist") {
            savePlaylists(_playlists.value.map { p ->
                if (p.id == id && trackId !in p.trackIds) p.copy(trackIds = p.trackIds.toMutableList().also { l -> l.add(at.coerceAtMost(l.size), trackId) }) else p
            })
        }
    }

    // ---- "Undo" bar ----
    private val _undo = MutableStateFlow<UndoItem?>(null)
    val undo: StateFlow<UndoItem?> = _undo.asStateFlow()
    fun offerUndo(message: String, run: () -> Unit) { _undo.value = UndoItem(message, run) }
    fun dismissUndo() { _undo.value = null }
    fun doUndo() { val u = _undo.value; _undo.value = null; u?.run?.invoke() }

    /** Tapping the tab you are already on scrolls its list back to the top. */
    val scrollTop = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Pull to refresh on Home: new streaming shelves and rebuilt "Made for you" mixes. */
    fun refreshHome() { mixKey = ""; loadYtHome(true); buildMixes(_tracks.value) }

    suspend fun lyricsFor(t: Track): Lyrics? = lyricsRepo.get(t)
    suspend fun rescanLyrics(t: Track): Lyrics? = lyricsRepo.rescan(t)
    suspend fun lyricPreview(title: String, artist: String, durSec: Int): List<String> = lyricsRepo.preview(title, artist, durSec)

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
        restoreLast()
        viewModelScope.launch { saveLoop() }
        viewModelScope.launch { shareLoop() }
        viewModelScope.launch { statsLoop() }
        viewModelScope.launch { autoLoop() }
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

    // ---- share what I'm listening to / my playlists with friends ----
    private var plSig = ""
    private var plJob: Job? = null

    private fun dna(t: Track): JSONObject {
        val o = JSONObject().put("t", t.title).put("a", t.artist)
        if (t.id < 0 && t.uri.startsWith("https://")) {
            o.put("id", "nat" + t.id).put("src", t.album).put("url", t.uri).put("d", (t.durationMs / 1000).toInt())
            if (t.artUrl.startsWith("https://")) o.put("img", t.artUrl)
        } else o.put("local", 1)
        return o
    }

    private fun shareable(t: Track?) = t != null && !t.uri.startsWith("yt:") && !(t.durationMs == 0L && t.uri.startsWith("http")) && LibrarySync.syncable(t.uri)

    private suspend fun shareLoop() {
        var sig = ""; var at = 0L
        while (true) {
            delay(4000)
            val a = _account.value
            if (!a.signedIn || a.step.isNotEmpty() || cloud.session == null) continue
            val st = player.state.value
            val t = trackById(st.currentId)
            val np = if (t != null && st.isPlaying && a.shareNp && shareable(t)) {
                val o = dna(t).put("p", 1)
                val tint = _tint.value
                if (tint != 0) o.put("c", "${(tint shr 16) and 255},${(tint shr 8) and 255},${tint and 255}")
                o
            } else null
            val s = if (np != null) np.optString("id").ifEmpty { np.optString("t") + "|" + np.optString("a") } else "off"
            if (s == sig && (np == null || System.currentTimeMillis() - at < 90_000)) continue
            sig = s; at = System.currentTimeMillis()
            try { if (cloud.fresh()) social.pushNp(np) } catch (e: Exception) { sig = "" }
        }
    }

    private fun schedulePlPush() {
        plJob?.cancel()
        plJob = viewModelScope.launch { delay(1500); pushPl() }
    }

    private suspend fun pushPl() {
        val a = _account.value
        if (!a.signedIn || cloud.session == null) return
        var pl: JSONArray? = null
        if (a.sharePl) {
            pl = JSONArray()
            val liked = JSONArray()
            _favs.value.mapNotNull { trackById(it) }.filter { shareable(it) }.take(150).forEach { liked.put(dna(it)) }
            if (liked.length() > 0) pl.put(JSONObject().put("id", "liked").put("name", "Liked songs").put("t", liked))
            _playlists.value.take(40).forEach { p ->
                val ts = JSONArray()
                p.trackIds.mapNotNull { trackById(it) }.filter { shareable(it) }.take(150).forEach { ts.put(dna(it)) }
                pl.put(JSONObject().put("id", p.id).put("name", p.name).put("t", ts))
            }
        }
        val s = pl?.toString().orEmpty()
        if (s == plSig || s.length > 380_000) return
        plSig = s
        try { if (cloud.fresh()) social.pushPl(pl) } catch (e: Exception) { plSig = "" }
    }

    fun setShare(np: Boolean? = null, pl: Boolean? = null) {
        viewModelScope.launch {
            try {
                cloud.setShare(np, pl)
                setAcct { it.copy(shareNp = np ?: it.shareNp, sharePl = pl ?: it.sharePl) }
                if (pl != null) { plSig = "#"; schedulePlPush() }
            } catch (_: Exception) {}
        }
    }

    // ---- remember what was playing ----
    private val lastPrefs = app.getSharedPreferences("milo_last", Context.MODE_PRIVATE)

    private fun restoreLast() {
        try {
            val arr = JSONArray(lastPrefs.getString("queue", "[]"))
            if (arr.length() == 0) return
            val list = (0 until arr.length()).map { SavedStore.fromJson(arr.getJSONObject(it)) }
            val idx = lastPrefs.getInt("index", 0).coerceIn(0, list.size - 1)
            val cur = list[idx]
            if (cur.durationMs == 0L && cur.uri.startsWith("http")) return // a live station: don't start it on its own
            _remote.value = _remote.value + list.associateBy { it.id }
            player.restore(list, idx, lastPrefs.getLong("pos", 0L))
        } catch (_: Exception) {}
    }

    private suspend fun saveLoop() {
        var lastIds: List<Long> = emptyList()
        while (true) {
            delay(3000)
            val st = player.state.value
            val cid = st.currentId ?: continue
            if (st.queueIds != lastIds) {
                val list = st.queueIds.mapNotNull { trackById(it) }
                if (list.isEmpty()) continue
                val arr = JSONArray()
                list.forEach { arr.put(SavedStore.toJson(it)) }
                lastPrefs.edit().putString("queue", arr.toString()).apply()
                lastIds = st.queueIds
            }
            val order = lastIds.mapNotNull { trackById(it) }
            val idx = order.indexOfFirst { it.id == cid }.coerceAtLeast(0)
            lastPrefs.edit().putInt("index", idx).putLong("pos", st.positionMs).apply()
        }
    }

    private var pendingShortcut: String? = null
    /** Long-press shortcuts on the app icon. If the library isn't loaded yet, wait for it. */
    fun runShortcut(kind: String) {
        if (!_scanned.value) { pendingShortcut = kind; return }
        val list = if (kind == "liked") _favs.value.mapNotNull { trackById(it) } else _tracks.value.shuffled()
        if (list.isEmpty()) { android.widget.Toast.makeText(getApplication(), "Nothing to play yet", android.widget.Toast.LENGTH_SHORT).show(); return }
        if (kind == "liked") playRemoteOrLocal(list.shuffled()) else player.playQueue(list, 0)
    }
    private fun playRemoteOrLocal(list: List<Track>) { if (list.any { it.id < 0 }) playRemote(list, 0) else player.playQueue(list, 0) }

    fun editInfo(id: Long, title: String, artist: String, album: String) {
        com.ubayanda.milonative.data.MetaEdits.init(getApplication())
        com.ubayanda.milonative.data.MetaEdits.set(id, title, artist, album); scan()
    }
    fun resetInfo(id: Long) { com.ubayanda.milonative.data.MetaEdits.reset(id); scan() }

    fun scan() {
        viewModelScope.launch {
            _scanning.value = true
            com.ubayanda.milonative.data.MetaEdits.init(getApplication())
            val list = com.ubayanda.milonative.data.MetaEdits.apply(repo.scan())
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
            pendingShortcut?.let { pendingShortcut = null; runShortcut(it) }
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

    // ---- multi-select ----
    private val _selection = MutableStateFlow<Set<Long>>(emptySet())
    val selection: StateFlow<Set<Long>> = _selection.asStateFlow()
    fun toggleSelect(id: Long) { _selection.value = _selection.value.toMutableSet().also { if (!it.add(id)) it.remove(id) } }
    fun selectAll(ids: List<Long>) { _selection.value = ids.toSet() }
    fun clearSelection() { _selection.value = emptySet() }

    fun movePlaylistItem(id: String, from: Int, to: Int) = savePlaylists(_playlists.value.map { p ->
        if (p.id != id || from !in p.trackIds.indices || to !in p.trackIds.indices) p
        else p.copy(trackIds = p.trackIds.toMutableList().also { it.add(to, it.removeAt(from)) })
    })

    /** Create a playlist holding all of [ids] (the multi-select "new playlist"). */
    /** Smart playlists, built from listening history and when songs were added. Local songs only. */
    fun smartTracks(kind: String): List<Track> {
        val all = _tracks.value
        return when (kind) {
            "most" -> all.map { it to statsStore.playsOf(it.title, it.artist) }.filter { it.second > 0 }.sortedByDescending { it.second }.take(100).map { it.first }
            "recent" -> all.sortedByDescending { it.addedSec }.take(100)
            else -> all.filter { statsStore.playsOf(it.title, it.artist) == 0 }.sortedByDescending { it.addedSec }
        }
    }

    /** Make a playlist from a parsed .m3u. Returns (songs found, songs in the file). */
    fun importM3u(name: String, entries: List<Pair<String, String>>): Pair<Int, Int> {
        val all = _tracks.value
        val byKey = HashMap<String, Track>(); val byTitle = HashMap<String, Track>()
        all.forEach { byKey.putIfAbsent(LibrarySync.key(it.title, it.artist), it); byTitle.putIfAbsent(it.title.trim().lowercase(), it) }
        val ids = entries.mapNotNull { (t, a) -> (if (a.isNotEmpty()) byKey[LibrarySync.key(t, a)] else null) ?: byTitle[t.trim().lowercase()] }.map { it.id }
        if (ids.isNotEmpty()) createPlaylistWith(name, ids)
        return ids.distinct().size to entries.size
    }

    fun createPlaylistWith(name: String, ids: List<Long>): String {
        ids.forEach { ensureSaved(it) }
        val pid = "p" + System.currentTimeMillis()
        savePlaylists(_playlists.value + Playlist(pid, name.trim().ifEmpty { "New playlist" }, ids.distinct()))
        return pid
    }
    fun addManyToPlaylist(pid: String, ids: List<Long>) {
        ids.forEach { ensureSaved(it) }
        savePlaylists(_playlists.value.map { if (it.id == pid) it.copy(trackIds = it.trackIds + ids.filter { x -> x !in it.trackIds }.distinct()) else it })
    }
    fun likeMany(ids: List<Long>) {
        val n = _favs.value.toMutableSet().also { it.addAll(ids) }
        ids.forEach { ensureSaved(it) }
        _favs.value = n
        prefs.edit().putStringSet("favs", n.map { it.toString() }.toSet()).apply()
        touch()
    }

    // ---- listening insights ----
    private val statsStore = com.ubayanda.milonative.data.StatsStore(app)
    class StatsUi(val totalMs: Long = 0L, val plays: Int = 0, val songs: List<com.ubayanda.milonative.data.StatsStore.Row> = emptyList(), val artists: List<Pair<String, Int>> = emptyList())
    private val _stats = MutableStateFlow(StatsUi())
    val stats: StateFlow<StatsUi> = _stats.asStateFlow()
    fun statYears(): List<Int> = statsStore.years()
    fun statsFor(y: Int) = StatsUi(statsStore.yearMs(y), statsStore.yearPlays(y), statsStore.yearTopSongs(y, 5), statsStore.yearTopArtists(y, 5))
    private fun refreshStats() {
        _stats.value = StatsUi(statsStore.totalMs, statsStore.plays(), statsStore.topSongs(5), statsStore.topArtists(5))
    }

    val offline = com.ubayanda.milonative.data.OfflineStore.also { it.init(app) }.items

    // ---- daily mixes: made from what you play and like, new every day ----
    class Mix(val title: String, val subtitle: String, val tracks: List<Track>, val remote: Boolean)
    private val _mixes = MutableStateFlow<List<Mix>>(emptyList())
    val mixes: StateFlow<List<Mix>> = _mixes.asStateFlow()
    private var mixKey = ""
    fun buildMixes(all: List<Track>) {
        val day = System.currentTimeMillis() / 86_400_000L
        // the key includes your listening taste, so the mixes refresh as you play more (every 5 plays) and as your top artists change
        val taste = statsStore.topArtists(3).joinToString(",") { it.first }
        val key = "$day:${all.size}:${_favs.value.size}:${statsStore.plays() / 5}:$taste"
        if (key == mixKey) return
        mixKey = key
        viewModelScope.launch {
            val local = withContext(Dispatchers.Default) {
                val rnd = java.util.Random(day * 7919L + all.size)
                fun k(t: Track) = t.title.lowercase().trim() + "|" + t.artist.lowercase().trim()
                val played = statsStore.topSongs(200)
                val playCount = played.associate { (it.title.lowercase().trim() + "|" + it.artist.lowercase().trim()) to it.plays }
                val favs = _favs.value
                val out = ArrayList<Mix>()
                val fav = all.filter { it.id in favs || (playCount[k(it)] ?: 0) >= 2 }.shuffled(rnd).take(40)
                if (fav.size >= 6) out.add(Mix("Daily mix", "Your favourites, reshuffled", fav, false))
                val fresh = all.filter { (playCount[k(it)] ?: 0) == 0 && it.id !in favs }.shuffled(rnd).take(40)
                if (fresh.size >= 8) out.add(Mix("Rediscover", "Songs you haven't played yet", fresh, false))
                statsStore.topArtists(3).forEach { (a, _) ->
                    val xs = all.filter { it.artist.equals(a, true) }.shuffled(rnd)
                    if (xs.size >= 5 && out.size < 5) out.add(Mix("More $a", "${xs.size} songs on your phone", xs.take(40), false))
                }
                out
            }
            _mixes.value = local
            // online: radios built from your three most played artists, and one blended mix of them
            val tops = statsStore.topArtists(3).map { it.first }
            if (tops.isNotEmpty() && online()) {
                val got = ArrayList<Pair<String, List<Track>>>()
                for (a in tops) {
                    try {
                        val card = youtube.artists(a).firstOrNull()
                        val radioId = card?.let { youtube.page(it.id).radioId }.orEmpty()
                        if (radioId.isNotEmpty()) {
                            val r = youtube.radio(radioId).take(40)
                            if (r.size >= 8) got.add(a to r)
                        }
                    } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (_: Exception) {}
                }
                if (got.isNotEmpty()) {
                    got.forEach { rememberRemote(it.second) }
                    val blend = ArrayList<Track>(); val seen = HashSet<Long>()
                    val longest = got.maxOf { it.second.size }
                    for (i in 0 until longest) for (g in got) g.second.getOrNull(i)?.let { if (seen.add(it.id)) blend.add(it) }
                    val extra = ArrayList<Mix>()
                    if (blend.size >= 12) extra.add(Mix("Picked for you", "Mixed from " + got.joinToString(", ") { it.first }, blend.take(60), true))
                    got.forEach { g -> extra.add(Mix(g.first + " radio", "Streaming, endless", g.second, true)) }
                    _mixes.value = _mixes.value + extra
                }
            }
        }
    }

    // ---- streaming-style home: YouTube Music shelves while online ----
    private val _ytHome = MutableStateFlow<List<com.ubayanda.milonative.data.YtMusic.Shelf>>(emptyList())
    val ytHome: StateFlow<List<com.ubayanda.milonative.data.YtMusic.Shelf>> = _ytHome.asStateFlow()
    private var ytHomeAt = 0L
    private val _ytHomeLoading = MutableStateFlow(false)
    val ytHomeLoading: StateFlow<Boolean> = _ytHomeLoading.asStateFlow()
    fun online(): Boolean {
        val cm = getApplication<Application>().getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager ?: return true
        val c = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return c.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    fun loadYtHome(force: Boolean = false) {
        if (!online()) { _ytHome.value = emptyList(); return }
        if (!force && _ytHome.value.isNotEmpty() && System.currentTimeMillis() - ytHomeAt < 20 * 60_000L) return
        viewModelScope.launch {
            _ytHomeLoading.value = true
            val r = withContext(Dispatchers.IO) { try { com.ubayanda.milonative.data.YtMusic.home() } catch (_: Exception) { emptyList() } }
            if (r.isNotEmpty()) { _ytHome.value = r; ytHomeAt = System.currentTimeMillis() }
            _ytHomeLoading.value = false
        }
    }
    /** Universal search helpers: songs on YouTube Music, and songs on this phone whose saved lyrics contain the text. */
    suspend fun ytSearch(q: String): List<Track> = withContext(Dispatchers.IO) {
        try { com.ubayanda.milonative.data.YtMusic.searchSongs(q).take(15).map { com.ubayanda.milonative.data.YtMusic.toTrack(it) } } catch (_: Exception) { emptyList() }
    }
    suspend fun lyricSearch(q: String): List<Pair<Track, String>> =
        lyricsRepo.searchCached(q).mapNotNull { (id, line) -> trackById(id)?.let { it to line } }

    /** Plays the song behind a YouTube / YouTube Music link. */
    fun openLink(raw: String) {
        val id = Regex("""(?:[?&]v=|youtu\.be/|shorts/|embed/)([A-Za-z0-9_-]{11})""").find(raw)?.groupValues?.get(1)
        if (id == null) { android.widget.Toast.makeText(getApplication(), "milo. can't play that link", android.widget.Toast.LENGTH_SHORT).show(); return }
        viewModelScope.launch {
            val t = withContext(Dispatchers.IO) {
                try {
                    com.ubayanda.milonative.data.YtMusic.init(getApplication())
                    val s = com.ubayanda.milonative.data.YtMusic.stream(id)
                    com.ubayanda.milonative.data.YtMusic.toTrack(com.ubayanda.milonative.data.YtMusic.Song(id, s.title.ifEmpty { "YouTube song" }, s.author, "", "", "", s.durationSec, "https://i.ytimg.com/vi/$id/hqdefault.jpg"))
                } catch (_: Exception) { null }
            }
            if (t == null) android.widget.Toast.makeText(getApplication(), "Couldn't open that link", android.widget.Toast.LENGTH_SHORT).show()
            else playRemote(listOf(t), 0)
        }
    }

    fun playYtCard(c: com.ubayanda.milonative.data.YtMusic.Card) {
        val s = com.ubayanda.milonative.data.YtMusic.Song(c.id, c.title, c.sub, "", "", "", 0, c.thumb)
        playRemote(listOf(com.ubayanda.milonative.data.YtMusic.toTrack(s, c.thumb)), 0)
    }

    // ---- scrobbling and listen together ----
    val scrobbler = com.ubayanda.milonative.data.Scrobbler(app).also { com.ubayanda.milonative.util.ErrorLog.init(app) }
    private var nowSent: Long? = null
    val together = com.ubayanda.milonative.data.Together(
        token = { cloud.fresh(); cloud.session?.accessToken },
        onState = { j -> viewModelScope.launch { followHost(j) } },
        onHello = { hostPing = 0 },
    )
    private var hostPing = 0
    val signedIn: Boolean get() = cloud.session != null

    fun joinTogether(code: String, asHost: Boolean, done: (Boolean) -> Unit) {
        viewModelScope.launch { done(together.join(code, asHost, com.ubayanda.milonative.data.CloudApi.URL, com.ubayanda.milonative.data.CloudApi.KEY)) }
    }

    /** Lite: send my current EQ to a friend in chat (the Lite stand-in for Listen together). */
    fun shareEq(friendId: String, done: (Boolean) -> Unit) {
        if (!signedIn) { done(false); return }
        com.ubayanda.milonative.player.AudioFx.ensure()
        val st = com.ubayanda.milonative.player.AudioFx.state.value
        if (!st.available) { done(false); return }
        social.send(friendId, "text", com.ubayanda.milonative.data.EqShare.make(st.preset, st.bass, st.spatial, st.levelsDb), null)
        done(true)
    }

    /** One tap: start (or reuse) a hosted session and send its code to a friend in chat. */
    fun inviteToTogether(friendId: String, done: (String?) -> Unit) {
        if (!signedIn) { done(null); return }
        viewModelScope.launch {
            var c = together.code.value
            if (c.isEmpty() || !together.host) {
                c = (1..6).map { "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".random() }.joinToString("")
                if (!together.join(c, true, com.ubayanda.milonative.data.CloudApi.URL, com.ubayanda.milonative.data.CloudApi.KEY)) { done(null); return@launch }
            }
            social.send(friendId, "text", com.ubayanda.milonative.data.Invite.make(c), null)
            done(c)
        }
    }

    private suspend fun followHost(j: org.json.JSONObject) {
        val vid = j.optString("yt"); if (vid.isEmpty()) return
        val playing = j.optBoolean("playing")
        val lag = if (playing) ((System.currentTimeMillis() - j.optLong("ts")) / 1000.0).coerceIn(0.0, 5.0) else 0.0
        val want = ((j.optDouble("pos", 0.0) + lag) * 1000).toLong()
        val st = player.state.value
        val cur = trackById(st.currentId)
        if (cur?.uri != "yt:$vid") {
            val s = com.ubayanda.milonative.data.YtMusic.Song(vid, j.optString("t"), j.optString("a"), "", "", "", j.optInt("d"), j.optString("img"))
            playRemote(listOf(com.ubayanda.milonative.data.YtMusic.toTrack(s, j.optString("img"))), 0)
            delay(1500)
            player.seekTo(want)
            if (!playing && player.state.value.isPlaying) player.toggle()
        } else {
            if (kotlin.math.abs(st.positionMs - want) > 3000) player.seekTo(want)
            if (st.isPlaying != playing) player.toggle()
        }
    }

    private suspend fun statsLoop() {
        refreshStats()
        var cur: Long? = null; var acc = 0L; var counted = false; var n = 0
        while (true) {
            delay(1000)
            val st = player.state.value
            if (st.currentId != cur) { cur = st.currentId; acc = 0L; counted = false }
            val t = trackById(cur)
            if (t != null && st.isPlaying) {
                acc += 1000
                val need = if (t.durationMs in 1..59_999L) t.durationMs / 2 else 30_000L
                val play = !counted && acc >= need
                if (play) counted = true
                statsStore.add(t, 1000L, play)
                if (nowSent != t.id) { nowSent = t.id; viewModelScope.launch { scrobbler.nowPlaying(t) } }
                if (play) viewModelScope.launch { scrobbler.scrobble(t) }
            }
            if (together.code.value.isNotEmpty() && together.host && t != null && t.uri.startsWith("yt:") && hostPing-- <= 0) {
                hostPing = 3
                together.broadcast(t.uri.removePrefix("yt:"), t, st.positionMs, st.isPlaying)
            }
            if (++n >= 10) { n = 0; statsStore.save(); refreshStats() }
        }
    }

    // ---- smart autoplay: when the queue runs out, line up songs that fit ----
    private val fxPrefs = app.getSharedPreferences("milo_fx", Context.MODE_PRIVATE)
    private val _autoplay = MutableStateFlow(fxPrefs.getBoolean("autoplay", true))
    val autoplay: StateFlow<Boolean> = _autoplay.asStateFlow()
    fun setAutoplay(on: Boolean) { _autoplay.value = on; fxPrefs.edit().putBoolean("autoplay", on).apply() }

    /** Songs the autoplay added, so the queue can say so. */
    private val _autoIds = MutableStateFlow<Set<Long>>(emptySet())
    val autoIds: StateFlow<Set<Long>> = _autoIds.asStateFlow()

    /**
     * Autoplay: keeps the queue topped up BEFORE it runs out (when 2 songs are left), so there is never a gap.
     * Songs streamed from YouTube get YouTube Music's own "up next" radio for the current song; songs from this phone
     * are scored on artist, album, era, likes and plays, minus anything already played or skipped this session,
     * and no artist appears more than twice in one batch.
     */
    private suspend fun autoLoop() {
        var lastSig = ""
        val played = HashSet<Long>()
        val skipArtists = HashMap<String, Int>()
        var prevId: Long? = null
        var prevPos = 0L
        var prevDur = 0L
        while (true) {
            delay(2000)
            val st = player.state.value
            val curId = st.currentId
            if (curId != prevId) {
                // a song that was left within its first 20 s (or 30 %) counts as a skip
                val pid = prevId
                if (pid != null && prevDur > 0 && prevPos < 20_000L && prevPos < prevDur * 0.3) trackById(pid)?.let { skipArtists.merge(it.artist, 1, Int::plus) }
                prevId = curId
            }
            prevPos = st.positionMs; prevDur = st.durationMs
            if (curId != null) played.add(curId)
            if (!_autoplay.value) continue
            if (!st.isPlaying || st.shuffle || st.repeat != 0 || st.queueIds.isEmpty()) continue
            if (st.queueIndex < st.queueIds.size - 3) continue
            val cur = trackById(curId) ?: continue
            val sig = st.queueIds.size.toString() + ":" + cur.id
            if (sig == lastSig) continue
            lastSig = sig
            val inQueue = st.queueIds.toHashSet()
            val queueTracks = st.queueIds.mapNotNull { trackById(it) }
            val picks: List<Track> = if (cur.uri.startsWith("yt:")) {
                val have = queueTracks.map { com.ubayanda.milonative.data.LibrarySync.key(it.title, it.artist) }.toHashSet()
                val perArtist = HashMap<String, Int>()
                try {
                    youtube.radio("RDAMVM" + cur.uri.removePrefix("yt:")).filter { it.id !in inQueue && it.id !in played }
                        .filter { have.add(com.ubayanda.milonative.data.LibrarySync.key(it.title, it.artist)) }
                        .filter { (skipArtists[it.artist] ?: 0) < 2 }
                        .filter { perArtist.merge(it.artist, 1, Int::plus)!! <= 2 }.take(8)
                } catch (_: Exception) { emptyList() }
            } else if (cur.id >= 0) {
                val queueArtists = queueTracks.map { it.artist }.toHashSet()
                val favs = _favs.value
                val rnd = kotlin.random.Random(System.nanoTime())
                withContext(Dispatchers.Default) {
                    val scored = _tracks.value.asSequence().filter { it.id !in inQueue && it.id !in played && it.durationMs > 30_000L }.map { t ->
                        var sc = rnd.nextFloat() * 1.3f
                        val known = cur.artist != "Unknown artist"
                        if (known && t.artist == cur.artist) sc += 2.6f
                        if (t.albumId == cur.albumId) sc += 1f
                        if (cur.year > 0 && t.year > 0) sc += (0.8f - kotlin.math.abs(t.year - cur.year) * 0.12f).coerceAtLeast(0f)
                        if (t.id in favs) sc += 1.2f
                        sc += minOf(statsStore.playsOf(t.title, t.artist), 10) * 0.12f
                        if (t.artist in queueArtists) sc += 0.5f
                        sc -= (skipArtists[t.artist] ?: 0) * 1.8f
                        t to sc
                    }.sortedByDescending { it.second }.map { it.first }.take(60).toList()
                    val perArtist = HashMap<String, Int>()
                    scored.filter { perArtist.merge(it.artist, 1, Int::plus)!! <= 2 }.take(8)
                }
            } else emptyList()
            if (picks.isNotEmpty()) {
                if (picks.any { it.uri.startsWith("yt:") }) rememberRemote(picks)
                player.addManyToQueue(picks)
                _autoIds.value = (_autoIds.value + picks.map { it.id }).let { if (it.size > 400) picks.map { p -> p.id }.toSet() else it }
            }
        }
    }

    // ---- backup file (likes, playlists, listening stats) ----
    fun exportBackup(): String = snapshot().put("stats", statsStore.json()).put("app", "milo-native").put("v", 1).toString(2)

    fun importBackup(text: String, done: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val o = JSONObject(text)
                if (!o.has("favs") && !o.has("playlists")) throw IllegalArgumentException("not a backup")
                applyRemote(o, union = true)
                o.optJSONObject("stats")?.let { statsStore.merge(it); refreshStats() }
                touch()
                done("Backup restored. Songs that aren't on this phone stay pending until they appear.")
            } catch (e: Exception) { done("That file isn't a milo. backup.") }
        }
    }

    fun toggleFav(id: Long) {
        val n = _favs.value.toMutableSet()
        val added = n.add(id)
        if (!added) { n.remove(id); offerUndo("Removed from Liked songs") { toggleFav(id) } }
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
    val youtube = com.ubayanda.milonative.data.YouTubeRepo(app)

    /** Make a listed online song known (so it can be liked) without playing it. */
    /** Lets the player screens know about online songs before they are played (so the queue, mini player and widgets can show them). */
    fun rememberRemote(list: List<Track>) { _remote.value = _remote.value + list.associateBy { it.id } }
    fun playRemoteKeep(t: Track) { _remote.value = _remote.value + (t.id to t) }

    fun playRemote(list: List<Track>, index: Int) {
        if (list.isEmpty()) return
        _remote.value = _remote.value + list.associateBy { it.id }
        val t = list[index.coerceIn(0, list.size - 1)]
        if (t.durationMs == 0L && t.uri.startsWith("http")) {
            // live stream: make sure it answers before handing it to the player
            viewModelScope.launch {
                if (discover.reachable(t.uri)) player.playQueue(list, index)
                else android.widget.Toast.makeText(getApplication(), "That station isn't responding. Try another.", android.widget.Toast.LENGTH_LONG).show()
            }
        } else player.playQueue(list, index)
    }

    /** Saves online songs on the phone (two at a time). Songs from the phone's own storage are skipped. */
    fun downloadTracks(list: List<Track>) {
        val app = getApplication<Application>()
        val yt = list.filter { it.uri.startsWith("yt:") }
        if (yt.isEmpty()) { android.widget.Toast.makeText(app, "These songs are already on your phone.", android.widget.Toast.LENGTH_SHORT).show(); return }
        rememberRemote(yt)
        android.widget.Toast.makeText(app, if (yt.size == 1) "Downloading…" else "Downloading ${yt.size} songs…", android.widget.Toast.LENGTH_SHORT).show()
        com.ubayanda.milonative.data.OfflineStore.enqueue(yt) { n, e ->
            android.widget.Toast.makeText(app, if (n > 0) "Saved $n song" + (if (n == 1) "" else "s") + " for offline" else e ?: "Already saved on this phone", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun trackById(id: Long?): Track? =
        if (id == null) null else _tracks.value.firstOrNull { it.id == id } ?: _remote.value[id] ?: _saved.value[id]

    override fun onCleared() {
        observer?.let { getApplication<Application>().contentResolver.unregisterContentObserver(it) }
        player.release()
        super.onCleared()
    }
}

/** Something the person can take back within a few seconds. */
class UndoItem(val message: String, val run: () -> Unit, val id: Long = System.nanoTime())
