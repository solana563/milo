package com.ubayanda.milonative.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Tune
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
    val cover = rememberCover(vm, t, 1000).value
    val soft = rememberSoft(vm, t).value

    var lyrics by remember(t.id) { mutableStateOf<Lyrics?>(null) }
    var lyricsLoading by remember(t.id) { mutableStateOf(true) }
    LaunchedEffect(t.id) {
        lyricsLoading = true
        lyrics = vm.lyricsFor(t)
        lyricsLoading = false
    }
    val l = lyrics
    val preview: String = when {
        l == null -> "Lyrics"
        l.synced.isNotEmpty() -> {
            val i = l.synced.indexOfLast { it.timeMs <= ui.positionMs + 250 }.coerceAtLeast(0)
            l.synced.drop(i).firstOrNull { it.text.isNotBlank() }?.text ?: "Lyrics"
        }
        else -> l.plain.lineSequence().firstOrNull { it.isNotBlank() } ?: "Lyrics"
    }

    BackHandler { if (panel != Panel.None) panel = Panel.None else onClose() }

    Box(Modifier.fillMaxSize().offset { IntOffset(0, drag.roundToInt().coerceAtLeast(0)) }.background(Milo.bg)) {
        Column(Modifier.fillMaxSize()) {
            // art: edge to edge, swipe down anywhere on it to go back
            Box(
                Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(bottomStart = 38.dp, bottomEnd = 38.dp)).background(Milo.card2).pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { _, dy -> drag += dy },
                        onDragEnd = { if (drag > 220f) onClose(); drag = 0f },
                        onDragCancel = { drag = 0f },
                    )
                }
            ) {
                if (cover != null) {
                    Image(cover.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, filterQuality = androidx.compose.ui.graphics.FilterQuality.High)
                } else {
                    Icon(Icons.Rounded.MusicNote, null, tint = Milo.dim, modifier = Modifier.align(Alignment.Center).size(96.dp))
                }
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.35f), 0.3f to Color.Transparent, 0.6f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.85f))))

                RoundBtn(Icons.Rounded.KeyboardArrowDown, "Back", Modifier.align(Alignment.TopStart).statusBarsPadding().padding(12.dp), onClick = onClose)

                Column(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    RoundBtn(Icons.Rounded.QueueMusic, "Queue") { panel = Panel.Queue }
                    RoundBtn(if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Like", tint = if (liked) Milo.amber else Color.White) { vm.toggleFav(t.id) }
                    RoundBtn(Icons.Rounded.Tune, "Sound") { panel = Panel.Sound }
                    MoreMenu(vm, t, ui)
                }

                Column(Modifier.align(Alignment.BottomStart).padding(start = 22.dp, end = 80.dp, bottom = 22.dp)) {
                    Text(t.title, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 36.sp)
                    Text(t.artist, color = Color(0xFFEDE8DC), fontSize = 20.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        t.album + if (t.year >= 1900) " · ${t.year}" else "",
                        color = Color(0xFFC9C3B6), fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.height(8.dp)) // black canvas shows between the art and the control card, like the web

            // control card
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp)).background(Milo.panel)
                    .navigationBarsPadding().padding(top = 16.dp, bottom = 6.dp),
            ) {
                Scrubber(ui, Modifier.padding(horizontal = 22.dp)) { vm.player.seekTo(it) }
                Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(fmtTime(ui.positionMs), color = Milo.text.copy(alpha = 0.85f), fontSize = 14.sp)
                    Text(fmtTime(ui.durationMs), color = Milo.text.copy(alpha = 0.85f), fontSize = 14.sp)
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconTap(Icons.Rounded.Shuffle, "Shuffle", 28.dp, if (ui.shuffle) Milo.amber else Milo.text) { vm.player.toggleShuffle() }
                    RoundDark(Icons.Rounded.SkipPrevious, "Previous") { vm.player.previous() }
                    Box(
                        Modifier.size(96.dp).clip(CircleShape).background(Milo.cream).clickable { vm.player.toggle() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(if (ui.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Play or pause", tint = Color.Black, modifier = Modifier.size(48.dp))
                    }
                    RoundDark(Icons.Rounded.SkipNext, "Next") { vm.player.next() }
                    IconTap(
                        if (ui.repeat == 1) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, "Repeat", 28.dp,
                        if (ui.repeat != 0) Milo.amber else Milo.text,
                    ) { vm.player.cycleRepeat() }
                }
                // lyrics handle: shows the line being sung; tap it or drag it up for the full lyrics page
                Column(
                    Modifier.fillMaxWidth().clickable { panel = Panel.Lyrics }.pointerInput(Unit) {
                        var total = 0f
                        detectVerticalDragGestures(
                            onVerticalDrag = { _, dy -> total += dy },
                            onDragEnd = { if (total < -30f) panel = Panel.Lyrics; total = 0f },
                            onDragCancel = { total = 0f },
                        )
                    }.padding(top = 8.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.width(80.dp).height(5.dp).clip(CircleShape).background(Milo.text.copy(alpha = 0.3f)))
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (lyricsLoading) "Lyrics" else preview, color = Milo.text.copy(alpha = 0.7f), fontSize = 17.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 24.dp),
                    )
                }
            }
        }

        when (panel) {
            Panel.Queue -> Sheet(0.72f, null, { panel = Panel.None }) { QueueContent(vm, ui) }
            Panel.Sound -> Sheet(0.8f, null, { panel = Panel.None }) { SoundContent(vm, ui) }
            Panel.Lyrics -> LyricsPage(vm, t, ui, soft, lyrics, lyricsLoading) { panel = Panel.None }
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
    LaunchedEffect(Unit) { AudioFx.ensure() }
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
private fun LyricsPage(vm: MainViewModel, t: Track, ui: PlayerUi, soft: Bitmap?, lyrics: Lyrics?, loading: Boolean, onBack: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    Box(Modifier.fillMaxSize().artBackdrop(soft, 0.62f).clickable(enabled = false) {}) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 22.dp, vertical = 12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Back", color = Milo.text, fontSize = 19.sp, modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onBack).padding(vertical = 10.dp, horizontal = 2.dp))
                Spacer(Modifier.weight(1f))
                if (lyrics != null) {
                    Box(
                        Modifier.size(48.dp).clip(CircleShape).clickable {
                            val text = if (lyrics.synced.isNotEmpty()) lyrics.synced.joinToString("\n") { it.text } else lyrics.plain
                            val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, t.title + " · " + t.artist + "\n\n" + text)
                            }
                            ctx.startActivity(android.content.Intent.createChooser(send, "Share lyrics").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                        },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.IosShare, "Share lyrics", tint = Milo.text, modifier = Modifier.size(26.dp)) }
                    Spacer(Modifier.width(10.dp))
                }
                Box(
                    Modifier.size(58.dp).clip(CircleShape).background(Milo.cream).clickable { vm.player.toggle() },
                    contentAlignment = Alignment.Center,
                ) { Icon(if (ui.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Play or pause", tint = Color.Black, modifier = Modifier.size(32.dp)) }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                com.ubayanda.milonative.ui.components.Cover(vm, t, 86.dp, 22.dp, px = 300)
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Text(t.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        t.artist + if (lyrics != null && lyrics.synced.isNotEmpty()) " · Synced · online" else if (lyrics != null) " · online" else "",
                        color = Color(0xFFD6D0C4), fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    loading -> Text("Finding lyrics…", color = Milo.dim, fontSize = 18.sp, modifier = Modifier.padding(vertical = 24.dp))
                    lyrics == null -> Text("No lyrics found for this song, or you're offline.", color = Milo.dim, fontSize = 18.sp, modifier = Modifier.padding(vertical = 24.dp))
                    lyrics.synced.isNotEmpty() -> {
                        val state = rememberLazyListState()
                        val idx = lyrics.synced.indexOfLast { it.timeMs <= ui.positionMs + 250 }
                        LaunchedEffect(idx) { if (idx >= 0) state.animateScrollToItem((idx - 2).coerceAtLeast(0)) }
                        LazyColumn(state = state, contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 20.dp)) {
                            itemsIndexed(lyrics.synced) { i, line ->
                                val on = i == idx
                                Text(
                                    line.text.ifEmpty { "•   •   •" },
                                    color = Color.White.copy(alpha = if (on) 1f else 0.38f),
                                    fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp,
                                    modifier = Modifier.fillMaxWidth().clickable { vm.player.seekTo(line.timeMs) }.padding(vertical = 12.dp),
                                )
                            }
                        }
                    }
                    else -> Text(
                        lyrics.plain, color = Color.White, fontSize = 22.sp, lineHeight = 32.sp,
                        modifier = Modifier.verticalScroll(rememberScrollState()).padding(vertical = 20.dp),
                    )
                }
            }
            Scrubber(ui, Modifier) { vm.player.seekTo(it) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(fmtTime(ui.positionMs), color = Milo.text.copy(alpha = 0.85f), fontSize = 14.sp)
                Text(fmtTime(ui.durationMs), color = Milo.text.copy(alpha = 0.85f), fontSize = 14.sp)
            }
        }
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
        modifier.size(52.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.32f)).clickable(onClick = onClick),
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
private fun Scrubber(ui: PlayerUi, modifier: Modifier = Modifier, onSeek: (Long) -> Unit) {
    var dragging by remember { mutableStateOf(false) }
    var dragFrac by remember { mutableFloatStateOf(0f) }
    val frac = if (ui.durationMs > 0) (ui.positionMs.toFloat() / ui.durationMs).coerceIn(0f, 1f) else 0f
    val shown = if (dragging) dragFrac else frac
    val dur = ui.durationMs
    val ring = Milo.panel
    Box(
        modifier.fillMaxWidth().height(40.dp)
            .pointerInput(dur) { detectTapGestures { off -> if (dur > 0) onSeek((off.x / size.width * dur).toLong()) } }
            .pointerInput(dur) {
                detectHorizontalDragGestures(
                    onDragStart = { off -> dragging = true; dragFrac = (off.x / size.width).coerceIn(0f, 1f) },
                    onHorizontalDrag = { change, _ -> dragFrac = (change.position.x / size.width).coerceIn(0f, 1f) },
                    onDragEnd = { if (dur > 0) onSeek((dragFrac * dur).toLong()); dragging = false },
                    onDragCancel = { dragging = false },
                )
            }
    ) {
        Canvas(Modifier.matchParentSize()) {
            val cy = size.height / 2
            val th = 4.dp.toPx()
            val r = 14.dp.toPx()
            drawRoundRect(Color.White.copy(alpha = 0.28f), Offset(0f, cy - th / 2), Size(size.width, th), CornerRadius(th / 2))
            val px = (size.width * shown).coerceIn(r, size.width - r)
            drawRoundRect(Color.White, Offset(0f, cy - th / 2), Size(px, th), CornerRadius(th / 2))
            drawCircle(Color.White, r, Offset(px, cy))
            drawCircle(ring, 4.5.dp.toPx(), Offset(px, cy), style = Stroke(2.dp.toPx()))
        }
    }
}

@Composable
private fun RoundDark(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        Modifier.size(70.dp).clip(CircleShape).background(Milo.card2).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, tint = Milo.text, modifier = Modifier.size(34.dp)) }
}
