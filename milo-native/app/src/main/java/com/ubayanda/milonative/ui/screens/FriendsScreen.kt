package com.ubayanda.milonative.ui.screens

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
import androidx.compose.foundation.layout.imePadding
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
                PersonRow(f, if (f.id in s.unread) "New message" else "Tap to chat", f.id in s.unread, onClick = { onChat(f) }) {
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreHoriz, "More", tint = Milo.dim) }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text("Open chat") }, onClick = { menu = false; onChat(f) })
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

@Composable
fun ChatScreen(vm: MainViewModel, f: Friend, onBack: () -> Unit) {
    val msgs by vm.social.msgs.collectAsState()
    val s by vm.social.ui.collectAsState()
    val ui by vm.player.state.collectAsState()
    var text by remember { mutableStateOf("") }
    val list = rememberLazyListState()
    LaunchedEffect(f.id) { vm.social.openChat(f) }
    androidx.compose.runtime.DisposableEffect(f.id) { onDispose { vm.social.closeChat() } }
    LaunchedEffect(msgs.size) { if (msgs.isNotEmpty()) list.scrollToItem(msgs.size - 1) }
    val current = vm.trackById(ui.currentId)

    fun go() { if (text.isNotBlank()) { vm.social.send(f.id, "text", text.trim()); text = "" } }

    Column(Modifier.fillMaxSize().background(Milo.panel).imePadding()) {
        TopBar(f.name, onBack)
        Text("@${f.username} · messages disappear after 24 hours", color = Milo.dim, fontSize = 12.sp, modifier = Modifier.padding(start = 20.dp, bottom = 6.dp))
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = list, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (msgs.isEmpty()) item { Text("Say hi 👋", color = Milo.dim, fontSize = 15.sp, modifier = Modifier.padding(20.dp)) }
            items(msgs, key = { it.id }) { m ->
                Column(Modifier.fillMaxWidth(), horizontalAlignment = if (m.mine) Alignment.End else Alignment.Start) {
                    val shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = if (m.mine) 20.dp else 6.dp, bottomEnd = if (m.mine) 6.dp else 20.dp)
                    Text(
                        m.shown, color = if (m.mine) Color.Black else Milo.text, fontSize = 16.sp,
                        modifier = Modifier.widthIn(max = 290.dp).clip(shape).background(if (m.mine) Milo.amber else Milo.card2, shape).padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                    Text(
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(m.at)) + if (m.mine && m.read) " · Seen" else "",
                        color = Milo.dim, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
        if (s.err.isNotEmpty()) Text(s.err, color = Color(0xFFFF8A80), fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (current != null) {
                IconButton(onClick = {
                    vm.social.send(f.id, "song", JSONObject().put("t", current.title).put("a", current.artist).put("al", current.album).toString())
                }) { Icon(Icons.Rounded.MusicNote, "Share what's playing", tint = Milo.amber) }
            }
            Field(text, { text = it; vm.social.clearErr() }, "Message", Modifier.weight(1f), ImeAction.Send) { go() }
            Box(Modifier.size(46.dp).clip(CircleShape).background(Milo.amber).clickable { go() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Send, "Send", tint = Color.Black, modifier = Modifier.size(22.dp))
            }
        }
    }
}
