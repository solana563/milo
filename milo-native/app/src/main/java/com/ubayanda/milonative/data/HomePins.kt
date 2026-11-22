package com.ubayanda.milonative.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** The tiles the person pinned on Home, in the order they chose. Keys: "sc:<shortcut>" or "pl:<playlist id>". */
object HomePins {
    val shortcuts = listOf("search", "friends", "downloads", "profile", "identify", "car", "bedside", "ipod")
    private val default = listOf("sc:search", "sc:friends", "sc:downloads", "sc:identify", "sc:car", "sc:bedside", "sc:ipod", "sc:profile")

    var list by mutableStateOf(default)
        private set
    private var loaded = false

    fun init(ctx: Context) {
        if (loaded) return
        loaded = true
        val s = ctx.applicationContext.getSharedPreferences("milo_pins", Context.MODE_PRIVATE).getString("l", null)
        if (s != null) list = s.split('\n').filter { it.isNotEmpty() }
    }

    private fun save(ctx: Context) {
        ctx.applicationContext.getSharedPreferences("milo_pins", Context.MODE_PRIVATE).edit().putString("l", list.joinToString("\n")).apply()
    }

    fun toggle(ctx: Context, key: String) {
        list = if (key in list) list - key else list + key
        save(ctx)
    }

    fun reset(ctx: Context) { list = default; save(ctx) }
}
