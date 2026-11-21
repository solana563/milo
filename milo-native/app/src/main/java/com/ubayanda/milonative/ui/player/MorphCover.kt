// Ported from Mochy Music's MorphingCover (GPL-3.0, github.com/realidkroo/Mochy-Music): one sheet progress p (0 = the mini player,
// 1 = the open player) moves the cover, the song info and the sheet's own backdrop between the two, with Mochy's curves
// (coverGrowth / coverTravel / smoothstep). Driven by the open spring, by dragging the player down, and by closing it, so the
// player opens out of the mini player and folds back into it. The ends are measured rects, so it lands exactly on the mini
// player's art, title, play / next buttons and bar. The cover also flies into the lyrics page's header as it rises. Not ported:
// Mochy's video / canvas layers and the reflection layout.
package com.ubayanda.milonative.ui.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import com.ubayanda.milonative.ui.theme.Milo
import com.ubayanda.milonative.ui.theme.MiloIcons
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ubayanda.milonative.ui.theme.Rad
import com.ubayanda.milonative.ui.theme.Ty
import kotlin.math.roundToInt

/** Where the mini player's art and title sit right now (window coordinates), reported by MiniPlayer. */
object MorphTargets {
    var miniArt by mutableStateOf<Rect?>(null)
    var miniTitle by mutableStateOf<Rect?>(null)
    /** the whole mini player bar: the open player's sheet grows out of it */
    var miniBar by mutableStateOf<Rect?>(null)
    var miniPlay by mutableStateOf<Rect?>(null)
    var miniSkip by mutableStateOf<Rect?>(null)
    /** the open player's cover box, and where the cover lands in the lyrics page's header (page offset removed) */
    var fullArt by mutableStateOf<Rect?>(null)
    var lyricsArt by mutableStateOf<Rect?>(null)
}

private fun mix(a: Float, b: Float, f: Float) = a + (b - a) * f
internal fun smooth(t: Float): Float { val x = t.coerceIn(0f, 1f); return x * x * (3f - 2f * x) }

// a late-growing cover (every style but edge-to-edge) keeps its mini size until the sheet is this far open
private const val LateGrowFrom = 0.7f
private fun coverGrowth(p: Float, growLate: Boolean) = if (growLate) smooth((p - LateGrowFrom) / (1f - LateGrowFrom)) else p
private fun coverTravel(p: Float, growLate: Boolean) = if (growLate) 1f - (1f - p) * (1f - p) else p

/** sheet progress from the open spring (0..1) and the downward drag in px: 1 open, 0 folded into the mini player */
fun sheetProgress(open: Float, dragPx: Float): Float = minOf(open, 1f - (dragPx / 320f).coerceIn(0f, 1f))

/**
 * The sheet's own backdrop: it grows out of the mini player's bar (its top edge, sides and corners) to the whole screen.
 * [progress] is read in the layer, so it never recomposes.
 */
fun Modifier.sheetReveal(progress: () -> Float, screenW: Float, screenH: Float, openRadius: Float): Modifier =
    this.graphicsLayer {
        val p = progress()
        if (p >= 0.999f) { clip = false; return@graphicsLayer }
        val bar = MorphTargets.miniBar
        val e = smooth(p)
        val top = mix(bar?.top ?: screenH, 0f, e)
        val left = mix(bar?.left ?: 0f, 0f, e)
        val right = mix(bar?.right ?: screenW, screenW, e)
        val bottom = mix(bar?.bottom ?: screenH, screenH, e)
        val r = mix(bar?.let { 36f * density } ?: 0f, openRadius, e)
        shape = GenericShape { _, _ -> addRoundRect(RoundRect(left, top, right, bottom, CornerRadius(r, r))) }
        clip = true
    }

/**
 * Draws in screen space (the caller cancels the player's own offset and scale), over the player.
 * [fromArt] / [fromTitle] are the open player's cover and title rects measured at rest; [growLate] is every cover style but edge to edge.
 */
@Composable
fun CoverMorphOverlay(
    cover: ImageBitmap?,
    title: String,
    artist: String,
    fromArt: Rect?,
    fromTitle: Rect?,
    progress: Float,
    growLate: Boolean = false,
    isPlaying: Boolean = false,
    fromPlay: Rect? = null,
    fromNext: Rect? = null,
) {
    val toArt = MorphTargets.miniArt
    val p = progress.coerceIn(0f, 1f)
    if (cover == null || fromArt == null || toArt == null || p >= 0.995f) return
    val d = LocalDensity.current
    val grow = coverGrowth(p, growLate)
    val travel = coverTravel(p, growLate)
    val w = mix(toArt.width, fromArt.width, grow)
    val h = mix(toArt.height, fromArt.height, grow)
    val cx = mix(toArt.center.x, fromArt.center.x, travel)
    val cy = mix(toArt.center.y, fromArt.center.y, travel)
    val l = cx - w / 2f
    val t = cy - h / 2f
    // the real cover is drawn under this one: hand over as it lands
    val handOver = smooth((0.995f - p) / 0.06f)
    val e = grow
    val topR = Rad.sm * (1f - e) + (if (growLate) 22.dp else 0.dp) * e
    val botR = Rad.sm * (1f - e) + (if (growLate) 22.dp else Rad.xl) * e
    Box(Modifier.fillMaxSize().graphicsLayer { alpha = handOver }) {
        Image(
            cover, null, contentScale = ContentScale.Crop,
            modifier = Modifier
                .offset { IntOffset(l.roundToInt(), t.roundToInt()) }
                .then(with(d) { Modifier.width(w.toDp()).height(h.toDp()) })
                .clip(RoundedCornerShape(topStart = topR, topEnd = topR, bottomStart = botR, bottomEnd = botR)),
        )
        val toTitle = MorphTargets.miniTitle
        if (fromTitle != null && toTitle != null) {
            val tl = mix(toTitle.left, fromTitle.left, p)
            val tt = mix(toTitle.top, fromTitle.top, p)
            // the title is the real player's own text once it's open: this one carries it over and fades out
            val ta = 1f - smooth((p - 0.2f) / 0.5f)
            if (ta > 0.01f) {
                Column(
                    Modifier
                        .offset { IntOffset(tl.roundToInt(), tt.roundToInt()) }
                        .then(with(d) { Modifier.width(mix(toTitle.width, fromTitle.width, p).toDp()) })
                        .graphicsLayer {
                            alpha = ta
                            val sc = mix(0.62f, 1f, p)
                            scaleX = sc; scaleY = sc
                            transformOrigin = TransformOrigin(0f, 0f)
                        },
                ) {
                    Text(title, color = Color.White, style = Ty.display, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(artist, color = Color(0xFFEDE8DC), style = Ty.heading, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        // the play and next buttons travel between the mini player's and the open player's
        val toPlay = MorphTargets.miniPlay
        if (fromPlay != null && toPlay != null) {
            val r = lerpRect(toPlay, fromPlay, p)
            FlyingButton(r, Milo.cream, if (isPlaying) MiloIcons.Pause else MiloIcons.PlayArrow, Color.Black)
        }
        val toSkip = MorphTargets.miniSkip
        if (fromNext != null && toSkip != null) {
            val r = lerpRect(toSkip, fromNext, p)
            FlyingButton(r, Milo.card2.copy(alpha = p), MiloIcons.SkipNext, Milo.text)
        }
    }
}

private fun lerpRect(a: Rect, b: Rect, f: Float) = Rect(mix(a.left, b.left, f), mix(a.top, b.top, f), mix(a.right, b.right, f), mix(a.bottom, b.bottom, f))

@Composable
private fun FlyingButton(r: Rect, bg: Color, icon: ImageVector, tint: Color) {
    val d = LocalDensity.current
    Box(
        Modifier
            .offset { IntOffset(r.left.roundToInt(), r.top.roundToInt()) }
            .then(with(d) { Modifier.size(r.width.toDp(), r.height.toDp()) })
            .clip(CircleShape).background(bg),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = tint, modifier = with(d) { Modifier.size((minOf(r.width, r.height) * 0.56f).toDp()) }) }
}

/**
 * The cover on its way into the lyrics page's header while the page rises ([lp] 0 = closed, 1 = open), drawn over the page.
 * Mochy's cover shrinks into the lyrics header the same way.
 */
@Composable
fun LyricsCoverFlight(cover: ImageBitmap?, fromArt: Rect?, lp: Float, growLate: Boolean) {
    val to = MorphTargets.lyricsArt
    val f = lp.coerceIn(0f, 1f)
    if (cover == null || fromArt == null || to == null || f <= 0.002f) return
    val d = LocalDensity.current
    val r = lerpRect(fromArt, to, f)
    val topR = (if (growLate) 22.dp else 0.dp) * (1f - f) + 22.dp * f
    val botR = (if (growLate) 22.dp else Rad.xl) * (1f - f) + 22.dp * f
    Image(
        cover, null, contentScale = ContentScale.Crop,
        modifier = Modifier
            .offset { IntOffset(r.left.roundToInt(), r.top.roundToInt()) }
            .then(with(d) { Modifier.width(r.width.toDp()).height(r.height.toDp()) })
            .clip(RoundedCornerShape(topStart = topR, topEnd = topR, bottomStart = botR, bottomEnd = botR)),
    )
}
