package com.ubayanda.milonative.data

import android.content.Context
import com.ubayanda.milonative.model.Track
import org.json.JSONArray
import org.json.JSONObject

/** Online songs and radio stations the person liked or put in a playlist, kept on the phone so they survive a restart. */
class SavedStore(context: Context) {
    private val prefs = context.getSharedPreferences("milo_saved", Context.MODE_PRIVATE)

    fun load(): Map<Long, Track> = try {
        val arr = JSONArray(prefs.getString("all", "[]"))
        (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }.associateBy { it.id }
    } catch (e: Exception) {
        emptyMap()
    }

    fun save(m: Map<Long, Track>) {
        val arr = JSONArray()
        m.values.forEach { arr.put(toJson(it)) }
        prefs.edit().putString("all", arr.toString()).apply()
    }

    companion object {
        fun toJson(t: Track): JSONObject = JSONObject()
            .put("i", t.id).put("u", t.uri).put("t", t.title).put("a", t.artist).put("al", t.album)
            .put("ai", t.albumId).put("d", t.durationMs).put("y", t.year).put("art", t.artUrl)

        fun fromJson(o: JSONObject): Track {
            val id = o.getLong("i")
            return Track(
                id = id, uri = o.getString("u"), title = o.optString("t"), artist = o.optString("a"), album = o.optString("al"),
                albumId = o.optLong("ai", 4_000_000_000_000L + (-id)), durationMs = o.optLong("d"), trackNo = 0,
                year = o.optInt("y"), addedSec = 0L, artUrl = o.optString("art"),
            )
        }
    }
}
