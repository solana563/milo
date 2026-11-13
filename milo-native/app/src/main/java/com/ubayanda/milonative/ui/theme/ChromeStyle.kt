package com.ubayanda.milonative.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Chrome mode: liquid-silver Y2K look on black. Panels get a polished-metal rim, sliders become chrome tubes with a glossy ball,
 * and chrome sparkles, orbit rings, a heart and a planet are scattered about and float on Now Playing.
 * Switch in Profile; off by default; only one theme (Y2K / Spider / Sticker / Nebula / Chrome) is on at a time.
 */
object ChromeStyle {
    var on by mutableStateOf(false)
        private set

    val black = Color(0xFF080809)
    val text = Color(0xFFEDEFF2)
    val dim = Color(0xFF8B9099)
    val silver = Color(0xFFD8DCE2)

    /** The rim of a panel: bright at the corners, darker in between, like a polished edge. */
    val rim: Brush get() = Brush.linearGradient(
        0f to Color.White, 0.3f to Color(0xFF6C727B), 0.5f to Color(0xFFF2F4F7), 0.75f to Color(0xFF4A4F57), 1f to Color(0xFFE6E9ED),
    )

    private fun prefs(c: Context) = c.getSharedPreferences("milo_ui", Context.MODE_PRIVATE)
    fun load(c: Context) { on = try { prefs(c).getBoolean("chrome", false) } catch (_: Throwable) { false } }
    fun set(c: Context, value: Boolean) { on = value; try { prefs(c).edit().putBoolean("chrome", value).apply() } catch (_: Throwable) {} }
}
