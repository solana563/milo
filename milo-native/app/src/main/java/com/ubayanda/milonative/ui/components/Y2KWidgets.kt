package com.ubayanda.milonative.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ubayanda.milonative.ui.theme.Y2K
import kotlinx.coroutines.flow.first
import kotlin.math.exp

/** The cover as a shiny CD: it spins up when music plays and winds down when paused, with an iridescent sheen and a hub. */
@Composable
fun SpinningCd(art: ImageBitmap?, playing: Boolean, modifier: Modifier = Modifier) {
    var angle by remember { mutableFloatStateOf(0f) }
    val spin by rememberUpdatedState(playing)
    LaunchedEffect(Unit) {
        var last = 0L
        var speed = 0f // degrees per second; eases up and down like a real disc
        while (true) {
            if (!spin && speed < 0.3f) {
                // stopped: stop drawing frames until the music plays again
                speed = 0f; last = 0L
                snapshotFlow { spin }.first { it }
            }
            androidx.compose.runtime.withFrameNanos { t ->
                if (last != 0L) {
                    val dt = ((t - last) / 1_000_000_000f).coerceAtMost(0.1f)
                    speed += ((if (spin) 80f else 0f) - speed) * (1f - exp(-2.6f * dt))
                    angle = (angle + speed * dt) % 360f
                }
                last = t
            }
        }
    }
    Box(modifier.aspectRatio(1f).clip(CircleShape).border(2.dp, Brush.linearGradient(listOf(Color.White, Y2K.aqua.copy(alpha = 0.5f), Color.White)), CircleShape)) {
        Box(Modifier.fillMaxSize().graphicsLayer { rotationZ = angle }) {
            if (art != null) Image(art, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Box(Modifier.fillMaxSize().background(Y2K.navy))
            Canvas(Modifier.fillMaxSize()) {
                val r = size.minDimension / 2f
                // rainbow sheen like the data side of a real CD
                drawCircle(Brush.sweepGradient(Y2K.rainbow.map { it.copy(alpha = 0.26f) }, Offset(size.width / 2f, size.height / 2f)), r)
                for (k in 1..5) drawCircle(Color.Black.copy(alpha = 0.10f), radius = r * (0.34f + 0.12f * k), style = Stroke(1f))
                // clear ring and hub
                drawCircle(Color.White.copy(alpha = 0.55f), r * 0.27f)
                drawCircle(Color.White.copy(alpha = 0.9f), r * 0.20f, style = Stroke(r * 0.03f))
                drawCircle(Color(0xFF031420), r * 0.09f)
            }
        }
        // a fixed highlight, so the disc catches the light while it turns underneath
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Brush.linearGradient(0f to Color.White.copy(alpha = 0.38f), 0.42f to Color.Transparent, 1f to Color.White.copy(alpha = 0.08f)), size.minDimension / 2f)
        }
    }
}

/** A backlit LCD strip like an old portable CD player: time, a quality tag, the title and the artist, with faint scan lines. */
@Composable
fun LcdStrip(title: String, artist: String, time: String, tag: String, playing: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier.fillMaxWidth().clip(shape)
            .background(Brush.verticalGradient(listOf(Y2K.lcdBg1, Y2K.lcdBg2)), shape)
            .border(1.dp, Y2K.aqua.copy(alpha = 0.45f), shape)
            .drawWithContent {
                drawContent()
                var y = 0f
                while (y < size.height) { drawLine(Color.Black.copy(alpha = 0.16f), Offset(0f, y), Offset(size.width, y), 1f); y += 3.dp.toPx() }
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text((if (playing) "▶ " else "❚❚ ") + time, color = Y2K.lcdGlow, style = Y2K.lcd)
            Text(tag.ifEmpty { "STEREO" }.uppercase(), color = Y2K.lcdGlow.copy(alpha = 0.7f), style = Y2K.lcd, maxLines = 1)
        }
        Text(title.uppercase(), color = Y2K.lcdGlow, style = Y2K.lcd, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
        Text(artist.uppercase(), color = Y2K.lcdGlow.copy(alpha = 0.7f), style = Y2K.lcd, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
