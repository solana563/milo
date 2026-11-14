package com.ubayanda.milonative.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/** A button feel: it dips slightly under the finger and springs back, with a light tick. Used by pills, chips and the player controls. */
fun Modifier.pressable(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    // liquid press: it squashes (wider than tall) under the finger and wobbles back like a drop
    val sx by animateFloatAsState(if (pressed && enabled) 0.96f else 1f, if (pressed) Bounce.snappy() else Bounce.scale(), label = "pressX")
    val sy by animateFloatAsState(if (pressed && enabled) 0.92f else 1f, if (pressed) Bounce.snappy() else Bounce.scale(), label = "pressY")
    val haptic = LocalHapticFeedback.current
    this.graphicsLayer { scaleX = sx; scaleY = sy }
        .clickable(interactionSource = src, indication = null, enabled = enabled, role = androidx.compose.ui.semantics.Role.Button) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() }
}
