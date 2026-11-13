package com.ubayanda.milonative.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Nebula mode: deep violet with a slowly flowing purple-and-teal marble behind everything (it moves while music plays),
 * twinkling glitter, curly clouds, neon cyan/magenta outlines and glowing orb sliders, and an astronaut helmet on Now Playing.
 * Switch in Profile; off by default; only one of Y2K / Spider / Sticker / Nebula is on at a time.
 */
object Nebula {
    var on by mutableStateOf(false)
        private set

    val cyan = Color(0xFF35F0FF)
    val magenta = Color(0xFFFF3DDA)
    val violet = Color(0xFF7A2DFF)
    val teal = Color(0xFF0E8CA6)
    val lilac = Color(0xFFB88BFF)
    val night = Color(0xFF07041A)
    val text = Color(0xFFF3ECFF)
    val dim = Color(0xFFB3A3D6)

    private fun prefs(c: Context) = c.getSharedPreferences("milo_ui", Context.MODE_PRIVATE)
    fun load(c: Context) { on = try { prefs(c).getBoolean("nebula", false) } catch (_: Throwable) { false } }
    fun set(c: Context, value: Boolean) { on = value; try { prefs(c).edit().putBoolean("nebula", value).apply() } catch (_: Throwable) {} }
}
