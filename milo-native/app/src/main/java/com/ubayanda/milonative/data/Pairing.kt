package com.ubayanda.milonative.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Lets someone else (a different account) control your player with play/pause/next/previous/seek, using a short code or QR.
 * What they may do is decided by the server (supabase_native_pairing.sql); nothing here is a credential.
 */
class Pairing(private val cloud: CloudApi) {
    class Person(val uid: String, val name: String, val expires: Long)
    class Lists(val controlling: List<Person>, val controllers: List<Person>)
    class Code(val code: String, val expires: Long)

    companion object {
        private const val ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
        fun link(code: String) = "milo://pair/$code"
        fun cleanCode(s: String): String = s.uppercase().filter { it in ALPHABET }.take(8)
        /** "milo://pair/ABCD2345", "abcd 2345" or "ABCD2345". Null when it can't be a pairing code. */
        fun parseCode(raw: String): String? = cleanCode(raw.trim().substringAfter("milo://pair/", raw.trim())).takeIf { it.length == 8 }

        /** What a scanned QR code is for: ("room" | "playlist" | "pair") to its code, or null. */
        fun route(raw: String): Pair<String, String>? {
            val t = raw.trim()
            return when {
                t.startsWith("milo://room/") -> Sessions.parseCode(t)?.let { "room" to it }
                t.startsWith("milo://playlist/") -> CPlaylists.parseCode(t)?.let { "playlist" to it }
                t.startsWith("milo://pair/") -> parseCode(t)?.let { "pair" to it }
                else -> null
            }
        }
        fun expiryText(expires: Long, now: Long = System.currentTimeMillis()): String {
            if (expires == 0L) return "until removed"
            val m = ((expires - now) / 60000).coerceAtLeast(0)
            return if (m < 60) "${m} min left" else if (m < 2880) "${m / 60} h left" else "${m / 1440} days left"
        }
    }

    private suspend fun rpc(name: String, body: JSONObject = JSONObject()): String = cloud.request("/rest/v1/rpc/$name", "POST", body)

    suspend fun create(hours: Int, myName: String): Code {
        val o = JSONObject(rpc("pair_create", JSONObject().put("p_hours", hours).put("p_my_name", myName)))
        return Code(o.getString("code"), o.optLong("expires"))
    }
    suspend fun redeem(code: String, myName: String): String = JSONObject(rpc("pair_redeem", JSONObject().put("p_code", code).put("p_my_name", myName))).optString("name")
    suspend fun revoke(other: String) { rpc("pair_revoke", JSONObject().put("p_other", other)) }

    suspend fun list(): Lists {
        val o = JSONObject(rpc("pair_list"))
        fun people(k: String): List<Person> = o.optJSONArray(k)?.let { a -> (0 until a.length()).map { val p = a.getJSONObject(it); Person(p.optString("uid"), p.optString("name"), p.optLong("expires")) } } ?: emptyList()
        return Lists(people("controlling"), people("controllers"))
    }

    suspend fun devices(owner: String): List<Connect.Device> {
        val a = JSONArray(rpc("pair_devices", JSONObject().put("p_owner", owner)))
        return (0 until a.length()).map { i ->
            val o = a.getJSONObject(i); val n = o.optJSONObject("now")
            Connect.Device(o.getString("id"), o.optString("name", "Device"), o.optString("kind", "phone"), o.optLong("last_seen"),
                if (n != null && n.optString("t").isNotEmpty()) Connect.Now(n.optString("t"), n.optString("a"), n.optString("img"), n.optLong("pos"), n.optLong("dur"), n.optBoolean("pl"), n.optLong("at"), emptyList()) else null)
        }
    }

    suspend fun send(owner: String, device: String, cmd: JSONObject) { rpc("pair_send", JSONObject().put("p_owner", owner).put("p_device", device).put("p_cmd", cmd)) }
}
