package com.ubayanda.milonative.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Spider mode: pitch black, bone-white text and a blood-red accent, with webs in the corners of every screen and panel,
 * a spider on Now Playing and a spider riding the sliders. Switch in Profile; off by default. Turning it on turns Y2K off.
 */
object Spider {
    var on by mutableStateOf(false)
        private set

    val red = Color(0xFFE0112B)
    val deepRed = Color(0xFF7A0A18)
    val bone = Color(0xFFEDEBE8)
    val dim = Color(0xFF8F8F96)
    val ink = Color(0xFF050506)
    val silk = Color(0xFFD9D9E0)

    private fun prefs(c: Context) = c.getSharedPreferences("milo_ui", Context.MODE_PRIVATE)
    fun load(c: Context) { on = try { prefs(c).getBoolean("spider", false) } catch (_: Throwable) { false } }
    fun set(c: Context, value: Boolean) {
        on = value
        try { prefs(c).edit().putBoolean("spider", value).apply() } catch (_: Throwable) {}
        if (value) Y2K.set(c, false)
    }
}
