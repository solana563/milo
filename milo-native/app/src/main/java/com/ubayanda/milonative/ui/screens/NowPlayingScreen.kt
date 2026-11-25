package com.ubayanda.milonative.ui.screens

import com.ubayanda.milonative.ui.player.LikeBurstOverlay
import com.ubayanda.milonative.ui.player.LikeBurstState
import com.ubayanda.milonative.ui.player.LikeSlot
import com.ubayanda.milonative.ui.player.likeButtonTarget
import com.ubayanda.milonative.ui.player.isDisc
import com.ubayanda.milonative.ui.player.sheetReveal
import com.ubayanda.milonative.ui.theme.MiloIcons
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.animateScrollBy
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
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.BluetoothAudio
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Forward10
import com.ubayanda.milonative.ui.components.LcdStrip
import com.ubayanda.milonative.ui.components.SpinningCd
import com.ubayanda.milonative.ui.theme.Y2K
import com.ubayanda.milonative.ui.theme.Spider
import com.ubayanda.milonative.ui.theme.Sticker
import com.ubayanda.milonative.ui.theme.Nebula
import com.ubayanda.milonative.ui.theme.ChromeStyle
import com.ubayanda.milonative.ui.components.ChromeFloat
import com.ubayanda.milonative.ui.components.drawChromeBall
import com.ubayanda.milonative.ui.components.chromeBrush
import com.ubayanda.milonative.ui.components.HelmetSticker
import com.ubayanda.milonative.ui.components.BuddySticker
import com.ubayanda.milonative.ui.components.StickerScatter
import com.ubayanda.milonative.ui.components.drawSparkle
import com.ubayanda.milonative.ui.components.HangingSpider
import com.ubayanda.milonative.ui.components.WebCorners
import com.ubayanda.milonative.ui.components.drawSpider
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Replay10
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
import androidx.compose.ui.graphics.SolidColor
import com.ubayanda.milonative.ui.components.wavyActive
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
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
import com.ubayanda.milonative.ui.components.cascadeIn
import com.ubayanda.milonative.ui.components.frosted
import com.ubayanda.milonative.ui.components.backdropSource
import com.ubayanda.milonative.ui.components.glassStroke
import com.ubayanda.milonative.ui.components.lift

/** The little bubble that confirms a double-tap on the art. [zone]: -1 back, 0 play/pause, 1 forward. */
private data class TapFlash(val zone: Int, val count: Int, val token: Long, val playing: Boolean = false)

private enum class Panel { None, Queue, Sound, Lyrics, Visual, Timer }

private class ArtKey(val id: Long, val bmp: Bitmap?, val done: Boolean)

@Composable
fun NowPlayingScreen(vm: MainViewModel, t: Track, ui: PlayerUi, liked: Boolean, onClose: () -> Unit) {
    var panel by remember { mutableStateOf(Panel.None) }
    var drag by remember { mutableFloatStateOf(0f) }
    // closing: the whole player follows your finger down and drops off the screen on the same bouncy spring the lyrics page uses
    var closeY by remember { mutableFloatStateOf(0f) }
    // 0 = folded into the mini player, 1 = open: the sheet grows out of the mini player and folds back into it (ui/player/MorphCover.kt)
    val openP = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) { openP.animateTo(1f, androidx.compose.animation.core.spring(0.86f, 240f)) }
    var morphArt by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var morphTitle by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var morphPlay by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var morphNext by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    val dragScope = androidx.compose.runtime.rememberCoroutineScope()
    val screenH = with(androidx.compose.ui.platform.LocalDensity.current) { androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp.toPx() }
    // the lyrics page rises with your finger and springs open (or back down): lyY = its distance from the top, screenH = closed
    var lyY by remember { mutableFloatStateOf(screenH) }
    var lyJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val lyScope = androidx.compose.runtime.rememberCoroutineScope()
    val lyHaptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val lyDens = androidx.compose.ui.platform.LocalDensity.current.density
    fun lyTo(target: Float) {
        lyJob?.cancel()
        val from = lyY
        val d = target - from
        if (target == 0f && from > 0f) lyHaptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
        if (kotlin.math.abs(d) < 1f) { lyY = target; return }
        // a visible bounce whatever the distance: the page glides to the target on a spring, overshoots by about 72 dp, then wobbles back (2-3 swings).
        // Closing never overshoots (that part is off screen), it just settles shut.
        val cap = 72f * lyDens
        val dir = if (d < 0f) -1f else 1f
        lyJob = lyScope.launch {
            var passed = false
            androidx.compose.animation.core.Animatable(0f).animateTo(1f, androidx.compose.animation.core.spring(0.424f, 200f, 0.002f)) {
                if (value >= 1f) passed = true
                lyY = if (!passed) from + d * value else if (target >= screenH) target else target + dir * (value - 1f) / 0.23f * cap
            }
            lyY = target
        }
    }
    val lyOpen = lyY < screenH - 0.5f
    var shut by remember { mutableStateOf(false) }
    // let go before the line: it springs back with a bounce; past it: it is thrown down the screen on the same spring, then closes
    fun springBackPlayer() { val from = closeY; dragScope.launch { androidx.compose.animation.core.Animatable(from).animateTo(0f, com.ubayanda.milonative.ui.components.Bounce.px()) { closeY = value } } }
    fun dismissPlayer() {
        if (shut) return
        shut = true
        val from = closeY
        dragScope.launch {
            var done = false
            androidx.compose.animation.core.Animatable(0f).animateTo(1f, androidx.compose.animation.core.spring(0.424f, 200f, 0.002f)) {
                closeY = if (value >= 1f) screenH else from + (screenH - from) * value
                if (value >= 1f && !done) { done = true; onClose() }
            }
        }
    }
    /** close from the back button / arrow: same drop as dragging it down */
    fun foldAway() = dismissPlayer()
    var castOpen by remember { mutableStateOf(false) }
    if (castOpen) com.ubayanda.milonative.ui.components.CastDialog { castOpen = false }
    // the cover for THIS song only (never the previous song's picture while the new one loads)
    val coverPair by androidx.compose.runtime.produceState<Pair<Long, Bitmap?>>(t.id to null, t.id) { value = t.id to vm.art.cover(t, 1000) }
    val cover = if (coverPair.first == t.id) coverPair.second else null
    val coverDone = coverPair.first == t.id
    val soft = rememberSoft(vm, t).value
    // double-tap the art: left = back 10 s, right = forward 10 s, middle = play / pause
    val uiNow by rememberUpdatedState(ui)
    val npCtx = androidx.compose.ui.platform.LocalContext.current
    var tapFlash by remember { mutableStateOf<TapFlash?>(null) }
    LaunchedEffect(tapFlash?.token) { if (tapFlash != null) { kotlinx.coroutines.delay(750); tapFlash = null } }
    val cardShape = RoundedCornerShape(Rad.xl)
    // flat cover-tinted card: no live blur or glass shader (they cost frames)
    val npGlass = Modifier.clip(cardShape).frosted(soft, 0.5f).border(1.dp, glassStroke(1.2f), cardShape)
    // behind a queue / sound sheet the player just dims (no blur)
    val behindDim by androidx.compose.animation.core.animateFloatAsState(if (panel == Panel.Queue || panel == Panel.Sound) 0.5f else 1f, label = "npDim")

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

    val likeBurst = remember { LikeBurstState() }
    BackHandler { if (lyOpen) lyTo(screenH) else if (panel != Panel.None) panel = Panel.None else foldAway() }

    val screenW = with(androidx.compose.ui.platform.LocalDensity.current) { androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp.toPx() }
    Box(Modifier.fillMaxSize().graphicsLayer {
        translationY = closeY
        if (closeY > 0f) {
            val r = 36.dp.toPx() * (closeY / screenH * 8f).coerceIn(0f, 1f)
            shape = RoundedCornerShape(topStart = r.toDp(), topEnd = r.toDp()); clip = true
        }
    }.sheetReveal({ com.ubayanda.milonative.ui.player.sheetProgress(openP.value, drag) }, screenW, screenH, 0f).background(Milo.bg)) {
        // Poster backdrop (Mochy Music): the song's colour fading into black, fully opaque
        val npAccent = vm.tint.collectAsState().value.let { if (it == 0) Milo.amber else Color(0xFF000000.toInt() or it) }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(npAccent.copy(alpha = 0.55f), Color(0xFF080809), Color.Black))))
        Column(Modifier.fillMaxSize().graphicsLayer { alpha = behindDim }.pointerInput("lyrics-swipe-all") {
            // swipe up anywhere on the player (title, gaps, buttons area) that a child did not already use
            var up = false
            var decided = false
            detectVerticalDragGestures(
                onDragStart = { lyJob?.cancel(); up = false; decided = false },
                onVerticalDrag = { c, dy ->
                    if (!decided && dy != 0f) { decided = true; up = dy < 0f || lyY < screenH }
                    if (up) { c.consume(); lyY = (lyY + dy).coerceIn(0f, screenH) }
                },
                onDragEnd = { if (up) lyTo(if (lyY < screenH * 0.88f) 0f else screenH) },
                onDragCancel = { if (up) lyTo(if (lyY < screenH * 0.88f) 0f else screenH) },
            )
        }) {
            // art: edge to edge, swipe down anywhere on it to go back
            Box(
                Modifier.fillMaxWidth().weight(1f).onGloballyPositioned { if (drag <= 0f) { morphArt = it.boundsInWindow(); com.ubayanda.milonative.ui.player.MorphTargets.fullArt = morphArt } }.pointerInput("double-tap") {
                    detectTapGestures(onDoubleTap = { off ->
                        if (!com.ubayanda.milonative.ui.player.PlayerGestures.doubleTap(npCtx)) return@detectTapGestures
                        val cur = uiNow
                        val zone = if (off.x < size.width / 3f) -1 else if (off.x > size.width * 2f / 3f) 1 else 0
                        val prev = tapFlash
                        // taps in a row on the same side add up (+10 s, +20 s ...) like video players
                        val n = if (zone != 0 && prev != null && prev.zone == zone) prev.count + 1 else 1
                        when (zone) {
                            0 -> if (com.ubayanda.milonative.ui.player.PlayerGestures.centreAction(npCtx) == 0) vm.player.toggle()
                            else -> vm.player.seekTo((cur.positionMs + zone * com.ubayanda.milonative.ui.player.PlayerGestures.seekSeconds(npCtx) * 1_000L).coerceIn(0L, if (cur.durationMs > 0) cur.durationMs else Long.MAX_VALUE))
                        }
                        tapFlash = TapFlash(zone, if (zone == 0) 1 else n, System.nanoTime(), playing = zone == 0 && !cur.isPlaying)
                    })
                }.pointerInput("swipe-skip") {
                    // swipe the art sideways: left = next song, right = previous
                    var hx = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { hx = 0f },
                        onHorizontalDrag = { _, d -> hx += d },
                        onDragEnd = {
                            val need = com.ubayanda.milonative.ui.player.PlayerGestures.swipeDistance(npCtx)
                            if (com.ubayanda.milonative.ui.player.PlayerGestures.swipeSkip(npCtx)) { if (hx < -need) vm.player.next() else if (hx > need) vm.player.previous() }
                            hx = 0f
                        },
                        onDragCancel = { hx = 0f },
                    )
                }.pointerInput(Unit) {
                    // down = fold the player away; up = the lyrics page follows your finger and springs open
                    var up = false
                    var decided = false
                    detectVerticalDragGestures(
                        onDragStart = { lyJob?.cancel(); up = false; decided = false },
                        onVerticalDrag = { c, dy ->
                            if (!decided && dy != 0f) { decided = true; up = dy < 0f && drag <= 0f }
                            if (up) { c.consume(); lyY = (lyY + dy).coerceIn(0f, screenH) }
                            else if (!shut) closeY = (closeY + dy).coerceAtLeast(0f)
                        },
                        onDragEnd = { if (up) lyTo(if (lyY < screenH * 0.88f) 0f else screenH) else if (closeY > screenH * 0.16f) dismissPlayer() else springBackPlayer() },
                        onDragCancel = { if (up) lyTo(if (lyY < screenH * 0.88f) 0f else screenH) else springBackPlayer() },
                    )
                }
            ) {
                val coverLook by com.ubayanda.milonative.ui.player.rememberCoverLook().collectAsState()
                // next / previous: the new cover glides in from the side it came from while the old one slips away
                androidx.compose.animation.AnimatedContent(
                    targetState = ArtKey(t.id, cover, coverDone),
                    contentKey = { it.id },
                    modifier = Modifier.fillMaxSize()
                        .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            drawRect(Brush.verticalGradient(0f to Color.Black, 0.6f to Color.Black, 1f to Color.Transparent), blendMode = androidx.compose.ui.graphics.BlendMode.DstIn)
                        },
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
                            if (b != null && Y2K.on) Box(Modifier.fillMaxSize()) {
                                // Y2K: the cover is a spinning CD on a darkened copy of itself
                                Image(b.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = 0.55f)
                                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Y2K.navy.copy(alpha = 0.55f), Y2K.navy.copy(alpha = 0.85f)))))
                                SpinningCd(b.asImageBitmap(), uiNow.isPlaying, Modifier.align(Alignment.Center).fillMaxWidth(0.72f))
                            }
                            else if (b != null && coverLook.style != com.ubayanda.milonative.ui.player.PlayerCoverStyle.EDGE_TO_EDGE) com.ubayanda.milonative.ui.player.CoverStage(b, coverLook, uiNow.isPlaying, positionProvider = { uiNow.positionMs }, durationProvider = { uiNow.durationMs }, trackKey = t.id, onSeek = { vm.player.seekTo(it) })
                            else if (b != null) Image(b.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, filterQuality = androidx.compose.ui.graphics.FilterQuality.High,
                                colorFilter = if (Sticker.on || ChromeStyle.on) androidx.compose.ui.graphics.ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(0f) }) else null)
                            else if (k.done) Icon(MiloIcons.MusicNote, null, tint = Milo.dim, modifier = Modifier.align(Alignment.Center).size(96.dp))
                        }
                        // an animated cover the person chose for this song plays silently over the art
                        val canvasCtx = androidx.compose.ui.platform.LocalContext.current
                        val chosenCanvas = com.ubayanda.milonative.ui.components.CanvasStore.get(canvasCtx, k.id)
                        // else, when the automatic mode is on, the official music video of a YouTube song plays instead
                        val autoCanvas = if (chosenCanvas == null && k.id == t.id) com.ubayanda.milonative.ui.components.AutoCanvas.urlFor(canvasCtx, t.uri) else null
                        (chosenCanvas ?: autoCanvas)?.let { cu ->
                            com.ubayanda.milonative.ui.components.CanvasLoop(cu, uiNow.isPlaying, Modifier.fillMaxSize())
                        }
                    }
                }
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.35f), 0.3f to Color.Transparent, 0.6f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.85f))))
                if (ChromeStyle.on) {
                    // Chrome: the art goes silver and dark, with a floating chrome planet and pulsing sparkles
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.25f), 1f to Color.Black.copy(alpha = 0.45f))))
                    ChromeFloat(uiNow.isPlaying, Modifier.align(Alignment.TopStart).padding(start = 8.dp, top = 52.dp).size(width = 170.dp, height = 120.dp))
                }
                if (Nebula.on) {
                    // Nebula: the art is washed in violet and magenta, with the astronaut floating in the corner
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Nebula.violet.copy(alpha = 0.38f), Nebula.magenta.copy(alpha = 0.30f)))))
                    HelmetSticker(uiNow.isPlaying, Modifier.align(Alignment.TopStart).padding(start = 10.dp, top = 52.dp).size(112.dp))
                }
                if (Sticker.on) {
                    // Sticker: black-and-white print, a heavy white frame, sparkles and the doodle buddy
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.55f))))
                    StickerScatter(alpha = 0.7f)
                    BuddySticker(uiNow.isPlaying, Modifier.align(Alignment.TopStart).padding(start = 14.dp, top = 56.dp).size(104.dp))
                }
                if (Spider.on) {
                    // Spider: the art sinks into a black and red vignette, with a web across it and a spider on a thread
                    Box(Modifier.fillMaxSize().background(Brush.radialGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.78f))))
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.6f to Color.Transparent, 1f to Spider.red.copy(alpha = 0.22f))))
                    WebCorners(alpha = 0.34f)
                    HangingSpider(uiNow.isPlaying, Modifier.align(Alignment.TopEnd).padding(end = 76.dp).width(60.dp).height(150.dp))
                }

                tapFlash?.let { f ->
                    Box(
                        Modifier.align(if (f.zone < 0) Alignment.CenterStart else if (f.zone > 0) Alignment.CenterEnd else Alignment.Center).padding(horizontal = 36.dp)
                            .size(92.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                if (f.zone < 0) MiloIcons.Replay10 else if (f.zone > 0) MiloIcons.Forward10 else if (f.playing) MiloIcons.PlayArrow else MiloIcons.Pause,
                                null, tint = Color.White, modifier = Modifier.size(40.dp),
                            )
                            if (f.zone != 0 && f.count > 1) Text("${f.count * 10} s", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                RoundBtn(MiloIcons.KeyboardArrowDown, "Back", Modifier.align(Alignment.TopStart).statusBarsPadding().padding(12.dp), onClick = { foldAway() })
                val outTop by com.ubayanda.milonative.player.BtOutput.now.collectAsState()
                RoundBtn(
                    if (outTop.kind != "speaker" && outTop.bt) MiloIcons.BluetoothAudio else if (outTop.kind != "speaker") MiloIcons.Headphones else MiloIcons.DockSpeaker,
                    "Output", Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(12.dp), onClick = { castOpen = true },
                )


                androidx.compose.animation.AnimatedContent(
                    targetState = t, contentKey = { it.id },
                    modifier = Modifier.align(Alignment.BottomStart),
                    transitionSpec = {
                        (androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(360, delayMillis = 90)) + androidx.compose.animation.slideInVertically(androidx.compose.animation.core.tween(380, delayMillis = 90)) { it / 6 })
                            .togetherWith(androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(160)))
                    },
                    label = "titles",
                ) { tt ->
                    Column(Modifier.padding(start = Sp.gutter, end = Sp.gutter, bottom = Sp.x6).onGloballyPositioned { if (drag <= 0f) morphTitle = it.boundsInWindow() }) {
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
                            Icon(if (outNow.bt) MiloIcons.BluetoothAudio else MiloIcons.Headphones, null, tint = Milo.amber, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(outNow.name, color = Milo.amber, style = Ty.caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            // the button strip: like, save, queue, sound, timer, visual, more. It uses the same frosted look as the controls card, so it follows the app's background.
            Row(
                Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 2.dp).padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // the heart pops when a song is liked
                val heartPop = remember { androidx.compose.animation.core.Animatable(1f) }
                LaunchedEffect(liked) { if (liked) { heartPop.snapTo(0.55f); heartPop.animateTo(1f, androidx.compose.animation.core.spring(0.32f, 420f)) } else heartPop.snapTo(1f) }
                StripBtn(if (liked) MiloIcons.Favorite else MiloIcons.FavoriteBorder, "Like", liked, Modifier.weight(1f).likeButtonTarget(likeBurst, LikeSlot.PLAYER).graphicsLayer { scaleX = heartPop.value; scaleY = heartPop.value }) { likeBurst.likeFromButton(LikeSlot.PLAYER, liked) { vm.toggleFav(t.id) } }
                if (t.uri.startsWith("yt:")) {
                    val savedNow = com.ubayanda.milonative.data.OfflineStore.items.collectAsState().value.any { it.id == t.uri.removePrefix("yt:") }
                    StripBtn(if (savedNow) MiloIcons.DownloadDone else MiloIcons.DockSave, if (savedNow) "Saved" else "Save", savedNow, Modifier.weight(1f)) { vm.downloadTracks(listOf(t)) }
                }
                StripBtn(MiloIcons.DockQueue, "Queue", false, Modifier.weight(1f)) { panel = Panel.Queue }
                StripBtn(MiloIcons.DockSound, "Sound", false, Modifier.weight(1f)) { panel = Panel.Sound }
                StripBtn(MiloIcons.DockTimer, "Sleep timer", ui.sleepLeftMs > 0, Modifier.weight(1f)) { panel = Panel.Timer }
                if (!com.ubayanda.milonative.BuildConfig.LITE) StripBtn(MiloIcons.GraphicEq, "Visualiser", false, Modifier.weight(1f)) { panel = Panel.Visual }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { MoreMenu(vm, t, ui) }
            }

            // control card
            val glowBase = vm.tint.collectAsState().value.let { if (it == 0) Milo.amber else Color(0xFF000000.toInt() or it) }
            val glowA by androidx.compose.animation.animateColorAsState(glowBase, androidx.compose.animation.core.tween(700), label = "glowA")
            val glowB by androidx.compose.animation.animateColorAsState(androidx.compose.ui.graphics.lerp(glowBase, Color.White, 0.22f), androidx.compose.animation.core.tween(700), label = "glowB")
            Column(
                Modifier.fillMaxWidth()
                    .pointerInput("lyrics-swipe") {
                        // swipe up on the card: the lyrics page follows your finger, then springs open (or back) when you let go
                        detectVerticalDragGestures(
                            onDragStart = { lyJob?.cancel() },
                            onVerticalDrag = { c, dy -> c.consume(); lyY = (lyY + dy).coerceIn(0f, screenH) },
                            onDragEnd = { lyTo(if (lyY < screenH * 0.88f) 0f else screenH) },
                            onDragCancel = { lyTo(if (lyY < screenH * 0.88f) 0f else screenH) },
                        )
                    }
                    .padding(top = Sp.x2, bottom = 0.dp),
            ) {
                if (Y2K.on) LcdStrip(t.title, t.artist, "${fmtTime(ui.positionMs)} / ${fmtTime(ui.durationMs)}", ui.quality, ui.isPlaying, Modifier.padding(horizontal = Sp.gutter).padding(bottom = Sp.x3))
                Scrubber(ui, Modifier.padding(horizontal = Sp.gutter), vm.tint.collectAsState().value) { vm.player.seekTo(it) }
                Row(Modifier.fillMaxWidth().padding(horizontal = Sp.x6), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(fmtTime(ui.positionMs), color = Milo.dim, style = Ty.caption)
                    Text(fmtTime(ui.durationMs), color = Milo.dim, style = Ty.caption)
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = Sp.x4, vertical = Sp.x1),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconTap(MiloIcons.Shuffle, "Shuffle", 28.dp, if (ui.shuffle) Milo.amber else Milo.text) { vm.player.toggleShuffle() }
                    RoundDark(MiloIcons.SkipPrevious, "Previous") { vm.player.previous() }
                    Box(
                        Modifier.size(72.dp).onGloballyPositioned { if (drag <= 0f) morphPlay = it.boundsInWindow() }.clip(CircleShape)
                            .background(if (Y2K.on) Brush.verticalGradient(listOf(Y2K.chromeLight, Y2K.chromeDark)) else androidx.compose.ui.graphics.SolidColor(Milo.cream))
                            .pressable { vm.player.toggle() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(if (ui.isPlaying) MiloIcons.Pause else MiloIcons.PlayArrow, "Play or pause", tint = Color.Black, modifier = Modifier.size(38.dp))
                    }
                    RoundDark(MiloIcons.SkipNext, "Next", Modifier.onGloballyPositioned { if (drag <= 0f) morphNext = it.boundsInWindow() }) { vm.player.next() }
                    IconTap(
                        if (ui.repeat == 1) MiloIcons.RepeatOne else MiloIcons.Repeat, "Repeat", 28.dp,
                        if (ui.repeat != 0) Milo.amber else Milo.text,
                    ) { vm.player.cycleRepeat() }
                }
            // the bottom black area: the lyrics, live. Tap it or drag up for the full page.
            Column(
                Modifier.fillMaxWidth().heightIn(min = 88.dp)
                    .background(Color.Black, RoundedCornerShape(topStart = Rad.xl, topEnd = Rad.xl))
                    .navigationBarsPadding().padding(top = 4.dp, bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    Modifier.fillMaxWidth().clickable { lyTo(0f) }.padding(top = 4.dp, bottom = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.width(56.dp).height(4.dp).clip(CircleShape).background(Milo.text.copy(alpha = 0.3f)))
                    Spacer(Modifier.height(4.dp))
                    val tickerFx = remember(t.id) { npCtx.getSharedPreferences("milo_fx", android.content.Context.MODE_PRIVATE) }
                    val tickerOn = tickerFx.getBoolean("lyticker", false) && (lyrics?.synced?.isNotEmpty() == true || lyricsLoading)
                    if (tickerOn) {
                        // Mochy's lyric ticker (opt-in, Sound > Lyrics ticker): the sung line with the karaoke sweep
                        val tickerLines = remember(lyrics) {
                            (lyrics?.synced ?: emptyList()).mapIndexed { n, tl ->
                                val nextLine = lyrics!!.synced.getOrNull(n + 1)?.timeMs ?: (tl.timeMs + 4000)
                                com.ubayanda.milonative.ui.lyrics.LyricsEntry(
                                    time = tl.timeMs, text = tl.text,
                                    words = tl.words.takeIf { it.isNotEmpty() }?.mapIndexed { k, w ->
                                        val end = tl.words.getOrNull(k + 1)?.timeMs ?: nextLine
                                        com.ubayanda.milonative.ui.lyrics.WordTimestamp(w.text, w.timeMs / 1000.0, end / 1000.0)
                                    },
                                )
                            }
                        }
                        val tickerOff = remember(t.id) { tickerFx.getLong("lyoff_" + t.id, 0L) }
                        com.ubayanda.milonative.ui.lyrics.MiniLyricsTicker(
                            lines = tickerLines, loaded = !lyricsLoading,
                            positionProvider = { uiNow.positionMs - tickerOff },
                            modifier = Modifier.padding(horizontal = Sp.x6),
                        )
                    } else LiveLyrics(lyrics, lyricsLoading, ui.positionMs)
                }
            }
            }
        }

        // the heart that flies from the Like button, thumps and dives back in (Mochy's like burst)
        LikeBurstOverlay(likeBurst)

        when (panel) {
            Panel.Queue -> Sheet(0.72f, soft, { panel = Panel.None }) { QueueContent(vm, ui) }
            Panel.Sound -> Sheet(0.8f, soft, { panel = Panel.None }) { SoundContent(vm, ui) }
            Panel.Lyrics -> {}
            Panel.Visual -> VisualPage(vm, t, ui, soft) { panel = Panel.None }
            Panel.Timer -> Sheet(0.52f, soft, { panel = Panel.None }) { TimerContent(vm, ui) }
            Panel.None -> {}
        }

        if (lyOpen) {
            Box(Modifier.fillMaxSize().offset { IntOffset(0, lyY.roundToInt()) }) {
                // fills the gap under the page for the moment it overshoots the top on its bounce
                Box(Modifier.fillMaxWidth().height(160.dp).align(Alignment.BottomCenter).offset(y = 160.dp).background(Milo.bg))
                Box(Modifier.fillMaxSize().graphicsLayer {
                    val r = 36.dp.toPx() * (lyY / screenH * 8f).coerceIn(0f, 1f)
                    shape = RoundedCornerShape(topStart = androidx.compose.foundation.shape.CornerSize(r), topEnd = androidx.compose.foundation.shape.CornerSize(r), bottomEnd = androidx.compose.foundation.shape.CornerSize(0f), bottomStart = androidx.compose.foundation.shape.CornerSize(0f))
                    clip = true
                }) {
                    Box(Modifier.fillMaxSize().pointerInput("lyrics-swipe-down") {
                        // pull down anywhere on the page to close it; it follows the finger and springs back if you let go early
                        var down = false
                        var decided = false
                        detectVerticalDragGestures(
                            onDragStart = { lyJob?.cancel(); down = false; decided = false },
                            onVerticalDrag = { c, dy ->
                                if (!decided && dy != 0f) { decided = true; down = dy > 0f }
                                if (down) { c.consume(); lyY = (lyY + dy).coerceIn(0f, screenH) }
                            },
                            onDragEnd = { if (down) lyTo(if (lyY > screenH * 0.16f) screenH else 0f) },
                            onDragCancel = { if (down) lyTo(0f) },
                        )
                    }) {
                        LyricsPage(vm, t, ui, soft, lyrics, lyricsLoading, { lyY }, { p -> lyTo(screenH); panel = p }) { lyTo(screenH) }
                    }
                    // grab this and pull down to close; it springs back if you let go early
                    Box(
                        Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 6.dp).width(140.dp).height(36.dp).pointerInput("lyrics-close") {
                            detectVerticalDragGestures(
                                onDragStart = { lyJob?.cancel() },
                                onVerticalDrag = { c, dy -> c.consume(); lyY = (lyY + dy).coerceIn(0f, screenH) },
                                onDragEnd = { lyTo(if (lyY > screenH * 0.16f) screenH else 0f) },
                                onDragCancel = { lyTo(0f) },
                            )
                        },
                        contentAlignment = Alignment.Center,
                    ) { Box(Modifier.width(44.dp).height(5.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.35f))) }
                }
            }
        }
    }
}

@Composable
private fun Sheet(frac: Float, backdrop: Bitmap?, onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val full = with(androidx.compose.ui.platform.LocalDensity.current) { androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp.toPx() } * frac
    var y by remember { mutableFloatStateOf(full) }   // starts below the screen and springs up
    var closing by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { androidx.compose.animation.core.Animatable(full).animateTo(0f, com.ubayanda.milonative.ui.components.Bounce.px()) { if (!closing) y = value } }
    fun springBack() { val from = y; scope.launch { androidx.compose.animation.core.Animatable(from).animateTo(0f, com.ubayanda.milonative.ui.components.Bounce.px()) { y = value } } }
    fun close() {
        if (closing) return
        closing = true
        val from = y
        scope.launch { androidx.compose.animation.core.Animatable(from).animateTo(full, com.ubayanda.milonative.ui.components.Bounce.px()) { y = value; if (value >= full) onClose() } }
    }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f * (1f - (y / full).coerceIn(0f, 1f)))).clickable(onClick = ::close)) {
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(frac)
                .offset { IntOffset(0, y.roundToInt().coerceAtLeast(0)) }
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)).frosted(backdrop, 0.6f)
                .border(1.dp, glassStroke(1.2f), RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .clickable(enabled = false) {}.navigationBarsPadding(),
        ) {
            // the handle area: drag it down to dismiss; let go early and the sheet springs back
            Box(
                Modifier.fillMaxWidth().height(36.dp).pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { _, dy -> if (!closing) y = (y + dy).coerceAtLeast(0f) },
                        onDragEnd = { if (y > 140f) close() else springBack() },
                        onDragCancel = { springBack() },
                    )
                },
                contentAlignment = Alignment.TopCenter,
            ) { Box(Modifier.padding(top = 10.dp).width(40.dp).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f))) }
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
            { f, t -> val l = local.toMutableList(); if (f in l.indices) { val it = l.removeAt(f); l.add(t.coerceIn(0, l.size), it); local = l } },
            { f, t -> vm.player.moveInQueue(f, t) },
        )
    }
    // the queue changed under a finger (a song ended, autoplay added tracks, it was cleared): drop the half-finished drag instead of moving stale indexes
    LaunchedEffect(ui.queueIds) { reorder.reset() }
    // the first moments of the page: rows rise in as a cascade (after Mochy's page transition)
    var fresh by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(900); fresh = false }
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
                else Modifier.animateItem().cascadeIn(i, fresh)
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

        Label("Volume")
        run {
            val vctx = androidx.compose.ui.platform.LocalContext.current
            val am = remember { vctx.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager }
            val maxV = remember { am.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
            var vol by remember { mutableFloatStateOf(am.getStreamVolume(android.media.AudioManager.STREAM_MUSIC).toFloat()) }
            // the hardware buttons change it too, so look again now and then while the panel is open
            LaunchedEffect(Unit) { while (true) { vol = am.getStreamVolume(android.media.AudioManager.STREAM_MUSIC).toFloat(); kotlinx.coroutines.delay(500) } }
            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("\uD83D\uDD0A", fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                MiloSlider(vol, { vol = it.roundToInt().toFloat(); am.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, vol.toInt(), 0) },
                    Modifier.weight(1f), valueRange = 0f..maxV.toFloat(), colors = amberSlider())
            }
        }

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
        if (xf >= 1f) {
            val xfp = androidx.compose.ui.platform.LocalContext.current.getSharedPreferences("milo_fx", 0)
            var gapAlbums by remember { mutableStateOf(xfp.getBoolean("gapless_albums", true)) }
            Row(Modifier.padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Keep albums gapless: songs from one album play straight through, no fade (Mochy)", color = Milo.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
                com.ubayanda.milonative.ui.components.MiloSwitch(
                    checked = gapAlbums, onCheckedChange = { gapAlbums = it; xfp.edit().putBoolean("gapless_albums", it).apply() },
                    colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = Milo.amber),
                )
            }
            var curve by remember { mutableStateOf(xfp.getInt("xfcurve", 0)) }
            LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(com.ubayanda.milonative.player.XFade.names.size) { i ->
                    MiloChip(com.ubayanda.milonative.player.XFade.names[i], curve == i) { curve = i; xfp.edit().putInt("xfcurve", i).apply() }
                }
            }
        }

        Label("Soft pause & skip")
        val tfp = androidx.compose.ui.platform.LocalContext.current.getSharedPreferences("milo_fx", 0)
        var tf by remember { mutableStateOf(tfp.getInt("tfade", 300).toFloat()) }
        Text(
            if (tf < 50f) "Off. Pause, play and skip act at once." else "${(tf / 100f).roundToInt() / 10f} s: pausing, resuming and skipping ease the volume instead of cutting. Works from the lock screen and notification too.",
            color = Milo.dim, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp),
        )
        MiloSlider(
            value = tf, onValueChange = { tf = (it / 100f).roundToInt() * 100f; tfp.edit().putInt("tfade", tf.toInt()).apply() }, valueRange = 0f..3000f, steps = 29,
            colors = amberSlider(), modifier = Modifier.padding(horizontal = 20.dp),
        )
        if (tf >= 50f) {
            var tfx by remember { mutableStateOf(tfp.getInt("tfx", 0)) }
            Text("Effect on pause, play and skip", color = Milo.dim, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
            Row(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                com.ubayanda.milonative.crossmix.TransportEffect.values().forEachIndexed { i, e ->
                    MiloChip(e.label, tfx == i) { tfx = i; tfp.edit().putInt("tfx", i).apply() }
                }
            }
        }
        var blend by remember { mutableStateOf(tfp.getBoolean("blend_skips", false)) }
        Row(Modifier.padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Blend skips: the song you skip to starts at once and crosses over (length = the slider above, 0.6 to 3 s)", color = Milo.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
            com.ubayanda.milonative.ui.components.MiloSwitch(
                checked = blend, onCheckedChange = { blend = it; tfp.edit().putBoolean("blend_skips", it).apply() },
                colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = Milo.amber),
            )
        }

        Label("Loudness matching")
        val fxp = androidx.compose.ui.platform.LocalContext.current.getSharedPreferences("milo_fx", 0)
        var rg by remember { mutableStateOf(fxp.getBoolean("rg", true)) }
        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Turns louder songs down to match quieter ones, using ReplayGain tags in your files and YouTube's own loudness. It never boosts, so nothing distorts. Applies from the next song.", color = Milo.dim, fontSize = 13.sp, modifier = Modifier.weight(1f))
            com.ubayanda.milonative.ui.components.MiloSwitch(checked = rg, onCheckedChange = { rg = it; fxp.edit().putBoolean("rg", it).apply() },
                colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = Milo.amber))
        }

        Label("Song analysis (experimental)")
        var xan by remember { mutableStateOf(fxp.getBoolean("xanalyse", false)) }
        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Reads each song's tempo, beats and key while it plays and writes what it found to Diagnostics. It is the first step toward beat-matched mixing and changes nothing you hear. A YouTube song that isn't saved is streamed once to be read, which uses data.", color = Milo.dim, fontSize = 13.sp, modifier = Modifier.weight(1f))
            com.ubayanda.milonative.ui.components.MiloSwitch(checked = xan, onCheckedChange = { xan = it; fxp.edit().putBoolean("xanalyse", it).apply() },
                colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = Milo.amber))
        }

        ExtrasSection(ui)

        Label("Equaliser")
        com.ubayanda.milonative.ui.components.GraphicEqPanel()

        var fine by remember { mutableStateOf(false) }
        Label("Fine tuning")
        MiloChip(if (fine) "Hide parametric EQ" else "Show parametric EQ", fine, Modifier.padding(horizontal = 20.dp)) { fine = !fine }
        if (fine) ParametricSection()

        CoverStyleSection()

        LyricTickerToggle()

        LoudnessSection()

        CrossmixSection()

        ReverbSection()

        HapticSection()

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
private fun CrossmixSection() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val fx = remember { ctx.getSharedPreferences("milo_fx", android.content.Context.MODE_PRIVATE) }
    var engine by remember { mutableStateOf(fx.getString(com.ubayanda.milonative.player.CrossmixLive.ENGINE, "classic") ?: "classic") }
    var len by remember { mutableStateOf(fx.getInt(com.ubayanda.milonative.player.CrossmixLive.LENGTH, 1)) }
    var tempo by remember { mutableStateOf(fx.getInt(com.ubayanda.milonative.player.CrossmixLive.TEMPO, 2)) }
    var style by remember { mutableStateOf(fx.getInt(com.ubayanda.milonative.player.CrossmixLive.STYLE, -1)) }
    var stunts by remember { mutableStateOf(fx.getBoolean(com.ubayanda.milonative.player.CrossmixLive.STUNTS, true)) }
    var always by remember { mutableStateOf(fx.getBoolean(com.ubayanda.milonative.player.CrossmixLive.ALWAYS, true)) }
    val status by com.ubayanda.milonative.player.CrossmixLive.status.collectAsState()
    val mixing by com.ubayanda.milonative.player.CrossmixLive.mixing.collectAsState()
    val sw = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = Milo.amber)
    fun putI(k: String, v: Int) = fx.edit().putInt(k, v).apply()

    Label("Transitions")
    Row(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()).padding(horizontal = 20.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MiloChip("Classic", engine == "classic") { engine = "classic"; fx.edit().putString(com.ubayanda.milonative.player.CrossmixLive.ENGINE, engine).apply() }
        MiloChip("Crossmix", engine == "crossmix") { engine = "crossmix"; fx.edit().putString(com.ubayanda.milonative.player.CrossmixLive.ENGINE, engine).apply() }
    }
    if (engine != "crossmix") {
        Text("Classic is milo's own crossfade (the Crossfade slider). Crossmix is Mochy Music's engine: it reads both songs' tempo, beats, key and where the vocals are, then blends them on the beat in a style that fits the pair.", color = Milo.dim, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp))
        return
    }
    Text((if (mixing) "Mixing now: " else "") + status, color = Milo.dim, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp))
    Text("Length", color = Milo.dim, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
    Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Short", "Medium", "Long").forEachIndexed { i, n -> MiloChip(n, len == i) { len = i; putI(com.ubayanda.milonative.player.CrossmixLive.LENGTH, i) } }
    }
    Text("Tempo match", color = Milo.dim, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
    Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Off", "Gentle", "Full").forEachIndexed { i, n -> MiloChip(n, tempo == i) { tempo = i; putI(com.ubayanda.milonative.player.CrossmixLive.TEMPO, i) } }
    }
    Text("Style", color = Milo.dim, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
    Row(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MiloChip("Fits the pair", style == -1) { style = -1; putI(com.ubayanda.milonative.player.CrossmixLive.STYLE, -1) }
        com.ubayanda.milonative.crossmix.CrossmixStyle.values().forEachIndexed { i, st -> MiloChip(st.label, style == i) { style = i; putI(com.ubayanda.milonative.player.CrossmixLive.STYLE, i) } }
    }
    Row(Modifier.padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Backspins, tape stops and risers when the energy jumps", color = Milo.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
        com.ubayanda.milonative.ui.components.MiloSwitch(checked = stunts, onCheckedChange = { stunts = it; fx.edit().putBoolean(com.ubayanda.milonative.player.CrossmixLive.STUNTS, it).apply() }, colors = sw)
    }
    Row(Modifier.padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Always blend the two songs, never a plain fade", color = Milo.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
        com.ubayanda.milonative.ui.components.MiloSwitch(checked = always, onCheckedChange = { always = it; fx.edit().putBoolean(com.ubayanda.milonative.player.CrossmixLive.ALWAYS, it).apply() }, colors = sw)
    }
    Row(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
        MiloChip("Try a mix now", false) { com.ubayanda.milonative.player.CrossmixLive.test.tryEmit(Unit) }
    }
}

@Composable
private fun CoverStyleSection() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val look by com.ubayanda.milonative.ui.player.rememberCoverLook().collectAsState()
    val sw = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = Milo.amber)
    Label("Backdrop")
    run {
        val fx = remember { ctx.getSharedPreferences("milo_fx", android.content.Context.MODE_PRIVATE) }
        var warp by remember { mutableStateOf(fx.getBoolean("pbg_warp", false)) }
        Row(Modifier.padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Liquid backdrop: the cover's colours drift slowly (Mochy). Applies from the next song.", color = Milo.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
            com.ubayanda.milonative.ui.components.MiloSwitch(checked = warp, onCheckedChange = { warp = it; fx.edit().putBoolean("pbg_warp", it).apply() }, colors = sw)
        }
    }
    Label("Cover style")
    Row(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()).padding(horizontal = 20.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        com.ubayanda.milonative.ui.player.PlayerCoverStyle.values().forEach { st ->
            MiloChip(st.label, look.style == st) { com.ubayanda.milonative.ui.player.CoverLook.set(ctx, look.copy(style = st)) }
        }
    }
    if (look.style.isDisc) {
        Row(Modifier.padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Spin while playing", color = Milo.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
            com.ubayanda.milonative.ui.components.MiloSwitch(checked = look.rotate, onCheckedChange = { com.ubayanda.milonative.ui.player.CoverLook.set(ctx, look.copy(rotate = it)) }, colors = sw)
        }
        Row(Modifier.padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Realistic vinyl (grooves and shine)", color = Milo.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
            com.ubayanda.milonative.ui.components.MiloSwitch(checked = look.realistic, onCheckedChange = { com.ubayanda.milonative.ui.player.CoverLook.set(ctx, look.copy(realistic = it)) }, colors = sw)
        }
        Text("Text printed round the label (leave empty for none)", color = Milo.dim, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        androidx.compose.foundation.text.BasicTextField(
            value = look.name,
            onValueChange = { com.ubayanda.milonative.ui.player.CoverLook.set(ctx, look.copy(name = it.take(24))) },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(color = Milo.text, fontSize = 15.sp),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(Milo.amber),
            modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.08f)).padding(horizontal = 14.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun LyricTickerToggle() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val fx = remember { ctx.getSharedPreferences("milo_fx", android.content.Context.MODE_PRIVATE) }
    var on by remember { mutableStateOf(fx.getBoolean("lyticker", false)) }
    val sw = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = Milo.amber)
    Label("Lyrics ticker")
    Row(Modifier.padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Karaoke line under the controls (Mochy style)", color = Milo.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
        com.ubayanda.milonative.ui.components.MiloSwitch(checked = on, onCheckedChange = { on = it; fx.edit().putBoolean("lyticker", it).apply() }, colors = sw)
    }
}

@Composable
private fun LoudnessSection() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val fx = remember { ctx.getSharedPreferences("milo_fx", android.content.Context.MODE_PRIVATE) }
    var on by remember { mutableStateOf(fx.getBoolean("loud_on", false)) }
    var boost by remember { mutableStateOf(fx.getInt("loud_boost", 4).toFloat()) }
    var comp by remember { mutableStateOf(fx.getInt("loud_comp", 30).toFloat()) }
    var rgMode by remember { mutableStateOf(fx.getInt("rg_mode", 0)) }
    var rgPre by remember { mutableStateOf(fx.getInt("rg_pre", 0).toFloat()) }
    val sw = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = Milo.amber)
    Label("Loudness")
    Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Louder without breaking up: lifts quiet parts, then a peak limiter", color = Milo.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
        com.ubayanda.milonative.ui.components.MiloSwitch(checked = on, onCheckedChange = { on = it; fx.edit().putBoolean("loud_on", it).apply() }, colors = sw)
    }
    if (on) {
        Text("Boost  +${boost.toInt()} dB", color = Milo.dim, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        androidx.compose.material3.Slider(boost, { boost = it }, valueRange = 0f..12f, steps = 11, onValueChangeFinished = { fx.edit().putInt("loud_boost", boost.toInt()).apply() }, modifier = Modifier.padding(horizontal = 12.dp))
        Text("Compression  ${comp.toInt()}%", color = Milo.dim, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        androidx.compose.material3.Slider(comp, { comp = it }, valueRange = 0f..100f, onValueChangeFinished = { fx.edit().putInt("loud_comp", comp.toInt()).apply() }, modifier = Modifier.padding(horizontal = 12.dp))
        Text("Gain reduction now: ${"%.1f".format(com.ubayanda.milonative.player.audio.LoudnessFx.reductionDb)} dB", color = Milo.dim, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp))
    }
    Text("ReplayGain (files that carry it)", color = Milo.dim, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
    Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Track", "Album").forEachIndexed { i, n -> MiloChip(n, rgMode == i) { rgMode = i; fx.edit().putInt("rg_mode", i).apply() } }
    }
    Text("Preamp  ${if (rgPre >= 0) "+" else ""}${rgPre.toInt()} dB (songs are only ever turned down)", color = Milo.dim, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
    androidx.compose.material3.Slider(rgPre, { rgPre = it }, valueRange = -6f..6f, steps = 11, onValueChangeFinished = { fx.edit().putInt("rg_pre", rgPre.toInt()).apply() }, modifier = Modifier.padding(horizontal = 12.dp))
}

@Composable
private fun HapticSection() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val fx = remember { ctx.getSharedPreferences("milo_fx", android.content.Context.MODE_PRIVATE) }
    var on by remember { mutableStateOf(fx.getBoolean("haptic_on", false)) }
    var sens by remember { mutableStateOf(fx.getInt("haptic_sens", 1)) }
    var focus by remember { mutableStateOf(fx.getInt("haptic_focus", 2)) }
    val sw = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = Milo.amber)
    Label("Haptics")
    Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Feel the music: the phone vibrates with the beat", color = Milo.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
        com.ubayanda.milonative.ui.components.MiloSwitch(checked = on, onCheckedChange = { on = it; fx.edit().putBoolean("haptic_on", it).apply() }, colors = sw)
    }
    if (on) {
        Text("Follows", color = Milo.dim, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        Row(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            com.ubayanda.milonative.player.audio.HapticFocus.values().forEachIndexed { i, f ->
                MiloChip(f.label, focus == i) { focus = i; fx.edit().putInt("haptic_focus", i).apply() }
            }
        }
        Text("Strength", color = Milo.dim, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            com.ubayanda.milonative.player.audio.HapticSensitivity.values().forEachIndexed { i, h ->
                MiloChip(h.label, sens == i) { sens = i; fx.edit().putInt("haptic_sens", i).apply() }
            }
        }
        Text("Works best with the phone in your hand or on a table; Bluetooth speakers add a short delay.", color = Milo.dim, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
    }
}

@Composable
private fun ReverbSection() {
    val rv by com.ubayanda.milonative.player.Reverb.state.collectAsState()
    val sw = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = Milo.amber)
    Label("Reverb")
    Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Put the music in a room", color = Milo.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
        com.ubayanda.milonative.ui.components.MiloSwitch(checked = rv.enabled, onCheckedChange = { com.ubayanda.milonative.player.Reverb.setEnabled(it) }, colors = sw)
    }
    if (rv.enabled) {
        Row(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()).padding(horizontal = 20.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            com.ubayanda.milonative.player.Reverb.presets.forEachIndexed { i, p ->
                MiloChip(p.name, rv.preset == i) { com.ubayanda.milonative.player.Reverb.setPreset(i) }
            }
        }
        Text("Amount", color = Milo.dim, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp))
        MiloSlider(value = rv.mix, onValueChange = { com.ubayanda.milonative.player.Reverb.setMix(it) }, valueRange = 0f..1f, colors = amberSlider(), modifier = Modifier.padding(horizontal = 20.dp))
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
private fun LyricsPage(vm: MainViewModel, t: Track, ui: PlayerUi, soft: Bitmap?, lyrics0: Lyrics?, loading: Boolean, pageOffset: () -> Float = { 0f }, onPanel: (Panel) -> Unit = {}, onBack: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val fx = remember { ctx.getSharedPreferences("milo_fx", 0) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var fresh by remember(t.id) { mutableStateOf<Lyrics?>(null) }
    var rescanning by remember(t.id) { mutableStateOf(false) }
    val lyrics = fresh ?: lyrics0
    var wordMode by remember { mutableStateOf(fx.getBoolean("lyword", false)) }
    var lyOff by remember(t.id) { mutableStateOf(fx.getLong("lyoff_" + t.id, 0L)) }
    // bouncy lyrics: the sung line pops and springs into place and the page eases after it. Off when the phone's animations are off.
    var bouncyPref by remember { mutableStateOf(fx.getBoolean("lybounce", true)) }
    val calm = remember { try { android.provider.Settings.Global.getFloat(ctx.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f } catch (swallowed: Throwable) { com.ubayanda.milonative.util.ErrorLog.swallowed(swallowed); false } }
    val bouncy = bouncyPref && !calm
    val dens = androidx.compose.ui.platform.LocalDensity.current.density
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
    // Mochy Music's lyric line (karaoke sweep, letter motion, glow, blur of the lines not sung); off = milo's own lines
    var glow by remember { mutableStateOf(fx.getBoolean("lyglow", false)) }
    var glowBlur by remember { mutableStateOf(fx.getBoolean("lyglowblur", true)) }
    // readings: Japanese, Chinese, Korean, Cyrillic, Hindi and Punjabi lines written out in Latin letters under the line (Mochy Music's romaniser)
    var roman by remember { mutableStateOf(fx.getBoolean("lyroman", true)) }
    val rm by androidx.compose.runtime.produceState<List<String?>?>(null, lyrics, roman) {
        value = if (roman && srcLines.isNotEmpty()) com.ubayanda.milonative.data.Romanizer.readAll(srcLines) else null
    }
    val coverBmp by com.ubayanda.milonative.ui.components.rememberCover(vm, t, 400)
    val tintNow = vm.tint.collectAsState().value
    fun card(lines: List<String>) = com.ubayanda.milonative.data.ShareCard.share(ctx, t.title, t.artist, lines, coverBmp, tintNow)
    val posterW = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp
    val posterAccent = if (tintNow == 0) Milo.amber else Color(0xFF000000.toInt() or tintNow)
    Box(Modifier.fillMaxSize().background(Color.Black).background(Brush.verticalGradient(listOf(posterAccent.copy(alpha = 0.55f), Color(0xFF080809), Color.Black))).clickable(enabled = false) {}) {
        // Poster: the cover as a great circle swung in from the top-left
        // the cover fills the top edge to edge and melts into the black at the bottom
        com.ubayanda.milonative.ui.components.Cover(vm, t, posterW, 0.dp, px = 900, modifier = Modifier
            .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(Brush.verticalGradient(0f to Color.Black, 0.55f to Color.Black, 1f to Color.Transparent), blendMode = androidx.compose.ui.graphics.BlendMode.DstIn)
            })
        Box(Modifier.fillMaxWidth().height(posterW * 0.3f).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent))))
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
                    ) { Icon(MiloIcons.IosShare, "Share lyrics", tint = Milo.text, modifier = Modifier.size(26.dp)) }
                    Spacer(Modifier.width(10.dp))
                }
                Box {
                    Box(Modifier.size(48.dp).clip(CircleShape).clickable { moreMenu = true }, contentAlignment = Alignment.Center) {
                        Icon(MiloIcons.MoreHoriz, "Lyrics options", tint = Milo.text, modifier = Modifier.size(26.dp))
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
                        DropdownMenuItem(text = { Text(if (bouncyPref) "Bouncy lyrics: on" else "Bouncy lyrics: off") }, onClick = {
                            bouncyPref = !bouncyPref; fx.edit().putBoolean("lybounce", bouncyPref).apply(); moreMenu = false
                        })
                        val flOn = com.ubayanda.milonative.ui.FloatingLyrics.enabled(ctx) && com.ubayanda.milonative.ui.FloatingLyrics.canDraw(ctx)
                        DropdownMenuItem(text = { Text(if (flOn) "Floating lyrics: on" else "Floating lyrics: off") }, onClick = {
                            moreMenu = false
                            if (flOn) com.ubayanda.milonative.ui.FloatingLyrics.set(ctx, false)
                            else if (com.ubayanda.milonative.ui.FloatingLyrics.canDraw(ctx)) com.ubayanda.milonative.ui.FloatingLyrics.set(ctx, true)
                            else {
                                // the person grants "Display over other apps" on the system page, then comes back
                                com.ubayanda.milonative.ui.FloatingLyrics.set(ctx, true)
                                android.widget.Toast.makeText(ctx, "Allow milo to display over other apps, then come back.", android.widget.Toast.LENGTH_LONG).show()
                                com.ubayanda.milonative.ui.FloatingLyrics.askPermission(ctx)
                            }
                        })
                        DropdownMenuItem(text = { Text("Lyric card look: " + com.ubayanda.milonative.data.ShareCard.STYLES[com.ubayanda.milonative.data.ShareCard.style(ctx)]) }, onClick = {
                            fx.edit().putInt("cardstyle", (com.ubayanda.milonative.data.ShareCard.style(ctx) + 1) % com.ubayanda.milonative.data.ShareCard.STYLES.size).apply(); moreMenu = false
                        })
                        DropdownMenuItem(text = { Text("Lyric card shape: " + com.ubayanda.milonative.data.ShareCard.FORMATS[com.ubayanda.milonative.data.ShareCard.format(ctx)]) }, onClick = {
                            fx.edit().putInt("cardfmt", (com.ubayanda.milonative.data.ShareCard.format(ctx) + 1) % com.ubayanda.milonative.data.ShareCard.FORMATS.size).apply(); moreMenu = false
                        })
                        DropdownMenuItem(text = { Text("Player Studio (skins)") }, onClick = { com.ubayanda.milonative.ui.player.PlayerStudioRequest.open = true; moreMenu = false })
                        DropdownMenuItem(text = { Text("Lyric style: " + if (glow) "Glow (Mochy)" else "Classic") }, onClick = { glow = !glow; fx.edit().putBoolean("lyglow", glow).apply(); moreMenu = false })
                        if (glow) DropdownMenuItem(text = { Text("Blur lines not sung: " + if (glowBlur) "on" else "off") }, onClick = { glowBlur = !glowBlur; fx.edit().putBoolean("lyglowblur", glowBlur).apply(); moreMenu = false })
                        DropdownMenuItem(text = { Text("Readings (Latin letters): " + if (roman) "on" else "off") }, onClick = { roman = !roman; fx.edit().putBoolean("lyroman", roman).apply(); moreMenu = false })
                        DropdownMenuItem(text = { Text("Lyrics earlier (\u22120.5 s)") }, onClick = { lyOff -= 500; fx.edit().putLong("lyoff_" + t.id, lyOff).apply() })
                        DropdownMenuItem(text = { Text("Lyrics later (+0.5 s)") }, onClick = { lyOff += 500; fx.edit().putLong("lyoff_" + t.id, lyOff).apply() })
                        if (lyOff != 0L) DropdownMenuItem(text = { Text("Reset timing (" + (if (lyOff > 0) "+" else "") + (lyOff / 1000.0) + " s)") }, onClick = { lyOff = 0; fx.edit().remove("lyoff_" + t.id).apply(); moreMenu = false })
                    }
                }
            }
            if (lyrics != null && lyrics.note.isNotEmpty()) Text(lyrics.note, color = Milo.amber, fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 8.dp))
            // Poster: the line being sung, huge and centred in the space between the top bar and the song details
            val posterLines = lyrics?.synced.orEmpty()
            val posterIdx = posterLines.indexOfLast { it.timeMs <= ui.positionMs - lyOff + 250 }
            val posterWords = when {
                loading || rescanning -> "Looking for lyrics\u2026"
                posterLines.isNotEmpty() && posterIdx >= 0 -> posterLines[posterIdx].text
                else -> t.title
            }
            Box(Modifier.fillMaxWidth().weight(1f).padding(top = posterW * 0.3f), contentAlignment = Alignment.Center) {
                androidx.compose.animation.AnimatedContent(
                    targetState = posterIdx to posterWords,
                    contentKey = { it.first to it.second },
                    transitionSpec = {
                        // the new line rises into the middle while the old one lifts away and fades
                        (androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(420, delayMillis = 120)) +
                            androidx.compose.animation.slideInVertically(androidx.compose.animation.core.tween(520, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { it / 3 } +
                            androidx.compose.animation.scaleIn(androidx.compose.animation.core.tween(520, easing = androidx.compose.animation.core.FastOutSlowInEasing), initialScale = 0.94f))
                            .togetherWith(
                                androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(240)) +
                                    androidx.compose.animation.slideOutVertically(androidx.compose.animation.core.tween(360, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { -it / 4 } +
                                    androidx.compose.animation.scaleOut(androidx.compose.animation.core.tween(360), targetScale = 0.96f),
                            )
                    },
                    label = "poster-words",
                ) { (idx, w) ->
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            w.ifBlank { "\u266A" }, color = Color.White, fontSize = (if (w.length > 40) 30f else 38f).sp, lineHeight = (if (w.length > 40) 35f else 44f).sp,
                            fontWeight = FontWeight.Black, maxLines = 5, overflow = TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        val sub = if (idx >= 0) tr?.getOrNull(idx)?.takeIf { it.isNotBlank() && !it.equals(w, ignoreCase = true) } ?: rm?.getOrNull(idx) else null
                        if (sub != null) Text(sub, color = Milo.amber, fontSize = 16.sp, lineHeight = 22.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 8.dp).fillMaxWidth())
                        val nextLine = if (idx >= 0) posterLines.getOrNull(idx + 1)?.text.orEmpty() else ""
                        val thirdLine = if (idx >= 0) posterLines.getOrNull(idx + 2)?.text.orEmpty() else ""
                        if (nextLine.isNotBlank()) Text(nextLine, color = Color.White.copy(alpha = 0.32f), fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 18.dp).fillMaxWidth())
                        if (thirdLine.isNotBlank()) Text(thirdLine, color = Color.White.copy(alpha = 0.18f), fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 8.dp).fillMaxWidth())
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(t.artist, color = Color.White.copy(alpha = 0.85f), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
            Text(t.title, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            com.ubayanda.milonative.ui.player.SkinSeek(ui, posterAccent, { vm.player.seekTo(it) })
            Spacer(Modifier.height(4.dp))
            com.ubayanda.milonative.ui.player.SkinControls(vm, ui)
        }
    }
}

@Composable
private fun MoreMenu(vm: MainViewModel, t: Track, ui: PlayerUi) {
    var open by remember { mutableStateOf(false) }
    val actions = LocalActions.current
    val canvasCtx = androidx.compose.ui.platform.LocalContext.current
    val songNow by rememberUpdatedState(t)
    val pickCanvas = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { u ->
        if (u != null) com.ubayanda.milonative.ui.components.CanvasStore.set(canvasCtx, songNow.id, u)
    }
    Box {
        StripBtn(MiloIcons.MoreHoriz, "More", false) { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("Add to playlist") }, onClick = { open = false; actions.addToPlaylist(t) })
            DropdownMenuItem(text = { Text("Share song") }, onClick = { open = false; actions.share(t) })
            DropdownMenuItem(text = { Text("Animated cover from music video: " + com.ubayanda.milonative.ui.components.AutoCanvas.labels[com.ubayanda.milonative.ui.components.AutoCanvas.mode(canvasCtx)]) },
                onClick = { com.ubayanda.milonative.ui.components.AutoCanvas.cycle(canvasCtx) })
            DropdownMenuItem(text = { Text("Animated cover: choose a video") }, onClick = { open = false; pickCanvas.launch(arrayOf("video/*")) })
            if (com.ubayanda.milonative.ui.components.CanvasStore.get(canvasCtx, t.id) != null)
                DropdownMenuItem(text = { Text("Animated cover: remove") }, onClick = { open = false; com.ubayanda.milonative.ui.components.CanvasStore.set(canvasCtx, t.id, null) })
            val dtapCtx = androidx.compose.ui.platform.LocalContext.current
            var dtapOn by remember { mutableStateOf(dtapCtx.getSharedPreferences("milo_fx", 0).getBoolean("dtap", true)) }
            DropdownMenuItem(text = { Text(if (dtapOn) "Double-tap gestures: on" else "Double-tap gestures: off") }, onClick = {
                dtapOn = !dtapOn; dtapCtx.getSharedPreferences("milo_fx", 0).edit().putBoolean("dtap", dtapOn).apply(); open = false
            })
            DropdownMenuItem(text = { Text("Car mode") }, onClick = { open = false; actions.carMode() })
            DropdownMenuItem(text = { Text("Bedside clock") }, onClick = { open = false; actions.bedside() })
        }
    }
}

/** "When should we stop?": pick how long, or stop at the end of this song. */
@Composable
private fun TimerContent(vm: MainViewModel, ui: PlayerUi) {
    Column(Modifier.fillMaxWidth().padding(horizontal = Sp.gutter, vertical = Sp.x2), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("When should we stop?", color = Milo.text, style = Ty.title)
        if (ui.sleepLeftMs > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(fmtTime(ui.sleepLeftMs), color = Milo.amber, style = Ty.display)
                    Text("left. The music fades out over the last 30 seconds.", color = Milo.dim, style = Ty.caption)
                }
                MiloChip("Cancel", false) { vm.player.sleepIn(0) }
            }
        }
        listOf(listOf(5, 10, 15), listOf(30, 45, 60)).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { m -> MiloChip("$m min", false) { vm.player.sleepIn(m) } }
            }
        }
        MiloChip("End of this song", false) { vm.player.sleepInMs((ui.durationMs - ui.positionMs).coerceAtLeast(1000L) + 500L) }
    }
}

@Composable
private fun RoundBtn(icon: ImageVector, desc: String, modifier: Modifier = Modifier, tint: Color = Color.White, flat: Boolean = false, badge: String? = null, label: String? = null, onClick: () -> Unit) {
    if (label != null) {
        // dock button: icon with a short caption under it so nothing has to be guessed
        Column(
            modifier.width(56.dp).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, desc, tint = tint, modifier = Modifier.size(24.dp))
            Text(label, color = tint.copy(alpha = 0.85f), fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
        }
        return
    }
    Box(
        modifier.size(if (flat) 48.dp else 52.dp).clip(CircleShape).then(if (flat) Modifier else Modifier.background(Color.Black.copy(alpha = 0.32f))).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, desc, tint = tint, modifier = Modifier.size(24.dp).then(if (badge != null) Modifier.offset(y = (-5).dp) else Modifier))
        if (badge != null) Text(badge, color = tint, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp))
    }
}

@Composable
private fun IconTap(icon: ImageVector, desc: String, size: Dp, tint: Color, onClick: () -> Unit) {
    Box(Modifier.size(52.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, desc, tint = tint, modifier = Modifier.size(size))
    }
}

@Composable
private fun Scrubber(ui: PlayerUi, modifier: Modifier = Modifier, tint: Int = 0, onSeek: (Long) -> Unit) {
    var dragging by remember { mutableStateOf(false) }
    var dragFrac by remember { mutableFloatStateOf(0f) }
    val frac = if (ui.durationMs > 0) (ui.positionMs.toFloat() / ui.durationMs).coerceIn(0f, 1f) else 0f
    val shown = if (dragging) dragFrac else frac
    val dur = ui.durationMs
    val ring = Milo.panel
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val tickAt = remember { intArrayOf(-1) }
    // the played part ripples while music plays and settles flat (with a bounce) when paused
    val calm = com.ubayanda.milonative.ui.components.rememberCalm()
    val waveK by androidx.compose.animation.core.animateFloatAsState(if (ui.isPlaying && !calm) 1f else 0f, com.ubayanda.milonative.ui.components.Bounce.scale(), label = "wave")
    var wavePhase by remember { mutableFloatStateOf(0f) }
    val run = ui.isPlaying && !calm
    // the wave travels about 32 dp a second; the angle is accumulated from real frame times so it never jumps when playback stops and starts
    // (the frame loop that animated the wave is gone: no wave, no per-frame work)
    val pressK by androidx.compose.animation.core.animateFloatAsState(if (dragging) 1f else 0f, com.ubayanda.milonative.ui.components.Bounce.scale(), label = "handle")
    // the bar takes the cover's colour, softened toward white, and changes colour smoothly when the song changes
    val baseC = if (tint == 0) Milo.amber else Color(0xFF000000.toInt() or tint)
    val fillC by androidx.compose.animation.animateColorAsState(androidx.compose.ui.graphics.lerp(baseC, Color.White, 0.35f), androidx.compose.animation.core.tween(600), label = "barC")
    Box(
        modifier.fillMaxWidth().height(44.dp)
            .pointerInput(dur) { detectTapGestures { off -> if (dur > 0) onSeek((off.x / size.width * dur).toLong()) } }
            .pointerInput(dur) {
                detectHorizontalDragGestures(
                    onDragStart = { off -> dragging = true; dragFrac = (off.x / size.width).coerceIn(0f, 1f); tickAt[0] = (dragFrac * 50).toInt(); haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove) },
                    onHorizontalDrag = { change, _ ->
                        dragFrac = (change.position.x / size.width).coerceIn(0f, 1f)
                        // a light tick every 2 % of the song while scrubbing
                        val k = (dragFrac * 50).toInt()
                        if (k != tickAt[0]) { tickAt[0] = k; haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove) }
                    },
                    onDragEnd = { if (dur > 0) onSeek((dragFrac * dur).toLong()); dragging = false },
                    onDragCancel = { dragging = false },
                )
            }
    ) {
        Canvas(Modifier.matchParentSize()) {
            val cy = size.height / 2
            val w = size.width
            val th = 4.dp.toPx()
            val wl = 40.dp.toPx()
            val amp = 3.dp.toPx() * waveK
            val r = 9.dp.toPx()
            val themed = ChromeStyle.on || Nebula.on || Sticker.on || Spider.on
            if (!themed) {
                // a thick plain pill with a big round handle (no wave since Round 86)
                val bar = 10.dp.toPx()
                val tr = (10f + 2f * pressK).dp.toPx()
                val tx2 = (w * shown).coerceIn(tr, w - tr)
                val top = cy - bar / 2
                drawRoundRect(fillC.copy(alpha = 0.28f), Offset(0f, top), Size(w, bar), CornerRadius(bar / 2))
                drawRoundRect(fillC, Offset(0f, top), Size(tx2, bar), CornerRadius(bar / 2))
                drawCircle(androidx.compose.ui.graphics.lerp(fillC, Color.White, 0.55f), tr, Offset(tx2, cy))
                return@Canvas
            }
            val gapHalf = (if (ChromeStyle.on) 14.dp else if (themed) 13.dp else 8.dp).toPx()
            val tx = (w * shown).coerceIn(r, w - r)
            val accent = if (Nebula.on) Nebula.cyan else if (Spider.on) Spider.red else Color.White
            // the part still to play: a straight line with a gap around the handle and a small stop dot at the end
            val tStart = tx + gapHalf
            if (tStart < w - th) {
                drawLine(Color.White.copy(alpha = 0.28f), Offset(tStart, cy), Offset(w - th / 2, cy), th, StrokeCap.Round)
            }
            if (tStart < w - 14.dp.toPx()) drawCircle(accent, 2.dp.toPx(), Offset(w - 4.dp.toPx(), cy))
            // the part already played
            val brush: Brush = when {
                ChromeStyle.on -> Brush.verticalGradient(0f to Color.White, 0.35f to Color(0xFF8E959E), 0.55f to Color(0xFF30343B), 0.8f to Color(0xFFD3D7DD), 1f to Color(0xFF8E959E), startY = cy - th / 2, endY = cy + th / 2)
                Nebula.on -> Brush.horizontalGradient(listOf(Nebula.cyan, Nebula.magenta), 0f, w)
                else -> SolidColor(if (Spider.on) Spider.red else Color.White)
            }
            wavyActive(brush, th / 2, tx - gapHalf, cy, th, 0f, wl, wavePhase, 18.dp.toPx())
            // the handle
            if (ChromeStyle.on) {
                drawChromeBall(Offset(tx, cy), 13.dp.toPx())
            } else if (Nebula.on) {
                drawCircle(Nebula.cyan.copy(alpha = 0.22f), 18.dp.toPx(), Offset(tx, cy)); drawCircle(Nebula.cyan.copy(alpha = 0.5f), 12.dp.toPx(), Offset(tx, cy)); drawCircle(Color.White, 7.dp.toPx(), Offset(tx, cy))
            } else if (Sticker.on) {
                drawSparkle(Offset(tx, cy), 16.dp.toPx(), Color.White)
            } else if (Spider.on) {
                drawSpider(Offset(tx, cy), 12.dp.toPx(), phase = shown * 40f)
            } else {
                // a vertical pill that narrows while it is held, like Android 16's slider handle
                val hw = (4f - 2f * pressK).dp.toPx()
                drawRoundRect(Color.White, Offset(tx - hw / 2, cy - 15.dp.toPx()), Size(hw, 30.dp.toPx()), CornerRadius(hw / 2))
            }
        }
    }
}

@Composable
private fun RoundDark(icon: ImageVector, desc: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.size(58.dp).clip(CircleShape).background(Milo.card2).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, tint = Milo.text, modifier = Modifier.size(28.dp)) }
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
                    Icon(if (ui.isPlaying) MiloIcons.Pause else MiloIcons.PlayArrow, "Play or pause", tint = Color.Black, modifier = Modifier.size(32.dp))
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

/** the open player's cover rect; every style but edge to edge shows its cover as a square stage under the status bar, not the whole art region */
private fun morphFrom(art: androidx.compose.ui.geometry.Rect?, growLate: Boolean, d: androidx.compose.ui.unit.Density, top: Int): androidx.compose.ui.geometry.Rect? =
    if (growLate && art != null) with(d) {
        val side = art.width - 60.dp.toPx()
        androidx.compose.ui.geometry.Rect(art.left + 30.dp.toPx(), art.top + top + 64.dp.toPx(), art.left + 30.dp.toPx() + side, art.top + top + 64.dp.toPx() + side)
    } else art

@Composable
private fun SheetMorphLayer(
    progress: () -> Float, cover: androidx.compose.ui.graphics.ImageBitmap?, title: String, artist: String,
    art: androidx.compose.ui.geometry.Rect?, titleRect: androidx.compose.ui.geometry.Rect?, growLate: Boolean,
    isPlaying: Boolean, playRect: androidx.compose.ui.geometry.Rect?, nextRect: androidx.compose.ui.geometry.Rect?,
) {
    val d = androidx.compose.ui.platform.LocalDensity.current
    val top = androidx.compose.foundation.layout.WindowInsets.statusBars.getTop(d)
    // read here, so only this layer redraws while the sheet moves
    com.ubayanda.milonative.ui.player.CoverMorphOverlay(cover, title, artist, morphFrom(art, growLate, d, top), titleRect, progress(), growLate, isPlaying, playRect, nextRect)
}

@Composable
private fun LyricsFlightLayer(lp: () -> Float, cover: androidx.compose.ui.graphics.ImageBitmap?, art: androidx.compose.ui.geometry.Rect?, growLate: Boolean) {
    val d = androidx.compose.ui.platform.LocalDensity.current
    val top = androidx.compose.foundation.layout.WindowInsets.statusBars.getTop(d)
    com.ubayanda.milonative.ui.player.LyricsCoverFlight(cover, morphFrom(art, growLate, d, top), lp(), growLate)
}


/** A button in the strip under the art: an icon on its own round backdrop. When it is on, the backdrop turns solid white and the icon dark (no accent colour). */
@Composable
private fun StripBtn(icon: ImageVector, desc: String, on: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.height(52.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(46.dp).clip(CircleShape)
                .background(if (on) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.12f), CircleShape)
                .border(1.dp, Color.White.copy(alpha = if (on) 0f else 0.14f), CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, desc, tint = if (on) Color.Black else Color.White, modifier = Modifier.size(22.dp)) }
    }
}

/** The line being sung and the two after it, shown under the controls. */
@Composable
private fun LiveLyrics(l: Lyrics?, loading: Boolean, positionMs: Long) {
    val lines: List<String>
    val key: Int
    when {
        loading -> { lines = listOf("Lyrics"); key = -2 }
        l == null -> { lines = listOf("No lyrics for this song"); key = -1 }
        l.synced.isNotEmpty() -> {
            val i = l.synced.indexOfLast { it.timeMs <= positionMs + 250 }.coerceAtLeast(0)
            lines = l.synced.drop(i).map { it.text }.filter { it.isNotBlank() }.take(2).ifEmpty { listOf("Lyrics") }
            key = i
        }
        else -> { lines = l.plain.lineSequence().filter { it.isNotBlank() }.take(2).toList().ifEmpty { listOf("Lyrics") }; key = 0 }
    }
    androidx.compose.animation.Crossfade(targetState = key to lines, animationSpec = androidx.compose.animation.core.tween(260), label = "liveLyrics") { (_, ls) ->
        Column(Modifier.fillMaxWidth().padding(horizontal = Sp.x6), horizontalAlignment = Alignment.Start) {
            ls.forEachIndexed { n, line ->
                Text(
                    line, color = Color.White.copy(alpha = if (n == 0) 1f else 0.4f),
                    fontSize = if (n == 0) 26.sp else 16.sp, fontWeight = if (n == 0) FontWeight.Black else FontWeight.SemiBold,
                    lineHeight = if (n == 0) 31.sp else 22.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = if (n == 0) 4.dp else 2.dp),
                )
            }
        }
    }
}
