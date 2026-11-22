package com.ubayanda.milonative.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** The last playlist imports (file or link): what was made, how many songs came from where, and the songs that weren't found. Kept on the phone only. */
object ImportLog {
    data class Entry(val name: String, val at: Long, val onPhone: Int, val fromYoutube: Int, val missing: List<String>)

    private const val PREFS = "milo_imports"
    private const val KEEP = 15
    private const val KEEP_MISSING = 100

    fun all(ctx: Context): List<Entry> = try {
        val a = JSONArray(ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("log", "[]"))
        (0 until a.length()).map { i ->
            val o = a.getJSONObject(i)
            val m = o.optJSONArray("m")
            Entry(o.optString("n"), o.optLong("t"), o.optInt("p"), o.optInt("y"), (0 until (m?.length() ?: 0)).map { m!!.getString(it) })
        }
    } catch (_: Exception) { emptyList() }

    fun add(ctx: Context, e: Entry) {
        val l = (listOf(e) + all(ctx)).take(KEEP)
        val a = JSONArray()
        l.forEach { x ->
            a.put(JSONObject().put("n", x.name).put("t", x.at).put("p", x.onPhone).put("y", x.fromYoutube).put("m", JSONArray(x.missing.take(KEEP_MISSING))))
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("log", a.toString()).apply()
    }

    fun clear(ctx: Context) { ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove("log").apply() }
}
