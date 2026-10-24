package com.ubayanda.milonative.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.ubayanda.milonative.model.PlayerUi
import com.ubayanda.milonative.model.Track
import com.ubayanda.milonative.ui.MainViewModel
import com.ubayanda.milonative.ui.components.Cover
import com.ubayanda.milonative.ui.components.EmptyNote
import com.ubayanda.milonative.ui.components.SectionTitle
import com.ubayanda.milonative.ui.components.TrackRow
import com.ubayanda.milonative.ui.components.artBackdrop
import com.ubayanda.milonative.ui.components.rememberSoft
import com.ubayanda.milonative.ui.theme.Milo
import com.ubayanda.milonative.util.greeting
import com.ubayanda.milonative.util.plural
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    vm: MainViewModel, tracks: List<Track>, albums: List<Album>, ui: PlayerUi, scanning: Boolean, bottomPad: Dp,
    likedCount: Int, playlistCount: Int,
    onSearch: () -> Unit, onOpenAlbum: (Album) -> Unit, onLiked: () -> Unit, onPlaylists: () -> Unit,
) {
    val recent = tracks.sortedByDescending { it.addedSec }.take(15)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = bottomPad)) {
        item {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        SimpleDateFormat("EEEE", Locale.getDefault()).format(Date()).uppercase(),
                        color = Milo.dim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
                    )
                    Text(
                        greeting(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)), color = Milo.text, fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold, maxLines = 1,
                    )
                }
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(Milo.card2, CircleShape).border(1.dp, Milo.line, CircleShape).clickable(onClick = onSearch),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Outlined.Search, "Search", tint = Milo.text, modifier = Modifier.size(22.dp)) }
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Brush.verticalGradient(listOf(Color(0xFFF99C00), Color(0xFFB36F00)))),
                    contentAlignment = Alignment.Center,
                ) { Text("G", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
        item { PlayerCard(vm, tracks, ui, scanning) }
        item {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Tile("Liked songs", plural(likedCount, "song"), Icons.Rounded.Favorite, true, Modifier.weight(1f), onLiked)
                    Tile("Playlists", plural(playlistCount, "playlist"), Icons.Rounded.QueueMusic, false, Modifier.weight(1f), onPlaylists)
                }
            }
        }
        if (tracks.isEmpty()) {
            item {
                if (!scanning) EmptyNote(
                    "No music found",
                    "Put songs on your phone, SD card or a USB drive and they will show up here on their own.",
                )
            }
        } else {
            item { SectionTitle("Recently added") }
            items(recent, key = { "r" + it.id }) { t ->
                TrackRow(vm, t, t.id == ui.currentId, Milo.amber) { vm.player.playQueue(recent, recent.indexOf(t)) }
            }
            item { SectionTitle("Albums") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(albums.take(24), key = { "a" + it.id + it.title }) { a -> AlbumTile(vm, a) { onOpenAlbum(a) } }
                }
            }
        }
    }
}

@Composable
private fun Tile(title: String, sub: String, icon: ImageVector, hot: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Row(
        modifier.clip(shape).background(Milo.card, shape).border(1.dp, Milo.line, shape).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(64.dp).background(if (hot) Brush.verticalGradient(listOf(Color(0xFFE39000), Color(0xFF6B4300))) else Brush.verticalGradient(listOf(Milo.card2, Milo.card))),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = if (hot) Color.White else Milo.text, modifier = Modifier.size(26.dp)) }
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(title, color = Milo.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(sub, color = Milo.dim, fontSize = 12.sp, maxLines = 1)
        }
    }
}

@Composable
private fun PlayerCard(vm: MainViewModel, tracks: List<Track>, ui: PlayerUi, scanning: Boolean) {
    val lead = vm.trackById(ui.currentId)
    val soft = rememberSoft(vm, lead).value
    val shape = RoundedCornerShape(30.dp)
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp).clip(shape)
            .then(if (lead != null) Modifier.artBackdrop(soft) else Modifier.background(Color.Black))
            .border(1.dp, Milo.line, shape).padding(22.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(24.dp).clip(CircleShape).background(Milo.amber), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Check, null, tint = Color.Black, modifier = Modifier.size(15.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text("Your player", color = Milo.dim, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(14.dp))
        if (lead != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Cover(vm, lead, 76.dp, 18.dp, px = 300)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(lead.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(lead.artist, color = Color(0xFFD8D8D8), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        } else {
            Text("Nothing playing yet", color = Milo.text, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                if (scanning && tracks.isEmpty()) "Looking for your music…" else "Pick a song and it shows up here",
                color = Milo.dim, fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Pill("Play", Icons.Rounded.PlayArrow, true, tracks.isNotEmpty()) {
                if (lead != null) vm.player.toggle() else vm.player.playQueue(tracks, 0)
            }
            Pill("Shuffle all", Icons.Rounded.Shuffle, false, tracks.isNotEmpty()) { vm.player.playShuffled(tracks) }
        }
    }
}

@Composable
fun Pill(label: String, icon: ImageVector?, filled: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier.clip(shape).background(if (filled) Milo.amber else Milo.card2, shape)
            .border(1.dp, if (filled) Milo.amber else Milo.line, shape)
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 20.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (filled) Color.Black else Milo.text, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(label, color = if (filled) Color.Black else Milo.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun AlbumTile(vm: MainViewModel, a: Album, onClick: () -> Unit) {
    val lead = a.tracks.first()
    val soft = rememberSoft(vm, lead).value
    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier.width(158.dp).clip(shape).artBackdrop(soft).border(1.dp, Milo.line, shape).clickable(onClick = onClick).padding(10.dp)
    ) {
        Cover(vm, lead, 138.dp, 16.dp, px = 300)
        Spacer(Modifier.height(8.dp))
        Text(a.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(a.artist, color = Color(0xFFD0D0D0), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
