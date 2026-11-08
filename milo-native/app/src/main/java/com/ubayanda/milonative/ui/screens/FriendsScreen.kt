package com.ubayanda.milonative.ui.screens

import com.ubayanda.milonative.ui.components.glass
import com.ubayanda.milonative.data.Invite
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateFloat
import com.ubayanda.milonative.data.ChatMedia
import com.ubayanda.milonative.ui.components.glassStroke
import com.ubayanda.milonative.ui.components.lift
import com.ubayanda.milonative.ui.components.pressable
import androidx.compose.material.icons.rounded.ChatBubble
import com.ubayanda.milonative.data.ChatMsg
import com.ubayanda.milonative.data.Friend
import com.ubayanda.milonative.data.FriendShare
import com.ubayanda.milonative.data.LastMsg
import com.ubayanda.milonative.data.LibrarySync
import com.ubayanda.milonative.data.SharedPlaylist
import com.ubayanda.milonative.data.SharedSong
import com.ubayanda.milonative.data.VoicePlayer
import com.ubayanda.milonative.data.VoiceRecorder
import com.ubayanda.milonative.model.Track
import com.ubayanda.milonative.ui.AccountUi
import com.ubayanda.milonative.ui.MainViewModel
import com.ubayanda.milonative.ui.components.Cover
import com.ubayanda.milonative.ui.components.UserAvatar
import com.ubayanda.milonative.ui.theme.Milo
import com.ubayanda.milonative.ui.theme.Ty
import com.ubayanda.milonative.ui.theme.Sp
import com.ubayanda.milonative.ui.theme.Rad
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DANGER = Color(0xFFFF8A80)
private fun hhmm(ms: Long) = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ms))

@Composable
private fun SmallBtn(label: String, filled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Text(
        label, color = if (filled) Color.Black else Milo.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clip(shape).background(if (filled) Milo.amber else Milo.card2, shape)
            .border(1.dp, if (filled) Milo.amber else Milo.line, shape).clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    )
}

@Composable
private fun Field(value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier, imeAction: ImeAction = ImeAction.Default, onGo: () -> Unit = {}) {
    val shape = RoundedCornerShape(Rad.lg)
    Box(modifier.clip(shape).glass(shape, 1.3f, Milo.card2).padding(horizontal = 18.dp, vertical = 14.dp)) {
        if (value.isEmpty()) Text(hint, color = Milo.dim, fontSize = 16.sp)
        BasicTextField(
            value = value, onValueChange = onChange, singleLine = true,
            textStyle = TextStyle(color = Milo.text, fontSize = 16.sp), cursorBrush = SolidColor(Milo.amber),
            keyboardOptions = KeyboardOptions(imeAction = imeAction),
            keyboardActions = KeyboardActions(onAny = { onGo() }),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun nowLine(s: SharedSong) = s.title + if (s.artist.isNotEmpty()) " · " + s.artist else ""

// ============================== chat list ==============================

@Composable
fun FriendsScreen(vm: MainViewModel, acct: AccountUi, onBack: () -> Unit, onChat: (Friend) -> Unit) {
    val s by vm.social.ui.collectAsState()
    val ctx = LocalContext.current
    var name by remember { mutableStateOf("") }
    var addOpen by remember { mutableStateOf(false) }
    var profile by remember { mutableStateOf<Friend?>(null) }
    var tab by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(0) }
    var q by remember { mutableStateOf("") }
    LaunchedEffect(acct.signedIn) {
        while (acct.signedIn) {
            vm.social.refresh()
            delay(10_000)
        }
    }
    Column(Modifier.fillMaxSize().background(Milo.panel)) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 6.dp, end = 10.dp, top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back", tint = Milo.text) }
            Text("Chats", color = Milo.text, style = Ty.display, modifier = Modifier.weight(1f))
            if (acct.signedIn) Box(Modifier.size(44.dp).clip(CircleShape).background(Milo.card2).clickable { addOpen = !addOpen }, contentAlignment = Alignment.Center) {
                Icon(if (addOpen) Icons.Rounded.Close else Icons.Rounded.PersonAdd, "Add a friend", tint = Milo.text, modifier = Modifier.size(22.dp))
            }
        }
        if (!acct.signedIn) {
            Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Sign in to add friends and chat", color = Milo.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Your account works on the web milo. too, so your friends and chats are the same everywhere.", color = Milo.dim, fontSize = 15.sp)
                SmallBtn("Log in", true) { vm.openGate("in") }
                SmallBtn("Sign up free", false) { vm.openGate("up") }
            }
            return@Column
        }
        val req = s.friends.filter { it.status == "incoming" }
        val out = s.friends.filter { it.status == "outgoing" }
        val fr = s.friends.filter { it.status == "friend" }.sortedWith(
            compareByDescending<Friend> { it.id in s.unread }.thenByDescending { s.last[it.id]?.at ?: 0L }.thenBy { it.name.lowercase() }
        )
        val frShown = if (q.isBlank()) fr else fr.filter { it.name.contains(q.trim(), true) || it.username.contains(q.trim(), true) }
        val badge = s.unread.size + req.size
        com.ubayanda.milonative.ui.components.GlassSegmented(
            listOf("Feed", if (badge > 0) "Chats \u00b7 $badge" else "Chats"), tab, { tab = it }, Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
        )
        if (tab == 0) {
            FeedContent(vm, fr, s, Modifier.weight(1f), onChat = onChat, onProfile = { profile = it }, onToChats = { tab = 1 }, requests = req.size)
            profile?.let { f -> FriendProfileDialog(vm, f, s.shared[f.id], onChat = { profile = null; onChat(f) }, onClose = { profile = null }) }
            return@Column
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 40.dp)) {
            if (addOpen) item(key = "add") {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Field(name, { name = it; vm.social.clearErr() }, "Their username", Modifier.weight(1f), ImeAction.Send) { vm.social.addByUsername(name); name = "" }
                        SmallBtn("Add", true) { vm.social.addByUsername(name); name = "" }
                    }
                }
            }
            if (s.err.isNotEmpty()) item(key = "err") { Text(s.err, color = DANGER, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) }
            if (s.note.isNotEmpty()) item(key = "note") { Text(s.note, color = Milo.amber, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) }
            if (req.isNotEmpty()) {
                item(key = "rhead") { Text("Requests", color = Milo.dim, style = Ty.micro, letterSpacing = 1.sp, modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 6.dp)) }
                items(req, key = { "q" + it.id }) { f ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clip(RoundedCornerShape(22.dp)).background(Milo.card).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        UserAvatar(f.name, f.avatar, 50.dp)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(f.name, color = Milo.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("@" + f.username + " wants to be friends", color = Milo.dim, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        SmallBtn("Accept", true) { vm.social.accept(f) }
                        Spacer(Modifier.size(6.dp))
                        SmallBtn("Decline", false) { vm.social.remove(f) }
                    }
                }
            }
            if (fr.size > 3) item(key = "search") { Field(q, { q = it }, "Search chats", Modifier.padding(horizontal = 18.dp, vertical = 6.dp).fillMaxWidth()) }
            item(key = "chead") { Text("Messages", color = Milo.dim, style = Ty.micro, letterSpacing = 1.sp, modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 4.dp)) }
            if (fr.isEmpty()) item(key = "empty") {
                Text(
                    if (s.loading) "Loading…" else "No friends yet. Tap the person icon above and add someone by their username.",
                    color = Milo.dim, fontSize = 15.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
            items(frShown, key = { "f" + it.id }) { f ->
                ChatRow(f, s.last[f.id], f.id in s.unread, s.shared[f.id]?.now, onClick = { onChat(f) }, onLong = { profile = f })
            }
            if (out.isNotEmpty()) {
                item(key = "ohead") { Text("Sent requests", color = Milo.dim, style = Ty.micro, letterSpacing = 1.sp, modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 6.dp)) }
                items(out, key = { "o" + it.id }) { f ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        UserAvatar(f.name, f.avatar, 46.dp)
                        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                            Text(f.name, color = Milo.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Text("Waiting for them to accept", color = Milo.dim, fontSize = 13.sp)
                        }
                        SmallBtn("Cancel", false) { vm.social.remove(f) }
                    }
                }
            }
        }
    }
    profile?.let { f ->
        FriendProfileDialog(vm, f, s.shared[f.id], onChat = { profile = null; onChat(f) }, onClose = { profile = null })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatRow(f: Friend, last: LastMsg?, unread: Boolean, now: SharedSong?, onClick: () -> Unit, onLong: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLong).padding(horizontal = 20.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserAvatar(f.name, f.avatar, 58.dp, ring = now != null)
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(f.name, color = Milo.text, style = Ty.heading.copy(fontWeight = if (unread) FontWeight.ExtraBold else FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            val preview = when {
                last != null -> (if (last.mine) "You: " else "") + last.text
                now != null -> "♪ " + nowLine(now)
                else -> "Say hi 👋"
            }
            Text(preview, color = if (unread) Milo.text else Milo.dim, fontSize = 14.sp, fontWeight = if (unread) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
            if (now != null && last != null) Text("♪ " + nowLine(now), color = Milo.amber, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
        }
        Column(horizontalAlignment = Alignment.End) {
            if (last != null) Text(hhmm(last.at), color = if (unread) Milo.amber else Milo.dim, fontSize = 12.sp)
            if (unread) Box(Modifier.padding(top = 6.dp).size(11.dp).clip(CircleShape).background(Milo.amber))
        }
    }
}

// ============================== friend profile ==============================

@Composable
private fun FriendProfileDialog(vm: MainViewModel, f: Friend, sh: FriendShare?, onChat: () -> Unit, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val cover by produceState<android.graphics.Bitmap?>(null, f.id) {
        val d = vm.social.fetchCover(f.id)
        value = if (d.isEmpty()) null else withContext(Dispatchers.Default) { ChatMedia.photoBitmap(d) }
    }
    var showPl by remember { mutableStateOf(false) }
    val now = sh?.now
    if (showPl && sh != null) FriendPlaylistsDialog(vm, f.name, sh.playlists) { showPl = false }
    androidx.compose.ui.window.Dialog(onDismissRequest = onClose) {
        Column(Modifier.clip(RoundedCornerShape(28.dp)).background(Milo.card)) {
            Box(Modifier.fillMaxWidth().height(130.dp).background(Brush.linearGradient(listOf(Color(0xFF6B4300), Milo.card2)))) {
                cover?.let { c -> androidx.compose.foundation.Image(remember(c) { c.asImageBitmap() }, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            }
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp)) {
                UserAvatar(f.name, f.avatar, 84.dp, Modifier.offset(y = (-42).dp))
                Column(Modifier.offset(y = (-34).dp)) {
                    Text(f.name, color = Milo.text, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Text("@" + f.username, color = Milo.dim, fontSize = 15.sp)
                    if (now != null) {
                        Row(Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Milo.card2).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Cover(vm, sharedTrack(now), 52.dp, 12.dp, px = 160)
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text("Listening now", color = Milo.amber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(now.title, color = Milo.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(now.artist, color = Milo.dim, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (!now.local && (now.url.isNotEmpty() || now.yt.isNotEmpty())) SmallBtn("Listen", true) { playShared(vm, ctx, now) }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SmallBtn("Message", true, onChat)
                        if (com.ubayanda.milonative.BuildConfig.LITE) SmallBtn("Share my EQ", false) {
                            vm.shareEq(f.id) { ok ->
                                android.widget.Toast.makeText(ctx, if (ok) "EQ sent" else "Couldn't share. Sign in and play a song first.", android.widget.Toast.LENGTH_SHORT).show()
                                if (ok) onClose()
                            }
                        }
                        if (!com.ubayanda.milonative.BuildConfig.LITE) SmallBtn("Listen together", false) {
                            vm.inviteToTogether(f.id) { c ->
                                android.widget.Toast.makeText(ctx, if (c != null) "Invite sent. Hosting $c. Play a YouTube song." else "Couldn't start. Sign in first.", android.widget.Toast.LENGTH_LONG).show()
                                if (c != null) onClose()
                            }
                        }
                        if (sh != null && sh.playlists.isNotEmpty()) SmallBtn("Playlists", false) { showPl = true }
                    }
                    Text(
                        "Remove friend", color = DANGER, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 10.dp).clip(RoundedCornerShape(12.dp)).clickable { vm.social.remove(f); onClose() }.padding(vertical = 10.dp, horizontal = 2.dp),
                    )
                }
            }
        }
    }
}

private fun sharedTrack(s: SharedSong): com.ubayanda.milonative.model.Track {
    val id = -(1L + (("shr:" + s.id + s.url).hashCode().toLong() and 0x7fffffffL))
    return com.ubayanda.milonative.model.Track(
        id = id, uri = s.url, title = s.title, artist = s.artist, album = "Shared", albumId = 4_000_000_000_000L + (-id),
        durationMs = if (s.durSec > 0) s.durSec * 1000L else 1L, trackNo = 0, year = 0, addedSec = 0L, artUrl = s.img,
    )
}

/** Plays a friend's shared song: from its web address, or opens it on YouTube when that is where it comes from. */
private fun playShared(vm: MainViewModel, ctx: android.content.Context, s: SharedSong) {
    if (s.url.startsWith("https://")) vm.playRemote(listOf(sharedTrack(s)), 0)
    else if (s.yt.isNotEmpty()) ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://youtu.be/" + s.yt)).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
}

@Composable
private fun FriendPlaylistsDialog(vm: MainViewModel, who: String, lists: List<SharedPlaylist>, onClose: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var open by remember { mutableStateOf<String?>(null) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onClose, containerColor = Milo.card, shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
        title = { com.ubayanda.milonative.ui.components.BlurBehind(); Text("$who's playlists", color = Milo.text) },
        text = {
            LazyColumn(Modifier.heightIn(max = 380.dp)) {
                lists.forEach { p ->
                    item(key = "h" + p.id) {
                        Text(
                            p.name + "  ·  " + p.songs.size, color = Milo.amber, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.fillMaxWidth().clickable { open = if (open == p.id) null else p.id }.padding(vertical = 10.dp),
                        )
                    }
                    if (open == p.id) {
                        val playable = p.songs.filter { !it.local && it.url.startsWith("https://") }
                        items(p.songs.size, key = { "s" + p.id + it }) { i ->
                            val sg = p.songs[i]
                            val can = !sg.local && (sg.url.startsWith("https://") || sg.yt.isNotEmpty())
                            Text(
                                sg.title + (if (sg.artist.isNotEmpty()) " · " + sg.artist else "") + (if (sg.local) "  (on their phone)" else ""),
                                color = if (can) Milo.text else Milo.dim, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth().then(
                                    if (can) Modifier.clickable {
                                        if (sg.url.startsWith("https://")) vm.playRemote(playable.map { sharedTrack(it) }, playable.indexOf(sg).coerceAtLeast(0)) else playShared(vm, ctx, sg)
                                    } else Modifier
                                ).padding(vertical = 8.dp, horizontal = 8.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = onClose) { Text("Close", color = Milo.amber) } },
    )
}


// ============================== conversation ==============================

private val REACTIONS = listOf("❤️", "😂", "😮", "😢", "👍", "🔥")
private val BIG = 22.dp
private val SMALL = 6.dp

private sealed class ChatItem {
    class Sep(val label: String, val key: String) : ChatItem()
    class Msg(val m: ChatMsg, val first: Boolean, val last: Boolean) : ChatItem()
}

private fun buildItems(msgs: List<ChatMsg>): List<ChatItem> {
    val out = ArrayList<ChatItem>(msgs.size + 8)
    msgs.forEachIndexed { i, m ->
        val prev = msgs.getOrNull(i - 1); val next = msgs.getOrNull(i + 1)
        val gapPrev = if (prev == null) Long.MAX_VALUE else m.at - prev.at
        if (prev == null || gapPrev > 15 * 60_000L) out.add(ChatItem.Sep(hhmm(m.at), "s" + m.id))
        val first = prev == null || prev.mine != m.mine || gapPrev > 3 * 60_000L
        val last = next == null || next.mine != m.mine || next.at - m.at > 3 * 60_000L || next.at - m.at > 15 * 60_000L
        out.add(ChatItem.Msg(m, first, last))
    }
    return out
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(vm: MainViewModel, f0: Friend, onBack: () -> Unit) {
    val msgs by vm.social.msgs.collectAsState()
    val s by vm.social.ui.collectAsState()
    val ui by vm.player.state.collectAsState()
    val f = s.friends.firstOrNull { it.id == f0.id } ?: f0
    val ctx = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var text by remember { mutableStateOf("") }
    var replyTo by remember { mutableStateOf<ChatMsg?>(null) }
    var menuFor by remember { mutableStateOf<ChatMsg?>(null) }
    var viewing by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var recording by remember { mutableStateOf(false) }
    var plus by remember { mutableStateOf(false) }
    var profile by remember { mutableStateOf(false) }
    var timeFor by remember { mutableStateOf<Long?>(null) }
    val recorder = remember { VoiceRecorder(ctx) }
    val list = rememberLazyListState()
    val rows = remember(msgs) { buildItems(msgs) }
    val lastMineId = remember(msgs) { msgs.lastOrNull { it.mine }?.id }
    val sh = s.shared[f.id]
    val now = sh?.now
    val theirTyping by vm.typing.theirs.collectAsState()
    LaunchedEffect(f.id) { vm.social.openChat(f); vm.typing.join(listOf(vm.social.me, f.id).sorted().joinToString(":"), vm.social.me) }
    androidx.compose.runtime.DisposableEffect(f.id) { onDispose { vm.social.closeChat(); vm.typing.leave(); VoicePlayer.stop(); recorder.cancel() } }
    LaunchedEffect(rows.size) { if (rows.isNotEmpty()) list.scrollToItem(rows.size - 1) }
    val current = vm.trackById(ui.currentId)
    val me = vm.social.me

    fun go() { if (text.isNotBlank()) { vm.social.send(f.id, "text", text.trim(), replyTo?.id); text = ""; replyTo = null } }
    fun finishVoice() {
        recording = false
        val d = recorder.finish()
        if (d == null) android.widget.Toast.makeText(ctx, "Voice note too short or too long.", android.widget.Toast.LENGTH_SHORT).show()
        else { vm.social.send(f.id, "voice", d, replyTo?.id); replyTo = null }
    }
    val photo = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val d = withContext(Dispatchers.IO) { ChatMedia.photoToData(ctx, uri) }
            if (d == null) android.widget.Toast.makeText(ctx, "Couldn't use that photo.", android.widget.Toast.LENGTH_SHORT).show()
            else { vm.social.send(f.id, "img", d, replyTo?.id); replyTo = null }
        }
    }
    val mic = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) recording = recorder.start() else android.widget.Toast.makeText(ctx, "Allow the microphone to send voice notes.", android.widget.Toast.LENGTH_SHORT).show()
    }
    LaunchedEffect(recording) { if (recording) { delay(45_000); if (recording) finishVoice() } }
    var tick by remember { mutableIntStateOf(0) }
    val swipeLimit = with(LocalDensity.current) { 64.dp.toPx() }

    Column(Modifier.fillMaxSize().background(Milo.panel).imePadding()) {
        // ---- header ----
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back", tint = Milo.text) }
            Row(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).clickable { profile = true }.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(f.name, f.avatar, 44.dp, ring = now != null)
                Column(Modifier.padding(start = 12.dp)) {
                    Text(f.name, color = Milo.text, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        if (now != null) "♪ " + nowLine(now) else "@" + f.username,
                        color = if (now != null) Milo.amber else Milo.dim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (now != null && !now.local && (now.url.isNotEmpty() || now.yt.isNotEmpty())) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 2.dp).clip(RoundedCornerShape(18.dp)).background(Milo.card).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Cover(vm, sharedTrack(now), 42.dp, 10.dp, px = 160)
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text(now.title, color = Milo.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(f.name + " is listening", color = Milo.dim, fontSize = 12.sp, maxLines = 1)
                }
                SmallBtn("Listen along", true) { playShared(vm, ctx, now) }
            }
        }

        // ---- messages ----
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = list, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
            if (rows.isEmpty()) item(key = "hi") {
                Column(Modifier.fillMaxWidth().padding(top = 60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    UserAvatar(f.name, f.avatar, 96.dp)
                    Text(f.name, color = Milo.text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 12.dp))
                    Text("Say hi 👋 Messages disappear after 24 hours.", color = Milo.dim, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
            items(rows, key = { if (it is ChatItem.Msg) "m" + it.m.id else (it as ChatItem.Sep).key }, contentType = { if (it is ChatItem.Msg) it.m.kind else "sep" }) { it ->
                if (it is ChatItem.Sep) {
                    Text(it.label, color = Milo.dim, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp))
                    return@items
                }
                val c = it as ChatItem.Msg
                val m = c.m
                var dx by remember(m.id) { mutableFloatStateOf(0f) }
                val fg = if (m.mine) Color.Black else Milo.text
                val shape = if (m.mine) RoundedCornerShape(
                    topStart = BIG, bottomStart = BIG, topEnd = if (c.first) BIG else SMALL, bottomEnd = if (c.last) BIG else SMALL,
                ) else RoundedCornerShape(
                    topEnd = BIG, bottomEnd = BIG, topStart = if (c.first) BIG else SMALL, bottomStart = if (c.last) BIG else SMALL,
                )
                Column(Modifier.fillMaxWidth().padding(top = if (c.first) 8.dp else 2.dp), horizontalAlignment = if (m.mine) Alignment.End else Alignment.Start) {
                    Row(Modifier.graphicsLayer { translationX = dx }, verticalAlignment = Alignment.Bottom) {
                        if (!m.mine) {
                            if (c.last) UserAvatar(f.name, f.avatar, 30.dp) else Spacer(Modifier.size(30.dp))
                            Spacer(Modifier.width(8.dp))
                        }
                        Column(horizontalAlignment = if (m.mine) Alignment.End else Alignment.Start) {
                            val bg: Modifier = if (m.mine) Modifier.background(Brush.linearGradient(listOf(Color(0xFFFFB02E), Color(0xFFF08A00))), shape) else Modifier.background(Milo.card2, shape)
                            Column(
                                Modifier.widthIn(max = 288.dp).clip(shape).then(bg)
                                    .pointerInput(m.id) {
                                        detectHorizontalDragGestures(
                                            onDragEnd = { if (dx > swipeLimit * 0.9f) { replyTo = m; haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress) }; dx = 0f },
                                            onDragCancel = { dx = 0f },
                                            onHorizontalDrag = { ch, d -> if (dx > 0f || d > 0f) { ch.consume(); dx = (dx + d * 0.6f).coerceIn(0f, swipeLimit) } },
                                        )
                                    }
                                    .combinedClickable(onClick = { timeFor = if (timeFor == m.id) null else m.id }, onLongClick = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress); menuFor = m })
                                    .padding(horizontal = if (m.kind == "img") 4.dp else 14.dp, vertical = if (m.kind == "img") 4.dp else 9.dp),
                            ) {
                                if (m.replyTo != null) {
                                    val q = msgs.firstOrNull { x -> x.id == m.replyTo }
                                    Row(Modifier.padding(bottom = 6.dp).clip(RoundedCornerShape(10.dp)).background(fg.copy(alpha = 0.12f)).height(androidx.compose.foundation.layout.IntrinsicSize.Min)) {
                                        Box(Modifier.width(3.dp).fillMaxHeight().background(fg.copy(alpha = 0.6f)))
                                        Text(
                                            if (q != null) (if (q.mine) "You: " else "") + q.shown else "Message expired",
                                            color = fg.copy(alpha = 0.75f), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        )
                                    }
                                }
                                when (m.kind) {
                                    "img" -> {
                                        val bmp by produceState<android.graphics.Bitmap?>(null, m.id) { value = withContext(Dispatchers.Default) { ChatMedia.photoBitmap(m.body) } }
                                        val b = bmp
                                        if (b != null) androidx.compose.foundation.Image(
                                            remember(b) { b.asImageBitmap() }, "Photo",
                                            Modifier.widthIn(max = 260.dp).clip(RoundedCornerShape(topStart = if (m.mine) 18.dp else 14.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = if (m.mine) 14.dp else 18.dp)).clickable { viewing = b },
                                            contentScale = ContentScale.FillWidth,
                                        ) else Text("📷 Loading photo…", color = fg, fontSize = 15.sp, modifier = Modifier.padding(10.dp))
                                    }
                                    "voice" -> {
                                        val playing = tick >= 0 && VoicePlayer.playingId == m.id
                                        Row(Modifier.clickable { VoicePlayer.toggle(ctx, m.id, m.body) { tick++ } }, verticalAlignment = Alignment.CenterVertically) {
                                            Box(Modifier.size(36.dp).clip(CircleShape).background(fg.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                                Icon(if (playing) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, if (playing) "Stop" else "Play", tint = fg, modifier = Modifier.size(24.dp))
                                            }
                                            Spacer(Modifier.width(10.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(30.dp)) {
                                                for (i in 0 until 22) {
                                                    val h = 6 + ((m.id * 31 + i * 17) % 19).toInt()
                                                    Box(Modifier.width(3.dp).height(h.dp).clip(CircleShape).background(fg.copy(alpha = if (playing) 0.95f else 0.55f)))
                                                }
                                            }
                                        }
                                    }
                                    "song" -> SongBubble(vm, m, fg)
                                    else -> {
                                        val inv = if (m.kind == "text") Invite.parse(m.body) else null
                                        val eqp = if (m.kind == "text") com.ubayanda.milonative.data.EqShare.parse(m.body) else null
                                        if (inv != null) InviteBubble(vm, inv, m.mine, fg)
                                        else if (eqp != null) EqBubble(eqp, m.mine, fg)
                                        else Text(m.shown, color = fg, style = Ty.body.copy(fontWeight = FontWeight.Normal))
                                    }
                                }
                            }
                            if (m.rx.isNotEmpty()) {
                                val grouped = m.rx.values.groupingBy { x -> x }.eachCount()
                                Row(Modifier.offset(y = (-8).dp).padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    grouped.forEach { (e, n) ->
                                        val mineRx = m.rx[me] == e
                                        Text(
                                            if (n > 1) "$e $n" else e, fontSize = 13.sp, color = Milo.text,
                                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(if (mineRx) Milo.amber.copy(alpha = 0.55f) else Milo.card)
                                                .border(1.dp, Milo.panel, RoundedCornerShape(12.dp))
                                                .clickable { vm.social.react(f.id, m, e) }.padding(horizontal = 7.dp, vertical = 2.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    val showStatus = m.mine && m.id == lastMineId
                    if (timeFor == m.id || showStatus) {
                        Row(Modifier.padding(horizontal = 6.dp, vertical = 2.dp).then(if (!m.mine) Modifier.padding(start = 38.dp) else Modifier), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                (if (timeFor == m.id) hhmm(m.at) + (if (showStatus) " · " else "") else "") + if (showStatus) (if (m.read) "Seen" else "Sent") else "",
                                color = Milo.dim, fontSize = 11.sp,
                            )
                            if (showStatus && m.read) { Spacer(Modifier.width(4.dp)); UserAvatar(f.name, f.avatar, 14.dp) }
                        }
                    }
                }
            }
        }
        if (s.err.isNotEmpty()) Text(s.err, color = DANGER, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        replyTo?.let { r ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp).clip(RoundedCornerShape(16.dp)).background(Milo.card).height(androidx.compose.foundation.layout.IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(4.dp).fillMaxHeight().background(Milo.amber))
                Column(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(if (r.mine) "Replying to yourself" else "Replying to " + f.name, color = Milo.amber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(r.shown, color = Milo.dim, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = { replyTo = null }) { Icon(Icons.Rounded.Close, "Cancel reply", tint = Milo.dim) }
            }
        }

        if (theirTyping) Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            TypingDots()
            Text(f.name.substringBefore(' ') + " is typing\u2026", color = Milo.amber, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp))
        }
        // ---- composer ----
        if (recording) {
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { recorder.cancel(); recording = false }) { Icon(Icons.Rounded.Close, "Cancel", tint = Milo.dim) }
                Text("● Recording… tap send when done", color = DANGER, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Box(Modifier.size(46.dp).clip(CircleShape).background(Milo.amber).clickable { finishVoice() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Send, "Send voice note", tint = Color.Black, modifier = Modifier.size(22.dp))
                }
            }
        } else {
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    Box(Modifier.size(46.dp).clip(CircleShape).background(Milo.card2).clickable { plus = true }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Add, "More", tint = Milo.amber, modifier = Modifier.size(26.dp))
                    }
                    DropdownMenu(expanded = plus, onDismissRequest = { plus = false }) {
                        DropdownMenuItem(text = { Text("Photo") }, leadingIcon = { Icon(Icons.Rounded.Image, null) }, onClick = {
                            plus = false
                            photo.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly))
                        })
                        if (current != null) DropdownMenuItem(text = { Text("Share what's playing") }, leadingIcon = { Icon(Icons.Rounded.MusicNote, null) }, onClick = {
                            plus = false
                            val o = JSONObject().put("t", current.title).put("a", current.artist).put("al", current.album)
                            if (current.id < 0 && current.uri.startsWith("https://")) { o.put("url", current.uri); if (current.artUrl.startsWith("https://")) o.put("img", current.artUrl) }
                            vm.social.send(f.id, "song", o.toString(), replyTo?.id); replyTo = null
                        })
                    }
                }
                val pill = RoundedCornerShape(26.dp)
                Box(Modifier.weight(1f).heightIn(min = 46.dp).clip(pill).background(Milo.card2, pill).padding(horizontal = 18.dp, vertical = 12.dp), contentAlignment = Alignment.CenterStart) {
                    if (text.isEmpty()) Text("Message", color = Milo.dim, fontSize = 16.sp)
                    BasicTextField(
                        value = text, onValueChange = { text = it; vm.social.clearErr(); if (it.isNotBlank()) vm.typing.ping() }, maxLines = 5,
                        textStyle = TextStyle(color = Milo.text, fontSize = 16.sp), cursorBrush = SolidColor(Milo.amber),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default, capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (text.isBlank()) {
                    Box(Modifier.size(46.dp).clip(CircleShape).background(Milo.card2).clickable {
                        if (androidx.core.content.ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) recording = recorder.start()
                        else mic.launch(android.Manifest.permission.RECORD_AUDIO)
                    }, contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Mic, "Record a voice note", tint = Milo.amber, modifier = Modifier.size(22.dp)) }
                } else {
                    Box(Modifier.size(46.dp).clip(CircleShape).background(Milo.amber).clickable { go() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Send, "Send", tint = Color.Black, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }

    menuFor?.let { m ->
        androidx.compose.ui.window.Dialog(onDismissRequest = { menuFor = null }) {
            Column(Modifier.clip(RoundedCornerShape(Rad.lg)).background(Milo.card).padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    REACTIONS.forEach { e ->
                        Text(e, fontSize = 26.sp, modifier = Modifier.clip(CircleShape).background(if (m.rx[me] == e) Milo.amber.copy(alpha = 0.35f) else Color.Transparent)
                            .clickable { vm.social.react(f.id, m, e); menuFor = null }.padding(6.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                MenuLine("Reply") { replyTo = m; menuFor = null }
                if (m.kind == "text") MenuLine("Copy") {
                    (ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager)
                        .setPrimaryClip(android.content.ClipData.newPlainText("message", m.body))
                    menuFor = null
                }
                if (m.mine) MenuLine("Unsend", danger = true) { vm.social.unsend(f.id, m); menuFor = null }
            }
        }
    }
    viewing?.let { b ->
        androidx.compose.ui.window.Dialog(onDismissRequest = { viewing = null }) {
            androidx.compose.foundation.Image(remember(b) { b.asImageBitmap() }, "Photo", Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { viewing = null }, contentScale = ContentScale.Fit)
        }
    }
    if (profile) FriendProfileDialog(vm, f, sh, onChat = { profile = false }, onClose = { profile = false })
}

/** "Listen together" invite: shows the code and a Join button. */
@Composable
private fun InviteBubble(vm: MainViewModel, code: String, mine: Boolean, fg: Color) {
    val ctx = LocalContext.current
    val cur by vm.together.code.collectAsState()
    if (com.ubayanda.milonative.BuildConfig.LITE) {
        Column(Modifier.widthIn(min = 200.dp)) {
            Text("\uD83C\uDFA7 Listen together", color = fg, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("Not in milo. Lite. Ask them to share a song instead.", color = fg.copy(alpha = 0.75f), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
        return
    }
    Column(Modifier.widthIn(min = 200.dp)) {
        Text("\uD83C\uDFA7 Listen together", color = fg, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("Code $code", color = fg.copy(alpha = 0.75f), fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp, bottom = 8.dp))
        if (cur == code) Text(if (vm.together.host) "You're hosting" else "You're listening in", color = fg.copy(alpha = 0.75f), fontSize = 12.sp)
        else if (!mine) SmallBtn("Join", true) {
            vm.joinTogether(code, false) { ok -> android.widget.Toast.makeText(ctx, if (ok) "Joined $code" else "Couldn't join. The session may have ended.", android.widget.Toast.LENGTH_SHORT).show() }
        }
        else Text("Session may have ended", color = fg.copy(alpha = 0.6f), fontSize = 12.sp)
    }
}

/** A friend's EQ preset: a little bar chart of the curve and a Try it button. */
@Composable
private fun EqBubble(p: com.ubayanda.milonative.data.EqShare.Preset, mine: Boolean, fg: Color) {
    val ctx = LocalContext.current
    Column(Modifier.widthIn(min = 200.dp)) {
        Text("\uD83C\uDF9A EQ preset", color = fg, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(p.name, color = fg.copy(alpha = 0.75f), fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp, bottom = 8.dp))
        val mx = (p.levels.maxOfOrNull { kotlin.math.abs(it) } ?: 1f).coerceAtLeast(1f)
        Row(Modifier.height(36.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
            p.levels.forEach { l -> Box(Modifier.width(7.dp).height((6f + 26f * ((l + mx) / (2f * mx))).dp).clip(RoundedCornerShape(3.dp)).background(fg.copy(alpha = 0.8f))) }
        }
        if (!mine) Box(Modifier.padding(top = 8.dp)) {
            SmallBtn("Try it", true) {
                com.ubayanda.milonative.player.AudioFx.applyCustom(p.levels, p.bass, p.spatial, p.name)
                android.widget.Toast.makeText(ctx, "EQ applied: " + p.name, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}

/** A shared song: cover, title and artist. Tap to play it if it's on this phone or online. */
@Composable
private fun SongBubble(vm: MainViewModel, m: ChatMsg, fg: Color) {
    val ctx = LocalContext.current
    val tracks by vm.tracks.collectAsState()
    val o = remember(m.body) { try { JSONObject(m.body) } catch (e: Exception) { JSONObject() } }
    val title = o.optString("t", "Song"); val artist = o.optString("a"); val url = o.optString("url"); val img = o.optString("img")
    val track: Track? = remember(m.body, tracks) {
        val k = LibrarySync.key(title, artist)
        tracks.firstOrNull { LibrarySync.key(it.title, it.artist) == k }
            ?: if (url.startsWith("https://")) sharedTrack(SharedSong(title, artist, "msg" + m.id, url, img, false, 0, "")) else null
    }
    Row(
        Modifier.widthIn(min = 200.dp).clickable {
            if (track == null) android.widget.Toast.makeText(ctx, "That song isn't on this phone.", android.widget.Toast.LENGTH_SHORT).show()
            else if (track.id >= 0) vm.player.playQueue(listOf(track), 0) else vm.playRemote(listOf(track), 0)
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(54.dp).clip(RoundedCornerShape(12.dp)).background(fg.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            if (track != null) Cover(vm, track, 54.dp, 12.dp, px = 160) else Icon(Icons.Rounded.MusicNote, null, tint = fg, modifier = Modifier.size(26.dp))
        }
        Column(Modifier.padding(start = 12.dp)) {
            Text(title, color = fg, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (artist.isNotEmpty()) Text(artist, color = fg.copy(alpha = 0.75f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (track != null) "Tap to play" else "Shared song", color = fg.copy(alpha = 0.6f), fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun MenuLine(label: String, danger: Boolean = false, onClick: () -> Unit) {
    Text(
        label, color = if (danger) DANGER else Milo.text, fontSize = 16.sp,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 12.dp),
    )
}


// ============================== feed ==============================

private val FEED_REACTIONS = listOf("\uD83D\uDD25", "\u2764\uFE0F", "\uD83D\uDC4F", "\uD83D\uDE2E")

@Composable
private fun LiveDot() {
    val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "live")
    val a by t.animateFloat(0.35f, 1f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(800), androidx.compose.animation.core.RepeatMode.Reverse), label = "liveA")
    Box(Modifier.size(8.dp).graphicsLayer { alpha = a }.clip(CircleShape).background(Milo.amber))
}

private fun react(vm: MainViewModel, ctx: android.content.Context, f: Friend, now: SharedSong, emoji: String) {
    vm.social.send(f.id, "text", emoji + " " + now.title, null)
    android.widget.Toast.makeText(ctx, "Sent to " + f.name, android.widget.Toast.LENGTH_SHORT).show()
}

@Composable
private fun EmojiTile(emoji: String, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(Milo.card2).border(1.dp, Milo.line, CircleShape)
            .pressable(true, onClick),
        contentAlignment = Alignment.Center,
    ) { Text(emoji, fontSize = 20.sp) }
}

/** The social feed: who is listening to what right now, with one-tap reactions, plus the playlists friends share. */
@Composable
private fun FeedContent(
    vm: MainViewModel, fr: List<Friend>, s: com.ubayanda.milonative.data.SocialUi, modifier: Modifier,
    onChat: (Friend) -> Unit, onProfile: (Friend) -> Unit, onToChats: () -> Unit, requests: Int,
) {
    val ctx = LocalContext.current
    var story by remember { mutableStateOf<Friend?>(null) }
    var plFor by remember { mutableStateOf<Friend?>(null) }
    val live = fr.filter { s.shared[it.id]?.now != null }
    val withPl = fr.filter { s.shared[it.id]?.playlists?.isNotEmpty() == true }
    val ordered = live + fr.filter { it !in live }
    val shape = RoundedCornerShape(Rad.xl)
    LazyColumn(modifier, contentPadding = PaddingValues(bottom = 40.dp)) {
        if (requests > 0) item(key = "req") {
            Row(
                Modifier.padding(horizontal = 18.dp, vertical = 6.dp).fillMaxWidth().lift(RoundedCornerShape(Rad.lg), 8f).clip(RoundedCornerShape(Rad.lg))
                    .background(Milo.amber.copy(alpha = 0.16f)).pressable(true, onToChats).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.PersonAdd, null, tint = Milo.amber, modifier = Modifier.size(22.dp))
                Text(if (requests == 1) "1 friend request is waiting" else "$requests friend requests are waiting", color = Milo.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 12.dp).weight(1f))
                Text("View", color = Milo.amber, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (fr.isNotEmpty()) item(key = "stories") {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(ordered, key = { "st" + it.id }) { f ->
                    val isLive = s.shared[f.id]?.now != null
                    Column(
                        Modifier.width(74.dp).clip(RoundedCornerShape(16.dp)).pressable(true) { if (isLive) story = f else onProfile(f) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        UserAvatar(f.name, f.avatar, 64.dp, ring = isLive)
                        Text(f.name.substringBefore(' '), color = if (isLive) Milo.text else Milo.dim, fontSize = 12.sp, fontWeight = if (isLive) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                        if (isLive) Text("listening", color = Milo.amber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        if (fr.isEmpty()) item(key = "nofr") {
            Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Your feed starts with friends", color = Milo.text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text("Add someone from the Chats tab (the person icon). You will see what they are playing and can react with one tap.", color = Milo.dim, fontSize = 15.sp)
                SmallBtn("Open chats", true, onToChats)
            }
        } else if (live.isEmpty() && withPl.isEmpty()) item(key = "quiet") {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("It's quiet right now", color = Milo.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("When a friend plays something and shares it, it shows up here. Turn on sharing in Profile so they can see yours too.", color = Milo.dim, fontSize = 14.sp)
            }
        }
        if (live.isNotEmpty()) item(key = "lh") { Text("Listening now", color = Milo.dim, style = Ty.micro, letterSpacing = 1.sp, modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 2.dp)) }
        items(live, key = { "nc" + it.id }) { f ->
            val now = s.shared[f.id]?.now ?: return@items
            val quote by androidx.compose.runtime.produceState(emptyList<String>(), now.title, now.artist) { value = vm.lyricPreview(now.title, now.artist, now.durSec) }
            // a post, like a thread: avatar and a line down the left, the song and its lyrics on the right
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth().lift(shape, 14f).glass(shape, 1.2f, Milo.card).border(1.dp, glassStroke(0.8f), shape).padding(16.dp).height(androidx.compose.foundation.layout.IntrinsicSize.Min),
            ) {
                Column(Modifier.fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                    UserAvatar(f.name, f.avatar, 44.dp, ring = true)
                    Box(Modifier.padding(top = 6.dp).width(2.dp).weight(1f).clip(RoundedCornerShape(1.dp)).background(Brush.verticalGradient(listOf(Milo.amber.copy(alpha = 0.7f), Color.Transparent))))
                }
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(f.name, color = Milo.text, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        Spacer(Modifier.width(8.dp)); LiveDot(); Text("  now", color = Milo.amber, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        Text("Chat", color = Milo.dim, fontSize = 13.sp, modifier = Modifier.clip(RoundedCornerShape(12.dp)).pressable(true) { onChat(f) }.padding(horizontal = 10.dp, vertical = 6.dp))
                    }
                    Text("is listening to", color = Milo.dim, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(Rad.md)).background(Milo.card2).padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Cover(vm, sharedTrack(now), 64.dp, Rad.sm, px = 200)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(now.title, color = Milo.text, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(now.artist, color = Milo.dim, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    if (quote.isNotEmpty()) Row(Modifier.padding(top = 12.dp)) {
                        Box(Modifier.width(3.dp).height((quote.size * 22).dp).clip(RoundedCornerShape(2.dp)).background(Milo.amber))
                        Column(Modifier.padding(start = 10.dp)) {
                            quote.forEach { Text(it, color = Milo.text.copy(alpha = 0.9f), fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            Text(if (!now.local && (now.url.isNotEmpty() || now.yt.isNotEmpty())) "Tap Listen too and the lyrics play in sync" else "Lyrics", color = Milo.dim, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        FEED_REACTIONS.forEach { e -> EmojiTile(e) { react(vm, ctx, f, now, e) } }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (!now.local && (now.url.isNotEmpty() || now.yt.isNotEmpty())) SmallBtn("Listen too", true) { playShared(vm, ctx, now) }
                        SmallBtn("Reply", false) { onChat(f) }
                    }
                }
            }
        }
        if (withPl.isNotEmpty()) item(key = "ph") { Text("Shared playlists", color = Milo.dim, style = Ty.micro, letterSpacing = 1.sp, modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 2.dp)) }
        items(withPl, key = { "pc" + it.id }) { f ->
            val lists = s.shared[f.id]?.playlists ?: return@items
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth().lift(shape, 10f).glass(shape, 1.1f, Milo.card).border(1.dp, glassStroke(0.7f), shape)
                    .pressable(true) { plFor = f }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                UserAvatar(f.name, f.avatar, 46.dp)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(f.name + " shared " + lists.size + (if (lists.size == 1) " playlist" else " playlists"), color = Milo.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(lists.take(3).joinToString(" \u00b7 ") { it.name }, color = Milo.dim, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.Rounded.PlayArrow, null, tint = Milo.amber, modifier = Modifier.size(26.dp))
            }
        }
    }
    story?.let { f ->
        val now = s.shared[f.id]?.now
        if (now == null) story = null else com.ubayanda.milonative.ui.components.MiloPopup({ story = null }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(f.name, f.avatar, 40.dp, ring = true)
                Column(Modifier.padding(start = 12.dp)) {
                    Text(f.name, color = Milo.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("listening now", color = Milo.amber, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Cover(vm, sharedTrack(now), 220.dp, Rad.lg, px = 500) }
            Text(now.title, color = Milo.text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 14.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(now.artist, color = Milo.dim, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                FEED_REACTIONS.forEach { e -> EmojiTile(e) { react(vm, ctx, f, now, e); story = null } }
            }
            Spacer(Modifier.height(8.dp))
            if (!now.local && (now.url.isNotEmpty() || now.yt.isNotEmpty())) com.ubayanda.milonative.ui.components.PopupAction(Icons.Rounded.PlayArrow, "Listen too") { story = null; playShared(vm, ctx, now) }
            com.ubayanda.milonative.ui.components.PopupAction(Icons.Rounded.ChatBubble, "Message " + f.name.substringBefore(' ')) { story = null; onChat(f) }
        }
    }
    plFor?.let { f -> FriendPlaylistsDialog(vm, f.name, s.shared[f.id]?.playlists ?: emptyList()) { plFor = null } }
}


@Composable
private fun TypingDots() {
    val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "typing")
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 0..2) {
            val a by t.animateFloat(0.25f, 1f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(500, delayMillis = i * 140), androidx.compose.animation.core.RepeatMode.Reverse), label = "d$i")
            Box(Modifier.size(7.dp).graphicsLayer { alpha = a; scaleX = 0.7f + 0.5f * a; scaleY = 0.7f + 0.5f * a }.clip(CircleShape).background(Milo.amber))
        }
    }
}
