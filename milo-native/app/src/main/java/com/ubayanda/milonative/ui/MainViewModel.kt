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
import com.ubayanda.milonative.data.PlaylistStore
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
)

private val UN_RE = Regex("^[a-z0-9_]{3,20}$")

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = MediaRepo(app)
    val art = ArtLoader(app)
    val player = PlayerHolder(app)
    private val lyricsRepo = LyricsRepo(app)
    private val store = PlaylistStore(app)
    private val cloud = CloudApi(app)

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
            else -> { seen(); setAcct { it.copy(signedIn = true, username = pr.username, email = email, step = "", busy = false, err = "") } }
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

    private fun savePlaylists(l: List<Playlist>) { _playlists.value = l; store.save(l) }

    fun createPlaylist(name: String, firstTrackId: Long? = null): String {
        val id = "p" + System.currentTimeMillis()
        savePlaylists(_playlists.value + Playlist(id, name.trim().ifEmpty { "New playlist" }, listOfNotNull(firstTrackId)))
        return id
    }
    fun deletePlaylist(id: String) = savePlaylists(_playlists.value.filterNot { it.id == id })
    fun renamePlaylist(id: String, name: String) = savePlaylists(_playlists.value.map { if (it.id == id) it.copy(name = name.trim().ifEmpty { it.name }) else it })
    fun addToPlaylist(id: String, trackId: Long) = savePlaylists(_playlists.value.map {
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
                val t = _tracks.value.firstOrNull { it.id == id }
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
        if (!n.add(id)) n.remove(id)
        _favs.value = n
        prefs.edit().putStringSet("favs", n.map { it.toString() }.toSet()).apply()
    }

    fun trackById(id: Long?): Track? = if (id == null) null else _tracks.value.firstOrNull { it.id == id }

    override fun onCleared() {
        observer?.let { getApplication<Application>().contentResolver.unregisterContentObserver(it) }
        player.release()
        super.onCleared()
    }
}
