package com.ubayanda.milonative.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Sticker mode: black-and-white "sticker sheet" look: heavy white outlines, four-point sparkles, orbit rings, a checker strip,
 * black-and-white cover art and a doodle buddy on Now Playing. Switch in Profile; off by default; only one of Y2K / Spider / Sticker is on at a time.
 */
object Sticker {
    var on by mutableStateOf(false)
        private set

    val ink = Color(0xFF000000)
    val paper = Color(0xFFFFFFFF)
    val dim = Color(0xFFA6A6A6)

    private fun prefs(c: Context) = c.getSharedPreferences("milo_ui", Context.MODE_PRIVATE)
    fun load(c: Context) { on = try { prefs(c).getBoolean("sticker", false) } catch (_: Throwable) { false } }
    fun set(c: Context, value: Boolean) { on = value; try { prefs(c).edit().putBoolean("sticker", value).apply() } catch (_: Throwable) {} }
}

/** Picks one theme (or none): "y2k", "spider", "sticker", null. */
object Themes {
    fun apply(c: Context, name: String?) {
        Y2K.set(c, name == "y2k")
        Spider.set(c, name == "spider")
        Sticker.set(c, name == "sticker")
        Nebula.set(c, name == "nebula")
    }
}
