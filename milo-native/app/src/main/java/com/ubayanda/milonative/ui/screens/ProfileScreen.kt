package com.ubayanda.milonative.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = bottomPad)) {
        ScreenTitle("Profile")
        val shape = RoundedCornerShape(30.dp)
        // the card takes the playing song's colour, fading to dark, like the web profile
        val tint = vm.tint.collectAsState().value
        val base = if (tint == 0) Milo.card2 else lerp(Color(tint), Color.Black, 0.45f)
        val cardBrush = Brush.linearGradient(listOf(base, lerp(base, Color.Black, 0.55f), Milo.card))
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp).clip(shape).background(cardBrush, shape).border(1.dp, Milo.line, shape).padding(22.dp),
        ) {
            Box(
                Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(Brush.verticalGradient(listOf(Color(0xFFF99C00), Color(0xFFB36F00)))),
                contentAlignment = Alignment.Center,
            ) { Text((a.username.firstOrNull() ?: 'G').uppercaseChar().toString(), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.SemiBold) }
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
                    Pill("Sign out", null, false) { vm.signOut() }
                } else {
                    Pill("Sign up free", null, true) { vm.openGate("up") }
                    Pill("Log in", null, false) { vm.openGate("in") }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        ServerCard(vm)
    }
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
