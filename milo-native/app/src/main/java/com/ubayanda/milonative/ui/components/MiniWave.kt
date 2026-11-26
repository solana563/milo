package com.ubayanda.milonative.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ubayanda.milonative.player.Spectrum
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow

/**
 * The little "now playing" waveform you see in the iPhone's island: thin, fully rounded white bars on one centre line.
 *
 * It adapts to the music, so loud, bass-heavy songs (amapiano, house, trap) do not leave every bar stuck at full height:
 * a slow gain follows the loudness, the response is curved so quiet and loud parts look different, beats add a quick lift,
 * bars rise fast and fall slowly, and the middle bars are a little taller than the outer ones for the island shape.
 */
@Composable
fun MiniWave(modifier: Modifier = Modifier, height: Dp = 30.dp, color: Color = Color.White, bars: Int = 9) {
    val an = remember { Spectrum.Analyzer() }
    var tick by remember { mutableIntStateOf(0) }
    val shown = remember(bars) { FloatArray(bars) }    // what is drawn
    val avg = remember(bars) { FloatArray(bars) }      // slow average per bar, to spot beats
    val peak = remember { floatArrayOf(0.25f) }        // slow-moving "how loud is this song" reference
    LaunchedEffect(an) {
        while (true) {
            androidx.compose.runtime.withFrameNanos { }
            an.update()
            val n = Spectrum.BARS
            val src = an.bars
            val usable = (n * 0.75f).coerceAtLeast(bars.toFloat())
            val raw = FloatArray(bars)
            var top = 0f
            for (i in 0 until bars) {
                // bars cover the spectrum on a curve, so the bass gets a few bars and the rest share the mids and highs
                val from = (usable * (i.toFloat() / bars).pow(1.6f)).toInt().coerceIn(0, n - 1)
                val to = (usable * ((i + 1).toFloat() / bars).pow(1.6f)).toInt().coerceIn(from + 1, n)
                var sum = 0f
                for (k in from until to) sum += src[k]
                raw[i] = sum / (to - from)
                if (raw[i] > top) top = raw[i]
            }
            // loudness reference: rises quickly, relaxes slowly (so a loud drop does not pin every bar at the top)
            peak[0] = if (top > peak[0]) peak[0] + (top - peak[0]) * 0.25f else peak[0] * 0.997f + top * 0.003f
            val ref = peak[0].coerceAtLeast(0.12f)
            for (i in 0 until bars) {
                val level = (raw[i] / ref).coerceIn(0f, 1f).pow(1.7f)                 // curve: mid values stay mid
                val beat = ((raw[i] - avg[i]) / ref).coerceAtLeast(0f) * 0.9f           // quick lift on a hit
                avg[i] += (raw[i] - avg[i]) * 0.05f
                val shape = 0.62f + 0.38f * cos(((i - (bars - 1) / 2f) / ((bars - 1) / 2f)) * (PI / 2)).toFloat()   // middle taller
                val target = ((level * 0.75f + beat) * shape).coerceIn(0f, 1f)
                val k = if (target > shown[i]) 0.55f else 0.14f                        // fast up, slow down
                shown[i] += (target - shown[i]) * k
            }
            tick++
        }
    }
    val bw = height * 0.115f
    val gap = height * 0.15f
    Canvas(modifier.width(bw * bars + gap * (bars - 1)).height(height)) {
        @Suppress("UNUSED_EXPRESSION") tick
        val w = size.width / (bars + (bars - 1) * (gap / bw))   // bar width in px
        val g = w * (gap / bw)
        val h = size.height
        val maxH = h * 0.88f                                    // never touches the top and bottom: room to breathe
        for (i in 0 until bars) {
            val bh = (w + shown[i] * (maxH - w)).coerceAtMost(maxH)
            drawRoundRect(color, topLeft = Offset(i * (w + g), (h - bh) / 2f), size = Size(w, bh), cornerRadius = CornerRadius(w / 2f))
        }
    }
}
