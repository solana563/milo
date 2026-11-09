package com.ubayanda.milonative.data

import android.content.Context
import com.ubayanda.milonative.model.Track
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * milo. Connect: your own devices, signed in to the same account, can see what is playing and control each other.
 * Everything goes through two Supabase tables protected by Row Level Security (see supabase_native_connect.sql): only the
 * signed-in owner can read or write them. A device id is a random number made on this install; it is not a password and no
 * login token ever leaves the phone. Connect is OFF until the person switches it on.
 */
class Connect(private val cloud: CloudApi, ctx: Context) {
    class Now(val title: String, val artist: String, val img: String, val posMs: Long, val durMs: Long, val playing: Boolean, val at: Long, val queue: List<Track>)
    class Device(val id: String, val name: String, val kind: String, val lastSeen: Long, val now: Now?)

    private val p = ctx.getSharedPreferences("milo_connect", Context.MODE_PRIVATE)
    val deviceId: String = p.getString("id", null) ?: UUID.randomUUID().toString().also { p.edit().putString("id", it).apply() }
    val deviceName: String = android.os.Build.MANUFACTURER.replaceFirstChar { it.uppercase() } + " " + android.os.Build.MODEL
    var enabled: Boolean
        get() = p.getBoolean("on", false)
        set(v) { p.edit().putBoolean("on", v).apply() }

    companion object {
        const val ONLINE_MS = 45_000L
        const val CMD_TTL_MS = 30_000L
        /** A device counts as online when it reported in recently. Clocks of signed-in phones are normally network-synced. */
        fun online(lastSeen: Long, now: Long) = lastSeen > 0 && now - lastSeen <= ONLINE_MS
        /** Commands wait only briefly: a "pause" from five minutes ago must never run now. */
        fun fresh(created: Long, now: Long) = created > 0 && now - created <= CMD_TTL_MS && created - now < 10_000L
        /** Where the song is now, given when the position was reported. */
        fun livePos(posMs: Long, at: Long, playing: Boolean, now: Long, durMs: Long) =
            (if (playing) posMs + (now - at).coerceAtLeast(0L) else posMs).coerceIn(0L, if (durMs > 0) durMs else Long.MAX_VALUE)
    }

    suspend fun heartbeat(now: JSONObject) {
        val s = cloud.session ?: return
        cloud.request(
            "/rest/v1/milo_devices?on_conflict=id", "POST",
            JSONObject().put("id", deviceId).put("uid", s.userId).put("name", deviceName).put("kind", "phone")
                .put("last_seen", System.currentTimeMillis()).put("now", now),
            headers = mapOf("Prefer" to "resolution=merge-duplicates,return=minimal"),
        )
    }

    suspend fun devices(): List<Device> {
        val a = JSONArray(cloud.request("/rest/v1/milo_devices?select=id,name,kind,last_seen,now&order=last_seen.desc"))
        return (0 until a.length()).map { i ->
            val o = a.getJSONObject(i)
            val n = o.optJSONObject("now")
            Device(o.getString("id"), o.optString("name", "Device"), o.optString("kind", "phone"), o.optLong("last_seen"),
                if (n != null && n.optString("t").isNotEmpty()) Now(
                    n.optString("t"), n.optString("a"), n.optString("img"), n.optLong("pos"), n.optLong("dur"), n.optBoolean("pl"), n.optLong("at"),
                    n.optJSONArray("q")?.let { q -> (0 until q.length()).mapNotNull { j -> try { SavedStore.fromJson(q.getJSONObject(j)) } catch (_: Exception) { null } } } ?: emptyList(),
                ) else null)
        }
    }

    suspend fun send(target: String, cmd: JSONObject) {
        val s = cloud.session ?: return
        cloud.request(
            "/rest/v1/milo_commands", "POST",
            JSONObject().put("uid", s.userId).put("target", target).put("from_device", deviceId).put("cmd", cmd).put("created", System.currentTimeMillis()),
            headers = mapOf("Prefer" to "return=minimal"),
        )
    }

    /** Commands waiting for this device. They are removed from the queue as they are read, so each runs once. */
    suspend fun take(): List<JSONObject> {
        val a = JSONArray(cloud.request("/rest/v1/milo_commands?target=eq.$deviceId&order=id.asc&select=id,cmd,created"))
        if (a.length() == 0) return emptyList()
        val ids = (0 until a.length()).joinToString(",") { a.getJSONObject(it).getLong("id").toString() }
        cloud.request("/rest/v1/milo_commands?id=in.($ids)", "DELETE")
        val now = System.currentTimeMillis()
        return (0 until a.length()).map { a.getJSONObject(it) }.filter { fresh(it.optLong("created"), now) }.mapNotNull { it.optJSONObject("cmd") }
    }

    /** Stop appearing in the account's device list (switched off, or signing out). */
    suspend fun remove() {
        if (cloud.session == null) return
        cloud.request("/rest/v1/milo_devices?id=eq.$deviceId", "DELETE")
    }
}
