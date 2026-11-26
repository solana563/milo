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

/**
 * The little "now playing" waveform you see in the iPhone's island: a few thin, fully rounded white bars,
 * all centred on one line, each growing up and down with its slice of the music.
 */
@Composable
fun MiniWave(modifier: Modifier = Modifier, height: Dp = 30.dp, color: Color = Color.White, bars: Int = 7) {
    val an = remember { Spectrum.Analyzer() }
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(an) {
        while (true) {
            androidx.compose.runtime.withFrameNanos { }
            an.update()
            tick++
        }
    }
    val bw = height * 0.16f
    val gap = height * 0.11f
    Canvas(modifier.width(bw * bars + gap * (bars - 1)).height(height)) {
        @Suppress("UNUSED_EXPRESSION") tick
        val n = Spectrum.BARS
        val src = an.bars
        val w = bw.toPx(); val g = gap.toPx(); val h = size.height
        val usable = (n * 0.75f).toInt().coerceAtLeast(bars)   // the top of the spectrum is mostly empty
        for (i in 0 until bars) {
            val from = i * usable / bars
            val to = ((i + 1) * usable / bars).coerceAtLeast(from + 1).coerceAtMost(n)
            var sum = 0f
            for (k in from until to) sum += src[k]
            val v = (sum / (to - from) * 1.7f).coerceIn(0f, 1f)
            val bh = (w + v * (h - w)).coerceAtMost(h)
            drawRoundRect(color, topLeft = Offset(i * (w + g), (h - bh) / 2f), size = Size(w, bh), cornerRadius = CornerRadius(w / 2f))
        }
    }
}
