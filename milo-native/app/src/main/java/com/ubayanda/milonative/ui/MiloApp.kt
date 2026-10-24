package com.ubayanda.milonative.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ubayanda.milonative.R
import com.ubayanda.milonative.model.Album
import com.ubayanda.milonative.model.Artist
import com.ubayanda.milonative.model.Track
import com.ubayanda.milonative.ui.components.BottomNav
import com.ubayanda.milonative.ui.components.MiniPlayer
import com.ubayanda.milonative.ui.components.NameDialog
import com.ubayanda.milonative.ui.components.PlaylistPicker
import com.ubayanda.milonative.ui.components.RowAction
import com.ubayanda.milonative.ui.components.Tab
import com.ubayanda.milonative.ui.screens.DetailScreen
import com.ubayanda.milonative.ui.screens.HomeScreen
import com.ubayanda.milonative.ui.screens.LibraryScreen
import com.ubayanda.milonative.ui.screens.NowPlayingScreen
import com.ubayanda.milonative.ui.screens.SearchScreen
import com.ubayanda.milonative.ui.theme.Milo
import androidx.compose.runtime.CompositionLocalProvider

private sealed class Detail {
    data class AlbumD(val album: Album) : Detail()
    data class ArtistD(val artist: Artist) : Detail()
    object Liked : Detail()
    data class PlaylistD(val id: String) : Detail()
}

@Composable
fun MiloApp(vm: MainViewModel) {
    val ctx = LocalContext.current
    val tracks by vm.tracks.collectAsState()
    val albums by vm.albums.collectAsState()
    val artists by vm.artists.collectAsState()
    val scanning by vm.scanning.collectAsState()
    val ui by vm.player.state.collectAsState()
    val favs by vm.favs.collectAsState()
    val playlists by vm.playlists.collectAsState()

    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    var section by rememberSaveable { mutableIntStateOf(0) }
    var detail by remember { mutableStateOf<Detail?>(null) }
    var playerOpen by remember { mutableStateOf(false) }
    var pickFor by remember { mutableStateOf<Track?>(null) }
    var newFor by remember { mutableStateOf<Track?>(null) }

    val byId = remember(tracks) { tracks.associateBy { it.id } }
    val current = byId[ui.currentId]
    val likedTracks = remember(tracks, favs) { tracks.filter { it.id in favs } }
    val bottomPad = if (current != null) 190.dp else 110.dp

    val actions = remember {
        TrackActions(
            addToPlaylist = { pickFor = it },
            share = { t ->
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "audio/*"
                    putExtra(Intent.EXTRA_STREAM, android.net.Uri.parse(t.uri))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                ctx.startActivity(Intent.createChooser(send, "Share song").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            },
        )
    }

    BackHandler(enabled = !playerOpen && detail != null) { detail = null }

    CompositionLocalProvider(LocalActions provides actions) {
        Box(Modifier.fillMaxSize().background(Milo.bg)) {
            Column(Modifier.fillMaxSize()) {
                // header: the milo. logo, centred, like the web app
                Box(Modifier.fillMaxWidth().statusBarsPadding().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                    Image(painterResource(R.drawable.logo_wordmark), "milo.", Modifier.height(40.dp))
                }
                val panelShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                Box(
                    Modifier.weight(1f).fillMaxWidth().clip(panelShape).background(Milo.panel, panelShape).border(1.dp, Milo.line, panelShape)
                ) {
                    when (val d = detail) {
                        is Detail.AlbumD -> DetailScreen(vm, d.album.title, d.album.artist, d.album.tracks, ui, bottomPad, { detail = null })
                        is Detail.ArtistD -> DetailScreen(vm, d.artist.name, "Artist", d.artist.tracks, ui, bottomPad, { detail = null })
                        is Detail.Liked -> DetailScreen(vm, "Liked songs", "Your favourites", likedTracks, ui, bottomPad, { detail = null }) { t ->
                            listOf(RowAction("Remove from Liked songs") { vm.toggleFav(t.id) })
                        }
                        is Detail.PlaylistD -> {
                            val p = playlists.firstOrNull { it.id == d.id }
                            LaunchedEffect(p == null) { if (p == null) detail = null }
                            if (p != null) {
                                var renaming by remember { mutableStateOf(false) }
                                val list = p.trackIds.mapNotNull { byId[it] }
                                DetailScreen(
                                    vm, p.name, "Playlist", list, ui, bottomPad, { detail = null },
                                    headerAction = "Rename" to { renaming = true },
                                    rowExtra = { t -> listOf(RowAction("Remove from this playlist") { vm.removeFromPlaylist(p.id, t.id) }) },
                                )
                                if (renaming) NameDialog("Rename playlist", p.name, "Save", { n -> vm.renamePlaylist(p.id, n); renaming = false }, { renaming = false })
                            }
                        }
                        null -> when (tab) {
                            Tab.Home -> HomeScreen(
                                vm, tracks, albums, ui, scanning, bottomPad, likedTracks.size, playlists.size,
                                onSearch = { tab = Tab.Search },
                                onOpenAlbum = { detail = Detail.AlbumD(it) },
                                onLiked = { detail = Detail.Liked },
                                onPlaylists = { section = 3; tab = Tab.Library },
                            )
                            Tab.Library -> LibraryScreen(
                                vm, tracks, albums, artists, playlists, likedTracks.size, ui, section, { section = it }, bottomPad,
                                { detail = Detail.AlbumD(it) }, { detail = Detail.ArtistD(it) }, { detail = Detail.PlaylistD(it.id) }, { detail = Detail.Liked },
                            )
                            Tab.Search -> SearchScreen(vm, tracks, ui, bottomPad)
                        }
                    }
                }
            }

            Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding()) {
                if (current != null) MiniPlayer(vm, current, ui, onOpen = { playerOpen = true })
                BottomNav(tab, onSelect = { tab = it; detail = null })
            }

            AnimatedVisibility(
                visible = playerOpen && current != null,
                enter = slideInVertically { it }, exit = slideOutVertically { it },
            ) {
                if (current != null) {
                    NowPlayingScreen(vm, current, ui, favs.contains(current.id)) { playerOpen = false }
                }
            }
        }

        pickFor?.let { t ->
            PlaylistPicker(
                playlists,
                onPick = { p -> vm.addToPlaylist(p.id, t.id); pickFor = null },
                onNew = { newFor = t; pickFor = null },
                onDismiss = { pickFor = null },
            )
        }
        newFor?.let { t ->
            NameDialog("New playlist", "", "Create", { n -> vm.createPlaylist(n, t.id); newFor = null }, { newFor = null })
        }
    }
}
