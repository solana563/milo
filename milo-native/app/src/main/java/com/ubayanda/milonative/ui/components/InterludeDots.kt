package com.ubayanda.milonative.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Gaps in the singing shorter than this are only a breath between lines, not an interlude. */
const val MinInterludeMs = 4_000L

/**
 * Three dots that count down an instrumental gap, lighting one by one, then swell as the next line arrives.
 * Idea from Mochy Music's LyricsInterludeDots (GPL-3.0, github.com/realidkroo/Mochy-Music); this one is written for milo and
 * is much simpler (no shader bloom). The row stays in the list at zero height when it isn't the gap being played, so lines
 * slide down to make room as it opens and back up as it closes.
 */
@Composable
fun InterludeDots(startMs: Long, endMs: Long, posMs: Long, color: Color = Color.White, modifier: Modifier = Modifier) {
    val span = (endMs - startMs).coerceAtLeast(1L)
    val active = posMs in startMs until endMs
    val open by animateFloatAsState(
        if (active) 1f else 0f,
        if (active) spring(Spring.DampingRatioNoBouncy, Spring.StiffnessLow) else tween(320), label = "interludeOpen",
    )
    if (open <= 0.001f && !active) return
    val progress = ((posMs - startMs).toFloat() / span).coerceIn(0f, 1f)
    val bob by rememberInfiniteTransition(label = "interludeBob").animateFloat(
        0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart), label = "bob",
    )
    // in the last stretch all three swell together, a cue that the singing is about to start
    val finale = ((progress - 0.88f) / 0.12f).coerceIn(0f, 1f)
    val swell = 1f + 0.22f * kotlin.math.sin(Math.PI.toFloat() * finale)
    Row(
        modifier.fillMaxWidth().height(56.dp)
            .layout { m, c ->
                val full = 56.dp.roundToPx()
                val p = m.measure(c.copy(minHeight = full, maxHeight = full))
                val shown = (full * open.coerceIn(0f, 1f)).roundToInt()
                layout(p.width, shown) { p.place(0, (shown - full) / 2) }
            }
            .graphicsLayer { alpha = open.coerceIn(0f, 1f) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        repeat(3) { i ->
            val slice = i / 3f
            val fill = (((progress - slice) * 3f) / 0.55f).coerceIn(0f, 1f).let { it * it * (3f - 2f * it) }
            val phase = bob * 2f * Math.PI.toFloat() - i * 0.9f
            Row(
                Modifier.size(12.dp).graphicsLayer {
                    val s = open.coerceIn(0f, 1f) * swell
                    scaleX = s; scaleY = s
                    translationY = kotlin.math.sin(phase) * 1.8.dp.toPx()
                    alpha = 0.3f + 0.7f * fill
                }.drawBehind { drawCircle(color, size.minDimension / 2f) },
            ) {}
            if (i < 2) Row(Modifier.size(7.dp)) {}
        }
    }
}
