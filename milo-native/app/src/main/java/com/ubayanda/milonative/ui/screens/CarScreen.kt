package com.ubayanda.milonative.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubayanda.milonative.model.PlayerUi
import com.ubayanda.milonative.model.Track
import com.ubayanda.milonative.ui.MainViewModel
import com.ubayanda.milonative.ui.components.Cover
import com.ubayanda.milonative.ui.theme.Milo

/** Big, simple controls for use while driving. The screen stays on. */
@Composable
fun CarScreen(vm: MainViewModel, t: Track?, ui: PlayerUi, liked: Boolean, onClose: () -> Unit) {
    val view = LocalView.current
    DisposableEffect(Unit) {
        val old = view.keepScreenOn
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = old }
    }
    Column(
        Modifier.fillMaxSize().background(Color.Black).clickable(enabled = false) {}.statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Exit car mode", color = Milo.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(CircleShape).background(Milo.card2).clickable(onClick = onClose).padding(horizontal = 22.dp, vertical = 14.dp))
            Spacer(Modifier.weight(1f))
            if (t != null) Box(Modifier.size(64.dp).clip(CircleShape).clickable { vm.toggleFav(t.id) }, contentAlignment = Alignment.Center) {
                Icon(if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Like", tint = if (liked) Milo.amber else Milo.text, modifier = Modifier.size(36.dp))
            }
        }
        Spacer(Modifier.weight(1f))
        Cover(vm, t, 220.dp, 36.dp, px = 600)
        Spacer(Modifier.height(24.dp))
        Text(t?.title ?: "Nothing playing", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(t?.artist.orEmpty(), color = Milo.dim, fontSize = 22.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(20.dp))
        val frac = if (ui.durationMs > 0) (ui.positionMs.toFloat() / ui.durationMs).coerceIn(0f, 1f) else 0f
        Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Milo.card2)) {
            Box(Modifier.fillMaxWidth(frac).height(8.dp).clip(CircleShape).background(Milo.amber))
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            Big(Icons.Rounded.SkipPrevious, "Previous", 104.dp, Milo.card2, Color.White) { vm.player.previous() }
            Big(if (ui.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Play or pause", 136.dp, Milo.amber, Color.Black) { vm.player.toggle() }
            Big(Icons.Rounded.SkipNext, "Next", 104.dp, Milo.card2, Color.White) { vm.player.next() }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Big(icon: ImageVector, desc: String, size: Dp, bg: Color, tint: Color, onClick: () -> Unit) {
    Box(Modifier.size(size).clip(CircleShape).background(bg).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, desc, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}
