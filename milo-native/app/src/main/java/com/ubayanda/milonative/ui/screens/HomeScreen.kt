package com.ubayanda.milonative.ui.screens

import com.ubayanda.milonative.ui.components.glass
import com.ubayanda.milonative.ui.components.lift
import com.ubayanda.milonative.ui.components.pressable
import com.ubayanda.milonative.ui.components.tactile
import com.ubayanda.milonative.ui.components.BouncyRefresh
import com.ubayanda.milonative.ui.components.glassStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubayanda.milonative.model.Album
import com.ubayanda.milonative.model.Artist
import com.ubayanda.milonative.model.PlayerUi
import com.ubayanda.milonative.model.Playlist
import com.ubayanda.milonative.model.Track
import com.ubayanda.milonative.ui.MainViewModel
import com.ubayanda.milonative.ui.components.Cover
import com.ubayanda.milonative.ui.components.EmptyNote
import com.ubayanda.milonative.ui.components.SectionTitle
import com.ubayanda.milonative.ui.components.TrackRow
import com.ubayanda.milonative.ui.components.artBackdrop
import com.ubayanda.milonative.ui.components.rememberCover
import com.ubayanda.milonative.ui.components.rememberSoft
import com.ubayanda.milonative.ui.theme.Milo
import com.ubayanda.milonative.util.greeting
import com.ubayanda.milonative.util.plural
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private class TileData(val title: String, val lead: Track?, val icon: ImageVector?, val hot: Boolean, val collage: List<Track> = emptyList(), val onClick: () -> Unit)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: MainViewModel, tracks: List<Track>, albums: List<Album>, artists: List<Artist>, playlists: List<Playlist>,
    byId: Map<Long, Track>, ui: PlayerUi, scanning: Boolean, bottomPad: Dp,
    likedCount: Int, liked: Boolean, name: String, initial: String,
    onProfile: () -> Unit, onSearch: () -> Unit, onOpenAlbum: (Album) -> Unit, onOpenArtist: (Artist) -> Unit,
    onOpenPlaylist: (Playlist) -> Unit, onLiked: () -> Unit, onFriends: () -> Unit = {}, onYtPage: (String) -> Unit = {},
    onDownloads: () -> Unit = {},
) {
    androidx.compose.runtime.LaunchedEffect(Unit) { vm.loadYtHome() }
    val homeState = vm.mem.list("home")
    androidx.compose.runtime.LaunchedEffect(Unit) { vm.scrollTop.collect { homeState.animateScrollToItem(0) } }
    val ytShelves by vm.ytHome.collectAsState()
    val ytLoading by vm.ytHomeLoading.collectAsState()
    val mixes by vm.mixes.collectAsState()
    val savedSongs by vm.offline.collectAsState()
    val offlineNow = !vm.online()
    val hideLocalPref by com.ubayanda.milonative.data.Region.hideLocal.collectAsState()
    val hideLocal = hideLocalPref && !offlineNow   // online + the person asked to hide their phone library
    val favNow by vm.favs.collectAsState()
    androidx.compose.runtime.LaunchedEffect(tracks.size, favNow.size, ui.currentId) { vm.buildMixes(tracks) }
    val recent = tracks.sortedByDescending { it.addedSec }.take(50)
    val tiles = ArrayList<TileData>()
    val favSet = vm.favs.collectAsState().value
    val likedCovers = tracks.filter { it.id in favSet }.distinctBy { it.albumId }.take(4)
    tiles.add(TileData("Liked songs", null, Icons.Rounded.Favorite, true, likedCovers, onLiked))
    playlists.take(2).forEach { p -> tiles.add(TileData(p.name, p.trackIds.firstNotNullOfOrNull { byId[it] }, Icons.Rounded.QueueMusic, false, emptyList()) { onOpenPlaylist(p) }) }
    albums.take(8 - tiles.size).forEach { a -> tiles.add(TileData(a.title, a.tracks.first(), null, false, emptyList()) { onOpenAlbum(a) }) }

    val card = RoundedCornerShape(38.dp)
    // like the web mockup: separate rounded cards on the black canvas, with a small gap between them
    BouncyRefresh(refreshing = ytLoading, onRefresh = { vm.refreshHome() }, modifier = Modifier.fillMaxSize()) {
    LazyColumn(Modifier.fillMaxSize(), state = homeState, contentPadding = PaddingValues(bottom = bottomPad)) {
        item {
            Column(Modifier.fillMaxWidth().lift(card, 12f).clip(card).background(Milo.panel, card).border(1.dp, Milo.line, card).padding(bottom = 6.dp)) {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 20.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    SimpleDateFormat("EEEE", Locale.getDefault()).format(Date()).uppercase(),
                    color = Milo.dim, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f), CircleShape).border(1.dp, Milo.line, CircleShape).clickable(onClick = onSearch),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Outlined.Search, "Search", tint = Milo.text, modifier = Modifier.size(24.dp)) }
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f), CircleShape).border(1.dp, Milo.line, CircleShape).clickable(onClick = onFriends),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Outlined.People, "Friends", tint = Milo.text, modifier = Modifier.size(24.dp)) }
                Spacer(Modifier.width(8.dp))
                val me by vm.account.collectAsState()
                com.ubayanda.milonative.ui.components.UserAvatar(
                    name.ifEmpty { initial }, me.avatar, 44.dp,
                    Modifier.clip(CircleShape).clickable(onClick = onProfile),
                )
            }
            Text(
                greeting(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) + if (name.isNotEmpty()) ", $name" else "",
                color = Milo.text, style = com.ubayanda.milonative.ui.theme.Ty.display, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 16.dp),
            )
                PlayerCard(vm, tracks, ui, scanning, liked)
                if (tracks.isEmpty() && !scanning) EmptyNote(
                    "No music found",
                    "Put songs on your phone, SD card or a USB drive and they will show up here on their own.",
                )
                if (!hideLocal) Column(Modifier.padding(horizontal = 14.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    tiles.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { t -> Tile(vm, t, Modifier.weight(1f)) }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        // saved songs: first when you're offline, after the streaming shelves when you're online
        if (savedSongs.isNotEmpty() && offlineNow) item(key = "dl-top") { DownloadedCard(vm, ui, card, onDownloads) }
        // online: YouTube Music leads right under the player (recommended playlists, trending artists, charts). Offline: only your own music below.
        if (ytShelves.isEmpty() && ytLoading) {
            item(key = "ysk") {
                Column(Modifier.padding(top = 10.dp).fillMaxWidth().lift(card, 12f).clip(card).background(Milo.panel, card).border(1.dp, Milo.line, card).padding(bottom = 14.dp)) {
                    com.ubayanda.milonative.ui.components.SkeletonShelf()
                    com.ubayanda.milonative.ui.components.SkeletonShelf()
                }
            }
        }
        if (ytShelves.isNotEmpty()) {
            items(ytShelves.size, key = { "ys" + ytShelves[it].title }) { i ->
                val sh = ytShelves[i]
                Column(Modifier.padding(top = 10.dp).fillMaxWidth().lift(card, 12f).clip(card).background(Milo.panel, card).border(1.dp, Milo.line, card).padding(bottom = 14.dp)) {
                    YtShelf(sh.title, sh.cards, vm) { c -> if (c.kind == "song") vm.playYtCard(c) else onYtPage(c.id) }
                }
            }
        }
        if (savedSongs.isNotEmpty() && !offlineNow) item(key = "dl") { DownloadedCard(vm, ui, card, onDownloads) }
        val shownMixes = if (hideLocal) mixes.filter { it.remote } else mixes
        if (shownMixes.isNotEmpty()) {
            item(key = "mixes") {
                Column(Modifier.padding(top = 10.dp).fillMaxWidth().lift(card, 12f).clip(card).background(Milo.panel, card).border(1.dp, Milo.line, card).padding(bottom = 14.dp)) {
                    SectionTitle("Made for you")
                    LazyRow(state = vm.mem.list("home-mixes"), contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(shownMixes.size, key = { "mx$it" + shownMixes[it].title }) { i ->
                            val m = shownMixes[i]
                            Column(Modifier.width(148.dp).tactile(14.dp) { vm.player.playQueue(m.tracks, 0) }) {
                                Box {
                                    Cover(vm, m.tracks.first(), 148.dp, 14.dp, px = 400)
                                    if (m.remote) Text("LIVE", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(8.dp).clip(RoundedCornerShape(6.dp)).background(Milo.amber).padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(m.title, color = Milo.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(m.subtitle, color = Milo.dim, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
        if (artists.isNotEmpty() && !hideLocal) {
            item {
                Column(Modifier.padding(top = 10.dp).fillMaxWidth().lift(card, 12f).clip(card).background(Milo.panel, card).border(1.dp, Milo.line, card).padding(top = 6.dp, bottom = 6.dp)) {
                if (artists.isNotEmpty()) {
                    SectionTitle("Your artists")
                    LazyRow(state = vm.mem.list("home-artists"), contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(artists.take(20), key = { "ar" + it.name }) { a ->
                            Column(Modifier.width(132.dp).clickable { onOpenArtist(a) }, horizontalAlignment = Alignment.CenterHorizontally) {
                                Cover(vm, a.tracks.first(), 132.dp, 66.dp, px = 400)
                                Spacer(Modifier.height(6.dp))
                                Text(a.name, color = Milo.text, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }
                }
            }
        }
        item {
            val rctx = androidx.compose.ui.platform.LocalContext.current
            val played = androidx.compose.runtime.remember(ui.currentId, tracks.size) {
                com.ubayanda.milonative.player.RecentStore.ids(rctx).mapNotNull { vm.trackById(it) }.take(20)
            }
            if (played.isNotEmpty()) {
                Column(Modifier.padding(top = 10.dp).fillMaxWidth().lift(card, 12f).clip(card).background(Milo.panel, card).border(1.dp, Milo.line, card).padding(bottom = 10.dp)) {
                    SectionTitle("Recently played")
                    LazyRow(state = vm.mem.list("home-played"), contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(played, key = { "rp" + it.id }) { t ->
                            Column(Modifier.width(120.dp).tactile(14.dp) {
                                if (played.any { it.id < 0 }) vm.playRemote(played, played.indexOf(t)) else vm.player.playQueue(played, played.indexOf(t))
                            }) {
                                Cover(vm, t, 120.dp, 14.dp, px = 360)
                                Spacer(Modifier.height(6.dp))
                                Text(t.title, color = if (t.id == ui.currentId) Milo.amber else Milo.text, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(t.artist, color = Milo.dim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
        if (tracks.isNotEmpty() && !hideLocal) {
            item {
                Column(Modifier.padding(top = 10.dp).fillMaxWidth().lift(card, 12f).clip(card).background(Milo.panel, card).border(1.dp, Milo.line, card).padding(bottom = 10.dp)) {
                    SectionTitle("Recently added")
                    recent.forEach { t ->
                        TrackRow(vm, t, t.id == ui.currentId, Milo.amber) { vm.player.playQueue(recent, recent.indexOf(t)) }
                    }
                }
            }
        }
    }
        }
}

@Composable
private fun Tile(vm: MainViewModel, t: TileData, modifier: Modifier) {
    val shape = RoundedCornerShape(com.ubayanda.milonative.ui.theme.Rad.xl)
    Row(
        modifier.height(60.dp).tactile(36.dp, clip = false, onClick = t.onClick).lift(shape, 14f).glass(shape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (t.collage.size >= 2) {
            // four covers in a 2 x 2 grid, like the web Liked songs tile
            val c = t.collage + List(4 - t.collage.size) { t.collage[it % t.collage.size] }
            Column(Modifier.size(60.dp)) {
                Row { Cover(vm, c[0], 30.dp, 0.dp, px = 160); Cover(vm, c[1], 30.dp, 0.dp, px = 160) }
                Row { Cover(vm, c[2], 30.dp, 0.dp, px = 160); Cover(vm, c[3], 30.dp, 0.dp, px = 160) }
            }
        } else if (t.lead != null) {
            Cover(vm, t.lead, 60.dp, 0.dp, px = 160)
        } else {
            Box(
                Modifier.size(60.dp).background(if (t.hot) Brush.verticalGradient(listOf(Color(0xFFE39000), Color(0xFF6B4300))) else Brush.verticalGradient(listOf(Milo.card2, Milo.card))),
                contentAlignment = Alignment.Center,
            ) { if (t.icon != null) Icon(t.icon, null, tint = if (t.hot) Color.White else Milo.text, modifier = Modifier.size(26.dp)) }
        }
        Text(
            t.title, color = Milo.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
}

@Composable
private fun PlayerCard(vm: MainViewModel, tracks: List<Track>, ui: PlayerUi, scanning: Boolean, liked: Boolean) {
    val lead = vm.trackById(ui.currentId)
    val cover = rememberCover(vm, lead, 600).value
    val dark = Color(0xFF050505)
    val shape = RoundedCornerShape(com.ubayanda.milonative.ui.theme.Rad.xl)
    Box(Modifier.fillMaxWidth().padding(horizontal = 17.dp).height(252.dp).lift(shape, 16f).clip(shape).background(dark).border(1.dp, Milo.line, shape)) {
        if (lead != null && cover != null) {
            Box(Modifier.matchParentSize(), contentAlignment = Alignment.CenterEnd) {
                Image(remember(cover) { cover.asImageBitmap() }, null, Modifier.fillMaxHeight().fillMaxWidth(0.75f), contentScale = ContentScale.Crop)
            }
            Box(
                Modifier.matchParentSize().background(
                    Brush.linearGradient(
                        colorStops = arrayOf(0f to dark, 0.30f to dark, 0.52f to dark.copy(alpha = 0.45f), 1f to Color.Transparent),
                        start = Offset(0f, 0f), end = Offset(1100f, 900f),
                    )
                )
            )
        }
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(26.dp).clip(CircleShape).background(Milo.amber), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Check, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(if (lead != null) "Now playing" else "Your player", color = Milo.text.copy(alpha = 0.75f), fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(8.dp))
            if (lead != null) {
                Text(lead.title, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 31.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth(0.68f))
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(0.82f)) {
                    Icon(Icons.Rounded.Headphones, null, tint = Milo.text.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (lead.album.isBlank() || lead.album == "Unknown album") lead.artist else lead.artist + " · " + lead.album,
                        color = Milo.text.copy(alpha = 0.75f), fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier.size(46.dp).clip(CircleShape).background(Milo.amber).clickable { vm.player.toggle() },
                        contentAlignment = Alignment.Center,
                    ) { Icon(if (ui.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Play or pause", tint = Color.Black, modifier = Modifier.size(26.dp)) }
                    val pill = RoundedCornerShape(com.ubayanda.milonative.ui.theme.Rad.xl)
                    Row(
                        Modifier.clip(pill).background(Color(0x55000000), pill).border(1.dp, Color.White.copy(alpha = 0.28f), pill)
                            .clickable { vm.toggleFav(lead.id) }.padding(horizontal = 16.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null, tint = if (liked) Milo.amber else Milo.text, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (liked) "Liked" else "Like", color = Milo.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                }
            } else {
                Text("Nothing playing yet", color = Milo.text, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 38.sp)
                Text(
                    if (scanning && tracks.isEmpty()) "Looking for your music…" else "Pick a song and it shows up here",
                    color = Milo.dim, fontSize = 17.sp, modifier = Modifier.padding(top = 6.dp),
                )
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Pill("Play", Icons.Rounded.PlayArrow, true, tracks.isNotEmpty()) { vm.player.playQueue(tracks, 0) }
                    Pill("Shuffle all", Icons.Rounded.Shuffle, false, tracks.isNotEmpty()) { vm.player.playShuffled(tracks) }
                }
            }
        }
    }
}

@Composable
fun Pill(label: String, icon: ImageVector?, filled: Boolean, enabled: Boolean = true, compact: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Row(
        Modifier.heightIn(min = 44.dp).pressable(enabled, onClick).clip(shape)
            .background(if (filled) androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Milo.amber.copy(alpha = 1f), Milo.amber.copy(alpha = 0.86f))) else androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Milo.card2, Milo.card)), shape)
            .border(1.dp, glassStroke(if (filled) 0.9f else 1f), shape)
            .padding(horizontal = if (compact) 15.dp else 22.dp, vertical = if (compact) 11.dp else 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (filled) Color.Black else Milo.text, modifier = Modifier.size(if (compact) 20.dp else 22.dp))
            Spacer(Modifier.width(if (compact) 6.dp else 8.dp))
        }
        Text(label, color = if (filled) Color.Black else Milo.text, fontSize = if (compact) 15.sp else 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
    }
}


@Composable
private fun DownloadedCard(vm: MainViewModel, ui: PlayerUi, card: androidx.compose.ui.graphics.Shape, onAll: () -> Unit) {
    val list = remember(vm.offline.collectAsState().value) { com.ubayanda.milonative.data.OfflineStore.tracks() }
    Column(Modifier.padding(top = 10.dp).fillMaxWidth().lift(card, 12f).clip(card).background(Milo.panel, card).border(1.dp, Milo.line, card).padding(bottom = 10.dp)) {
        SectionTitle("Downloaded")
        Text("${list.size} song" + (if (list.size == 1) "" else "s") + " that play with no connection", color = Milo.dim, fontSize = 13.sp, modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 10.dp))
        Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Pill("Play all", Icons.Rounded.PlayArrow, true, compact = true) { vm.rememberRemote(list); vm.player.playQueue(list, 0) }
            Pill("Shuffle", Icons.Rounded.Shuffle, false, compact = true) { vm.rememberRemote(list); vm.player.playShuffled(list) }
            Pill("See all", null, false, compact = true) { onAll() }
        }
        list.take(8).forEach { t -> TrackRow(vm, t, t.id == ui.currentId, Milo.amber) { vm.rememberRemote(list); vm.player.playQueue(list, list.indexOf(t)) } }
    }
}
