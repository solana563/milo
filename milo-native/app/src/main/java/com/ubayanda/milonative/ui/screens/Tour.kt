package com.ubayanda.milonative.ui.screens

import com.ubayanda.milonative.ui.components.glass
import com.ubayanda.milonative.ui.components.lift
import android.content.Context
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubayanda.milonative.ui.theme.Milo
import com.ubayanda.milonative.ui.theme.Rad
import com.ubayanda.milonative.ui.theme.Ty
import kotlinx.coroutines.flow.MutableStateFlow

/** First-run tour: a few cards on what milo. does. Shown once; Profile has a button to see it again. */
object Tour {
    val open = MutableStateFlow(false)
    private fun p(c: Context) = c.getSharedPreferences("milo_ui", Context.MODE_PRIVATE)
    fun seen(c: Context) = p(c).getBoolean("tour_v1", false)
    fun mark(c: Context) { p(c).edit().putBoolean("tour_v1", true).apply() }
}

private class Page(val title: String, val body: String)

private val PAGES = listOf(
    Page("Welcome to milo.", "Your music on this phone, plus a whole streaming catalogue when you're online. Offline, it's just your own library."),
    Page("Home is a guide", "When you're connected, Home fills with recommended playlists, charts and artists from YouTube Music. Your own songs sit below it."),
    Page("Search finds anything", "One box for your songs, playlists, friends and online music. Type a lyric you remember and it looks for the song."),
    Page("Make it sound right", "Open the player and tap Sound for the equaliser, a five-band parametric EQ that remembers each pair of headphones, and loudness matching."),
    Page("Share the moment", "Add friends, chat, and start Listen together to hear the same song at the same spot. Your yearly recap is on Profile."),
)

@Composable
fun TourOverlay() {
    val ctx = LocalContext.current
    val open by Tour.open.collectAsState()
    LaunchedEffect(Unit) { if (!Tour.seen(ctx)) Tour.open.value = true }
    if (!open) return
    var i by rememberSaveable { mutableIntStateOf(0) }
    val last = i == PAGES.lastIndex
    fun close() { Tour.mark(ctx); Tour.open.value = false; i = 0 }
    val shape = RoundedCornerShape(Rad.xl)
    Box(Modifier.fillMaxSize().background(Color(0xCC000000)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
        Column(Modifier.padding(24.dp).fillMaxWidth().lift(shape, 14f).glass(shape).padding(24.dp)) {
            Text("${i + 1} of ${PAGES.size}", color = Milo.amber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(PAGES[i].title, color = Milo.text, style = Ty.title, modifier = Modifier.padding(top = 8.dp))
            Text(PAGES[i].body, color = Milo.dim, style = Ty.body, modifier = Modifier.padding(top = 8.dp, bottom = 20.dp))
            Row(Modifier.padding(bottom = 18.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PAGES.indices.forEach { k -> Box(Modifier.size(8.dp).clip(CircleShape).background(if (k == i) Milo.amber else Milo.line)) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!last) Pill("Skip", null, false) { close() }
                Pill(if (last) "Get started" else "Next", null, true) { if (last) close() else i++ }
            }
        }
    }
}
