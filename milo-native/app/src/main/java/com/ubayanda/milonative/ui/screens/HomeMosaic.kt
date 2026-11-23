package com.ubayanda.milonative.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubayanda.milonative.data.HomePins
import com.ubayanda.milonative.model.Album
import com.ubayanda.milonative.model.Playlist
import com.ubayanda.milonative.ui.MainViewModel
import com.ubayanda.milonative.ui.components.Cover
import com.ubayanda.milonative.ui.components.glass
import com.ubayanda.milonative.ui.components.lift
import com.ubayanda.milonative.ui.components.tactile
import com.ubayanda.milonative.ui.theme.Milo
import com.ubayanda.milonative.ui.theme.MiloIcons
import com.ubayanda.milonative.ui.theme.Rad
import com.ubayanda.milonative.ui.theme.Sp
import com.ubayanda.milonative.ui.theme.Ty
import com.ubayanda.milonative.util.greeting
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private class Shortcut(val key: String, val label: String, val icon: ImageVector)

private val shortcutDefs = listOf(
    Shortcut("search", "Search", MiloIcons.Search),
    Shortcut("friends", "Friends", MiloIcons.People),
    Shortcut("downloads", "Downloads", MiloIcons.Download),
    Shortcut("profile", "Profile", MiloIcons.Person),
    Shortcut("identify", "Identify", MiloIcons.Mic),
    Shortcut("car", "Car mode", MiloIcons.Explore),
    Shortcut("bedside", "Bedside", MiloIcons.Bedtime),
    Shortcut("ipod", "iPod", MiloIcons.GraphicEq),
)

@Composable
fun HomeMosaic(
    vm: MainViewModel, name: String, initial: String, albums: List<Album>, playlists: List<Playlist>,
    byId: Map<Long, com.ubayanda.milonative.model.Track>, likedCount: Int, savedCount: Int,
    onSearch: () -> Unit, onFriends: () -> Unit, onProfile: () -> Unit, onDownloads: () -> Unit, onLiked: () -> Unit,
    onOpenAlbum: (Album) -> Unit, onOpenPlaylist: (Playlist) -> Unit, player: @Composable () -> Unit,
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val actions = com.ubayanda.milonative.ui.LocalActions.current
    androidx.compose.runtime.LaunchedEffect(Unit) { HomePins.init(ctx) }
    var editing by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // slim header: weekday and greeting on the left, you on the right
        Row(Modifier.fillMaxWidth().padding(start = Sp.gutter, end = Sp.gutter, top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    SimpleDateFormat("EEEE", Locale.getDefault()).format(Date()).uppercase(),
                    color = Milo.dim, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
                )
                Text(
                    greeting(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) + if (name.isNotEmpty()) ", $name" else "",
                    color = Milo.text, style = Ty.title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp),
                )
            }
            val me by vm.account.collectAsState()
            com.ubayanda.milonative.ui.components.UserAvatar(name.ifEmpty { initial }, me.avatar, 44.dp, Modifier.clip(CircleShape).clickable(onClick = onProfile))
        }
        // now playing leads
        player()
        Column(Modifier.padding(horizontal = Sp.gutter), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            val recentAlbums = remember(albums.size) {
                com.ubayanda.milonative.player.RecentStore.ids(ctx).mapNotNull { vm.trackById(it) }
                    .filter { it.id > 0 }.mapNotNull { t -> albums.firstOrNull { it.id == t.albumId } }.distinctBy { it.id }.take(4)
            }
            if (recentAlbums.isNotEmpty()) {
                Text("Jump back in", color = Milo.text, style = Ty.title)
                TileGrid(vm, recentAlbums.map { a -> PinTile(a.title, MiloIcons.MusicNote, a.tracks.first()) { onOpenAlbum(a) } })
            }
        }
        Column(Modifier.padding(horizontal = Sp.gutter), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            HeroStats(vm, onProfile)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BigTile("Liked songs", likedCount.toString(), MiloIcons.Favorite, Modifier.weight(1f), onLiked)
                BigTile("Saved offline", savedCount.toString(), MiloIcons.Download, Modifier.weight(1f), onDownloads)
            }
            val pins = HomePins.list
            val pinned = pins.mapNotNull { k ->
                when {
                    k.startsWith("sc:") -> shortcutDefs.firstOrNull { it.key == k.removePrefix("sc:") }?.let { s ->
                        PinTile(s.label, s.icon, null) {
                            when (s.key) {
                                "search" -> onSearch(); "friends" -> onFriends(); "downloads" -> onDownloads(); "profile" -> onProfile()
                                "identify" -> actions.identify(); "car" -> actions.carMode(); "bedside" -> actions.bedside(); "ipod" -> actions.ipod()
                            }
                        }
                    }
                    k.startsWith("pl:") -> playlists.firstOrNull { it.id == k.removePrefix("pl:") }?.let { p ->
                        PinTile(p.name, MiloIcons.QueueMusic, p.trackIds.firstNotNullOfOrNull { byId[it] }) { onOpenPlaylist(p) }
                    }
                    else -> null
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Quick access", color = Milo.text, style = Ty.title, modifier = Modifier.weight(1f))
                Text("Edit", color = Milo.amber, style = Ty.label, modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { editing = true }.padding(horizontal = 8.dp, vertical = 4.dp))
            }
            TileGrid(vm, pinned)
        }
    }
    if (editing) PinsDialog(playlists) { editing = false }
}

private class PinTile(val label: String, val icon: ImageVector, val cover: com.ubayanda.milonative.model.Track?, val onClick: () -> Unit)

@Composable
private fun HeroStats(vm: MainViewModel, onOpen: () -> Unit) {
    val st by vm.stats.collectAsState()
    val days = vm.dayStats()
    val today = remember(st.totalMs) { days.todayMs() }
    val week = remember(st.totalMs) { days.lastDays(7) }
    val streak = remember(st.totalMs) { days.streak() }
    val shape = RoundedCornerShape(Rad.xl)
    Row(
        Modifier.fillMaxWidth().tactile(36.dp, clip = false, onClick = onOpen).lift(shape, 14f).glass(shape).padding(horizontal = 22.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("LISTENED TODAY", color = Milo.dim, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp)
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) {
                Text((today / 60_000L).toString(), color = Milo.text, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 44.sp)
                Text(" min", color = Milo.dim, style = Ty.label, modifier = Modifier.padding(bottom = 7.dp))
            }
            Text(if (streak == 0) "Start a streak today" else "$streak-day streak", color = if (streak > 0) Milo.amber else Milo.dim, style = Ty.label)
        }
        Spacer(Modifier.width(12.dp))
        // the last seven days as slim bars, today on the right
        val peak = (week.maxOrNull() ?: 0L).coerceAtLeast(1L)
        Row(Modifier.height(56.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.Bottom) {
            week.forEachIndexed { i, ms ->
                val h = if (ms <= 0L) 6.dp else (8 + 48 * (ms.toFloat() / peak)).dp
                Box(Modifier.width(7.dp).height(h).clip(RoundedCornerShape(4.dp)).background(if (i == 6) Milo.amber else Milo.text.copy(alpha = 0.22f)))
            }
        }
    }
}

@Composable
private fun BigTile(label: String, value: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(Rad.xl)
    Column(modifier.tactile(36.dp, clip = false, onClick = onClick).lift(shape, 14f).glass(shape).padding(18.dp)) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(Milo.text.copy(alpha = 0.08f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Milo.amber, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(value, color = Milo.text, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 30.sp)
        Text(label, color = Milo.dim, style = Ty.label)
    }
}

@Composable
private fun TileGrid(vm: MainViewModel, tiles: List<PinTile>) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        tiles.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { t ->
                    Column(Modifier.weight(1f).tactile(22.dp, clip = false, onClick = t.onClick), horizontalAlignment = Alignment.CenterHorizontally) {
                        BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(1f)) {
                            val shape = RoundedCornerShape(22.dp)
                            if (t.cover != null) Cover(vm, t.cover, maxWidth, 22.dp, px = 240)
                            else Box(Modifier.size(maxWidth).lift(shape, 10f).glass(shape), contentAlignment = Alignment.Center) {
                                Icon(t.icon, null, tint = Milo.text, modifier = Modifier.size(28.dp))
                            }
                        }
                        Text(t.label, color = Milo.text, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun PinsDialog(playlists: List<Playlist>, onClose: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val pins = HomePins.list
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Pinned tiles") },
        text = {
            Column(Modifier.heightIn(max = 380.dp).verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                Text("Tick what you want on Home. New ones go to the end.", color = Milo.dim, style = Ty.caption, modifier = Modifier.padding(bottom = 8.dp))
                shortcutDefs.forEach { s -> PinRow(s.label, "sc:" + s.key in pins) { HomePins.toggle(ctx, "sc:" + s.key) } }
                if (playlists.isNotEmpty()) Text("Playlists", color = Milo.dim, style = Ty.label, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                playlists.forEach { p -> PinRow(p.name, "pl:" + p.id in pins) { HomePins.toggle(ctx, "pl:" + p.id) } }
            }
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = onClose) { Text("Done") } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = { HomePins.reset(ctx) }) { Text("Reset") } },
    )
}

@Composable
private fun PinRow(label: String, on: Boolean, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.material3.Checkbox(checked = on, onCheckedChange = { onToggle() })
        Text(label, color = Milo.text, style = Ty.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
