package com.ubayanda.milonative.ui.player

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** which cover style the player wears and its record options (prefs "milo_fx": cover_style, disc_rotate, disc_real, disc_name) */
object CoverLook {
    data class State(val style: PlayerCoverStyle = PlayerCoverStyle.EDGE_TO_EDGE, val rotate: Boolean = true, val realistic: Boolean = true, val name: String = "")

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()
    private var loaded = false

    private fun prefs(c: Context) = c.applicationContext.getSharedPreferences("milo_fx", Context.MODE_PRIVATE)

    fun init(c: Context) {
        if (loaded) return
        loaded = true
        val p = prefs(c)
        val styles = PlayerCoverStyle.values()
        _state.value = State(
            style = styles.getOrElse(p.getInt("cover_style", 0)) { PlayerCoverStyle.EDGE_TO_EDGE },
            rotate = p.getBoolean("disc_rotate", true),
            realistic = p.getBoolean("disc_real", true),
            name = p.getString("disc_name", "") ?: "",
        )
    }

    fun set(c: Context, s: State) {
        _state.value = s
        prefs(c).edit().putInt("cover_style", s.style.ordinal).putBoolean("disc_rotate", s.rotate).putBoolean("disc_real", s.realistic).putString("disc_name", s.name).apply()
    }
}

/**
 * The player's cover in one of Mochy's styles: the cover softened across the whole screen, with the cover itself as a rounded square or a record
 * (small, big running off the edge, with a tonearm, or next to the album). The screen's usual controls sit over it as before.
 */
@Composable
fun CoverStage(
    bmp: Bitmap, look: CoverLook.State, playing: Boolean, modifier: Modifier = Modifier,
    /** for the Turntable's arm, which follows the song and is its seek bar */
    positionProvider: () -> Long = { 0L }, durationProvider: () -> Long = { 0L },
    trackKey: Any? = null, onSeek: (Long) -> Unit = {},
) {
    val art = remember(bmp) { bmp.asImageBitmap() }
    var angle by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(playing, look.rotate) {
        if (playing && look.rotate) {
            var last = androidx.compose.runtime.withFrameNanos { it }
            while (true) {
                val now = androidx.compose.runtime.withFrameNanos { it }
                angle = (angle + (now - last) / 1_000_000_000f * 24f) % 360f
                last = now
            }
        }
    }
    val colour by animateFloatAsState(if (playing) 1f else 0.5f, label = "discColour")
    val lift by animateFloatAsState(if (playing) 0f else 1f, label = "tonearm")

    Box(modifier.fillMaxSize()) {
        Image(art, null, Modifier.fillMaxSize().blur(36.dp), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.38f)))
        val stage = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 64.dp).fillMaxWidth()
        when {
            look.style == PlayerCoverStyle.SQUARED -> androidx.compose.foundation.layout.Column(stage.padding(horizontal = 30.dp)) {
                var coverPx by remember { mutableFloatStateOf(0f) }
                Image(
                    art, null,
                    Modifier.fillMaxWidth().aspectRatio(1f).onSizeChanged { coverPx = it.height.toFloat() }.shadow(28.dp, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp)),
                    contentScale = ContentScale.Crop, filterQuality = FilterQuality.High,
                )
                // the cover mirrored under itself, blurred, spilling into the backdrop (Mochy's cover reflection)
                CoverReflection(bmp, coverHeight = { coverPx }, magnify = { 1f }, alpha = { 0.55f }, modifier = Modifier.fillMaxWidth().aspectRatio(1f))
            }
            look.style == PlayerCoverStyle.TURNTABLE -> {
                // the record runs off the left edge with its arm reaching in from the top right; the slab goes under it
                Box(Modifier.align(Alignment.TopCenter).statusBarsPadding().fillMaxWidth().aspectRatio(0.72f)) {
                    TurntableBase(Modifier.fillMaxSize())
                    DiscCover(
                        style = look.style, artworkUrl = art, discName = look.name, realistic = look.realistic, editMode = false,
                        rotationProvider = { angle }, modifier = Modifier.fillMaxSize(), saturationProvider = { colour },
                    )
                    TurntableArm(
                        active = true, interactive = true, modifier = Modifier.fillMaxSize(),
                        positionProvider = positionProvider, durationProvider = durationProvider,
                        isPlaying = playing, trackKey = trackKey, onSeek = onSeek,
                    )
                }
            }
            else -> {
                val big = look.style.isBigDisc
                Box(stage.padding(horizontal = if (big) 0.dp else 26.dp).aspectRatio(1f)) {
                    DiscCover(
                        style = look.style,
                        artworkUrl = art,
                        discName = look.name,
                        realistic = look.realistic,
                        editMode = false,
                        rotationProvider = { angle },
                        modifier = Modifier.fillMaxSize(),
                        saturationProvider = { colour },
                    )
                    if (look.style.hasTonearm) DiscTonearm(liftProvider = { lift }, modifier = Modifier.fillMaxSize())
                }
            }
        }
        // the dark ramp the full-screen cover has: keeps the title and controls readable
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.7f))))
    }
}

@Composable
fun rememberCoverLook(): StateFlow<CoverLook.State> {
    val ctx = LocalContext.current
    remember { CoverLook.init(ctx); true }
    return CoverLook.state
}
