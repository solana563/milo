package com.ubayanda.milonative.data

import com.ubayanda.milonative.model.Track

/** What the phone remembers about one song. Times are epoch milliseconds, 0 = never. */
data class SongStat(val plays: Int = 0, val skips: Int = 0, val last: Long = 0L, val first: Long = 0L)

/** A read-only picture of the listener's taste at one moment. Built by [TasteStore], consumed by [SmartMix]. */
class Taste(
    val songs: Map<String, SongStat> = emptyMap(),
    val artistBoost: Map<String, Int> = emptyMap(),
    val genreBoost: Map<String, Int> = emptyMap(),
    val bannedArtists: Set<String> = emptySet(),
    val bannedSongs: Set<String> = emptySet(),
    val favs: Set<Long> = emptySet(),
    /** When listening was first recorded. Needed so "not played for a while" is never claimed about a period nobody was watching. */
    val since: Long = 0L,
    val now: Long = System.currentTimeMillis(),
) {
    private val artistTotals: Map<String, Int> by lazy {
        val m = HashMap<String, Int>()
        songs.forEach { (k, v) -> m.merge(k.substringAfterLast('|'), v.plays, Int::plus) }
        m
    }
    fun stat(t: Track) = songs[SmartMix.key(t)] ?: SongStat()
    fun artistPlays(t: Track) = artistTotals[t.artist.trim().lowercase()] ?: 0
    fun banned(t: Track) = t.artist.trim().lowercase() in bannedArtists || SmartMix.key(t) in bannedSongs
}

/**
 * Builds playlists from what the listener actually does, entirely on the phone: no accounts, no uploads, no outside AI.
 * Every function is a plain calculation over the song list and a [Taste], so results are explainable and testable.
 * Tempo and energy are NOT analysed (that would need audio analysis); moods come from genre tags, year and title words only.
 */
object SmartMix {
    const val DAY = 86_400_000L
    const val MIN_MOOD = 8

    /** Highest score first, each song scored exactly ONCE. Never put randomness inside a sort comparison: the sort needs a song to keep the same score. */
    private fun <T> byScore(list: List<T>, score: (T) -> Double): List<T> = list.map { it to score(it) }.sortedByDescending { it.second }.map { it.first }

    fun key(t: Track) = t.title.trim().lowercase() + "|" + t.artist.trim().lowercase()

    fun eligible(all: List<Track>, taste: Taste) = all.filter { it.durationMs > 30_000L && !taste.banned(it) }

    /** Higher = the listener likes it more. Plays and likes push up, skips push down, a liked artist helps a little. */
    fun affinity(t: Track, taste: Taste): Double {
        val s = taste.stat(t)
        var a = Math.log(1.0 + s.plays) * 2.0 - s.skips * 1.2
        if (t.id in taste.favs) a += 3.0
        a += Math.log(1.0 + taste.artistPlays(t)) * 0.6
        a += (taste.artistBoost[t.artist.trim().lowercase()] ?: 0) * 1.5
        if (t.genre.isNotBlank()) a += (taste.genreBoost[t.genre.trim().lowercase()] ?: 0) * 1.0
        return a
    }

    /** At most [per] songs of one artist, in order. */
    fun spread(list: List<Track>, per: Int = 3): List<Track> {
        val n = HashMap<String, Int>()
        return list.filter { n.merge(it.artist.lowercase(), 1, Int::plus)!! <= per }
    }

    fun smart(all: List<Track>, taste: Taste, seed: Long, n: Int = 40): List<Track> {
        val rnd = java.util.Random(seed)
        val pool = eligible(all, taste).filter { taste.stat(it).plays > 0 || it.id in taste.favs }
        return spread(byScore(pool) { affinity(it, taste) + rnd.nextDouble() * 1.5 }).take(n)
    }

    /** Four parts: familiar favourites, new finds, more from preferred artists, and favourites that haven't had a turn lately. Nothing played in the last 12 hours. */
    fun daily(all: List<Track>, taste: Taste, seed: Long, n: Int = 30): List<Track> {
        val rnd = java.util.Random(seed)
        val pool = eligible(all, taste).filter { taste.now - taste.stat(it).last > 12 * 3_600_000L }
        val per = n / 4
        val liked = { t: Track -> t.id in taste.favs || taste.stat(t).plays >= 2 }
        val familiar = byScore(pool.filter(liked)) { affinity(it, taste) + rnd.nextDouble() * 2 }.take(per + n % 4)
        val used = familiar.map { it.id }.toHashSet()
        val fresh = pool.filter { it.id !in used && (taste.stat(it).first > taste.now - 14 * DAY && taste.stat(it).first > 0 || it.addedSec * 1000 > taste.now - 30 * DAY) }
            .shuffled(rnd).take(per)
        fresh.forEach { used.add(it.id) }
        val topArtists = pool.filter { taste.artistPlays(it) > 0 }.groupBy { it.artist.lowercase() }.entries
            .sortedByDescending { e -> taste.artistPlays(e.value.first()) }.take(8).map { it.key }.toSet()
        val preferred = pool.filter { it.id !in used && it.artist.lowercase() in topArtists && taste.stat(it).plays == 0 }.shuffled(rnd).take(per)
        preferred.forEach { used.add(it.id) }
        val rested = pool.filter { it.id !in used && liked(it) && taste.stat(it).last > 0 }.sortedBy { taste.stat(it).last }.take(per)
        return spread(familiar + fresh + preferred + rested + pool.filter { it.id !in used }.shuffled(rnd), 3).distinctBy { it.id }.take(n).shuffled(rnd)
    }

    /** Songs once played or liked a lot and not heard for [days] days. Empty until listening has been recorded for that long. */
    fun forgotten(all: List<Track>, taste: Taste, days: Int = 45, n: Int = 40): List<Track> {
        val cut = taste.now - days * DAY
        return eligible(all, taste).filter { t ->
            val s = taste.stat(t)
            val loved = t.id in taste.favs || s.plays >= 3
            loved && ((s.last in 1 until cut) || (s.last == 0L && t.id in taste.favs && taste.since in 1 until cut))
        }.sortedByDescending { affinity(it, taste) }.take(n)
    }

    /** Songs not played yet, from artists and genres the listener already likes. */
    fun discovery(all: List<Track>, taste: Taste, seed: Long, n: Int = 40): List<Track> {
        val rnd = java.util.Random(seed)
        val pool = eligible(all, taste).filter { taste.stat(it).plays == 0 && taste.stat(it).skips == 0 }
        return spread(byScore(pool) { t ->
            taste.artistPlays(t) * 0.5 + (taste.artistBoost[t.artist.trim().lowercase()] ?: 0) * 2.0 +
                (if (t.genre.isNotBlank()) (taste.genreBoost[t.genre.trim().lowercase()] ?: 0) * 1.0 else 0.0) + rnd.nextDouble() * 2
        }, 2).take(n)
    }

    enum class Mood(val label: String) { CHILL("Chill"), FOCUS("Focus"), WORKOUT("Workout"), ENERGY("Energy"), SLEEP("Sleep"), ROMANTIC("Romantic"), PARTY("Party"), NOSTALGIA("Nostalgia") }

    private val genreWords = mapOf(
        Mood.CHILL to listOf("ambient", "chill", "lounge", "downtempo", "acoustic", "folk", "bossa", "jazz", "soul", "lo-fi", "lofi", "indie"),
        Mood.FOCUS to listOf("classical", "instrumental", "ambient", "lo-fi", "lofi", "piano", "soundtrack", "score", "post-rock", "study"),
        Mood.WORKOUT to listOf("hip hop", "hip-hop", "rap", "trap", "electronic", "dance", "edm", "house", "techno", "rock", "metal", "drum", "gym"),
        Mood.ENERGY to listOf("dance", "electronic", "rock", "metal", "punk", "house", "drum and bass", "techno", "edm"),
        Mood.SLEEP to listOf("ambient", "new age", "meditation", "sleep", "classical", "lullaby", "nature"),
        Mood.ROMANTIC to listOf("r&b", "rnb", "soul", "romantic", "ballad", "love", "slow jam"),
        Mood.PARTY to listOf("dance", "house", "hip hop", "hip-hop", "reggaeton", "afro", "amapiano", "kwaito", "club", "disco", "edm", "pop"),
    )
    private val titleWords = mapOf(
        Mood.SLEEP to listOf("sleep", "lullaby", "dream", "goodnight"),
        Mood.ROMANTIC to listOf("love", "heart", "kiss", "forever"),
    )

    /** Does the song's own information suggest this mood? Never a guess without something to go on. */
    fun fits(t: Track, mood: Mood, thisYear: Int): Boolean {
        val g = t.genre.trim().lowercase()
        val title = t.title.lowercase()
        if (mood == Mood.NOSTALGIA) return t.year in 1..(thisYear - 15)
        val byGenre = g.isNotEmpty() && genreWords[mood].orEmpty().any { g.contains(it) }
        val byTitle = titleWords[mood].orEmpty().any { Regex("\\b" + it + "\\b").containsMatchIn(title) }
        if (mood == Mood.SLEEP && t.durationMs in 1..119_999L) return false          // too short to wind down to
        if (mood == Mood.WORKOUT && t.durationMs > 480_000L) return false
        return byGenre || byTitle
    }

    /** A mood mix, or null when fewer than [MIN_MOOD] songs carry enough information to say they fit. */
    fun mood(all: List<Track>, taste: Taste, mood: Mood, seed: Long, thisYear: Int, n: Int = 40): List<Track>? {
        val m = eligible(all, taste).filter { fits(it, mood, thisYear) }
        if (m.size < MIN_MOOD) return null
        val rnd = java.util.Random(seed)
        return spread(byScore(m) { affinity(it, taste) + rnd.nextDouble() * 2 }, 3).take(n).takeIf { it.size >= MIN_MOOD }
    }

    /** Plays per genre, biggest first. Empty when the songs have no genre tags. */
    fun genres(all: List<Track>, taste: Taste, n: Int = 5): List<Pair<String, Int>> =
        all.filter { it.genre.isNotBlank() }.groupBy { it.genre.trim() }
            .map { (g, v) -> g to v.sumOf { taste.stat(it).plays } }.filter { it.second > 0 }.sortedByDescending { it.second }.take(n)
}
