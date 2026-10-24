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
import androidx.compose.runtime.produceState
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
            Image(b.asImageBitmap(), null, Modifier.size(size), contentScale = ContentScale.Crop)
        } else {
            Icon(Icons.Rounded.MusicNote, null, tint = Milo.dim, modifier = Modifier.size(size / 2))
        }
    }
}

/**
 * The art-adaptive card backdrop: the tiny (36px) bitmap is stretched up with smooth filtering, which is a blur
 * without any blur effect, so it costs almost nothing. The stretch is always at screen scale, so every card shows the
 * same blur density whatever its height.
 */
@Composable
fun Modifier.artBackdrop(bmp: Bitmap?, shade: Float = 0.42f): Modifier {
    val screenH = with(LocalDensity.current) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    if (bmp == null) return this.background(Milo.card)
    val img = bmp.asImageBitmap()
    return this.drawBehind {
        drawImage(
            img,
            srcSize = IntSize(img.width, img.height),
            dstSize = IntSize(size.width.roundToInt(), max(size.height, screenH).roundToInt()),
            filterQuality = FilterQuality.High,
        )
        drawRect(Color.Black.copy(alpha = shade))
    }
}

fun fade(c: Color, a: Float) = c.copy(alpha = a)

fun verticalFade(top: Color, bottom: Color) = Brush.verticalGradient(listOf(top, bottom))
