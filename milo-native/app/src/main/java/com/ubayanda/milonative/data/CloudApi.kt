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

data class CloudProfile(val id: String, val username: String, val name: String, val termsV: Int, val avatar: String = "", val cover: String = "", val shareNp: Boolean = true, val sharePl: Boolean = true)

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
        /** Where the "confirm your email" link lands: the address of verified.html once it is hosted (leave empty to use the Site URL set in Supabase). */
        const val VERIFY_URL = ""
        const val URL = "https://lwcypxiwkgphuekygrps.supabase.co"
        const val KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imx3Y3lweGl3a2dwaHVla3lncnBzIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODQxMjU3NzEsImV4cCI6MjA5OTcwMTc3MX0.qCLKCluVfjkByX9GqRvxcihVl0Qo8eJOhfkVXN-nqAQ"
    }

    private fun loadSession(): CloudSession? {
        val at = SecureStore.get(prefs, "at")?.ifEmpty { null } ?: return null
        return CloudSession(
            at, SecureStore.get(prefs, "rt") ?: "", prefs.getLong("exp", 0L),
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
        SecureStore.put(SecureStore.put(prefs.edit(), "at", s.accessToken), "rt", s.refreshToken).putLong("exp", s.expiresAt)
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
                    com.ubayanda.milonative.util.ErrorLog.api(method, path, r.code)
                    throw CloudError(r.code, m.ifEmpty { "HTTP ${r.code}" })
                }
                tx
            }
        } catch (e: CloudError) {
            throw e
        } catch (e: Exception) {
            com.ubayanda.milonative.util.ErrorLog.api(method, path, 0)
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
                "/auth/v1/signup" + (if (VERIFY_URL.isNotEmpty()) "?redirect_to=" + java.net.URLEncoder.encode(VERIFY_URL, "UTF-8") else ""), "POST",
                JSONObject().put("email", email).put("password", password)
                    .put("data", JSONObject().put("username", username).put("name", username)),
                anon = true,
            )
        )
        if (!r.has("access_token")) return false
        storeSession(r, email)
        return true
    }

    /** Sign in (or up) with a Google ID token; the server must have the Google provider switched on. */
    suspend fun signInGoogle(idToken: String) {
        val r = request("/auth/v1/token?grant_type=id_token", "POST", JSONObject().put("provider", "google").put("id_token", idToken), anon = true)
        val j = JSONObject(r)
        storeSession(j, j.optJSONObject("user")?.optString("email").orEmpty())
    }

    /** Asks Supabase to email a password-reset link. The answer is the same whether or not the address has an account. */
    /** Ends every other login of this account (other phones, the web app). This device stays signed in. */
    suspend fun signOutOthers() {
        request("/auth/v1/logout?scope=others", "POST")
    }

    suspend fun recover(email: String) {
        request("/auth/v1/recover", "POST", JSONObject().put("email", email), anon = true)
    }

    suspend fun signIn(email: String, password: String) {
        val r = request("/auth/v1/token?grant_type=password", "POST", JSONObject().put("email", email).put("password", password), anon = true)
        storeSession(JSONObject(r), email)
    }

    /** True only when the server answered and said this account has no profile yet (not when we're offline). */
    @Volatile var profileMissing = false

    suspend fun loadProfile(): CloudProfile? {
        profileMissing = false
        val s = session ?: return null
        if (!fresh()) return null
        return try {
            val arr = JSONArray(request("/rest/v1/profiles?select=*&id=eq.${s.userId}"))
            if (arr.length() == 0) { profileMissing = true; null } else arr.getJSONObject(0).let {
                val av = it.optJSONObject("av")
                CloudProfile(
                    s.userId, it.optString("username"), it.optString("name"), it.optInt("terms_v", 0),
                    if (av != null && av.optString("t") == "img") av.optString("u") else "",
                    if (it.isNull("cover")) "" else it.optString("cover"),
                    it.optBoolean("share_np", true), it.optBoolean("share_pl", true),
                )
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

    suspend fun updateProfile(name: String, username: String) {
        val s = session ?: return
        if (!fresh()) throw CloudError(401, "Please log in again first.")
        request(
            "/rest/v1/profiles?id=eq.${s.userId}", "PATCH", JSONObject().put("name", name).put("username", username),
            headers = mapOf("Prefer" to "return=minimal"),
        )
    }

    /** Profile picture, in the same shape the web milo. uses ({"t":"img","u":data}); null puts the plain colour back. */
    suspend fun setShare(np: Boolean?, pl: Boolean?) {
        val s = session ?: return
        if (!fresh()) throw CloudError(401, "Please log in again first.")
        val b = JSONObject()
        if (np != null) b.put("share_np", np)
        if (pl != null) b.put("share_pl", pl)
        request("/rest/v1/profiles?id=eq.${s.userId}", "PATCH", b, headers = mapOf("Prefer" to "return=minimal"))
    }

    suspend fun setAvatar(data: String?) {
        val s = session ?: return
        if (!fresh()) throw CloudError(401, "Please log in again first.")
        val av = if (data != null) JSONObject().put("t", "img").put("u", data) else JSONObject().put("t", "color").put("c", "249,156,0")
        request("/rest/v1/profiles?id=eq.${s.userId}", "PATCH", JSONObject().put("av", av), headers = mapOf("Prefer" to "return=minimal"))
    }

    suspend fun setCover(data: String?) {
        val s = session ?: return
        if (!fresh()) throw CloudError(401, "Please log in again first.")
        request("/rest/v1/profiles?id=eq.${s.userId}", "PATCH", JSONObject().put("cover", data ?: JSONObject.NULL), headers = mapOf("Prefer" to "return=minimal"))
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
