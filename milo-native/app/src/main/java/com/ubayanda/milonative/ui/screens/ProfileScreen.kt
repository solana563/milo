package com.ubayanda.milonative.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.matchParentSize
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

@Composable
fun ProfileScreen(vm: MainViewModel, a: AccountUi, songs: Int, likes: Int, playlists: Int, hours: Int, bottomPad: Dp) {
    var editing by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    val syncMsg by vm.syncMsg.collectAsState()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = bottomPad)) {
        ScreenTitle("Profile")
        val shape = RoundedCornerShape(30.dp)
        // the card takes the playing song's colour, fading to dark, like the web profile
        val tint = vm.tint.collectAsState().value
        val base = if (tint == 0) Milo.card2 else lerp(Color(tint), Color.Black, 0.45f)
        val cardBrush = Brush.linearGradient(listOf(base, lerp(base, Color.Black, 0.55f), Milo.card))
        val coverBmp = remember(a.cover) { if (a.cover.isEmpty()) null else ChatMedia.photoBitmap(a.cover) }
        val avatarBmp = remember(a.avatar) { if (a.avatar.isEmpty()) null else ChatMedia.photoBitmap(a.avatar) }
        Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp).clip(shape).background(cardBrush, shape).border(1.dp, Milo.line, shape)) {
        if (coverBmp != null) {
            androidx.compose.foundation.Image(coverBmp.asImageBitmap(), null, Modifier.matchParentSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.25f), Color.Black.copy(alpha = 0.8f)))))
        }
        Column(Modifier.padding(22.dp)) {
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
        ServerCard(vm)
        if (a.signedIn) {
            Spacer(Modifier.height(18.dp))
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
        containerColor = Milo.card, titleContentColor = Milo.text, textContentColor = Milo.text,
        title = { Text("Edit profile") },
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
        Text(label, color = Milo.text, fontSize = 15.sp, modifier = Modifier.weight(1f))
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
        containerColor = Milo.card, titleContentColor = Milo.text, textContentColor = Milo.text,
        title = { Text("Delete your account?") },
        text = {
            Column {
                Text("This permanently deletes your account, friends, messages and cloud backup. Songs on this phone are not touched.", color = Milo.dim, fontSize = 15.sp)
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
    val shape = RoundedCornerShape(30.dp)
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).clip(shape).background(Milo.card, shape).border(1.dp, Milo.line, shape).padding(22.dp)) {
        Text("My server", color = Milo.text, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            "Stream your own library from Navidrome or any Subsonic server. It shows up under Discover.",
            color = Milo.dim, fontSize = 15.sp, modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
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
