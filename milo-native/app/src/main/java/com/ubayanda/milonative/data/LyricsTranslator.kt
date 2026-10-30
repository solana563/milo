package com.ubayanda.milonative.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest

/** Translates lyric lines with MyMemory (free, no key). Results are cached so each song is only translated once per language. */
class LyricsTranslator(ctx: Context) {
    private val dir = File(ctx.cacheDir, "lyrtr").also { it.mkdirs() }

    private fun hash(s: String) = MessageDigest.getInstance("MD5").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }

    /** One translated string per input line (same order), or null when it couldn't be done. */
    suspend fun translate(lines: List<String>, lang: String): List<String>? = withContext(Dispatchers.IO) {
        if (lines.isEmpty()) return@withContext emptyList()
        val f = File(dir, hash(lang + "\n" + lines.joinToString("\n")) + ".json")
        try {
            if (f.exists()) {
                val a = JSONArray(f.readText())
                if (a.length() == lines.size) return@withContext List(a.length()) { a.getString(it) }
            }
        } catch (_: Exception) {}
        val out = arrayOfNulls<String>(lines.size)
        try {
            var i = 0
            while (i < lines.size) {
                val idx = ArrayList<Int>(); var n = 0
                while (i < lines.size && (idx.isEmpty() || n + lines[i].length < 420)) {
                    val s = lines[i].replace(Regex("\\s+"), " ").trim()
                    idx.add(i); n += s.length + 1; i++
                }
                val src = idx.map { lines[it].replace(Regex("\\s+"), " ").trim() }
                var parts = one(src.joinToString("\n"), lang).split("\n")
                if (parts.size != src.size) parts = src.map { if (it.isEmpty()) "" else one(it, lang) }
                idx.forEachIndexed { j, k -> out[k] = parts.getOrElse(j) { "" }.trim() }
            }
        } catch (_: Exception) { return@withContext null }
        val res = out.map { it ?: "" }
        try { f.writeText(JSONArray(res).toString()) } catch (_: Exception) {}
        res
    }

    private fun one(q: String, lang: String): String {
        if (q.isBlank()) return ""
        val url = "https://api.mymemory.translated.net/get?q=" + URLEncoder.encode(q, "UTF-8") + "&langpair=Autodetect%7C" + lang
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 8000; c.readTimeout = 10000
        c.setRequestProperty("User-Agent", "milo-native/1.0")
        val j = JSONObject(c.inputStream.bufferedReader().use { it.readText() })
        if (j.optInt("responseStatus") != 200) throw RuntimeException("limit")
        return j.getJSONObject("responseData").optString("translatedText")
    }

    companion object {
        val languages = listOf(
            "en" to "English", "es" to "Spanish", "fr" to "French", "de" to "German", "pt" to "Portuguese", "it" to "Italian",
            "yo" to "Yoruba", "ha" to "Hausa", "ig" to "Igbo", "sw" to "Swahili", "ar" to "Arabic", "hi" to "Hindi",
            "ja" to "Japanese", "ko" to "Korean", "zh" to "Chinese", "ru" to "Russian", "tr" to "Turkish",
        )
    }
}
