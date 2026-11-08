package com.ubayanda.milonative.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.rounded.Search
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
import com.ubayanda.milonative.ui.components.ScreenTitle
import com.ubayanda.milonative.ui.components.SectionTitle
import com.ubayanda.milonative.ui.components.TrackRow
import com.ubayanda.milonative.ui.theme.Milo
import com.ubayanda.milonative.ui.theme.Ty
import com.ubayanda.milonative.ui.theme.Sp
import com.ubayanda.milonative.ui.theme.Rad
import com.ubayanda.milonative.util.plural

@Composable
fun SearchScreen(
    vm: MainViewModel, tracks: List<Track>, albums: List<Album>, artists: List<Artist>, ui: PlayerUi, bottomPad: Dp,
    onOpenAlbum: (Album) -> Unit, onOpenArtist: (Artist) -> Unit,
    onOpenPlaylist: (com.ubayanda.milonative.model.Playlist) -> Unit = {}, onOpenFriend: (com.ubayanda.milonative.data.Friend) -> Unit = {},
) {
    var q by vm.mem::sQuery
    val s = q.trim()
    val ctx = LocalContext.current
    val focus = LocalFocusManager.current
    remember { com.ubayanda.milonative.util.RecentSearches.init(ctx) }
    var recents by remember { mutableStateOf(com.ubayanda.milonative.util.RecentSearches.all()) }
    // online with "hide my phone's music" on: an empty search page is not a dump of the phone library, and YouTube results come first
    val hideLocal by com.ubayanda.milonative.data.Region.hideLocal.collectAsState()
    val ytFirst = hideLocal && vm.online()
    fun done() { com.ubayanda.milonative.util.RecentSearches.add(q); recents = com.ubayanda.milonative.util.RecentSearches.all() }
    val hits = remember(s, tracks, ytFirst) {
        if (s.isEmpty()) (if (ytFirst) emptyList() else tracks)
        else tracks.filter { it.title.contains(s, true) || it.artist.contains(s, true) || it.album.contains(s, true) }
    }
    val albumHits = remember(s, albums) { if (s.isEmpty()) emptyList() else albums.filter { it.title.contains(s, true) || it.artist.contains(s, true) } }
    val artistHits = remember(s, artists) { if (s.isEmpty()) emptyList() else artists.filter { it.name.contains(s, true) } }
    val pls by vm.playlists.collectAsState()
    val soc by vm.social.ui.collectAsState()
    val plHits = remember(s, pls) { if (s.isEmpty()) emptyList() else pls.filter { it.name.contains(s, true) } }
    val frHits = remember(s, soc.friends) {
        if (s.isEmpty()) emptyList() else soc.friends.filter { it.status == "friend" && (it.name.contains(s, true) || it.username.contains(s, true)) }
    }
    var ytHits by vm.mem::sYt
    var lyHits by vm.mem::sLy
    var ytBusy by remember { mutableStateOf(false) }
    LaunchedEffect(s) {
        if (s.length < 2) { ytHits = emptyList(); lyHits = emptyList(); ytBusy = false; vm.mem.sKey = ""; return@LaunchedEffect }
        // coming back from an artist or album: the results are still here, so leave them (and the scroll position) alone
        if (vm.mem.sKey == s && (ytHits.isNotEmpty() || lyHits.isNotEmpty())) return@LaunchedEffect
        kotlinx.coroutines.delay(450)
        vm.mem.list("search").scrollToItem(0)
        lyHits = vm.lyricSearch(s)
        if (vm.online()) { ytBusy = true; ytHits = vm.ytSearch(s); ytBusy = false } else ytHits = emptyList()
        vm.mem.sKey = s
    }
    val lyricQuery = s.trim().split(" ").size >= 3
    val ytSection: LazyListScope.() -> Unit = {
        if (ytHits.isNotEmpty() || ytBusy) item(key = "yt-head") { SectionTitle(if (lyricQuery) "On YouTube Music \u00b7 matches for those lyrics" else "On YouTube Music") }
        if (ytBusy && ytHits.isEmpty()) item(key = "yt-busy") { Text("Searching\u2026", color = Milo.dim, style = Ty.caption, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }
        items(ytHits, key = { "yt" + it.id }, contentType = { "track" }) { t ->
            TrackRow(vm, t, t.id == ui.currentId, Milo.amber) { done(); vm.playRemote(listOf(t), 0) }  // alone in the queue: Autoplay then adds similar songs
        }
    }
    Column(Modifier.fillMaxSize()) {
        ScreenTitle("Find something to sit with")
        OutlinedTextField(
            value = q, onValueChange = { q = it }, singleLine = true,
            placeholder = { Text("Songs, artists, friends or a lyric you remember", color = Milo.dim, fontSize = 17.sp) },
            shape = RoundedCornerShape(50),
            leadingIcon = { androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Rounded.Search, null, tint = Milo.dim) },
            trailingIcon = { if (q.isNotEmpty()) Icon(Icons.Rounded.Close, "Clear search", tint = Milo.dim, modifier = Modifier.size(44.dp).clip(androidx.compose.foundation.shape.CircleShape).clickable { q = "" }.padding(10.dp)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { done(); focus.clearFocus() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0x59000000), unfocusedContainerColor = Color(0x59000000),
                focusedBorderColor = Milo.amber, unfocusedBorderColor = Milo.line,
                cursorColor = Milo.amber, focusedTextColor = Milo.text, unfocusedTextColor = Milo.text,
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        )
        LazyColumn(state = vm.mem.list("search"), contentPadding = PaddingValues(top = 6.dp, bottom = bottomPad)) {
            if (s.isEmpty() && recents.isNotEmpty()) {
                item(key = "recent-head") {
                    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Recent searches", color = Milo.text, style = Ty.title, modifier = Modifier.weight(1f))
                        Text("Clear", color = Milo.amber, style = Ty.caption, modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { com.ubayanda.milonative.util.RecentSearches.clear(); recents = emptyList() }.padding(horizontal = 10.dp, vertical = 8.dp))
                    }
                }
                items(recents, key = { "rc" + it }) { w ->
                    Row(Modifier.fillMaxWidth().clickable { q = w }.padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.History, null, tint = Milo.dim, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(14.dp))
                        Text(w, color = Milo.text, style = Ty.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            if (ytFirst) ytSection()
            if (s.isNotEmpty() && hits.isEmpty() && albumHits.isEmpty() && artistHits.isEmpty() && plHits.isEmpty() && frHits.isEmpty() && lyHits.isEmpty() && ytHits.isEmpty() && !ytBusy) item { EmptyNote("Nothing found", "Try a different spelling.") }
            if (hits.isNotEmpty()) item { SectionTitle("Songs") }
            items(hits.take(if (s.isEmpty()) 200 else 50), key = { "s" + it.id }, contentType = { "track" }) { t ->
                TrackRow(vm, t, t.id == ui.currentId, Milo.amber) { vm.player.playQueue(hits, hits.indexOf(t)) }
            }
            if (albumHits.isNotEmpty()) item { SectionTitle("Albums") }
            items(albumHits.take(20), key = { "al" + it.id + it.title }) { a ->
                Row(Modifier.fillMaxWidth().clickable { onOpenAlbum(a) }.padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Cover(vm, a.tracks.first(), 56.dp, Rad.sm, px = 160)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(a.title, color = Milo.text, style = Ty.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(a.artist + " · " + plural(a.tracks.size, "song"), color = Milo.dim, style = Ty.caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            if (lyHits.isNotEmpty()) item { SectionTitle("In your lyrics") }
            items(lyHits, key = { "ly" + it.first.id }) { (t, line) ->
                Column(Modifier.fillMaxWidth().clickable { vm.player.playQueue(listOf(t), 0) }.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Text(t.title + " \u00b7 " + t.artist, color = Milo.text, style = Ty.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("\u201c" + line + "\u201d", color = Milo.amber, style = Ty.caption, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            if (plHits.isNotEmpty()) item { SectionTitle("Playlists") }
            items(plHits.take(10), key = { "pl" + it.id }) { p ->
                Column(Modifier.fillMaxWidth().clickable { onOpenPlaylist(p) }.padding(horizontal = 20.dp, vertical = 10.dp)) {
                    Text(p.name, color = Milo.text, style = Ty.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(plural(p.trackIds.size, "song"), color = Milo.dim, style = Ty.caption)
                }
            }
            if (frHits.isNotEmpty()) item { SectionTitle("Friends") }
            items(frHits.take(10), key = { "fr" + it.id }) { f ->
                Column(Modifier.fillMaxWidth().clickable { onOpenFriend(f) }.padding(horizontal = 20.dp, vertical = 10.dp)) {
                    Text(f.name, color = Milo.text, style = Ty.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("@" + f.username, color = Milo.dim, style = Ty.caption)
                }
            }
            if (!ytFirst) ytSection()
            if (artistHits.isNotEmpty()) item { SectionTitle("Artists") }
            items(artistHits.take(20), key = { "ar" + it.name }) { a ->
                Row(Modifier.fillMaxWidth().clickable { onOpenArtist(a) }.padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Cover(vm, a.tracks.first(), 56.dp, 28.dp, px = 160)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(a.name, color = Milo.text, style = Ty.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(plural(a.tracks.size, "song"), color = Milo.dim, style = Ty.caption)
                    }
                }
            }
        }
    }
}
