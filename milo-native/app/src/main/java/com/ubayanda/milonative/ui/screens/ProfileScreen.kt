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
        val shape = RoundedCornerShape(34.dp)
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).clip(shape).background(Milo.card, shape).border(1.dp, Milo.line, shape).padding(26.dp),
        ) {
            Box(
                Modifier.size(78.dp).clip(RoundedCornerShape(24.dp)).background(Brush.verticalGradient(listOf(Color(0xFFF99C00), Color(0xFFB36F00)))),
                contentAlignment = Alignment.Center,
            ) { Text((a.username.firstOrNull() ?: 'G').uppercaseChar().toString(), color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.height(26.dp))
            Text(if (a.signedIn) a.username else "Guest", color = Milo.text, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 42.sp)
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
    }
}

@Composable
private fun Stat(n: Int, label: String) {
    Column {
        Text(n.toString(), color = Milo.text, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = Milo.dim, fontSize = 16.sp)
    }
}
