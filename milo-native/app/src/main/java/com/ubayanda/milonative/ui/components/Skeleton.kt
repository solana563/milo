package com.ubayanda.milonative.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ubayanda.milonative.ui.theme.Milo
import com.ubayanda.milonative.ui.theme.Sp

/** A soft light that sweeps across a placeholder while something loads. */
fun Modifier.shimmer(): Modifier = composed {
    val t = rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart), label = "x")
    val base = Milo.card2
    val hi = Color.White.copy(alpha = 0.07f)
    this.background(base).drawBehind {
        val w = size.width.coerceAtLeast(1f)
        val c = (x * 2.2f - 0.6f) * w
        drawRect(Brush.linearGradient(listOf(Color.Transparent, hi, Color.Transparent), start = Offset(c - w * 0.4f, 0f), end = Offset(c + w * 0.4f, size.height * 0.4f)))
    }
}

@Composable
fun SkeletonBox(width: Dp, height: Dp, radius: Dp = 10.dp, modifier: Modifier = Modifier) {
    Box(modifier.width(width).height(height).clip(RoundedCornerShape(radius)).shimmer())
}

/** Rows like song rows: a cover square and two lines of text. */
@Composable
fun SkeletonRows(count: Int = 6, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = Sp.gutter, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(count) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Box(Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)).shimmer())
                Spacer(Modifier.width(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.width(if (it % 2 == 0) 190.dp else 150.dp).height(14.dp).clip(RoundedCornerShape(7.dp)).shimmer())
                    Box(Modifier.width(110.dp).height(11.dp).clip(RoundedCornerShape(6.dp)).shimmer())
                }
            }
        }
    }
}

/** A shelf of cover cards with a title bar, for Home and artist pages. */
@Composable
fun SkeletonShelf(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Box(Modifier.padding(start = Sp.gutter, top = Sp.x4, bottom = Sp.x2).width(150.dp).height(18.dp).clip(RoundedCornerShape(9.dp)).shimmer())
        LazyRow(contentPadding = PaddingValues(horizontal = Sp.gutter), horizontalArrangement = Arrangement.spacedBy(12.dp), userScrollEnabled = false) {
            items(5) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(140.dp).clip(RoundedCornerShape(12.dp)).shimmer())
                    Box(Modifier.width(110.dp).height(12.dp).clip(RoundedCornerShape(6.dp)).shimmer())
                }
            }
        }
    }
}

/** Lines of lyrics-shaped bars. */
@Composable
fun SkeletonLines(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        listOf(0.9f, 0.7f, 0.82f, 0.55f, 0.76f, 0.64f).forEach { f ->
            Box(Modifier.fillMaxWidth(f).height(24.dp).clip(RoundedCornerShape(12.dp)).shimmer())
        }
    }
}
