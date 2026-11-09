package com.ubayanda.milonative.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubayanda.milonative.data.Member
import com.ubayanda.milonative.data.RoomSnap
import com.ubayanda.milonative.data.Sessions
import com.ubayanda.milonative.model.Track
import com.ubayanda.milonative.ui.MainViewModel
import com.ubayanda.milonative.ui.components.Cover
import com.ubayanda.milonative.ui.components.QrImage
import com.ubayanda.milonative.ui.theme.Milo
import com.ubayanda.milonative.ui.theme.Ty

private val REACTIONS = listOf("fire" to "🔥", "heart" to "❤️", "clap" to "👏", "laugh" to "😂", "music" to "🎶", "wow" to "😮")
private fun emoji(kind: String) = REACTIONS.firstOrNull { it.first == kind }?.second ?: "🎵"

/** milo. Sessions: listening rooms. */
@Composable
fun SessionsScreen(vm: MainViewModel, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val room by vm.room.collectAsState()
    Box(Modifier.fillMaxSize().background(Milo.bg).statusBarsPadding().navigationBarsPadding().clickable(enabled = false) {}) {
        val r = room
        if (r == null) SessionsHome(vm, onClose) else RoomView(vm, r, onClose)
    }
}

@Composable
private fun Field(value: String, hint: String, onChange: (String) -> Unit, onDone: () -> Unit = {}, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Box(modifier.fillMaxWidth().clip(shape).background(Color.Black.copy(alpha = 0.3f), shape).border(1.dp, Milo.line, shape).padding(horizontal = 14.dp, vertical = 12.dp)) {
        if (value.isEmpty()) Text(hint, color = Milo.dim, fontSize = 15.sp)
        BasicTextField(
            value = value, onValueChange = onChange, singleLine = true, textStyle = TextStyle(color = Milo.text, fontSize = 15.sp), cursorBrush = SolidColor(Milo.amber),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { onDone() }), modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SwitchRow(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Milo.text, fontSize = 14.sp, modifier = Modifier.weight(1f).padding(end = 12.dp))
        Switch(checked = on, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedTrackColor = Milo.amber, checkedThumbColor = Color.Black))
    }
}

@Composable
private fun SessionsHome(vm: MainViewModel, onClose: () -> Unit) {
    val msg by vm.sessionMsg.collectAsState()
    val pub by vm.publicRooms.collectAsState()
    var code by remember { mutableStateOf(vm.pendingCode) }
    var name by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(false) }
    var open by remember { mutableStateOf(false) }
    LaunchedEffect(vm.pendingCode) { if (vm.pendingCode.isNotEmpty()) code = vm.pendingCode }
    LaunchedEffect(Unit) { while (true) { vm.refreshPublicRooms(); kotlinx.coroutines.delay(8000) } }
    val card = RoundedCornerShape(20.dp)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { Pill("Back", null, false, compact = true, onClick = onClose); Spacer(Modifier.width(12.dp)); Text("milo. Sessions", color = Milo.text, style = Ty.title) } }
        item { Text("Listen together in a room: one host plays, everyone hears the same song at the same time, and can suggest songs and react.", color = Milo.dim, style = Ty.caption) }
        if (!vm.signedIn) item { Text("Sign in first (Profile) to start or join a session.", color = Milo.amber, style = Ty.label) }
        if (msg.isNotEmpty()) item { Text(msg, color = Color(0xFFFF8A80), style = Ty.caption) }
        item {
            Column(Modifier.fillMaxWidth().clip(card).background(Milo.card2).padding(16.dp)) {
                Text("Join with a code", color = Milo.text, style = Ty.heading)
                Spacer(Modifier.height(8.dp))
                Field(code, "6-letter code, or paste an invite link", { code = it }, { vm.joinRoom(code) })
                Row(Modifier.padding(top = 10.dp)) { Pill("Join", null, true, compact = true) { vm.joinRoom(code) } }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().clip(card).background(Milo.card2).padding(16.dp)) {
                Text("Start a session", color = Milo.text, style = Ty.heading)
                Spacer(Modifier.height(8.dp))
                Field(name, "Session name (optional)", { name = it })
                SwitchRow("Public: listed so anyone signed in can join", isPublic) { isPublic = it }
                SwitchRow("Open queue: listeners add songs without approval", open) { open = it }
                Text(if (isPublic) "Public sessions appear in the list below while you are hosting." else "Private: only people with your code or link can join.", color = Milo.dim, style = Ty.micro, modifier = Modifier.padding(top = 6.dp))
                Row(Modifier.padding(top = 10.dp)) { Pill("Start session", null, true, compact = true) { vm.createRoom(name, isPublic, open) } }
            }
        }
        item { Text("Public sessions", color = Milo.dim, style = Ty.label) }
        if (pub.isEmpty()) item { Text("None live right now.", color = Milo.dim, style = Ty.caption) }
        items(pub, key = { it.code }) { p ->
            Row(Modifier.fillMaxWidth().clip(card).background(Milo.card).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(p.name, color = Milo.text, style = Ty.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Host " + p.host + " · " + p.listeners + " listening" + (if (p.now.isNotEmpty()) " · " + p.now else ""), color = Milo.dim, style = Ty.caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Pill("Join", null, true, compact = true) { vm.joinRoom(p.code) }
            }
        }
        item { Text("Be kind: hosts can mute or remove people, and you can report or block anyone from the participant list.", color = Milo.dim, style = Ty.micro) }
    }
}

@Composable
private fun PlayingBars(active: Boolean) {
    val t = rememberInfiniteTransition(label = "bars")
    Row(Modifier.height(18.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
        listOf(380, 520, 450).forEachIndexed { i, ms ->
            val h by t.animateFloat(0.25f, 1f, infiniteRepeatable(tween(ms, easing = LinearEasing), RepeatMode.Reverse, initialStartOffset = androidx.compose.animation.core.StartOffset(i * 120)), label = "b$i")
            Box(Modifier.width(4.dp).height((18 * (if (active) h else 0.25f)).dp).clip(RoundedCornerShape(2.dp)).background(Milo.amber))
        }
    }
}

@Composable
private fun Avatar(m: Member, onClick: () -> Unit) {
    val hue = (m.name.hashCode().toLong().let { if (it < 0) -it else it } % 360).toFloat()
    Box(Modifier.size(46.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(Color.hsv(hue, 0.45f, 0.75f)).then(if (m.host) Modifier.border(2.dp, Milo.amber, CircleShape) else Modifier), contentAlignment = Alignment.Center) {
            Text(m.name.take(1).uppercase().ifEmpty { "?" }, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        }
        Box(Modifier.align(Alignment.BottomEnd).size(11.dp).clip(CircleShape).background(if (m.online) Color(0xFF4CD964) else Milo.dim).border(1.5.dp, Milo.bg, CircleShape))
        if (m.muted) Text("🔇", fontSize = 12.sp, modifier = Modifier.align(Alignment.TopEnd))
    }
}

@Composable
private fun RoomView(vm: MainViewModel, r: RoomSnap, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val events by vm.roomEvents.collectAsState()
    val msg by vm.sessionMsg.collectAsState()
    val follow by vm.follow.collectAsState()
    val ps by vm.player.state.collectAsState()
    var picked by remember { mutableStateOf<Member?>(null) }
    var showQr by remember { mutableStateOf(false) }
    var q by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Track>>(emptyList()) }
    var chat by remember { mutableStateOf("") }
    val card = RoundedCornerShape(20.dp)
    val hs = r.state
    val now = System.currentTimeMillis()
    val away = Sessions.hostAway(r.hostSeen, now)

    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill("Hide", null, false, compact = true, onClick = onClose); Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) { Text(r.name, color = Milo.text, style = Ty.title, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(if (r.isHost) "You are the host" else "Hosted by " + (r.members.firstOrNull { it.host }?.name ?: ""), color = Milo.dim, style = Ty.caption) }
                Pill(if (r.isHost) "End" else "Leave", null, false, compact = true) { vm.leaveRoom() }
            }
        }
        if (msg.isNotEmpty()) item { Text(msg, color = Color(0xFFFF8A80), style = Ty.caption) }

        item {
            Column(Modifier.fillMaxWidth().clip(card).background(Milo.card2).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val showing = if (r.isHost) vm.trackById(ps.currentId) else hs?.track
                Cover(vm, showing, 180.dp, radius = 18.dp, px = 540)
                Spacer(Modifier.height(10.dp))
                val title = if (r.isHost) (showing?.title ?: "Nothing playing") else (hs?.title ?: "Nothing playing yet")
                val artist = if (r.isHost) (showing?.artist ?: "Pick a song to start") else (hs?.artist ?: "")
                Text(title, color = Milo.text, style = Ty.heading, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(artist, color = Milo.dim, style = Ty.caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                PlayingBars(if (r.isHost) ps.isPlaying else (hs?.playing == true && !away))
                if (!r.isHost) {
                    Spacer(Modifier.height(6.dp))
                    when {
                        away -> Text("The host seems to be away. Waiting…", color = Milo.amber, style = Ty.caption)
                        hs != null && !Sessions.playable(hs.track) -> Text("The host is playing a song from their own phone. It can't be heard here, so you'll hear the next shared one.", color = Milo.amber, style = Ty.caption)
                        hs == null -> Text("Waiting for the host to play something.", color = Milo.dim, style = Ty.caption)
                    }
                    SwitchRow("Follow the host (keeps my player in sync)", follow) { vm.setFollow(it) }
                } else {
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill(if (ps.isPlaying) "Pause" else "Play", null, true, compact = true) { vm.player.toggle() }
                        Pill("Play next from queue", null, false, compact = true, enabled = r.queue.any { it.status == "queued" }) { vm.hostPlayNext() }
                    }
                    Text("Only online (YouTube) songs are shared; songs from your own storage can't be heard by others.", color = Milo.dim, style = Ty.micro, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }

        item {
            Column(Modifier.fillMaxWidth().clip(card).background(Milo.card).padding(16.dp)) {
                Text("Invite", color = Milo.text, style = Ty.heading)
                Text(r.code, color = Milo.amber, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 4.dp))
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("Share", null, true, compact = true) {
                        ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "Join my milo. session \"" + r.name + "\": " + Sessions.link(r.code) + "  (or enter code " + r.code + " in milo. > Profile > Sessions)"), null))
                    }
                    Pill(if (showQr) "Hide QR" else "Show QR", null, false, compact = true) { showQr = !showQr }
                }
                if (showQr) Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) { QrImage(Sessions.link(r.code), 200.dp) }
                Text(if (r.isPublic) "Public: listed for everyone signed in." else "Private: only people with the code or link can join.", color = Milo.dim, style = Ty.micro, modifier = Modifier.padding(top = 6.dp))
            }
        }

        item {
            Column(Modifier.fillMaxWidth().clip(card).background(Milo.card).padding(16.dp)) {
                Text("Listening now · " + r.members.count { it.online }, color = Milo.text, style = Ty.heading)
                LazyRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(r.members, key = { it.uid }) { m -> if (!vm.isBlocked(m.uid)) Avatar(m) { picked = m } }
                }
                Text("Tap a person for options.", color = Milo.dim, style = Ty.micro, modifier = Modifier.padding(top = 4.dp))
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                REACTIONS.forEach { (k, e) -> Box(Modifier.size(48.dp).clip(CircleShape).background(Milo.card2).clickable(enabled = !r.iAmMuted) { vm.roomReact(k) }, contentAlignment = Alignment.Center) { Text(e, fontSize = 22.sp) } }
            }
            if (r.iAmMuted) Text("The host muted you.", color = Milo.amber, style = Ty.caption, modifier = Modifier.padding(top = 6.dp))
        }

        item {
            Column(Modifier.fillMaxWidth().clip(card).background(Milo.card).padding(16.dp)) {
                Text("Queue", color = Milo.text, style = Ty.heading)
                if (r.queue.isEmpty()) Text("Empty. " + (if (r.openQueue) "Anyone can add songs." else "Listeners suggest songs; the host approves them."), color = Milo.dim, style = Ty.caption, modifier = Modifier.padding(top = 4.dp))
                r.queue.filter { !vm.isBlocked(it.byUid) }.forEach { qi ->
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(qi.track.title, color = Milo.text, style = Ty.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(qi.track.artist + " · " + qi.by + (if (qi.status == "suggested") " · waiting for approval" else ""), color = Milo.dim, style = Ty.caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (r.isHost) {
                            if (qi.status == "suggested") Pill("Approve", null, true, compact = true) { vm.roomQueueAction(qi.id, "approve") }
                            Spacer(Modifier.width(6.dp))
                            Pill("Remove", null, false, compact = true) { vm.roomQueueAction(qi.id, "remove") }
                        } else if (qi.byUid == r.myUid) Pill("Remove", null, false, compact = true) { vm.roomQueueAction(qi.id, "remove") }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Field(q, "Search a song to " + (if (r.isHost || r.openQueue) "add" else "suggest"), { q = it }, { vm.roomSearch(q) { results = it } })
                Row(Modifier.padding(top = 8.dp)) { Pill("Search", null, false, compact = true) { vm.roomSearch(q) { results = it } } }
                results.forEach { t ->
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(t.title, color = Milo.text, style = Ty.label, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(t.artist, color = Milo.dim, style = Ty.caption, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        Pill(if (r.isHost || r.openQueue) "Add" else "Suggest", null, true, compact = true, enabled = !r.iAmMuted) { vm.roomSuggest(t); results = results - t }
                    }
                }
            }
        }

        item {
            Column(Modifier.fillMaxWidth().clip(card).background(Milo.card).padding(16.dp)) {
                Text("Activity", color = Milo.text, style = Ty.heading)
                if (events.isEmpty()) Text("Quiet so far.", color = Milo.dim, style = Ty.caption, modifier = Modifier.padding(top = 4.dp))
                events.takeLast(20).reversed().forEach { e ->
                    val line = when (e.kind) { "react" -> e.name + "  " + emoji(e.body); "join" -> e.name + " joined"; "leave" -> e.name + " left"; else -> e.name + ": " + e.body }
                    Text(line, color = if (e.kind == "msg" || e.kind == "react") Milo.text else Milo.dim, style = Ty.caption, modifier = Modifier.padding(top = 4.dp))
                }
                Spacer(Modifier.height(8.dp))
                Field(chat, "Say something…", { chat = it.take(140) }, { vm.roomSay(chat); chat = "" })
            }
        }
    }

    picked?.let { m ->
        val self = m.uid == r.myUid
        androidx.compose.ui.window.Dialog(onDismissRequest = { picked = null }) {
            Column(Modifier.clip(card).background(Milo.card2).padding(18.dp)) {
                Text(m.name + (if (m.host) " · host" else ""), color = Milo.text, style = Ty.heading)
                Text(if (m.online) "Listening now" else "Not active right now", color = Milo.dim, style = Ty.caption, modifier = Modifier.padding(top = 2.dp, bottom = 10.dp))
                if (self) Text("That's you.", color = Milo.dim, style = Ty.caption) else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (r.isHost) {
                            Pill(if (m.muted) "Unmute" else "Mute", null, false, compact = true) { vm.roomModerate(m.uid, if (m.muted) "unmute" else "mute"); picked = null }
                            Pill("Remove from session", null, false, compact = true) { vm.roomModerate(m.uid, "kick"); picked = null }
                            Pill("Ban from session", null, false, compact = true) { vm.roomModerate(m.uid, "ban"); picked = null }
                        }
                        Pill("Report", null, false, compact = true) { vm.roomReport(m.uid, "Reported from session " + r.code); picked = null }
                        Pill("Block (hide on this phone)", null, false, compact = true) { vm.blockPerson(m.uid); picked = null }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Pill("Close", null, true, compact = true) { picked = null }
            }
        }
    }
}
