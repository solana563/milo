package com.ubayanda.milonative.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Rows arriving with a page: each rises into place from lower down and fades in, the further down the list the later and the
 * further it travels, so they land as a cascade. After Mochy Music's page transition (GPL-3.0, github.com/realidkroo/Mochy-Music,
 * ui/player/PlayerPageTransition.kt); written for milo. Pass [enabled] = true only while the page is new, so rows that scroll into
 * view later do not animate. Rows past [maxRows] just appear.
 */
@Composable
fun Modifier.cascadeIn(index: Int, enabled: Boolean, maxRows: Int = 10): Modifier {
    if (!enabled || index < 0 || index >= maxRows) return this
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * 34L)
        a.animateTo(1f, spring(0.78f, 240f))
    }
    return this.graphicsLayer {
        val e = a.value
        alpha = e.coerceIn(0f, 1f)
        // unclamped, so a quick open overshoots a hair and settles
        translationY = (1f - e) * (34.dp.toPx() + 9.dp.toPx() * index)
    }
}
