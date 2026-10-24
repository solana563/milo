package com.ubayanda.milonative.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
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

/** Art-tinted glass bar: cover, title, a cream play/pause circle, thin progress line. */
@Composable
fun MiniPlayer(vm: MainViewModel, t: Track, ui: PlayerUi, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val soft = rememberSoft(vm, t).value
    val frac = if (ui.durationMs > 0) (ui.positionMs.toFloat() / ui.durationMs).coerceIn(0f, 1f) else 0f
    val shape = RoundedCornerShape(32.dp)
    Column(
        modifier.fillMaxWidth().padding(horizontal = 14.dp)
            .clip(shape).artBackdrop(soft, 0.55f).border(1.dp, Milo.line, shape)
            .clickable(onClick = onOpen)
    ) {
        Row(Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Cover(vm, t, 58.dp, 16.dp, px = 160)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(t.title, color = Milo.text, fontSize = 19.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(t.artist, color = Color(0xFFD6D0C4), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier.size(56.dp).clip(CircleShape).background(Milo.cream).clickable { vm.player.toggle() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (ui.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Play or pause", tint = Color.Black, modifier = Modifier.size(30.dp))
            }
        }
        Box(Modifier.fillMaxWidth().height(3.dp).padding(horizontal = 18.dp)) {
            Box(Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f)))
            Box(Modifier.fillMaxWidth(frac).height(3.dp).clip(CircleShape).background(Milo.cream))
        }
        Spacer(Modifier.height(12.dp))
    }
}

/** The small "Now playing" button that floats above the mini player on screens without the player card. */
@Composable
fun NowPlayingPill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(30.dp)
    Row(
        modifier.clip(shape).background(Milo.card.copy(alpha = 0.97f), shape).border(1.dp, Milo.line, shape)
            .clickable(onClick = onClick).padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.KeyboardArrowDown, null, tint = Milo.text, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text("Now playing", color = Milo.text, fontSize = 17.sp, fontWeight = FontWeight.Medium)
    }
}
