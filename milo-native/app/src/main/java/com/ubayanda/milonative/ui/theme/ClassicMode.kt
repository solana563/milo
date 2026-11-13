package com.ubayanda.milonative.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Classic player mode: replaces the whole app with a small click-wheel music player (like an old pocket player).
 * Only music you have downloaded, the songs on the phone and what you search for. Switch in Profile; the way back is Settings inside it.
 */
object ClassicMode {
    var on by mutableStateOf(false)
        private set
    /** Black body instead of silver. */
    var dark by mutableStateOf(false)
        private set
    /** A small tick in the phone's vibration motor for every step of the wheel. */
    var haptics by mutableStateOf(true)
        private set

    private fun prefs(c: Context) = c.getSharedPreferences("milo_ui", Context.MODE_PRIVATE)
    fun load(c: Context) {
        try { val p = prefs(c); on = p.getBoolean("classic", false); dark = p.getBoolean("classic_dark", false); haptics = p.getBoolean("classic_haptics", true) } catch (_: Throwable) {}
    }
    fun setOn(c: Context, v: Boolean) { on = v; try { prefs(c).edit().putBoolean("classic", v).apply() } catch (_: Throwable) {} }
    fun setDark(c: Context, v: Boolean) { dark = v; try { prefs(c).edit().putBoolean("classic_dark", v).apply() } catch (_: Throwable) {} }
    fun setHaptics(c: Context, v: Boolean) { haptics = v; try { prefs(c).edit().putBoolean("classic_haptics", v).apply() } catch (_: Throwable) {} }
}
