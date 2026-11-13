package com.ubayanda.milonative.ui.screens

import android.content.Context
import android.media.AudioManager
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubayanda.milonative.data.OfflineStore
import com.ubayanda.milonative.model.Track
import com.ubayanda.milonative.ui.MainViewModel
import com.ubayanda.milonative.ui.components.Cover
import com.ubayanda.milonative.ui.theme.ClassicMode
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.atan2

private sealed interface Scr {
    object Root : Scr
    object Music : Scr
    object Songs : Scr
    object Artists : Scr
    object Albums : Scr
    object Playlists : Scr
    object Downloaded : Scr
    object Search : Scr
    object Now : Scr
    object Settings : Scr
    data class ArtistOf(val name: String) : Scr
    data class AlbumOf(val name: String) : Scr
    data class PlaylistOf(val id: String, val name: String) : Scr
}

private fun titleOf(s: Scr) = when (s) {
    Scr.Root -> "milo."; Scr.Music -> "Music"; Scr.Songs -> "Songs"; Scr.Artists -> "Artists"; Scr.Albums -> "Albums"
    Scr.Playlists -> "Playlists"; Scr.Downloaded -> "Downloaded"; Scr.Search -> "Search"; Scr.Now -> "Now Playing"; Scr.Settings -> "Settings"
    is Scr.ArtistOf -> s.name; is Scr.AlbumOf -> s.name; is Scr.PlaylistOf -> s.name
}

private class Entry(val scr: Scr) { var sel by mutableIntStateOf(0) }

private data class CRow(val label: String, val sub: String = "", val chevron: Boolean = false, val playing: Boolean = false, val onSelect: () -> Unit)

private fun mmss(ms: Long): String { val s = (ms / 1000).coerceAtLeast(0); return "%d:%02d".format(s / 60, s % 60) }

/** The whole app in classic mode: a small screen with a menu on top and a click wheel underneath. */
@Composable
fun ClassicApp(vm: MainViewModel) {
    val ctx = LocalContext.current
    val view = LocalView.current
    val ui by vm.player.state.collectAsState()
    val local by vm.tracks.collectAsState()
    val offline by vm.offline.collectAsState()
    val pls by vm.playlists.collectAsState()
    val dark = ClassicMode.dark
    val haptics = ClassicMode.haptics

    val downloaded = remember(offline) { OfflineStore.tracks() }
    val songs = remember(downloaded, local) { (downloaded + local).distinctBy { it.id }.sortedBy { it.title.lowercase() } }
    val byId = remember(songs) { songs.associateBy { it.id } }
    val byArtist = remember(songs) { songs.groupBy { it.artist.ifBlank { "Unknown artist" } }.toSortedMap(String.CASE_INSENSITIVE_ORDER) }
    val byAlbum = remember(songs) { songs.groupBy { it.album.ifBlank { "Unknown album" } }.toSortedMap(String.CASE_INSENSITIVE_ORDER) }
    val current = vm.trackById(ui.currentId)

    val stack = remember { mutableStateListOf(Entry(Scr.Root)) }
    val top = stack.last()

    var q by rememberSaveable { mutableStateOf("") }
    var ytHits by remember { mutableStateOf<List<Track>>(emptyList()) }
    LaunchedEffect(q) {
        val s = q.trim()
        if (s.length < 2) { ytHits = emptyList(); return@LaunchedEffect }
        delay(450)
        ytHits = if (vm.online()) vm.ytSearch(s) else emptyList()
    }

    fun open(s: Scr) { stack.add(Entry(s)) }
    fun play(list: List<Track>, i: Int) {
        if (list.isEmpty()) return
        vm.playRemote(list, i)
        if (stack.last().scr != Scr.Now) open(Scr.Now)
    }
    fun trackRows(list: List<Track>, online: Boolean = false) = if (list.isEmpty()) listOf(CRow("Nothing here yet", "Search for a song or download one") {})
        else list.mapIndexed { i, t -> CRow(t.title, t.artist + if (online) " · online" else "", playing = t.id == ui.currentId) { play(list, i) } }

    fun rowsFor(s: Scr): List<CRow> = when (s) {
        Scr.Root -> buildList {
            add(CRow("Music", chevron = true) { open(Scr.Music) })
            add(CRow("Search", chevron = true) { open(Scr.Search) })
            add(CRow("Downloaded", chevron = true) { open(Scr.Downloaded) })
            if (ui.currentId != null) add(CRow("Now Playing", chevron = true) { open(Scr.Now) })
            add(CRow("Shuffle Songs") { if (songs.isNotEmpty()) play(songs.shuffled(), 0) })
            add(CRow("Settings", chevron = true) { open(Scr.Settings) })
        }
        Scr.Music -> listOf(
            CRow("Playlists", chevron = true) { open(Scr.Playlists) }, CRow("Artists", chevron = true) { open(Scr.Artists) },
            CRow("Albums", chevron = true) { open(Scr.Albums) }, CRow("Songs", chevron = true) { open(Scr.Songs) },
        )
        Scr.Songs -> trackRows(songs)
        Scr.Downloaded -> trackRows(downloaded)
        Scr.Artists -> byArtist.map { (n, l) -> CRow(n, "${l.size} song" + (if (l.size == 1) "" else "s"), chevron = true) { open(Scr.ArtistOf(n)) } }
        is Scr.ArtistOf -> trackRows(byArtist[s.name].orEmpty())
        Scr.Albums -> byAlbum.map { (n, l) -> CRow(n, l.firstOrNull()?.artist.orEmpty(), chevron = true) { open(Scr.AlbumOf(n)) } }
        is Scr.AlbumOf -> trackRows(byAlbum[s.name].orEmpty().sortedWith(compareBy({ it.trackNo }, { it.title.lowercase() })))
        Scr.Playlists -> pls.map { p -> CRow(p.name, "${p.trackIds.size} songs", chevron = true) { open(Scr.PlaylistOf(p.id, p.name)) } }
            .ifEmpty { listOf(CRow("No playlists", "") {}) }
        is Scr.PlaylistOf -> trackRows(pls.firstOrNull { it.id == s.id }?.trackIds.orEmpty().mapNotNull { byId[it] })
        Scr.Search -> {
            val k = q.trim()
            if (k.length < 2) listOf(CRow("Type to search", "Your songs and YouTube Music") {})
            else {
                val mine = songs.filter { it.title.contains(k, true) || it.artist.contains(k, true) || it.album.contains(k, true) }.take(60)
                val ids = mine.map { it.id }.toSet()
                val net = ytHits.filter { it.id !in ids }
                val rows = mine.mapIndexed { i, t -> CRow(t.title, t.artist, playing = t.id == ui.currentId) { play(mine, i) } } +
                    net.map { t -> CRow(t.title, t.artist + " · online", playing = t.id == ui.currentId) { play(listOf(t), 0) } }
                rows.ifEmpty { listOf(CRow("No matches", "") {}) }
            }
        }
        Scr.Settings -> listOf(
            CRow("Back to milo.", "Leave classic mode") { ClassicMode.setOn(ctx, false) },
            CRow("Body", if (dark) "Black" else "Silver") { ClassicMode.setDark(ctx, !dark) },
            CRow("Wheel ticks", if (haptics) "On" else "Off") { ClassicMode.setHaptics(ctx, !haptics) },
            CRow("Shuffle", if (ui.shuffle) "On" else "Off") { vm.player.toggleShuffle() },
            CRow("Refresh library") { vm.scan() },
        )
        Scr.Now -> emptyList()
    }

    val rows = remember(top.scr, songs, downloaded, byArtist, byAlbum, pls, q, ytHits, ui.currentId, ui.shuffle, dark, haptics) { rowsFor(top.scr) }
    val sel = top.sel.coerceIn(0, (rows.size - 1).coerceAtLeast(0))

    // volume (wheel on Now Playing)
    val am = remember { ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    var volUntil by remember { mutableLongStateOf(0L) }
    var volTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(volTick) { if (volTick > 0) { delay(1400); volUntil = 0L } }

    fun tick() { if (haptics) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }
    fun step(dir: Int) {
        if (top.scr == Scr.Now) {
            val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val cur = am.getStreamVolume(AudioManager.STREAM_MUSIC)
            val next = (cur + dir).coerceIn(0, max)
            if (next != cur) { am.setStreamVolume(AudioManager.STREAM_MUSIC, next, 0); tick() }
            volUntil = System.currentTimeMillis() + 1400; volTick++
        } else {
            val n = (sel + dir).coerceIn(0, (rows.size - 1).coerceAtLeast(0))
            if (n != sel) { top.sel = n; tick() }
        }
    }
    fun back() { if (stack.size > 1) stack.removeAt(stack.size - 1) }
    fun center() { if (top.scr == Scr.Now) vm.player.toggle() else rows.getOrNull(sel)?.onSelect?.invoke() }

    BackHandler(enabled = stack.size > 1) { back() }

    val bodyBrush = if (dark) Brush.verticalGradient(listOf(Color(0xFF1E1E21), Color(0xFF09090A))) else Brush.verticalGradient(listOf(Color(0xFFF4F5F7), Color(0xFFD3D6DB)))
    Column(Modifier.fillMaxSize().background(bodyBrush).statusBarsPadding().navigationBarsPadding().padding(horizontal = 18.dp, vertical = 14.dp)) {
        // the screen
        Column(
            Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(12.dp)).border(3.dp, if (dark) Color(0xFF3A3A3E) else Color(0xFF9EA3AB), RoundedCornerShape(12.dp)).background(Color.White),
        ) {
            Row(
                Modifier.fillMaxWidth().height(32.dp).background(Brush.verticalGradient(listOf(Color(0xFFF7F8FA), Color(0xFFC3C8CF)))).padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.width(24.dp))
                Text(titleOf(top.scr), color = Color(0xFF1F2329), fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                Box(Modifier.width(24.dp), contentAlignment = Alignment.CenterEnd) {
                    if (ui.currentId != null) Icon(if (ui.isPlaying) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, null, tint = Color(0xFF2F6FD1), modifier = Modifier.size(18.dp))
                }
            }
            if (top.scr == Scr.Search) {
                val fr = remember { FocusRequester() }
                LaunchedEffect(Unit) { try { fr.requestFocus() } catch (_: Throwable) {} }
                BasicTextField(
                    value = q, onValueChange = { q = it; top.sel = 0 }, singleLine = true,
                    textStyle = TextStyle(color = Color(0xFF1F2329), fontSize = 16.sp), cursorBrush = SolidColor(Color(0xFF2F6FD1)),
                    modifier = Modifier.fillMaxWidth().padding(10.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFEDEFF2)).padding(horizontal = 12.dp, vertical = 9.dp).focusRequester(fr),
                )
            }
            if (top.scr == Scr.Now) NowPlayingPane(vm, current, ui, volUntil, am)
            else {
                val lazy = rememberLazyListState()
                LaunchedEffect(top, rows.size) { lazy.scrollToItem((top.sel - 2).coerceAtLeast(0)) }
                LaunchedEffect(sel) {
                    val first = lazy.firstVisibleItemIndex
                    val last = lazy.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: first
                    if (sel < first) lazy.scrollToItem(sel) else if (sel >= last) lazy.scrollToItem((sel - (last - first) + 1).coerceAtLeast(0))
                }
                LazyColumn(state = lazy, modifier = Modifier.fillMaxWidth().weight(1f)) {
                    itemsIndexed(rows) { i, r ->
                        val on = i == sel
                        Row(
                            Modifier.fillMaxWidth().height(46.dp)
                                .background(if (on) Brush.verticalGradient(listOf(Color(0xFF5AA8F7), Color(0xFF1A6AD0))) else SolidColor(Color.Transparent))
                                .clickable { top.sel = i; r.onSelect() }.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text((if (r.playing) "♪ " else "") + r.label, color = if (on) Color.White else Color(0xFF1F2329), fontSize = 16.sp, fontWeight = if (r.playing) FontWeight.Bold else FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (r.sub.isNotEmpty()) Text(r.sub, color = if (on) Color(0xFFDCEBFF) else Color(0xFF7B818A), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (r.chevron) Text("›", color = if (on) Color.White else Color(0xFF8B9099), fontSize = 24.sp)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        ClickWheel(
            dark = dark, playing = ui.isPlaying,
            onStep = ::step, onMenu = ::back, onPrev = { tick(); vm.player.previous() }, onNext = { tick(); vm.player.next() },
            onPlay = { tick(); vm.player.toggle() }, onCenter = { tick(); center() },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun NowPlayingPane(vm: MainViewModel, t: Track?, ui: com.ubayanda.milonative.model.PlayerUi, volUntil: Long, am: AudioManager) {
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Cover(vm, t, 150.dp, radius = 6.dp, px = 360)
        Spacer(Modifier.height(10.dp))
        Text(t?.title ?: "Nothing playing", color = Color(0xFF1F2329), fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(t?.artist.orEmpty(), color = Color(0xFF4A5058), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(t?.album.orEmpty(), color = Color(0xFF7B818A), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.weight(1f))
        val showVol = System.currentTimeMillis() < volUntil
        val frac = if (showVol) am.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        else if (ui.durationMs > 0) (ui.positionMs.toFloat() / ui.durationMs).coerceIn(0f, 1f) else 0f
        Box(
            Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)).background(Color(0xFFD3D7DD))
                .pointerInput(ui.durationMs) { detectTapGestures { o -> if (ui.durationMs > 0 && !showVol) vm.player.seekTo((o.x / size.width * ui.durationMs).toLong()) } },
        ) {
            Box(Modifier.fillMaxSize().padding(end = 0.dp)) {
                Box(Modifier.fillMaxWidth(frac).fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF5AA8F7), Color(0xFF1A6AD0)))))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(if (showVol) "Volume" else mmss(ui.positionMs), color = Color(0xFF4A5058), fontSize = 12.sp)
            Text(if (showVol) "" else "-" + mmss((ui.durationMs - ui.positionMs).coerceAtLeast(0)), color = Color(0xFF4A5058), fontSize = 12.sp)
        }
    }
}

/** The click wheel: turn a finger round it to scroll, tap the rim for menu / back / forward / play, tap the middle to choose. */
@Composable
private fun ClickWheel(
    dark: Boolean, playing: Boolean, onStep: (Int) -> Unit, onMenu: () -> Unit, onPrev: () -> Unit, onNext: () -> Unit, onPlay: () -> Unit, onCenter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val step by rememberUpdatedState(onStep)
    val menu by rememberUpdatedState(onMenu)
    val prev by rememberUpdatedState(onPrev)
    val next by rememberUpdatedState(onNext)
    val play by rememberUpdatedState(onPlay)
    val center by rememberUpdatedState(onCenter)
    val label = if (dark) Color(0xFFE6E7EA) else Color(0xFF878B93)
    Box(
        modifier.size(270.dp)
            .pointerInput(Unit) {
                detectTapGestures { off ->
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val d = off - c
                    if (d.getDistance() < size.width * 0.19f) center()
                    else {
                        val a = atan2(d.y, d.x) * 180f / PI.toFloat()
                        when {
                            a in -135f..-45f -> menu()
                            a in -45f..45f -> next()
                            a in 45f..135f -> play()
                            else -> prev()
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                var last = 0f; var acc = 0f
                fun ang(o: Offset) = atan2(o.y - size.height / 2f, o.x - size.width / 2f) * 180f / PI.toFloat()
                detectDragGestures(
                    onDragStart = { o -> last = ang(o); acc = 0f },
                    onDrag = { change, _ ->
                        val a = ang(change.position)
                        var d = a - last
                        if (d > 180f) d -= 360f
                        if (d < -180f) d += 360f
                        last = a; acc += d
                        while (acc >= 24f) { acc -= 24f; step(1) }
                        while (acc <= -24f) { acc += 24f; step(-1) }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            drawCircle(
                if (dark) Brush.verticalGradient(listOf(Color(0xFF38383C), Color(0xFF1B1B1E))) else Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFDCDEE2))),
                r,
            )
            drawCircle(if (dark) Color(0xFF55555A) else Color(0xFFB9BDC4), r, style = Stroke(1.5.dp.toPx()))
            drawCircle(
                if (dark) Brush.verticalGradient(listOf(Color(0xFF17171A), Color(0xFF09090A))) else Brush.verticalGradient(listOf(Color(0xFFF1F2F4), Color(0xFFCFD2D7))),
                r * 0.38f,
            )
            drawCircle(if (dark) Color(0xFF47474C) else Color(0xFFA9ADB4), r * 0.38f, style = Stroke(1.dp.toPx()))
        }
        Text("MENU", color = label, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.align(Alignment.TopCenter).padding(top = 22.dp))
        Icon(Icons.Rounded.SkipPrevious, "Previous", tint = label, modifier = Modifier.align(Alignment.CenterStart).padding(start = 18.dp).size(30.dp))
        Icon(Icons.Rounded.SkipNext, "Next", tint = label, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 18.dp).size(30.dp))
        Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Play or pause", tint = label, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp).size(30.dp))
    }
}
