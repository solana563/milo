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
import androidx.compose.material.icons.rounded.Download
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
import com.ubayanda.milonative.ui.components.lift
import com.ubayanda.milonative.ui.components.pressable
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Radio
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val RADIO_TAGS = listOf("Top" to "", "Pop" to "pop", "Hip-hop" to "hip hop", "Jazz" to "jazz", "Rock" to "rock", "News" to "news")
private val YT_TERMS = listOf("Trending" to "official music video", "Pop" to "pop music", "Hip-hop" to "hip hop", "Afrobeats" to "afrobeats", "Amapiano" to "amapiano", "Rock" to "rock music", "Lo-fi" to "lofi hip hop", "Jazz" to "jazz")
private val SERVER_VIEWS = listOf("Random mix", "Starred")
private val PREVIEW_TERMS = listOf("Trending" to "top hits", "Pop" to "pop", "Hip-hop" to "hip hop", "Afrobeats" to "afrobeats", "R&B" to "r&b", "Amapiano" to "amapiano")

private fun chartTitle(region: Int) = when (region) {
    com.ubayanda.milonative.data.Region.LOCAL -> "Top songs in " + com.ubayanda.milonative.data.Region.homeName
    com.ubayanda.milonative.data.Region.WORLD -> "Top songs internationally"
    else -> "Top songs, local and international"
}

/** Reads the real charts, then finds each song, artist and album on YouTube Music. Returns null when no chart could be read, so the caller can fall back to searching. */
private suspend fun chartFeed(
    scope: kotlinx.coroutines.CoroutineScope, vm: MainViewModel, region: Int,
    setArtists: (List<com.ubayanda.milonative.data.YtMusic.Card>) -> Unit, setAlbums: (List<com.ubayanda.milonative.data.YtMusic.Card>) -> Unit,
): List<Track>? {
    val countries = com.ubayanda.milonative.data.Region.chartCountries(region)
    val (songCharts, albumCharts) = coroutineScope {
        countries.map { c -> async { vm.discover.chart(c, "songs", 12) } }.awaitAll() to countries.map { c -> async { vm.discover.chart(c, "albums", 8) } }.awaitAll()
    }
    val songs = com.ubayanda.milonative.data.Region.mix(songCharts) { it.name.lowercase() + "|" + it.artist.lowercase() }
    if (songs.isEmpty()) return null
    scope.launch {
        val names = songs.map { it.artist.split(Regex(",| & | feat\\.? | ft\\.? ", RegexOption.IGNORE_CASE)).first().trim() }.filter { it.isNotBlank() }.distinct().take(10)
        setArtists(coroutineScope { names.map { n -> async { vm.youtube.artists(n).firstOrNull() } }.awaitAll() }.filterNotNull().distinctBy { it.id })
    }
    scope.launch {
        val albums = com.ubayanda.milonative.data.Region.mix(albumCharts) { it.name.lowercase() + "|" + it.artist.lowercase() }.take(10)
        setAlbums(coroutineScope { albums.map { a -> async { vm.youtube.albums(a.name + " " + a.artist).firstOrNull() } }.awaitAll() }.filterNotNull().distinctBy { it.id })
    }
    return coroutineScope { songs.take(20).map { s -> async { vm.youtube.search(s.name + " " + s.artist).firstOrNull() } }.awaitAll() }.filterNotNull().distinctBy { it.id }
}

private fun feedTitle(label: String) = when (label) { "Trending" -> "Trending songs"; "New releases" -> "Latest songs"; else -> "$label songs" }

/** Online listening: live radio and 30-second previews. Nothing is downloaded or saved. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(vm: MainViewModel, ui: PlayerUi, bottomPad: Dp, onYouTube: (List<Track>, Int) -> Unit, onYtPage: (String) -> Unit = {}) {
    val favs by vm.favs.collectAsState()
    val rctx = androidx.compose.ui.platform.LocalContext.current
    remember { com.ubayanda.milonative.data.Region.init(rctx) }
    val region by com.ubayanda.milonative.data.Region.mode.collectAsState()
    val rChips = remember(region) { com.ubayanda.milonative.data.Region.chips(region) }
    var mode by vm.mem::dMode // 3 = the YouTube feed (opens here), 0 = live radio, 1 = previews, 2 = my server
    var sources by remember { mutableStateOf(false) }
    var query by vm.mem::dQuery
    var tag by vm.mem::dTag
    var items by vm.mem::dItems
    var loading by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf("") }
    var yArtists by vm.mem::dArtists
    var yAlbums by vm.mem::dAlbums
    var chartMode by vm.mem::dChart
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var tick by vm.mem::dTick
    var refreshing by remember { mutableStateOf(false) }
    val discState = vm.mem.list("discover-$mode")
    LaunchedEffect(Unit) { vm.scrollTop.collect { discState.animateScrollToItem(0) } }

    LaunchedEffect(mode, tag, query, region, tick) {
        // coming back from an artist or album: the results are still here, so keep them (and the scroll position)
        val fkey = "$mode|$tag|$query|$region|$tick"
        if (vm.mem.dKey == fkey && (items.isNotEmpty() || yArtists.isNotEmpty() || yAlbums.isNotEmpty())) { loading = false; refreshing = false; return@LaunchedEffect }
        delay(if (query.isEmpty()) 0 else 450) // wait while the person is still typing
        loading = true
        err = ""
        try {
            items = when (mode) {
                0 -> vm.discover.radio(query, RADIO_TAGS[tag.coerceIn(0, RADIO_TAGS.lastIndex)].second, com.ubayanda.milonative.data.Region.radioCountry(region))
                1 -> {
                    val qs = if (query.isNotBlank()) com.ubayanda.milonative.data.Region.forSearch(query.trim(), region) else rChips[tag.coerceIn(0, rChips.lastIndex)].qs
                    com.ubayanda.milonative.data.Region.mix(qs.map { vm.discover.previews(it.term, it.country) }) { it.id }
                }
                3 -> {
                    yArtists = emptyList(); yAlbums = emptyList()
                    // the front page is a real chart (Apple Music's most played for the chosen countries), not a search
                    val ch = if (query.isBlank() && tag == 0) chartFeed(this, vm, region, { yArtists = it }, { yAlbums = it }) else null
                    chartMode = ch != null
                    if (ch != null) ch else run {
                    // artists and albums appear above the songs when they arrive
                    val qs = if (query.isNotBlank()) com.ubayanda.milonative.data.Region.forSearch(query.trim(), region) else rChips[tag.coerceIn(0, rChips.lastIndex)].qs
                    if (query.isNotBlank()) {
                        launch { yArtists = vm.youtube.artists(query) }
                        launch { yAlbums = vm.youtube.albums(query) }
                    } else {
                        // the feed: new releases, and the artists behind the trending songs
                        launch {
                            yAlbums = com.ubayanda.milonative.data.Region.mix(com.ubayanda.milonative.data.Region.albumQs(region).map { q ->
                                com.ubayanda.milonative.data.Region.inCountry(q.country) { vm.youtube.albums(q.term) }
                            }) { it.id }
                        }
                    }
                    val songs = com.ubayanda.milonative.data.Region.mix(qs.map { q ->
                        com.ubayanda.milonative.data.Region.inCountry(q.country) { vm.youtube.search(q.term) }
                    }) { it.id }
                    if (query.isBlank()) launch {
                        val cc = qs.firstOrNull()?.country.orEmpty()
                        val names = songs.map { it.artist }.filter { it.isNotBlank() && it != "Unknown artist" }.distinct().take(10)
                        for (n in names) {
                            val c = com.ubayanda.milonative.data.Region.inCountry(cc) { vm.youtube.artists(n).firstOrNull() }
                            if (c != null && yArtists.none { it.id == c.id }) yArtists = yArtists + c
                        }
                    }
                    songs
                    }
                }
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
        if (err.isEmpty()) vm.mem.dKey = fkey
        discState.scrollToItem(0) // a new search starts at the top
        loading = false
        refreshing = false
    }

    Column(Modifier.fillMaxSize()) {
        ScreenTitle("Discover")
        Row(Modifier.fillMaxWidth().padding(horizontal = Sp.gutter), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            // the radio button is big and always there: tap it for stations, tap again to go back to the feed
            val rShape = RoundedCornerShape(Rad.lg)
            Row(
                Modifier.weight(1f).height(56.dp).lift(rShape, 12f).clip(rShape)
                    .background(if (mode == 0) androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Milo.amber, Milo.amber.copy(alpha = 0.85f))) else androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Milo.card2, Milo.card)), rShape)
                    .border(1.dp, com.ubayanda.milonative.ui.components.glassStroke(1f), rShape)
                    .pressable(true) { query = ""; tag = 0; mode = if (mode == 0) 3 else 0 }.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.material3.Icon(Icons.Rounded.Radio, null, tint = if (mode == 0) Color.Black else Milo.amber, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Live radio", color = if (mode == 0) Color.Black else Milo.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(if (mode == 0) "Tap to go back to the feed" else "Stations from around the world", color = if (mode == 0) Color.Black.copy(alpha = 0.7f) else Milo.dim, style = Ty.micro)
                }
                Text("LIVE", color = if (mode == 0) Color.Black else Milo.amber, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
            }
            Box(
                Modifier.size(56.dp).clip(RoundedCornerShape(Rad.lg)).background(if (mode == 1 || mode == 2) Milo.amber.copy(alpha = 0.25f) else Milo.card2)
                    .border(1.dp, com.ubayanda.milonative.ui.components.glassStroke(1f), RoundedCornerShape(Rad.lg))
                    .pressable(true) { sources = true },
                contentAlignment = Alignment.Center,
            ) { androidx.compose.material3.Icon(Icons.Rounded.MoreHoriz, "More sources", tint = Milo.text, modifier = Modifier.size(26.dp)) }
        }
        if (sources) com.ubayanda.milonative.ui.components.MiloPopup({ sources = false }) {
            Text("Sources", color = Milo.text, style = Ty.title, modifier = Modifier.padding(start = 14.dp, bottom = 8.dp))
            com.ubayanda.milonative.ui.components.PopupAction(Icons.Rounded.Home, "YouTube feed") { sources = false; query = ""; tag = 0; mode = 3 }
            com.ubayanda.milonative.ui.components.PopupAction(Icons.Rounded.PlayArrow, "30-second previews (iTunes)") { sources = false; query = ""; tag = 0; mode = 1 }
            com.ubayanda.milonative.ui.components.PopupAction(Icons.Rounded.Storage, "My server (Navidrome / Subsonic)") { sources = false; query = ""; tag = 0; mode = 2 }
        }
        if (mode != 2) {
            Spacer(Modifier.height(Sp.x3))
            com.ubayanda.milonative.ui.components.GlassSegmented(
                listOf("Local \u00b7 " + com.ubayanda.milonative.data.Region.home, "Both", "International"), region,
                { tag = 0; com.ubayanda.milonative.data.Region.set(it) }, Modifier.padding(horizontal = Sp.gutter),
            )
        }
        Spacer(Modifier.height(Sp.x3))
        val shape = RoundedCornerShape(Rad.lg)
        Box(Modifier.padding(horizontal = Sp.gutter).fillMaxWidth().clip(shape).background(Color.Black.copy(alpha = 0.3f), shape).border(1.dp, Milo.line, shape).padding(horizontal = Sp.gutter, vertical = 14.dp)) {
            if (query.isEmpty()) Text(when (mode) { 0 -> "Search radio stations"; 1 -> "Search songs to preview"; 3 -> "Search songs, artists, albums"; else -> "Search your server" }, color = Milo.dim, fontSize = 16.sp)
            BasicTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                textStyle = TextStyle(color = Milo.text, fontSize = 16.sp), cursorBrush = SolidColor(Milo.amber),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onAny = {}),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(10.dp))
        val chips = when (mode) { 0 -> RADIO_TAGS.map { it.first }; 1, 3 -> rChips.map { it.label }; else -> SERVER_VIEWS }
        LazyRow(contentPadding = PaddingValues(horizontal = Sp.gutter), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(chips) { i, label -> MiloChip(label, i == tag && query.isEmpty()) { query = ""; tag = i } }
        }
        Spacer(Modifier.height(8.dp))
        if (mode != 3) Text(
            when (mode) { 0 -> "Live streams from the free radio-browser directory."; 1 -> "30-second previews from iTunes. To hear full songs, add them to your phone."; else -> if (vm.server.configured) "Streaming from " + vm.server.url else "Connect your Navidrome or Subsonic server in Profile." },
            color = Milo.dim, style = Ty.caption, modifier = Modifier.padding(horizontal = Sp.gutter, vertical = Sp.x1),
        )
        when {
            loading && items.isEmpty() -> com.ubayanda.milonative.ui.components.SkeletonRows(7)
            err.isNotEmpty() -> Text(err, color = Color(0xFFFF8A80), fontSize = 15.sp, modifier = Modifier.padding(20.dp))
            items.isEmpty() && !(mode == 2 && !vm.server.configured) -> Text("Nothing found. Try another search.", color = Milo.dim, style = Ty.caption, modifier = Modifier.padding(20.dp))
        }
        com.ubayanda.milonative.ui.components.BouncyRefresh(refreshing = refreshing, onRefresh = { refreshing = true; tick++ }, modifier = Modifier.fillMaxSize()) {
    LazyColumn(Modifier.fillMaxSize(), state = discState, contentPadding = PaddingValues(bottom = bottomPad)) {
            if (mode == 3 && yArtists.isNotEmpty()) item(key = "yar") { YtShelf(if (query.isBlank()) "Trending artists" else "Artists", yArtists, vm) { onYtPage(it.id) } }
            if (mode == 3 && yAlbums.isNotEmpty()) item(key = "yal") { YtShelf(if (chartMode) "Top albums" else if (query.isBlank()) "New releases" else "Albums", yAlbums, vm) { onYtPage(it.id) } }
            if (mode == 3 && items.isNotEmpty() && (yArtists.isNotEmpty() || yAlbums.isNotEmpty() || query.isBlank())) item(key = "ysg") {
                Text(if (chartMode) chartTitle(region) else if (query.isBlank()) feedTitle(rChips[tag.coerceIn(0, rChips.lastIndex)].label) else "Songs", color = Milo.text, style = Ty.heading, modifier = Modifier.padding(start = Sp.gutter, end = Sp.gutter, top = Sp.x4, bottom = Sp.x1))
            }
            itemsIndexed(items, key = { _, t -> t.id }, contentType = { _, _ -> "track" }) { i, t ->
                val on = t.id == ui.currentId
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = if (on) 8.dp else 0.dp)
                        .then(if (on) Modifier.clip(RoundedCornerShape(Rad.lg)).background(Milo.amber.copy(alpha = 0.16f)) else Modifier).clickable {
                        // a song you searched for plays alone in the queue and Autoplay adds similar songs; the trending feed keeps its list
                        val one = query.isNotBlank() && mode != 0
                        val q = if (one) listOf(t) else items
                        val qi = if (one) 0 else i
                        if (mode == 3) scope.launch {
                            // plays in milo.'s own player; if no direct stream can be found, the old embedded player is the way out
                            if (vm.youtube.canStream(t)) vm.playRemote(q, qi) else onYouTube(q, qi)
                        } else vm.playRemote(q, qi)
                    }.padding(horizontal = 20.dp, vertical = 8.dp),
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
                    if (mode == 3 && t.uri.startsWith("yt:")) androidx.compose.material3.Icon(
                        Icons.Rounded.Download, "Download for offline", tint = Milo.dim,
                        modifier = Modifier.size(48.dp).clip(androidx.compose.foundation.shape.CircleShape).clickable { vm.downloadTracks(listOf(t)) }.padding(13.dp),
                    )
                }
            }
        }
        }
    }
}
