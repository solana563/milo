package com.ubayanda.milonative.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubayanda.milonative.data.YtMusic
import com.ubayanda.milonative.model.PlayerUi
import com.ubayanda.milonative.ui.MainViewModel
import com.ubayanda.milonative.ui.components.Cover
import com.ubayanda.milonative.ui.components.TrackRow
import com.ubayanda.milonative.ui.components.rememberCover
import com.ubayanda.milonative.ui.theme.Milo
import com.ubayanda.milonative.ui.theme.Rad
import com.ubayanda.milonative.ui.theme.Sp
import com.ubayanda.milonative.ui.theme.Ty
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** An artist as a poster, not a circle: a tall photo with the name set on it. */
@Composable
fun YtPoster(vm: MainViewModel, c: YtMusic.Card, onClick: () -> Unit) {
    val t = remember(c.id) { YtMusic.cardTrack(c, 400) }
    val bmp by rememberCover(vm, t, 400)
    val shape = RoundedCornerShape(Rad.md + 2.dp)
    Box(Modifier.width(138.dp).aspectRatio(3f / 4f).clip(shape).background(Milo.card2).clickable(onClick = onClick)) {
        val b = bmp
        if (b != null) {
            // press photos are shot with headroom, so the crop sits above centre
            Image(remember(b) { b.asImageBitmap() }, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alignment = BiasAlignment(0f, -0.45f))
        } else {
            Icon(Icons.Rounded.Person, null, tint = Milo.dim, modifier = Modifier.align(Alignment.Center).size(40.dp))
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(colorStops = arrayOf(0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.85f)))))
        Column(Modifier.align(Alignment.BottomStart).padding(11.dp)) {
            Text(c.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (c.sub.isNotEmpty()) Text(c.sub, color = Color.White.copy(alpha = 0.66f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** An album, single, playlist or song card: square cover, title and a line under it. */
@Composable
fun YtSquare(vm: MainViewModel, c: YtMusic.Card, onClick: () -> Unit) {
    val t = remember(c.id) { YtMusic.cardTrack(c, 400) }
    Column(Modifier.width(140.dp).clip(RoundedCornerShape(Rad.sm)).clickable(onClick = onClick)) {
        Cover(vm, t, 140.dp, Rad.sm, px = 400)
        Spacer(Modifier.height(6.dp))
        Text(c.title, color = Milo.text, style = Ty.label, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (c.sub.isNotEmpty()) Text(c.sub, color = Milo.dim, style = Ty.caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun YtShelf(title: String, cards: List<YtMusic.Card>, vm: MainViewModel, onCard: (YtMusic.Card) -> Unit) {
    Text(title, color = Milo.text, style = Ty.heading, modifier = Modifier.padding(start = Sp.gutter, end = Sp.gutter, top = Sp.x4, bottom = Sp.x2))
    LazyRow(contentPadding = PaddingValues(horizontal = Sp.gutter), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(cards, key = { it.kind + it.id }) { c ->
            if (c.kind == "artist") YtPoster(vm, c) { onCard(c) } else YtSquare(vm, c) { onCard(c) }
        }
    }
}

/** An artist, album or playlist from YouTube Music, opened inside milo. [onOpen] goes to another page (an album from an artist, say). */
@Composable
fun YtPageScreen(vm: MainViewModel, id: String, ui: PlayerUi, bottomPad: Dp, onOpen: (String) -> Unit, onBack: () -> Unit) {
    var page by remember(id) { mutableStateOf<YtMusic.Page?>(vm.youtube.cachedPage(id)) }
    var err by remember(id) { mutableStateOf("") }
    var tick by remember(id) { mutableIntStateOf(0) }
    LaunchedEffect(id, tick) {
        err = ""
        if (tick > 0) page = null
        // a page you already opened is kept, so going back to it is instant and keeps its scroll position
        if (page == null) try { page = vm.youtube.page(id) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { err = e.message ?: "Couldn't open this." }
    }
    val p = page
    Box(Modifier.fillMaxSize()) {
        when {
            p == null -> Column(Modifier.fillMaxSize().statusBarsPadding()) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBackIosNew, "Back", tint = Milo.text) }
                if (err.isEmpty()) { com.ubayanda.milonative.ui.components.SkeletonBox(300.dp, 200.dp, 18.dp, Modifier.padding(Sp.gutter)); com.ubayanda.milonative.ui.components.SkeletonRows(5) }
                else {
                    Text("Couldn't open this", color = Milo.text, style = Ty.title, modifier = Modifier.padding(horizontal = Sp.gutter))
                    Text(err, color = Milo.dim, style = Ty.caption, modifier = Modifier.padding(Sp.gutter))
                    Box(Modifier.padding(horizontal = Sp.gutter)) { Pill("Try again", null, true) { tick++ } }
                }
            }
            p.kind == "artist" -> YtArtistView(vm, p, ui, bottomPad, onOpen, onBack)
            else -> {
                val tracks = remember(p) { p.songs.map { YtMusic.toTrack(it, p.cover) } }
                LaunchedEffect(tracks) { vm.rememberRemote(tracks) }
                // a playlist's own cover if somebody chose one; YouTube's 2x2 collage of the first songs is not drawn as a cover
                val cover = remember(p) {
                    if (p.isCollage || p.cover.isEmpty()) tracks.firstOrNull()
                    else YtMusic.cardTrack(YtMusic.Card(p.kind, p.id, p.title, "", p.cover), 480)
                }
                DetailScreen(vm, p.title, p.subtitle, tracks, ui, bottomPad, onBack, coverTrack = cover)
            }
        }
    }
}

@Composable
private fun YtArtistView(vm: MainViewModel, p: YtMusic.Page, ui: PlayerUi, bottomPad: Dp, onOpen: (String) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val tracks = remember(p) { p.songs.map { YtMusic.toTrack(it, p.avatar.ifEmpty { p.banner }) } }
    LaunchedEffect(tracks) { vm.rememberRemote(tracks) }
    val bannerTrack = remember(p) { YtMusic.cardTrack(YtMusic.Card("artist", p.id, p.title, "", p.banner.ifEmpty { p.avatar }), 1000) }
    val banner by rememberCover(vm, bannerTrack, 1000)
    var more by remember(p) { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), state = vm.mem.list("ytartist:" + p.id), contentPadding = PaddingValues(bottom = bottomPad)) {
        item {
            Box(Modifier.fillMaxWidth().height(320.dp).background(Milo.card2)) {
                val b = banner
                if (b != null) Image(remember(b) { b.asImageBitmap() }, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alignment = BiasAlignment(0f, -0.5f))
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(colorStops = arrayOf(0f to Color.Black.copy(alpha = 0.35f), 0.5f to Color.Transparent, 1f to Milo.bg))))
                IconButton(onClick = onBack, modifier = Modifier.statusBarsPadding().padding(4.dp)) { Icon(Icons.Rounded.ArrowBackIosNew, "Back", tint = Color.White) }
                Column(Modifier.align(Alignment.BottomStart).padding(horizontal = Sp.gutter)) {
                    Text(p.title, color = Color.White, style = Ty.display, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (p.subtitle.isNotEmpty()) Text(p.subtitle, color = Color(0xFFD8D8D8), style = Ty.caption)
                }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()).padding(horizontal = Sp.gutter, vertical = Sp.x3), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("Play", Icons.Rounded.PlayArrow, true, tracks.isNotEmpty(), compact = true) { vm.player.playQueue(tracks, 0) }
                Pill("Shuffle", Icons.Rounded.Shuffle, false, tracks.isNotEmpty(), compact = true) { vm.player.playShuffled(tracks) }
                Pill("Download", Icons.Rounded.Download, false, tracks.isNotEmpty(), compact = true) {
                    vm.rememberRemote(tracks)
                    Toast.makeText(ctx, "Downloading ${tracks.size} songs…", Toast.LENGTH_SHORT).show()
                    com.ubayanda.milonative.data.OfflineStore.enqueue(tracks) { n, e -> Toast.makeText(ctx, if (n > 0) "Saved $n song" + (if (n == 1) "" else "s") + " for offline" else e ?: "Nothing new to save", Toast.LENGTH_SHORT).show() }
                }
                if (p.radioId.isNotEmpty()) Pill("Radio", Icons.Rounded.Radio, false, compact = true) {
                    scope.launch {
                        try {
                            val r = vm.youtube.radio(p.radioId)
                            if (r.isEmpty()) Toast.makeText(ctx, "No radio for this artist.", Toast.LENGTH_SHORT).show()
                            else { vm.rememberRemote(r); vm.player.playQueue(r.take(50), 0) }
                        } catch (e: CancellationException) { throw e } catch (e: Exception) {
                            Toast.makeText(ctx, "Couldn't start radio.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            if (p.description.isNotEmpty()) {
                Text(
                    p.description, color = Milo.dim, style = Ty.caption, maxLines = if (more) 30 else 3, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = Sp.gutter).clickable { more = !more },
                )
            }
        }
        if (tracks.isNotEmpty()) {
            item { Text("Top songs", color = Milo.text, style = Ty.heading, modifier = Modifier.padding(start = Sp.gutter, end = Sp.gutter, top = Sp.x4, bottom = Sp.x1)) }
            itemsIndexed(tracks, key = { _, t -> "a" + t.id }, contentType = { _, _ -> "track" }) { i, t ->
                TrackRow(vm, t, t.id == ui.currentId, Milo.amber) { vm.player.playQueue(tracks, i) }
            }
        }
        items(p.sections.size, key = { "s$it" }) { k ->
            val (title, cards) = p.sections[k]
            YtShelf(title, cards, vm) { c ->
                if (c.kind == "song") {
                    val t = YtMusic.toTrack(YtMusic.Song(c.id, c.title, c.sub, "", "", "", 0, c.thumb))
                    vm.rememberRemote(listOf(t)); vm.player.playQueue(listOf(t), 0)
                } else onOpen(c.id)
            }
        }
    }
}
