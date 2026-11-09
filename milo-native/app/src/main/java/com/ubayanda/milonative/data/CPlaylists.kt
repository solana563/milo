package com.ubayanda.milonative.data

import com.ubayanda.milonative.model.Track
import org.json.JSONArray
import org.json.JSONObject

class CplSummary(val id: String, val name: String, val code: String, val role: String, val isPublic: Boolean, val count: Int)
class CplPublic(val name: String, val code: String, val owner: String, val count: Int)
class CplMember(val uid: String, val name: String, val role: String)
class CplItem(val id: Long, val track: Track, val by: String, val byUid: String)
class CplHistory(val name: String, val action: String, val detail: String, val at: Long)
class CplDetail(
    val id: String, val name: String, val code: String, val isPublic: Boolean, val defaultRole: String, val ownerUid: String,
    val myUid: String, val myRole: String, val members: List<CplMember>, val items: List<CplItem>, val history: List<CplHistory>,
)

/**
 * Client for shared playlists. The server decides what each role may do (see supabase_native_playlists.sql);
 * the helpers below only decide which buttons to show.
 */
class CPlaylists(private val cloud: CloudApi) {
    companion object {
        fun link(code: String) = "milo://playlist/$code"
        /** Takes "milo://playlist/ABC234", "ABC 234" or "abc234". Null when it can't be a code. */
        fun parseCode(raw: String): String? {
            val t = raw.trim().substringAfter("milo://playlist/", raw.trim())
            return Sessions.cleanCode(t).takeIf { it.length == 6 }
        }
        fun canAdd(role: String) = role == "owner" || role == "editor"
        fun canReorder(role: String) = role == "owner" || role == "editor"
        /** Owners remove anything; editors only what they added. */
        fun canRemove(role: String, addedBy: String, me: String) = role == "owner" || (role == "editor" && addedBy == me)
        fun roleLabel(role: String) = when (role) { "owner" -> "Owner"; "editor" -> "Can edit"; else -> "View only" }
        /** The new neighbour to pass to the server when moving the item at [from] to [to] (list positions). Null = to the top. */
        fun afterIdFor(ids: List<Long>, from: Int, to: Int): Long? {
            if (from !in ids.indices || to !in ids.indices || from == to) return ids.getOrNull(from)
            val rest = ids.toMutableList().also { it.removeAt(from) }
            return if (to == 0) null else rest[to - 1]
        }
    }

    private suspend fun rpc(name: String, body: JSONObject = JSONObject()): String = cloud.request("/rest/v1/rpc/$name", "POST", body)

    suspend fun create(name: String, public: Boolean, defaultRole: String, myName: String): Pair<String, String> {
        val o = JSONObject(rpc("cpl_create", JSONObject().put("p_name", name).put("p_public", public).put("p_default_role", defaultRole).put("p_my_name", myName)))
        return o.getString("id") to o.getString("code")
    }
    suspend fun join(code: String, myName: String): String = JSONObject(rpc("cpl_join", JSONObject().put("p_code", code).put("p_my_name", myName))).getString("id")
    suspend fun leave(pl: String) { rpc("cpl_leave", JSONObject().put("p_pl", pl)) }
    suspend fun delete(pl: String) { rpc("cpl_delete", JSONObject().put("p_pl", pl)) }
    suspend fun add(pl: String, t: Track) { rpc("cpl_add", JSONObject().put("p_pl", pl).put("p_track", SavedStore.toJson(t))) }
    suspend fun remove(item: Long) { rpc("cpl_remove", JSONObject().put("p_item", item)) }
    suspend fun move(item: Long, after: Long?) { rpc("cpl_move", JSONObject().put("p_item", item).put("p_after", after ?: JSONObject.NULL)) }
    suspend fun setRole(pl: String, uid: String, role: String) { rpc("cpl_set_role", JSONObject().put("p_pl", pl).put("p_uid", uid).put("p_role", role)) }
    suspend fun settings(pl: String, name: String, public: Boolean, defaultRole: String) {
        rpc("cpl_settings", JSONObject().put("p_pl", pl).put("p_name", name).put("p_public", public).put("p_default_role", defaultRole))
    }

    suspend fun list(): List<CplSummary> {
        val a = JSONArray(rpc("cpl_list"))
        return (0 until a.length()).map { val o = a.getJSONObject(it); CplSummary(o.optString("id"), o.optString("name"), o.optString("code"), o.optString("role"), o.optBoolean("public"), o.optInt("count")) }
    }
    suspend fun publicLists(): List<CplPublic> {
        val a = JSONArray(rpc("cpl_public"))
        return (0 until a.length()).map { val o = a.getJSONObject(it); CplPublic(o.optString("name"), o.optString("code"), o.optString("owner"), o.optInt("count")) }
    }

    suspend fun get(pl: String): CplDetail {
        val j = JSONObject(rpc("cpl_get", JSONObject().put("p_pl", pl)))
        val p = j.getJSONObject("pl"); val me = j.getJSONObject("me")
        fun <T> list(k: String, f: (JSONObject) -> T?): List<T> = j.optJSONArray(k)?.let { a -> (0 until a.length()).mapNotNull { f(a.getJSONObject(it)) } } ?: emptyList()
        return CplDetail(
            p.getString("id"), p.optString("name"), p.optString("code"), p.optBoolean("public"), p.optString("default_role", "editor"), p.optString("owner"),
            me.optString("uid"), me.optString("role", "viewer"),
            list("members") { CplMember(it.optString("uid"), it.optString("name"), it.optString("role")) },
            list("items") { i -> try { CplItem(i.getLong("id"), SavedStore.fromJson(i.getJSONObject("track")), i.optString("by"), i.optString("uid")) } catch (_: Exception) { null } },
            list("history") { CplHistory(it.optString("name"), it.optString("action"), it.optString("detail"), it.optLong("at")) },
        )
    }
}
