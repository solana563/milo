package com.ubayanda.milonative.ui.screens

import com.ubayanda.milonative.ui.components.MiloSlider
import com.ubayanda.milonative.ui.components.glass
import com.ubayanda.milonative.ui.components.lift
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.asImageBitmap
import com.ubayanda.milonative.data.ChatMedia
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubayanda.milonative.ui.AccountUi
import com.ubayanda.milonative.ui.MainViewModel
import com.ubayanda.milonative.ui.components.ScreenTitle
import com.ubayanda.milonative.ui.theme.Milo
import com.ubayanda.milonative.ui.theme.Ty
import com.ubayanda.milonative.ui.theme.Sp
import com.ubayanda.milonative.ui.theme.Rad

@Composable
fun ProfileScreen(vm: MainViewModel, a: AccountUi, songs: Int, likes: Int, playlists: Int, hours: Int, bottomPad: Dp) {
    var editing by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    val syncMsg by vm.syncMsg.collectAsState()
    Column(Modifier.fillMaxSize().verticalScroll(vm.mem.scroll("profile")).padding(bottom = bottomPad)) {
        ScreenTitle("Profile")
        val shape = RoundedCornerShape(com.ubayanda.milonative.ui.theme.Rad.xl)
        // the card takes the playing song's colour, fading to dark, like the web profile
        val tint = vm.tint.collectAsState().value
        val base = if (tint == 0) Milo.card2 else lerp(Color(tint), Color.Black, 0.45f)
        val cardBrush = Brush.linearGradient(listOf(base, lerp(base, Color.Black, 0.55f), Milo.card))
        val coverBmp = remember(a.cover) { if (a.cover.isEmpty()) null else ChatMedia.photoBitmap(a.cover) }
        val avatarBmp = remember(a.avatar) { if (a.avatar.isEmpty()) null else ChatMedia.photoBitmap(a.avatar) }
        Box(Modifier.fillMaxWidth().padding(horizontal = Sp.gutter).clip(shape).background(cardBrush, shape).border(1.dp, Milo.line, shape)) {
        if (coverBmp != null) {
            androidx.compose.foundation.Image(coverBmp.asImageBitmap(), null, Modifier.matchParentSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.25f), Color.Black.copy(alpha = 0.8f)))))
        }
        Column(Modifier.padding(Sp.x6)) {
            Box(
                Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(Brush.verticalGradient(listOf(Color(0xFFF99C00), Color(0xFFB36F00)))),
                contentAlignment = Alignment.Center,
            ) {
                if (avatarBmp != null) androidx.compose.foundation.Image(avatarBmp.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                else Text((a.username.firstOrNull() ?: 'G').uppercaseChar().toString(), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(22.dp))
            Text(if (a.signedIn) a.username else "Guest", color = Milo.text, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 38.sp)
            Text(
                if (a.signedIn) "@" + a.username + " · Signed in" else "Not signed in",
                color = Milo.dim, fontSize = 16.sp, modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                if (a.signedIn) "Listening to $songs songs on milo." else "Create an account for friends and chat.",
                color = Milo.text, fontSize = 18.sp, modifier = Modifier.padding(top = 14.dp),
            )
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                Stat(likes, "Liked")
                Stat(playlists, "Playlists")
                Stat(hours, "Hours")
            }
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (a.signedIn) {
                    Pill("Edit profile", null, true) { editing = true }
                    Pill("Sign out", null, false) { vm.signOut() }
                } else {
                    Pill("Sign up free", null, true) { vm.openGate("up") }
                    Pill("Log in", null, false) { vm.openGate("in") }
                }
            }
        }
        }
        if (a.signedIn && syncMsg.isNotEmpty()) Text(syncMsg, color = Milo.dim, fontSize = 13.sp, modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 10.dp))
        Spacer(Modifier.height(16.dp))
        InsightsCard(vm)
        Spacer(Modifier.height(16.dp))
        ToolsCard(vm)
        Spacer(Modifier.height(16.dp))
        ServerCard(vm)
        Spacer(Modifier.height(16.dp))
        YouTubeKeyCard(vm)
        Spacer(Modifier.height(16.dp))
        ScrobbleCard(vm)
        Spacer(Modifier.height(16.dp))
        TogetherCard(vm)
        Spacer(Modifier.height(16.dp))
        BluetoothCard()
        Spacer(Modifier.height(16.dp))
        DownloadsCard(vm)
        Spacer(Modifier.height(16.dp))
        ReportCard()
        Spacer(Modifier.height(16.dp))
        AccentCard()
        Spacer(Modifier.height(16.dp))
        HomeLocalCard()
        Spacer(Modifier.height(16.dp))
        Box(Modifier.padding(horizontal = Sp.gutter)) { Pill("Show the welcome tour", null, false) { Tour.open.value = true } }
        if (a.signedIn) {
            Spacer(Modifier.height(16.dp))
            SharingCard(vm, a)
        }
        Spacer(Modifier.height(16.dp))
        BroughtByCard()
        if (a.signedIn) {
            Spacer(Modifier.height(18.dp))
            var othersMsg by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
            Text(
                "Sign out of my other devices", color = Milo.amber, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp).clip(RoundedCornerShape(12.dp))
                    .clickable { othersMsg = "Working…"; vm.signOutOtherDevices { e -> othersMsg = e ?: "Done. Other devices are signed out." } }
                    .padding(vertical = 10.dp),
            )
            if (othersMsg.isNotEmpty()) Text(othersMsg, color = Color(0xFFB8B8C0), fontSize = 13.sp, modifier = Modifier.padding(horizontal = 24.dp))
            Text(
                "Delete account", color = Color(0xFFFF8A80), fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp).clip(RoundedCornerShape(12.dp)).clickable { deleting = true }.padding(vertical = 10.dp),
            )
        }
    }
    if (editing) EditProfileDialog(vm, a) { editing = false }
    if (deleting) DeleteAccountDialog(vm) { deleting = false }
}

@Composable
private fun EditProfileDialog(vm: MainViewModel, a: AccountUi, onClose: () -> Unit) {
    var name by remember { mutableStateOf(a.name.ifEmpty { a.username }) }
    var user by remember { mutableStateOf(a.username) }
    var err by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var forCover by remember { mutableStateOf(false) }
    fun setPhoto(cover: Boolean, data: String?) {
        busy = true; err = ""
        vm.setPhoto(cover, data) { e -> busy = false; if (e != null) err = e }
    }
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val cover = forCover
            busy = true; err = ""
            scope.launch {
                val d = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    if (cover) ChatMedia.cropToData(ctx, uri, 640, 360, 68_000) else ChatMedia.cropToData(ctx, uri, 256, 256, 58_000)
                }
                if (d == null) { busy = false; err = "Couldn't use that photo." } else setPhoto(cover, d)
            }
        }
    }
    fun pick(cover: Boolean) {
        forCover = cover
        picker.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = { if (!busy) onClose() },
        containerColor = Milo.card, shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp), titleContentColor = Milo.text, textContentColor = Milo.text,
        title = { com.ubayanda.milonative.ui.components.BlurBehind(); Text("Edit profile") },
        text = {
            Column {
                ServerField(name, "Display name", false) { name = it }
                Spacer(Modifier.height(10.dp))
                ServerField(user, "Username", false) { user = it }
                Spacer(Modifier.height(14.dp))
                PhotoRow("Profile picture", a.avatar.isNotEmpty(), { pick(false) }) { setPhoto(false, null) }
                Spacer(Modifier.height(8.dp))
                PhotoRow("Cover photo", a.cover.isNotEmpty(), { pick(true) }) { setPhoto(true, null) }
                if (err.isNotEmpty()) Text(err, color = Color(0xFFFF8A80), fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp))
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = {
                if (busy) return@TextButton
                busy = true; err = ""
                vm.editProfile(name, user) { e -> busy = false; if (e == null) onClose() else err = e }
            }) { Text(if (busy) "Saving..." else "Save", color = Milo.amber) }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = { if (!busy) onClose() }) { Text("Cancel", color = Milo.dim) } },
    )
}

@Composable
private fun PhotoRow(label: String, has: Boolean, onChange: () -> Unit, onRemove: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Milo.text, style = Ty.label, modifier = Modifier.weight(1f))
        Text("Change", color = Milo.amber, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onChange).padding(8.dp))
        if (has) Text("Remove", color = Milo.dim, fontSize = 14.sp, modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onRemove).padding(8.dp))
    }
}

@Composable
private fun DeleteAccountDialog(vm: MainViewModel, onClose: () -> Unit) {
    var err by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = { if (!busy) onClose() },
        containerColor = Milo.card, shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp), titleContentColor = Milo.text, textContentColor = Milo.text,
        title = { com.ubayanda.milonative.ui.components.BlurBehind(); Text("Delete your account?") },
        text = {
            Column {
                Text("This permanently deletes your account, friends, messages and cloud backup. Songs on this phone are not touched.", color = Milo.dim, style = Ty.caption)
                if (err.isNotEmpty()) Text(err, color = Color(0xFFFF8A80), fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp))
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = {
                if (busy) return@TextButton
                busy = true; err = ""
                vm.deleteAccount { e -> busy = false; if (e == null) onClose() else err = e }
            }) { Text(if (busy) "Deleting..." else "Delete", color = Color(0xFFFF8A80)) }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = { if (!busy) onClose() }) { Text("Cancel", color = Milo.dim) } },
    )
}

@Composable
private fun ServerCard(vm: MainViewModel) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var url by rememberSaveable { mutableStateOf(vm.server.url) }
    var user by rememberSaveable { mutableStateOf(vm.server.user) }
    var pass by rememberSaveable { mutableStateOf("") }
    var status by remember { mutableStateOf(if (vm.server.configured) "Connected to " + vm.server.url else "") }
    var busy by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(com.ubayanda.milonative.ui.theme.Rad.xl)
    Column(Modifier.fillMaxWidth().padding(horizontal = Sp.gutter).lift(shape, 14f).glass(shape).padding(Sp.x6)) {
        Text("My server", color = Milo.text, style = Ty.title)
        Text(
            "Stream your own library from Navidrome or any Subsonic server. It shows up under Discover.",
            color = Milo.dim, style = Ty.caption, modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
        )
        ServerField(url, "Server address (http://192.168.1.5:4533)", false) { url = it }
        Spacer(Modifier.height(10.dp))
        ServerField(user, "Username", false) { user = it }
        Spacer(Modifier.height(10.dp))
        ServerField(pass, "Password", true) { pass = it }
        if (status.isNotEmpty()) Text(status, color = Milo.dim, fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp))
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Pill(if (busy) "Connecting..." else "Connect", null, true) {
                if (busy || url.isBlank() || user.isBlank()) return@Pill
                busy = true; status = ""
                scope.launch {
                    val pw = if (pass.isEmpty()) vm.server.password() else pass
                    vm.server.save(url, user, pw)
                    val err = vm.server.check()
                    if (err != null) vm.server.clear()
                    status = err ?: "Connected. Open Discover, then My server."
                    busy = false
                }
            }
            if (vm.server.configured) Pill("Disconnect", null, false) {
                vm.server.clear(); url = ""; user = ""; pass = ""; status = "Disconnected."
            }
        }
    }
}

@Composable
private fun SharingCard(vm: MainViewModel, a: AccountUi) {
    val shape = RoundedCornerShape(com.ubayanda.milonative.ui.theme.Rad.xl)
    Column(Modifier.fillMaxWidth().padding(horizontal = Sp.gutter).lift(shape, 14f).glass(shape).padding(Sp.x6)) {
        Text("Sharing with friends", color = Milo.text, style = Ty.title)
        Text("Only your friends can see these, and only song names (never your files).", color = Milo.dim, style = Ty.caption, modifier = Modifier.padding(top = 6.dp, bottom = 8.dp))
        ShareRow("Show what I'm listening to", a.shareNp) { vm.setShare(np = it) }
        ShareRow("Share my playlists and liked songs", a.sharePl) { vm.setShare(pl = it) }
    }
}

@Composable
private fun HomeLocalCard() {
    val shape = RoundedCornerShape(com.ubayanda.milonative.ui.theme.Rad.xl)
    val hide by com.ubayanda.milonative.data.Region.hideLocal.collectAsState()
    Column(Modifier.fillMaxWidth().padding(horizontal = Sp.gutter).lift(shape, 14f).glass(shape).padding(Sp.x6)) {
        Text("Home", color = Milo.text, style = Ty.title)
        Text("Online, Home can show only streaming picks. Your phone's songs stay in Library, and Home shows them again when you're offline.", color = Milo.dim, style = Ty.caption, modifier = Modifier.padding(top = 6.dp, bottom = 8.dp))
        ShareRow("Hide my phone's music on Home while online", hide) { com.ubayanda.milonative.data.Region.setHideLocal(it) }
    }
}

@Composable
private fun ShareRow(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Milo.text, fontSize = 16.sp, modifier = Modifier.weight(1f))
        com.ubayanda.milonative.ui.components.MiloSwitch(
            checked = on, onCheckedChange = onChange,
            colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = Milo.amber),
        )
    }
}

@Composable
private fun YouTubeKeyCard(vm: MainViewModel) {
    var key by rememberSaveable { mutableStateOf(vm.youtube.key) }
    var saved by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(com.ubayanda.milonative.ui.theme.Rad.xl)
    Column(Modifier.fillMaxWidth().padding(horizontal = Sp.gutter).lift(shape, 14f).glass(shape).padding(Sp.x6)) {
        Text("YouTube", color = Milo.text, style = Ty.title)
        Text(
            "YouTube songs now search and stream inside milo. without a key. This key is only a backup for the old search if the main one ever fails (free from Google Cloud).",
            color = Milo.dim, style = Ty.caption, modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
        )
        ServerField(key, "Your YouTube Data API key (optional)", false) { key = it; saved = false }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Pill("Save", null, true) { vm.youtube.setKey(key); saved = true }
            if (saved) Text(if (key.isBlank()) "Using the built-in key." else "Saved.", color = Milo.dim, fontSize = 14.sp)
        }
    }
}

@Composable
private fun ServerField(value: String, hint: String, secret: Boolean, onChange: (String) -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Milo.card2).padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        if (value.isEmpty()) Text(hint, color = Milo.dim, fontSize = 16.sp)
        androidx.compose.foundation.text.BasicTextField(
            value = value, onValueChange = onChange, singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(color = Milo.text, fontSize = 16.sp),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(Milo.text),
            visualTransformation = if (secret) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = if (secret) androidx.compose.ui.text.input.KeyboardType.Password else androidx.compose.ui.text.input.KeyboardType.Uri,
                autoCorrect = false,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun Stat(n: Int, label: String) {
    Column {
        Text(n.toString(), color = Milo.text, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = Milo.dim, fontSize = 16.sp)
    }
}

@Composable
private fun InsightsCard(vm: MainViewModel) {
    val stAll by vm.stats.collectAsState()
    var year by remember { mutableStateOf<Int?>(null) }
    val years = remember(stAll) { vm.statYears() }
    val st = remember(stAll, year) { year?.let { vm.statsFor(it) } ?: stAll }
    val shape = RoundedCornerShape(com.ubayanda.milonative.ui.theme.Rad.xl)
    Column(Modifier.fillMaxWidth().padding(horizontal = Sp.gutter).lift(shape, 14f).glass(shape).padding(Sp.x6)) {
        Text(if (year == null) "Your listening" else "Your $year recap", color = Milo.text, style = Ty.title)
        Row(Modifier.padding(top = 8.dp)) { Pill("Taste profile", null, true, compact = true) { vm.showTaste = true } }
        if (years.isNotEmpty()) Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("All time", null, year == null, compact = true) { year = null }
            years.take(4).forEach { y -> Pill(y.toString(), null, year == y, compact = true) { year = y } }
        }
        if (st.plays == 0) {
            Text("Play a few songs and your favourites will show up here.", color = Milo.dim, style = Ty.caption, modifier = Modifier.padding(top = 6.dp))
        } else {
            val mins = st.totalMs / 60_000L
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                Column { Text(if (mins >= 120) "${mins / 60}h" else "${mins}m", color = Milo.text, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold); Text("listened", color = Milo.dim, fontSize = 14.sp) }
                Column { Text(st.plays.toString(), color = Milo.text, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold); Text("plays", color = Milo.dim, fontSize = 14.sp) }
            }
            Text("Top songs", color = Milo.amber, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
            st.songs.forEachIndexed { i, r ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Text("${i + 1}", color = Milo.dim, style = Ty.caption, modifier = Modifier.padding(end = 12.dp))
                    Text(r.title + " · " + r.artist, color = Milo.text, style = Ty.label, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text("${r.plays}×", color = Milo.dim, fontSize = 14.sp)
                }
            }
            val ctxR = androidx.compose.ui.platform.LocalContext.current
            androidx.compose.foundation.layout.Box(Modifier.padding(top = 14.dp)) { Pill("Share my recap", null, false) {
                val hrs = st.totalMs / 3_600_000.0
                val txt = buildString {
                    append((if (year == null) "My milo. recap\n" else "My milo. $year recap\n"))
                    append(String.format(java.util.Locale.US, "%.1f hours listened \u00b7 %d plays\n", hrs, st.plays))
                    if (st.artists.isNotEmpty()) append("Top artist: ").append(st.artists[0].first).append("\n")
                    append("\nTop songs\n")
                    st.songs.forEachIndexed { i, r -> append("${i + 1}. ").append(r.title).append(" \u00b7 ").append(r.artist).append("\n") }
                    if (st.artists.size > 1) {
                        append("\nTop artists\n")
                        st.artists.forEachIndexed { i, (n, _) -> append("${i + 1}. ").append(n).append("\n") }
                    }
                }
                val send = android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, txt)
                ctxR.startActivity(android.content.Intent.createChooser(send, "Share my recap").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
            } }
            if (st.artists.isNotEmpty()) {
                Text("Top artists", color = Milo.amber, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))
                st.artists.forEachIndexed { i, (n, c) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Text("${i + 1}", color = Milo.dim, style = Ty.caption, modifier = Modifier.padding(end = 12.dp))
                        Text(n, color = Milo.text, style = Ty.label, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text("${c}×", color = Milo.dim, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolsCard(vm: MainViewModel) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val auto by vm.autoplay.collectAsState()
    val actions = com.ubayanda.milonative.ui.LocalActions.current
    var msg by remember { mutableStateOf("") }
    val save = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) msg = try {
            ctx.contentResolver.openOutputStream(uri)?.use { it.write(vm.exportBackup().toByteArray()) }; "Backup saved."
        } catch (e: Exception) { "Couldn't save the backup." }
    }
    val open = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = try { ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } } catch (e: Exception) { null }
            if (text == null) msg = "Couldn't read that file." else vm.importBackup(text) { msg = it }
        }
    }
    val shape = RoundedCornerShape(com.ubayanda.milonative.ui.theme.Rad.xl)
    Column(Modifier.fillMaxWidth().padding(horizontal = Sp.gutter).lift(shape, 14f).glass(shape).padding(Sp.x6)) {
        Text("Tools", color = Milo.text, style = Ty.title)
        ShareRow("Smart autoplay (keep similar songs coming)", auto) { vm.setAutoplay(it) }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Pill("Car mode", null, true) { actions.carMode() }
            Pill("Save backup", null, false) { save.launch("milo-backup.json") }
            Pill("Restore", null, false) { open.launch(arrayOf("application/json", "text/plain", "*/*")) }
        }
        if (msg.isNotEmpty()) Text(msg, color = Milo.dim, fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp))
    }
}


@Composable
private fun ScrobbleCard(vm: MainViewModel) {
    val sc = vm.scrobbler
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var lb by rememberSaveable { mutableStateOf(sc.lbToken) }
    var key by rememberSaveable { mutableStateOf(sc.fmKey) }
    var secret by rememberSaveable { mutableStateOf(sc.fmSecret) }
    var user by rememberSaveable { mutableStateOf(sc.fmUser) }
    var pass by rememberSaveable { mutableStateOf("") }
    var fmOn by remember { mutableStateOf(sc.fmOn) }
    val shape = RoundedCornerShape(com.ubayanda.milonative.ui.theme.Rad.xl)
    fun toast(m: String) = android.widget.Toast.makeText(ctx, m, android.widget.Toast.LENGTH_SHORT).show()
    Column(Modifier.fillMaxWidth().padding(horizontal = Sp.gutter).lift(shape, 14f).glass(shape).padding(Sp.x6)) {
        Text("Scrobbling", color = Milo.text, style = Ty.title)
        Text(
            "Send what you play to ListenBrainz and Last.fm. A play is sent once it counts (30 seconds, or half of a short song).",
            color = Milo.dim, style = Ty.caption, modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
        )
        Text("ListenBrainz", color = Milo.text, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
        ServerField(lb, "User token (listenbrainz.org/settings)", true) { lb = it }
        Spacer(Modifier.height(10.dp))
        Pill("Save ListenBrainz", null, true) { sc.setListenBrainz(lb); toast(if (lb.isBlank()) "ListenBrainz off" else "ListenBrainz saved") }
        Spacer(Modifier.height(20.dp))
        Text(if (fmOn) "Last.fm · connected as ${sc.fmUser}" else "Last.fm", color = Milo.text, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
        if (!fmOn) {
            Text("Make a free API account at last.fm/api/account/create. Your password is used once to sign in and is not stored.", color = Milo.dim, style = Ty.caption, modifier = Modifier.padding(bottom = 10.dp))
            ServerField(key, "API key", false) { key = it }
            Spacer(Modifier.height(8.dp))
            ServerField(secret, "Shared secret", true) { secret = it }
            Spacer(Modifier.height(8.dp))
            ServerField(user, "Username", false) { user = it }
            Spacer(Modifier.height(8.dp))
            ServerField(pass, "Password", true) { pass = it }
            Spacer(Modifier.height(10.dp))
            Pill("Connect Last.fm", null, true) {
                if (key.isBlank() || secret.isBlank() || user.isBlank() || pass.isBlank()) toast("Fill in all four boxes")
                else scope.launch {
                    val err = sc.connectLastFm(key.trim(), secret.trim(), user.trim(), pass)
                    pass = ""
                    if (err == null) { fmOn = true; toast("Last.fm connected") } else toast(err)
                }
            }
        } else Pill("Disconnect Last.fm", null, false) { sc.disconnectLastFm(); fmOn = false; toast("Last.fm disconnected") }
    }
}

@Composable
private fun TogetherCard(vm: MainViewModel) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val code by vm.together.code.collectAsState()
    var join by rememberSaveable { mutableStateOf("") }
    val shape = RoundedCornerShape(com.ubayanda.milonative.ui.theme.Rad.xl)
    fun toast(m: String) = android.widget.Toast.makeText(ctx, m, android.widget.Toast.LENGTH_SHORT).show()
    Column(Modifier.fillMaxWidth().padding(horizontal = Sp.gutter).lift(shape, 14f).glass(shape).padding(Sp.x6)) {
        Text("Listen together", color = Milo.text, style = Ty.title)
        Text(
            "Host a session and share the code, or enter a friend's code, to hear the same song at the same spot. Works for YouTube songs, and everyone needs to be signed in. Web and phone can join the same session.",
            color = Milo.dim, style = Ty.caption, modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
        )
        if (code.isNotEmpty()) {
            Text((if (vm.together.host) "Hosting" else "Listening in") + " · code $code", color = Milo.amber, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 10.dp))
            Pill("Leave session", null, false) { vm.together.leave(); toast("Left the session") }
        } else {
            Pill("Host a session", null, true) {
                if (!vm.signedIn) toast("Sign in first")
                else {
                    val c = (1..6).map { "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".random() }.joinToString("")
                    vm.joinTogether(c, true) { ok -> toast(if (ok) "Hosting $c. Play a YouTube song." else "Couldn't start the session") }
                }
            }
            Spacer(Modifier.height(14.dp))
            ServerField(join, "Friend's code", false) { join = it.uppercase().take(6) }
            Spacer(Modifier.height(10.dp))
            Pill("Join", null, false) {
                if (!vm.signedIn) toast("Sign in first")
                else if (join.length < 4) toast("Enter the code")
                else vm.joinTogether(join, false) { ok -> toast(if (ok) "Joined $join" else "Couldn't join") }
            }
        }
    }
}


@Composable
private fun BluetoothCard() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val out by com.ubayanda.milonative.player.BtOutput.now.collectAsState()
    var resume by remember { mutableStateOf(com.ubayanda.milonative.player.BtOutput.autoResume(ctx)) }
    var vol by remember { mutableStateOf(com.ubayanda.milonative.player.BtOutput.rememberVolume(ctx)) }
    var delay by remember { mutableStateOf(com.ubayanda.milonative.player.BtOutput.lyricDelay(ctx).toFloat()) }
    var named by remember { mutableStateOf(com.ubayanda.milonative.player.BtOutput.canNameDevices(ctx)) }
    val ask = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) {
        named = it; com.ubayanda.milonative.player.BtOutput.refresh(ctx)
    }
    val shape = RoundedCornerShape(com.ubayanda.milonative.ui.theme.Rad.xl)
    Column(Modifier.fillMaxWidth().padding(horizontal = Sp.gutter).lift(shape, 14f).glass(shape).padding(Sp.x6)) {
        Text("Bluetooth and output", color = Milo.text, style = Ty.title)
        Text(
            "Playing on: " + out.name + when (out.kind) { "bluetooth" -> " (Bluetooth)"; "usb" -> " (USB)"; else -> "" },
            color = Milo.amber, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        )
        if (!named) {
            Text("Allow Bluetooth access so milo. can show your device's name.", color = Milo.dim, style = Ty.caption, modifier = Modifier.padding(bottom = 8.dp))
            Pill("Allow", null, true) { if (android.os.Build.VERSION.SDK_INT >= 31) ask.launch(android.Manifest.permission.BLUETOOTH_CONNECT) }
            Spacer(Modifier.height(10.dp))
        }
        ShareRow("Carry on when headphones reconnect", resume) { resume = it; com.ubayanda.milonative.player.BtOutput.setAutoResume(ctx, it) }
        ShareRow("Remember volume for each device", vol) { vol = it; com.ubayanda.milonative.player.BtOutput.setRememberVolume(ctx, it) }
        Text("Lyrics delay on Bluetooth: ${delay.toInt()} ms", color = Milo.text, fontSize = 16.sp, modifier = Modifier.padding(top = 10.dp))
        Text("Bluetooth sound arrives a little late. Raise this until the highlighted line matches what you hear.", color = Milo.dim, style = Ty.caption)
        com.ubayanda.milonative.ui.components.MiloSlider(
            value = delay, onValueChange = { delay = it },
            onValueChangeFinished = { com.ubayanda.milonative.player.BtOutput.setLyricDelay(ctx, delay.toInt()) },
            valueRange = 0f..800f, steps = 15,
            colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = Milo.amber, activeTrackColor = Milo.amber),
        )
        Pill("Bluetooth settings", null, false) {
            try { ctx.startActivity(android.content.Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {}
        }
    }
}


@Composable
private fun AccentCard() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val shape = RoundedCornerShape(com.ubayanda.milonative.ui.theme.Rad.xl)
    Column(Modifier.fillMaxWidth().padding(horizontal = Sp.gutter).lift(shape, 14f).glass(shape).padding(Sp.x6)) {
        Text("Accent colour", color = Milo.text, style = Ty.title)
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            com.ubayanda.milonative.ui.theme.Accent.choices.forEach { c ->
                val on = Milo.amber == c
                Box(
                    Modifier.size(38.dp).clip(androidx.compose.foundation.shape.CircleShape).background(c)
                        .border(if (on) 3.dp else 1.dp, if (on) Milo.text else Milo.line, androidx.compose.foundation.shape.CircleShape)
                        .clickable { com.ubayanda.milonative.ui.theme.Accent.set(ctx, c) }
                        .semantics { contentDescription = "Accent colour" + if (on) ", selected" else "" },
                )
            }
        }
    }
}

@Composable
private fun ReportCard() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val shape = RoundedCornerShape(com.ubayanda.milonative.ui.theme.Rad.xl)
    Column(Modifier.fillMaxWidth().padding(horizontal = Sp.gutter).lift(shape, 14f).glass(shape).padding(Sp.x6)) {
        Text("Something not working?", color = Milo.text, style = Ty.title)
        Text(
            "Send the last things that went wrong (a song that wouldn't play, a crash) with your phone model. You choose where it goes, and it holds no account details.",
            color = Milo.dim, style = Ty.caption, modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
        )
        Pill("Send error report", null, true) { com.ubayanda.milonative.util.ErrorLog.share(ctx) }
        var keep by remember { mutableStateOf(com.ubayanda.milonative.util.ErrorLog.keepOn(ctx)) }
        ShareRow("Keep this list when the app closes", keep) { keep = it; com.ubayanda.milonative.util.ErrorLog.setKeep(ctx, it) }
        Text(
            "Off by default. When on, the list is saved on this phone only (never uploaded) and holds failures, how long the library scan took, and server error codes. Emails, tokens and web-address details are removed before saving.",
            color = Milo.dim, style = Ty.micro, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
        )
        Pill("Clear the list", null, false) { com.ubayanda.milonative.util.ErrorLog.clear(ctx) }
    }
}


@Composable
private fun DownloadsCard(vm: MainViewModel) {
    val items by vm.offline.collectAsState()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val shape = RoundedCornerShape(com.ubayanda.milonative.ui.theme.Rad.xl)
    val mb = items.sumOf { it.bytes } / 1048576.0
    Column(Modifier.fillMaxWidth().padding(horizontal = Sp.gutter).lift(shape, 14f).glass(shape).padding(Sp.x6)) {
        Text("Downloads", color = Milo.text, style = Ty.title)
        Text(
            if (items.isEmpty()) "Nothing saved yet. Open a YouTube song's menu and choose Download, or tap Download on an artist, album or playlist."
            else "${items.size} song" + (if (items.size == 1) "" else "s") + " saved · " + "%.1f".format(mb) + " MB. They play with no connection.",
            color = Milo.dim, style = Ty.caption, modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
        )
        Pill("Manage downloads", null, true) { vm.showDownloads = true }
        Spacer(Modifier.height(10.dp))
        Pill("milo. Connect: my devices", null, false) { vm.showConnect = true }
        Spacer(Modifier.height(10.dp))
        Pill("milo. Sessions: listening rooms", null, true) { vm.showSessions = true }
        Spacer(Modifier.height(10.dp))
        Pill("Shared playlists", null, false) { vm.showCpl = true }
        Spacer(Modifier.height(10.dp))
        Pill("Audio info: formats and outputs", null, false) { vm.showAudioInfo = true }
        Spacer(Modifier.height(10.dp))
        Pill("Scan a milo. QR code", null, false) { vm.showScanner = true }
        if (com.ubayanda.milonative.push.Push.available(androidx.compose.ui.platform.LocalContext.current)) {
            val pctx = androidx.compose.ui.platform.LocalContext.current
            var pushOn by remember { mutableStateOf(com.ubayanda.milonative.push.Push.enabled(pctx)) }
            ShareRow("Instant message notifications", pushOn) { pushOn = it; com.ubayanda.milonative.push.Push.setEnabled(pctx, it) }
        }
        if (items.isNotEmpty()) Spacer(Modifier.height(10.dp))
        if (items.isNotEmpty()) Pill("Remove all downloads", null, false) {
            com.ubayanda.milonative.data.OfflineStore.clear()
            android.widget.Toast.makeText(ctx, "Downloads removed", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}


@Composable
private fun BroughtByCard() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val shape = RoundedCornerShape(com.ubayanda.milonative.ui.theme.Rad.xl)
    fun open(url: String, mail: Boolean = false) {
        try {
            val i = android.content.Intent(if (mail) android.content.Intent.ACTION_SENDTO else android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
            if (mail) i.putExtra(android.content.Intent.EXTRA_SUBJECT, "milo.")
            ctx.startActivity(i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
            android.widget.Toast.makeText(ctx, "Couldn't open that", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = Sp.gutter).lift(shape, 14f).glass(shape).padding(Sp.x6), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Brought to you by", color = Milo.dim, style = Ty.caption)
        Text("theLEAF foundation", color = Milo.text, style = Ty.title, modifier = Modifier.padding(top = 2.dp))
        Text(
            "milo. is part of theLEAF foundation \u00a92021.\nmilo. \u2014 my music played my way.",
            color = Milo.dim, style = Ty.caption, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Pill("Instagram  @ubayanda.rsa", null, false) { open("https://instagram.com/ubayanda.rsa") }
            Pill("WhatsApp channel", null, false) { open("https://whatsapp.com/channel/0029Vb8fzZ76hENyyzsta30d") }
            Pill("ubayanda563@gmail.com", null, false) { open("mailto:ubayanda563@gmail.com", true) }
        }
    }
}
