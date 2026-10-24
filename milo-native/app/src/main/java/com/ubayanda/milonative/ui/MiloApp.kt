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
import androidx.compose.runtime.SideEffect
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.foundation.layout.fillMaxHeight
import com.ubayanda.milonative.ui.theme.TintState
import com.ubayanda.milonative.ui.components.NowPlayingPill
import com.ubayanda.milonative.util.ColorMath
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntSize
import com.ubayanda.milonative.ui.components.rememberSoft
import kotlin.math.roundToInt
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
import com.ubayanda.milonative.ui.screens.ProfileScreen
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
    val acct by vm.account.collectAsState()
    val tintInt by vm.tint.collectAsState()

    // the whole app takes the playing song's colour, like the web milo.
    val neutral = tintInt == 0
    val hsl = if (neutral) doubleArrayOf(0.09, 0.05, 0.0) else ColorMath.rgbToHsl((tintInt shr 16) and 255, (tintInt shr 8) and 255, tintInt and 255)
    fun hue(l: Double, sMul: Double): Color {
        val sat = if (neutral || hsl[1] < 0.08) 0.04 else (hsl[1].coerceAtMost(0.7) * sMul)
        val rgb = ColorMath.hslToRgb(hsl[0], sat, l)
        return Color(rgb[0], rgb[1], rgb[2])
    }
    val panelT = if (neutral) Color(0xFF0B0A09) else hue(0.17, 0.95)
    val panel by animateColorAsState(panelT, tween(300), label = "panel")
    val white = Color.White
    SideEffect {
        TintState.panel = panel
        TintState.card = lerp(panel, white, 0.075f)
        TintState.card2 = lerp(panel, white, 0.13f)
        TintState.line = lerp(panel, white, 0.2f)
        TintState.art = !neutral
    }

    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    var section by rememberSaveable { mutableIntStateOf(0) }
    var detail by remember { mutableStateOf<Detail?>(null) }
    var playerOpen by remember { mutableStateOf(false) }
    var pickFor by remember { mutableStateOf<Track?>(null) }
    var newFor by remember { mutableStateOf<Track?>(null) }

    val byId = remember(tracks) { tracks.associateBy { it.id } }
    val current = byId[ui.currentId]
    val likedTracks = remember(tracks, favs) { tracks.filter { it.id in favs } }
    val bottomPad = if (current != null) 270.dp else 120.dp
    val hours = remember(tracks) { (tracks.sumOf { it.durationMs } / 3_600_000L).toInt() }
    val currentSoft = rememberSoft(vm, current).value

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
            // the song's art, stretched soft, fills the screen behind the header and panel
            if (currentSoft != null && !neutral) {
                val img = currentSoft.asImageBitmap()
                Box(
                    Modifier.fillMaxSize().drawBehind {
                        drawImage(img, dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()), filterQuality = FilterQuality.High)
                        drawRect(Color.Black.copy(alpha = 0.18f))
                    }
                )
            }
            Column(Modifier.fillMaxSize()) {
                // header: the milo. logo, centred, like the web app
                Box(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 20.dp, bottom = 37.dp), contentAlignment = Alignment.Center) {
                    Image(painterResource(R.drawable.logo_wordmark), "milo.", Modifier.height(46.dp))
                }
                val panelShape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)
                Box(
                    Modifier.weight(1f).fillMaxWidth().clip(panelShape).background(Milo.panel.copy(alpha = 0.96f), panelShape).border(1.dp, Milo.line, panelShape)
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
                                vm, tracks, albums, artists, playlists, byId, ui, scanning, bottomPad,
                                likedTracks.size, current != null && favs.contains(current.id), acct.username,
                                (acct.username.firstOrNull() ?: 'G').uppercaseChar().toString(),
                                onProfile = { tab = Tab.Profile },
                                onSearch = { tab = Tab.Search },
                                onOpenAlbum = { detail = Detail.AlbumD(it) },
                                onOpenArtist = { detail = Detail.ArtistD(it) },
                                onOpenPlaylist = { detail = Detail.PlaylistD(it.id) },
                                onLiked = { detail = Detail.Liked },
                            )
                            Tab.Library -> LibraryScreen(
                                vm, tracks, albums, artists, playlists, likedTracks.size, ui, section, { section = it }, bottomPad,
                                { detail = Detail.AlbumD(it) }, { detail = Detail.ArtistD(it) }, { detail = Detail.PlaylistD(it.id) }, { detail = Detail.Liked },
                            )
                            Tab.Search -> SearchScreen(vm, tracks, albums, artists, ui, bottomPad, { detail = Detail.AlbumD(it) }, { detail = Detail.ArtistD(it) })
                            Tab.Profile -> ProfileScreen(vm, acct, tracks.size, likedTracks.size, playlists.size, hours, bottomPad)
                        }
                    }
                }
            }

            Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding()) {
                if (current != null && tab != Tab.Home && detail == null) {
                    NowPlayingPill({ playerOpen = true }, Modifier.align(Alignment.CenterHorizontally).padding(bottom = 10.dp))
                }
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
