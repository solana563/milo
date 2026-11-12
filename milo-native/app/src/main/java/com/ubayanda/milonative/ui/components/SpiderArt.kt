package com.ubayanda.milonative.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.ubayanda.milonative.ui.theme.Spider
import kotlinx.coroutines.flow.first
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private const val RAD = (Math.PI / 180.0).toFloat()

/**
 * A web fanning out from a corner: [spokes] threads between the two angles (degrees, 0 = right, 90 = down) and [rings]
 * sagging cross-threads, each one drawn as a curve that dips towards the corner between spokes.
 */
fun DrawScope.drawWeb(origin: Offset, radius: Float, fromDeg: Float, toDeg: Float, color: Color, spokes: Int = 6, rings: Int = 6, width: Float = 1f) {
    val step = (toDeg - fromDeg) / spokes
    fun at(deg: Float, r: Float) = Offset(origin.x + cos(deg * RAD) * r, origin.y + sin(deg * RAD) * r)
    for (s in 0..spokes) drawLine(color, origin, at(fromDeg + step * s, radius), width)
    for (k in 1..rings) {
        val r = radius * k / rings
        val p = Path()
        p.moveTo(at(fromDeg, r).x, at(fromDeg, r).y)
        for (s in 0 until spokes) {
            val a = at(fromDeg + step * (s + 1), r)
            val c = at(fromDeg + step * (s + 0.5f), r * 0.86f)
            p.quadraticTo(c.x, c.y, a.x, a.y)
        }
        drawPath(p, color, style = Stroke(width, cap = StrokeCap.Round))
    }
}

/** A spider seen from above: round abdomen with a red hourglass, small head, eight jointed legs that twitch with [phase]. */
fun DrawScope.drawSpider(c: Offset, r: Float, body: Color = Color(0xFF0A0A0C), mark: Color = Spider.red, phase: Float = 0f) {
    val leg = Color(0xFF16161A)
    val w = r * 0.15f
    for (i in 0..3) for (side in intArrayOf(-1, 1)) {
        val y0 = (i - 1.5f) * r * 0.22f
        val start = Offset(c.x + side * r * 0.25f, c.y + y0 + r * 0.1f)
        val wig = sin(phase * 3f + i * 1.3f + (if (side > 0) 1.7f else 0f)) * r * 0.1f
        val knee = Offset(start.x + side * r * 1.05f, start.y + (i - 1.5f) * r * 0.55f - r * 0.55f)
        val foot = Offset(knee.x + side * r * 0.75f, knee.y + r * (0.95f + i * 0.12f) + wig)
        val p = Path().apply { moveTo(start.x, start.y); lineTo(knee.x, knee.y); lineTo(foot.x, foot.y) }
        drawPath(p, leg, style = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(p, Color.White.copy(alpha = 0.18f), style = Stroke(w * 0.25f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
    drawCircle(body, r * 0.5f, Offset(c.x, c.y + r * 0.38f))
    drawCircle(body, r * 0.3f, Offset(c.x, c.y - r * 0.28f))
    drawCircle(Color.White.copy(alpha = 0.16f), r * 0.18f, Offset(c.x - r * 0.18f, c.y + r * 0.2f))
    // hourglass mark
    val m = Path().apply {
        val cx = c.x; val cy = c.y + r * 0.42f; val h = r * 0.26f; val ww = r * 0.17f
        moveTo(cx - ww, cy - h); lineTo(cx + ww, cy - h); lineTo(cx, cy); close()
        moveTo(cx - ww, cy + h); lineTo(cx + ww, cy + h); lineTo(cx, cy); close()
    }
    drawPath(m, mark)
    drawCircle(mark.copy(alpha = 0.95f), r * 0.045f, Offset(c.x - r * 0.1f, c.y - r * 0.34f))
    drawCircle(mark.copy(alpha = 0.95f), r * 0.045f, Offset(c.x + r * 0.1f, c.y - r * 0.34f))
}

/** Webs in the top-left and bottom-right corners. Does not take touches. */
@Composable
fun WebCorners(modifier: Modifier = Modifier, alpha: Float = 0.2f) {
    Canvas(modifier.fillMaxSize()) {
        val r = min(size.width, size.height) * 0.62f
        val col = Spider.silk.copy(alpha = alpha)
        drawWeb(Offset(0f, 0f), r, 0f, 90f, col, spokes = 7, rings = 8, width = 1.1.dp.toPx())
        drawWeb(Offset(size.width, size.height), r * 0.8f, 180f, 270f, col.copy(alpha = alpha * 0.8f), spokes = 6, rings = 7, width = 1.dp.toPx())
    }
}

/** A small web tucked into a panel's top-right corner. */
fun Modifier.webCorner(): Modifier = this.drawBehind {
    val r = min(size.minDimension, 150.dp.toPx())
    drawWeb(Offset(size.width, 0f), r, 90f, 180f, Spider.silk.copy(alpha = 0.13f), spokes = 4, rings = 5, width = 1.dp.toPx())
}

/** The spider on Now Playing: it hangs from the top on a thread and rocks gently while the music plays. */
@Composable
fun HangingSpider(playing: Boolean, modifier: Modifier = Modifier) {
    var t by remember { mutableFloatStateOf(0f) }
    val on by rememberUpdatedState(playing)
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
        val x = size.width / 2f + sin(t * 1.1f) * size.width * 0.05f
        val drop = size.height * (0.55f + 0.12f * sin(t * 0.8f))
        drawLine(Spider.silk.copy(alpha = 0.55f), Offset(size.width / 2f, 0f), Offset(x, drop), 1.2.dp.toPx())
        drawSpider(Offset(x, drop + 14.dp.toPx()), 15.dp.toPx(), phase = t * 4f)
    }
}
