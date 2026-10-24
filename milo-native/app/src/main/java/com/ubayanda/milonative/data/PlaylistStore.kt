package com.ubayanda.milonative.data

import android.content.Context
import com.ubayanda.milonative.model.Playlist
import org.json.JSONArray
import org.json.JSONObject

/** Playlists live on the phone (a small JSON in app storage). They only remember song ids, so no files are copied. */
class PlaylistStore(context: Context) {
    private val prefs = context.getSharedPreferences("milo_playlists", Context.MODE_PRIVATE)

    fun load(): List<Playlist> {
        val raw = prefs.getString("all", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val ids = o.getJSONArray("t")
                Playlist(o.getString("id"), o.getString("n"), (0 until ids.length()).map { ids.getLong(it) })
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(list: List<Playlist>) {
        val arr = JSONArray()
        list.forEach { p ->
            val o = JSONObject()
            o.put("id", p.id)
            o.put("n", p.name)
            val ids = JSONArray()
            p.trackIds.forEach { ids.put(it) }
            o.put("t", ids)
            arr.put(o)
        }
        prefs.edit().putString("all", arr.toString()).apply()
    }
}
