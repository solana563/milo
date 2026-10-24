package com.ubayanda.milonative.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
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
import com.ubayanda.milonative.model.Artist
import com.ubayanda.milonative.model.PlayerUi
import com.ubayanda.milonative.model.Track
import com.ubayanda.milonative.ui.MainViewModel
import com.ubayanda.milonative.ui.components.Cover
import com.ubayanda.milonative.ui.components.EmptyNote
import com.ubayanda.milonative.ui.components.LetterHeader
import com.ubayanda.milonative.ui.components.TrackRow
import com.ubayanda.milonative.ui.theme.Milo
import com.ubayanda.milonative.util.NaturalOrder
import com.ubayanda.milonative.util.plural
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(
    vm: MainViewModel, tracks: List<Track>, albums: List<Album>, artists: List<Artist>, ui: PlayerUi, accent: Color,
    section: Int, onSection: (Int) -> Unit, bottomPad: Dp, onOpenAlbum: (Album) -> Unit, onOpenArtist: (Artist) -> Unit,
) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text("Library", color = Milo.text, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 16.dp, top = 18.dp, bottom = 10.dp))
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
            listOf("Songs", "Albums", "Artists").forEachIndexed { i, label ->
                val on = i == section
                Text(
                    label, color = if (on) Color.Black else Milo.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(if (on) Color.White else Milo.card2)
                        .clickable { onSection(i) }.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
        Spacer(Modifier.padding(top = 8.dp))
        when {
            tracks.isEmpty() -> EmptyNote("No music found", "Songs on your phone, SD card or USB drive show up here automatically.")
            section == 0 -> SongsList(vm, tracks, ui, accent, bottomPad)
            section == 1 -> LazyColumn(contentPadding = PaddingValues(bottom = bottomPad)) {
                items(albums, key = { "al" + it.id + it.title }) { a ->
                    ListRow(vm, a.tracks.first(), a.title, a.artist + " · " + plural(a.tracks.size, "song")) { onOpenAlbum(a) }
                }
            }
            else -> LazyColumn(contentPadding = PaddingValues(bottom = bottomPad)) {
                items(artists, key = { "ar" + it.name }) { a ->
                    ListRow(vm, a.tracks.first(), a.name, plural(a.tracks.size, "song")) { onOpenArtist(a) }
                }
            }
        }
    }
}

@Composable
private fun ListRow(vm: MainViewModel, lead: Track, title: String, sub: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Cover(vm, lead, 52.dp, 10.dp, px = 120)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Milo.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sub, color = Milo.dim, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Songs A to Z with letter headers and a tappable letter rail on the right. */
@Composable
private fun SongsList(vm: MainViewModel, tracks: List<Track>, ui: PlayerUi, accent: Color, bottomPad: Dp) {
    val state: LazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // flat rows: a header whenever the first letter changes
    val rows = ArrayList<Any>()
    val letterIndex = LinkedHashMap<String, Int>()
    var last = ""
    for (t in tracks) {
        val l = NaturalOrder.letter(t.title)
        if (l != last) { letterIndex[l] = rows.size; rows.add(l); last = l }
        rows.add(t)
    }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(state = state, contentPadding = PaddingValues(bottom = bottomPad)) {
            itemsIndexed(rows, key = { _, r -> if (r is Track) "t" + r.id else "h$r" }) { _, r ->
                if (r is Track) TrackRow(vm, r, r.id == ui.currentId, accent) { vm.player.playQueue(tracks, tracks.indexOf(r)) }
                else LetterHeader(r as String, accent)
            }
        }
        Column(
            Modifier.align(Alignment.CenterEnd).fillMaxHeight(0.7f).padding(end = 2.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
        ) {
            letterIndex.keys.forEach { l ->
                Text(
                    l, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { scope.launch { state.scrollToItem(letterIndex[l] ?: 0) } }.padding(horizontal = 6.dp),
                )
            }
        }
    }
}
