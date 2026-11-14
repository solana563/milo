package com.ubayanda.milonative.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import com.ubayanda.milonative.ui.components.ReorderState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubayanda.milonative.model.PlayerUi
import com.ubayanda.milonative.model.Track
import com.ubayanda.milonative.ui.MainViewModel
import com.ubayanda.milonative.ui.components.Cover
import com.ubayanda.milonative.ui.components.EmptyNote
import com.ubayanda.milonative.ui.components.RowAction
import com.ubayanda.milonative.ui.components.TrackRow
import com.ubayanda.milonative.ui.components.pressable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import com.ubayanda.milonative.ui.components.artWash
import com.ubayanda.milonative.ui.components.rememberSoft
import com.ubayanda.milonative.ui.theme.Milo
import com.ubayanda.milonative.util.plural

/** Album, artist, Liked songs or playlist page: header card with the art, play / shuffle, then the songs. */
@Composable
fun DetailScreen(
    vm: MainViewModel, title: String, subtitle: String, tracks: List<Track>, ui: PlayerUi,
    bottomPad: Dp, onBack: () -> Unit,
    headerAction: Pair<String, () -> Unit>? = null,
    /** When set, rows get a drag handle and this runs (from, to) after a drop. */
    onMove: ((Int, Int) -> Unit)? = null,
    rowExtra: (Track) -> List<RowAction> = { emptyList() },
    /** Draw this picture as the cover instead of the first song's (an online playlist's own cover, say). */
    coverTrack: Track? = null,
    headerAction2: Pair<String, () -> Unit>? = null,
    /** Which playlist / album / artist a Download from this page belongs to, so the Downloads screen can keep them together. */
    downloadGroup: com.ubayanda.milonative.data.OfflineStore.Group? = null,
) {
    val lead = coverTrack ?: tracks.firstOrNull()
    val soft = rememberSoft(vm, lead).value
    val shape = RoundedCornerShape(com.ubayanda.milonative.ui.theme.Rad.xl)
    val listState = vm.mem.list("detail:$title|$subtitle")
    var local by remember(tracks) { mutableStateOf(tracks) }
    val moveNow by rememberUpdatedState(onMove)
    val reorder = if (onMove != null) remember(listState) {
        ReorderState(
            listState, 1,
            { f, t -> local = local.toMutableList().also { it.add(t, it.removeAt(f)) } },
            { f, t -> moveNow?.invoke(f, t) },
        )
    } else null
    val ytTracks = remember(tracks) { tracks.filter { it.uri.startsWith("yt:") } }
    val savedNow = com.ubayanda.milonative.data.OfflineStore.items.collectAsState().value
    val allSaved = ytTracks.isNotEmpty() && ytTracks.all { t -> savedNow.any { it.id == t.uri.removePrefix("yt:") } }
    val curId by androidx.compose.runtime.rememberUpdatedState(ui.currentId)
    val jumpScope = androidx.compose.runtime.rememberCoroutineScope()
    androidx.compose.runtime.DisposableEffect(Unit) {
        val j: () -> Boolean = {
            val i = local.indexOfFirst { it.id == curId }
            if (i >= 0) { jumpScope.launch { listState.animateScrollToItem(i + 1) }; true } else false
        }
        val can: () -> Boolean = { local.any { it.id == curId } }
        vm.jumper = j; vm.canJump = can
        onDispose { if (vm.jumper === j) { vm.jumper = null; vm.canJump = null } }
    }
    LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = bottomPad)) {
        item {
            androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
                val w = maxWidth
                Box(Modifier.fillMaxWidth()) {
                    // the art fills the whole top of the page, then melts into the panel (like the iOS 27 artist page)
                    Cover(vm, lead, w, 0.dp, px = 1000)
                    Box(
                        Modifier.matchParentSize().background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0f to Color.Black.copy(alpha = 0.55f), 0.24f to Color.Transparent,
                                    0.50f to Milo.panel.copy(alpha = 0f), 0.80f to Milo.panel.copy(alpha = 0.88f), 1f to Milo.panel,
                                ),
                            ),
                        ),
                    )
                    Row(Modifier.align(Alignment.TopStart).fillMaxWidth().padding(top = 8.dp, start = 6.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        HeroBtn(Icons.Rounded.ArrowBackIosNew, "Back", onBack)
                        Spacer(Modifier.weight(1f))
                        if (tracks.isNotEmpty()) HeroText("Select") { vm.selectAll(tracks.map { it.id }) }
                        if (headerAction2 != null) HeroText(headerAction2.first) { headerAction2.second() }
                        if (headerAction != null) HeroText(headerAction.first) { headerAction.second() }
                    }
                    Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(title, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, maxLines = 2)
                        Text(subtitle + " · " + plural(tracks.size, "song"), color = Color(0xFFE6E6E6), fontSize = 13.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                            val on = tracks.isNotEmpty()
                            HeroBtn(Icons.Rounded.Shuffle, "Shuffle", { vm.player.playShuffled(tracks) }, 52.dp, on)
                            Box(
                                Modifier.size(76.dp).pressable(on) { vm.player.playQueue(tracks, 0) }.clip(androidx.compose.foundation.shape.CircleShape).background(Color.White.copy(alpha = if (on) 1f else 0.4f)),
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.Rounded.PlayArrow, "Play", tint = Color.Black, modifier = Modifier.size(42.dp)) }
                            if (ytTracks.isNotEmpty()) HeroBtn(
                                if (allSaved) Icons.Rounded.DownloadDone else Icons.Rounded.Download, "Download", { vm.downloadTracks(ytTracks, downloadGroup) }, 52.dp,
                            ) else Spacer(Modifier.size(52.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            if (tracks.isEmpty()) EmptyNote("Nothing here yet", "Use the ⋮ menu on any song to add it.")
        }
        itemsIndexed(local, key = { _, t -> "d" + t.id }, contentType = { _, _ -> "track" }) { i, t ->
            val key = "d" + t.id
            val idx by rememberUpdatedState(i)
            val dragging = reorder?.isDragging(key) == true
            Box(
                if (dragging) Modifier.zIndex(1f).graphicsLayer { translationY = reorder?.offset ?: 0f }
                else Modifier.animateItem()
            ) {
                TrackRow(vm, t, t.id == ui.currentId, Milo.amber, rowExtra(t), handle = reorder?.handle({ idx }, key)) { vm.player.playQueue(local, i) }
            }
        }
    }
}

@Composable
private fun HeroBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit, size: Dp = 44.dp, enabled: Boolean = true) {
    Box(
        Modifier.size(size).pressable(enabled, onClick).clip(androidx.compose.foundation.shape.CircleShape)
            .background(Color.Black.copy(alpha = 0.35f)).border(1.dp, Color.White.copy(alpha = 0.22f), androidx.compose.foundation.shape.CircleShape),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = Color.White, modifier = Modifier.size(size * 0.46f)) }
}

@Composable
private fun HeroText(label: String, onClick: () -> Unit) {
    Box(Modifier.padding(start = 6.dp).pressable(true, onClick).clip(RoundedCornerShape(20.dp)).background(Color.Black.copy(alpha = 0.35f)).border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(20.dp)).padding(horizontal = 14.dp, vertical = 9.dp)) {
        Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}
