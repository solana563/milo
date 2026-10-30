package com.ubayanda.milonative.data

import android.content.Context
import com.ubayanda.milonative.model.Track
import org.json.JSONArray
import org.json.JSONObject

/** What was listened to, kept on the phone: total time, plays per song, per artist. */
class StatsStore(ctx: Context) {
    class Row(val title: String, val artist: String, var plays: Int, var ms: Long)

    private val p = ctx.getSharedPreferences("milo_stats", Context.MODE_PRIVATE)
    private val songs = LinkedHashMap<String, Row>()
    var totalMs = 0L
        private set

    init {
        try {
            val o = JSONObject(p.getString("d", "{}") ?: "{}")
            totalMs = o.optLong("tot", 0L)
            val a = o.optJSONArray("songs") ?: JSONArray()
            for (i in 0 until a.length()) {
                val r = a.getJSONObject(i)
                songs[r.getString("k")] = Row(r.optString("t"), r.optString("a"), r.optInt("n"), r.optLong("ms"))
            }
        } catch (_: Exception) {}
    }

    /** Add [ms] of listening to [t]; [play] marks that this song just counted as one play. */
    fun add(t: Track, ms: Long, play: Boolean) {
        totalMs += ms
        if (t.durationMs == 0L && t.uri.startsWith("http")) return // radio: counts as time only
        val r = songs.getOrPut(LibrarySync.key(t.title, t.artist)) { Row(t.title, t.artist, 0, 0L) }
        r.ms += ms
        if (play) r.plays++
    }

    fun plays() = songs.values.sumOf { it.plays }
    fun topSongs(n: Int) = songs.values.filter { it.plays > 0 }.sortedWith(compareByDescending<Row> { it.plays }.thenByDescending { it.ms }).take(n)
    fun topArtists(n: Int): List<Pair<String, Int>> =
        songs.values.filter { it.plays > 0 && it.artist.isNotBlank() && it.artist != "Unknown artist" }
            .groupBy { it.artist }.map { (k, v) -> k to v.sumOf { it.plays } }.sortedByDescending { it.second }.take(n)

    fun save() { p.edit().putString("d", json().toString()).apply() }

    fun json(): JSONObject {
        val a = JSONArray()
        songs.forEach { (k, r) -> a.put(JSONObject().put("k", k).put("t", r.title).put("a", r.artist).put("n", r.plays).put("ms", r.ms)) }
        return JSONObject().put("tot", totalMs).put("songs", a)
    }

    /** Merge a backup in, keeping the larger number for each song. */
    fun merge(o: JSONObject) {
        totalMs = maxOf(totalMs, o.optLong("tot", 0L))
        val a = o.optJSONArray("songs") ?: return
        for (i in 0 until a.length()) {
            val r = a.getJSONObject(i)
            val k = r.optString("k"); if (k.isEmpty()) continue
            val cur = songs[k]
            if (cur == null) songs[k] = Row(r.optString("t"), r.optString("a"), r.optInt("n"), r.optLong("ms"))
            else { cur.plays = maxOf(cur.plays, r.optInt("n")); cur.ms = maxOf(cur.ms, r.optLong("ms")) }
        }
        save()
    }
}
