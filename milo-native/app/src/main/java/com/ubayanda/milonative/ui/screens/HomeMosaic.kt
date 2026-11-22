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
import androidx.compose.ui.graphics.Color
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
import com.ubayanda.milonative.ui.components.PixelText
import com.ubayanda.milonative.ui.components.PixelTextFit
import com.ubayanda.milonative.ui.components.tactile
import com.ubayanda.milonative.ui.theme.Milo
import com.ubayanda.milonative.ui.theme.MiloIcons
import com.ubayanda.milonative.ui.theme.Sp
import com.ubayanda.milonative.ui.theme.Ty
import com.ubayanda.milonative.util.greeting
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Flat colour blocks on black, like the mockup: one colour for the tile, one for what's drawn on it. */
class Block(val bg: Color, val fg: Color)

object Mosaic {
    val blue = Block(Color(0xFF3B9CF2), Color(0xFF0B1B2B))
    val pink = Block(Color(0xFFFAC3D6), Color(0xFF2B0F1A))
    val green = Block(Color(0xFF7CCB5B), Color(0xFF102208))
    val red = Block(Color(0xFFEA4B3C), Color.White)
    val amber = Block(Color(0xFFF2A33A), Color(0xFF2B1A00))
    val violet = Block(Color(0xFF8B5CF6), Color.White)
    val teal = Block(Color(0xFF2BB5A0), Color(0xFF04201B))
    val sky = Block(Color(0xFF9AD0FF), Color(0xFF0B1B2B))
    val cycle = listOf(red, sky, violet, green, amber, teal, pink, blue)
}

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
    onOpenAlbum: (Album) -> Unit, onOpenPlaylist: (Playlist) -> Unit,
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val actions = com.ubayanda.milonative.ui.LocalActions.current
    androidx.compose.runtime.LaunchedEffect(Unit) { HomePins.init(ctx) }
    var editing by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(horizontal = Sp.gutter), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // top bar: the weekday in pixels, then search / friends / you
        Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                val day = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
                if (com.ubayanda.milonative.ui.components.PixelFont.supports(day)) PixelText(day, Milo.text, 3.dp)
                else Text(day.uppercase(), color = Milo.text, style = Ty.title)
                Text(
                    greeting(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) + if (name.isNotEmpty()) ", $name" else "",
                    color = Milo.dim, style = Ty.label, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp),
                )
            }
            val me by vm.account.collectAsState()
            com.ubayanda.milonative.ui.components.UserAvatar(name.ifEmpty { initial }, me.avatar, 44.dp, Modifier.clip(CircleShape).clickable(onClick = onProfile))
        }
        HeroStats(vm, onProfile)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigTile("LIKED", likedCount.toString(), MiloIcons.Favorite, Mosaic.pink, Modifier.weight(1f), onLiked)
            BigTile("SAVED", savedCount.toString(), MiloIcons.Download, Mosaic.green, Modifier.weight(1f), onDownloads)
        }
        // pinned tiles: shortcuts and playlists the person chose
        val pins = HomePins.list
        val pinned = pins.mapNotNull { k ->
            when {
                k.startsWith("sc:") -> shortcutDefs.firstOrNull { it.key == k.removePrefix("sc:") }?.let { s ->
                    PinTile(s.label, s.icon, null, null) {
                        when (s.key) {
                            "search" -> onSearch(); "friends" -> onFriends(); "downloads" -> onDownloads(); "profile" -> onProfile()
                            "identify" -> actions.identify(); "car" -> actions.carMode(); "bedside" -> actions.bedside(); "ipod" -> actions.ipod()
                        }
                    }
                }
                k.startsWith("pl:") -> playlists.firstOrNull { it.id == k.removePrefix("pl:") }?.let { p ->
                    PinTile(p.name, MiloIcons.QueueMusic, p.trackIds.firstNotNullOfOrNull { byId[it] }, null) { onOpenPlaylist(p) }
                }
                else -> null
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            PixelText("PINNED", Milo.dim, 2.dp, Modifier.weight(1f, fill = false))
            Spacer(Modifier.weight(1f))
            Text("Edit", color = Milo.amber, style = Ty.label, modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { editing = true }.padding(horizontal = 8.dp, vertical = 4.dp))
        }
        TileGrid(vm, pinned)
        // recently played albums, as covers
        val rctx = ctx
        val recentAlbums = remember(albums.size) {
            com.ubayanda.milonative.player.RecentStore.ids(rctx).mapNotNull { vm.trackById(it) }
                .filter { it.id > 0 }.mapNotNull { t -> albums.firstOrNull { it.id == t.albumId } }.distinctBy { it.id }.take(4)
        }
        if (recentAlbums.isNotEmpty()) {
            PixelText("RECENT", Milo.dim, 2.dp)
            TileGrid(vm, recentAlbums.map { a -> PinTile(a.title, MiloIcons.MusicNote, a.tracks.first(), null) { onOpenAlbum(a) } })
        }
    }
    if (editing) PinsDialog(playlists) { editing = false }
}

private class PinTile(val label: String, val icon: ImageVector, val cover: com.ubayanda.milonative.model.Track?, val block: Block?, val onClick: () -> Unit)

@Composable
private fun HeroStats(vm: MainViewModel, onOpen: () -> Unit) {
    val st by vm.stats.collectAsState()
    val days = vm.dayStats()
    val today = remember(st.totalMs) { days.todayMs() }
    val week = remember(st.totalMs) { days.lastDays(7) }
    val streak = remember(st.totalMs) { days.streak() }
    val b = Mosaic.blue
    val shape = RoundedCornerShape(30.dp)
    Row(Modifier.fillMaxWidth().clip(shape).background(b.bg, shape).clickable(onClick = onOpen).padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            PixelText("TODAY", b.fg.copy(alpha = 0.7f), 3.dp)
            Spacer(Modifier.height(8.dp))
            PixelTextFit(((today / 60_000L).toString()) + " MIN", b.fg, 6.dp, 3.dp)
            Spacer(Modifier.height(10.dp))
            PixelTextFit(if (streak == 0) "NO STREAK YET" else "$streak DAY STREAK", b.fg, 2.dp, 1.dp)
        }
        Spacer(Modifier.width(12.dp))
        // the last seven days, one pixel column each, today on the right
        val peak = (week.maxOrNull() ?: 0L).coerceAtLeast(1L)
        Canvas(Modifier.size(width = 84.dp, height = 62.dp)) {
            val cw = size.width / 7f
            val sq = cw * 0.72f
            val rowsMax = 6
            val rh = size.height / rowsMax
            week.forEachIndexed { i, ms ->
                val lit = if (ms <= 0L) 0 else (ms * rowsMax / peak).toInt().coerceIn(1, rowsMax)
                for (r in 0 until rowsMax) {
                    val on = r < lit
                    val a = if (on) (if (i == 6) 1f else 0.75f) else 0.16f
                    drawRect(b.fg.copy(alpha = a), Offset(i * cw + (cw - sq) / 2f, size.height - (r + 1) * rh + (rh - sq) / 2f), Size(sq, sq))
                }
            }
        }
    }
}

@Composable
private fun BigTile(label: String, value: String, icon: ImageVector, b: Block, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(32.dp)
    Box(modifier.aspectRatio(1f).tactile(32.dp, clip = false, onClick = onClick).clip(shape).background(b.bg, shape).padding(18.dp)) {
        Icon(icon, null, tint = b.fg, modifier = Modifier.size(34.dp).align(Alignment.TopStart))
        Column(Modifier.align(Alignment.BottomStart)) {
            PixelTextFit(value, b.fg, 6.dp, 3.dp)
            Spacer(Modifier.height(8.dp))
            PixelText(label, b.fg.copy(alpha = 0.7f), 2.dp)
        }
    }
}

@Composable
private fun TileGrid(vm: MainViewModel, tiles: List<PinTile>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        tiles.withIndex().toList().chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (i, t) ->
                    Column(Modifier.weight(1f).tactile(22.dp, clip = false, onClick = t.onClick), horizontalAlignment = Alignment.CenterHorizontally) {
                        val b = t.block ?: Mosaic.cycle[i % Mosaic.cycle.size]
                        BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(1f)) {
                            val shape = RoundedCornerShape(22.dp)
                            if (t.cover != null) Cover(vm, t.cover, maxWidth, 22.dp, px = 240)
                            else Box(Modifier.size(maxWidth).clip(shape).background(b.bg, shape), contentAlignment = Alignment.Center) {
                                Icon(t.icon, null, tint = b.fg, modifier = Modifier.size(30.dp))
                            }
                        }
                        Text(t.label, color = Milo.text, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 5.dp))
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
