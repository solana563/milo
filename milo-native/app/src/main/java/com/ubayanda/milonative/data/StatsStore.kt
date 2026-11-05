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

    /** The same numbers kept per calendar year, so there can be a real yearly recap. They start from when this version was installed. */
    class Year(var ms: Long = 0L, val songs: LinkedHashMap<String, Row> = LinkedHashMap())
    private val yrs = LinkedHashMap<Int, Year>()
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
            val y = o.optJSONObject("yrs")
            y?.keys()?.forEach { k ->
                val yo = y.getJSONObject(k); val yb = Year(yo.optLong("tot", 0L))
                val ya = yo.optJSONArray("songs") ?: JSONArray()
                for (i in 0 until ya.length()) { val r = ya.getJSONObject(i); yb.songs[r.getString("k")] = Row(r.optString("t"), r.optString("a"), r.optInt("n"), r.optLong("ms")) }
                k.toIntOrNull()?.let { yrs[it] = yb }
            }
        } catch (_: Exception) {}
    }

    /** Add [ms] of listening to [t]; [play] marks that this song just counted as one play. */
    fun add(t: Track, ms: Long, play: Boolean) {
        totalMs += ms
        val yb = yrs.getOrPut(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)) { Year() }
        yb.ms += ms
        if (t.durationMs == 0L && t.uri.startsWith("http")) return // radio: counts as time only
        val key = LibrarySync.key(t.title, t.artist)
        val r = songs.getOrPut(key) { Row(t.title, t.artist, 0, 0L) }
        r.ms += ms
        if (play) r.plays++
        val yr = yb.songs.getOrPut(key) { Row(t.title, t.artist, 0, 0L) }
        yr.ms += ms
        if (play) yr.plays++
    }

    fun playsOf(title: String, artist: String) = songs[LibrarySync.key(title, artist)]?.plays ?: 0
    fun plays() = songs.values.sumOf { it.plays }
    fun topSongs(n: Int) = songs.values.filter { it.plays > 0 }.sortedWith(compareByDescending<Row> { it.plays }.thenByDescending { it.ms }).take(n)
    fun topArtists(n: Int): List<Pair<String, Int>> =
        songs.values.filter { it.plays > 0 && it.artist.isNotBlank() && it.artist != "Unknown artist" }
            .groupBy { it.artist }.map { (k, v) -> k to v.sumOf { it.plays } }.sortedByDescending { it.second }.take(n)

    fun years(): List<Int> = yrs.keys.sortedDescending()
    fun yearMs(y: Int) = yrs[y]?.ms ?: 0L
    fun yearPlays(y: Int) = yrs[y]?.songs?.values?.sumOf { it.plays } ?: 0
    fun yearTopSongs(y: Int, n: Int) = (yrs[y]?.songs?.values ?: emptyList<Row>()).filter { it.plays > 0 }
        .sortedWith(compareByDescending<Row> { it.plays }.thenByDescending { it.ms }).take(n)
    fun yearTopArtists(y: Int, n: Int): List<Pair<String, Int>> =
        (yrs[y]?.songs?.values ?: emptyList<Row>()).filter { it.plays > 0 && it.artist.isNotBlank() && it.artist != "Unknown artist" }
            .groupBy { it.artist }.map { (k, v) -> k to v.sumOf { it.plays } }.sortedByDescending { it.second }.take(n)

    fun save() { p.edit().putString("d", json().toString()).apply() }

    fun json(): JSONObject {
        val a = JSONArray()
        songs.forEach { (k, r) -> a.put(JSONObject().put("k", k).put("t", r.title).put("a", r.artist).put("n", r.plays).put("ms", r.ms)) }
        val y = JSONObject()
        yrs.forEach { (yy, yb) ->
            val ya = JSONArray()
            yb.songs.forEach { (k, r) -> ya.put(JSONObject().put("k", k).put("t", r.title).put("a", r.artist).put("n", r.plays).put("ms", r.ms)) }
            y.put(yy.toString(), JSONObject().put("tot", yb.ms).put("songs", ya))
        }
        return JSONObject().put("tot", totalMs).put("songs", a).put("yrs", y)
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
        val yo = o.optJSONObject("yrs")
        yo?.keys()?.forEach { k ->
            val yy = k.toIntOrNull() ?: return@forEach
            val src = yo.getJSONObject(k); val yb = yrs.getOrPut(yy) { Year() }
            yb.ms = maxOf(yb.ms, src.optLong("tot", 0L))
            val ya = src.optJSONArray("songs") ?: return@forEach
            for (i in 0 until ya.length()) {
                val r = ya.getJSONObject(i); val kk = r.optString("k"); if (kk.isEmpty()) continue
                val cur = yb.songs[kk]
                if (cur == null) yb.songs[kk] = Row(r.optString("t"), r.optString("a"), r.optInt("n"), r.optLong("ms"))
                else { cur.plays = maxOf(cur.plays, r.optInt("n")); cur.ms = maxOf(cur.ms, r.optLong("ms")) }
            }
        }
        save()
    }
}
