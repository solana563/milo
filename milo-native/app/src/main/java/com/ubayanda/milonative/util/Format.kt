package com.ubayanda.milonative.util

fun fmtTime(ms: Long): String {
    val s = (ms.coerceAtLeast(0L) / 1000L).toInt()
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}

fun greeting(hour: Int): String = when {
    hour < 12 -> "Good morning"
    hour < 18 -> "Good afternoon"
    else -> "Good evening"
}

fun plural(n: Int, one: String, many: String = one + "s"): String = if (n == 1) "$n $one" else "$n $many"
