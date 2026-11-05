package com.ubayanda.milonative.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ubayanda.milonative.player.Spectrum

/** Visual styles: 0 bars with falling peak marks, 1 mirrored bars, 2 flowing wave. */
const val SPECTRUM_STYLES = 3

/** A live spectrum of the music. It reads the player's own audio (no microphone permission) and redraws every frame while on screen. */
@Composable
fun SpectrumView(accent: Color, modifier: Modifier = Modifier, height: Dp = 56.dp, style: Int = 0) {
    val an = remember { Spectrum.Analyzer() }
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(an) {
        while (true) {
            androidx.compose.runtime.withFrameNanos { }
            an.update()
            tick++
        }
    }
    Canvas(modifier.fillMaxWidth().height(height)) {
        @Suppress("UNUSED_EXPRESSION") tick   // reading it makes the canvas redraw each frame
        val n = Spectrum.BARS
        val bars = an.bars
        val w = size.width; val h = size.height
        when (style) {
            2 -> {
                // a smooth curve through the band heights, filled below, with a faint reflection
                fun y(i: Int) = h - (bars[i.coerceIn(0, n - 1)] * (h * 0.92f)).coerceAtLeast(2f)
                val step = w / (n - 1)
                val line = Path().apply {
                    moveTo(0f, y(0))
                    for (i in 1 until n) {
                        val x0 = (i - 1) * step; val x1 = i * step
                        val xm = (x0 + x1) / 2f
                        cubicTo(xm, y(i - 1), xm, y(i), x1, y(i))
                    }
                }
                val fill = Path().apply { addPath(line); lineTo(w, h); lineTo(0f, h); close() }
                drawPath(fill, Brush.verticalGradient(listOf(accent.copy(alpha = 0.55f), accent.copy(alpha = 0.04f)), startY = 0f, endY = h))
                drawPath(line, accent, style = Stroke(width = 3.2f))
            }
            else -> {
                val gap = w * 0.010f
                val bw = (w - gap * (n - 1)) / n
                for (i in 0 until n) {
                    val v = bars[i]
                    val x = i * (bw + gap)
                    if (style == 1) {
                        val bh = (v * h).coerceAtLeast(4f)
                        val top = (h - bh) / 2f
                        drawRoundRect(
                            Brush.verticalGradient(listOf(accent.copy(alpha = 0.35f), accent, accent.copy(alpha = 0.35f)), startY = top, endY = top + bh),
                            topLeft = Offset(x, top), size = Size(bw, bh), cornerRadius = CornerRadius(bw / 2f),
                        )
                    } else {
                        val bh = (v * h).coerceAtLeast(3f)
                        val top = h - bh
                        drawRoundRect(
                            Brush.verticalGradient(listOf(accent, accent.copy(alpha = 0.30f)), startY = top, endY = h),
                            topLeft = Offset(x, top), size = Size(bw, bh), cornerRadius = CornerRadius(bw / 2f),
                        )
                        val py = h - (an.peaks[i] * h).coerceAtLeast(3f) - 5f
                        if (an.peaks[i] > 0.04f) drawRoundRect(accent.copy(alpha = 0.9f), Offset(x, py), Size(bw, 3f), CornerRadius(1.5f))
                    }
                }
            }
        }
    }
}
