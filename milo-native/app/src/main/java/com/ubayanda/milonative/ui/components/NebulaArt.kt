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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.ubayanda.milonative.ui.theme.Nebula
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlin.math.PI
import kotlin.math.sin

private class Band(val y: Float, val amp: Float, val freq: Float, val speed: Float, val phase: Float, val width: Float, val color: Color)

private val bands = listOf(
    Band(0.12f, 0.05f, 2.2f, 0.5f, 0.0f, 90f, Color(0xFF0E8CA6).copy(alpha = 0.30f)),
    Band(0.22f, 0.06f, 1.7f, -0.4f, 1.3f, 110f, Color(0xFFB88BFF).copy(alpha = 0.26f)),
    Band(0.38f, 0.07f, 2.6f, 0.35f, 2.1f, 80f, Color(0xFF0E8CA6).copy(alpha = 0.26f)),
    Band(0.52f, 0.05f, 1.9f, -0.5f, 0.7f, 120f, Color(0xFFB88BFF).copy(alpha = 0.24f)),
    Band(0.68f, 0.07f, 2.3f, 0.45f, 3.0f, 95f, Color(0xFF0E8CA6).copy(alpha = 0.28f)),
    Band(0.82f, 0.06f, 1.6f, -0.35f, 1.9f, 115f, Color(0xFFB88BFF).copy(alpha = 0.28f)),
    Band(0.95f, 0.05f, 2.4f, 0.4f, 0.4f, 90f, Color(0xFF0E8CA6).copy(alpha = 0.22f)),
)

/** A curly cloud with a white outline: overlapping circles and a pointed tail, a few curls inside. */
fun DrawScope.drawSwirlCloud(c: Offset, s: Float, fill: Color, edge: Color = Color.White) {
    val lobes = listOf(Triple(-0.38f, 0.05f, 0.36f), Triple(0.0f, -0.17f, 0.48f), Triple(0.42f, 0.0f, 0.38f), Triple(0.08f, 0.17f, 0.36f), Triple(-0.12f, 0.2f, 0.3f))
    val tail = Path().apply { moveTo(c.x - s * 1.0f, c.y + s * 0.3f); lineTo(c.x - s * 0.45f, c.y - s * 0.02f); lineTo(c.x - s * 0.4f, c.y + s * 0.4f); close() }
    for (pass in 0..1) {
        val grow = if (pass == 0) s * 0.07f else 0f
        val col = if (pass == 0) edge else fill
        for ((dx, dy, r) in lobes) drawCircle(col, r * s + grow, Offset(c.x + dx * s, c.y + dy * s))
        drawRoundRect(col, Offset(c.x - s * 0.62f - grow, c.y + s * 0.12f - grow), Size(s * 1.3f + grow * 2, s * 0.3f + grow * 2), CornerRadius(s * 0.15f))
        drawPath(tail, col)
        if (pass == 0) drawPath(tail, col, style = Stroke(grow * 2f))
    }
    val curl = Stroke(s * 0.045f, cap = StrokeCap.Round)
    for ((dx, dy, r) in listOf(Triple(0.02f, -0.12f, 0.17f), Triple(-0.32f, 0.12f, 0.1f), Triple(0.42f, 0.02f, 0.12f))) {
        drawArc(edge, 200f, 250f, false, Offset(c.x + (dx - r) * s, c.y + (dy - r) * s), Size(r * 2 * s, r * 2 * s), style = curl)
    }
}

/** An astronaut helmet with a glowing cyan visor full of magenta glitter. */
fun DrawScope.drawHelmet(c: Offset, r: Float, glitter: Float = 0f) {
    drawCircle(Brush.radialGradient(listOf(Color(0xFF3B2A86), Color(0xFF150C3C)), Offset(c.x - r * 0.3f, c.y - r * 0.4f), r * 1.6f), r, c)
    drawCircle(Color(0xFF1D1250), r * 0.3f, Offset(c.x - r * 0.98f, c.y + r * 0.1f))
    drawRoundRect(Color(0xFF1D1250), Offset(c.x - r * 0.7f, c.y + r * 0.85f), Size(r * 1.4f, r * 0.45f), CornerRadius(r * 0.2f))
    val vt = Offset(c.x - r * 0.62f, c.y - r * 0.28f); val vs = Size(r * 1.55f, r * 0.82f); val cr = CornerRadius(r * 0.38f)
    drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFC300E6), Color(0xFF5E0FA8)), vt.y, vt.y + vs.height), vt, vs, cr)
    var seed = 7
    for (i in 0 until 26) {
        seed = (seed * 1103515245 + 12345) and 0x7fffffff
        val fx = (seed % 1000) / 1000f
        seed = (seed * 1103515245 + 12345) and 0x7fffffff
        val fy = (seed % 1000) / 1000f
        val a = 0.35f + 0.55f * sin(glitter * 2f + i).let { it * it }
        drawCircle(Color(0xFFFFB8F5).copy(alpha = a), r * 0.025f, Offset(vt.x + vs.width * (0.08f + 0.84f * fx), vt.y + vs.height * (0.12f + 0.76f * fy)))
    }
    drawRoundRect(Nebula.cyan.copy(alpha = 0.28f), vt, vs, cr, style = Stroke(r * 0.2f))
    drawRoundRect(Nebula.cyan, vt, vs, cr, style = Stroke(r * 0.07f))
}

/** The whole-screen backdrop: marble bands that drift while [playing], glows, glitter at the top and a few clouds. */
@Composable
fun NebulaBackdrop(playing: Boolean, modifier: Modifier = Modifier) {
    var t by remember { mutableFloatStateOf(0f) }
    val calm = rememberCalm()
    val on by rememberUpdatedState(playing && !calm)
    LaunchedEffect(Unit) {
        while (true) {
            if (!on) snapshotFlow { on }.first { it }
            delay(80)
            t += 0.08f
        }
    }
    Canvas(modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        drawRect(Brush.verticalGradient(listOf(Nebula.night, Color(0xFF120830), Color(0xFF0A0624))))
        drawCircle(Brush.radialGradient(listOf(Nebula.magenta.copy(alpha = 0.34f), Color.Transparent), Offset(w * 0.85f, h * 0.82f), w * 0.85f), w * 0.85f, Offset(w * 0.85f, h * 0.82f))
        drawCircle(Brush.radialGradient(listOf(Nebula.violet.copy(alpha = 0.30f), Color.Transparent), Offset(w * 0.1f, h * 0.1f), w * 0.8f), w * 0.8f, Offset(w * 0.1f, h * 0.1f))
        for (b in bands) {
            val p = Path()
            var x = -12f
            while (x <= w + 24f) {
                val k = x / w
                val y = h * b.y + h * b.amp * sin(k * b.freq * 2f * PI.toFloat() + t * b.speed + b.phase) + h * b.amp * 0.5f * sin(k * b.freq * 5f - t * b.speed * 1.3f)
                if (x < 0f) p.moveTo(x, y) else p.lineTo(x, y)
                x += 24f
            }
            drawPath(p, b.color, style = Stroke(b.width.dp.toPx(), cap = StrokeCap.Round))
        }
        drawRect(Color(0xFF05030F).copy(alpha = 0.42f)) // keeps text readable over the swirl
        // clouds
        val cloud = Nebula.violet.copy(alpha = 0.55f); val edge = Color.White.copy(alpha = 0.5f)
        drawSwirlCloud(Offset(w * 0.2f, h * 0.17f), w * 0.15f, cloud, edge)
        drawSwirlCloud(Offset(w * 0.86f, h * 0.43f), w * 0.1f, cloud, edge)
        drawSwirlCloud(Offset(w * 0.12f, h * 0.64f), w * 0.09f, cloud, edge)
        drawSwirlCloud(Offset(w * 0.66f, h * 0.92f), w * 0.13f, cloud, edge)
        // glitter, mostly at the top
        var seed = 11
        for (i in 0 until 130) {
            seed = (seed * 1103515245 + 12345) and 0x7fffffff
            val fx = (seed % 1000) / 1000f
            seed = (seed * 1103515245 + 12345) and 0x7fffffff
            val fy = (seed % 1000) / 1000f
            val a = 0.3f + 0.5f * sin(t * 1.6f + i).let { it * it }
            val col = when (i % 3) { 0 -> Color(0xFF4B3DFF); 1 -> Color(0xFFFF4D8D); else -> Color(0xFF9D7BFF) }
            drawCircle(col.copy(alpha = a), (1.2f + (i % 4) * 0.6f).dp.toPx(), Offset(w * fx, h * fy * 0.36f))
        }
    }
}

/** The astronaut on Now Playing: floats gently while music plays, visor glitter twinkles. */
@Composable
fun HelmetSticker(playing: Boolean, modifier: Modifier = Modifier) {
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
        drawHelmet(Offset(size.width * 0.5f, size.height * 0.48f + sin(t * 1.4f) * r * 0.06f), r, t)
    }
}
