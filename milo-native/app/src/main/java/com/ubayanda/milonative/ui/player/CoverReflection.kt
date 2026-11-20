// from Mochy Music (GPL-3.0, github.com/realidkroo/Mochy-Music); the picture comes from milo's bitmap and is blurred here with Mochy's box blur.
// CoverReflection.kt
//
// The edge-to-edge cover mirrored under itself, heavily blurred and fading out downward, so the
// cover seems to spill into the moving backdrop. The picture is blurred once, small, off the main
// thread, and drawn stretched as one mesh whose vertex colours do the fade: no live blur and no
// offscreen layer, on every Android version.

package com.ubayanda.milonative.ui.player

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

// the picture is this small before it's blurred; stretched over the screen that's a soft, heavy blur for almost nothing
internal const val ReflectionPx = 96

// blur radius in those small pixels, roughly 95px on a phone screen
internal const val ReflectionBlurPx = 6

// how strong the mirror is where it meets the cover
private const val ReflectionStrength = 0.92f

// fully faded this far down its height, so it's gone well before the screen's bottom edge
private const val ReflectionFadeEnd = 0.9f

// rows in the mesh, spread over the whole picture; the fade is eased between them
private const val MeshRows = 32

// the cover's picture flipped under it; size this to start at the cover's bottom edge and as wide as the cover
@Composable
internal fun CoverReflection(
    source: Bitmap?,
    // the cover's height right now, px, so the mirror crops exactly as the cover does
    coverHeight: () -> Float,
    // how much the cover is zoomed about its centre (1.05 while playing)
    magnify: () -> Float,
    // 0 to 1, read while drawing, so fading it never needs a layer
    alpha: () -> Float,
    modifier: Modifier = Modifier,
    // the new flagship's: kept whole all the way down, the cover's own picture blurred under the controls
    steady: Boolean = false,
) {
    if (source == null) return
    val picture by produceState<Bitmap?>(null, source) {
        value = withContext(Dispatchers.Default) {
            runCatching { blurSmall(Bitmap.createScaledBitmap(source, ReflectionPx, ReflectionPx, true), ReflectionBlurPx) }.getOrNull()
        }
    }
    val mesh = remember { ReflectionMesh() }
    Spacer(
        modifier = modifier.drawBehind {
            val bitmap = picture ?: return@drawBehind
            val a = alpha().coerceIn(0f, 1f)
            if (a <= 0.004f) return@drawBehind
            mesh.draw(this, bitmap, coverHeight(), magnify(), a, steady)
        },
    )
}

// reused every frame: the vertices and their colours are only rewritten, never reallocated
private class ReflectionMesh {
    private val verts = FloatArray((MeshRows + 1) * 2 * 2)
    private val colors = IntArray((MeshRows + 1) * 2)
    private val paint = android.graphics.Paint().apply { isFilterBitmap = true }

    fun draw(scope: DrawScope, bitmap: Bitmap, coverHeight: Float, magnify: Float, alpha: Float, steady: Boolean): Unit = with(scope) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f || coverHeight <= 0f) return
        val bw = bitmap.width.toFloat()
        val bh = bitmap.height.toFloat()

        // the cover crops its picture to fill its box, so the mirror crops the same way, then zooms with it
        val fit = max(w / bw, coverHeight / bh)
        val srcLeft = (bw - w / fit) / 2f
        val srcBottom = (bh + coverHeight / fit) / 2f
        val scale = fit * magnify

        for (row in 0..MeshRows) {
            val v = bh * row / MeshRows
            // flipped: the cover's bottom row sits at the top, and rows further up the picture go further down
            val y = (srcBottom - v) * scale
            val fade = if (steady) alpha else fadeAt(y / h) * alpha
            val colour = android.graphics.Color.argb((fade * 255f).roundToInt().coerceIn(0, 255), 255, 255, 255)
            for (col in 0..1) {
                val u = bw * col
                val x = w / 2f + ((u - srcLeft) * fit - w / 2f) * magnify
                val i = row * 2 + col
                verts[i * 2] = x
                verts[i * 2 + 1] = y
                colors[i] = colour
            }
        }
        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            val saved = native.save()
            native.clipRect(0f, 0f, w, h)
            native.drawBitmapMesh(bitmap, 1, MeshRows, verts, 0, colors, 0, paint)
            native.restoreToCount(saved)
        }
    }

    // strong where it meets the cover, easing out to nothing toward the bottom
    private fun fadeAt(t: Float): Float {
        val x = (t.coerceIn(0f, 1f) / ReflectionFadeEnd).coerceIn(0f, 1f)
        val eased = x * x * (3f - 2f * x)
        return ReflectionStrength * (1f - eased)
    }
}

/** three passes of a box blur on a small bitmap (Mochy's BackdropBlurTransformation) */
internal fun blurSmall(input: Bitmap, radiusPx: Int): Bitmap {
    val width = input.width
    val height = input.height
    if (radiusPx <= 0 || width == 0 || height == 0) return input
    val pixels = IntArray(width * height)
    input.getPixels(pixels, 0, width, 0, 0, width, height)
    repeat(3) {
        boxBlurHorizontal(pixels, width, height, radiusPx)
        boxBlurVertical(pixels, width, height, radiusPx)
    }
    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    output.setPixels(pixels, 0, width, 0, 0, width, height)
    return output
}

private fun boxBlurHorizontal(pixels: IntArray, width: Int, height: Int, radius: Int) {
    val row = IntArray(width)
    for (y in 0 until height) {
        val rowStart = y * width
        for (x in 0 until width) row[x] = pixels[rowStart + x]
        for (x in 0 until width) {
            var a = 0; var r = 0; var g = 0; var b = 0; var count = 0
            val from = (x - radius).coerceAtLeast(0)
            val to = (x + radius).coerceAtMost(width - 1)
            for (i in from..to) {
                val p = row[i]
                a += (p ushr 24) and 0xFF
                r += (p ushr 16) and 0xFF
                g += (p ushr 8) and 0xFF
                b += p and 0xFF
                count++
            }
            pixels[rowStart + x] = ((a / count) shl 24) or ((r / count) shl 16) or ((g / count) shl 8) or (b / count)
        }
    }
}

private fun boxBlurVertical(pixels: IntArray, width: Int, height: Int, radius: Int) {
    val col = IntArray(height)
    for (x in 0 until width) {
        for (y in 0 until height) col[y] = pixels[y * width + x]
        for (y in 0 until height) {
            var a = 0; var r = 0; var g = 0; var b = 0; var count = 0
            val from = (y - radius).coerceAtLeast(0)
            val to = (y + radius).coerceAtMost(height - 1)
            for (i in from..to) {
                val p = col[i]
                a += (p ushr 24) and 0xFF
                r += (p ushr 16) and 0xFF
                g += (p ushr 8) and 0xFF
                b += p and 0xFF
                count++
            }
            pixels[y * width + x] = ((a / count) shl 24) or ((r / count) shl 16) or ((g / count) shl 8) or (b / count)
        }
    }
}
