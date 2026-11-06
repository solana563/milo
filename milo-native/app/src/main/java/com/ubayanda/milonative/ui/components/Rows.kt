package com.ubayanda.milonative.ui.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.DownloadDone
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
import com.ubayanda.milonative.ui.theme.Rad
import com.ubayanda.milonative.ui.theme.Sp
import com.ubayanda.milonative.ui.theme.Ty
import androidx.compose.foundation.layout.heightIn
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
    var editing by remember { mutableStateOf(false) }
    val savedItems = com.ubayanda.milonative.data.OfflineStore.items.collectAsState().value
    val isYt = t.uri.startsWith("yt:")
    val saved = isYt && savedItems.any { it.id == t.uri.removePrefix("yt:") }
    val dlNow = com.ubayanda.milonative.data.OfflineStore.progress.collectAsState().value[t.uri.removePrefix("yt:")]
    var dx by remember { mutableFloatStateOf(0f) }
    val limit = with(LocalDensity.current) { 96.dp.toPx() }
    val swipeOn = handle == null && !selecting
    Box(Modifier.fillMaxWidth().clipToBounds()) {
        // what the swipe will do, shown behind the row
        if (dx > 8f) Text("Play next", color = Milo.amber, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.CenterStart).padding(start = 22.dp))
        if (dx < -8f) Text(if (liked) "Unlike" else "Like", color = Milo.amber, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 22.dp))
        val hotShape = RoundedCornerShape(Rad.lg)
        val hot = Modifier.padding(horizontal = Sp.x2).clip(hotShape)
            .background(if (current) androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(accent.copy(alpha = 0.26f), accent.copy(alpha = 0.08f))) else androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Milo.card, Milo.card)), hotShape)
            .border(1.dp, if (current) accent.copy(alpha = 0.55f) else Milo.line, hotShape)
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
                .padding(start = if (current || picked) Sp.x3 else Sp.gutter, end = Sp.x1, top = Sp.x2, bottom = Sp.x2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selecting) {
                Icon(
                    if (picked) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, if (picked) "Selected" else "Not selected",
                    tint = if (picked) Milo.amber else Milo.dim, modifier = Modifier.padding(end = 12.dp).size(24.dp),
                )
            }
            Cover(vm, t, 56.dp, Rad.sm, px = 160)
            Spacer(Modifier.width(Sp.x3))
            Column(Modifier.weight(1f)) {
                Text(
                    t.title, color = if (current) accent else Milo.text, style = Ty.body,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (t.album.isBlank() || t.album == "Unknown album") t.artist else t.artist + " · " + t.album,
                    color = Milo.dim, style = Ty.caption, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            if (saved) Icon(Icons.Rounded.DownloadDone, "Saved on this phone", tint = Milo.amber, modifier = Modifier.padding(end = 6.dp).size(16.dp))
            else if (dlNow != null && dlNow >= 0f) Text("${(dlNow * 100).toInt()}%", color = Milo.amber, style = Ty.caption, modifier = Modifier.padding(end = 6.dp))
            else if (isYt && !selecting) Icon(
                Icons.Rounded.Download, "Download for offline", tint = Milo.dim,
                modifier = Modifier.padding(end = 2.dp).size(40.dp).clip(RoundedCornerShape(20.dp)).clickable { vm.downloadTracks(listOf(t)) }.padding(9.dp),
            )
            if (current) PlayingBars(accent)
            Text(fmtTime(t.durationMs), color = if (current) accent else Milo.dim, style = Ty.caption)
            if (handle != null) {
                Icon(Icons.Rounded.DragHandle, "Drag to reorder", tint = Milo.dim, modifier = Modifier.padding(horizontal = 6.dp).size(40.dp).then(handle).padding(8.dp))
            }
            if (!selecting) Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Rounded.MoreHoriz, "More", tint = Milo.dim, modifier = Modifier.size(22.dp))
                }
                if (editing) EditInfoDialog(
                    t.title, t.artist.takeIf { it != "Unknown artist" } ?: "", t.album, com.ubayanda.milonative.data.MetaEdits.has(t.id),
                    { n, a, al -> editing = false; vm.editInfo(t.id, n, a, al) }, { editing = false; vm.resetInfo(t.id) }, { editing = false },
                )
                if (menu) MiloPopup({ menu = false }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Cover(vm, t, 68.dp, Rad.md, px = 200)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t.title, color = Milo.text, style = Ty.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(
                                if (t.album.isBlank() || t.album == "Unknown album") t.artist else t.artist + " \u00b7 " + t.album,
                                color = Milo.dim, style = Ty.caption, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PopupQuick(Icons.Rounded.SkipNext, "Play next") { menu = false; vm.player.playNext(t) }
                        PopupQuick(Icons.Rounded.QueueMusic, "Queue") { menu = false; vm.player.addToQueue(t) }
                        PopupQuick(if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, if (liked) "Liked" else "Like", on = liked) { menu = false; vm.toggleFav(t.id) }
                        PopupQuick(Icons.Rounded.PlaylistAdd, "Playlist") { menu = false; actions.addToPlaylist(t) }
                    }
                    Spacer(Modifier.height(8.dp))
                    PopupAction(Icons.Rounded.Checklist, "Select multiple") { menu = false; vm.toggleSelect(t.id) }
                    PopupAction(Icons.Rounded.Share, "Share") { menu = false; actions.share(t) }
                    if (t.id >= 0 && t.uri.startsWith("content:")) PopupAction(Icons.Rounded.Edit, "Edit info") { menu = false; editing = true }
                    if (isYt) PopupAction(
                        if (saved) Icons.Rounded.DeleteOutline else Icons.Rounded.Download,
                        if (saved) "Remove download" else "Download for offline",
                    ) {
                        menu = false
                        if (saved) { com.ubayanda.milonative.data.OfflineStore.remove(t.uri.removePrefix("yt:")); Toast.makeText(ctx, "Removed from this phone", Toast.LENGTH_SHORT).show() }
                        else { vm.rememberRemote(listOf(t)); com.ubayanda.milonative.data.OfflineStore.enqueue(listOf(t)) { n, e -> Toast.makeText(ctx, if (n > 0) "Saved for offline" else e ?: "Couldn't download", Toast.LENGTH_SHORT).show() }; Toast.makeText(ctx, "Downloading…", Toast.LENGTH_SHORT).show() }
                    }
                    extra.forEach { a -> PopupAction(Icons.Rounded.RemoveCircleOutline, a.label, Color(0xFFFF8A80)) { menu = false; a.run() } }
                }
            }
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text, color = Milo.text, style = Ty.title,
        modifier = modifier.padding(start = Sp.gutter, end = Sp.gutter, top = Sp.x6, bottom = Sp.x2),
    )
}

@Composable
fun ScreenTitle(text: String) {
    Text(
        text, color = Milo.text, style = Ty.display,
        modifier = Modifier.padding(start = Sp.gutter, end = Sp.gutter, top = Sp.x6, bottom = Sp.x4),
    )
}

@Composable
fun LetterHeader(letter: String, accent: Color) {
    Text(
        letter, color = Milo.amber, style = Ty.micro,
        modifier = Modifier.fillMaxWidth().padding(horizontal = Sp.gutter, vertical = Sp.x2),
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
        label, color = if (on) Color.Black else Milo.text, style = Ty.label,
        textAlign = TextAlign.Center, maxLines = 1, softWrap = false,
        modifier = modifier.heightIn(min = 44.dp).pressable(true, onClick).clip(shape).background(if (on) Milo.amber else Milo.card2, shape)
            .border(1.dp, glassStroke(if (on) 0.9f else 1f), shape)
            .padding(horizontal = Sp.x3, vertical = 11.dp),
    )
}


/** Three little bars that dance beside the song that is playing. */
@Composable
fun PlayingBars(color: Color) {
    val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "bars")
    val a by t.animateFloat(0.3f, 1f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(520), androidx.compose.animation.core.RepeatMode.Reverse), label = "a")
    val b by t.animateFloat(1f, 0.35f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(430), androidx.compose.animation.core.RepeatMode.Reverse), label = "b")
    val c by t.animateFloat(0.5f, 0.95f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(610), androidx.compose.animation.core.RepeatMode.Reverse), label = "c")
    Row(Modifier.padding(end = 8.dp).height(14.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
        listOf(a, b, c).forEach { h: Float -> Box(Modifier.width(3.dp).height((14f * h).dp).clip(RoundedCornerShape(2.dp)).background(color)) }
    }
}
