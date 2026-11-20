package com.ubayanda.milonative.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubayanda.milonative.model.Track
import com.ubayanda.milonative.ui.MainViewModel
import com.ubayanda.milonative.ui.components.Cover
import com.ubayanda.milonative.ui.theme.MiloIcons

/** True on Android TV and other leanback devices, where the remote's arrow keys are the only way to move. */
fun isTvDevice(ctx: android.content.Context): Boolean {
    val ui = ctx.getSystemService(android.content.Context.UI_MODE_SERVICE) as? android.app.UiModeManager
    return ui?.currentModeType == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION ||
        ctx.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK)
}

/** A white ring and a slight grow on whatever the remote is pointing at. */
private fun Modifier.tvFocus(shape: Shape, onClick: () -> Unit): Modifier = androidx.compose.ui.composed {
    var focused by remember { mutableStateOf(false) }
    this.onFocusChanged { focused = it.isFocused }
        .scale(if (focused) 1.06f else 1f)
        .then(if (focused) Modifier.border(3.dp, Color.White, shape) else Modifier)
        .clip(shape)
        .clickable(onClick = onClick)
}

/**
 * Big-screen milo: rows you move through with the remote's arrow keys (shuffle and liked, albums, songs) and a glass now-playing bar
 * along the bottom. Idea from Mochy Music's TV interface (GPL-3.0, github.com/realidkroo/Mochy-Music, ui/tv); this one is far smaller.
 */
@Composable
fun TvScreen(vm: MainViewModel) {
    val tracks by vm.tracks.collectAsState()
    val albums by vm.albums.collectAsState()
    val favs by vm.favs.collectAsState()
    val ui by vm.player.state.collectAsState()
    val byId = remember(tracks) { tracks.associateBy { it.id } }
    val current = byId[ui.currentId]
    val liked = remember(tracks, favs) { tracks.filter { it.id in favs } }

    Column(Modifier.fillMaxSize().background(Color(0xFF0E0D0C)).padding(horizontal = 48.dp, vertical = 32.dp)) {
        Text("milo.", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(16.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(28.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TvPill("Shuffle all", MiloIcons.Shuffle) { if (tracks.isNotEmpty()) vm.player.playShuffled(tracks) }
                    TvPill("Liked songs", MiloIcons.PlayArrow) { if (liked.isNotEmpty()) vm.player.playQueue(liked, 0) }
                }
            }
            if (albums.isNotEmpty()) item {
                Text("Albums", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 10.dp, horizontal = 6.dp)) {
                    items(albums.take(60), key = { it.id }) { a ->
                        val lead = a.tracks.firstOrNull()
                        Column(Modifier.width(180.dp).tvFocus(RoundedCornerShape(16.dp)) { vm.player.playQueue(a.tracks, 0) }.padding(6.dp)) {
                            Cover(vm, lead, 168.dp, 12.dp, px = 400)
                            Text(a.title, color = Color.White, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                            Text(a.artist, color = Color(0xFFA8A194), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            if (tracks.isNotEmpty()) item {
                Text("Songs", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                val list = remember(tracks) { tracks.take(80) }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 10.dp, horizontal = 6.dp)) {
                    items(list.size, key = { list[it].id }) { i ->
                        val t = list[i]
                        Column(Modifier.width(150.dp).tvFocus(RoundedCornerShape(16.dp)) { vm.player.playQueue(list, i) }.padding(6.dp)) {
                            Cover(vm, t, 138.dp, 12.dp, px = 300)
                            Text(t.title, color = Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                            Text(t.artist, color = Color(0xFFA8A194), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        // now playing, glass pill along the bottom
        if (current != null) {
            val shape = RoundedCornerShape(36.dp)
            Row(
                Modifier.fillMaxWidth().clip(shape).background(Color.White.copy(alpha = 0.12f)).border(1.dp, Color.White.copy(alpha = 0.22f), shape).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Cover(vm, current, 64.dp, 14.dp, px = 200)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(current.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(current.artist, color = Color(0xFFD6D0C4), fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val frac = if (ui.durationMs > 0) (ui.positionMs.toFloat() / ui.durationMs).coerceIn(0f, 1f) else 0f
                    Box(Modifier.padding(top = 8.dp).fillMaxWidth().height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f))) {
                        Box(Modifier.fillMaxWidth(frac).height(4.dp).background(Color.White))
                    }
                }
                Spacer(Modifier.width(16.dp))
                TvRound(MiloIcons.SkipPrevious, "Previous", 56.dp) { vm.player.previous() }
                Spacer(Modifier.width(10.dp))
                TvRound(if (ui.isPlaying) MiloIcons.Pause else MiloIcons.PlayArrow, "Play or pause", 68.dp) { vm.player.toggle() }
                Spacer(Modifier.width(10.dp))
                TvRound(MiloIcons.SkipNext, "Next", 56.dp) { vm.player.next() }
            }
        }
    }
}

@Composable
private fun TvPill(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val shape = RoundedCornerShape(30.dp)
    Row(
        Modifier.height(56.dp).tvFocus(shape, onClick).background(Color.White.copy(alpha = 0.16f)).padding(horizontal = 26.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TvRound(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, size: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    Box(Modifier.size(size).tvFocus(CircleShape, onClick).background(Color.White.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
        Icon(icon, label, tint = Color.White, modifier = Modifier.size(size * 0.5f))
    }
}
