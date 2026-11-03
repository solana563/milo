package com.ubayanda.milonative.ui.components

import android.graphics.Bitmap
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import com.ubayanda.milonative.util.ColorMath
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.ubayanda.milonative.model.Track
import com.ubayanda.milonative.ui.MainViewModel
import com.ubayanda.milonative.ui.theme.Milo
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun rememberCover(vm: MainViewModel, t: Track?, px: Int = 480): State<Bitmap?> =
    produceState<Bitmap?>(null, t?.id, px) { value = if (t == null) null else vm.art.cover(t, px) }

@Composable
fun rememberSoft(vm: MainViewModel, t: Track?): State<Bitmap?> =
    produceState<Bitmap?>(null, t?.id) { value = if (t == null) null else vm.art.soft(t) }

/** Square cover with a quiet placeholder when the song has no art. */
@Composable
fun Cover(vm: MainViewModel, t: Track?, size: Dp, radius: Dp = 10.dp, px: Int = 360, modifier: Modifier = Modifier) {
    var done by remember(t?.id, px) { mutableStateOf(false) }
    val bmp by produceState<Bitmap?>(null, t?.id, px) { value = if (t == null) null else vm.art.cover(t, px); done = true }
    val shape = RoundedCornerShape(radius)
    Box(modifier.size(size).clip(shape).background(Milo.card2), contentAlignment = Alignment.Center) {
        val b = bmp
        if (b == null && !done && t != null) Box(Modifier.size(size).shimmer())
        androidx.compose.animation.Crossfade(targetState = b, animationSpec = androidx.compose.animation.core.tween(260), label = "cover") { x ->
            if (x != null) Image(remember(x) { x.asImageBitmap() }, null, Modifier.size(size), contentScale = ContentScale.Crop)
            else if (done || t == null) Icon(Icons.Rounded.MusicNote, null, tint = Milo.dim, modifier = Modifier.size(size / 2))
        }
    }
}

/**
 * Cards are a SOLID colour that follows the song: its dominant colour mixed into the dark card colour.
 * No art, no blur, no gradient, like the web cards.
 */
@Composable
fun Modifier.artBackdrop(bmp: Bitmap?, shade: Float = 0.42f): Modifier {
    val glow: Color? = remember(bmp) { bmp?.let { glowOf(it) } }
    if (glow == null) return this.background(Milo.card)
    val solid = androidx.compose.ui.graphics.lerp(Color(0xFF141312), glow, 0.30f)
    return this.background(solid)
}

/**
 * Album / playlist / artist header backdrop: the cover, softened and stretched across the card, fading into the page colour at the
 * bottom so the title stays readable. [bmp] is the small pre-blurred cover, so nothing needs a GPU blur effect.
 */
@Composable
fun Modifier.artWash(bmp: Bitmap?, strength: Float = 0.62f): Modifier {
    val img = remember(bmp) { bmp?.asImageBitmap() }
    if (img == null) return this.background(Milo.card)
    val glow = remember(bmp) { bmp?.let { glowOf(it) } }
    val base = if (glow == null) Color(0xFF141312) else androidx.compose.ui.graphics.lerp(Color(0xFF141312), glow, 0.22f)
    return this.background(base).drawBehind {
        drawImage(img, dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()), alpha = strength, filterQuality = FilterQuality.High)
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF141312).copy(alpha = 0.78f))))
    }
}

/** The song's colour taken from the small cover bitmap, or null when it is plain grey/black/white. */
fun glowOf(bmp: Bitmap): Color? {
    val px = IntArray(bmp.width * bmp.height)
    bmp.getPixels(px, 0, bmp.width, 0, 0, bmp.width, bmp.height)
    val dom = ColorMath.dominant(px)
    return if (dom < 0) null else Color(0xFF000000.toInt() or dom)
}

fun fade(c: Color, a: Float) = c.copy(alpha = a)

fun verticalFade(top: Color, bottom: Color) = Brush.verticalGradient(listOf(top, bottom))
