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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ubayanda.milonative.player.AudioCaps
import com.ubayanda.milonative.ui.MainViewModel
import com.ubayanda.milonative.ui.theme.Milo
import com.ubayanda.milonative.ui.theme.Ty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Audio info: formats this phone can play, what the current output supports, and the format of the song playing. */
@Composable
fun AudioInfoScreen(vm: MainViewModel, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val ctx = LocalContext.current
    val ps by vm.player.state.collectAsState()
    val cur = vm.trackById(ps.currentId)
    val formats = remember { AudioCaps.formats() }
    var outs by remember { mutableStateOf(AudioCaps.outputs(ctx)) }
    var info by remember { mutableStateOf<AudioCaps.Info?>(null) }
    LaunchedEffect(cur?.id) { outs = AudioCaps.outputs(ctx); info = cur?.let { t -> withContext(Dispatchers.IO) { AudioCaps.trackInfo(ctx, t.uri) } } }
    val card = RoundedCornerShape(20.dp)
    Box(Modifier.fillMaxSize().background(Milo.bg).statusBarsPadding().navigationBarsPadding().clickable(enabled = false) {}) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Row(verticalAlignment = Alignment.CenterVertically) { Pill("Back", null, false, compact = true, onClick = onClose); Spacer(Modifier.width(12.dp)); Text("Audio info", color = Milo.text, style = Ty.title) } }
            item {
                Column(Modifier.fillMaxWidth().clip(card).background(Milo.card2).padding(16.dp)) {
                    Text("Now playing", color = Milo.text, style = Ty.heading)
                    val i = info
                    Text(
                        when {
                            cur == null -> "Nothing is playing."
                            i != null -> AudioCaps.mimeLabel(i.mime) + (if (i.rate > 0) " · ${i.rate / 1000.0} kHz" else "") + (if (i.channels > 0) " · ${if (i.channels == 1) "mono" else if (i.channels == 2) "stereo" else i.channels.toString() + " ch"}" else "") + (if (i.kbps > 0) " · ${i.kbps} kbps" else "")
                            cur.uri.startsWith("yt:") || cur.uri.startsWith("http") -> "Streamed: YouTube picks the format and quality when the song starts."
                            else -> "This file's format couldn't be read."
                        }, color = Milo.dim, style = Ty.caption, modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
            item { Text("Formats this phone can play", color = Milo.dim, style = Ty.label) }
            item {
                Column(Modifier.fillMaxWidth().clip(card).background(Milo.card).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    formats.forEach { f ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(f.name, color = Milo.text, style = Ty.body, modifier = Modifier.weight(1f))
                            Text((if (f.ok) "Yes" else "No") + " · " + f.how, color = if (f.ok) Milo.amber else Color(0xFFFF8A80), style = Ty.micro)
                        }
                    }
                    Text("Read from this phone's own decoder list. A format marked No won't play here, whatever the file extension says.", color = Milo.dim, style = Ty.micro)
                }
            }
            item { Text("Audio outputs connected now", color = Milo.dim, style = Ty.label) }
            if (outs.isEmpty()) item { Text("No output reported.", color = Milo.dim, style = Ty.caption) }
            outs.forEach { o ->
                item {
                    Column(Modifier.fillMaxWidth().clip(card).background(Milo.card).padding(14.dp)) {
                        Text(o.name + " · " + o.type, color = Milo.text, style = Ty.body)
                        Text(o.note + if (o.hiRes) ". High-resolution capable." else "", color = Milo.dim, style = Ty.caption)
                    }
                }
            }
            item {
                Text("Which Bluetooth codec is in use (SBC, AAC, LDAC...) can't be read by apps on Android; check Developer options > Bluetooth audio codec. High-resolution capable means the output reports 88.2 kHz or higher with 24-bit or better; it doesn't guarantee your song or headphones are.", color = Milo.dim, style = Ty.micro)
            }
        }
    }
}
