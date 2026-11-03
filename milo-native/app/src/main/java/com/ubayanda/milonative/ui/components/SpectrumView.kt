package com.ubayanda.milonative.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ubayanda.milonative.player.AudioFx
import com.ubayanda.milonative.player.Spectrum

/** Bars that move with the music. Starts the analyser while it is on screen and stops it when it leaves. */
@Composable
fun SpectrumView(accent: Color, modifier: Modifier = Modifier, height: Dp = 56.dp, mirrored: Boolean = false) {
    val bars by Spectrum.bars.collectAsState()
    DisposableEffect(Unit) {
        Spectrum.start(AudioFx.sessionId)
        onDispose { Spectrum.stop() }
    }
    Box(modifier.fillMaxWidth().height(height)) {
        Canvas(Modifier.fillMaxWidth().height(height)) {
            val n = bars.size
            val gap = size.width * 0.012f
            val w = (size.width - gap * (n - 1)) / n
            for (i in 0 until n) {
                val h = (bars[i] * size.height).coerceAtLeast(3f)
                val x = i * (w + gap)
                val top = if (mirrored) (size.height - h) / 2f else size.height - h
                drawRoundRect(
                    Brush.verticalGradient(listOf(accent, accent.copy(alpha = 0.35f)), startY = top, endY = top + h),
                    topLeft = Offset(x, top), size = Size(w, h), cornerRadius = CornerRadius(w / 2f),
                )
            }
        }
    }
}
