package com.ubayanda.milonative.ui.player

import android.content.Context

/**
 * What the gestures on the open player do, and how hard you have to swipe. Read when a gesture happens, so a change in Profile > Gestures
 * applies at once. The idea of making the player's gestures adjustable comes from Mochy Music (GPL-3.0, github.com/realidkroo/Mochy-Music);
 * the settings themselves are milo's own.
 */
object PlayerGestures {
    private const val PREFS = "milo_fx"

    fun doubleTap(ctx: Context): Boolean = p(ctx).getBoolean("dtap", true)
    /** 0 = play or pause, 1 = nothing */
    fun centreAction(ctx: Context): Int = p(ctx).getInt("g_centre", 0)
    fun seekSeconds(ctx: Context): Int = p(ctx).getInt("g_seek", 10)
    fun swipeSkip(ctx: Context): Boolean = p(ctx).getBoolean("g_swipe", true)
    /** how far (px) a sideways swipe on the cover has to travel to skip; smaller = lighter */
    fun swipeDistance(ctx: Context): Float = p(ctx).getInt("g_dist", 160).toFloat()

    fun setDoubleTap(ctx: Context, v: Boolean) { p(ctx).edit().putBoolean("dtap", v).apply() }
    fun setCentre(ctx: Context, v: Int) { p(ctx).edit().putInt("g_centre", v).apply() }
    fun setSeek(ctx: Context, v: Int) { p(ctx).edit().putInt("g_seek", v).apply() }
    fun setSwipeSkip(ctx: Context, v: Boolean) { p(ctx).edit().putBoolean("g_swipe", v).apply() }
    fun setSwipeDistance(ctx: Context, v: Int) { p(ctx).edit().putInt("g_dist", v).apply() }
    fun reset(ctx: Context) { p(ctx).edit().remove("dtap").remove("g_centre").remove("g_seek").remove("g_swipe").remove("g_dist").apply() }

    private fun p(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
