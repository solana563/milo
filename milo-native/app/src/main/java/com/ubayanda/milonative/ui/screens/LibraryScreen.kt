package com.ubayanda.milonative.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.ubayanda.milonative.ui.components.LetterHeader
import com.ubayanda.milonative.ui.components.MiloChip
import com.ubayanda.milonative.ui.components.NameDialog
import com.ubayanda.milonative.ui.components.ScreenTitle
import com.ubayanda.milonative.ui.components.TrackRow
import com.ubayanda.milonative.ui.theme.Milo
import com.ubayanda.milonative.util.NaturalOrder
import com.ubayanda.milonative.util.plural
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(
    vm: MainViewModel, tracks: List<Track>, albums: List<Album>, artists: List<Artist>, playlists: List<Playlist>,
    likedCount: Int, ui: PlayerUi, section: Int, onSection: (Int) -> Unit, bottomPad: Dp,
    onOpenAlbum: (Album) -> Unit, onOpenArtist: (Artist) -> Unit, onOpenPlaylist: (Playlist) -> Unit, onLiked: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        ScreenTitle("Your library")
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Songs", "Albums", "Artists", "Playlists").forEachIndexed { i, label ->
                MiloChip(label, i == section, Modifier.weight(1f)) { onSection(i) }
            }
        }
        Spacer(Modifier.padding(top = 10.dp))
        when {
            section == 3 -> PlaylistsList(vm, playlists, likedCount, bottomPad, onOpenPlaylist, onLiked)
            tracks.isEmpty() -> EmptyNote("No music found", "Songs on your phone, SD card or USB drive show up here automatically.")
            section == 0 -> SongsList(vm, tracks, ui, bottomPad)
            section == 1 -> LazyVerticalGrid(
                columns = GridCells.Fixed(2), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = bottomPad),
                horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                gridItems(albums, key = { "al" + it.id + it.title }, contentType = { "album" }) { a ->
                    GridCard(vm, a.tracks.first(), a.title, a.artist + " · " + plural(a.tracks.size, "song"), false) { onOpenAlbum(a) }
                }
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(2), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = bottomPad),
                horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                gridItems(artists, key = { "ar" + it.name }, contentType = { "artist" }) { a ->
                    GridCard(vm, a.tracks.first(), a.name, plural(a.tracks.size, "song"), true) { onOpenArtist(a) }
                }
            }
        }
    }
}

/** Big square cover (round for artists) with the name under it, for the two-column album and artist grids. */
@Composable
private fun GridCard(vm: MainViewModel, lead: Track, title: String, sub: String, round: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick),
        horizontalAlignment = if (round) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
            Cover(vm, lead, maxWidth, if (round) maxWidth / 2 else 22.dp, px = 480)
        }
        Text(
            title, color = Milo.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
            textAlign = if (round) androidx.compose.ui.text.style.TextAlign.Center else androidx.compose.ui.text.style.TextAlign.Start,
            modifier = Modifier.padding(top = 8.dp, start = 2.dp, end = 2.dp).then(if (round) Modifier.fillMaxWidth() else Modifier),
        )
        Text(
            sub, color = Milo.dim, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            textAlign = if (round) androidx.compose.ui.text.style.TextAlign.Center else androidx.compose.ui.text.style.TextAlign.Start,
            modifier = Modifier.padding(start = 2.dp, end = 2.dp).then(if (round) Modifier.fillMaxWidth() else Modifier),
        )
    }
}

@Composable
private fun ListRow(vm: MainViewModel, lead: Track, title: String, sub: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Cover(vm, lead, 54.dp, 14.dp, px = 160)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Milo.text, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sub, color = Milo.dim, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun IconRow(icon: ImageVector, hot: Boolean, title: String, sub: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(
                if (hot) Brush.verticalGradient(listOf(Color(0xFFE39000), Color(0xFF6B4300))) else Brush.verticalGradient(listOf(Milo.card2, Milo.card))
            ),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = if (hot) Color.White else Milo.text, modifier = Modifier.size(24.dp)) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Milo.text, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sub, color = Milo.dim, fontSize = 13.sp, maxLines = 1)
        }
    }
}

@Composable
private fun PlaylistsList(
    vm: MainViewModel, playlists: List<Playlist>, likedCount: Int, bottomPad: Dp,
    onOpen: (Playlist) -> Unit, onLiked: () -> Unit,
) {
    var creating by remember { mutableStateOf(false) }
    LazyColumn(contentPadding = PaddingValues(bottom = bottomPad)) {
        item { IconRow(Icons.Rounded.Add, false, "New playlist", "Make your own mix", { creating = true }) }
        item { IconRow(Icons.Rounded.Favorite, true, "Liked songs", plural(likedCount, "song"), onLiked) }
        items(playlists, key = { it.id }) { p ->
            IconRow(Icons.Rounded.QueueMusic, false, p.name, plural(p.trackIds.size, "song")) { onOpen(p) }
        }
        if (playlists.isEmpty()) item { EmptyNote("No playlists yet", "Tap New playlist, or use the ⋮ menu on any song and choose Add to playlist.") }
    }
    if (creating) {
        NameDialog("New playlist", "", "Create", { n -> vm.createPlaylist(n); creating = false }, { creating = false })
    }
}

/** Songs A to Z with letter headers and a tappable letter rail on the right. */
@Composable
private fun SongsList(vm: MainViewModel, tracks: List<Track>, ui: PlayerUi, bottomPad: Dp) {
    val state: LazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val rows = remember(tracks) {
        val out = ArrayList<Any>()
        var last = ""
        for (t in tracks) {
            val l = NaturalOrder.letter(t.title)
            if (l != last) { out.add(l); last = l }
            out.add(t)
        }
        out
    }
    val letterIndex = remember(rows) {
        val m = LinkedHashMap<String, Int>()
        rows.forEachIndexed { i, r -> if (r is String) m[r] = i }
        m
    }
    val curId by androidx.compose.runtime.rememberUpdatedState(ui.currentId)
    androidx.compose.runtime.DisposableEffect(rows) {
        val j: () -> Boolean = {
            val i = rows.indexOfFirst { it is Track && it.id == curId }
            if (i >= 0) { scope.launch { state.animateScrollToItem((i - 2).coerceAtLeast(0)) }; true } else false
        }
        vm.jumper = j
        onDispose { if (vm.jumper === j) vm.jumper = null }
    }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(state = state, contentPadding = PaddingValues(end = 18.dp, bottom = bottomPad)) {
            itemsIndexed(rows, key = { _, r -> if (r is Track) "t" + r.id else "h$r" }, contentType = { _, r -> if (r is Track) "track" else "header" }) { _, r ->
                if (r is Track) TrackRow(vm, r, r.id == ui.currentId, Milo.amber) { vm.player.playQueue(tracks, tracks.indexOf(r)) }
                else LetterHeader(r as String, Milo.amber)
            }
        }
        Column(
            Modifier.align(Alignment.CenterEnd).fillMaxHeight(0.7f).padding(end = 2.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            letterIndex.keys.forEach { l ->
                Text(
                    l, color = Milo.amber, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { scope.launch { state.scrollToItem(letterIndex[l] ?: 0) } }.padding(horizontal = 6.dp),
                )
            }
        }
    }
}
