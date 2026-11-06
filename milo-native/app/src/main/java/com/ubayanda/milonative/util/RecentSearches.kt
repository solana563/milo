package com.ubayanda.milonative.util

import android.content.Context
import android.content.SharedPreferences

/** The last few things the person searched for, newest first. */
object RecentSearches {
    private var p: SharedPreferences? = null
    fun init(c: Context) { if (p == null) p = c.applicationContext.getSharedPreferences("milo_recent", Context.MODE_PRIVATE) }
    fun all(): List<String> = p?.getString("q", "").orEmpty().split("\n").filter { it.isNotBlank() }
    fun add(q: String) {
        val t = q.trim()
        if (t.length < 2) return
        val l = (listOf(t) + all().filter { !it.equals(t, true) }).take(8)
        p?.edit()?.putString("q", l.joinToString("\n"))?.apply()
    }
    fun clear() { p?.edit()?.remove("q")?.apply() }
}
