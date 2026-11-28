package com.ubayanda.milonative.data

import com.ubayanda.milonative.model.Track
import kotlin.math.abs

/** What counts as "the same song": same title and artist (ignoring case, spaces and punctuation) and nearly the same length, whatever file or source it comes from. */
object Dupes {
    private const val LENGTH_SLACK_MS = 3_000L

    fun norm(s: String?): String = s.orEmpty().lowercase().filter { it.isLetterOrDigit() }

    fun sameSong(titleA: String?, artistA: String?, durA: Long, titleB: String?, artistB: String?, durB: Long): Boolean {
        val t = norm(titleA)
        if (t.isEmpty() || t != norm(titleB) || norm(artistA) != norm(artistB)) return false
        return durA <= 0L || durB <= 0L || abs(durA - durB) <= LENGTH_SLACK_MS
    }

    fun same(a: Track, b: Track): Boolean = a.id == b.id || a.uri == b.uri || sameSong(a.title, a.artist, a.durationMs, b.title, b.artist, b.durationMs)

    /** [list] without repeats (first copy wins). [keep] is always kept, even when an earlier copy of it exists, so a song the person tapped stays in the queue. */
    fun distinct(list: List<Track>, keep: Track? = null): List<Track> {
        if (list.size < 2) return list
        fun key(t: Track) = norm(t.title) + "|" + norm(t.artist) + "|" + (t.durationMs / 4_000L)
        val winner = HashMap<String, Long>()          // key -> id of the track that stays
        if (keep != null) winner[key(keep)] = keep.id
        for (t in list) winner.putIfAbsent(key(t), t.id)
        return list.filter { t -> t.id == keep?.id || winner[key(t)] == t.id }.distinctBy { it.id }
    }
}
