package com.ubayanda.milonative.ui.components

import android.graphics.Bitmap
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
    val bmp by rememberCover(vm, t, px)
    val shape = RoundedCornerShape(radius)
    Box(modifier.size(size).clip(shape).background(Milo.card2), contentAlignment = Alignment.Center) {
        val b = bmp
        if (b != null) {
            Image(remember(b) { b.asImageBitmap() }, null, Modifier.size(size), contentScale = ContentScale.Crop)
        } else {
            Icon(Icons.Rounded.MusicNote, null, tint = Milo.dim, modifier = Modifier.size(size / 2))
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

/** The song's colour taken from the small cover bitmap, or null when it is plain grey/black/white. */
fun glowOf(bmp: Bitmap): Color? {
    val px = IntArray(bmp.width * bmp.height)
    bmp.getPixels(px, 0, bmp.width, 0, 0, bmp.width, bmp.height)
    val dom = ColorMath.dominant(px)
    return if (dom < 0) null else Color(0xFF000000.toInt() or dom)
}

fun fade(c: Color, a: Float) = c.copy(alpha = a)

fun verticalFade(top: Color, bottom: Color) = Brush.verticalGradient(listOf(top, bottom))
