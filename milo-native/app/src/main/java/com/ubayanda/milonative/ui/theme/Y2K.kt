package com.ubayanda.milonative.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The Y2K look: aqua gel, chrome and a deep-blue panel, a spinning CD and an LCD read-out on Now Playing.
 * A switch in Profile turns it on; with it off nothing about the normal look changes.
 */
object Y2K {
    var on by mutableStateOf(false)
        private set

    val aqua = Color(0xFF35E0FF)
    val ice = Color(0xFFEAF6FF)
    val dim = Color(0xFF9DB7CC)
    val navy = Color(0xFF05182B)
    val chromeLight = Color(0xFFFFFFFF)
    val chromeDark = Color(0xFF9CC9E6)
    val lcdBg1 = Color(0xFF031A22)
    val lcdBg2 = Color(0xFF062A36)
    val lcdGlow = Color(0xFF7DF3FF)
    val rainbow = listOf(Color(0xFFFF6EC7), Color(0xFFFFE96E), Color(0xFF6EFFB5), Color(0xFF6EC7FF), Color(0xFFB56EFF), Color(0xFFFF6EC7))

    /** Dot-matrix style read-out text (the phone's own monospace font). */
    val lcd = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

    private fun prefs(c: Context) = c.getSharedPreferences("milo_ui", Context.MODE_PRIVATE)
    fun load(c: Context) { on = try { prefs(c).getBoolean("y2k", false) } catch (_: Throwable) { false } }
    fun set(c: Context, value: Boolean) { on = value; try { prefs(c).edit().putBoolean("y2k", value).apply() } catch (_: Throwable) {} }
}
