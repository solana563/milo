package com.ubayanda.milonative.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ubayanda.milonative.data.Connect
import com.ubayanda.milonative.ui.MainViewModel
import com.ubayanda.milonative.ui.theme.Milo
import com.ubayanda.milonative.ui.theme.Ty
import org.json.JSONObject

/** milo. Connect: your other devices, what they are playing, and buttons to control them or move the music across. */
@Composable
fun ConnectScreen(vm: MainViewModel, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val on by vm.connectOn.collectAsState()
    val peers by vm.peers.collectAsState()
    val msg by vm.connectMsg.collectAsState()
    var open by remember { mutableStateOf<String?>(null) }
    val card = RoundedCornerShape(20.dp)
    // refresh the list while this screen is open
    LaunchedEffect(on) { while (true) { vm.refreshPeers(); kotlinx.coroutines.delay(4000) } }

    LazyColumn(
        Modifier.fillMaxSize().background(Milo.bg).statusBarsPadding().navigationBarsPadding().clickable(enabled = false) {},
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { Pill("Back", null, false, compact = true, onClick = onClose); Spacer(Modifier.padding(6.dp)); Text("milo. Connect", color = Milo.text, style = Ty.title) } }
        item {
            Column(Modifier.fillMaxWidth().clip(card).background(Milo.card2).padding(16.dp)) {
                Text("This phone: " + vm.thisDeviceName, color = Milo.text, style = Ty.heading)
                Text("See and control your own devices that are signed in to the same account. Nothing is shared with anyone else, and no login details are sent between devices.", color = Milo.dim, style = Ty.caption, modifier = Modifier.padding(top = 4.dp))
                if (!vm.signedIn) Text("Sign in first (Profile) to use Connect.", color = Milo.amber, style = Ty.label, modifier = Modifier.padding(top = 8.dp)) else
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (on) "Visible to, and controllable by, my other devices" else "Off: this phone is hidden", color = Milo.text, modifier = Modifier.padding(end = 12.dp).weight(1f))
                        Switch(checked = on, onCheckedChange = { vm.setConnect(it) }, colors = SwitchDefaults.colors(checkedTrackColor = Milo.amber, checkedThumbColor = Color.Black))
                    }
                if (msg.isNotEmpty()) Text(msg, color = Color(0xFFFF8A80), style = Ty.caption, modifier = Modifier.padding(top = 8.dp))
                if (on) Text("Works while milo. is running on both devices. It checks every few seconds, so there can be a short delay.", color = Milo.dim, style = Ty.micro, modifier = Modifier.padding(top = 6.dp))
            }
        }
        if (vm.signedIn && on) {
            item { Text("Your other devices", color = Milo.dim, style = Ty.label) }
            if (peers.isEmpty()) item { Text("No other devices yet. Install milo. on another device, sign in to the same account and switch Connect on there.", color = Milo.dim, style = Ty.caption) }
            items(peers, key = { it.id }) { d ->
                val live = Connect.online(d.lastSeen, System.currentTimeMillis())
                Column(Modifier.fillMaxWidth().clip(card).background(Milo.card).clickable(enabled = live) { open = if (open == d.id) null else d.id }.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(if (live) Color(0xFF4CD964) else Milo.dim))
                        Spacer(Modifier.padding(5.dp))
                        Column(Modifier.weight(1f)) {
                            Text(d.name, color = Milo.text, style = Ty.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                if (!live) "Offline" else d.now?.let { (if (it.playing) "Playing · " else "Paused · ") + it.title + " · " + it.artist } ?: "Idle",
                                color = Milo.dim, style = Ty.caption, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (open == d.id && live) {
                        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Pill("Prev", null, false, compact = true) { vm.remoteSend(d.id, JSONObject().put("t", "prev")) }
                            Pill(if (d.now?.playing == true) "Pause" else "Play", null, true, compact = true) { vm.remoteSend(d.id, JSONObject().put("t", "toggle")) }
                            Pill("Next", null, false, compact = true) { vm.remoteSend(d.id, JSONObject().put("t", "next")) }
                        }
                        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Pill("-15s", null, false, compact = true) { vm.remoteSend(d.id, JSONObject().put("t", "seekby").put("ms", -15000)) }
                            Pill("+15s", null, false, compact = true) { vm.remoteSend(d.id, JSONObject().put("t", "seekby").put("ms", 15000)) }
                        }
                        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Pill("Play here", null, false, compact = true) { vm.playHere(d) }
                            Pill("Send my music there", null, false, compact = true) { vm.sendTo(d) }
                        }
                        Text("Moving music works for online (YouTube) songs. Songs stored on one phone can't play on another.", color = Milo.dim, style = Ty.micro, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
    }
}
