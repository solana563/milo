package com.ubayanda.milonative.ui.screens

import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.asImageBitmap
import com.ubayanda.milonative.data.ChatMedia
import com.ubayanda.milonative.data.ChatMsg
import com.ubayanda.milonative.data.VoicePlayer
import com.ubayanda.milonative.data.VoiceRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import com.ubayanda.milonative.data.SharedPlaylist
import com.ubayanda.milonative.data.SharedSong
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubayanda.milonative.data.Friend
import com.ubayanda.milonative.ui.AccountUi
import com.ubayanda.milonative.ui.MainViewModel
import com.ubayanda.milonative.ui.theme.Milo
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
private fun TopBar(title: String, onBack: () -> Unit, trailing: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 6.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back", tint = Milo.text) }
        Text(title, color = Milo.text, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
private fun Avatar(name: String, size: Int = 46) {
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape((size / 3).dp)).background(Brush.verticalGradient(listOf(Color(0xFFF99C00), Color(0xFFB36F00)))),
        contentAlignment = Alignment.Center,
    ) { Text((name.firstOrNull() ?: '?').uppercaseChar().toString(), color = Color.White, fontSize = (size * 0.42f).sp, fontWeight = FontWeight.SemiBold) }
}

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
    val shape = RoundedCornerShape(24.dp)
    Box(modifier.clip(shape).background(Color.Black.copy(alpha = 0.3f), shape).border(1.dp, Milo.line, shape).padding(horizontal = 18.dp, vertical = 14.dp)) {
        if (value.isEmpty()) Text(hint, color = Milo.dim, fontSize = 16.sp)
        BasicTextField(
            value = value, onValueChange = onChange, singleLine = true,
            textStyle = TextStyle(color = Milo.text, fontSize = 16.sp), cursorBrush = SolidColor(Milo.amber),
            keyboardOptions = KeyboardOptions(imeAction = imeAction),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onAny = { onGo() }),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun FriendsScreen(vm: MainViewModel, acct: AccountUi, onBack: () -> Unit, onChat: (Friend) -> Unit) {
    val s by vm.social.ui.collectAsState()
    var name by remember { mutableStateOf("") }
    LaunchedEffect(acct.signedIn) {
        while (acct.signedIn) {
            vm.social.refresh()
            delay(10_000)
        }
    }
    Column(Modifier.fillMaxSize().background(Milo.panel)) {
        TopBar("Friends", onBack)
        if (!acct.signedIn) {
            Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Sign in to add friends and chat", color = Milo.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Your account works on the web milo. too, so your friends and chats are the same everywhere.", color = Milo.dim, fontSize = 15.sp)
                SmallBtn("Log in", true) { vm.openGate("in") }
                SmallBtn("Sign up free", false) { vm.openGate("up") }
            }
            return@Column
        }
        LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 40.dp)) {
            item {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Add a friend", color = Milo.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Field(name, { name = it; vm.social.clearErr() }, "Their username", Modifier.weight(1f), ImeAction.Send) { vm.social.addByUsername(name); name = "" }
                        SmallBtn("Add", true) { vm.social.addByUsername(name); name = "" }
                    }
                    if (s.err.isNotEmpty()) Text(s.err, color = Color(0xFFFF8A80), fontSize = 14.sp)
                    if (s.note.isNotEmpty()) Text(s.note, color = Milo.amber, fontSize = 14.sp)
                }
            }
            val req = s.friends.filter { it.status == "incoming" }
            val fr = s.friends.filter { it.status == "friend" }
            val out = s.friends.filter { it.status == "outgoing" }
            if (req.isNotEmpty()) {
                item { Text("Requests", color = Milo.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 18.dp, top = 18.dp, bottom = 6.dp)) }
                items(req, key = { "q" + it.id }) { f ->
                    PersonRow(f, "wants to be friends", false) {
                        SmallBtn("Accept", true) { vm.social.accept(f) }
                        Spacer(Modifier.size(8.dp))
                        SmallBtn("Decline", false) { vm.social.remove(f) }
                    }
                }
            }
            item { Text("Your friends", color = Milo.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 18.dp, top = 18.dp, bottom = 6.dp)) }
            if (fr.isEmpty()) {
                item {
                    Text(
                        if (s.loading) "Loading…" else "No friends yet. Add someone by their username and they can accept from their milo.",
                        color = Milo.dim, fontSize = 15.sp, modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
                    )
                }
            }
            items(fr, key = { "f" + it.id }) { f ->
                var menu by remember { mutableStateOf(false) }
                var showPl by remember { mutableStateOf(false) }
                val ctx = androidx.compose.ui.platform.LocalContext.current
                val sh = s.shared[f.id]
                val now = sh?.now
                val sub = when {
                    f.id in s.unread -> "New message"
                    now != null -> "♪ " + now.title + if (now.artist.isNotEmpty()) " · " + now.artist else ""
                    else -> "Tap to chat"
                }
                if (showPl && sh != null) FriendPlaylistsDialog(vm, f.name, sh.playlists) { showPl = false }
                PersonRow(f, sub, f.id in s.unread || now != null, onClick = { onChat(f) }) {
                    if (now != null && !now.local && (now.url.isNotEmpty() || now.yt.isNotEmpty())) {
                        SmallBtn("Listen", true) { playShared(vm, ctx, now) }
                        Spacer(Modifier.size(4.dp))
                    }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreHoriz, "More", tint = Milo.dim) }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text("Open chat") }, onClick = { menu = false; onChat(f) })
                            if (sh != null && sh.playlists.isNotEmpty()) DropdownMenuItem(text = { Text("Their playlists") }, onClick = { menu = false; showPl = true })
                            DropdownMenuItem(text = { Text("Remove friend (deletes the chat)") }, onClick = { menu = false; vm.social.remove(f) })
                        }
                    }
                }
            }
            if (out.isNotEmpty()) {
                item { Text("Sent", color = Milo.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 18.dp, top = 18.dp, bottom = 6.dp)) }
                items(out, key = { "o" + it.id }) { f ->
                    PersonRow(f, "Waiting for them to accept", false) { SmallBtn("Cancel", false) { vm.social.remove(f) } }
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
        onDismissRequest = onClose, containerColor = Milo.card,
        title = { Text("$who's playlists", color = Milo.text) },
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

@Composable
private fun PersonRow(f: Friend, sub: String, dot: Boolean, onClick: (() -> Unit)? = null, trailing: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(f.name)
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(f.name, color = Milo.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("@" + f.username + " · " + sub, color = if (dot) Milo.amber else Milo.dim, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        trailing()
    }
}

private val REACTIONS = listOf("❤️", "😂", "😮", "😢", "👍", "🔥")

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(vm: MainViewModel, f: Friend, onBack: () -> Unit) {
    val msgs by vm.social.msgs.collectAsState()
    val s by vm.social.ui.collectAsState()
    val ui by vm.player.state.collectAsState()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var replyTo by remember { mutableStateOf<ChatMsg?>(null) }
    var menuFor by remember { mutableStateOf<ChatMsg?>(null) }
    var viewing by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var recording by remember { mutableStateOf(false) }
    val recorder = remember { VoiceRecorder(ctx) }
    val list = rememberLazyListState()
    LaunchedEffect(f.id) { vm.social.openChat(f) }
    androidx.compose.runtime.DisposableEffect(f.id) { onDispose { vm.social.closeChat(); VoicePlayer.stop(); recorder.cancel() } }
    LaunchedEffect(msgs.size) { if (msgs.isNotEmpty()) list.scrollToItem(msgs.size - 1) }
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

    Column(Modifier.fillMaxSize().background(Milo.panel).imePadding()) {
        TopBar(f.name, onBack)
        Text("@${f.username} · messages disappear after 24 hours", color = Milo.dim, fontSize = 12.sp, modifier = Modifier.padding(start = 20.dp, bottom = 6.dp))
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = list, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (msgs.isEmpty()) item { Text("Say hi 👋", color = Milo.dim, fontSize = 15.sp, modifier = Modifier.padding(20.dp)) }
            items(msgs, key = { it.id }, contentType = { it.kind }) { m ->
                Column(Modifier.fillMaxWidth(), horizontalAlignment = if (m.mine) Alignment.End else Alignment.Start) {
                    val shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = if (m.mine) 20.dp else 6.dp, bottomEnd = if (m.mine) 6.dp else 20.dp)
                    val fg = if (m.mine) Color.Black else Milo.text
                    Column(
                        Modifier.widthIn(max = 290.dp).clip(shape).background(if (m.mine) Milo.amber else Milo.card2, shape)
                            .combinedClickable(onClick = {}, onLongClick = { menuFor = m })
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        val q = m.replyTo?.let { id -> msgs.firstOrNull { it.id == id } }
                        if (m.replyTo != null) {
                            Text(
                                if (q != null) (if (q.mine) "You: " else "") + q.shown else "Message expired",
                                color = fg.copy(alpha = 0.7f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(bottom = 6.dp).clip(RoundedCornerShape(8.dp)).background(fg.copy(alpha = 0.1f)).padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                        when (m.kind) {
                            "img" -> {
                                val bmp by androidx.compose.runtime.produceState<android.graphics.Bitmap?>(null, m.id) { value = withContext(Dispatchers.Default) { ChatMedia.photoBitmap(m.body) } }
                                val b = bmp
                                if (b != null) androidx.compose.foundation.Image(
                                    b.asImageBitmap(), "Photo", Modifier.widthIn(max = 250.dp).clip(RoundedCornerShape(14.dp)).clickable { viewing = b },
                                    contentScale = androidx.compose.ui.layout.ContentScale.FillWidth,
                                ) else Text("📷 Loading photo...", color = fg, fontSize = 15.sp)
                            }
                            "voice" -> {
                                val playing = tick >= 0 && VoicePlayer.playingId == m.id
                                Row(Modifier.clickable { VoicePlayer.toggle(ctx, m.id, m.body) { tick++ } }, verticalAlignment = Alignment.CenterVertically) {
                                    Icon(if (playing) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, if (playing) "Stop" else "Play", tint = fg, modifier = Modifier.size(30.dp))
                                    Text("Voice message", color = fg, fontSize = 16.sp, modifier = Modifier.padding(start = 6.dp))
                                }
                            }
                            else -> Text(m.shown, color = fg, fontSize = 16.sp)
                        }
                    }
                    if (m.rx.isNotEmpty()) {
                        val grouped = m.rx.values.groupingBy { it }.eachCount()
                        Row(Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            grouped.forEach { (e, n) ->
                                val mineRx = m.rx[me] == e
                                Text(
                                    if (n > 1) "$e $n" else e, fontSize = 13.sp, color = Milo.text,
                                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(if (mineRx) Milo.amber.copy(alpha = 0.35f) else Milo.card2)
                                        .clickable { vm.social.react(f.id, m, e) }.padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
                        }
                    }
                    Text(
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(m.at)) + if (m.mine && m.read) " · Seen" else "",
                        color = Milo.dim, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
        if (s.err.isNotEmpty()) Text(s.err, color = Color(0xFFFF8A80), fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        replyTo?.let { r ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(14.dp)).background(Milo.card2).padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Replying to " + r.shown, color = Milo.dim, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                IconButton(onClick = { replyTo = null }) { Icon(Icons.Rounded.Close, "Cancel reply", tint = Milo.dim) }
            }
        }
        if (recording) {
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { recorder.cancel(); recording = false }) { Icon(Icons.Rounded.Close, "Cancel", tint = Milo.dim) }
                Text("● Recording... tap send when done", color = Color(0xFFFF8A80), fontSize = 15.sp, modifier = Modifier.weight(1f))
                Box(Modifier.size(46.dp).clip(CircleShape).background(Milo.amber).clickable { finishVoice() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Send, "Send voice note", tint = Color.Black, modifier = Modifier.size(22.dp))
                }
            }
        } else {
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { photo.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Icon(Icons.Rounded.Image, "Send a photo", tint = Milo.amber)
                }
                if (current != null) {
                    IconButton(onClick = {
                        vm.social.send(f.id, "song", JSONObject().put("t", current.title).put("a", current.artist).put("al", current.album).toString(), replyTo?.id); replyTo = null
                    }) { Icon(Icons.Rounded.MusicNote, "Share what's playing", tint = Milo.amber) }
                }
                Field(text, { text = it; vm.social.clearErr() }, "Message", Modifier.weight(1f), ImeAction.Send) { go() }
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
            Column(Modifier.clip(RoundedCornerShape(24.dp)).background(Milo.card).padding(16.dp)) {
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
            androidx.compose.foundation.Image(b.asImageBitmap(), "Photo", Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { viewing = null }, contentScale = androidx.compose.ui.layout.ContentScale.Fit)
        }
    }
}

@Composable
private fun MenuLine(label: String, danger: Boolean = false, onClick: () -> Unit) {
    Text(
        label, color = if (danger) Color(0xFFFF8A80) else Milo.text, fontSize = 16.sp,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 12.dp),
    )
}
