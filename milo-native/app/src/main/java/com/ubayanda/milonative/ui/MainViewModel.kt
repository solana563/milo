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
import com.ubayanda.milonative.data.MediaRepo
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

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = MediaRepo(app)
    val art = ArtLoader(app)
    val player = PlayerHolder(app)
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

    /** RGB (0xAARRGGBB) of the colour the UI leans toward, taken from the playing song's art. */
    private val _accent = MutableStateFlow(0xFF8A8A8A.toInt())
    val accent: StateFlow<Int> = _accent.asStateFlow()

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
                if (t != null) _accent.value = computeAccent(t)
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

    private suspend fun computeAccent(t: Track): Int {
        val bmp: Bitmap = art.soft(t) ?: return 0xFF8A8A8A.toInt()
        return withContext(Dispatchers.Default) {
            val px = IntArray(bmp.width * bmp.height)
            bmp.getPixels(px, 0, bmp.width, 0, 0, bmp.width, bmp.height)
            0xFF000000.toInt() or ColorMath.accent(ColorMath.dominant(px))
        }
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
