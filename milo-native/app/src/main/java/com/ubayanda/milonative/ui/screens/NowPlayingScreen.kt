package com.ubayanda.milonative.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubayanda.milonative.model.PlayerUi
import com.ubayanda.milonative.model.Track
import com.ubayanda.milonative.ui.MainViewModel
import com.ubayanda.milonative.ui.components.artBackdrop
import com.ubayanda.milonative.ui.components.rememberCover
import com.ubayanda.milonative.ui.components.rememberSoft
import com.ubayanda.milonative.ui.components.TrackRow
import com.ubayanda.milonative.ui.theme.Milo
import com.ubayanda.milonative.util.fmtTime
import kotlin.math.roundToInt

@Composable
fun NowPlayingScreen(vm: MainViewModel, t: Track, ui: PlayerUi, accent: Color, liked: Boolean, onClose: () -> Unit) {
    var showQueue by remember { mutableStateOf(false) }
    var drag by remember { mutableFloatStateOf(0f) }
    val cover = rememberCover(vm, t, 900).value
    val soft = rememberSoft(vm, t).value

    BackHandler { if (showQueue) showQueue = false else onClose() }

    Box(
        Modifier.fillMaxSize().offset { IntOffset(0, drag.roundToInt().coerceAtLeast(0)) }.background(Milo.bg)
    ) {
        Column(Modifier.fillMaxSize()) {
            // art: edge to edge, swipe down anywhere on it to go back
            Box(
                Modifier.fillMaxWidth().weight(1f).background(Milo.card2).pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { _, dy -> drag += dy },
                        onDragEnd = { if (drag > 220f) onClose(); drag = 0f },
                        onDragCancel = { drag = 0f },
                    )
                }
            ) {
                if (cover != null) {
                    Image(cover.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Rounded.MusicNote, null, tint = Milo.dim, modifier = Modifier.align(Alignment.Center).size(96.dp))
                }
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.45f), 0.35f to Color.Transparent, 0.6f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.85f))))

                RoundBtn(Icons.Rounded.KeyboardArrowDown, "Back", Modifier.align(Alignment.TopStart).statusBarsPadding().padding(12.dp), onClick = onClose)

                Column(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    RoundBtn(Icons.Rounded.QueueMusic, "Queue") { showQueue = true }
                    RoundBtn(if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Like", tint = if (liked) accent else Color.White) { vm.toggleFav(t.id) }
                    SpeedMenu(ui, vm)
                    SleepMenu(ui, vm)
                }

                Column(Modifier.align(Alignment.BottomStart).padding(start = 20.dp, end = 76.dp, bottom = 18.dp)) {
                    Text(t.title, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 34.sp)
                    Text(t.artist, color = Color(0xFFE6E6E6), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        t.album + if (t.year > 0) " · ${t.year}" else "",
                        color = Color(0xFFBDBDBD), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // control card
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).artBackdrop(soft, 0.55f)
                    .navigationBarsPadding().padding(top = 14.dp, bottom = 6.dp),
            ) {
                ProgressBar(ui, accent) { vm.player.seekTo(it) }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconTap(Icons.Rounded.Shuffle, "Shuffle", 26.dp, if (ui.shuffle) accent else Color.White) { vm.player.toggleShuffle() }
                    IconTap(Icons.Rounded.SkipPrevious, "Previous", 38.dp, Color.White) { vm.player.previous() }
                    Box(
                        Modifier.size(72.dp).clip(CircleShape).background(Color.White).clickable { vm.player.toggle() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(if (ui.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Play or pause", tint = Color.Black, modifier = Modifier.size(40.dp))
                    }
                    IconTap(Icons.Rounded.SkipNext, "Next", 38.dp, Color.White) { vm.player.next() }
                    IconTap(
                        if (ui.repeat == 1) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, "Repeat", 26.dp,
                        if (ui.repeat != 0) accent else Color.White,
                    ) { vm.player.cycleRepeat() }
                }
                // slide handle: lyrics arrive in a later release, for now it opens the queue
                Column(Modifier.fillMaxWidth().clickable { showQueue = true }.padding(top = 8.dp, bottom = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.width(40.dp).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.4f)))
                    Spacer(Modifier.height(6.dp))
                    Text("Up next", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (showQueue) QueueSheet(vm, ui, accent) { showQueue = false }
    }
}

@Composable
private fun RoundBtn(icon: ImageVector, desc: String, modifier: Modifier = Modifier, tint: Color = Color.White, onClick: () -> Unit) {
    Box(
        modifier.size(46.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, tint = tint, modifier = Modifier.size(24.dp)) }
}

@Composable
private fun IconTap(icon: ImageVector, desc: String, size: androidx.compose.ui.unit.Dp, tint: Color, onClick: () -> Unit) {
    Box(Modifier.size(52.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, desc, tint = tint, modifier = Modifier.size(size))
    }
}

@Composable
private fun ProgressBar(ui: PlayerUi, accent: Color, onSeek: (Long) -> Unit) {
    var dragging by remember { mutableStateOf(false) }
    var dragVal by remember { mutableFloatStateOf(0f) }
    val frac = if (ui.durationMs > 0) (ui.positionMs.toFloat() / ui.durationMs).coerceIn(0f, 1f) else 0f
    val shown = if (dragging) dragVal else frac
    Column(Modifier.padding(horizontal = 18.dp)) {
        Slider(
            value = shown,
            onValueChange = { dragging = true; dragVal = it },
            onValueChangeFinished = { onSeek((dragVal * ui.durationMs).toLong()); dragging = false },
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = accent, inactiveTrackColor = Color.White.copy(alpha = 0.25f)),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(fmtTime(if (dragging) (dragVal * ui.durationMs).toLong() else ui.positionMs), color = Color(0xFFE0E0E0), fontSize = 12.sp)
            Text(fmtTime(ui.durationMs), color = Color(0xFFE0E0E0), fontSize = 12.sp)
        }
    }
}

@Composable
private fun SpeedMenu(ui: PlayerUi, vm: MainViewModel) {
    var open by remember { mutableStateOf(false) }
    Box {
        RoundBtn(Icons.Rounded.GraphicEq, "Playback speed") { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            listOf(0.75f, 1f, 1.25f, 1.5f, 2f).forEach { s ->
                DropdownMenuItem(
                    text = { Text((if (s == 1f) "Normal" else "${s}x") + if (ui.speed == s) "  ✓" else "") },
                    onClick = { vm.player.setSpeed(s); open = false },
                )
            }
        }
    }
}

@Composable
private fun SleepMenu(ui: PlayerUi, vm: MainViewModel) {
    var open by remember { mutableStateOf(false) }
    Box {
        RoundBtn(Icons.Rounded.MoreHoriz, "More") { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (ui.sleepLeftMs > 0) {
                DropdownMenuItem(text = { Text("Sleep timer: ${fmtTime(ui.sleepLeftMs)} left. Cancel") }, onClick = { vm.player.sleepIn(0); open = false })
            }
            listOf(15, 30, 45, 60).forEach { m ->
                DropdownMenuItem(text = { Text("Sleep in $m min") }, onClick = { vm.player.sleepIn(m); open = false })
            }
        }
    }
}

@Composable
private fun QueueSheet(vm: MainViewModel, ui: PlayerUi, accent: Color, onClose: () -> Unit) {
    val items = ui.queueIds.mapNotNull { vm.trackById(it) }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable(onClick = onClose)) {
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxSize(0.72f)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(Milo.card)
                .clickable(enabled = false) {}.navigationBarsPadding(),
        ) {
            Text("Up next", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(18.dp))
            LazyColumn {
                itemsIndexed(items, key = { i, t -> "q$i-${t.id}" }) { i, t ->
                    TrackRow(vm, t, i == ui.queueIndex, accent) { vm.player.playQueueIndex(i) }
                }
            }
        }
    }
}
