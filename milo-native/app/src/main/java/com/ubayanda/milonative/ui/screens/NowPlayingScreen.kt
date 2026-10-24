package com.ubayanda.milonative.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubayanda.milonative.model.Lyrics
import com.ubayanda.milonative.model.PlayerUi
import com.ubayanda.milonative.model.Track
import com.ubayanda.milonative.player.AudioFx
import com.ubayanda.milonative.ui.LocalActions
import com.ubayanda.milonative.ui.MainViewModel
import com.ubayanda.milonative.ui.components.MiloChip
import com.ubayanda.milonative.ui.components.RowAction
import com.ubayanda.milonative.ui.components.TrackRow
import com.ubayanda.milonative.ui.components.artBackdrop
import com.ubayanda.milonative.ui.components.rememberCover
import com.ubayanda.milonative.ui.components.rememberSoft
import com.ubayanda.milonative.ui.theme.Milo
import com.ubayanda.milonative.util.fmtTime
import kotlin.math.roundToInt

private enum class Panel { None, Queue, Sound, Lyrics }

@Composable
fun NowPlayingScreen(vm: MainViewModel, t: Track, ui: PlayerUi, liked: Boolean, onClose: () -> Unit) {
    var panel by remember { mutableStateOf(Panel.None) }
    var drag by remember { mutableFloatStateOf(0f) }
    val cover = rememberCover(vm, t, 900).value
    val soft = rememberSoft(vm, t).value

    BackHandler { if (panel != Panel.None) panel = Panel.None else onClose() }

    Box(Modifier.fillMaxSize().offset { IntOffset(0, drag.roundToInt().coerceAtLeast(0)) }.background(Milo.bg)) {
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
                    RoundBtn(Icons.Rounded.QueueMusic, "Queue") { panel = Panel.Queue }
                    RoundBtn(if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Like", tint = if (liked) Milo.amber else Color.White) { vm.toggleFav(t.id) }
                    RoundBtn(Icons.Rounded.GraphicEq, "Sound") { panel = Panel.Sound }
                    MoreMenu(vm, t, ui)
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
                Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)).artBackdrop(soft, 0.55f)
                    .navigationBarsPadding().padding(top = 14.dp, bottom = 6.dp),
            ) {
                ProgressBar(ui) { vm.player.seekTo(it) }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconTap(Icons.Rounded.Shuffle, "Shuffle", 26.dp, if (ui.shuffle) Milo.amber else Color.White) { vm.player.toggleShuffle() }
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
                        if (ui.repeat != 0) Milo.amber else Color.White,
                    ) { vm.player.cycleRepeat() }
                }
                // lyrics slide handle: tap it or drag it up
                Column(
                    Modifier.fillMaxWidth().clickable { panel = Panel.Lyrics }.pointerInput(Unit) {
                        var total = 0f
                        detectVerticalDragGestures(
                            onVerticalDrag = { _, dy -> total += dy },
                            onDragEnd = { if (total < -30f) panel = Panel.Lyrics; total = 0f },
                            onDragCancel = { total = 0f },
                        )
                    }.padding(top = 8.dp, bottom = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.width(40.dp).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.4f)))
                    Spacer(Modifier.height(6.dp))
                    Text("Lyrics", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                }
            }
        }

        when (panel) {
            Panel.Queue -> Sheet(0.72f, null, { panel = Panel.None }) { QueueContent(vm, ui) }
            Panel.Sound -> Sheet(0.8f, null, { panel = Panel.None }) { SoundContent(vm, ui) }
            Panel.Lyrics -> Sheet(0.86f, soft, { panel = Panel.None }) { LyricsContent(vm, t, ui) }
            Panel.None -> {}
        }
    }
}

@Composable
private fun Sheet(frac: Float, backdrop: Bitmap?, onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable(onClick = onClose)) {
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(frac)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)).artBackdrop(backdrop, 0.7f)
                .clickable(enabled = false) {}.navigationBarsPadding(),
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp).width(40.dp).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f)))
            content()
        }
    }
}

@Composable
private fun QueueContent(vm: MainViewModel, ui: PlayerUi) {
    val items = ui.queueIds.mapNotNull { vm.trackById(it) }
    Text("Up next", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(20.dp))
    LazyColumn {
        itemsIndexed(items, key = { i, t -> "q$i-${t.id}" }) { i, t ->
            TrackRow(vm, t, i == ui.queueIndex, Milo.amber, listOf(RowAction("Remove from queue") { vm.player.removeFromQueue(i) })) { vm.player.playQueueIndex(i) }
        }
    }
}

@Composable
private fun SoundContent(vm: MainViewModel, ui: PlayerUi) {
    val fx by AudioFx.state.collectAsState()
    Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        Text("Sound", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(20.dp))

        Label("Speed")
        LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf(0.75f, 1f, 1.25f, 1.5f, 2f)) { s ->
                MiloChip(if (s == 1f) "Normal" else "${s}x", ui.speed == s) { vm.player.setSpeed(s) }
            }
        }

        Label("Equaliser")
        if (!fx.available) {
            Text("This phone doesn't expose an equaliser. Speed still works.", color = Milo.dim, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 20.dp))
        } else {
            LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AudioFx.presets.keys.toList()) { name -> MiloChip(name, fx.preset == name) { AudioFx.applyPreset(name) } }
            }
            Spacer(Modifier.height(8.dp))
            fx.levelsDb.forEachIndexed { i, db ->
                val hz = fx.freqsHz.getOrElse(i) { 0 }
                Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (hz >= 1000) "${hz / 1000}k" else "$hz", color = Milo.dim, fontSize = 12.sp, modifier = Modifier.width(40.dp))
                    Slider(
                        value = db, onValueChange = { AudioFx.setBand(i, it) },
                        valueRange = fx.minDb..(if (fx.maxDb > fx.minDb) fx.maxDb else fx.minDb + 1f),
                        colors = amberSlider(), modifier = Modifier.weight(1f),
                    )
                    Text("${db.roundToInt()} dB", color = Milo.dim, fontSize = 12.sp, modifier = Modifier.width(48.dp))
                }
            }
            Label("Bass boost")
            Slider(
                value = fx.bass.toFloat(), onValueChange = { AudioFx.setBass(it.roundToInt()) }, valueRange = 0f..1000f,
                colors = amberSlider(), modifier = Modifier.padding(horizontal = 20.dp),
            )
            Label("Spatial")
            Slider(
                value = fx.spatial.toFloat(), onValueChange = { AudioFx.setSpatial(it.roundToInt()) }, valueRange = 0f..1000f,
                colors = amberSlider(), modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
    }
}

@Composable
private fun amberSlider() = SliderDefaults.colors(
    thumbColor = Milo.amber, activeTrackColor = Milo.amber, inactiveTrackColor = Color.White.copy(alpha = 0.2f),
)

@Composable
private fun Label(text: String) {
    Text(text, color = Milo.dim, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp, modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 8.dp))
}

@Composable
private fun LyricsContent(vm: MainViewModel, t: Track, ui: PlayerUi) {
    var lyrics by remember(t.id) { mutableStateOf<Lyrics?>(null) }
    var loading by remember(t.id) { mutableStateOf(true) }
    LaunchedEffect(t.id) {
        loading = true
        lyrics = vm.lyricsFor(t)
        loading = false
    }
    Text("Lyrics", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 4.dp))
    Text(t.title + " · " + t.artist, color = Milo.dim, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp))
    val l = lyrics
    when {
        loading -> Text("Finding lyrics…", color = Milo.dim, fontSize = 15.sp, modifier = Modifier.padding(20.dp))
        l == null -> Text("No lyrics found for this song, or you're offline.", color = Milo.dim, fontSize = 15.sp, modifier = Modifier.padding(20.dp))
        l.synced.isNotEmpty() -> {
            val state = rememberLazyListState()
            val idx = l.synced.indexOfLast { it.timeMs <= ui.positionMs + 250 }
            LaunchedEffect(idx) { if (idx >= 0) state.animateScrollToItem((idx - 2).coerceAtLeast(0)) }
            LazyColumn(state = state, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 16.dp)) {
                itemsIndexed(l.synced) { i, line ->
                    Text(
                        line.text.ifEmpty { "♪" },
                        color = Color.White.copy(alpha = if (i == idx) 1f else 0.4f),
                        fontSize = 26.sp, fontWeight = if (i == idx) FontWeight.ExtraBold else FontWeight.Bold, lineHeight = 32.sp,
                        modifier = Modifier.fillMaxWidth().clickable { vm.player.seekTo(line.timeMs) }.padding(vertical = 8.dp),
                    )
                }
            }
        }
        else -> Text(
            l.plain, color = Color.White, fontSize = 20.sp, lineHeight = 30.sp,
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
        )
    }
}

@Composable
private fun MoreMenu(vm: MainViewModel, t: Track, ui: PlayerUi) {
    var open by remember { mutableStateOf(false) }
    val actions = LocalActions.current
    Box {
        RoundBtn(Icons.Rounded.MoreHoriz, "More") { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("Add to playlist") }, onClick = { open = false; actions.addToPlaylist(t) })
            DropdownMenuItem(text = { Text("Share song") }, onClick = { open = false; actions.share(t) })
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
private fun RoundBtn(icon: ImageVector, desc: String, modifier: Modifier = Modifier, tint: Color = Color.White, onClick: () -> Unit) {
    Box(
        modifier.size(46.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, tint = tint, modifier = Modifier.size(24.dp)) }
}

@Composable
private fun IconTap(icon: ImageVector, desc: String, size: Dp, tint: Color, onClick: () -> Unit) {
    Box(Modifier.size(52.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, desc, tint = tint, modifier = Modifier.size(size))
    }
}

@Composable
private fun ProgressBar(ui: PlayerUi, onSeek: (Long) -> Unit) {
    var dragging by remember { mutableStateOf(false) }
    var dragVal by remember { mutableFloatStateOf(0f) }
    val frac = if (ui.durationMs > 0) (ui.positionMs.toFloat() / ui.durationMs).coerceIn(0f, 1f) else 0f
    val shown = if (dragging) dragVal else frac
    Column(Modifier.padding(horizontal = 18.dp)) {
        Slider(
            value = shown,
            onValueChange = { dragging = true; dragVal = it },
            onValueChangeFinished = { onSeek((dragVal * ui.durationMs).toLong()); dragging = false },
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Milo.amber, inactiveTrackColor = Color.White.copy(alpha = 0.25f)),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(fmtTime(if (dragging) (dragVal * ui.durationMs).toLong() else ui.positionMs), color = Color(0xFFE0E0E0), fontSize = 12.sp)
            Text(fmtTime(ui.durationMs), color = Color(0xFFE0E0E0), fontSize = 12.sp)
        }
    }
}
