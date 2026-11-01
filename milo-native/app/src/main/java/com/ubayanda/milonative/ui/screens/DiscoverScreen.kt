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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubayanda.milonative.model.PlayerUi
import com.ubayanda.milonative.model.Track
import com.ubayanda.milonative.ui.MainViewModel
import com.ubayanda.milonative.ui.components.Cover
import com.ubayanda.milonative.ui.components.MiloChip
import com.ubayanda.milonative.ui.components.ScreenTitle
import com.ubayanda.milonative.ui.theme.Milo
import com.ubayanda.milonative.ui.theme.Ty
import com.ubayanda.milonative.ui.theme.Sp
import com.ubayanda.milonative.ui.theme.Rad
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

private val RADIO_TAGS = listOf("Top" to "", "Pop" to "pop", "Hip-hop" to "hip hop", "Jazz" to "jazz", "Rock" to "rock", "News" to "news")
private val YT_TERMS = listOf("Trending" to "official music video", "Pop" to "pop music", "Hip-hop" to "hip hop", "Afrobeats" to "afrobeats", "Amapiano" to "amapiano", "Rock" to "rock music", "Lo-fi" to "lofi hip hop", "Jazz" to "jazz")
private val SERVER_VIEWS = listOf("Random mix", "Starred")
private val PREVIEW_TERMS = listOf("Trending" to "top hits", "Pop" to "pop", "Hip-hop" to "hip hop", "Afrobeats" to "afrobeats", "R&B" to "r&b", "Amapiano" to "amapiano")

/** Online listening: live radio and 30-second previews. Nothing is downloaded or saved. */
@Composable
fun DiscoverScreen(vm: MainViewModel, ui: PlayerUi, bottomPad: Dp, onYouTube: (List<Track>, Int) -> Unit) {
    val favs by vm.favs.collectAsState()
    var mode by rememberSaveable { mutableIntStateOf(0) } // 0 = live radio, 1 = previews, 2 = my server
    var query by rememberSaveable { mutableStateOf("") }
    var tag by rememberSaveable { mutableIntStateOf(0) }
    var items by remember { mutableStateOf<List<Track>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf("") }

    LaunchedEffect(mode, tag, query) {
        delay(if (query.isEmpty()) 0 else 450) // wait while the person is still typing
        loading = true
        err = ""
        try {
            items = when (mode) {
                0 -> vm.discover.radio(query, RADIO_TAGS[tag.coerceIn(0, RADIO_TAGS.lastIndex)].second)
                1 -> vm.discover.previews(query.ifBlank { PREVIEW_TERMS[tag.coerceIn(0, PREVIEW_TERMS.lastIndex)].second })
                3 -> vm.youtube.search(query.ifBlank { YT_TERMS[tag.coerceIn(0, YT_TERMS.lastIndex)].second })
                else -> if (!vm.server.configured) emptyList()
                else if (query.isNotBlank()) vm.server.search(query)
                else if (tag == 1) vm.server.starred() else vm.server.random()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            items = emptyList()
            err = if (mode == 2) (e.message ?: "Couldn't reach your server.") else if (mode == 3) (e.message ?: "Couldn't reach YouTube.") else "Couldn't load that. Check your internet connection and try again."
        }
        loading = false
    }

    Column(Modifier.fillMaxSize()) {
        ScreenTitle("Discover")
        Row(Modifier.fillMaxWidth().padding(horizontal = Sp.gutter), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiloChip("Radio", mode == 0, Modifier.weight(1f)) { mode = 0; tag = 0 }
            MiloChip("Previews", mode == 1, Modifier.weight(1f)) { mode = 1; tag = 0 }
            MiloChip("YouTube", mode == 3, Modifier.weight(1f)) { mode = 3; tag = 0 }
            MiloChip("Server", mode == 2, Modifier.weight(1f)) { mode = 2; tag = 0 }
        }
        Spacer(Modifier.height(Sp.x3))
        val shape = RoundedCornerShape(Rad.lg)
        Box(Modifier.padding(horizontal = Sp.gutter).fillMaxWidth().clip(shape).background(Color.Black.copy(alpha = 0.3f), shape).border(1.dp, Milo.line, shape).padding(horizontal = Sp.gutter, vertical = 14.dp)) {
            if (query.isEmpty()) Text(when (mode) { 0 -> "Search radio stations"; 1 -> "Search songs to preview"; 3 -> "Search YouTube"; else -> "Search your server" }, color = Milo.dim, fontSize = 16.sp)
            BasicTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                textStyle = TextStyle(color = Milo.text, fontSize = 16.sp), cursorBrush = SolidColor(Milo.amber),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onAny = {}),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(10.dp))
        val chips = when (mode) { 0 -> RADIO_TAGS.map { it.first }; 1 -> PREVIEW_TERMS.map { it.first }; 3 -> YT_TERMS.map { it.first }; else -> SERVER_VIEWS }
        LazyRow(contentPadding = PaddingValues(horizontal = Sp.gutter), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(chips) { i, label -> MiloChip(label, i == tag && query.isEmpty()) { query = ""; tag = i } }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            when (mode) { 0 -> "Live streams from the free radio-browser directory."; 3 -> "Songs from YouTube, played in YouTube's own player. Audio stops when you leave the app or lock the screen."; 1 -> "30-second previews from iTunes. To hear full songs, add them to your phone."; else -> if (vm.server.configured) "Streaming from " + vm.server.url else "Connect your Navidrome or Subsonic server in Profile." },
            color = Milo.dim, style = Ty.caption, modifier = Modifier.padding(horizontal = Sp.gutter, vertical = Sp.x1),
        )
        when {
            loading && items.isEmpty() -> Text("Loading…", color = Milo.dim, style = Ty.caption, modifier = Modifier.padding(20.dp))
            err.isNotEmpty() -> Text(err, color = Color(0xFFFF8A80), fontSize = 15.sp, modifier = Modifier.padding(20.dp))
            items.isEmpty() && !(mode == 2 && !vm.server.configured) -> Text("Nothing found. Try another search.", color = Milo.dim, style = Ty.caption, modifier = Modifier.padding(20.dp))
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = bottomPad)) {
            itemsIndexed(items, key = { _, t -> t.id }, contentType = { _, _ -> "track" }) { i, t ->
                val on = t.id == ui.currentId
                Row(
                    Modifier.fillMaxWidth().clickable { if (mode == 3) onYouTube(items, i) else vm.playRemote(items, i) }.padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Cover(vm, t, 56.dp, Rad.sm, px = 160)
                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                        Text(t.title, color = if (on) Milo.amber else Milo.text, style = Ty.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            if (mode == 0) t.artist else t.artist + " · " + t.album,
                            color = Milo.dim, style = Ty.caption, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (mode == 0) Text("LIVE", color = Milo.amber, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 6.dp))
                    val liked = favs.contains(t.id)
                    if (mode != 3) androidx.compose.material3.Icon(
                        if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = if (liked) "Remove from Liked" else "Save",
                        tint = if (liked) Milo.amber else Milo.dim,
                        modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape)
                            .clickable { vm.playRemoteKeep(t); vm.toggleFav(t.id) }.padding(9.dp),
                    )
                }
            }
        }
    }
}
