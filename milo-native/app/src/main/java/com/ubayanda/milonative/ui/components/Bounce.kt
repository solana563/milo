package com.ubayanda.milonative.ui.components

import com.ubayanda.milonative.ui.theme.MiloIcons
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.ubayanda.milonative.ui.theme.Milo
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * The app's one spring. Written in React Native / Reanimated terms as damping 12, stiffness 200 (mass 1); Compose takes a damping
 * RATIO, which is damping / (2 * sqrt(stiffness * mass)) = 12 / (2 * sqrt(200)) = 0.424. Used for press releases, swipe-backs,
 * sheet drags and the pull-to-refresh bounce. Presses go DOWN with a stiffer spring so they feel instant, and come back with this one.
 */
object Bounce {
    const val DAMPING_RATIO = 0.424f
    const val STIFFNESS = 200f

    /** For scales and other small values. */
    fun scale(): SpringSpec<Float> = spring(DAMPING_RATIO, STIFFNESS)
    /** For pixel distances: stops once it is within 1.5 px. */
    fun px(): SpringSpec<Float> = spring(DAMPING_RATIO, STIFFNESS, 1.5f)
    fun dp(): SpringSpec<Dp> = spring(DAMPING_RATIO, STIFFNESS, Dp.VisibilityThreshold)
    fun offset(): SpringSpec<IntOffset> = spring(DAMPING_RATIO, STIFFNESS, IntOffset.VisibilityThreshold)
    /** The fast spring a finger-down uses. */
    fun snappy(): SpringSpec<Float> = spring(0.75f, 900f)
}

/**
 * A tactile card: it dips under the finger and its corners round off (like a squeezed pillow), then springs back with a wobble.
 * Put it BEFORE any background or glass so those are squeezed with it. [clip] = false keeps shadows from being cut (scale only).
 */
fun Modifier.tactile(radius: Dp, down: Float = 0.96f, clip: Boolean = true, enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(pressed) { if (pressed && enabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    val s by animateFloatAsState(if (pressed && enabled) down else 1f, if (pressed) Bounce.snappy() else Bounce.scale(), label = "tactileScale")
    val r by animateDpAsState(if (pressed && enabled) radius * 1.7f else radius, Bounce.dp(), label = "tactileRadius")
    this.graphicsLayer {
        scaleX = s; scaleY = s
        if (clip) { shape = RoundedCornerShape(r.coerceAtLeast(0.dp)); this.clip = true }
    }.clickable(interactionSource = src, indication = null, enabled = enabled, onClick = onClick)
}

/**
 * Pull to refresh with a spring: the list follows the finger with a rubber band (harder the further you pull), a bubble drops in from the
 * top and pops with a tick when the pull is far enough, and on release everything springs (with a bounce) to a hold position while
 * [refreshing], then bounces back up when it is done.
 */
@Composable
fun BouncyRefresh(refreshing: Boolean, onRefresh: () -> Unit, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val density = LocalDensity.current
    val threshold = with(density) { 84.dp.toPx() }
    val maxPull = with(density) { 200.dp.toPx() }
    val hold = threshold * 0.8f
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var pull by remember { mutableFloatStateOf(0f) }
    var armed by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    val refreshingNow by rememberUpdatedState(refreshing)
    val onRefreshNow by rememberUpdatedState(onRefresh)

    fun settle(target: Float) {
        job?.cancel()
        val from = pull
        job = scope.launch { Animatable(from).animateTo(target, Bounce.px()) { pull = value } }
    }
    LaunchedEffect(refreshing) {
        if (refreshing) { if (pull < hold) settle(hold) } else if (pull > 0f) { armed = false; settle(0f) }
    }
    val connection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // pushing back up while pulled down: shrink the pull first
                if (source == NestedScrollSource.UserInput && available.y < 0f && pull > 0f && !refreshingNow) {
                    job?.cancel()
                    val n = (pull + available.y).coerceAtLeast(0f)
                    val used = n - pull
                    pull = n
                    if (armed && n < threshold) armed = false
                    return Offset(0f, used)
                }
                return Offset.Zero
            }
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0f && !refreshingNow) {
                    job?.cancel()
                    val resist = 0.5f * (1f - (pull / maxPull).coerceIn(0f, 0.9f))
                    val n = (pull + available.y * resist).coerceAtMost(maxPull)
                    pull = n
                    if (!armed && n >= threshold) { armed = true; haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                    else if (armed && n < threshold) armed = false
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }
            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pull > 0f && !refreshingNow) {
                    if (pull >= threshold) { onRefreshNow(); settle(hold) } else { armed = false; settle(0f) }
                    return available
                }
                return Velocity.Zero
            }
        }
    }
    val p = (pull / threshold).coerceIn(0f, 1f)
    val pop by animateFloatAsState(if (armed) 1.2f else 1f, Bounce.scale(), label = "refreshPop")
    Box(modifier.nestedScroll(connection)) {
        Box(Modifier.fillMaxSize().graphicsLayer { translationY = pull }, content = content)
        Box(
            Modifier.align(Alignment.TopCenter)
                .offset { IntOffset(0, (pull * 0.5f - 22.dp.toPx()).roundToInt()) }
                .size(44.dp)
                .graphicsLayer {
                    val sc = (0.4f + 0.6f * p) * pop
                    scaleX = sc; scaleY = sc
                    alpha = (pull / (threshold * 0.4f)).coerceIn(0f, 1f)
                }
                .clip(CircleShape).background(Milo.card2, CircleShape).border(1.dp, Milo.line, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            MiloPullMark(p = p, armed = armed, refreshing = refreshing)
        }
    }
}

/**
 * The pull indicator: a ring of dots that appear one after another as you pull (and turn with your finger), pop when the pull is far
 * enough, then circle with a rolling swell while it loads. Drawn by hand, no icon.
 */
@Composable
private fun MiloPullMark(p: Float, armed: Boolean, refreshing: Boolean) {
    val spin = androidx.compose.animation.core.rememberInfiniteTransition(label = "pullSpin")
    val t by spin.animateFloat(
        0f, 1f,
        androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(1100, easing = androidx.compose.animation.core.LinearEasing)),
        label = "pullT",
    )
    val dots = 8
    val col = Milo.amber
    androidx.compose.foundation.Canvas(Modifier.size(30.dp)) {
        val c = center
        val ring = size.minDimension / 2f - 4.dp.toPx()
        val base = 2.2.dp.toPx()
        val turn = if (refreshing) t * 360f else p * 200f
        for (i in 0 until dots) {
            val a = Math.toRadians((turn + i * 360.0 / dots) - 90.0)
            val x = c.x + (ring * Math.cos(a)).toFloat()
            val y = c.y + (ring * Math.sin(a)).toFloat()
            val grow: Float; val fade: Float
            if (refreshing) {
                // a swell rolls round the ring, so the dots look like they chase each other
                val w = ((t * dots - i) % dots + dots) % dots
                val k = (1f - (w / dots)).coerceIn(0f, 1f)
                grow = 0.55f + 0.9f * k * k; fade = 0.35f + 0.65f * k
            } else {
                // dot i shows up once the pull has reached its share
                val k = ((p * (dots + 1) - i) ).coerceIn(0f, 1f)
                grow = (0.3f + 0.7f * k) * (if (armed) 1.15f else 1f); fade = k
            }
            drawCircle(col.copy(alpha = fade), radius = base * grow, center = Offset(x, y))
        }
    }
}
