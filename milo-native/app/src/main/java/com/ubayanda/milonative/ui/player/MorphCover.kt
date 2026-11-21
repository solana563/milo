// Idea from Mochy Music's MorphingCover (GPL-3.0, github.com/realidkroo/Mochy-Music): while the player folds away,
// the cover and the song info travel to the mini player's own slots instead of the whole page just shrinking.
// Mochy's version runs its whole bottom sheet (2k lines: mini pill, backdrops, canvas); this is the same
// idea for milo's drag-down, driven by measured rects so it lands exactly on the mini player's art and title.
package com.ubayanda.milonative.ui.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
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
}

private fun mix(a: Float, b: Float, f: Float) = a + (b - a) * f
private fun smooth(t: Float): Float { val x = t.coerceIn(0f, 1f); return x * x * (3f - 2f * x) }

/**
 * Draws in screen space (the caller cancels the player's own offset and scale), over the player.
 * [fromArt] / [fromTitle] are the full player's rects measured at rest.
 */
@Composable
fun CoverMorphOverlay(
    cover: ImageBitmap?,
    title: String,
    artist: String,
    fromArt: Rect?,
    fromTitle: Rect?,
    dragPx: Float,
) {
    val toArt = MorphTargets.miniArt
    if (cover == null || fromArt == null || toArt == null || dragPx <= 0f) return
    val e = smooth(dragPx / 320f)
    val d = LocalDensity.current
    val l = mix(fromArt.left, toArt.left, e)
    val t = mix(fromArt.top, toArt.top, e)
    val w = mix(fromArt.width, toArt.width, e)
    val h = mix(fromArt.height, toArt.height, e)
    val topR = Rad.sm * e
    val botR = Rad.xl * (1f - e) + Rad.sm * e
    Box(Modifier.fillMaxSize()) {
        Image(
            cover, null, contentScale = ContentScale.Crop,
            modifier = Modifier
                .offset { IntOffset(l.roundToInt(), t.roundToInt()) }
                .then(with(d) { Modifier.width(w.toDp()).height(h.toDp()) })
                .clip(RoundedCornerShape(topStart = topR, topEnd = topR, bottomStart = botR, bottomEnd = botR)),
        )
        val toTitle = MorphTargets.miniTitle
        if (fromTitle != null && toTitle != null) {
            val a = (1f - e) * 3f
            if (a > 0f) {
                val tl = mix(fromTitle.left, toTitle.left, e)
                val tt = mix(fromTitle.top, toTitle.top, e)
                Column(
                    Modifier
                        .offset { IntOffset(tl.roundToInt(), tt.roundToInt()) }
                        .then(with(d) { Modifier.width(fromTitle.width.toDp()) })
                        .graphicsLayer {
                            alpha = a.coerceIn(0f, 1f)
                            val s = mix(1f, 0.62f, e)
                            scaleX = s; scaleY = s
                            transformOrigin = TransformOrigin(0f, 0f)
                        },
                ) {
                    Text(title, color = Color.White, style = Ty.display, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(artist, color = Color(0xFFEDE8DC), style = Ty.heading, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
