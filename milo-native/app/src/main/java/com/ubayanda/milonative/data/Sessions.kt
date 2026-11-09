package com.ubayanda.milonative.data

import com.ubayanda.milonative.model.Track
import org.json.JSONArray
import org.json.JSONObject

/** One person in a session. [online] means they reported in during the last few seconds. */
class Member(val uid: String, val name: String, val role: String, val muted: Boolean, val online: Boolean) { val host get() = role == "host" }
class QueueItem(val id: Long, val by: String, val byUid: String, val track: Track, val status: String)
class RoomEvent(val id: Long, val uid: String, val name: String, val kind: String, val body: String, val at: Long)
class PublicRoom(val name: String, val code: String, val host: String, val listeners: Int, val now: String)

/** What the host is playing. [track] is null when it is a song from the host's own storage, which nobody else can hear. */
class HostState(val track: Track?, val title: String, val artist: String, val posMs: Long, val playing: Boolean, val at: Long)

class RoomSnap(
    val id: String, val name: String, val code: String, val isPublic: Boolean, val openQueue: Boolean, val hostSeen: Long, val hostUid: String,
    val state: HostState?, val myUid: String, val myRole: String, val iAmMuted: Boolean,
    val members: List<Member>, val queue: List<QueueItem>, val events: List<RoomEvent>,
    /** null while the room is live; otherwise why it ended for me: "left", "removed" or "ended". */
    val gone: String? = null,
) { val isHost get() = myRole == "host" }

/**
 * Client for milo. Sessions. The server is the only authority: every call goes through a database function that checks
 * who you are, whether you are in the room, whether you are the host, muted or banned, and how fast you are writing
 * (see supabase_native_sessions.sql). This class just asks and parses.
 */
class Sessions(private val cloud: CloudApi) {
    companion object {
        /** Roman letters and digits without 0/O/1/I/L, as the server makes them. */
        fun cleanCode(s: String): String = s.uppercase().filter { it in "ABCDEFGHJKMNPQRSTUVWXYZ23456789" }.take(6)
        fun link(code: String) = "milo://room/$code"
        /** Takes "milo://room/ABC234", "ABC 234" or "abc234". Null when it can't be a code. */
        fun parseCode(raw: String): String? {
            val t = raw.trim().substringAfter("milo://room/", raw.trim())
            return cleanCode(t).takeIf { it.length == 6 }
        }
        /** Only YouTube songs can play on someone else's phone. */
        fun playable(t: Track?) = t != null && t.uri.startsWith("yt:")
        /** Should a listener move their player? Returns the position to seek to, or -1 when already in sync. */
        fun seekTarget(hostPos: Long, hostAt: Long, hostPlaying: Boolean, myPos: Long, now: Long, driftMs: Long = 3000L): Long {
            val target = if (hostPlaying) hostPos + (now - hostAt).coerceIn(0L, 20_000L) else hostPos
            return if (kotlin.math.abs(myPos - target) > driftMs) target else -1L
        }
        /** The host counts as away after a minute without a heartbeat. */
        fun hostAway(hostSeen: Long, now: Long) = hostSeen > 0 && now - hostSeen > 60_000L
    }

    private suspend fun rpc(name: String, body: JSONObject = JSONObject()): String = cloud.request("/rest/v1/rpc/$name", "POST", body)

    suspend fun create(name: String, public: Boolean, openQueue: Boolean, myName: String): Pair<String, String> {
        val o = JSONObject(rpc("create_room", JSONObject().put("p_name", name).put("p_public", public).put("p_open_queue", openQueue).put("p_my_name", myName)))
        return o.getString("id") to o.getString("code")
    }

    suspend fun join(code: String, myName: String): Pair<String, String> {
        val o = JSONObject(rpc("join_room", JSONObject().put("p_code", code).put("p_my_name", myName)))
        return o.getString("id") to o.getString("code")
    }

    suspend fun leave(room: String) { rpc("leave_room", JSONObject().put("p_room", room)) }
    suspend fun end(room: String) { rpc("end_room", JSONObject().put("p_room", room)) }
    suspend fun push(room: String, state: JSONObject) { rpc("host_push", JSONObject().put("p_room", room).put("p_state", state)) }
    suspend fun suggest(room: String, track: Track) { rpc("suggest_track", JSONObject().put("p_room", room).put("p_track", SavedStore.toJson(track))) }
    suspend fun queueAction(id: Long, action: String) { rpc("queue_action", JSONObject().put("p_id", id).put("p_action", action)) }
    suspend fun react(room: String, kind: String) { rpc("post_event", JSONObject().put("p_room", room).put("p_kind", "react").put("p_body", kind)) }
    suspend fun say(room: String, text: String) { rpc("post_event", JSONObject().put("p_room", room).put("p_kind", "msg").put("p_body", text)) }
    suspend fun moderate(room: String, uid: String, action: String) { rpc("moderate", JSONObject().put("p_room", room).put("p_target", uid).put("p_action", action)) }
    suspend fun report(room: String, uid: String, reason: String) { rpc("report_user", JSONObject().put("p_room", room).put("p_target", uid).put("p_reason", reason)) }

    suspend fun publicRooms(): List<PublicRoom> {
        val a = JSONArray(rpc("public_rooms"))
        return (0 until a.length()).map { val o = a.getJSONObject(it); PublicRoom(o.optString("name"), o.optString("code"), o.optString("host"), o.optInt("listeners"), o.optString("now", "")) }
    }

    suspend fun snapshot(room: String, sinceEvent: Long): RoomSnap {
        val j = JSONObject(rpc("room_snapshot", JSONObject().put("p_room", room).put("p_since", sinceEvent)))
        j.optString("gone", "").takeIf { it.isNotEmpty() }?.let { return RoomSnap(room, "", "", false, false, 0, "", null, "", "member", false, emptyList(), emptyList(), emptyList(), gone = it) }
        val r = j.getJSONObject("room"); val me = j.getJSONObject("me")
        val st = r.optJSONObject("state")
        val tj = st?.optJSONObject("t")
        val hs = if (st != null && st.length() > 0 && (st.has("title") || tj != null)) HostState(
            tj?.let { try { SavedStore.fromJson(it) } catch (_: Exception) { null } }, st.optString("title", tj?.optString("title").orEmpty()), st.optString("artist", ""),
            st.optLong("pos"), st.optBoolean("pl"), st.optLong("at"),
        ) else null
        fun <T> list(k: String, f: (JSONObject) -> T?): List<T> = j.optJSONArray(k)?.let { a -> (0 until a.length()).mapNotNull { f(a.getJSONObject(it)) } } ?: emptyList()
        return RoomSnap(
            r.getString("id"), r.optString("name"), r.optString("code"), r.optBoolean("public"), r.optBoolean("open"), r.optLong("host_seen"), r.optString("host"),
            hs, me.optString("uid"), me.optString("role", "member"), me.optBoolean("muted"),
            list("members") { Member(it.optString("uid"), it.optString("name"), it.optString("role"), it.optBoolean("muted"), it.optBoolean("on")) },
            list("queue") { q -> try { QueueItem(q.getLong("id"), q.optString("by"), q.optString("uid"), SavedStore.fromJson(q.getJSONObject("track")), q.optString("status")) } catch (_: Exception) { null } },
            list("events") { RoomEvent(it.getLong("id"), it.optString("uid"), it.optString("name"), it.optString("kind"), it.optString("body"), it.optLong("at")) },
        )
    }
}
