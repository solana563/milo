package com.ubayanda.milonative.ui.player

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubayanda.milonative.model.Lyrics
import com.ubayanda.milonative.model.PlayerUi
import com.ubayanda.milonative.model.Track
import com.ubayanda.milonative.ui.MainViewModel
import com.ubayanda.milonative.ui.components.Cover
import com.ubayanda.milonative.ui.components.MiloChip
import com.ubayanda.milonative.ui.components.MiloSlider
import com.ubayanda.milonative.ui.components.MiloSwitch
import com.ubayanda.milonative.ui.lyrics.LyricsEntry
import com.ubayanda.milonative.ui.lyrics.WordTimestamp
import com.ubayanda.milonative.ui.screens.BedsideScreen
import com.ubayanda.milonative.ui.screens.IpodScreen
import com.ubayanda.milonative.ui.theme.Milo
import com.ubayanda.milonative.ui.theme.MiloIcons
import kotlin.math.max

/**
 * Player skins: the Now Playing screen can be drawn in different ways, and Player Studio (the picker below) switches between them.
 * The idea and the skin names come from Mochy Music's player styles and Player Studio (GPL-3.0, github.com/realidkroo/Mochy-Music,
 * ui/player/styles and ui/player/studio); the drawing is written for milo on milo's own player state.
 *
 * FLAGSHIP is milo's own full player (NowPlayingScreen). IPOD and BEDSIDE are milo's existing full-screen skins.
 * POSTER, TIMELINE and LYRICS are drawn here.
 */
enum class PlayerSkin(val key: String, val label: String, val blurb: String) {
    FLAGSHIP("flagship", "Flagship", "milo's own player: the cover, lyrics, queue and every setting"),
    POSTER("poster", "Poster", "A big round cover and the song's words set huge beneath it"),
    TIMELINE("timeline", "Timeline", "The lyrics as a timeline: times down the side, the sung line lit"),
    LYRICS("lyrics", "Lyrics", "Only the words, centred and big, with small controls"),
    IPOD("ipod", "iPod", "A little screen and a click wheel"),
    BEDSIDE("bedside", "Bedside", "A dim clock with the song under it");

    companion object {
        fun of(key: String?): PlayerSkin = values().firstOrNull { it.key == key } ?: FLAGSHIP
    }
}

object PlayerSkins {
    private const val PREFS = "milo_fx"
    private const val KEY = "pskin"
    var current by mutableStateOf(PlayerSkin.FLAGSHIP)
        private set

    fun load(ctx: Context) {
        current = PlayerSkin.of(ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null))
    }

    fun set(ctx: Context, skin: PlayerSkin) {
        current = skin
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, skin.key).apply()
    }
}

/**
 * What the Studio's Customize tab changes on the drawn skins (Poster, Timeline, Lyrics). Each value is saved on its own and applies live.
 * [accent] 0 = the cover's colour, 1 = milo's amber, 2 = white. [backdrop] 0 = a gradient of that colour, 1 = plain black.
 */
object SkinStyle {
    private const val PREFS = "milo_fx"
    var textScale by mutableFloatStateOf(1f)
        private set
    var circleScale by mutableFloatStateOf(1f)
        private set
    var accent by mutableStateOf(0)
        private set
    var backdrop by mutableStateOf(0)
        private set
    var seek by mutableStateOf(true)
        private set
    var controls by mutableStateOf(true)
        private set
    var centred by mutableStateOf(false)
        private set
    var info by mutableStateOf(true)
        private set
    private var loaded = false

    fun load(ctx: Context) {
        if (loaded) return
        loaded = true
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        textScale = p.getFloat("sk_text", 1f); circleScale = p.getFloat("sk_circle", 1f)
        accent = p.getInt("sk_accent", 0); backdrop = p.getInt("sk_bg", 0)
        seek = p.getBoolean("sk_seek", true); controls = p.getBoolean("sk_controls", true); centred = p.getBoolean("sk_centred", false); info = p.getBoolean("sk_info", true)
    }

    fun setText(ctx: Context, v: Float) { textScale = v; put(ctx) { putFloat("sk_text", v) } }
    fun setCircle(ctx: Context, v: Float) { circleScale = v; put(ctx) { putFloat("sk_circle", v) } }
    fun setAccent(ctx: Context, v: Int) { accent = v; put(ctx) { putInt("sk_accent", v) } }
    fun setBackdrop(ctx: Context, v: Int) { backdrop = v; put(ctx) { putInt("sk_bg", v) } }
    fun setSeek(ctx: Context, v: Boolean) { seek = v; put(ctx) { putBoolean("sk_seek", v) } }
    fun setControls(ctx: Context, v: Boolean) { controls = v; put(ctx) { putBoolean("sk_controls", v) } }
    fun setCentred(ctx: Context, v: Boolean) { centred = v; put(ctx) { putBoolean("sk_centred", v) } }
    fun setInfo(ctx: Context, v: Boolean) { info = v; put(ctx) { putBoolean("sk_info", v) } }

    /** a ready-made combination of the options above */
    fun preset(ctx: Context, name: String) {
        when (name) {
            "Midnight" -> { setText(ctx, 1f); setCircle(ctx, 1f); setAccent(ctx, 0); setBackdrop(ctx, 0); setSeek(ctx, true); setControls(ctx, true); setCentred(ctx, false); setInfo(ctx, true) }
            "Mono" -> { setText(ctx, 1f); setCircle(ctx, 0.9f); setAccent(ctx, 2); setBackdrop(ctx, 1); setSeek(ctx, true); setControls(ctx, true); setCentred(ctx, false); setInfo(ctx, true) }
            "Loud" -> { setText(ctx, 1.4f); setCircle(ctx, 1.2f); setAccent(ctx, 1); setBackdrop(ctx, 0); setSeek(ctx, false); setControls(ctx, false); setCentred(ctx, true); setInfo(ctx, false) }
            "Quiet" -> { setText(ctx, 0.8f); setCircle(ctx, 0.7f); setAccent(ctx, 0); setBackdrop(ctx, 1); setSeek(ctx, true); setControls(ctx, true); setCentred(ctx, true); setInfo(ctx, true) }
        }
    }
    fun reset(ctx: Context) {
        textScale = 1f; circleScale = 1f; accent = 0; backdrop = 0; seek = true; controls = true; centred = false; info = true
        put(ctx) { remove("sk_text"); remove("sk_circle"); remove("sk_accent"); remove("sk_bg"); remove("sk_seek"); remove("sk_controls"); remove("sk_centred"); remove("sk_info") }
    }

    private fun put(ctx: Context, f: android.content.SharedPreferences.Editor.() -> Unit) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply(f).apply()
    }
}

private fun skinBrush(b: Brush): Brush = if (SkinStyle.backdrop == 1) SolidColor(Color.Black) else b

/** Lets the flagship's menu ask the host for Player Studio without passing callbacks through the whole screen. */
object PlayerStudioRequest {
    var open by mutableStateOf(false)
}

/**
 * Chooses what the open player shows. [flagship] draws milo's own Now Playing screen. Any other skin fills the screen, with a small
 * Studio button that opens the picker, so you can always change skin or go back to the flagship.
 */
@Composable
fun PlayerSkinHost(
    vm: MainViewModel,
    t: Track,
    ui: PlayerUi,
    onClose: () -> Unit,
    flagship: @Composable () -> Unit,
) {
    val ctx = LocalContext.current
    remember { PlayerSkins.load(ctx); SkinStyle.load(ctx) }
    val skin = PlayerSkins.current
    var studio by remember { mutableStateOf(false) }
    val asked = PlayerStudioRequest.open
    LaunchedEffect(asked) { if (asked) { studio = true; PlayerStudioRequest.open = false } }

    if (skin == PlayerSkin.FLAGSHIP) {
        Box(Modifier.fillMaxSize()) {
            flagship()
            if (studio) PlayerStudio(vm, t) { studio = false }
        }
        return
    }

    BackHandler(enabled = !studio) { onClose() }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AnimatedContent(
            targetState = skin,
            transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(200)) },
            label = "skin",
        ) { s ->
            when (s) {
                PlayerSkin.POSTER -> PosterSkin(vm, t, ui, onClose)
                PlayerSkin.TIMELINE -> TimelineSkin(vm, t, ui, onClose)
                PlayerSkin.LYRICS -> LyricsSkin(vm, t, ui, onClose)
                PlayerSkin.IPOD -> IpodScreen(vm, t, ui, onClose)
                PlayerSkin.BEDSIDE -> BedsideScreen(vm, t, ui, onClose)
                PlayerSkin.FLAGSHIP -> Unit
            }
        }
        // the Studio button, on every skin so you can always change skin; on the dim Bedside clock it is faint
        if (!studio) {
            Box(
                Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(end = 12.dp, top = 8.dp).size(40.dp)
                    .graphicsLayer { alpha = if (skin == PlayerSkin.BEDSIDE) 0.45f else 1f }
                    .clip(CircleShape).background(Color.Black.copy(alpha = 0.38f))
                    .clickable { studio = true },
                contentAlignment = Alignment.Center,
            ) { Icon(MiloIcons.Tune, "Player Studio", tint = Color.White, modifier = Modifier.size(20.dp)) }
        }
        if (studio) PlayerStudio(vm, t) { studio = false }
    }
}

// ---- Player Studio (the picker) --------------------------------------------------------------------------------------

/** A sheet of skin cards: tap one and the player switches to it. */
@Composable
fun PlayerStudio(vm: MainViewModel, t: Track, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val chosen = PlayerSkins.current
    BackHandler { onDone() }
    var tab by remember { mutableStateOf(0) }
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (tab == 0) 0.55f else 0f))
            .pointerInput(Unit) { detectTapGestures { onDone() } },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)).background(Milo.panel)
                .pointerInput(Unit) { detectTapGestures { } }.navigationBarsPadding().padding(top = 14.dp, bottom = 16.dp),
        ) {
            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Player Studio", color = Milo.text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                MiloChip("Skins", tab == 0) { tab = 0 }
                MiloChip("Customize", tab == 1) { tab = 1 }
            }
            if (tab == 0) {
                Text("Pick how the player looks. milo keeps your choice.", color = Milo.dim, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PlayerSkin.values().forEach { s ->
                        SkinCard(vm, t, s, s == chosen) { PlayerSkins.set(ctx, s); onDone() }
                    }
                }
                Text(chosen.blurb, color = Milo.dim, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp))
            } else CustomizeTab(chosen)
        }
    }
}

/** The Studio's editor for the drawn skins: changes show on the player above the sheet as you make them. */
@Composable
private fun CustomizeTab(skin: PlayerSkin) {
    val ctx = LocalContext.current
    val drawn = skin == PlayerSkin.POSTER || skin == PlayerSkin.TIMELINE || skin == PlayerSkin.LYRICS
    Column(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
        if (!drawn) {
            Text(
                if (skin == PlayerSkin.FLAGSHIP) "The flagship has its own settings in Now Playing: cover style, backdrop, sound and lyrics."
                else "${skin.label} has no options yet. Choose Poster, Timeline or Lyrics to change their look.",
                color = Milo.dim, fontSize = 13.sp, modifier = Modifier.padding(vertical = 10.dp),
            )
            return@Column
        }
        Text("Presets", color = Milo.text, fontSize = 14.sp)
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Midnight", "Mono", "Loud", "Quiet").forEach { n -> MiloChip(n, false) { SkinStyle.preset(ctx, n) } }
        }
        Text("Text size", color = Milo.text, fontSize = 14.sp)
        MiloSlider(value = SkinStyle.textScale, valueRange = 0.7f..1.4f, onValueChange = { SkinStyle.setText(ctx, (it * 20).toInt() / 20f) })
        if (skin == PlayerSkin.POSTER) {
            Text("Cover circle size", color = Milo.text, fontSize = 14.sp)
            MiloSlider(value = SkinStyle.circleScale, valueRange = 0.6f..1.3f, onValueChange = { SkinStyle.setCircle(ctx, (it * 20).toInt() / 20f) })
        }
        Text("Colour", color = Milo.text, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
        Row(Modifier.padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiloChip("From the cover", SkinStyle.accent == 0) { SkinStyle.setAccent(ctx, 0) }
            MiloChip("Amber", SkinStyle.accent == 1) { SkinStyle.setAccent(ctx, 1) }
            MiloChip("White", SkinStyle.accent == 2) { SkinStyle.setAccent(ctx, 2) }
        }
        Row(Modifier.padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiloChip("Gradient backdrop", SkinStyle.backdrop == 0) { SkinStyle.setBackdrop(ctx, 0) }
            MiloChip("Plain black", SkinStyle.backdrop == 1) { SkinStyle.setBackdrop(ctx, 1) }
        }
        if (skin != PlayerSkin.TIMELINE) SkinToggle("Song title and artist", SkinStyle.info) { SkinStyle.setInfo(ctx, it) }
        SkinToggle("Seek bar", SkinStyle.seek) { SkinStyle.setSeek(ctx, it) }
        SkinToggle("Play, previous and next", SkinStyle.controls) { SkinStyle.setControls(ctx, it) }
        if (skin == PlayerSkin.POSTER) SkinToggle("Centre the words", SkinStyle.centred) { SkinStyle.setCentred(ctx, it) }
        MiloChip("Reset", false, Modifier.padding(top = 6.dp)) { SkinStyle.reset(ctx) }
    }
}

@Composable
private fun SkinToggle(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Milo.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
        MiloSwitch(checked = on, onCheckedChange = onChange)
    }
}

@Composable
private fun SkinCard(vm: MainViewModel, t: Track, s: PlayerSkin, selected: Boolean, onPick: () -> Unit) {
    val ring = if (selected) Milo.amber else Milo.line
    Column(
        Modifier.width(104.dp).clickable(remember { MutableInteractionSource() }, null) { onPick() },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(width = 104.dp, height = 184.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF0B0B0D))
                .drawBehind { drawRect(ring, style = androidx.compose.ui.graphics.drawscope.Stroke(width = if (selected) 6f else 2f)) },
        ) {
            // a still drawing of the skin: not the live thing, just enough to tell them apart
            when (s) {
                PlayerSkin.FLAGSHIP -> Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.padding(top = 14.dp)) { Cover(vm, t, 76.dp, 8.dp, px = 200) }
                    Spacer(Modifier.height(8.dp)); SkinBar(60.dp, 5.dp); Spacer(Modifier.height(4.dp)); SkinBar(40.dp, 4.dp, 0.5f)
                    Spacer(Modifier.weight(1f)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { SkinDot(); SkinDot(); SkinDot() }
                }
                PlayerSkin.POSTER -> Box(Modifier.fillMaxSize()) {
                    Box(Modifier.offset((-34).dp, (-34).dp).size(110.dp).clip(CircleShape).background(Milo.amber.copy(alpha = 0.7f)))
                    Column(Modifier.align(Alignment.BottomStart).padding(10.dp)) {
                        SkinBar(80.dp, 9.dp); Spacer(Modifier.height(4.dp)); SkinBar(64.dp, 9.dp); Spacer(Modifier.height(8.dp)); SkinBar(40.dp, 4.dp, 0.5f)
                    }
                }
                PlayerSkin.TIMELINE -> Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(5) { n ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SkinBar(14.dp, 3.dp, 0.4f); Spacer(Modifier.width(6.dp))
                            SkinBar(if (n == 2) 54.dp else 38.dp, if (n == 2) 6.dp else 4.dp, if (n == 2) 1f else 0.45f)
                        }
                    }
                }
                PlayerSkin.LYRICS -> Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    SkinBar(54.dp, 4.dp, 0.3f); Spacer(Modifier.height(8.dp)); SkinBar(76.dp, 8.dp); Spacer(Modifier.height(5.dp)); SkinBar(60.dp, 8.dp); Spacer(Modifier.height(8.dp)); SkinBar(50.dp, 4.dp, 0.3f)
                }
                PlayerSkin.IPOD -> Column(Modifier.fillMaxSize().background(Color(0xFFEDEBE6)).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFFD5DFCC)))
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.size(70.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) { Box(Modifier.size(24.dp).clip(CircleShape).background(Color(0xFFDAD8D2))) }
                    Spacer(Modifier.height(6.dp))
                }
                PlayerSkin.BEDSIDE -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("21:07", color = Color(0xFFB9503C), fontSize = 26.sp, fontWeight = FontWeight.Light)
                    Spacer(Modifier.height(8.dp)); SkinBar(50.dp, 4.dp, 0.4f)
                }
            }
        }
        Text(s.label, color = if (selected) Milo.amber else Milo.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun SkinBar(w: Dp, h: Dp, a: Float = 0.85f) {
    Box(Modifier.size(w, h).clip(RoundedCornerShape(h)).background(Color.White.copy(alpha = a)))
}

@Composable
private fun SkinDot() { Box(Modifier.size(14.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.8f))) }

// ---- shared bits of the drawn skins ----------------------------------------------------------------------------------

/** The song's synced lines, and whether they've finished loading. Empty lines = no synced lyrics. */
@Composable
private fun rememberSkinLines(vm: MainViewModel, t: Track): Pair<List<LyricsEntry>, Boolean> {
    var loaded by remember(t.id) { mutableStateOf(false) }
    val lyrics by produceState<Lyrics?>(null, t.id) {
        value = com.ubayanda.milonative.util.ErrorLog.catching { vm.lyricsFor(t) }.getOrNull()
        loaded = true
    }
    val lines = remember(lyrics) {
        val l = lyrics?.synced.orEmpty()
        l.mapIndexed { n, tl ->
            val nextLine = l.getOrNull(n + 1)?.timeMs ?: (tl.timeMs + 4000)
            LyricsEntry(
                time = tl.timeMs, text = tl.text,
                words = tl.words.takeIf { it.isNotEmpty() }?.mapIndexed { k, w ->
                    val end = tl.words.getOrNull(k + 1)?.timeMs ?: nextLine
                    WordTimestamp(w.text, w.timeMs / 1000.0, end / 1000.0)
                },
            )
        }
    }
    return lines to loaded
}

private fun currentIndex(lines: List<LyricsEntry>, pos: Long): Int {
    var idx = -1
    for (i in lines.indices) { if (lines[i].time <= pos) idx = i else break }
    return idx
}

private fun mmss(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

/** A thin seek bar: drag or tap to move the playhead. */
@Composable
internal fun SkinSeek(ui: PlayerUi, accent: Color, onSeek: (Long) -> Unit, modifier: Modifier = Modifier) {
    var dragging by remember { mutableStateOf(false) }
    var frac by remember { mutableFloatStateOf(0f) }
    val live = if (ui.durationMs > 0) (ui.positionMs.toFloat() / ui.durationMs).coerceIn(0f, 1f) else 0f
    val shown = if (dragging) frac else live
    Column(modifier) {
        Box(
            Modifier.fillMaxWidth().height(28.dp)
                .pointerInput(ui.durationMs) {
                    detectTapGestures { o -> if (ui.durationMs > 0) onSeek((o.x / size.width * ui.durationMs).toLong().coerceIn(0L, ui.durationMs)) }
                }
                .pointerInput(ui.durationMs) {
                    detectHorizontalDragGestures(
                        onDragStart = { o -> dragging = true; frac = (o.x / size.width).coerceIn(0f, 1f) },
                        onDragEnd = { dragging = false; if (ui.durationMs > 0) onSeek((frac * ui.durationMs).toLong()) },
                        onDragCancel = { dragging = false },
                        onHorizontalDrag = { c, _ -> frac = (c.position.x / size.width).coerceIn(0f, 1f) },
                    )
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f)))
            Box(Modifier.fillMaxWidth(shown).height(4.dp).clip(CircleShape).background(accent))
        }
        Row(Modifier.fillMaxWidth()) {
            Text(mmss(if (dragging) (frac * ui.durationMs).toLong() else ui.positionMs), color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
            Spacer(Modifier.weight(1f))
            Text(mmss(ui.durationMs), color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
        }
    }
}

@Composable
internal fun SkinControls(vm: MainViewModel, ui: PlayerUi, modifier: Modifier = Modifier, big: Dp = 64.dp) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp).clip(CircleShape).clickable { vm.player.previous() }, contentAlignment = Alignment.Center) {
            Icon(MiloIcons.SkipPrevious, "Previous", tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(18.dp))
        Box(Modifier.size(big).clip(CircleShape).background(Color.White).clickable { vm.player.toggle() }, contentAlignment = Alignment.Center) {
            Icon(if (ui.isPlaying) MiloIcons.Pause else MiloIcons.PlayArrow, if (ui.isPlaying) "Pause" else "Play", tint = Color.Black, modifier = Modifier.size(big * 0.5f))
        }
        Spacer(Modifier.width(18.dp))
        Box(Modifier.size(48.dp).clip(CircleShape).clickable { vm.player.next() }, contentAlignment = Alignment.Center) {
            Icon(MiloIcons.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(28.dp))
        }
    }
}

/** the song's colour, from milo's cover tint (0 when none yet) */
@Composable
private fun rememberSkinAccent(vm: MainViewModel): Color {
    val tint by vm.tint.collectAsState()
    return when (SkinStyle.accent) { 1 -> Milo.amber; 2 -> Color.White; else -> if (tint == 0) Milo.amber else Color(tint).copy(alpha = 1f) }
}

@Composable
private fun SkinBack(onClose: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.size(40.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.38f)).clickable { onClose() }, contentAlignment = Alignment.Center) {
        Icon(MiloIcons.ArrowBackIosNew, "Close", tint = Color.White, modifier = Modifier.size(18.dp).graphicsLayer { rotationZ = -90f })
    }
}

// ---- Poster ---------------------------------------------------------------------------------------------------------

/**
 * Mochy's Poster idea: the cover cut into a great circle swung in from the top-left, and beneath it the song's current line performed huge,
 * with the artist and title set small under it. A song with no synced lyrics shouts its title instead. Swipe sideways for next/previous.
 */
@Composable
private fun PosterSkin(vm: MainViewModel, t: Track, ui: PlayerUi, onClose: () -> Unit) {
    val (lines, loaded) = rememberSkinLines(vm, t)
    val accent = rememberSkinAccent(vm)
    val pos by rememberUpdatedState(ui.positionMs)
    val idx by remember(lines) { derivedStateOf { currentIndex(lines, pos) } }
    val words = if (lines.isNotEmpty() && idx >= 0) lines[idx].text else t.title
    var hx by remember { mutableFloatStateOf(0f) }
    BoxWithConstraints(
        Modifier.fillMaxSize().background(skinBrush(Brush.verticalGradient(listOf(accent.copy(alpha = 0.55f), Color(0xFF080809), Color.Black))))
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = { if (hx < -160f) vm.player.next() else if (hx > 160f) vm.player.previous(); hx = 0f },
                    onDragCancel = { hx = 0f },
                    onHorizontalDrag = { _, d -> hx += d },
                )
            },
    ) {
        val circle = maxWidth * 1.15f * SkinStyle.circleScale
        Box(
            Modifier.offset(x = -(circle * 0.28f), y = -(circle * 0.22f)).size(circle).clip(CircleShape)
                .pointerInput(Unit) { detectTapGestures { vm.player.toggle() } },
        ) { Cover(vm, t, circle, circle / 2, px = 700) }
        Column(
            Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            AnimatedContent(targetState = words, transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(160)) }, label = "poster-words") { w ->
                val k = SkinStyle.textScale
                Text(
                    w.ifBlank { "♪" }, color = Color.White, fontSize = (if (w.length > 40) 30f else 40f).sp * k, lineHeight = (if (w.length > 40) 34f else 44f).sp * k,
                    fontWeight = FontWeight.Black, maxLines = 4, overflow = TextOverflow.Ellipsis,
                    textAlign = if (SkinStyle.centred) TextAlign.Center else TextAlign.Start, modifier = Modifier.fillMaxWidth(),
                )
            }
            if (SkinStyle.info) {
                Spacer(Modifier.height(14.dp))
                Text(t.artist, color = Color.White.copy(alpha = 0.85f), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = if (SkinStyle.centred) TextAlign.Center else TextAlign.Start, modifier = Modifier.fillMaxWidth())
                Text(t.title, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = if (SkinStyle.centred) TextAlign.Center else TextAlign.Start, modifier = Modifier.fillMaxWidth())
            }
            if (!loaded) Text("Looking for lyrics…", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
            Spacer(Modifier.height(10.dp))
            if (SkinStyle.seek) SkinSeek(ui, accent, { vm.player.seekTo(it) })
            Spacer(Modifier.height(4.dp))
            if (SkinStyle.controls) SkinControls(vm, ui)
        }
        SkinBack(onClose, Modifier.statusBarsPadding().padding(start = 12.dp, top = 8.dp))
    }
}

// ---- Timeline ---------------------------------------------------------------------------------------------------------

/** The lyrics as a timeline: each line's time down the left, a rail, and the line being sung lit. Tap a line to jump there. */
@Composable
private fun TimelineSkin(vm: MainViewModel, t: Track, ui: PlayerUi, onClose: () -> Unit) {
    val (lines, loaded) = rememberSkinLines(vm, t)
    val accent = rememberSkinAccent(vm)
    val pos by rememberUpdatedState(ui.positionMs)
    val idx by remember(lines) { derivedStateOf { currentIndex(lines, pos) } }
    val list = rememberLazyListState()
    LaunchedEffect(idx) { if (idx >= 0) com.ubayanda.milonative.util.ErrorLog.catching { list.animateScrollToItem(max(0, idx - 2)) } }
    Column(Modifier.fillMaxSize().background(skinBrush(Brush.verticalGradient(listOf(accent.copy(alpha = 0.35f), Color.Black)))).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 64.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            SkinBack(onClose)
            Spacer(Modifier.width(10.dp))
            Cover(vm, t, 44.dp, 8.dp, px = 160)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(t.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(t.artist, color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (lines.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(if (loaded) "No synced lyrics for this song" else "Looking for lyrics…", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
            }
        } else {
            LazyColumn(state = list, modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 40.dp)) {
                itemsIndexed(lines) { n, line ->
                    val state = if (n == idx) 2 else if (n < idx) 0 else 1
                    Row(
                        Modifier.fillMaxWidth().height(IntrinsicSize.Min).clickable(remember { MutableInteractionSource() }, null) { vm.player.seekTo(line.time) },
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(mmss(line.time), color = Color.White.copy(alpha = if (state == 2) 0.9f else 0.35f), fontSize = 11.sp, modifier = Modifier.width(38.dp).padding(top = 6.dp))
                        // the rail: a dot per line, the sung one big and lit, a line joining them
                        Box(Modifier.width(22.dp).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                            Box(Modifier.padding(top = 9.dp).width(2.dp).fillMaxHeight().background(Color.White.copy(alpha = 0.12f)))
                            Box(
                                Modifier.padding(top = if (state == 2) 4.dp else 6.dp).size(if (state == 2) 10.dp else 6.dp).clip(CircleShape)
                                    .background(if (state == 2) accent else Color.White.copy(alpha = if (state == 0) 0.5f else 0.25f)),
                            )
                        }
                        Text(
                            line.text.ifBlank { "♪" },
                            color = Color.White.copy(alpha = if (state == 2) 1f else if (state == 0) 0.55f else 0.4f),
                            fontSize = (if (state == 2) 24f else 18f).sp * SkinStyle.textScale, lineHeight = (if (state == 2) 30f else 24f).sp * SkinStyle.textScale,
                            fontWeight = if (state == 2) FontWeight.ExtraBold else FontWeight.SemiBold,
                            modifier = Modifier.weight(1f).padding(bottom = 16.dp),
                        )
                    }
                }
            }
        }
        Column(Modifier.padding(horizontal = 24.dp)) {
            if (SkinStyle.seek) SkinSeek(ui, accent, { vm.player.seekTo(it) })
            if (SkinStyle.controls) SkinControls(vm, ui, Modifier.padding(bottom = 10.dp), 56.dp)
        }
    }
}

// ---- Lyrics -----------------------------------------------------------------------------------------------------------

/** Only the words: the line being sung centred and large, the one before and after dim above and below it, small controls at the bottom. */
@Composable
private fun LyricsSkin(vm: MainViewModel, t: Track, ui: PlayerUi, onClose: () -> Unit) {
    val (lines, loaded) = rememberSkinLines(vm, t)
    val accent = rememberSkinAccent(vm)
    val pos by rememberUpdatedState(ui.positionMs)
    val idx by remember(lines) { derivedStateOf { currentIndex(lines, pos) } }
    Box(Modifier.fillMaxSize().background(skinBrush(Brush.radialGradient(listOf(accent.copy(alpha = 0.5f), Color.Black), center = Offset(300f, 300f), radius = 1400f)))) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 26.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                SkinBack(onClose)
                Spacer(Modifier.width(12.dp))
                if (SkinStyle.info) {
                Column(Modifier.weight(1f).padding(end = 52.dp)) {
                    Text(t.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(t.artist, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                }
            }
            Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                if (lines.isEmpty()) {
                    Text(
                        if (loaded) t.title else "Looking for lyrics…", color = Color.White, fontSize = 34.sp * SkinStyle.textScale, lineHeight = 40.sp * SkinStyle.textScale,
                        fontWeight = FontWeight.Black, textAlign = TextAlign.Center,
                    )
                    if (loaded) Text("No synced lyrics for this song", color = Color.White.copy(alpha = 0.45f), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                } else {
                    AnimatedContent(targetState = idx, transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(180)) }, label = "lyrics-skin") { i ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(lines.getOrNull(i - 1)?.text.orEmpty(), color = Color.White.copy(alpha = 0.3f), fontSize = 18.sp, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(18.dp))
                            Text(
                                (if (i >= 0) lines[i].text else "").ifBlank { "♪" }, color = Color.White, fontSize = 34.sp * SkinStyle.textScale, lineHeight = 40.sp * SkinStyle.textScale,
                                fontWeight = FontWeight.Black, textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(18.dp))
                            Text(lines.getOrNull(i + 1)?.text.orEmpty(), color = Color.White.copy(alpha = 0.3f), fontSize = 18.sp, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            if (SkinStyle.seek) SkinSeek(ui, accent, { vm.player.seekTo(it) })
            if (SkinStyle.controls) SkinControls(vm, ui, Modifier.padding(bottom = 10.dp), 52.dp)
        }
    }
}
