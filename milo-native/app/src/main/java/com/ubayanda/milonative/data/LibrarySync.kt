package com.ubayanda.milonative.data

import org.json.JSONArray
import org.json.JSONObject

class RemoteLibrary(val ts: Long, val data: JSONObject)

/**
 * Backs up likes, playlists and saved stations to the person's account (table milo_native_sync, see supabase_native_sync.sql).
 * Songs on the phone are matched by title + artist so the same library works on another phone.
 */
class LibrarySync(private val cloud: CloudApi) {
    /** null = nothing stored yet. Throws CloudError when the table is missing or the network is down. */
    suspend fun pull(): RemoteLibrary? {
        val s = cloud.session ?: return null
        val arr = JSONArray(cloud.request("/rest/v1/milo_native_sync?select=data,ts&uid=eq.${s.userId}"))
        if (arr.length() == 0) return null
        val o = arr.getJSONObject(0)
        return RemoteLibrary(o.optLong("ts"), o.optJSONObject("data") ?: JSONObject())
    }

    suspend fun push(ts: Long, data: JSONObject) {
        val s = cloud.session ?: return
        cloud.request(
            "/rest/v1/milo_native_sync?on_conflict=uid", "POST",
            JSONObject().put("uid", s.userId).put("data", data).put("ts", ts),
            headers = mapOf("Prefer" to "resolution=merge-duplicates,return=minimal"),
        )
    }

    /** Wipes the account (needs the delete_my_account function from the SQL file), then forgets the login on this phone. */
    suspend fun deleteAccount() {
        cloud.request("/rest/v1/rpc/delete_my_account", "POST", JSONObject())
        cloud.signOutLocal()
    }

    companion object {
        fun norm(s: String) = s.lowercase().trim().replace(Regex("\\s+"), " ")
        fun key(title: String, artist: String) = norm(title) + "|" + norm(artist)
        /** Signed server addresses carry a login hash, so they stay on the phone. */
        fun syncable(uri: String) = !uri.contains("/rest/stream")
    }
}
