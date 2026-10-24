package com.ubayanda.milonative.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class CloudError(val status: Int, message: String) : Exception(message)

data class CloudSession(val accessToken: String, val refreshToken: String, val expiresAt: Long, val userId: String, val email: String)

data class CloudProfile(val id: String, val username: String, val name: String, val termsV: Int)

/**
 * Talks to the same Supabase project as the web milo., so one account works in both.
 * The key below is the project's public "anon" key (the web app ships the same one); row-level rules protect the data.
 */
class CloudApi(context: Context) {
    private val prefs = context.getSharedPreferences("milo_cloud", Context.MODE_PRIVATE)
    private val http = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS).readTimeout(12, TimeUnit.SECONDS).writeTimeout(12, TimeUnit.SECONDS).build()
    private val json = "application/json".toMediaType()

    var session: CloudSession? = loadSession()
        private set

    companion object {
        const val TERMS_V = 1
        private const val URL = "https://lwcypxiwkgphuekygrps.supabase.co"
        private const val KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imx3Y3lweGl3a2dwaHVla3lncnBzIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODQxMjU3NzEsImV4cCI6MjA5OTcwMTc3MX0.qCLKCluVfjkByX9GqRvxcihVl0Qo8eJOhfkVXN-nqAQ"
    }

    private fun loadSession(): CloudSession? {
        val at = prefs.getString("at", null) ?: return null
        return CloudSession(
            at, prefs.getString("rt", "") ?: "", prefs.getLong("exp", 0L),
            prefs.getString("uid", "") ?: "", prefs.getString("email", "") ?: "",
        )
    }

    private fun storeSession(j: JSONObject, fallbackEmail: String = "") {
        val user = j.optJSONObject("user")
        val s = CloudSession(
            j.getString("access_token"), j.optString("refresh_token", ""),
            System.currentTimeMillis() + j.optLong("expires_in", 3600L) * 1000 - 30_000,
            user?.optString("id").orEmpty().ifEmpty { session?.userId.orEmpty() },
            user?.optString("email").orEmpty().ifEmpty { session?.email ?: fallbackEmail },
        )
        session = s
        prefs.edit().putString("at", s.accessToken).putString("rt", s.refreshToken).putLong("exp", s.expiresAt)
            .putString("uid", s.userId).putString("email", s.email).apply()
    }

    fun signOutLocal() {
        session = null
        prefs.edit().clear().apply()
    }

    suspend fun request(
        path: String, method: String = "GET", body: JSONObject? = null, anon: Boolean = false,
        headers: Map<String, String> = emptyMap(),
    ): String = withContext(Dispatchers.IO) {
        val token = if (anon || session == null) KEY else session!!.accessToken
        val rb = when {
            body != null -> body.toString().toRequestBody(json)
            method == "GET" || method == "HEAD" -> null
            else -> "".toRequestBody(json)
        }
        val b = Request.Builder().url(URL + path).method(method, rb)
            .header("apikey", KEY).header("Authorization", "Bearer $token").header("Content-Type", "application/json")
        headers.forEach { (k, v) -> b.header(k, v) }
        try {
            http.newCall(b.build()).execute().use { r ->
                val tx = r.body?.string().orEmpty()
                if (!r.isSuccessful) {
                    val m = try {
                        val o = JSONObject(tx)
                        listOf("msg", "error_description", "message", "error").map { o.optString(it) }.firstOrNull { it.isNotEmpty() }.orEmpty()
                    } catch (e: Exception) { "" }
                    throw CloudError(r.code, m.ifEmpty { "HTTP ${r.code}" })
                }
                tx
            }
        } catch (e: CloudError) {
            throw e
        } catch (e: Exception) {
            throw CloudError(0, "Couldn't reach the server. Check your internet connection.")
        }
    }

    /** Make sure the access token is still valid (refresh it if it expired). False when the session is gone. */
    suspend fun fresh(): Boolean {
        val s = session ?: return false
        if (System.currentTimeMillis() < s.expiresAt) return true
        return try {
            val r = request("/auth/v1/token?grant_type=refresh_token", "POST", JSONObject().put("refresh_token", s.refreshToken), anon = true)
            storeSession(JSONObject(r))
            true
        } catch (e: CloudError) {
            if (e.status in listOf(400, 401, 403)) signOutLocal()
            false
        }
    }

    suspend fun usernameFree(u: String): Boolean =
        request("/rest/v1/rpc/username_free", "POST", JSONObject().put("u", u), anon = true).trim() != "false"

    /** Returns true when signed in straight away, false when the email must be confirmed first. */
    suspend fun signUp(email: String, password: String, username: String): Boolean {
        val r = JSONObject(
            request(
                "/auth/v1/signup", "POST",
                JSONObject().put("email", email).put("password", password)
                    .put("data", JSONObject().put("username", username).put("name", username)),
                anon = true,
            )
        )
        if (!r.has("access_token")) return false
        storeSession(r, email)
        return true
    }

    suspend fun signIn(email: String, password: String) {
        val r = request("/auth/v1/token?grant_type=password", "POST", JSONObject().put("email", email).put("password", password), anon = true)
        storeSession(JSONObject(r), email)
    }

    suspend fun loadProfile(): CloudProfile? {
        val s = session ?: return null
        if (!fresh()) return null
        return try {
            val arr = JSONArray(request("/rest/v1/profiles?select=*&id=eq.${s.userId}"))
            if (arr.length() == 0) null else arr.getJSONObject(0).let {
                CloudProfile(s.userId, it.optString("username"), it.optString("name"), it.optInt("terms_v", 0))
            }
        } catch (e: CloudError) {
            null
        }
    }

    suspend fun createProfile(username: String) {
        val s = session ?: return
        request(
            "/rest/v1/profiles", "POST", JSONObject().put("id", s.userId).put("username", username).put("name", username),
            headers = mapOf("Prefer" to "return=minimal"),
        )
    }

    suspend fun agreeTerms() {
        val s = session ?: return
        val now = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(java.util.Date())
        request(
            "/rest/v1/profiles?id=eq.${s.userId}", "PATCH", JSONObject().put("terms_v", TERMS_V).put("terms_at", now),
            headers = mapOf("Prefer" to "return=minimal"),
        )
    }

    fun friendly(e: Exception): String {
        val m = e.message.orEmpty()
        return when {
            Regex("Invalid login", RegexOption.IGNORE_CASE).containsMatchIn(m) -> "Wrong email or password."
            Regex("already registered|already been registered", RegexOption.IGNORE_CASE).containsMatchIn(m) -> "That email already has an account. Log in instead."
            Regex("not confirmed", RegexOption.IGNORE_CASE).containsMatchIn(m) -> "Confirm your email first, then log in."
            Regex("password", RegexOption.IGNORE_CASE).containsMatchIn(m) && Regex("6|short|weak", RegexOption.IGNORE_CASE).containsMatchIn(m) -> "Use a password with at least 6 characters."
            else -> m.ifEmpty { "Something went wrong. Try again." }
        }
    }
}
