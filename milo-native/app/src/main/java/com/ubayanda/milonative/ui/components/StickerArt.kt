package com.ubayanda.milonative.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import kotlin.math.sin

/** The classic four-point sparkle: a star whose sides curve in towards the middle. */
fun DrawScope.drawSparkle(c: Offset, r: Float, color: Color) {
    val p = Path().apply {
        moveTo(c.x, c.y - r)
        quadraticTo(c.x, c.y, c.x + r, c.y)
        quadraticTo(c.x, c.y, c.x, c.y + r)
        quadraticTo(c.x, c.y, c.x - r, c.y)
        quadraticTo(c.x, c.y, c.x, c.y - r)
        close()
    }
    drawPath(p, color)
}

/** A flat orbit ring (two nested ellipses, tilted), like the ring stickers. */
fun DrawScope.drawOrbit(c: Offset, rx: Float, ry: Float, tiltDeg: Float, color: Color, stroke: Float) {
    rotate(tiltDeg, c) {
        drawOval(color, Offset(c.x - rx, c.y - ry), Size(rx * 2, ry * 2), style = Stroke(stroke))
        drawOval(color, Offset(c.x - rx * 0.62f, c.y - ry * 0.55f), Size(rx * 1.24f, ry * 1.1f), style = Stroke(stroke * 0.55f))
    }
}

/** A chequered flag strip, [rows] cells tall. */
fun DrawScope.drawChecker(topLeft: Offset, width: Float, cell: Float, rows: Int, color: Color) {
    val cols = (width / cell).toInt()
    for (r in 0 until rows) for (k in 0 until cols) if ((r + k) % 2 == 0) drawRect(color, Offset(topLeft.x + k * cell, topLeft.y + r * cell), Size(cell, cell))
}

/** An original doodle buddy: a round, wobbly head with two ears, tall oval eyes and a zigzag grin. */
fun DrawScope.drawBuddy(c: Offset, r: Float, wiggle: Float = 0f, fill: Color = Color.White, line: Color = Color.Black) {
    val w = r * 0.09f
    val ears = Path().apply {
        moveTo(c.x - r * 0.75f, c.y - r * 0.45f); lineTo(c.x - r * 0.95f + wiggle, c.y - r * 1.35f); lineTo(c.x - r * 0.25f, c.y - r * 0.85f)
        moveTo(c.x + r * 0.75f, c.y - r * 0.45f); lineTo(c.x + r * 0.95f + wiggle, c.y - r * 1.35f); lineTo(c.x + r * 0.25f, c.y - r * 0.85f)
    }
    drawPath(ears, fill); drawPath(ears, line, style = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawOval(fill, Offset(c.x - r, c.y - r * 0.85f), Size(r * 2, r * 1.75f))
    drawOval(line, Offset(c.x - r, c.y - r * 0.85f), Size(r * 2, r * 1.75f), style = Stroke(w))
    drawOval(line, Offset(c.x - r * 0.52f, c.y - r * 0.38f), Size(r * 0.24f, r * 0.42f))
    drawOval(line, Offset(c.x + r * 0.28f, c.y - r * 0.38f), Size(r * 0.24f, r * 0.42f))
    val grin = Path().apply {
        moveTo(c.x - r * 0.55f, c.y + r * 0.25f)
        for (i in 1..6) lineTo(c.x - r * 0.55f + r * 0.183f * i, c.y + r * (if (i % 2 == 1) 0.5f else 0.25f))
    }
    drawPath(grin, line, style = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** Sparkles, a ring and a chequered strip scattered over the whole screen, faint and still (no per-frame drawing). */
@Composable
fun StickerScatter(modifier: Modifier = Modifier, alpha: Float = 0.3f) {
    Canvas(modifier.fillMaxSize()) {
        val c = Color.White.copy(alpha = alpha)
        val u = size.minDimension / 100f
        val spots = listOf(
            Triple(0.10f, 0.07f, 7f), Triple(0.88f, 0.12f, 5f), Triple(0.93f, 0.33f, 3.2f), Triple(0.06f, 0.42f, 4f),
            Triple(0.80f, 0.58f, 6f), Triple(0.15f, 0.72f, 3.5f), Triple(0.55f, 0.88f, 4.5f), Triple(0.94f, 0.86f, 7f),
        )
        for ((fx, fy, s) in spots) drawSparkle(Offset(size.width * fx, size.height * fy), s * u, c)
        drawOrbit(Offset(size.width * 0.9f, size.height * 0.46f), 11 * u, 4 * u, -20f, c, 0.45.dp.toPx() * 3f)
        drawOrbit(Offset(size.width * 0.1f, size.height * 0.57f), 8 * u, 3 * u, 18f, c, 0.4.dp.toPx() * 3f)
        drawChecker(Offset(size.width * 0.58f, size.height * 0.015f), 22 * u, 2.2f * u, 2, c)
    }
}

/** A small sparkle in a panel's top-right corner. */
fun Modifier.sparkleCorner(): Modifier = this.drawBehind {
    drawSparkle(Offset(size.width - 22.dp.toPx(), 22.dp.toPx()), 9.dp.toPx(), Color.White.copy(alpha = 0.55f))
    drawSparkle(Offset(size.width - 40.dp.toPx(), 36.dp.toPx()), 4.dp.toPx(), Color.White.copy(alpha = 0.4f))
}

/** The doodle buddy on Now Playing: bobs and twitches its ears while music plays, sits still when paused. */
@Composable
fun BuddySticker(playing: Boolean, modifier: Modifier = Modifier) {
    var t by remember { mutableFloatStateOf(0f) }
    val calm = rememberCalm()
    val on by rememberUpdatedState(playing && !calm)
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            if (!on) { last = 0L; snapshotFlow { on }.first { it } }
            androidx.compose.runtime.withFrameNanos { n ->
                if (last != 0L) t += ((n - last) / 1_000_000_000f).coerceAtMost(0.1f)
                last = n
            }
        }
    }
    Canvas(modifier) {
        val r = size.width * 0.3f
        val bob = sin(t * 7f) * r * 0.08f
        drawSparkle(Offset(size.width * 0.85f, size.height * 0.2f + bob), r * 0.35f, Color.White)
        drawBuddy(Offset(size.width * 0.42f, size.height * 0.62f + bob), r, wiggle = sin(t * 9f) * r * 0.12f)
    }
}
