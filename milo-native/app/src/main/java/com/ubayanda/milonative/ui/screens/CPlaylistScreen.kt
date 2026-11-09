package com.ubayanda.milonative.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.ubayanda.milonative.data.CPlaylists
import com.ubayanda.milonative.data.CplDetail
import com.ubayanda.milonative.model.Track
import com.ubayanda.milonative.ui.MainViewModel
import com.ubayanda.milonative.ui.components.Cover
import com.ubayanda.milonative.ui.components.IconAction
import com.ubayanda.milonative.ui.components.QrImage
import com.ubayanda.milonative.ui.theme.Milo
import com.ubayanda.milonative.ui.theme.Ty

/** Shared playlists: owner, editors and viewers, with change history and invite links. */
@Composable
fun CPlaylistScreen(vm: MainViewModel, onClose: () -> Unit) {
    val open by vm.cplOpen.collectAsState()
    BackHandler { if (open != null) vm.cplOpenList(null) else onClose() }
    LaunchedEffect(Unit) { while (true) { vm.cplRefresh(); kotlinx.coroutines.delay(6000) } }
    Box(Modifier.fillMaxSize().background(Milo.bg).statusBarsPadding().navigationBarsPadding().clickable(enabled = false) {}) {
        val d = open
        if (d == null) CplHome(vm, onClose) else CplDetailView(vm, d)
    }
}

@Composable
private fun CplField(value: String, hint: String, onChange: (String) -> Unit, onDone: () -> Unit = {}) {
    val shape = RoundedCornerShape(14.dp)
    Box(Modifier.fillMaxWidth().clip(shape).background(Color.Black.copy(alpha = 0.3f), shape).border(1.dp, Milo.line, shape).padding(horizontal = 14.dp, vertical = 12.dp)) {
        if (value.isEmpty()) Text(hint, color = Milo.dim, fontSize = 15.sp)
        BasicTextField(
            value = value, onValueChange = onChange, singleLine = true, textStyle = TextStyle(color = Milo.text, fontSize = 15.sp), cursorBrush = SolidColor(Milo.amber),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { onDone() }), modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CplSwitch(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Milo.text, fontSize = 14.sp, modifier = Modifier.weight(1f).padding(end = 12.dp))
        Switch(checked = on, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedTrackColor = Milo.amber, checkedThumbColor = Color.Black))
    }
}

@Composable
private fun CplHome(vm: MainViewModel, onClose: () -> Unit) {
    val list by vm.cplList.collectAsState()
    val pub by vm.cplPublic.collectAsState()
    val msg by vm.cplMsg.collectAsState()
    val loaded by vm.cplLoaded.collectAsState()
    var code by remember { mutableStateOf(vm.pendingPlaylistCode) }
    var name by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(false) }
    var editors by remember { mutableStateOf(true) }
    LaunchedEffect(vm.pendingPlaylistCode) { if (vm.pendingPlaylistCode.isNotEmpty()) code = vm.pendingPlaylistCode }
    val card = RoundedCornerShape(20.dp)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { Pill("Back", null, false, compact = true, onClick = onClose); Spacer(Modifier.width(12.dp)); Text("Shared playlists", color = Milo.text, style = Ty.title) } }
        item { Text("Build a playlist together. The owner decides who can edit; every change is logged. Online (YouTube) songs only, because songs stored on one phone can't play on another.", color = Milo.dim, style = Ty.caption) }
        if (!vm.signedIn) item { Text("Sign in first (Profile) to use shared playlists.", color = Milo.amber, style = Ty.label) }
        if (msg.isNotEmpty()) item { Text(msg, color = Color(0xFFFF8A80), style = Ty.caption) }
        item {
            Column(Modifier.fillMaxWidth().clip(card).background(Milo.card2).padding(16.dp)) {
                Text("Join with a code", color = Milo.text, style = Ty.heading)
                Spacer(Modifier.height(8.dp))
                CplField(code, "6-letter code, or paste an invite link", { code = it }, { vm.cplJoin(code) })
                Row(Modifier.padding(top = 10.dp)) { Pill("Join", null, true, compact = true) { vm.cplJoin(code) } }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().clip(card).background(Milo.card2).padding(16.dp)) {
                Text("New shared playlist", color = Milo.text, style = Ty.heading)
                Spacer(Modifier.height(8.dp))
                CplField(name, "Name", { name = it })
                CplSwitch("Public: listed for anyone signed in to find", isPublic) { isPublic = it }
                CplSwitch("People who join can edit (off = view only)", editors) { editors = it }
                Row(Modifier.padding(top = 10.dp)) { Pill("Create", null, true, compact = true) { vm.cplCreate(name, isPublic, editors); name = "" } }
            }
        }
        item { Text("My shared playlists", color = Milo.dim, style = Ty.label) }
        if (list.isEmpty()) item { Text(if (!vm.signedIn) "Sign in to see yours." else if (!loaded) "Loading…" else if (msg.isNotEmpty()) "Couldn't load. See the message above." else "None yet. Create one above or join with a code.", color = Milo.dim, style = Ty.caption) }
        items(list, key = { it.id }) { p ->
            Row(Modifier.fillMaxWidth().clip(card).background(Milo.card).clickable { vm.cplOpenList(p.id) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(p.name, color = Milo.text, style = Ty.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${p.count} songs · ${CPlaylists.roleLabel(p.role)}" + if (p.isPublic) " · public" else "", color = Milo.dim, style = Ty.caption)
                }
            }
        }
        item { Text("Public playlists", color = Milo.dim, style = Ty.label) }
        if (pub.isEmpty()) item { Text(if (loaded) "None listed." else "Loading…", color = Milo.dim, style = Ty.caption) }
        items(pub, key = { "pub" + it.code }) { p ->
            Row(Modifier.fillMaxWidth().clip(card).background(Milo.card).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(p.name, color = Milo.text, style = Ty.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${p.count} songs · by ${p.owner}", color = Milo.dim, style = Ty.caption)
                }
                Pill("Join", null, false, compact = true) { vm.cplJoin(p.code) }
            }
        }
    }
}

private fun ago(at: Long): String {
    val m = ((System.currentTimeMillis() - at) / 60000).coerceAtLeast(0)
    return when { m < 1 -> "just now"; m < 60 -> "${m}m ago"; m < 1440 -> "${m / 60}h ago"; else -> "${m / 1440}d ago" }
}

private fun describe(action: String, detail: String) = when (action) {
    "add" -> "added $detail"; "remove" -> "removed $detail"; "move" -> "moved $detail"; "join" -> "joined"; "settings" -> "changed settings"; else -> action
}

@Composable
private fun CplDetailView(vm: MainViewModel, d: CplDetail) {
    val ctx = LocalContext.current
    val msg by vm.cplMsg.collectAsState()
    val ps by vm.player.state.collectAsState()
    var q by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Track>>(emptyList()) }
    var tab by remember { mutableStateOf(0) }
    var showQr by remember { mutableStateOf(false) }
    var renaming by remember(d.id) { mutableStateOf(d.name) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val card = RoundedCornerShape(20.dp)
    val me = d.myRole
    val owner = me == "owner"
    val ids = d.items.map { it.id }

    fun search() {
        val term = q.trim(); if (term.isEmpty()) return
        scope.launch { results = try { vm.youtube.search(term).take(8) } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { emptyList() } }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill("Back", null, false, compact = true) { vm.cplOpenList(null) }; Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(d.name, color = Milo.text, style = Ty.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${d.items.size} songs · ${CPlaylists.roleLabel(me)}" + if (d.isPublic) " · public" else " · private", color = Milo.dim, style = Ty.caption)
                }
            }
        }
        if (msg.isNotEmpty()) item { Text(msg, color = Color(0xFFFF8A80), style = Ty.caption) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("Play", null, true, enabled = d.items.isNotEmpty(), compact = true) { vm.cplPlay(d) }
                Pill("Copy to my library", null, false, enabled = d.items.isNotEmpty(), compact = true) { vm.cplCopy(d) }
                Pill("Invite", null, false, compact = true) { showQr = !showQr }
            }
        }
        if (showQr) item {
            Column(Modifier.fillMaxWidth().clip(card).background(Milo.card2).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Code " + d.code, color = Milo.text, style = Ty.heading)
                Text("New members join as: " + CPlaylists.roleLabel(d.defaultRole), color = Milo.dim, style = Ty.caption)
                Spacer(Modifier.height(10.dp))
                QrImage(CPlaylists.link(d.code), 180.dp)
                Row(Modifier.padding(top = 10.dp)) {
                    Pill("Share link", null, true, compact = true) {
                        ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "Join my milo. playlist \"" + d.name + "\": " + CPlaylists.link(d.code) + "  (or enter code " + d.code + " in milo. > Profile > Shared playlists)"), null))
                    }
                }
            }
        }
        if (CPlaylists.canAdd(me)) item {
            Column(Modifier.fillMaxWidth().clip(card).background(Milo.card2).padding(16.dp)) {
                Text("Add songs", color = Milo.text, style = Ty.heading)
                Spacer(Modifier.height(8.dp))
                CplField(q, "Search YouTube", { q = it }, { search() })
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("Search", null, true, compact = true) { search() }
                    val cur = vm.trackById(ps.currentId)
                    if (cur != null && cur.uri.startsWith("yt:")) Pill("Add current song", null, false, compact = true) { vm.cplAdd(d.id, cur) }
                }
                results.forEach { t ->
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Cover(vm, t, 40.dp, 8.dp, 96)
                        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                            Text(t.title, color = Milo.text, style = Ty.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(t.artist, color = Milo.dim, style = Ty.caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Pill("Add", null, false, compact = true) { vm.cplAdd(d.id, t) }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Songs", "History", "People").forEachIndexed { i, l -> Pill(l, null, tab == i, compact = true) { tab = i } }
            }
        }
        if (tab == 0) {
            if (d.items.isEmpty()) item { Text(if (CPlaylists.canAdd(me)) "Empty. Search above to add the first song." else "Empty so far.", color = Milo.dim, style = Ty.caption) }
            itemsIndexed(d.items, key = { _, it -> it.id }) { i, it ->
                Row(Modifier.fillMaxWidth().clip(card).background(Milo.card).clickable { vm.cplPlay(d, i) }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Cover(vm, it.track, 44.dp, 8.dp, 96)
                    Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                        Text(it.track.title, color = Milo.text, style = Ty.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(it.track.artist + " · added by " + it.by, color = Milo.dim, style = Ty.micro, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (CPlaylists.canReorder(me)) {
                        if (i > 0) IconAction("▲", "Move up", Milo.amber) { vm.cplMove(it.id, CPlaylists.afterIdFor(ids, i, i - 1)) }
                        if (i < d.items.size - 1) IconAction("▼", "Move down", Milo.amber) { vm.cplMove(it.id, CPlaylists.afterIdFor(ids, i, i + 1)) }
                    }
                    if (CPlaylists.canRemove(me, it.byUid, d.myUid)) IconAction("✕", "Remove from playlist", Milo.dim) { vm.cplRemove(it.id) }
                }
            }
        }
        if (tab == 1) {
            if (d.history.isEmpty()) item { Text("No changes yet.", color = Milo.dim, style = Ty.caption) }
            items(d.history.size) { i ->
                val h = d.history[i]
                Text(h.name + " " + describe(h.action, h.detail) + " · " + ago(h.at), color = Milo.dim, style = Ty.caption)
            }
        }
        if (tab == 2) {
            items(d.members, key = { it.uid }) { m ->
                Row(Modifier.fillMaxWidth().clip(card).background(Milo.card).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(m.name + if (m.uid == d.myUid) " (you)" else "", color = Milo.text, style = Ty.body)
                        Text(CPlaylists.roleLabel(m.role), color = Milo.dim, style = Ty.caption)
                    }
                    if (owner && m.role != "owner") {
                        Pill(if (m.role == "editor") "View only" else "Can edit", null, false, compact = true) { vm.cplSetRole(d.id, m.uid, if (m.role == "editor") "viewer" else "editor") }
                        Spacer(Modifier.width(6.dp))
                        Pill("Remove", null, false, compact = true) { vm.cplSetRole(d.id, m.uid, "remove") }
                    }
                }
            }
            if (owner) item {
                Column(Modifier.fillMaxWidth().clip(card).background(Milo.card2).padding(16.dp)) {
                    Text("Settings", color = Milo.text, style = Ty.heading)
                    Spacer(Modifier.height(8.dp))
                    CplField(renaming, "Name", { renaming = it })
                    CplSwitch("Public: listed for anyone signed in to find", d.isPublic) { vm.cplSettings(d.id, renaming, it, d.defaultRole == "editor") }
                    CplSwitch("New members can edit (off = view only)", d.defaultRole == "editor") { vm.cplSettings(d.id, renaming, d.isPublic, it) }
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill("Save name", null, true, compact = true) { vm.cplSettings(d.id, renaming, d.isPublic, d.defaultRole == "editor") }
                        Pill("Delete playlist", null, false, compact = true) { vm.cplDelete(d.id) }
                    }
                }
            } else item { Row { Pill("Leave playlist", null, false, compact = true) { vm.cplLeave(d.id) } } }
        }
    }
}
