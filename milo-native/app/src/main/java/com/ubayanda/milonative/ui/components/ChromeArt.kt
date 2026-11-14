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
import androidx.compose.ui.graphics.Brush
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

private val steel = Color(0xFF8E959E)
private val dark = Color(0xFF30343B)
private val bright = Color(0xFFF5F7FA)

/** Polished metal: sharp alternating light and dark bands, the way chrome reflects its surroundings. */
fun chromeBrush(a: Offset, b: Offset): Brush = Brush.linearGradient(
    0f to Color.White, 0.2f to steel, 0.38f to bright, 0.56f to dark, 0.78f to Color(0xFFD3D7DD), 1f to Color.White,
    start = a, end = b,
)

/** A four-point sparkle in chrome, with thin bright lines along its spikes. */
fun DrawScope.drawChromeSparkle(c: Offset, r: Float, alpha: Float = 1f) {
    val p = Path().apply {
        moveTo(c.x, c.y - r)
        quadraticTo(c.x, c.y, c.x + r, c.y)
        quadraticTo(c.x, c.y, c.x, c.y + r)
        quadraticTo(c.x, c.y, c.x - r, c.y)
        quadraticTo(c.x, c.y, c.x, c.y - r)
        close()
    }
    drawPath(p, chromeBrush(Offset(c.x - r, c.y - r), Offset(c.x + r, c.y + r)), alpha = alpha)
    drawPath(p, Color(0xFF14161A).copy(alpha = 0.7f * alpha), style = Stroke(r * 0.03f, join = StrokeJoin.Round))
    val hl = Color.White.copy(alpha = 0.85f * alpha)
    drawLine(hl, Offset(c.x, c.y - r * 0.92f), Offset(c.x, c.y + r * 0.92f), r * 0.035f, StrokeCap.Round)
    drawLine(hl, Offset(c.x - r * 0.92f, c.y), Offset(c.x + r * 0.92f, c.y), r * 0.035f, StrokeCap.Round)
}

/** A chrome ring seen at an angle: a thick metal band with a thin highlight on top. */
fun DrawScope.drawChromeOrbit(c: Offset, rx: Float, ry: Float, tiltDeg: Float, band: Float, alpha: Float = 1f) {
    rotate(tiltDeg, c) {
        val tl = Offset(c.x - rx, c.y - ry); val sz = Size(rx * 2, ry * 2)
        drawOval(chromeBrush(tl, Offset(c.x + rx, c.y + ry)), tl, sz, alpha = alpha, style = Stroke(band))
        drawOval(Color.White.copy(alpha = 0.7f * alpha), Offset(tl.x, tl.y - band * 0.12f), sz, style = Stroke(band * 0.14f))
    }
}

/** A glossy chrome ball: bright upper-left, dark lower-right, a white specular dot and a thin dark rim. */
fun DrawScope.drawChromeBall(c: Offset, r: Float, alpha: Float = 1f) {
    drawCircle(
        Brush.radialGradient(0f to Color.White, 0.35f to Color(0xFFB3B9C1), 0.75f to Color(0xFF2B2E35), 1f to Color(0xFF6A707A), center = Offset(c.x - r * 0.35f, c.y - r * 0.4f), radius = r * 1.5f),
        r, c, alpha = alpha,
    )
    drawCircle(Color(0xFF101114).copy(alpha = 0.8f * alpha), r, c, style = Stroke(r * 0.07f))
    drawCircle(Color.White.copy(alpha = alpha), r * 0.14f, Offset(c.x - r * 0.38f, c.y - r * 0.42f))
}

/** A puffy chrome heart with a glossy highlight. */
fun DrawScope.drawChromeHeart(c: Offset, r: Float, alpha: Float = 1f) {
    val p = Path().apply {
        moveTo(c.x, c.y + r * 0.9f)
        cubicTo(c.x - r * 1.7f, c.y - r * 0.1f, c.x - r * 0.95f, c.y - r * 1.15f, c.x, c.y - r * 0.35f)
        cubicTo(c.x + r * 0.95f, c.y - r * 1.15f, c.x + r * 1.7f, c.y - r * 0.1f, c.x, c.y + r * 0.9f)
        close()
    }
    drawPath(p, chromeBrush(Offset(c.x - r, c.y - r), Offset(c.x + r, c.y + r)), alpha = alpha)
    drawPath(p, Color(0xFF101114).copy(alpha = 0.85f * alpha), style = Stroke(r * 0.07f, join = StrokeJoin.Round))
    rotate(-28f, Offset(c.x - r * 0.55f, c.y - r * 0.5f)) {
        drawOval(Color.White.copy(alpha = 0.75f * alpha), Offset(c.x - r * 0.85f, c.y - r * 0.62f), Size(r * 0.6f, r * 0.24f))
    }
}

/** A chrome planet: a ball with a tilted ring that passes in front and behind. */
fun DrawScope.drawChromePlanet(c: Offset, r: Float, alpha: Float = 1f) {
    val rx = r * 1.75f; val ry = r * 0.5f; val band = r * 0.16f
    val tl = Offset(c.x - rx, c.y - ry); val sz = Size(rx * 2, ry * 2)
    rotate(-18f, c) { drawOval(chromeBrush(tl, Offset(c.x + rx, c.y + ry)), tl, sz, alpha = alpha, style = Stroke(band)) }
    drawChromeBall(c, r, alpha)
    rotate(-18f, c) { drawArc(chromeBrush(tl, Offset(c.x + rx, c.y + ry)), 0f, 180f, false, tl, sz, alpha = alpha, style = Stroke(band)) }
}

/** Chrome shapes scattered over the whole screen, still and faint. */
@Composable
fun ChromeScatter(modifier: Modifier = Modifier, alpha: Float = 0.5f) {
    Canvas(modifier.fillMaxSize()) {
        val u = size.minDimension / 100f
        val w = size.width; val h = size.height
        drawChromeSparkle(Offset(w * 0.1f, h * 0.08f), 8 * u, alpha)
        drawChromeSparkle(Offset(w * 0.2f, h * 0.13f), 3 * u, alpha)
        drawChromeSparkle(Offset(w * 0.9f, h * 0.2f), 5.5f * u, alpha)
        drawChromeSparkle(Offset(w * 0.06f, h * 0.46f), 4 * u, alpha)
        drawChromeSparkle(Offset(w * 0.93f, h * 0.62f), 7 * u, alpha)
        drawChromeSparkle(Offset(w * 0.5f, h * 0.9f), 4.5f * u, alpha)
        drawChromeOrbit(Offset(w * 0.88f, h * 0.4f), 10 * u, 3.5f * u, -22f, 1.4f * u, alpha)
        drawChromeOrbit(Offset(w * 0.12f, h * 0.7f), 9 * u, 3 * u, 20f, 1.2f * u, alpha)
        drawChromeHeart(Offset(w * 0.84f, h * 0.84f), 5 * u, alpha)
        drawChromePlanet(Offset(w * 0.14f, h * 0.9f), 3.6f * u, alpha)
    }
}

/** A pair of small chrome sparkles in a panel's top-right corner. */
fun Modifier.chromeCorner(): Modifier = this.drawBehind {
    drawChromeSparkle(Offset(size.width - 22.dp.toPx(), 22.dp.toPx()), 10.dp.toPx(), 0.9f)
    drawChromeSparkle(Offset(size.width - 42.dp.toPx(), 38.dp.toPx()), 4.5.dp.toPx(), 0.8f)
}

/** Now Playing: a chrome planet bobbing in the corner and sparkles that pulse while the music plays. */
@Composable
fun ChromeFloat(playing: Boolean, modifier: Modifier = Modifier) {
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
        val r = size.width * 0.2f
        drawChromePlanet(Offset(size.width * 0.4f, size.height * 0.5f + sin(t * 1.5f) * r * 0.12f), r)
        val k1 = 0.8f + 0.2f * sin(t * 4f); val k2 = 0.8f + 0.2f * sin(t * 5f + 2f)
        drawChromeSparkle(Offset(size.width * 0.86f, size.height * 0.2f), r * 0.55f * k1)
        drawChromeSparkle(Offset(size.width * 0.9f, size.height * 0.78f), r * 0.35f * k2)
    }
}
