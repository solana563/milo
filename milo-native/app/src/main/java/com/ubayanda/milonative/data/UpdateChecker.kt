package com.ubayanda.milonative.data

import android.content.Context
import com.ubayanda.milonative.BuildConfig
import com.ubayanda.milonative.util.ErrorLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Tells the user when a newer milo. is out. It reads the newest GitHub Release of the repository the app was built from
 * (set by CI from GITHUB_REPOSITORY, or MILO_UPDATE_REPO) and looks for the file `milo.apk` on it.
 * It is off in the F-Droid build (F-Droid updates the app itself) and when no repository is known.
 */
object UpdateChecker {
    data class Update(val version: String, val notes: String, val apkUrl: String, val pageUrl: String)

    private const val APK_NAME = "milo.apk"
    private const val EVERY_MS = 12L * 60 * 60 * 1000
    private val http = OkHttpClient.Builder().connectTimeout(8, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).build()

    fun enabled() = BuildConfig.SELF_UPDATE && BuildConfig.UPDATE_REPO.isNotBlank()

    /** "v1.2.3-rc1" -> [1, 2, 3] */
    private fun parts(v: String) = v.trim().removePrefix("v").substringBefore('-').substringBefore('+').split('.').map { it.toIntOrNull() ?: 0 }

    fun isNewer(candidate: String, current: String): Boolean {
        val a = parts(candidate); val b = parts(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }; val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    /** The update to offer, or null. Asks GitHub at most every 12 hours (or always when [force]); a version the user chose "Not now" for is not offered again unless [force]. */
    suspend fun check(ctx: Context, force: Boolean = false): Update? = withContext(Dispatchers.IO) {
        if (!enabled()) return@withContext null
        val sp = ctx.getSharedPreferences("milo_update", 0)
        val now = System.currentTimeMillis()
        if (force || now - sp.getLong("last", 0L) > EVERY_MS) {
            val r = ErrorLog.catching { fetch() }
            r.getOrNull()?.let { u ->
                sp.edit().putLong("last", now).putString("v", u.version).putString("n", u.notes).putString("apk", u.apkUrl).putString("page", u.pageUrl).apply()
            } ?: run { if (r.isSuccess) sp.edit().putLong("last", now).remove("v").apply() }
        }
        val v = sp.getString("v", null) ?: return@withContext null
        if (!isNewer(v, BuildConfig.VERSION_NAME)) return@withContext null
        if (!force && sp.getString("dismissed", null) == v) return@withContext null
        Update(v, sp.getString("n", "").orEmpty(), sp.getString("apk", "").orEmpty(), sp.getString("page", "").orEmpty())
    }

    fun dismiss(ctx: Context, version: String) {
        ctx.getSharedPreferences("milo_update", 0).edit().putString("dismissed", version).apply()
    }

    private fun fetch(): Update? {
        val req = Request.Builder().url("https://api.github.com/repos/${BuildConfig.UPDATE_REPO}/releases/latest")
            .header("Accept", "application/vnd.github+json").build()
        http.newCall(req).execute().use { resp ->
            if (resp.code == 404) return null   // no release yet
            if (!resp.isSuccessful) { ErrorLog.add("update", "GitHub answered ${resp.code}"); return null }
            val j = JSONObject(resp.body?.string().orEmpty())
            val tag = j.optString("tag_name")
            if (tag.isBlank()) return null
            var apk = ""
            val assets = j.optJSONArray("assets")
            if (assets != null) for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                if (a.optString("name") == APK_NAME) apk = a.optString("browser_download_url")
            }
            val page = j.optString("html_url")
            if (apk.isBlank()) apk = page   // no apk attached: send them to the release page
            return Update(tag.removePrefix("v"), j.optString("body").trim(), apk, page)
        }
    }
}
