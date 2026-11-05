package com.ubayanda.milonative.player

import android.content.Context

/** Long recordings (podcasts, audiobooks, DJ mixes) remember where you stopped and how fast you were listening. */
object Resume {
    const val LONG_MS = 20L * 60_000L
    private fun p(c: Context) = c.getSharedPreferences("milo_resume", Context.MODE_PRIVATE)

    fun put(c: Context, id: String, posMs: Long, speed: Float) { p(c).edit().putString(id, "$posMs,$speed").apply() }
    fun drop(c: Context, id: String) { p(c).edit().remove(id).apply() }
    /** (position in ms, speed) or null. */
    fun get(c: Context, id: String): Pair<Long, Float>? {
        val v = p(c).getString(id, null) ?: return null
        val a = v.split(',')
        val pos = a.getOrNull(0)?.toLongOrNull() ?: return null
        return pos to (a.getOrNull(1)?.toFloatOrNull() ?: 1f)
    }
}
