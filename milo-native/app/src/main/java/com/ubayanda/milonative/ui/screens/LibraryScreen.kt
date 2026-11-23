package com.ubayanda.milonative.ui.screens

import com.ubayanda.milonative.ui.theme.MiloIcons
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.ubayanda.milonative.ui.components.glass
import com.ubayanda.milonative.ui.components.lift
import com.ubayanda.milonative.ui.theme.Milo
import com.ubayanda.milonative.util.NaturalOrder
import com.ubayanda.milonative.util.plural
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(
    vm: MainViewModel, tracks: List<Track>, albums: List<Album>, artists: List<Artist>, playlists: List<Playlist>,
    likedCount: Int, ui: PlayerUi, section: Int, onSection: (Int) -> Unit, bottomPad: Dp,
    onOpenAlbum: (Album) -> Unit, onOpenArtist: (Artist) -> Unit, onOpenPlaylist: (Playlist) -> Unit, onLiked: () -> Unit, onSmart: (String) -> Unit = {},
    /** true = shuffle, false = play in order. */
    onPlayLiked: (Boolean) -> Unit = {},
) {
    val savedCount = vm.offline.collectAsState().value.size
    Column(Modifier.fillMaxSize()) {
        ScreenTitle("Your library")
        if (likedCount > 0 && section != 3) LikedBanner(likedCount, onLiked, { onPlayLiked(false) }, { onPlayLiked(true) })
        com.ubayanda.milonative.ui.components.GlassSegmented(listOf("Songs", "Albums", "Artists", "Playlists"), section, onSection, Modifier.padding(horizontal = 18.dp))
        Spacer(Modifier.padding(top = 10.dp))
        if (savedCount > 0) IconRow(MiloIcons.Download, true, "Downloads", com.ubayanda.milonative.data.OfflineStore.summary().ifEmpty { plural(savedCount, "song") } + " · play with no connection", { onSmart("downloads") })
        when {
            section == 3 -> PlaylistsList(vm, playlists, tracks, likedCount, bottomPad, onOpenPlaylist, onLiked, onSmart)
            tracks.isEmpty() -> EmptyNote("No music found", "Songs on your phone, SD card or USB drive show up here automatically.")
            section == 0 -> SongsList(vm, tracks, ui, bottomPad)
            section == 1 -> LazyVerticalGrid(
                state = vm.mem.grid("lib-albums"), columns = GridCells.Fixed(2), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = bottomPad),
                horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                gridItems(albums, key = { "al" + it.id + it.title }, contentType = { "album" }) { a ->
                    GridCard(vm, a.tracks.first(), a.title, a.artist + " · " + plural(a.tracks.size, "song"), false) { onOpenAlbum(a) }
                }
            }
            else -> LazyVerticalGrid(
                state = vm.mem.grid("lib-artists"), columns = GridCells.Fixed(2), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = bottomPad),
                horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                gridItems(artists, key = { "ar" + it.name }, contentType = { "artist" }) { a ->
                    GridCard(vm, a.tracks.first(), a.name, plural(a.tracks.size, "song"), true) { onOpenArtist(a) }
                }
            }
        }
    }
}

/** Your favourites up front: one tap to play or shuffle them, or open the list. */
@Composable
private fun LikedBanner(count: Int, onOpen: () -> Unit, onPlay: () -> Unit, onShuffle: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp).clip(shape)
            .lift(shape, 10f).glass(shape)
            .clickable(onClick = onOpen).padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(Milo.text.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
            Icon(MiloIcons.Favorite, null, tint = Milo.amber, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("YOUR FAVOURITES", color = Milo.dim, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
            Text("Liked songs", color = Milo.text, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(plural(count, "song"), color = Milo.dim, fontSize = 13.sp, maxLines = 1)
        }
        Box(Modifier.size(48.dp).clip(androidx.compose.foundation.shape.CircleShape).clickable(onClick = onShuffle), contentAlignment = Alignment.Center) {
            Icon(MiloIcons.Shuffle, "Shuffle liked songs", tint = Milo.text, modifier = Modifier.size(24.dp))
        }
        Box(Modifier.size(52.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Milo.cream).clickable(onClick = onPlay), contentAlignment = Alignment.Center) {
            Icon(MiloIcons.PlayArrow, "Play liked songs", tint = Color.Black, modifier = Modifier.size(30.dp))
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
            Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(Milo.text.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = if (hot) Milo.amber else Milo.text, modifier = Modifier.size(24.dp)) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Milo.text, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sub, color = Milo.dim, fontSize = 13.sp, maxLines = 1)
        }
    }
}

@Composable
private fun PlaylistsList(
    vm: MainViewModel, playlists: List<Playlist>, allTracks: List<Track>, likedCount: Int, bottomPad: Dp,
    onOpen: (Playlist) -> Unit, onLiked: () -> Unit, onSmart: (String) -> Unit,
) {
    var creating by remember { mutableStateOf(false) }
    var linking by remember { mutableStateOf(false) }
    var history by remember { mutableStateOf(false) }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val linkScope = rememberCoroutineScope()
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = try { ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: "" } catch (e: Exception) { "" }
            val name = (ctx.contentResolver.query(uri, null, null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(c.getColumnIndexOrThrow("_display_name")) else null } ?: "Imported playlist").substringBeforeLast('.')
            val isCsv = name.isNotEmpty() && (ctx.contentResolver.query(uri, null, null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(c.getColumnIndexOrThrow("_display_name")).orEmpty().endsWith(".csv", true) else false } ?: false)
            val entries = if (isCsv) com.ubayanda.milonative.util.CsvPlaylist.parse(text) else com.ubayanda.milonative.util.M3u.parse(text)
            android.widget.Toast.makeText(ctx, "Matching " + entries.size + " songs\u2026", android.widget.Toast.LENGTH_SHORT).show()
            linkScope.launch {
                val r = try { vm.importEntries(name, entries) } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { null }
                val msg = when {
                    r == null -> "Couldn't import that file."
                    r.onPhone + r.fromYoutube == 0 -> "None of those " + r.total + " songs were found."
                    else -> "Imported " + (r.onPhone + r.fromYoutube) + " of " + r.total + " songs (" + r.onPhone + " on this phone, " + r.fromYoutube + " from YouTube Music)" + (if (r.missing > 0) ". " + r.missing + " not found." else ".")
                }
                android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
    val byId = remember(allTracks) { allTracks.associateBy { it.id } }
    LazyColumn(state = vm.mem.list("lib-playlists"), contentPadding = PaddingValues(bottom = bottomPad)) {
        item { IconRow(MiloIcons.Add, false, "New playlist", "Make your own mix", { creating = true }) }
        item { IconRow(MiloIcons.Add, false, "Import a playlist", "An .m3u or .csv file; songs not on the phone are found on YouTube Music", { picker.launch(arrayOf("*/*")) }) }
        item { IconRow(MiloIcons.QueueMusic, false, "Import history", "What was imported and what wasn't found", { history = true }) }
        item { IconRow(MiloIcons.Add, false, "Import from a link", "A public YouTube Music playlist", { linking = true }) }
        item { IconRow(MiloIcons.Favorite, true, "Liked songs", plural(likedCount, "song"), onLiked) }
        item { IconRow(MiloIcons.QueueMusic, false, "Most played", "Built from your listening", { onSmart("most") }) }
        item { IconRow(MiloIcons.QueueMusic, false, "Recently added", "Your newest songs", { onSmart("recent") }) }
        item { IconRow(MiloIcons.QueueMusic, false, "Never played", "Songs waiting for you", { onSmart("never") }) }
        items(playlists, key = { it.id }) { p ->
            val covers = remember(p.trackIds, byId) { p.trackIds.mapNotNull { byId[it] } }
            Row(Modifier.fillMaxWidth().clickable { onOpen(p) }.padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                com.ubayanda.milonative.ui.components.CoverCollage(vm, covers, 52.dp, 14.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.name, color = Milo.text, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(plural(p.trackIds.size, "song"), color = Milo.dim, fontSize = 13.sp, maxLines = 1)
                }
            }
        }
        if (playlists.isEmpty()) item { EmptyNote("No playlists yet", "Tap New playlist, or use the ⋮ menu on any song and choose Add to playlist.") }
    }
    if (history) {
        val log = remember { com.ubayanda.milonative.data.ImportLog.all(ctx) }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { history = false },
            title = { Text("Import history") },
            text = {
                Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                    if (log.isEmpty()) Text("Nothing imported yet.", color = Milo.dim, fontSize = 14.sp)
                    log.forEach { e ->
                        Text(e.name, color = Milo.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp))
                        Text(
                            java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT).format(java.util.Date(e.at)) + " \u00b7 " +
                                e.onPhone + " on this phone, " + e.fromYoutube + " from YouTube Music, " + e.missing.size + " not found",
                            color = Milo.dim, fontSize = 12.sp,
                        )
                        if (e.missing.isNotEmpty()) Text("Not found: " + e.missing.joinToString("; "), color = Milo.dim, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { history = false }) { Text("Close") } },
            dismissButton = { if (log.isNotEmpty()) androidx.compose.material3.TextButton(onClick = { com.ubayanda.milonative.data.ImportLog.clear(ctx); history = false }) { Text("Clear") } },
        )
    }
    if (linking) {
        NameDialog("Paste a playlist link", "", "Import", { link ->
            linking = false
            android.widget.Toast.makeText(ctx, "Importing\u2026", android.widget.Toast.LENGTH_SHORT).show()
            linkScope.launch {
                val msg = try { val (n, name) = vm.importPlaylistLink(link); "Imported $n songs into \"$name\"" }
                catch (e: kotlinx.coroutines.CancellationException) { throw e }
                catch (e: Exception) { e.message ?: "Couldn't import that link." }
                android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_LONG).show()
            }
        }, { linking = false })
    }
    if (creating) {
        NameDialog("New playlist", "", "Create", { n -> vm.createPlaylist(n); creating = false }, { creating = false })
    }
}

/** Songs A to Z with letter headers and a tappable letter rail on the right. */
@Composable
private fun SongsList(vm: MainViewModel, tracks: List<Track>, ui: PlayerUi, bottomPad: Dp) {
    val state: LazyListState = vm.mem.list("lib-songs")
    val scope = rememberCoroutineScope()
    androidx.compose.runtime.LaunchedEffect(Unit) { vm.scrollTop.collect { state.animateScrollToItem(0) } }
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
        val can: () -> Boolean = { rows.any { it is Track && it.id == curId } }
        vm.jumper = j; vm.canJump = can
        onDispose { if (vm.jumper === j) { vm.jumper = null; vm.canJump = null } }
    }
    Box(Modifier.fillMaxSize()) {
        val scanningNow by vm.scanning.collectAsState()
        com.ubayanda.milonative.ui.components.BouncyRefresh(refreshing = scanningNow, onRefresh = { vm.scan() }, modifier = Modifier.fillMaxSize()) {
        LazyColumn(state = state, contentPadding = PaddingValues(end = 18.dp, bottom = bottomPad)) {
            itemsIndexed(rows, key = { _, r -> if (r is Track) "t" + r.id else "h$r" }, contentType = { _, r -> if (r is Track) "track" else "header" }) { _, r ->
                if (r is Track) TrackRow(vm, r, r.id == ui.currentId, Milo.amber) { vm.player.playQueue(tracks, tracks.indexOf(r)) }
                else LetterHeader(r as String, Milo.amber)
            }
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
