package com.ubayanda.milonative.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ubayanda.milonative.ui.MainViewModel
import com.ubayanda.milonative.ui.theme.MiloWordmark
import kotlinx.coroutines.delay

/**
 * Your listening as a story: full-screen pages you tap through (right = next, left = back; it also moves on by itself), ending with a card
 * you can share. It is built only from what milo counted on this phone (time listened, plays, top songs and artists). The story idea comes
 * from Mochy Music's recap (GPL-3.0, ui/screens/recap); the pages and look here are milo's own.
 */
@Composable
fun RecapStory(st: MainViewModel.StatsUi, label: String, daysInPeriod: Int?, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val pages = remember(st) {
        val l = ArrayList<String>()
        l.add("intro"); l.add("time"); l.add("plays")
        if (st.songs.isNotEmpty()) { l.add("topsong"); l.add("songs") }
        if (st.artists.isNotEmpty()) l.add("artists")
        l.add("card")
        l
    }
    var page by remember { mutableIntStateOf(0) }
    val last = pages.size - 1
    // moves on by itself after a few seconds; any tap restarts the wait
    LaunchedEffect(page) { if (page < last) { delay(6500); page++ } }
    val palette = listOf(
        listOf(Color(0xFF3A1C71), Color(0xFFD76D77)), listOf(Color(0xFF0F2027), Color(0xFF2C5364)), listOf(Color(0xFF42275A), Color(0xFF734B6D)),
        listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0)), listOf(Color(0xFF134E5E), Color(0xFF71B280)), listOf(Color(0xFFB24592), Color(0xFFF15F79)),
        listOf(Color(0xFF232526), Color(0xFF414345)),
    )
    val colors = palette[page % palette.size]
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)) {
        Box(
            Modifier.fillMaxSize().background(Brush.verticalGradient(colors))
                .pointerInput(page) {
                    detectTapGestures { o -> if (o.x < size.width / 3f) { if (page > 0) page-- } else if (page < last) page++ }
                },
        ) {
            AnimatedContent(targetState = page, transitionSpec = { fadeIn(tween(320)) togetherWith fadeOut(tween(200)) }, label = "recap-page", modifier = Modifier.fillMaxSize()) { p ->
                Column(
                    Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 28.dp, vertical = 64.dp),
                    verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    when (pages[p]) {
                        "intro" -> {
                            MiloWordmark(size = 40.sp, color = Color.White)
                            Spacer(Modifier.height(28.dp))
                            Text("Your $label", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                            Text("in music", color = Color.White.copy(alpha = 0.85f), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(24.dp))
                            Text("Tap to see it", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                        }
                        "time" -> {
                            val mins = st.totalMs / 60_000L
                            Text("You listened for", color = Color.White.copy(alpha = 0.85f), fontSize = 20.sp)
                            CountUp(if (mins >= 120) (mins / 60).toInt() else mins.toInt(), if (mins >= 120) "hours" else "minutes")
                            // an album is about 45 minutes
                            val albums = (mins / 45L).toInt()
                            if (albums >= 2) Text("That's about $albums albums back to back.", color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp, textAlign = TextAlign.Center)
                        }
                        "plays" -> {
                            Text("You played", color = Color.White.copy(alpha = 0.85f), fontSize = 20.sp)
                            CountUp(st.plays, if (st.plays == 1) "song" else "songs")
                            if (daysInPeriod != null && daysInPeriod > 0 && st.plays >= daysInPeriod) {
                                Text("About " + (st.plays / daysInPeriod) + " a day.", color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp)
                            }
                        }
                        "topsong" -> {
                            val s = st.songs[0]
                            Text("Your number one song", color = Color.White.copy(alpha = 0.85f), fontSize = 20.sp)
                            Spacer(Modifier.height(18.dp))
                            Text(s.title, color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, maxLines = 4, overflow = TextOverflow.Ellipsis)
                            Text(s.artist, color = Color.White.copy(alpha = 0.85f), fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp))
                            Text("${s.plays} plays", color = Color.White.copy(alpha = 0.65f), fontSize = 15.sp, modifier = Modifier.padding(top = 14.dp))
                        }
                        "songs" -> {
                            Text("Your top songs", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.height(18.dp))
                            st.songs.forEachIndexed { i, r -> StaggerRow(i) {
                                Text("${i + 1}", color = Color.White.copy(alpha = 0.6f), fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(r.title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(r.artist, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Text("${r.plays}×", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                            } }
                        }
                        "artists" -> {
                            Text("Your top artist", color = Color.White.copy(alpha = 0.85f), fontSize = 20.sp)
                            Spacer(Modifier.height(10.dp))
                            Text(st.artists[0].first, color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(22.dp))
                            val top = st.artists[0].second.coerceAtLeast(1)
                            st.artists.forEachIndexed { i, (n, c) -> StaggerRow(i) {
                                Column(Modifier.weight(1f)) {
                                    Row { Text(n, color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis); Text("$c", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp) }
                                    Box(Modifier.padding(top = 4.dp).fillMaxWidth().height(6.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f))) {
                                        Box(Modifier.fillMaxWidth(c.toFloat() / top).fillMaxHeight().clip(CircleShape).background(Color.White))
                                    }
                                }
                            } }
                        }
                        else -> {
                            // the share card
                            Column(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Color.Black.copy(alpha = 0.35f)).padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                MiloWordmark(size = 26.sp, color = Color.White)
                                Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
                                val mins = st.totalMs / 60_000L
                                Text(if (mins >= 120) "${mins / 60} hours" else "$mins minutes", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
                                Text("${st.plays} plays", color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp)
                                if (st.artists.isNotEmpty()) Text("Top artist: " + st.artists[0].first, color = Color.White, fontSize = 15.sp, modifier = Modifier.padding(top = 14.dp), textAlign = TextAlign.Center)
                                if (st.songs.isNotEmpty()) Text("Top song: " + st.songs[0].title, color = Color.White, fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp), textAlign = TextAlign.Center)
                            }
                            Spacer(Modifier.height(22.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                RecapButton("Share") {
                                    val txt = buildString {
                                        append("My milo. $label recap\n")
                                        val m = st.totalMs / 60_000L
                                        append(if (m >= 120) "${m / 60} hours" else "$m minutes").append(" · ${st.plays} plays\n")
                                        if (st.artists.isNotEmpty()) append("Top artist: ").append(st.artists[0].first).append("\n")
                                        st.songs.forEachIndexed { i, r -> append("${i + 1}. ").append(r.title).append(" · ").append(r.artist).append("\n") }
                                    }
                                    val send = android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, txt)
                                    ctx.startActivity(android.content.Intent.createChooser(send, "Share my recap").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                                }
                                RecapButton("Close") { onClose() }
                            }
                        }
                    }
                }
            }
            // progress segments across the top
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                pages.indices.forEach { i ->
                    Box(Modifier.weight(1f).height(3.dp).clip(CircleShape).background(Color.White.copy(alpha = if (i <= page) 0.95f else 0.3f)))
                }
            }
        }
    }
}

@Composable
private fun CountUp(target: Int, unit: String) {
    val anim = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(target) { anim.animateTo(target.toFloat(), tween(1400, easing = FastOutSlowInEasing)) }
    Text(anim.value.toInt().toString(), color = Color.White, fontSize = 88.sp, fontWeight = FontWeight.Black)
    Text(unit, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun StaggerRow(i: Int, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    var on by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { delay(180L * i + 150L); on = 1 }
    val a by animateFloatAsState(if (on == 1) 1f else 0f, tween(420), label = "stagger")
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp).alpha(a), verticalAlignment = Alignment.CenterVertically, content = content)
}

@Composable
private fun RecapButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(26.dp)).background(Color.White.copy(alpha = 0.22f)).clickable(remember { MutableInteractionSource() }, null) { onClick() }.padding(horizontal = 26.dp, vertical = 13.dp),
    ) { Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
}
