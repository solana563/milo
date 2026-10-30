package com.ubayanda.milonative.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubayanda.milonative.model.Track
import com.ubayanda.milonative.ui.LocalActions
import com.ubayanda.milonative.ui.MainViewModel
import com.ubayanda.milonative.ui.theme.Figtree
import com.ubayanda.milonative.ui.theme.Milo
import com.ubayanda.milonative.util.fmtTime

class RowAction(val label: String, val run: () -> Unit)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackRow(
    vm: MainViewModel, t: Track, current: Boolean, accent: Color,
    extra: List<RowAction> = emptyList(),
    /** When set, a drag handle is shown (queue / playlists) and swiping is turned off. */
    handle: Modifier? = null,
    onClick: () -> Unit,
) {
    val actions = LocalActions.current
    val ctx = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val favState = vm.favs.collectAsState()
    val liked by remember(t.id) { androidx.compose.runtime.derivedStateOf { favState.value.contains(t.id) } }
    val selState = vm.selection.collectAsState()
    val selecting by remember { androidx.compose.runtime.derivedStateOf { selState.value.isNotEmpty() } }
    val picked by remember(t.id) { androidx.compose.runtime.derivedStateOf { t.id in selState.value } }
    var menu by remember { mutableStateOf(false) }
    var dx by remember { mutableFloatStateOf(0f) }
    val limit = with(LocalDensity.current) { 96.dp.toPx() }
    val swipeOn = handle == null && !selecting
    Box(Modifier.fillMaxWidth().clipToBounds()) {
        // what the swipe will do, shown behind the row
        if (dx > 8f) Text("Play next", color = Milo.amber, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.CenterStart).padding(start = 22.dp))
        if (dx < -8f) Text(if (liked) "Unlike" else "Like", color = Milo.amber, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 22.dp))
        val hot = Modifier.padding(horizontal = 8.dp).clip(RoundedCornerShape(20.dp)).background(Milo.card)
        Row(
            Modifier.graphicsLayer { translationX = dx * 0.55f }
                .then(if (current || picked) hot else Modifier).fillMaxWidth()
                .then(
                    if (swipeOn) Modifier.pointerInput(t.id) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (dx > limit) { vm.player.playNext(t); haptic.performHapticFeedback(HapticFeedbackType.LongPress); Toast.makeText(ctx, "Playing next", Toast.LENGTH_SHORT).show() }
                                else if (dx < -limit) { vm.toggleFav(t.id); haptic.performHapticFeedback(HapticFeedbackType.LongPress); Toast.makeText(ctx, if (liked) "Removed from Liked songs" else "Added to Liked songs", Toast.LENGTH_SHORT).show() }
                                dx = 0f
                            },
                            onDragCancel = { dx = 0f },
                            onHorizontalDrag = { change, d -> change.consume(); dx = (dx + d).coerceIn(-limit * 1.3f, limit * 1.3f) },
                        )
                    } else Modifier
                )
                .combinedClickable(
                    onClick = { if (selecting) vm.toggleSelect(t.id) else onClick() },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (selecting) vm.toggleSelect(t.id) else menu = true
                    },
                )
                .padding(start = if (current || picked) 12.dp else 20.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selecting) {
                Icon(
                    if (picked) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, if (picked) "Selected" else "Not selected",
                    tint = if (picked) Milo.amber else Milo.dim, modifier = Modifier.padding(end = 12.dp).size(24.dp),
                )
            }
            Cover(vm, t, 52.dp, 12.dp, px = 160)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    t.title, color = if (current) Milo.amber else Milo.text, fontSize = 16.sp, fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (t.album.isBlank() || t.album == "Unknown album") t.artist else t.artist + " · " + t.album,
                    color = Milo.dim, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Text(fmtTime(t.durationMs), color = Milo.dim, fontSize = 12.sp)
            if (handle != null) {
                Icon(Icons.Rounded.DragHandle, "Drag to reorder", tint = Milo.dim, modifier = Modifier.padding(horizontal = 6.dp).size(40.dp).then(handle).padding(8.dp))
            }
            if (!selecting) Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Rounded.MoreHoriz, "More", tint = Milo.dim, modifier = Modifier.size(22.dp))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Play next") }, onClick = { menu = false; vm.player.playNext(t) })
                    DropdownMenuItem(text = { Text("Add to queue") }, onClick = { menu = false; vm.player.addToQueue(t) })
                    DropdownMenuItem(text = { Text(if (liked) "Remove from Liked songs" else "Add to Liked songs") }, onClick = { menu = false; vm.toggleFav(t.id) })
                    DropdownMenuItem(text = { Text("Add to playlist") }, onClick = { menu = false; actions.addToPlaylist(t) })
                    DropdownMenuItem(text = { Text("Select multiple") }, onClick = { menu = false; vm.toggleSelect(t.id) })
                    DropdownMenuItem(text = { Text("Share") }, onClick = { menu = false; actions.share(t) })
                    extra.forEach { a -> DropdownMenuItem(text = { Text(a.label) }, onClick = { menu = false; a.run() }) }
                }
            }
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text, color = Milo.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(horizontal = 20.dp, vertical = 12.dp),
    )
}

@Composable
fun ScreenTitle(text: String) {
    Text(
        text, color = Milo.text, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 37.sp,
        modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 26.dp, bottom = 14.dp),
    )
}

@Composable
fun LetterHeader(letter: String, accent: Color) {
    Text(
        letter, color = Milo.amber, fontSize = 13.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
    )
}

@Composable
fun EmptyNote(title: String, body: String) {
    Column(
        Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, color = Milo.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Text(body, color = Milo.dim, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}

/** Amber when selected, dark pill with a hairline border otherwise: the web milo. chip. */
@Composable
fun MiloChip(label: String, on: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Text(
        label, color = if (on) Color.Black else Milo.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center, maxLines = 1, softWrap = false,
        modifier = modifier.clip(shape).background(if (on) Milo.amber else Milo.card2, shape)
            .border(1.dp, if (on) Milo.amber else Milo.line, shape).clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
    )
}
