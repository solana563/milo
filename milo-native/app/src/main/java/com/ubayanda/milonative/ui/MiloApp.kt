package com.ubayanda.milonative.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.ContentScale
import com.ubayanda.milonative.ui.components.rememberCover
import androidx.compose.ui.geometry.Offset
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
import com.ubayanda.milonative.ui.components.backdropSource
import com.ubayanda.milonative.ui.components.rememberBackdropState
import com.ubayanda.milonative.ui.components.MiniPlayer
import com.ubayanda.milonative.ui.components.NameDialog
import com.ubayanda.milonative.ui.components.PlaylistPicker
import com.ubayanda.milonative.ui.components.RowAction
import com.ubayanda.milonative.ui.components.Tab
import com.ubayanda.milonative.ui.screens.DiscoverScreen
import com.ubayanda.milonative.ui.screens.DetailScreen
import com.ubayanda.milonative.ui.screens.HomeScreen
import com.ubayanda.milonative.ui.screens.LibraryScreen
import com.ubayanda.milonative.ui.screens.NowPlayingScreen
import com.ubayanda.milonative.ui.screens.SearchScreen
import com.ubayanda.milonative.ui.screens.ProfileScreen
import com.ubayanda.milonative.ui.theme.Milo
import androidx.compose.runtime.CompositionLocalProvider

private data class ScreenKey(val yt: String?, val detail: Detail?, val tab: Tab, val depth: Int)

private sealed class Detail {
    data class AlbumD(val album: Album) : Detail()
    data class ArtistD(val artist: Artist) : Detail()
    object Liked : Detail()
    data class PlaylistD(val id: String) : Detail()
    data class Smart(val kind: String) : Detail()
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
    // near-black like the web panel; colourful art lifts it a little, grey/dark art keeps it almost black
    val panelT = if (neutral) Color(0xFF0B0A09) else if (hsl[1] < 0.2) hue(0.065, 1.0) else hue(0.105, 0.9)
    val panel by animateColorAsState(panelT, tween(300), label = "panel")
    val white = Color.White
    SideEffect {
        TintState.panel = panel
        TintState.card = lerp(panel, white, 0.06f)
        TintState.card2 = lerp(panel, white, 0.11f)
        TintState.line = lerp(panel, white, 0.17f)
        TintState.art = !neutral
    }

    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    var section by rememberSaveable { mutableIntStateOf(0) }
    var detail by remember { mutableStateOf<Detail?>(null) }
    var playerOpen by remember { mutableStateOf(false) }
    var friendsOpen by remember { mutableStateOf(false) }
    var car by remember { mutableStateOf(false) }
    var yt by remember { mutableStateOf<Pair<List<Track>, Int>?>(null) }
    /** YouTube Music pages (artist / album / playlist), newest last; back goes to the one before. */
    var ytPages by remember { mutableStateOf<List<String>>(emptyList()) }
    var chatWith by remember { mutableStateOf<com.ubayanda.milonative.data.Friend?>(null) }
    var pickFor by remember { mutableStateOf<Track?>(null) }
    var newFor by remember { mutableStateOf<Track?>(null) }

    val remoteById by vm.remote.collectAsState()
    val savedById by vm.saved.collectAsState()
    val byId = remember(tracks, remoteById, savedById) { tracks.associateBy { it.id } + savedById + remoteById }
    val current = byId[ui.currentId]
    val likedTracks = remember(tracks, favs, savedById) { tracks.filter { it.id in favs } + savedById.values.filter { it.id in favs } }
    val bottomPad = if (current != null) 270.dp else 120.dp
    val hours = remember(tracks) { (tracks.sumOf { it.durationMs } / 3_600_000L).toInt() }
    val currentSoft = rememberSoft(vm, current).value
    val headerArt = rememberCover(vm, current, 600).value

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
            carMode = { car = true },
        )
    }

    val selection by vm.selection.collectAsState()
    var pickMany by remember { mutableStateOf(false) }
    val backdrop = rememberBackdropState()
    var newMany by remember { mutableStateOf(false) }
    // Back from anywhere goes Home first; on Home the first back warns and the second leaves the app.
    val lastBack = remember { longArrayOf(0L) }
    BackHandler(enabled = !car && selection.isEmpty() && !playerOpen && !friendsOpen && detail == null && yt == null && ytPages.isEmpty()) {
        if (tab != Tab.Home) tab = Tab.Home
        else {
            val now = System.currentTimeMillis()
            if (now - lastBack[0] < 2000L) (ctx as? android.app.Activity)?.finish()
            else { lastBack[0] = now; android.widget.Toast.makeText(ctx, "Press back again to exit", android.widget.Toast.LENGTH_SHORT).show() }
        }
    }
    BackHandler(enabled = car) { car = false }
    BackHandler(enabled = selection.isNotEmpty()) { vm.clearSelection() }
    BackHandler(enabled = !playerOpen && friendsOpen) { if (chatWith != null) chatWith = null else friendsOpen = false }
    BackHandler(enabled = !playerOpen && !friendsOpen && detail != null) { detail = null }

    CompositionLocalProvider(LocalActions provides actions) {
        Box(Modifier.fillMaxSize().background(Milo.bg)) {
            // the song's art, stretched soft, fills the screen behind the header and panel
            if (current != null && headerArt != null) {
                // the art behind the logo stays crisp and fully visible, fading out down the screen (like the web)
                val img = headerArt.asImageBitmap()
                Box(
                    Modifier.fillMaxWidth().fillMaxHeight(0.6f)
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            drawRect(Brush.verticalGradient(0.5f to Color.Black, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
                        }
                ) {
                    Image(img, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.15f), Color.Black.copy(alpha = 0.5f)))))
                }
            }
            Column(Modifier.fillMaxSize().backdropSource(backdrop)) {
                // header: the milo. logo, centred, like the web app
                Box(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 20.dp, bottom = 37.dp), contentAlignment = Alignment.Center) {
                    Image(painterResource(R.drawable.logo_wordmark), "milo.", Modifier.height(42.dp))
                }
                // like the web: solid cards, edge to edge. Other screens are one card with big top curves; Home draws its own two cards
                val panelShape = RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp)
                val ownCards = detail == null && tab == Tab.Home
                Box(
                    if (ownCards) Modifier.weight(1f).fillMaxWidth().clip(panelShape).background(Milo.bg, panelShape)
                    else Modifier.weight(1f).fillMaxWidth().clip(panelShape).background(Milo.panel, panelShape).border(1.dp, Milo.line, panelShape)
                ) {
                    // every screen change animates: deeper pages slide in from the right, going back slides out, switching tabs glides sideways
                    androidx.compose.animation.AnimatedContent(
                        targetState = ScreenKey(ytPages.lastOrNull(), detail, tab, if (ytPages.isNotEmpty()) 10 + ytPages.size else if (detail != null) 1 else 0),
                        transitionSpec = {
                            val ease = androidx.compose.animation.core.CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
                            val depthChange = targetState.depth != initialState.depth || targetState.yt != initialState.yt || targetState.detail != initialState.detail
                            if (depthChange) {
                                val forward = targetState.depth >= initialState.depth
                                if (forward)
                                    (androidx.compose.animation.slideInHorizontally(tween(340, easing = ease)) { it / 3 } + androidx.compose.animation.fadeIn(tween(260)) + androidx.compose.animation.scaleIn(tween(340, easing = ease), 0.96f)) togetherWith
                                        (androidx.compose.animation.slideOutHorizontally(tween(340, easing = ease)) { -it / 6 } + androidx.compose.animation.fadeOut(tween(200)) + androidx.compose.animation.scaleOut(tween(340, easing = ease), 0.96f))
                                else
                                    (androidx.compose.animation.slideInHorizontally(tween(340, easing = ease)) { -it / 6 } + androidx.compose.animation.fadeIn(tween(260)) + androidx.compose.animation.scaleIn(tween(340, easing = ease), 0.96f)) togetherWith
                                        (androidx.compose.animation.slideOutHorizontally(tween(340, easing = ease)) { it / 3 } + androidx.compose.animation.fadeOut(tween(200)) + androidx.compose.animation.scaleOut(tween(340, easing = ease), 0.96f))
                            } else {
                                val dir = if (targetState.tab.ordinal > initialState.tab.ordinal) 1 else -1
                                (androidx.compose.animation.slideInHorizontally(tween(300, easing = ease)) { dir * it / 8 } + androidx.compose.animation.fadeIn(tween(240, delayMillis = 50))) togetherWith
                                    (androidx.compose.animation.slideOutHorizontally(tween(260, easing = ease)) { -dir * it / 8 } + androidx.compose.animation.fadeOut(tween(140)))
                            }
                        },
                        label = "screen",
                    ) { sc ->
                    if (sc.yt != null) {
                        BackHandler(enabled = !playerOpen && yt == null) { ytPages = ytPages.dropLast(1) }
                        androidx.compose.runtime.key(sc.yt) {
                            com.ubayanda.milonative.ui.screens.YtPageScreen(vm, sc.yt, ui, bottomPad, { ytPages = ytPages + it }) { ytPages = ytPages.dropLast(1) }
                        }
                    } else
                    when (val d = sc.detail) {
                        is Detail.AlbumD -> DetailScreen(vm, d.album.title, d.album.artist, d.album.tracks, ui, bottomPad, { detail = null })
                        is Detail.ArtistD -> DetailScreen(vm, d.artist.name, "Artist", d.artist.tracks, ui, bottomPad, { detail = null })
                        is Detail.Liked -> DetailScreen(vm, "Liked songs", "Your favourites", likedTracks, ui, bottomPad, { detail = null }, rowExtra = { t ->
                            listOf(RowAction("Remove from Liked songs") { vm.toggleFav(t.id) })
                        })
                        is Detail.Smart -> {
                            val saved = vm.offline.collectAsState().value
                            DetailScreen(
                                vm, when (d.kind) { "most" -> "Most played"; "recent" -> "Recently added"; "downloads" -> "Downloads"; else -> "Never played" },
                                if (d.kind == "downloads") "Saved on this phone, plays with no connection" else "Made for you",
                                remember(d.kind, tracks, ui.currentId, saved) {
                                    if (d.kind == "downloads") com.ubayanda.milonative.data.OfflineStore.tracks().also { vm.rememberRemote(it) } else vm.smartTracks(d.kind)
                                },
                                ui, bottomPad, { detail = null },
                            )
                        }
                        is Detail.PlaylistD -> {
                            val p = playlists.firstOrNull { it.id == d.id }
                            LaunchedEffect(p == null) { if (p == null) detail = null }
                            if (p != null) {
                                var renaming by remember { mutableStateOf(false) }
                                var exporting by remember { mutableStateOf(false) }
                                val list = p.trackIds.mapNotNull { byId[it] }
                                val ctxE = androidx.compose.ui.platform.LocalContext.current
                                val saver = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.CreateDocument("audio/x-mpegurl")) { uri ->
                                    if (uri != null) {
                                        try { ctxE.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(com.ubayanda.milonative.util.M3u.export(list)) }
                                            android.widget.Toast.makeText(ctxE, "Playlist saved", android.widget.Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) { android.widget.Toast.makeText(ctxE, "Couldn't save the file", android.widget.Toast.LENGTH_SHORT).show() }
                                    }
                                }
                                LaunchedEffect(exporting) { if (exporting) { exporting = false; saver.launch(p.name.replace(Regex("[^A-Za-z0-9 _-]"), "") + ".m3u") } }
                                DetailScreen(
                                    vm, p.name, "Playlist", list, ui, bottomPad, { detail = null },
                                    headerAction = "Rename" to { renaming = true },
                                    headerAction2 = "Export" to { exporting = true },
                                    onMove = { f, t -> vm.movePlaylistItem(p.id, f, t) },
                                    rowExtra = { t -> listOf(RowAction("Remove from this playlist") { vm.removeFromPlaylist(p.id, t.id) }) },
                                )
                                if (renaming) NameDialog("Rename playlist", p.name, "Save", { n -> vm.renamePlaylist(p.id, n); renaming = false }, { renaming = false })
                            }
                        }
                        null -> when (sc.tab) {
                            Tab.Home -> HomeScreen(
                                vm, tracks, albums, artists, playlists, byId, ui, scanning, bottomPad,
                                likedTracks.size, current != null && favs.contains(current.id), acct.username,
                                (acct.username.firstOrNull() ?: 'G').uppercaseChar().toString(),
                                onProfile = { tab = Tab.Profile },
                                onSearch = { tab = Tab.Search },
                                onOpenAlbum = { detail = Detail.AlbumD(it) },
                                onOpenArtist = { detail = Detail.ArtistD(it) },
                                onOpenPlaylist = { detail = Detail.PlaylistD(it.id) },
                                onDownloads = { detail = Detail.Smart("downloads") },
                                onLiked = { detail = Detail.Liked },
                                onFriends = { friendsOpen = true },
                                onYtPage = { ytPages = ytPages + it },
                            )
                            Tab.Library -> LibraryScreen(
                                vm, tracks, albums, artists, playlists, likedTracks.size, ui, section, { section = it }, bottomPad,
                                { detail = Detail.AlbumD(it) }, { detail = Detail.ArtistD(it) }, { detail = Detail.PlaylistD(it.id) }, { detail = Detail.Liked },
                                onSmart = { detail = Detail.Smart(it) },
                            )
                            Tab.Discover -> DiscoverScreen(vm, ui, bottomPad, { l, i -> yt = l to i }, { ytPages = ytPages + it })
                            Tab.Search -> SearchScreen(vm, tracks, albums, artists, ui, bottomPad, { detail = Detail.AlbumD(it) }, { detail = Detail.ArtistD(it) }, onOpenPlaylist = { detail = Detail.PlaylistD(it.id) }, onOpenFriend = { chatWith = it; friendsOpen = true })
                            Tab.Profile -> ProfileScreen(vm, acct, tracks.size, likedTracks.size, playlists.size, hours, bottomPad)
                        }
                    }
                    }
                }
            }

            Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding()) {
                com.ubayanda.milonative.ui.components.UndoBar(vm)
                com.ubayanda.milonative.ui.components.DownloadBar()
                if (selection.isNotEmpty()) {
                    val chosen = selection.mapNotNull { byId[it] ?: vm.trackById(it) }
                    com.ubayanda.milonative.ui.components.SelectionBar(
                        selection.size, onClose = { vm.clearSelection() },
                        onPlayNext = { vm.player.playNextMany(chosen); vm.clearSelection() },
                        onQueue = { vm.player.addManyToQueue(chosen); vm.clearSelection() },
                        onLike = { vm.likeMany(selection.toList()); vm.clearSelection() },
                        onPlaylist = { pickMany = true },
                        onDownload = { vm.downloadTracks(chosen); vm.clearSelection() },
                    )
                } else if (current != null && tab != Tab.Home && vm.canJump?.invoke() == true) {
                    NowPlayingPill({ if (vm.jumper?.invoke() != true) playerOpen = true }, Modifier.align(Alignment.CenterHorizontally).padding(bottom = 10.dp))
                }
                if (current != null) MiniPlayer(vm, current, ui, onOpen = { playerOpen = true }, backdrop = backdrop)
                BottomNav(tab, onSelect = {
                    if (it == tab && detail == null && ytPages.isEmpty() && yt == null) vm.scrollTop.tryEmit(Unit)
                    else { tab = it; detail = null; ytPages = emptyList() }
                }, avatar = acct.avatar, name = acct.username, backdrop = backdrop)
            }

            if (friendsOpen) {
                Box(Modifier.fillMaxSize()) {
                    val cw = chatWith
                    if (cw != null) com.ubayanda.milonative.ui.screens.ChatScreen(vm, cw) { chatWith = null }
                    else com.ubayanda.milonative.ui.screens.FriendsScreen(vm, acct, { friendsOpen = false }) { chatWith = it }
                }
            }

            yt?.let { (l, i) -> Box(Modifier.fillMaxSize()) { com.ubayanda.milonative.ui.screens.YouTubeScreen(vm, l, i) { yt = null } } }

            AnimatedVisibility(
                visible = playerOpen && current != null,
                enter = slideInVertically { it }, exit = slideOutVertically { it },
            ) {
                if (current != null) {
                    NowPlayingScreen(vm, current, ui, favs.contains(current.id)) { playerOpen = false }
                }
            }

            AnimatedVisibility(visible = car, enter = androidx.compose.animation.fadeIn(), exit = androidx.compose.animation.fadeOut()) {
                com.ubayanda.milonative.ui.screens.CarScreen(vm, current, ui, current != null && favs.contains(current.id)) { car = false }
            }
            com.ubayanda.milonative.ui.screens.TourOverlay()
        }

        pickFor?.let { t ->
            PlaylistPicker(
                playlists,
                onPick = { p -> vm.addToPlaylist(p.id, t.id); pickFor = null },
                onNew = { newFor = t; pickFor = null },
                onDismiss = { pickFor = null },
            )
        }
        if (pickMany) {
            PlaylistPicker(
                playlists,
                onPick = { p -> vm.addManyToPlaylist(p.id, selection.toList()); pickMany = false; vm.clearSelection() },
                onNew = { newMany = true; pickMany = false },
                onDismiss = { pickMany = false },
            )
        }
        if (newMany) {
            NameDialog("New playlist", "", "Create", { n -> vm.createPlaylistWith(n, selection.toList()); newMany = false; vm.clearSelection() }, { newMany = false })
        }
        newFor?.let { t ->
            NameDialog("New playlist", "", "Create", { n -> vm.createPlaylist(n, t.id); newFor = null }, { newFor = null })
        }
    }
}
