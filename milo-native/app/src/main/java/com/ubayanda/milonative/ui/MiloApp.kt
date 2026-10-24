package com.ubayanda.milonative.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ubayanda.milonative.model.Album
import com.ubayanda.milonative.model.Artist
import com.ubayanda.milonative.ui.components.BottomNav
import com.ubayanda.milonative.ui.components.MiniPlayer
import com.ubayanda.milonative.ui.components.Tab
import com.ubayanda.milonative.ui.screens.DetailScreen
import com.ubayanda.milonative.ui.screens.HomeScreen
import com.ubayanda.milonative.ui.screens.LibraryScreen
import com.ubayanda.milonative.ui.screens.NowPlayingScreen
import com.ubayanda.milonative.ui.screens.SearchScreen
import com.ubayanda.milonative.ui.theme.Milo

@Composable
fun MiloApp(vm: MainViewModel) {
    val tracks by vm.tracks.collectAsState()
    val albums by vm.albums.collectAsState()
    val artists by vm.artists.collectAsState()
    val scanning by vm.scanning.collectAsState()
    val ui by vm.player.state.collectAsState()
    val favs by vm.favs.collectAsState()
    val accentInt by vm.accent.collectAsState()
    val accent = Color(accentInt)

    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    var section by rememberSaveable { mutableIntStateOf(0) }
    var album by remember { mutableStateOf<Album?>(null) }
    var artist by remember { mutableStateOf<Artist?>(null) }
    var playerOpen by remember { mutableStateOf(false) }

    val current = vm.trackById(ui.currentId)
    // room at the bottom of every list for the floating mini player + nav bar
    val bottomPad = if (current != null) 190.dp else 110.dp

    BackHandler(enabled = !playerOpen && (album != null || artist != null)) { album = null; artist = null }

    Box(Modifier.fillMaxSize().background(Milo.bg)) {
        val a = album
        val ar = artist
        when {
            a != null -> DetailScreen(vm, a.title, a.artist, a.tracks, ui, accent, bottomPad) { album = null }
            ar != null -> DetailScreen(vm, ar.name, "Artist", ar.tracks, ui, accent, bottomPad) { artist = null }
            tab == Tab.Home -> HomeScreen(vm, tracks, albums, ui, accent, scanning, bottomPad) { album = it }
            tab == Tab.Library -> LibraryScreen(
                vm, tracks, albums, artists, ui, accent, section, { section = it }, bottomPad, { album = it }, { artist = it },
            )
            else -> SearchScreen(vm, tracks, ui, accent, bottomPad)
        }

        Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding()) {
            if (current != null) {
                MiniPlayer(vm, current, ui, accent, onOpen = { playerOpen = true })
            }
            BottomNav(tab, accent, onSelect = { tab = it; album = null; artist = null })
        }

        AnimatedVisibility(
            visible = playerOpen && current != null,
            enter = slideInVertically { it }, exit = slideOutVertically { it },
        ) {
            if (current != null) {
                NowPlayingScreen(vm, current, ui, accent, favs.contains(current.id)) { playerOpen = false }
            }
        }
    }
}
