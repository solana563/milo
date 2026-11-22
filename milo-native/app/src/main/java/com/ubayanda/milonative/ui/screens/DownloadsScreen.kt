package com.ubayanda.milonative.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubayanda.milonative.data.OfflineStore
import com.ubayanda.milonative.model.PlayerUi
import com.ubayanda.milonative.ui.MainViewModel
import com.ubayanda.milonative.ui.components.CoverCollage
import com.ubayanda.milonative.ui.components.GlassSegmented
import com.ubayanda.milonative.ui.components.SectionTitle
import com.ubayanda.milonative.ui.components.TrackRow
import com.ubayanda.milonative.ui.theme.Milo
import com.ubayanda.milonative.ui.theme.MiloIcons
import com.ubayanda.milonative.ui.theme.Sp
import com.ubayanda.milonative.ui.theme.Ty

/**
 * The saved songs kept the way they were downloaded: whole playlists, whole albums, whole artists, and songs saved one by one.
 * Tap a playlist/album/artist to open just its songs. (The Download Manager screen keeps the same split for managing files and sizes.)
 */
@Composable
fun DownloadsScreen(vm: MainViewModel, ui: PlayerUi, bottomPad: Dp, onBack: () -> Unit) {
    val items by OfflineStore.items.collectAsState()
    var open by remember { mutableStateOf<Pair<String, String>?>(null) }
    var tab by remember { mutableStateOf(0) }

    val playlists = remember(items) { items.filter { it.gKind == "playlist" }.groupBy { it.gName } }
    val albums = remember(items) { items.filter { it.gKind == "album" }.groupBy { it.gName } }
    val artists = remember(items) { items.filter { it.gKind == "artist" }.groupBy { it.gName } }
    val singles = remember(items) { OfflineStore.tracksOf("", "") }

    // the tabs that have something in them
    val tabs = remember(items) {
        val l = arrayListOf("All")
        if (playlists.isNotEmpty()) l.add("Playlists")
        if (albums.isNotEmpty()) l.add("Albums")
        if (artists.isNotEmpty()) l.add("Artists")
        if (singles.isNotEmpty()) l.add("Singles")
        l
    }
    val safeTab = tab.coerceIn(0, tabs.size - 1)
    val now = tabs[safeTab]

    BackHandler(enabled = open != null) { open = null }
    val o = open
    if (o != null) {
        val list = OfflineStore.tracksOf(o.first, o.second)
        if (list.isEmpty()) { open = null; return }
        DetailScreen(
            vm, if (o.first.isEmpty()) "Singles" else o.second,
            when (o.first) { "playlist" -> "Downloaded playlist"; "album" -> "Downloaded album"; "artist" -> "Downloaded from an artist"; else -> "Saved one by one" },
            list.also { vm.rememberRemote(it) }, ui, bottomPad, { open = null },
        )
        return
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = bottomPad)) {
        item {
            Row(Modifier.padding(start = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clickable { onBack() }, contentAlignment = Alignment.Center) {
                    Icon(MiloIcons.ArrowBackIosNew, "Back", tint = Milo.text, modifier = Modifier.size(20.dp))
                }
                Text("Downloads", color = Milo.text, style = Ty.display, modifier = Modifier.padding(start = 4.dp))
            }
            Text(
                if (items.isEmpty()) "Nothing saved yet" else OfflineStore.summary(),
                color = Milo.dim, fontSize = 13.sp, modifier = Modifier.padding(start = Sp.gutter, end = Sp.gutter, bottom = 10.dp),
            )
            if (tabs.size > 2) GlassSegmented(tabs, safeTab, { tab = it }, Modifier.padding(horizontal = 18.dp, vertical = 4.dp))
        }
        if (items.isEmpty()) {
            item { Text("Open a YouTube song's menu and choose Download, or use Download on an album or playlist.", color = Milo.dim, style = Ty.caption, modifier = Modifier.padding(Sp.gutter)) }
        }
        if ((now == "All" || now == "Playlists") && playlists.isNotEmpty()) {
            item { SectionTitle("Playlists") }
            groupRows(vm, "playlist", playlists) { open = it }
        }
        if ((now == "All" || now == "Albums") && albums.isNotEmpty()) {
            item { SectionTitle("Albums") }
            groupRows(vm, "album", albums) { open = it }
        }
        if ((now == "All" || now == "Artists") && artists.isNotEmpty()) {
            item { SectionTitle("Artists") }
            groupRows(vm, "artist", artists) { open = it }
        }
        if ((now == "All" || now == "Singles") && singles.isNotEmpty()) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { SectionTitle("Singles") }
                    com.ubayanda.milonative.ui.components.MiloChip("Play", false, Modifier.padding(end = 8.dp)) { vm.rememberRemote(singles); vm.player.playQueue(singles, 0) }
                    com.ubayanda.milonative.ui.components.MiloChip("Shuffle", false, Modifier.padding(end = Sp.gutter)) { vm.rememberRemote(singles); vm.player.playShuffled(singles) }
                }
                Text("Songs you saved one at a time", color = Milo.dim, fontSize = 13.sp, modifier = Modifier.padding(start = Sp.gutter, end = Sp.gutter, bottom = 4.dp))
            }
            items(singles, key = { "single-" + it.id }) { t ->
                TrackRow(vm, t, t.id == ui.currentId, Milo.amber) { vm.rememberRemote(singles); vm.player.playQueue(singles, singles.indexOf(t)) }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.groupRows(
    vm: MainViewModel, kind: String, groups: Map<String, List<OfflineStore.Item>>, onOpen: (Pair<String, String>) -> Unit,
) {
    val sorted = groups.entries.sortedByDescending { e -> e.value.maxOfOrNull { it.at } ?: 0L }
    items(sorted, key = { "grp-$kind-" + it.key }) { e ->
        val tracks = remember(e.value) { OfflineStore.tracksOf(kind, e.key) }
        val bytes = e.value.sumOf { it.bytes }
        Row(Modifier.fillMaxWidth().clickable { onOpen(kind to e.key) }.padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            CoverCollage(vm, tracks, 56.dp, 14.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(e.key, color = Milo.text, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    e.value.size.toString() + (if (e.value.size == 1) " song" else " songs") + " · " + (if (bytes >= 1_048_576L) "%.0f MB".format(bytes / 1_048_576.0) else "%.0f KB".format(bytes / 1024.0)),
                    color = Milo.dim, fontSize = 13.sp, maxLines = 1,
                )
            }
        }
    }
}
