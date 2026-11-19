package com.ubayanda.milonative.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * A short "about" text for an artist or album from Wikipedia's summary service (no account, no key). Idea from Mochy Music
 * (GPL-3.0, github.com/realidkroo/Mochy-Music, utils/Wikipedia.kt); written for milo. Blocking: call off the main thread.
 * Wikipedia text is CC BY-SA; the page shows "Wikipedia" under it.
 */
object WikiAbout {
    private val cache = HashMap<String, String?>()

    private fun summary(title: String): String? = try {
        val u = "https://en.wikipedia.org/api/rest_v1/page/summary/" + URLEncoder.encode(title.replace(' ', '_'), "UTF-8").replace("+", "%20")
        val c = URL(u).openConnection() as HttpURLConnection
        c.connectTimeout = 6000; c.readTimeout = 8000
        c.setRequestProperty("User-Agent", "milo-native/1.0 (+https://github.com/ubayanda)")
        if (c.responseCode != 200) null else {
            val j = JSONObject(c.inputStream.bufferedReader().use { it.readText() })
            j.optString("extract").takeIf { it.isNotBlank() && j.optString("type") != "disambiguation" && !it.contains("may refer to", true) }
        }
    } catch (_: Exception) { null }

    /** kind is "artist" or "album" (anything else is looked up by name alone) */
    fun fetch(kind: String, name: String, artist: String? = null): String? {
        val key = "$kind|$name|$artist"
        synchronized(cache) { if (cache.containsKey(key)) return cache[key] }
        val tries = when (kind) {
            "album" -> listOfNotNull(artist?.let { "$name ($it album)" }, artist?.let { "$name ($it)" }, "$name (album)")
            "artist" -> listOf("$name (musician)", "$name (singer)", "$name (band)", name)
            else -> listOf(name)
        }
        var out: String? = null
        for (t in tries) {
            val s = summary(t) ?: continue
            // a bare title can land on something that isn't music: for artists, the page should talk about music
            if (kind == "artist" && t == name && !Regex("singer|rapper|band|musician|songwriter|producer|DJ|composer|duo|group", RegexOption.IGNORE_CASE).containsMatchIn(s)) continue
            out = s; break
        }
        synchronized(cache) { cache[key] = out }
        return out
    }
}
