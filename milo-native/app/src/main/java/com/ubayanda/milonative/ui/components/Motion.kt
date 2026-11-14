package com.ubayanda.milonative.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** True when the phone's "Remove animations" setting is on: the themes' moving decorations stay still. */
@Composable
fun rememberCalm(): Boolean {
    val ctx = LocalContext.current
    return remember {
        try { android.provider.Settings.Global.getFloat(ctx.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f } catch (_: Throwable) { false }
    }
}
