package com.ubayanda.milonative.data

import android.content.Context

/**
 * The "What's new" list. It shows once after an update (when the build number goes up; a fresh install doesn't show it) and any time from
 * Profile > About. The list is bundled with the app: it is written by hand with each batch of changes, and nothing is downloaded.
 * The idea of an update and changelog sheet comes from Mochy Music (GPL-3.0, ui/screens/update); the sheet here is milo's own.
 */
object WhatsNew {
    class Entry(val title: String, val body: String)

    val entries: List<Entry> = listOf(
        Entry("Player Studio", "Switch the open player between Flagship, Poster, Timeline, Lyrics, iPod and Bedside from the ••• menu. Poster, Timeline and Lyrics can be restyled (text size, colour, backdrop, presets)."),
        Entry("A smoother open and fold", "The mini player's cover, title and buttons now travel into the full player and back, and the lyrics cover flies into its header."),
        Entry("Downloads, kept the way you saved them", "Library > Downloads separates whole playlists, albums and artists from songs saved one by one, with a cover for each."),
        Entry("Playlists", "Playlist covers are collages of their songs, with a short description you can write. Import from an .m3u or .csv file, or from a public milo. Stream playlist link; songs not on your phone are found on milo. Stream, and Import history shows what wasn't."),
        Entry("Fix a song", "From a song's ⋮ menu: shift the lyrics' timing, use your own lyrics file, or choose your own cover picture."),
        Entry("Recap story", "Profile > Your listening > Watch my recap: your time, plays, top songs and artists as a story you can share."),
        Entry("Gestures", "Profile > Gestures: choose what double-tap does, how far a skip jumps, and how far to swipe the cover."),
        Entry("Sound", "Loudness levelling and ReplayGain, gapless albums, crossfade on skips, and a rebuilt way of handling calls and other apps' audio so a song doesn't speed up after a call."),
    )

    private const val PREFS = "milo_whatsnew"

    /** true once per update; a fresh install marks the current build as seen without showing anything */
    fun pending(ctx: Context, build: Int): Boolean {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val seen = p.getInt("seen", -1)
        if (seen == -1) { p.edit().putInt("seen", build).apply(); return false }
        return build > seen
    }

    fun markSeen(ctx: Context, build: Int) { ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt("seen", build).apply() }
}
