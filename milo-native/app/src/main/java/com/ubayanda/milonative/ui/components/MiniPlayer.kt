package com.ubayanda.milonative.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubayanda.milonative.model.PlayerUi
import com.ubayanda.milonative.model.Track
import com.ubayanda.milonative.ui.MainViewModel
import com.ubayanda.milonative.ui.theme.Milo

@Composable
fun MiniPlayer(vm: MainViewModel, t: Track, ui: PlayerUi, accent: Color, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val soft = rememberSoft(vm, t).value
    val frac = if (ui.durationMs > 0) (ui.positionMs.toFloat() / ui.durationMs).coerceIn(0f, 1f) else 0f
    Column(
        modifier.fillMaxWidth().padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(18.dp)).artBackdrop(soft, 0.5f)
            .clickable(onClick = onOpen)
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Cover(vm, t, 44.dp, 10.dp, px = 120)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(t.title, color = Milo.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(t.artist, color = Color(0xFFD0D0D0), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = { vm.player.toggle() }) {
                Icon(if (ui.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Play or pause", tint = Milo.text, modifier = Modifier.size(30.dp))
            }
            IconButton(onClick = { vm.player.next() }) {
                Icon(Icons.Rounded.SkipNext, "Next", tint = Milo.text, modifier = Modifier.size(28.dp))
            }
        }
        Box(Modifier.fillMaxWidth().height(2.dp).padding(horizontal = 14.dp)) {
            Box(Modifier.fillMaxWidth().height(2.dp).background(Color.White.copy(alpha = 0.2f)))
            Box(Modifier.fillMaxWidth(frac).height(2.dp).background(accent))
        }
        Spacer(Modifier.height(6.dp))
    }
}
