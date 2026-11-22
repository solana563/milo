package com.ubayanda.milonative.data

import android.content.Context

/** A short description for each playlist you make ("songs for long drives"). Kept on the phone, keyed by the playlist's id. */
object PlaylistNotes {
    private const val PREFS = "milo_plnotes"
    fun get(ctx: Context, id: String): String = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(id, "").orEmpty()
    fun set(ctx: Context, id: String, text: String) {
        val e = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        if (text.isBlank()) e.remove(id) else e.putString(id, text.trim().take(300))
        e.apply()
    }
}
