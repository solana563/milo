package com.ubayanda.milonative.data

import org.json.JSONObject

/**
 * Remembers what the person added and removed, per item, with the time. When two phones merge, the newest action on each item wins,
 * so a song you unliked (or a playlist you deleted) on one phone no longer comes back from the other.
 * Items with no record are treated as present, so older data and older app versions behave as before.
 */
class SyncOps(private val ops: MutableMap<String, Pair<Long, Boolean>> = HashMap()) {
    fun record(key: String, alive: Boolean, ts: Long = System.currentTimeMillis()) {
        val old = ops[key]
        if (old == null || ts >= old.first) ops[key] = ts to alive
    }

    /** True only when the newest record for this item says it was removed. */
    fun dead(key: String): Boolean = ops[key]?.second == false

    /** Newest record per item wins. On an exact tie the item is kept: a leftover song is better than a lost one. */
    fun merge(other: SyncOps) {
        other.ops.forEach { (k, v) ->
            val mine = ops[k]
            if (mine == null || v.first > mine.first) ops[k] = v
            else if (v.first == mine.first && v.second != mine.second) ops[k] = v.first to true   // tie: keep the item
        }
    }

    /** Records older than a year go (a phone that hasn't synced for a year is treated as new). Alive and removed records age the same, so a removal can't outlive the re-add that cancelled it. */
    fun prune(now: Long = System.currentTimeMillis(), maxEntries: Int = 3000) {
        ops.entries.removeAll { (_, v) -> now - v.first > 365L * DAY }
        if (ops.size > maxEntries) ops.entries.sortedBy { it.value.first }.take(ops.size - maxEntries).forEach { ops.remove(it.key) }
    }

    val size: Int get() = ops.size

    fun toJson(): JSONObject {
        val o = JSONObject()
        ops.forEach { (k, v) -> o.put(k, org.json.JSONArray().put(v.first).put(if (v.second) 1 else 0)) }
        return o
    }

    companion object {
        private const val DAY = 86_400_000L
        fun fromJson(o: JSONObject?): SyncOps {
            val s = SyncOps()
            o?.keys()?.forEach { k -> o.optJSONArray(k)?.let { a -> if (a.length() >= 2) s.ops[k] = a.optLong(0) to (a.optInt(1) == 1) } }
            return s
        }
    }
}
