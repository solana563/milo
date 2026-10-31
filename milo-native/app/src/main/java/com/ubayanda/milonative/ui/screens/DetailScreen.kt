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
import com.ubayanda.milonative.ui.components.artBackdrop
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
) {
    val lead = tracks.firstOrNull()
    val soft = rememberSoft(vm, lead).value
    val shape = RoundedCornerShape(30.dp)
    val listState = rememberLazyListState()
    var local by remember(tracks) { mutableStateOf(tracks) }
    val moveNow by rememberUpdatedState(onMove)
    val reorder = if (onMove != null) remember(listState) {
        ReorderState(
            listState, 1,
            { f, t -> local = local.toMutableList().also { it.add(t, it.removeAt(f)) } },
            { f, t -> moveNow?.invoke(f, t) },
        )
    } else null
    val curId by androidx.compose.runtime.rememberUpdatedState(ui.currentId)
    val jumpScope = androidx.compose.runtime.rememberCoroutineScope()
    androidx.compose.runtime.DisposableEffect(Unit) {
        val j: () -> Boolean = {
            val i = local.indexOfFirst { it.id == curId }
            if (i >= 0) { jumpScope.launch { listState.animateScrollToItem(i + 1) }; true } else false
        }
        vm.jumper = j
        onDispose { if (vm.jumper === j) vm.jumper = null }
    }
    LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = bottomPad)) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBackIosNew, "Back", tint = Milo.text) }
                Spacer(Modifier.weight(1f))
                if (tracks.isNotEmpty()) {
                    Text(
                        "Select", color = Milo.dim, fontSize = 14.sp,
                        modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable { vm.selectAll(tracks.map { it.id }) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
                if (headerAction != null) {
                    Text(
                        headerAction.first, color = Milo.dim, fontSize = 14.sp,
                        modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable { headerAction.second() }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
            }
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp).clip(shape).artBackdrop(soft).border(1.dp, Milo.line, shape).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Cover(vm, lead, 180.dp, 22.dp, px = 480)
                Spacer(Modifier.height(14.dp))
                Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                Text(subtitle + " · " + plural(tracks.size, "song"), color = Color(0xFFD8D8D8), fontSize = 13.sp)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Pill("Play", Icons.Rounded.PlayArrow, true, tracks.isNotEmpty()) { vm.player.playQueue(tracks, 0) }
                    Pill("Shuffle", Icons.Rounded.Shuffle, false, tracks.isNotEmpty()) { vm.player.playShuffled(tracks) }
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
