package com.ubayanda.milonative.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ubayanda.milonative.R

private class Leaf(val x: Float, val y: Float, val w: Int, val rot: Float, val a: Float, val phase: Float)

private val leaves = listOf(
    Leaf(0.06f, 0.05f, 120, -12f, 0.42f, 0f),
    Leaf(0.70f, 0.02f, 90, 18f, 0.28f, 0.4f),
    Leaf(0.38f, 0.17f, 130, 8f, 0.18f, 0.7f),
    Leaf(-0.04f, 0.27f, 100, 24f, 0.30f, 0.2f),
    Leaf(0.62f, 0.25f, 120, -6f, 0.34f, 0.9f),
    Leaf(0.14f, 0.40f, 90, -18f, 0.20f, 0.5f),
)


/** The slowly drifting milo. leaves behind the welcome screens. */
@Composable
fun LeavesBackground(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "leaves")
    val drift by t.animateFloat(0f, 1f, infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Reverse), label = "d")
    BoxWithConstraints(modifier) {
        val w = maxWidth
        val h = maxHeight
        leaves.forEach { l ->
            Image(
                painterResource(R.drawable.ic_leaf), null,
                Modifier.offset(x = w * l.x, y = h * l.y + (drift - 0.5f).times(24f * (0.6f + l.phase)).dp)
                    .width(l.w.dp).rotate(l.rot + (drift - 0.5f) * 8f).alpha(l.a),
            )
        }
    }
}
