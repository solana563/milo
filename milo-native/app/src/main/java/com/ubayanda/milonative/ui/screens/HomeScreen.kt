package com.ubayanda.milonative.ui.screens

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import java.util.Calendar

@Composable
fun HomeScreen(
    vm: MainViewModel, tracks: List<Track>, albums: List<Album>, ui: PlayerUi, accent: Color,
    scanning: Boolean, bottomPad: Dp, onOpenAlbum: (Album) -> Unit,
) {
    val recent = tracks.sortedByDescending { it.addedSec }.take(15)
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(bottom = bottomPad)) {
        item {
            Text(
                greeting(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)), color = Milo.text, fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 16.dp, top = 18.dp, end = 16.dp),
            )
            Text(
                if (scanning && tracks.isEmpty()) "Looking for your music…" else plural(tracks.size, "song") + " on this phone",
                color = Milo.dim, fontSize = 14.sp, modifier = Modifier.padding(start = 16.dp, bottom = 14.dp),
            )
        }
        if (tracks.isEmpty()) {
            item {
                if (!scanning) EmptyNote(
                    "No music found",
                    "Put songs on your phone, SD card or a USB drive and they will show up here on their own.",
                )
            }
        } else {
            item { HeroCard(vm, tracks, ui, accent) }
            item { SectionTitle("Recently added") }
            items(recent, key = { "r" + it.id }) { t ->
                TrackRow(vm, t, t.id == ui.currentId, accent) { vm.player.playQueue(recent, recent.indexOf(t)) }
            }
            item { SectionTitle("Albums") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(albums.take(24), key = { "a" + it.id + it.title }) { a -> AlbumTile(vm, a) { onOpenAlbum(a) } }
                }
            }
        }
    }
}

@Composable
private fun HeroCard(vm: MainViewModel, tracks: List<Track>, ui: PlayerUi, accent: Color) {
    val lead = vm.trackById(ui.currentId) ?: tracks.first()
    val soft = rememberSoft(vm, lead).value
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(24.dp)).artBackdrop(soft).padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Cover(vm, lead, 92.dp, 14.dp, px = 360)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(if (ui.currentId != null) "Now playing" else "Your music", color = Color(0xFFD8D8D8), fontSize = 12.sp)
                Text(lead.title, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(lead.artist, color = Color(0xFFD8D8D8), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { vm.player.playQueue(tracks, tracks.indexOf(lead).coerceAtLeast(0)) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                shape = RoundedCornerShape(22.dp),
            ) {
                Icon(Icons.Rounded.PlayArrow, null)
                Text("Play", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { vm.player.playShuffled(tracks) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.18f), contentColor = Color.White),
                shape = RoundedCornerShape(22.dp),
            ) {
                Icon(Icons.Rounded.Shuffle, null)
                Text("Shuffle all", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AlbumTile(vm: MainViewModel, a: Album, onClick: () -> Unit) {
    val lead = a.tracks.first()
    val soft = rememberSoft(vm, lead).value
    Column(
        Modifier.width(150.dp).clip(RoundedCornerShape(18.dp)).artBackdrop(soft).clickable(onClick = onClick).padding(10.dp)
    ) {
        Cover(vm, lead, 130.dp, 12.dp, px = 300)
        Spacer(Modifier.height(8.dp))
        Text(a.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(a.artist, color = Color(0xFFD0D0D0), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
