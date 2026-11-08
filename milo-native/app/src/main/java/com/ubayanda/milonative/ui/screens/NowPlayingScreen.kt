package com.ubayanda.milonative.ui.screens

import com.ubayanda.milonative.ui.components.MiloSlider
import com.ubayanda.milonative.ui.components.pressable
import androidx.compose.animation.togetherWith
import android.graphics.Bitmap
import com.ubayanda.milonative.ui.components.shimmer
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.BluetoothAudio
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
import kotlinx.coroutines.launch
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import com.ubayanda.milonative.ui.components.ReorderState
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
import androidx.compose.ui.text.withStyle
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
import com.ubayanda.milonative.ui.theme.Rad
import com.ubayanda.milonative.ui.theme.Sp
import com.ubayanda.milonative.ui.theme.Ty
import com.ubayanda.milonative.util.fmtTime
import kotlin.math.roundToInt
import androidx.compose.ui.draw.blur
import com.ubayanda.milonative.ui.components.frosted
import com.ubayanda.milonative.ui.components.backdropSource
import com.ubayanda.milonative.ui.components.liquidGlass
import com.ubayanda.milonative.ui.components.glassStroke
import com.ubayanda.milonative.ui.components.lift

private enum class Panel { None, Queue, Sound, Lyrics, Visual }

private class ArtKey(val id: Long, val bmp: Bitmap?, val done: Boolean)

@Composable
fun NowPlayingScreen(vm: MainViewModel, t: Track, ui: PlayerUi, liked: Boolean, onClose: () -> Unit) {
    var panel by remember { mutableStateOf(Panel.None) }
    var drag by remember { mutableFloatStateOf(0f) }
    var castOpen by remember { mutableStateOf(false) }
    if (castOpen) com.ubayanda.milonative.ui.components.CastDialog { castOpen = false }
    // the cover for THIS song only (never the previous song's picture while the new one loads)
    val coverPair by androidx.compose.runtime.produceState<Pair<Long, Bitmap?>>(t.id to null, t.id) { value = t.id to vm.art.cover(t, 1000) }
    val cover = if (coverPair.first == t.id) coverPair.second else null
    val coverDone = coverPair.first == t.id
    val soft = rememberSoft(vm, t).value
    val cardShape = RoundedCornerShape(topStart = Rad.xl, topEnd = Rad.xl)
    val npBackdrop = com.ubayanda.milonative.ui.components.rememberBackdropState()
    val npLive = npBackdrop.takeIf { android.os.Build.VERSION.SDK_INT >= 31 }
    val npGlass = if (npLive != null) Modifier.liquidGlass(cardShape, Rad.xl, npLive, Color(0xFF0B0A09), 1.2f, tintAlpha = 0.5f)
        else Modifier.clip(cardShape).frosted(soft, 0.5f).border(1.dp, glassStroke(1.2f), cardShape)
    // behind a queue / sound sheet the player blurs (Android 12+; older phones just get the dim)
    val behindBlur by androidx.compose.animation.core.animateDpAsState(if (panel == Panel.Queue || panel == Panel.Sound) 22.dp else 0.dp, label = "npBlur")

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
        // the cover, softened, fills the whole screen behind everything so the glass card has something to frost
        Box(Modifier.fillMaxSize().backdropSource(npBackdrop)) {
            soft?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize().blur(behindBlur), contentScale = ContentScale.Crop, alpha = 0.5f) }
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))
        }
        Column(Modifier.fillMaxSize().blur(behindBlur)) {
            // art: edge to edge, swipe down anywhere on it to go back
            Box(
                Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(bottomStart = Rad.xl, bottomEnd = Rad.xl)).background(Milo.card2).pointerInput("swipe-skip") {
                    // swipe the art sideways: left = next song, right = previous
                    var hx = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { hx = 0f },
                        onHorizontalDrag = { _, d -> hx += d },
                        onDragEnd = { if (hx < -160f) vm.player.next() else if (hx > 160f) vm.player.previous(); hx = 0f },
                        onDragCancel = { hx = 0f },
                    )
                }.pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { _, dy -> drag += dy },
                        onDragEnd = { if (drag > 220f) onClose(); drag = 0f },
                        onDragCancel = { drag = 0f },
                    )
                }
            ) {
                // next / previous: the new cover glides in from the side it came from while the old one slips away
                androidx.compose.animation.AnimatedContent(
                    targetState = ArtKey(t.id, cover, coverDone),
                    contentKey = { it.id },
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = {
                        val d = vm.player.navDir
                        (androidx.compose.animation.slideInHorizontally(androidx.compose.animation.core.tween(420, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { (it * 0.22f * d).toInt() } +
                            androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(380)) +
                            androidx.compose.animation.scaleIn(androidx.compose.animation.core.tween(420, easing = androidx.compose.animation.core.FastOutSlowInEasing), initialScale = 0.94f))
                            .togetherWith(
                                androidx.compose.animation.slideOutHorizontally(androidx.compose.animation.core.tween(320, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { (-it * 0.12f * d).toInt() } +
                                    androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(240)),
                            )
                    },
                    label = "cover",
                ) { k ->
                    Box(Modifier.fillMaxSize()) {
                        if (k.bmp == null && !k.done) Box(Modifier.fillMaxSize().shimmer())
                        androidx.compose.animation.Crossfade(targetState = k.bmp, animationSpec = androidx.compose.animation.core.tween(320), label = "coverFade") { b ->
                            if (b != null) Image(b.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, filterQuality = androidx.compose.ui.graphics.FilterQuality.High)
                            else if (k.done) Icon(Icons.Rounded.MusicNote, null, tint = Milo.dim, modifier = Modifier.align(Alignment.Center).size(96.dp))
                        }
                    }
                }
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.35f), 0.3f to Color.Transparent, 0.6f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.85f))))

                RoundBtn(Icons.Rounded.KeyboardArrowDown, "Back", Modifier.align(Alignment.TopStart).statusBarsPadding().padding(12.dp), onClick = onClose)

                Column(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    RoundBtn(Icons.Rounded.QueueMusic, "Queue") { panel = Panel.Queue }
                    RoundBtn(if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Like", tint = if (liked) Milo.amber else Color.White) { vm.toggleFav(t.id) }
                    if (t.uri.startsWith("yt:")) {
                        val savedNow = com.ubayanda.milonative.data.OfflineStore.items.collectAsState().value.any { it.id == t.uri.removePrefix("yt:") }
                        RoundBtn(if (savedNow) Icons.Rounded.DownloadDone else Icons.Rounded.Download, "Download", tint = if (savedNow) Milo.amber else Color.White) { vm.downloadTracks(listOf(t)) }
                    }
                    RoundBtn(Icons.Rounded.Tune, "Sound") { panel = Panel.Sound }
                    RoundBtn(Icons.Rounded.GraphicEq, "Visualiser") { panel = Panel.Visual }
                    RoundBtn(Icons.Rounded.Cast, "Cast") { castOpen = true }
                    MoreMenu(vm, t, ui)
                }

                androidx.compose.animation.AnimatedContent(
                    targetState = t, contentKey = { it.id },
                    modifier = Modifier.align(Alignment.BottomStart),
                    transitionSpec = {
                        (androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(360, delayMillis = 90)) + androidx.compose.animation.slideInVertically(androidx.compose.animation.core.tween(380, delayMillis = 90)) { it / 6 })
                            .togetherWith(androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(160)))
                    },
                    label = "titles",
                ) { tt ->
                    Column(Modifier.padding(start = Sp.gutter, end = 80.dp, bottom = Sp.x6)) {
                        Text(tt.title, color = Color.White, style = Ty.display, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(tt.artist, color = Color(0xFFEDE8DC), style = Ty.heading, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = Sp.x1))
                        Text(
                            tt.album + if (tt.year >= 1900) " · ${tt.year}" else "",
                            color = Color(0xFFC9C3B6), style = Ty.caption, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        if (ui.quality.isNotEmpty()) Text(
                            ui.quality, color = Milo.amber, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                            modifier = Modifier.padding(top = 6.dp).clip(RoundedCornerShape(6.dp)).border(1.dp, Milo.amber.copy(alpha = 0.6f), RoundedCornerShape(6.dp)).padding(horizontal = 7.dp, vertical = 2.dp),
                        )
                        val outNow by com.ubayanda.milonative.player.BtOutput.now.collectAsState()
                        if (outNow.kind != "speaker") Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (outNow.bt) Icons.Rounded.BluetoothAudio else Icons.Rounded.Headphones, null, tint = Milo.amber, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(outNow.name, color = Milo.amber, style = Ty.caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp)) // black canvas shows between the art and the control card, like the web

            // control card
            Column(
                Modifier.fillMaxWidth()
                    .lift(cardShape, 18f).then(npGlass)
                    .navigationBarsPadding().padding(top = Sp.x5, bottom = Sp.x2),
            ) {
                Scrubber(ui, Modifier.padding(horizontal = Sp.gutter)) { vm.player.seekTo(it) }
                Row(Modifier.fillMaxWidth().padding(horizontal = Sp.x6), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(fmtTime(ui.positionMs), color = Milo.dim, style = Ty.caption)
                    Text(fmtTime(ui.durationMs), color = Milo.dim, style = Ty.caption)
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = Sp.x4, vertical = Sp.x3),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconTap(Icons.Rounded.Shuffle, "Shuffle", 28.dp, if (ui.shuffle) Milo.amber else Milo.text) { vm.player.toggleShuffle() }
                    RoundDark(Icons.Rounded.SkipPrevious, "Previous") { vm.player.previous() }
                    Box(
                        Modifier.size(88.dp).clip(CircleShape).background(Milo.cream).pressable { vm.player.toggle() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(if (ui.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Play or pause", tint = Color.Black, modifier = Modifier.size(46.dp))
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
                        if (lyricsLoading) "Lyrics" else preview, color = Milo.text.copy(alpha = 0.7f), style = Ty.body,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = Sp.x6),
                    )
                }
            }
        }

        when (panel) {
            Panel.Queue -> Sheet(0.72f, soft, { panel = Panel.None }) { QueueContent(vm, ui) }
            Panel.Sound -> Sheet(0.8f, soft, { panel = Panel.None }) { SoundContent(vm, ui) }
            Panel.Lyrics -> LyricsPage(vm, t, ui, soft, lyrics, lyricsLoading) { panel = Panel.None }
            Panel.Visual -> VisualPage(vm, t, ui, soft) { panel = Panel.None }
            Panel.None -> {}
        }
    }
}

@Composable
private fun Sheet(frac: Float, backdrop: Bitmap?, onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable(onClick = onClose)) {
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(frac)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)).frosted(backdrop, 0.6f)
                .border(1.dp, glassStroke(1.2f), RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .clickable(enabled = false) {}.navigationBarsPadding(),
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp).width(40.dp).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f)))
            content()
        }
    }
}

@Composable
private fun QueueContent(vm: MainViewModel, ui: PlayerUi) {
    val listState = rememberLazyListState()
    var local by remember(ui.queueIds) { mutableStateOf(ui.queueIds.mapNotNull { vm.trackById(it) }) }
    // same song twice in the queue still needs distinct keys
    val keys = remember(local) {
        val seen = HashMap<Long, Int>()
        local.map { t -> val n = seen.getOrDefault(t.id, 0); seen[t.id] = n + 1; "q${t.id}#$n" }
    }
    val reorder = remember(listState) {
        ReorderState(
            listState, 1,
            { f, t -> local = local.toMutableList().also { it.add(t, it.removeAt(f)) } },
            { f, t -> vm.player.moveInQueue(f, t) },
        )
    }
    LazyColumn(state = listState) {
        item {
            val auto by vm.autoplay.collectAsState()
            val autoIds by vm.autoIds.collectAsState()
            val added = local.count { it.id in autoIds }
            Column(Modifier.padding(Sp.gutter)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Up next", color = Color.White, style = Ty.title, modifier = Modifier.weight(1f))
                    MiloChip(if (auto) "Autoplay on" else "Autoplay off", auto) { vm.setAutoplay(!auto) }
                }
                Text(
                    if (!auto) "Turn Autoplay on and milo. keeps similar songs coming when the queue runs low."
                    else if (added > 0) "Autoplay added $added similar song" + (if (added == 1) "" else "s") + " to this queue."
                    else "milo. adds similar songs when the queue runs low.",
                    color = Milo.dim, style = Ty.caption, modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        itemsIndexed(local, key = { i, _ -> keys.getOrElse(i) { "q$i" } }) { i, t ->
            val key = keys.getOrElse(i) { "q$i" }
            val idx by rememberUpdatedState(i)
            val dragging = reorder.isDragging(key)
            Box(
                if (dragging) Modifier.zIndex(1f).graphicsLayer { translationY = reorder.offset }
                else Modifier.animateItem()
            ) {
                TrackRow(
                    vm, t, t.id == ui.currentId, Milo.amber, listOf(RowAction("Remove from queue") { vm.player.removeFromQueue(i) }),
                    handle = reorder.handle({ idx }, key),
                ) { vm.player.playQueueIndex(i) }
            }
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

        Label("Crossfade")
        var xf by remember { mutableStateOf(vm.player.crossfade().toFloat()) }
        Text(
            if (xf < 1f) "Off. Songs play gaplessly back to back." else "${xf.roundToInt()} s: the end of one song overlaps the start of the next, fading out while the next fades in.",
            color = Milo.dim, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp),
        )
        MiloSlider(
            value = xf, onValueChange = { xf = it; vm.player.setCrossfade(it.roundToInt()) }, valueRange = 0f..12f, steps = 11,
            colors = amberSlider(), modifier = Modifier.padding(horizontal = 20.dp),
        )

        Label("Loudness matching")
        val fxp = androidx.compose.ui.platform.LocalContext.current.getSharedPreferences("milo_fx", 0)
        var rg by remember { mutableStateOf(fxp.getBoolean("rg", true)) }
        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Turns louder songs down to match quieter ones, using ReplayGain tags in your files and YouTube's own loudness. It never boosts, so nothing distorts. Applies from the next song.", color = Milo.dim, fontSize = 13.sp, modifier = Modifier.weight(1f))
            com.ubayanda.milonative.ui.components.MiloSwitch(checked = rg, onCheckedChange = { rg = it; fxp.edit().putBoolean("rg", it).apply() },
                colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = Milo.amber))
        }

        ExtrasSection(ui)

        Label("Equaliser")
        com.ubayanda.milonative.ui.components.GraphicEqPanel()

        var fine by remember { mutableStateOf(false) }
        Label("Fine tuning")
        MiloChip(if (fine) "Hide parametric EQ" else "Show parametric EQ", fine, Modifier.padding(horizontal = 20.dp)) { fine = !fine }
        if (fine) ParametricSection()

        if (fx.available) {
            Label("Effects")
            Text("Bass boost", color = Milo.dim, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp))
            MiloSlider(
                value = fx.bass.toFloat(), onValueChange = { AudioFx.setBass(it.roundToInt()) }, valueRange = 0f..1000f,
                colors = amberSlider(), modifier = Modifier.padding(horizontal = 20.dp),
            )
            Text("Spatial", color = Milo.dim, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp))
            MiloSlider(
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
private fun ExtrasSection(ui: PlayerUi) {
    val ex by com.ubayanda.milonative.player.Extras.state.collectAsState()
    val lp by com.ubayanda.milonative.player.Extras.loop.collectAsState()
    val sw = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = Milo.amber)
    Label("Listening extras")
    Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Skip silence between and inside songs", color = Milo.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
        com.ubayanda.milonative.ui.components.MiloSwitch(checked = ex.skipSilence, onCheckedChange = { com.ubayanda.milonative.player.Extras.set(ex.copy(skipSilence = it)) }, colors = sw)
    }
    Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Mono (both ears hear the same mix)", color = Milo.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
        com.ubayanda.milonative.ui.components.MiloSwitch(checked = ex.mono, onCheckedChange = { com.ubayanda.milonative.player.Extras.set(ex.copy(mono = it)) }, colors = sw)
    }
    Text(
        "Balance  " + if (ex.balance < -0.02f) "left ${(-ex.balance * 100).roundToInt()}%" else if (ex.balance > 0.02f) "right ${(ex.balance * 100).roundToInt()}%" else "centre",
        color = Milo.dim, fontSize = 13.sp, modifier = Modifier.padding(start = 20.dp, top = 6.dp),
    )
    MiloSlider(
        value = ex.balance, onValueChange = { com.ubayanda.milonative.player.Extras.set(ex.copy(balance = if (kotlin.math.abs(it) < 0.04f) 0f else it)) },
        valueRange = -1f..1f, colors = amberSlider(), modifier = Modifier.padding(horizontal = 20.dp),
    )
    val loopText = when {
        lp.a < 0 -> "Set A"
        lp.b < 0 -> "Set B (A at ${fmtTime(lp.a)})"
        else -> "Looping ${fmtTime(lp.a)} to ${fmtTime(lp.b)}. Tap to clear"
    }
    Text(
        "A-B loop: " + loopText, color = Milo.amber, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp).clip(RoundedCornerShape(12.dp))
            .clickable { com.ubayanda.milonative.player.Extras.markLoop(ui.positionMs) }.padding(horizontal = 6.dp, vertical = 8.dp),
    )
}

@Composable
private fun Label(text: String) {
    Text(text, color = Milo.dim, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp, modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 8.dp))
}

@Composable
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
private fun LyricsPage(vm: MainViewModel, t: Track, ui: PlayerUi, soft: Bitmap?, lyrics0: Lyrics?, loading: Boolean, onBack: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val fx = remember { ctx.getSharedPreferences("milo_fx", 0) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var fresh by remember(t.id) { mutableStateOf<Lyrics?>(null) }
    var rescanning by remember(t.id) { mutableStateOf(false) }
    val lyrics = fresh ?: lyrics0
    var wordMode by remember { mutableStateOf(fx.getBoolean("lyword", false)) }
    var lyOff by remember(t.id) { mutableStateOf(fx.getLong("lyoff_" + t.id, 0L)) }
    var moreMenu by remember { mutableStateOf(false) }
    var lang by remember { mutableStateOf(fx.getString("lylang", "") ?: "") }
    var langMenu by remember { mutableStateOf(false) }
    val translator = remember { com.ubayanda.milonative.data.LyricsTranslator(ctx) }
    val srcLines = remember(lyrics) { lyrics?.let { if (it.synced.isNotEmpty()) it.synced.map { l -> l.text } else it.plain.lines() } ?: emptyList() }
    val tr by androidx.compose.runtime.produceState<List<String>?>(null, lyrics, lang) {
        value = null
        if (lang.isNotEmpty() && srcLines.isNotEmpty()) {
            val r = translator.translate(srcLines, lang)
            if (r == null) android.widget.Toast.makeText(ctx, "Couldn't translate right now. Check your connection.", android.widget.Toast.LENGTH_SHORT).show()
            value = r
        }
    }
    val coverBmp by com.ubayanda.milonative.ui.components.rememberCover(vm, t, 400)
    val tintNow = vm.tint.collectAsState().value
    fun card(lines: List<String>) = com.ubayanda.milonative.data.ShareCard.share(ctx, t.title, t.artist, lines, coverBmp, tintNow)
    Box(Modifier.fillMaxSize().artBackdrop(soft, 0.62f).clickable(enabled = false) {}) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 22.dp, vertical = 12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Back", color = Milo.text, fontSize = 19.sp, modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onBack).padding(vertical = 10.dp, horizontal = 2.dp))
                Spacer(Modifier.weight(1f))
                if (lyrics != null) {
                    Box {
                        Box(Modifier.size(48.dp).clip(CircleShape).clickable { langMenu = true }, contentAlignment = Alignment.Center) {
                            Text("A文", color = if (lang.isEmpty()) Milo.dim else Milo.amber, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        DropdownMenu(expanded = langMenu, onDismissRequest = { langMenu = false }) {
                            DropdownMenuItem(text = { Text("Translation off") }, onClick = { langMenu = false; lang = ""; fx.edit().putString("lylang", "").apply() })
                            com.ubayanda.milonative.data.LyricsTranslator.languages.forEach { (code, name) ->
                                DropdownMenuItem(text = { Text("Translate to $name") }, onClick = { langMenu = false; lang = code; fx.edit().putString("lylang", code).apply() })
                            }
                        }
                    }
                    Spacer(Modifier.width(4.dp))
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
                Box {
                    Box(Modifier.size(48.dp).clip(CircleShape).clickable { moreMenu = true }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.MoreHoriz, "Lyrics options", tint = Milo.text, modifier = Modifier.size(26.dp))
                    }
                    DropdownMenu(expanded = moreMenu, onDismissRequest = { moreMenu = false }) {
                        DropdownMenuItem(text = { Text(if (rescanning) "Rescanning\u2026" else "Rescan lyrics") }, onClick = {
                            moreMenu = false
                            if (!rescanning) scope.launch {
                                rescanning = true
                                val r = vm.rescanLyrics(t)
                                rescanning = false
                                if (r != null) fresh = r else android.widget.Toast.makeText(ctx, "No lyrics found for this song.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        })
                        DropdownMenuItem(text = { Text(if (wordMode) "Word highlight: on" else "Word highlight: off") }, onClick = {
                            wordMode = !wordMode; fx.edit().putBoolean("lyword", wordMode).apply(); moreMenu = false
                        })
                        DropdownMenuItem(text = { Text("Lyrics earlier (\u22120.5 s)") }, onClick = { lyOff -= 500; fx.edit().putLong("lyoff_" + t.id, lyOff).apply() })
                        DropdownMenuItem(text = { Text("Lyrics later (+0.5 s)") }, onClick = { lyOff += 500; fx.edit().putLong("lyoff_" + t.id, lyOff).apply() })
                        if (lyOff != 0L) DropdownMenuItem(text = { Text("Reset timing (" + (if (lyOff > 0) "+" else "") + (lyOff / 1000.0) + " s)") }, onClick = { lyOff = 0; fx.edit().remove("lyoff_" + t.id).apply(); moreMenu = false })
                    }
                }
                Spacer(Modifier.width(6.dp))
                Box(
                    Modifier.size(58.dp).clip(CircleShape).background(Milo.cream).pressable { vm.player.toggle() },
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
            if (lyrics != null && lyrics.note.isNotEmpty()) Text(lyrics.note, color = Milo.amber, fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(8.dp))
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    loading || rescanning -> com.ubayanda.milonative.ui.components.SkeletonLines()
                    lyrics == null -> Text("No lyrics found for this song, or you're offline.", color = Milo.dim, fontSize = 18.sp, modifier = Modifier.padding(vertical = 24.dp))
                    lyrics.synced.isNotEmpty() -> {
                        val state = rememberLazyListState()
                        val btNow by com.ubayanda.milonative.player.BtOutput.now.collectAsState()
                        val btDelay = if (btNow.bt) com.ubayanda.milonative.player.BtOutput.lyricDelay(ctx) else 0
                        val pos = ui.positionMs - btDelay - lyOff
                        val idx = lyrics.synced.indexOfLast { it.timeMs <= pos + 250 }
                        LaunchedEffect(idx) { if (idx >= 0) state.animateScrollToItem((idx - 2).coerceAtLeast(0)) }
                        LazyColumn(state = state, contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 20.dp)) {
                            itemsIndexed(lyrics.synced, contentType = { _, _ -> "line" }) { i, line ->
                                val on = i == idx
                                Column(
                                    Modifier.fillMaxWidth().combinedClickable(
                                        onClick = { vm.player.seekTo(line.timeMs) },
                                        onLongClick = { card(listOf(line.text, lyrics.synced.getOrNull(i + 1)?.text.orEmpty())) },
                                    ).padding(vertical = 12.dp),
                                ) {
                                    val words = if (on) line.words.ifEmpty { if (wordMode) estimateWords(line, lyrics.synced.getOrNull(i + 1)?.timeMs) else emptyList() } else emptyList()
                                    if (on && words.isNotEmpty()) {
                                        // word-by-word: each word fills in as it is sung (timed words when the lyrics have them, estimated from the line otherwise)
                                        val lineEnd = lyrics.synced.getOrNull(i + 1)?.timeMs ?: (line.timeMs + 4000)
                                        Text(
                                            androidx.compose.ui.text.buildAnnotatedString {
                                                words.forEachIndexed { wi, w ->
                                                    val end = words.getOrNull(wi + 1)?.timeMs ?: minOf(lineEnd, w.timeMs + 900)
                                                    val p = ((pos - w.timeMs).toFloat() / (end - w.timeMs).coerceAtLeast(1)).coerceIn(0f, 1f)
                                                    withStyle(androidx.compose.ui.text.SpanStyle(color = Color.White.copy(alpha = 0.42f + 0.58f * p))) { append(w.text) }
                                                    if (wi < words.lastIndex) append(" ")
                                                }
                                            },
                                            fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp,
                                        )
                                    } else Text(
                                        line.text.ifEmpty { "•   •   •" },
                                        color = Color.White.copy(alpha = if (on) 1f else 0.38f),
                                        fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp,
                                    )
                                    val tl = tr?.getOrNull(i).orEmpty()
                                    if (tl.isNotBlank() && !tl.equals(line.text, ignoreCase = true)) {
                                        Text(tl, color = Milo.amber.copy(alpha = if (on) 1f else 0.5f), fontSize = 18.sp, lineHeight = 24.sp, modifier = Modifier.padding(top = 4.dp))
                                    }
                                }
                            }
                        }
                    }
                    else -> Column(Modifier.verticalScroll(rememberScrollState()).padding(vertical = 20.dp)) {
                        val ls = lyrics.plain.lines()
                        ls.forEachIndexed { i, l ->
                            Text(l, color = Color.White, fontSize = 22.sp, lineHeight = 32.sp,
                                modifier = Modifier.fillMaxWidth().combinedClickable(onClick = {}, onLongClick = { card(ls.drop(i).take(3)) }))
                            val tl = tr?.getOrNull(i).orEmpty()
                            if (tl.isNotBlank() && !tl.equals(l, ignoreCase = true)) Text(tl, color = Milo.amber, fontSize = 16.sp, lineHeight = 22.sp)
                        }
                    }
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
            DropdownMenuItem(text = { Text("Car mode") }, onClick = { open = false; actions.carMode() })
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
            val th = 3.dp.toPx()
            val r = 9.dp.toPx()
            drawRoundRect(Color.White.copy(alpha = 0.28f), Offset(0f, cy - th / 2), Size(size.width, th), CornerRadius(th / 2))
            val px = (size.width * shown).coerceIn(r, size.width - r)
            drawRoundRect(Color.White, Offset(0f, cy - th / 2), Size(px, th), CornerRadius(th / 2))
            drawCircle(Color.White, r, Offset(px, cy))
            drawCircle(ring, 3.dp.toPx(), Offset(px, cy), style = Stroke(1.5.dp.toPx()))
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


/** Full-screen visualiser: the cover softly behind, big mirrored bars in the song's colour, title and controls kept simple. */
@Composable
private fun VisualPage(vm: MainViewModel, t: Track, ui: PlayerUi, soft: Bitmap?, onBack: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val tint = vm.tint.collectAsState().value
    val accent = if (tint == 0) Milo.amber else Color(0xFF000000.toInt() or tint)
    val coverBmp by com.ubayanda.milonative.ui.components.rememberCover(vm, t, 600)
    Box(Modifier.fillMaxSize().artBackdrop(soft, 0.62f).clickable(enabled = false) {}) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 22.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Back", color = Milo.text, fontSize = 19.sp, modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onBack).padding(vertical = 10.dp, horizontal = 2.dp))
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(58.dp).clip(CircleShape).background(Milo.cream).pressable { vm.player.toggle() }, contentAlignment = Alignment.Center) {
                    Icon(if (ui.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Play or pause", tint = Color.Black, modifier = Modifier.size(32.dp))
                }
            }
            Spacer(Modifier.weight(1f))
            coverBmp?.let { Image(it.asImageBitmap(), null, Modifier.size(220.dp).clip(RoundedCornerShape(28.dp)), contentScale = ContentScale.Crop) }
            Spacer(Modifier.height(24.dp))
            var vstyle by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(1) }
            com.ubayanda.milonative.ui.components.SpectrumView(accent, Modifier.clickable { vstyle = (vstyle + 1) % com.ubayanda.milonative.ui.components.SPECTRUM_STYLES }, height = 180.dp, style = vstyle)
            Text("Tap the visual to change its look", color = Milo.dim, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            Spacer(Modifier.height(24.dp))
            Text(t.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Text(t.artist, color = Color(0xFFD6D0C4), fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.weight(1f))
        }
    }
}


/** Five-band parametric equaliser, remembered for each output (phone speaker, each pair of headphones). */
@Composable
private fun ParametricSection() {
    val peq by com.ubayanda.milonative.player.ParamEq.state.collectAsState()
    Label("Parametric EQ")
    Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(if (peq.device == "default") "For: phone speaker and anything without its own curve" else "For: " + peq.device, color = Milo.amber, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("Set each band's frequency, gain and width (Q). It works on every output, and each pair of headphones keeps its own curve.", color = Milo.dim, fontSize = 13.sp)
        }
        com.ubayanda.milonative.ui.components.MiloSwitch(checked = peq.enabled, onCheckedChange = { com.ubayanda.milonative.player.ParamEq.setEnabled(it) },
            colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = Milo.amber))
    }
    if (peq.enabled) {
        peq.bands.forEachIndexed { i, b ->
            Column(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                Text("Band ${i + 1}  ·  " + (if (b.hz >= 1000f) "%.1f kHz".format(b.hz / 1000f) else "${b.hz.roundToInt()} Hz") + "  ·  " + "%+.1f dB".format(b.db) + "  ·  Q %.1f".format(b.q), color = Color.White, fontSize = 13.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Freq", color = Milo.dim, fontSize = 12.sp, modifier = Modifier.width(38.dp))
                    MiloSlider(
                        value = (Math.log10((b.hz / 20f).toDouble()) / 3.0).toFloat(), valueRange = 0f..1f,
                        onValueChange = { x -> com.ubayanda.milonative.player.ParamEq.setBand(i, b.copy(hz = (20.0 * Math.pow(1000.0, x.toDouble())).toFloat())) },
                        colors = amberSlider(), modifier = Modifier.weight(1f),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Gain", color = Milo.dim, fontSize = 12.sp, modifier = Modifier.width(38.dp))
                    MiloSlider(value = b.db, valueRange = -12f..12f, onValueChange = { x -> com.ubayanda.milonative.player.ParamEq.setBand(i, b.copy(db = (x * 2).roundToInt() / 2f)) }, colors = amberSlider(), modifier = Modifier.weight(1f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Q", color = Milo.dim, fontSize = 12.sp, modifier = Modifier.width(38.dp))
                    MiloSlider(value = b.q, valueRange = 0.3f..4f, onValueChange = { x -> com.ubayanda.milonative.player.ParamEq.setBand(i, b.copy(q = (x * 10).roundToInt() / 10f)) }, colors = amberSlider(), modifier = Modifier.weight(1f))
                }
            }
        }
        MiloChip("Reset curve", false, Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) { com.ubayanda.milonative.player.ParamEq.reset() }
    }
}


/** Lyrics that only have line times: spread the line across its words by length so a word-by-word highlight is still possible. */
private fun estimateWords(line: com.ubayanda.milonative.model.LyricLine, nextMs: Long?): List<com.ubayanda.milonative.model.LyricWord> {
    val tokens = line.text.split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (tokens.size < 2) return emptyList()
    val span = (minOf((nextMs ?: (line.timeMs + 4000)) - line.timeMs, 7000L) * 0.9).toLong().coerceAtLeast(600L)
    val weights = tokens.map { it.length + 2 }
    val total = weights.sum().toFloat()
    var acc = 0f
    return tokens.mapIndexed { i, w ->
        val start = line.timeMs + (span * acc / total).toLong()
        acc += weights[i]
        com.ubayanda.milonative.model.LyricWord(start, w)
    }
}
