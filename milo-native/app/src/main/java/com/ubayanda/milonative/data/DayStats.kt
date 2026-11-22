package com.ubayanda.milonative.data

import android.content.Context
import java.util.Calendar

/** Listening time per calendar day, kept on the phone, for the Home stats widget and the streak. Starts counting from the first day this version runs. */
class DayStats(ctx: Context) {
    private val p = ctx.getSharedPreferences("milo_days", Context.MODE_PRIVATE)
    private val days = HashMap<Int, Long>()

    init {
        try { p.all.forEach { (k, v) -> if (k.startsWith("d") && v is Long) k.substring(1).toIntOrNull()?.let { days[it] = v } } } catch (_: Exception) {}
    }

    private fun key(c: Calendar) = c.get(Calendar.YEAR) * 10000 + (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH)
    private fun ago(n: Int): Int = Calendar.getInstance().also { it.add(Calendar.DAY_OF_YEAR, -n) }.let { key(it) }

    fun add(ms: Long) {
        if (ms <= 0L) return
        val k = ago(0)
        val v = (days[k] ?: 0L) + ms
        days[k] = v
        p.edit().putLong("d$k", v).apply()
    }

    fun todayMs(): Long = days[ago(0)] ?: 0L

    /** Listening time for the last [n] days, oldest first, today last. */
    fun lastDays(n: Int): List<Long> = (n - 1 downTo 0).map { days[ago(it)] ?: 0L }

    /** Days in a row with at least a minute of music. Today not having started yet doesn't break it. */
    fun streak(): Int {
        val minMs = 60_000L
        var i = if ((days[ago(0)] ?: 0L) >= minMs) 0 else 1
        var n = 0
        while ((days[ago(i)] ?: 0L) >= minMs && n < 3650) { n++; i++ }
        return n
    }
}
