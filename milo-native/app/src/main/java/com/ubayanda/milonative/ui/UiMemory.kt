package com.ubayanda.milonative.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ubayanda.milonative.data.YtMusic
import com.ubayanda.milonative.model.Track

/**
 * Where you were. Screens leave the composition when you open an album or switch tabs, which used to throw away their scroll
 * position and search results. This lives in the view model instead, so coming back puts the list exactly where you left it.
 */
class UiMemory {
    private val lists = object : LinkedHashMap<String, LazyListState>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, LazyListState>?): Boolean = size > 48
    }
    private val grids = HashMap<String, LazyGridState>()
    private val scrolls = HashMap<String, ScrollState>()

    fun list(key: String): LazyListState = lists.getOrPut(key) { LazyListState(0, 0) }
    fun grid(key: String): LazyGridState = grids.getOrPut(key) { LazyGridState(0, 0) }
    fun scroll(key: String): ScrollState = scrolls.getOrPut(key) { ScrollState(0) }

    // Discover: what was searched and what came back
    var dMode by mutableIntStateOf(3)
    var dQuery by mutableStateOf("")
    var dTag by mutableIntStateOf(0)
    var dItems by mutableStateOf<List<Track>>(emptyList())
    var dArtists by mutableStateOf<List<YtMusic.Card>>(emptyList())
    var dAlbums by mutableStateOf<List<YtMusic.Card>>(emptyList())
    var dChart by mutableStateOf(false)
    var dTick by mutableIntStateOf(0)
    var dKey = ""

    // Search tab
    var sQuery by mutableStateOf("")
    var sYt by mutableStateOf<List<Track>>(emptyList())
    var sLy by mutableStateOf<List<Pair<Track, String>>>(emptyList())
    var sKey = ""
}
