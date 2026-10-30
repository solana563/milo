package com.ubayanda.milonative.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.time.Instant
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

/** "friend" = accepted, "incoming" = they asked you, "outgoing" = you asked them. */
data class Friend(val id: String, val username: String, val name: String, val status: String)

data class ChatMsg(
    val id: Long, val mine: Boolean, val kind: String, val body: String, val at: Long, val read: Boolean,
    val replyTo: Long? = null, val rx: Map<String, String> = emptyMap(),
) {
    /** What to show: text as is, a shared song as "♪ title · artist". */
    val shown: String
        get() = when (kind) {
            "text" -> body
            "song" -> try {
                val o = JSONObject(body)
                "♪ " + o.optString("t", "Song") + o.optString("a").let { if (it.isNotEmpty()) " · $it" else "" }
            } catch (e: Exception) { "♪ Song" }
            "img" -> "📷 Photo"
            "voice" -> "🎤 Voice message"
            else -> body
        }
}

/** A song a friend shares: just its description. [local] ones only exist on their phone, so they can be seen but not played. */
data class SharedSong(val title: String, val artist: String, val id: String, val url: String, val img: String, val local: Boolean, val durSec: Int, val yt: String)
data class SharedPlaylist(val id: String, val name: String, val songs: List<SharedSong>)
data class FriendShare(val now: SharedSong?, val playlists: List<SharedPlaylist>)

data class SocialUi(
    val loading: Boolean = false,
    val friends: List<Friend> = emptyList(),
    val err: String = "",
    val note: String = "",
    val unread: Set<String> = emptySet(),
    val shared: Map<String, FriendShare> = emptyMap(),
)

/**
 * Friends and chat on the same Supabase tables as the web milo. (friendships, messages), so friends and
 * conversations are shared between the web app and this one. Messages live 24 hours, like the web.
 */
class SocialModel(private val cloud: CloudApi, private val scope: CoroutineScope) {
    private val _ui = MutableStateFlow(SocialUi())
    val ui: StateFlow<SocialUi> = _ui.asStateFlow()

    private val _msgs = MutableStateFlow<List<ChatMsg>>(emptyList())
    val msgs: StateFlow<List<ChatMsg>> = _msgs.asStateFlow()

    private var poll: Job? = null
    private var chatWith: String? = null

    val me get() = cloud.session?.userId.orEmpty()
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
    private fun chatId(f: String) = if (me < f) "$me:$f" else "$f:$me"
    private fun dayAgo() = enc(Instant.now().minus(24, ChronoUnit.HOURS).toString())
    private fun ms(s: String) = try { OffsetDateTime.parse(s).toInstant().toEpochMilli() } catch (e: Exception) { 0L }

    private fun fail(e: Exception) {
        _ui.value = _ui.value.copy(loading = false, err = if (e is CloudError) cloud.friendly(e) else "Something went wrong. Try again.")
    }

    fun refresh() {
        if (cloud.session == null) return
        scope.launch {
            _ui.value = _ui.value.copy(loading = true, err = "")
            try {
                if (!cloud.fresh()) { _ui.value = SocialUi(); return@launch }
                val fr = JSONArray(cloud.request("/rest/v1/friendships?select=a,b,status"))
                val others = LinkedHashMap<String, String>() // other id -> status role
                for (i in 0 until fr.length()) {
                    val o = fr.getJSONObject(i)
                    val a = o.getString("a"); val b = o.getString("b"); val st = o.getString("status")
                    val other = if (a == me) b else a
                    others[other] = if (st == "accepted") "friend" else if (a == me) "outgoing" else "incoming"
                }
                val profiles = HashMap<String, Pair<String, String>>()
                if (others.isNotEmpty()) {
                    val ids = others.keys.joinToString(",")
                    val ps = JSONArray(cloud.request("/rest/v1/profiles?select=id,username,name&id=in.($ids)"))
                    for (i in 0 until ps.length()) {
                        val p = ps.getJSONObject(i)
                        profiles[p.getString("id")] = p.optString("username") to p.optString("name")
                    }
                }
                val list = others.map { (id, st) ->
                    val p = profiles[id]
                    Friend(id, p?.first ?: "someone", p?.second.orEmpty().ifEmpty { p?.first ?: "someone" }, st)
                }.sortedWith(compareBy({ it.status != "incoming" }, { it.status != "friend" }, { it.username }))
                // who has written to me that I have not read
                val un = HashSet<String>()
                try {
                    val r = JSONArray(cloud.request("/rest/v1/messages?select=sender&rcpt=eq.$me&read_at=is.null&created_at=gt.${dayAgo()}&limit=200"))
                    for (i in 0 until r.length()) un.add(r.getJSONObject(i).getString("sender"))
                } catch (_: Exception) {}
                val shared = HashMap<String, FriendShare>()
                val acc = list.filter { it.status == "friend" }.map { it.id }
                if (acc.isNotEmpty()) try {
                    val rows = JSONArray(cloud.request("/rest/v1/shared?select=user_id,np,np_at,pl,pl_at&user_id=in.(${acc.joinToString(",")})"))
                    for (i in 0 until rows.length()) {
                        val r = rows.getJSONObject(i)
                        val np = r.optJSONObject("np")
                        val live = np != null && np.optInt("p") == 1 && !r.isNull("np_at") && System.currentTimeMillis() - ms(r.optString("np_at")) < 240_000
                        val pls = ArrayList<SharedPlaylist>()
                        r.optJSONArray("pl")?.let { a ->
                            for (j in 0 until a.length()) {
                                val p = a.getJSONObject(j)
                                val ts = p.optJSONArray("t") ?: JSONArray()
                                pls.add(SharedPlaylist(p.optString("id"), p.optString("name"), (0 until ts.length()).map { song(ts.getJSONObject(it)) }))
                            }
                        }
                        shared[r.getString("user_id")] = FriendShare(if (live) song(np!!) else null, pls)
                    }
                } catch (_: Exception) {}
                _ui.value = _ui.value.copy(loading = false, friends = list, unread = un, shared = shared, err = "")
            } catch (e: Exception) { fail(e) }
        }
    }

    private fun song(o: JSONObject) = SharedSong(
        o.optString("t"), o.optString("a"), o.optString("id"), o.optString("url"), o.optString("img"),
        o.optInt("local", 0) == 1 || o.optBoolean("local", false), o.optInt("d", 0), o.optString("yt"),
    )

    /** Publish (or with null, clear) what is playing, so friends see it. Same table and shape as the web milo. */
    suspend fun pushNp(np: JSONObject?) {
        val now = Instant.now().toString()
        cloud.request(
            "/rest/v1/shared?on_conflict=user_id", "POST",
            JSONObject().put("user_id", me).put("np", np ?: JSONObject.NULL).put("np_at", now),
            headers = mapOf("Prefer" to "resolution=merge-duplicates,return=minimal"),
        )
    }

    suspend fun pushPl(pl: JSONArray?) {
        val now = Instant.now().toString()
        cloud.request(
            "/rest/v1/shared?on_conflict=user_id", "POST",
            JSONObject().put("user_id", me).put("pl", pl ?: JSONObject.NULL).put("pl_at", now),
            headers = mapOf("Prefer" to "resolution=merge-duplicates,return=minimal"),
        )
    }

    fun addByUsername(name: String) {
        val u = name.trim().lowercase().removePrefix("@")
        if (u.isEmpty()) return
        scope.launch {
            _ui.value = _ui.value.copy(err = "", note = "")
            try {
                val r = JSONArray(cloud.request("/rest/v1/rpc/find_user", "POST", JSONObject().put("u", u)))
                if (r.length() == 0) { _ui.value = _ui.value.copy(err = "No one is called @$u."); return@launch }
                val id = r.getJSONObject(0).getString("id")
                if (id == me) { _ui.value = _ui.value.copy(err = "That's you."); return@launch }
                cloud.request(
                    "/rest/v1/friendships", "POST", JSONObject().put("a", me).put("b", id).put("status", "pending"),
                    headers = mapOf("Prefer" to "return=minimal"),
                )
                _ui.value = _ui.value.copy(note = "Request sent to @$u.")
                refresh()
            } catch (e: Exception) {
                val m = e.message.orEmpty()
                _ui.value = _ui.value.copy(
                    err = when {
                        Regex("duplicate|already|friendships_pair|23505", RegexOption.IGNORE_CASE).containsMatchIn(m) -> "You already have a request or friendship with @$u."
                        Regex("friend_limit", RegexOption.IGNORE_CASE).containsMatchIn(m) -> "You've reached the friend limit."
                        else -> if (e is CloudError) cloud.friendly(e) else "Something went wrong. Try again."
                    }
                )
            }
        }
    }

    fun accept(f: Friend) = scope.launch {
        try {
            cloud.request("/rest/v1/friendships?a=eq.${f.id}&b=eq.$me", "PATCH", JSONObject().put("status", "accepted"), headers = mapOf("Prefer" to "return=minimal"))
            refresh()
        } catch (e: Exception) { fail(e) }
    }

    /** Decline a request, cancel one you sent, or remove a friend (this also deletes the chat). */
    fun remove(f: Friend) = scope.launch {
        try {
            cloud.request("/rest/v1/friendships?or=(and(a.eq.$me,b.eq.${f.id}),and(a.eq.${f.id},b.eq.$me))", "DELETE", headers = mapOf("Prefer" to "return=minimal"))
            refresh()
        } catch (e: Exception) { fail(e) }
    }

    /** Open a chat: load it now, then check for new messages every few seconds until [closeChat]. */
    fun openChat(f: Friend) {
        chatWith = f.id
        _msgs.value = emptyList()
        poll?.cancel()
        poll = scope.launch {
            while (chatWith == f.id) {
                loadChat(f.id)
                delay(3500)
            }
        }
    }

    fun closeChat() {
        chatWith = null
        poll?.cancel()
        _msgs.value = emptyList()
        refresh()
    }

    private suspend fun loadChat(fid: String) {
        try {
            if (!cloud.fresh()) return
            val r = JSONArray(
                cloud.request("/rest/v1/messages?select=id,sender,rcpt,kind,body,created_at,read_at,reply_to,rx&chat=eq.${enc(chatId(fid))}&created_at=gt.${dayAgo()}&order=created_at.asc&limit=300")
            )
            val out = ArrayList<ChatMsg>()
            var unreadFromThem = false
            for (i in 0 until r.length()) {
                val o = r.getJSONObject(i)
                val mine = o.getString("sender") == me
                val read = !o.isNull("read_at")
                if (!mine && !read) unreadFromThem = true
                val rxo = o.optJSONObject("rx")
                val rx = HashMap<String, String>()
                rxo?.keys()?.forEach { k -> rx[k] = rxo.optString(k) }
                val rt = if (o.isNull("reply_to")) null else o.optLong("reply_to")
                out.add(ChatMsg(o.getLong("id"), mine, o.optString("kind"), o.optString("body"), ms(o.optString("created_at")), read, rt, rx))
            }
            if (chatWith == fid) _msgs.value = out
            if (unreadFromThem) {
                val now = Instant.now().toString()
                try {
                    cloud.request(
                        "/rest/v1/messages?rcpt=eq.$me&sender=eq.$fid&read_at=is.null", "PATCH", JSONObject().put("read_at", now),
                        headers = mapOf("Prefer" to "return=minimal"),
                    )
                } catch (_: Exception) {}
            }
        } catch (_: Exception) { /* a missed check is fine, the next one retries */ }
    }

    /** Add, change or remove your own emoji on a message (tap the same one again to remove it). */
    fun react(fid: String, m: ChatMsg, emoji: String) {
        val mine = me
        val rx = HashMap(m.rx)
        if (rx[mine] == emoji) rx.remove(mine) else rx[mine] = emoji
        _msgs.value = _msgs.value.map { if (it.id == m.id) it.copy(rx = rx) else it }
        scope.launch {
            try {
                val o = JSONObject(); rx.forEach { (k, v) -> o.put(k, v) }
                cloud.request("/rest/v1/messages?id=eq.${m.id}", "PATCH", JSONObject().put("rx", o), headers = mapOf("Prefer" to "return=minimal"))
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(err = "Couldn't react.")
                loadChat(fid)
            }
        }
    }

    /** Take back a message you sent. */
    fun unsend(fid: String, m: ChatMsg) {
        _msgs.value = _msgs.value.filterNot { it.id == m.id }
        scope.launch {
            try { cloud.request("/rest/v1/messages?id=eq.${m.id}", "DELETE", headers = mapOf("Prefer" to "return=minimal")) }
            catch (e: Exception) { _ui.value = _ui.value.copy(err = "Couldn't unsend.") }
            loadChat(fid)
        }
    }

    fun send(fid: String, kind: String, body: String, replyTo: Long? = null) {
        if (body.isBlank()) return
        scope.launch {
            try {
                val b = JSONObject().put("rcpt", fid).put("kind", kind).put("body", body)
                if (replyTo != null) b.put("reply_to", replyTo)
                cloud.request("/rest/v1/messages", "POST", b, headers = mapOf("Prefer" to "return=minimal"))
                loadChat(fid)
            } catch (e: Exception) {
                val m = e.message.orEmpty()
                _ui.value = _ui.value.copy(
                    err = when {
                        Regex("rate_limited", RegexOption.IGNORE_CASE).containsMatchIn(m) -> "Slow down a little."
                        Regex("too_large", RegexOption.IGNORE_CASE).containsMatchIn(m) -> "That's too big to send."
                        else -> "Couldn't send. Try again."
                    }
                )
            }
        }
    }

    fun clearErr() { _ui.value = _ui.value.copy(err = "", note = "") }
}
