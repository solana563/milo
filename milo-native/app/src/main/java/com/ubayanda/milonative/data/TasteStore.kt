package com.ubayanda.milonative.data

import android.content.Context
import com.ubayanda.milonative.model.Track
import org.json.JSONArray
import org.json.JSONObject

/**
 * The listening memory behind the smart mixes and the Taste Profile. It lives only in this phone's private storage,
 * is never uploaded, can be switched off, and can be wiped at any time. Blocked artists and songs are the listener's own choices,
 * so they keep working even when personalisation is off.
 */
class TasteStore(ctx: Context) {
    private val p = ctx.getSharedPreferences("milo_taste", Context.MODE_PRIVATE)
    private val songs = HashMap<String, SongStat>()
    private val labels = HashMap<String, String>()         // key -> "Title · Artist", for the blocked list
    private val artistFirst = HashMap<String, Long>()      // lowercase artist -> first time played
    private val artistName = HashMap<String, String>()
    private val artistBoost = HashMap<String, Int>()
    private val genreBoost = HashMap<String, Int>()
    private val bannedArtists = LinkedHashMap<String, String>()   // lowercase -> display
    private val bannedSongs = LinkedHashMap<String, String>()     // key -> label
    val hours = LongArray(24)
    val weekdays = LongArray(7)
    val weeks = LinkedHashMap<String, Long>()                  // "2026-W45" -> ms
    @Volatile var enabled = true
        private set
    var since = 0L
        private set

    init {
        try {
            val o = JSONObject(p.getString("d", "{}") ?: "{}")
            enabled = o.optBoolean("on", true)
            since = o.optLong("since", 0L)
            o.optJSONArray("songs")?.let { a -> for (i in 0 until a.length()) { val r = a.getJSONObject(i); val k = r.getString("k"); songs[k] = SongStat(r.optInt("p"), r.optInt("s"), r.optLong("l"), r.optLong("f")); r.optString("n").takeIf { it.isNotEmpty() }?.let { labels[k] = it } } }
            o.optJSONObject("af")?.let { j -> j.keys().forEach { k -> val r = j.getJSONObject(k); artistFirst[k] = r.optLong("t"); artistName[k] = r.optString("n") } }
            o.optJSONObject("ab")?.let { j -> j.keys().forEach { artistBoost[it] = j.optInt(it) } }
            o.optJSONObject("gb")?.let { j -> j.keys().forEach { genreBoost[it] = j.optInt(it) } }
            o.optJSONObject("ba")?.let { j -> j.keys().forEach { bannedArtists[it] = j.optString(it) } }
            o.optJSONObject("bs")?.let { j -> j.keys().forEach { bannedSongs[it] = j.optString(it) } }
            o.optJSONArray("h")?.let { a -> for (i in 0 until minOf(24, a.length())) hours[i] = a.optLong(i) }
            o.optJSONArray("w")?.let { a -> for (i in 0 until minOf(7, a.length())) weekdays[i] = a.optLong(i) }
            o.optJSONObject("wk")?.let { j -> j.keys().asSequence().sorted().forEach { weeks[it] = j.optLong(it) } }
        } catch (_: Exception) {}
    }

    private fun weekKey(): String {
        val c = java.util.Calendar.getInstance()
        return c.get(java.util.Calendar.YEAR).toString() + "-W" + "%02d".format(c.get(java.util.Calendar.WEEK_OF_YEAR))
    }

    /** One second (or so) of listening to [t]; [play] when it just counted as a play. */
    @Synchronized fun tick(t: Track, ms: Long, play: Boolean) {
        if (!enabled) return
        if (t.durationMs == 0L && t.uri.startsWith("http")) return          // radio stream: not a song
        val now = System.currentTimeMillis()
        if (since == 0L) since = now
        val c = java.util.Calendar.getInstance()
        hours[c.get(java.util.Calendar.HOUR_OF_DAY)] += ms
        weekdays[(c.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7] += ms      // Monday = 0
        weeks.merge(weekKey(), ms, Long::plus)
        while (weeks.size > 16) weeks.remove(weeks.keys.first())
        if (!play) return
        val k = SmartMix.key(t)
        val s = songs[k] ?: SongStat()
        songs[k] = s.copy(plays = s.plays + 1, last = now, first = if (s.first == 0L) now else s.first)
        labels[k] = t.title + " · " + t.artist
        val a = t.artist.trim().lowercase()
        if (a.isNotEmpty() && t.artist != "Unknown artist" && a !in artistFirst) { artistFirst[a] = now; artistName[a] = t.artist }
    }

    @Synchronized fun skip(t: Track) {
        if (!enabled) return
        val k = SmartMix.key(t)
        val s = songs[k] ?: SongStat()
        songs[k] = s.copy(skips = s.skips + 1)
    }

    @Synchronized fun moreLike(t: Track) {
        val a = t.artist.trim().lowercase()
        if (a.isNotEmpty() && t.artist != "Unknown artist") artistBoost.merge(a, 1, Int::plus)
        if (t.genre.isNotBlank()) genreBoost.merge(t.genre.trim().lowercase(), 1, Int::plus)
        save()
    }

    @Synchronized fun banArtist(name: String) { bannedArtists[name.trim().lowercase()] = name; save() }
    @Synchronized fun banSong(t: Track) { val k = SmartMix.key(t); bannedSongs[k] = t.title + " · " + t.artist; save() }
    @Synchronized fun unbanArtist(lower: String) { bannedArtists.remove(lower); save() }
    @Synchronized fun unbanSong(key: String) { bannedSongs.remove(key); save() }
    @Synchronized fun isBanned(t: Track): Boolean = bannedArtists.containsKey(t.artist.trim().lowercase()) || bannedSongs.containsKey(SmartMix.key(t))
    fun blockedArtists(): List<Pair<String, String>> = bannedArtists.entries.map { it.key to it.value }
    fun blockedSongs(): List<Pair<String, String>> = bannedSongs.entries.map { it.key to it.value }

    @Synchronized fun setEnabled(on: Boolean) { enabled = on; save() }

    /** Forget everything learned from listening. Blocked artists and songs are kept: they are explicit choices. */
    @Synchronized fun reset() {
        songs.clear(); labels.clear(); artistFirst.clear(); artistName.clear(); artistBoost.clear(); genreBoost.clear()
        hours.fill(0); weekdays.fill(0); weeks.clear(); since = 0L
        save()
    }

    /** What the mixes see. With personalisation off they see no listening history at all, only the explicit blocks and likes. */
    fun snapshot(favs: Set<Long>): Taste = Taste(
        songs = if (enabled) HashMap(songs) else emptyMap(),
        artistBoost = if (enabled) HashMap(artistBoost) else emptyMap(),
        genreBoost = if (enabled) HashMap(genreBoost) else emptyMap(),
        bannedArtists = bannedArtists.keys.toSet(), bannedSongs = bannedSongs.keys.toSet(), favs = favs, since = if (enabled) since else 0L,
    )

    /** Artists first heard in the last [days] days, newest first. */
    fun newArtists(days: Int = 30): List<String> {
        val cut = System.currentTimeMillis() - days * SmartMix.DAY
        return artistFirst.filter { it.value >= cut }.entries.sortedByDescending { it.value }.map { artistName[it.key] ?: it.key }
    }
    fun songCount() = songs.values.count { it.plays > 0 }
    fun skipCount() = songs.values.sumOf { it.skips }

    @Synchronized fun save() {
        val o = JSONObject().put("on", enabled).put("since", since)
        val a = JSONArray(); songs.forEach { (k, v) -> a.put(JSONObject().put("k", k).put("p", v.plays).put("s", v.skips).put("l", v.last).put("f", v.first).put("n", labels[k] ?: "")) }
        o.put("songs", a)
        o.put("af", JSONObject().also { j -> artistFirst.forEach { (k, v) -> j.put(k, JSONObject().put("t", v).put("n", artistName[k] ?: k)) } })
        o.put("ab", JSONObject(artistBoost as Map<*, *>)).put("gb", JSONObject(genreBoost as Map<*, *>))
        o.put("ba", JSONObject(bannedArtists as Map<*, *>)).put("bs", JSONObject(bannedSongs as Map<*, *>))
        o.put("h", JSONArray(hours.toList())).put("w", JSONArray(weekdays.toList())).put("wk", JSONObject(weeks as Map<*, *>))
        p.edit().putString("d", o.toString()).apply()
    }
}
