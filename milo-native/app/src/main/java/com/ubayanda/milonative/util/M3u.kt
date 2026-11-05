package com.ubayanda.milonative.util

import com.ubayanda.milonative.model.Track

/** Reading and writing .m3u playlists. Songs are matched by title and artist, since file locations differ between devices. */
object M3u {
    fun export(tracks: List<Track>): String {
        val sb = StringBuilder("#EXTM3U\n")
        for (t in tracks) {
            val who = if (t.artist.isBlank() || t.artist == "Unknown artist") "" else t.artist + " - "
            sb.append("#EXTINF:").append(t.durationMs / 1000).append(',').append(who).append(t.title).append('\n')
            sb.append(if (t.uri.startsWith("yt:")) "https://music.youtube.com/watch?v=" + t.uri.removePrefix("yt:") else t.uri).append('\n')
        }
        return sb.toString()
    }

    /** (title, artist) guesses, in order. The artist is empty when the file only gave a title. */
    fun parse(text: String): List<Pair<String, String>> {
        val out = ArrayList<Pair<String, String>>()
        var pending: String? = null
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            if (line.startsWith("#EXTINF:", true)) { pending = line.substringAfter(',', "").trim(); continue }
            if (line.startsWith("#")) continue
            val label = pending?.takeIf { it.isNotEmpty() } ?: line.substringAfterLast('/').substringAfterLast('\\').substringBeforeLast('.')
            pending = null
            val i = label.indexOf(" - ")
            out.add(if (i > 0) label.substring(i + 3).trim() to label.substring(0, i).trim() else label.trim() to "")
        }
        return out
    }
}
