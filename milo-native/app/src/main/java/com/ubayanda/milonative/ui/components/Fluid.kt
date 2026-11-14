package com.ubayanda.milonative.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawBehind
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** A line that ripples like a wave from the left edge to [endX]. [amp] = 0 gives a plain round-capped line (what a paused song shows). */
fun DrawScope.wavyLine(brush: Brush, endX: Float, cy: Float, thick: Float, amp: Float, wavelength: Float, phase: Float) {
    if (endX <= 0f) return
    val p = Path()
    if (amp < 0.05f) {
        p.moveTo(0f, cy); p.lineTo(endX, cy)
    } else {
        val k = (2.0 * PI / wavelength).toFloat()
        p.moveTo(0f, cy + amp * sin(phase))
        var x = 0f
        while (x < endX) { x = min(x + 3f, endX); p.lineTo(x, cy + amp * sin(x * k + phase)) }
    }
    drawPath(p, brush, style = Stroke(thick, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun shifted(c: Color): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(android.graphics.Color.argb(255, (c.red * 255).toInt(), (c.green * 255).toInt(), (c.blue * 255).toInt()), hsv)
    hsv[0] = (hsv[0] + 40f) % 360f
    hsv[1] = (hsv[1] * 1.1f).coerceAtMost(1f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

/**
 * Moving colour behind the full player: the song's dominant colour (from the cover) and a neighbouring hue, blended slowly,
 * with a soft glow that drifts while music plays. Colours crossfade when the song changes. Still when paused or when the phone's
 * "Remove animations" is on.
 */
@Composable
fun AuraBackdrop(tint: Int, playing: Boolean, modifier: Modifier = Modifier) {
    val calm = rememberCalm()
    val dark = Color(0xFF0B0A09)
    val base = if (tint == 0) Color(0xFF2A2622) else androidx.compose.ui.graphics.lerp(dark, Color(0xFF000000.toInt() or tint), 0.6f)
    val a by animateColorAsState(base, tween(900), label = "auraA")
    val b by animateColorAsState(remember(base) { shifted(base) }, tween(900), label = "auraB")
    var phase by remember { mutableFloatStateOf(0f) }
    val live = playing && !calm
    LaunchedEffect(live) { while (live) { delay(50); phase = (phase + 0.0035f) % 1f } }
    Box(
        modifier.drawBehind {
            drawRect(Brush.linearGradient(listOf(a, b.copy(alpha = 0.8f), dark), Offset(0f, 0f), Offset(size.width, size.height)))
            val ang = phase * 2f * PI.toFloat()
            val c = Offset(size.width * (0.5f + 0.3f * cos(ang)), size.height * (0.35f + 0.15f * sin(ang)))
            drawRect(Brush.radialGradient(listOf(b.copy(alpha = 0.5f), Color.Transparent), c, size.maxDimension * 0.6f))
        }
    )
}
