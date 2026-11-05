package com.ubayanda.milonative.data

import android.content.Context
import com.ubayanda.milonative.model.Track
import org.json.JSONObject

/** Title, artist and album fixes for songs on the phone. They are kept inside milo. (the audio files are not touched) and applied when the library loads. */
object MetaEdits {
    private var prefs: android.content.SharedPreferences? = null
    private var map = HashMap<Long, Triple<String, String, String>>()

    @Synchronized fun init(c: Context) {
        if (prefs != null) return
        val p = c.applicationContext.getSharedPreferences("milo_meta", Context.MODE_PRIVATE); prefs = p
        try {
            val o = JSONObject(p.getString("d", "{}") ?: "{}")
            o.keys().forEach { k -> val v = o.getJSONObject(k); k.toLongOrNull()?.let { map[it] = Triple(v.optString("t"), v.optString("a"), v.optString("al")) } }
        } catch (_: Exception) {}
    }

    private fun save() {
        val o = JSONObject()
        map.forEach { (k, v) -> o.put(k.toString(), JSONObject().put("t", v.first).put("a", v.second).put("al", v.third)) }
        prefs?.edit()?.putString("d", o.toString())?.apply()
    }

    fun has(id: Long) = map.containsKey(id)
    fun set(id: Long, title: String, artist: String, album: String) { map[id] = Triple(title.trim(), artist.trim(), album.trim()); save() }
    fun reset(id: Long) { if (map.remove(id) != null) save() }

    fun apply(list: List<Track>): List<Track> =
        if (map.isEmpty()) list else list.map { t ->
            val e = map[t.id] ?: return@map t
            t.copy(title = e.first.ifEmpty { t.title }, artist = e.second.ifEmpty { t.artist }, album = e.third.ifEmpty { t.album })
        }
}
