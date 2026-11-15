package com.ubayanda.milonative.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * Round 84: all live blur and the liquid-glass shader are gone (they cost frames, mostly on mid-range phones).
 * These names stay so callers keep compiling; backdropSource records nothing and liquidGlass is the plain glass() surface.
 */

/** Kept for API compatibility. Nothing is recorded any more. */
class BackdropState {
    var origin by mutableStateOf(Offset.Zero)
}

@Composable
fun rememberBackdropState(): BackdropState = remember { BackdropState() }

/** No-op since Round 84. */
fun Modifier.backdropSource(state: BackdropState): Modifier = this

/** A flat translucent surface (same as glass()). The extra parameters are ignored. */
@Composable
fun Modifier.liquidGlass(
    shape: Shape, radius: Dp, backdrop: BackdropState?, tint: Color, strength: Float = 1f,
    tintAlpha: Float = 0.42f, blur: Dp = 14.dp, refract: Boolean = true,
): Modifier = this.glass(shape, strength, tint.copy(alpha = 0.94f))
