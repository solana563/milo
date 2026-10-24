package com.ubayanda.milonative.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.ubayanda.milonative.ui.components.ScreenTitle
import com.ubayanda.milonative.ui.components.SectionTitle
import com.ubayanda.milonative.ui.components.TrackRow
import com.ubayanda.milonative.ui.theme.Milo
import com.ubayanda.milonative.util.plural

@Composable
fun SearchScreen(
    vm: MainViewModel, tracks: List<Track>, albums: List<Album>, artists: List<Artist>, ui: PlayerUi, bottomPad: Dp,
    onOpenAlbum: (Album) -> Unit, onOpenArtist: (Artist) -> Unit,
) {
    var q by rememberSaveable { mutableStateOf("") }
    val s = q.trim()
    val hits = remember(s, tracks) {
        if (s.isEmpty()) tracks
        else tracks.filter { it.title.contains(s, true) || it.artist.contains(s, true) || it.album.contains(s, true) }
    }
    val albumHits = remember(s, albums) { if (s.isEmpty()) emptyList() else albums.filter { it.title.contains(s, true) || it.artist.contains(s, true) } }
    val artistHits = remember(s, artists) { if (s.isEmpty()) emptyList() else artists.filter { it.name.contains(s, true) } }
    Column(Modifier.fillMaxSize()) {
        ScreenTitle("Find something to sit with")
        OutlinedTextField(
            value = q, onValueChange = { q = it }, singleLine = true,
            placeholder = { Text("Songs, albums or artists", color = Milo.dim, fontSize = 17.sp) },
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Milo.card, unfocusedContainerColor = Milo.card,
                focusedBorderColor = Milo.amber, unfocusedBorderColor = Milo.line,
                cursorColor = Milo.amber, focusedTextColor = Milo.text, unfocusedTextColor = Milo.text,
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        )
        LazyColumn(contentPadding = PaddingValues(top = 6.dp, bottom = bottomPad)) {
            if (s.isNotEmpty() && hits.isEmpty() && albumHits.isEmpty() && artistHits.isEmpty()) item { EmptyNote("Nothing found", "Try a different spelling.") }
            if (hits.isNotEmpty()) item { SectionTitle("Songs") }
            items(hits.take(if (s.isEmpty()) 200 else 50), key = { "s" + it.id }) { t ->
                TrackRow(vm, t, t.id == ui.currentId, Milo.amber) { vm.player.playQueue(hits, hits.indexOf(t)) }
            }
            if (albumHits.isNotEmpty()) item { SectionTitle("Albums") }
            items(albumHits.take(20), key = { "al" + it.id + it.title }) { a ->
                Row(Modifier.fillMaxWidth().clickable { onOpenAlbum(a) }.padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Cover(vm, a.tracks.first(), 54.dp, 14.dp, px = 160)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(a.title, color = Milo.text, fontSize = 17.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(a.artist + " · " + plural(a.tracks.size, "song"), color = Milo.dim, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            if (artistHits.isNotEmpty()) item { SectionTitle("Artists") }
            items(artistHits.take(20), key = { "ar" + it.name }) { a ->
                Row(Modifier.fillMaxWidth().clickable { onOpenArtist(a) }.padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Cover(vm, a.tracks.first(), 54.dp, 27.dp, px = 160)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(a.name, color = Milo.text, fontSize = 17.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(plural(a.tracks.size, "song"), color = Milo.dim, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
