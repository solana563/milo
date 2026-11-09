package com.ubayanda.milonative.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubayanda.milonative.data.SmartMix
import com.ubayanda.milonative.ui.MainViewModel
import com.ubayanda.milonative.ui.theme.Milo
import com.ubayanda.milonative.ui.theme.Ty

/** What milo. has learned from listening, shown plainly, with the switches to turn it off or wipe it. Everything here is stored on this phone only. */
@Composable
fun TasteProfileScreen(vm: MainViewModel, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val ts = vm.tasteStore
    var tick by remember { mutableIntStateOf(0) }
    var confirm by remember { mutableStateOf(false) }
    val stats by vm.stats.collectAsState()
    val local by vm.tracks.collectAsState()
    val lists by vm.playlists.collectAsState()
    val favs by vm.favs.collectAsState()
    val card = RoundedCornerShape(20.dp)
    val taste = remember(tick, stats) { ts.snapshot(favs) }
    val genres = remember(tick, stats, local) { SmartMix.genres(local, taste, 6) }
    val newArtists = remember(tick, stats) { ts.newArtists(30).take(8) }
    val hours = remember(tick, stats) { ts.hours.copyOf() }
    val days = remember(tick, stats) { ts.weekdays.copyOf() }
    val weeks = remember(tick, stats) { ts.weeks.entries.toList().takeLast(8).map { it.value / 60_000L } }
    // variety: how many different artists a playlist holds relative to its length (100% = every song a different artist)
    val variety = remember(lists, local) {
        val ids = local.associateBy { it.id }
        val scores = lists.mapNotNull { p -> val ts2 = p.trackIds.mapNotNull { ids[it] }; if (ts2.size >= 4) ts2.map { it.artist.lowercase() }.toSet().size * 100 / ts2.size else null }
        if (scores.isEmpty()) null else scores.average().toInt() to scores.size
    }
    val blockedA = remember(tick) { ts.blockedArtists() }
    val blockedS = remember(tick) { ts.blockedSongs() }
    val mins = stats.totalMs / 60_000L

    LazyColumn(
        Modifier.fillMaxSize().background(Milo.bg).statusBarsPadding().navigationBarsPadding().clickable(enabled = false) {},
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { Pill("Back", null, false, compact = true, onClick = onClose); Spacer(Modifier.padding(6.dp)); Text("Taste profile", color = Milo.text, style = Ty.title) } }

        item {
            Box2(card) {
                Text("Personalisation", color = Milo.text, style = Ty.heading)
                Text("Smart mixes learn from what you play, like and skip. This stays on this phone: it is never uploaded and no outside AI is used.", color = Milo.dim, style = Ty.caption, modifier = Modifier.padding(top = 4.dp))
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (ts.enabled) "Learning from my listening" else "Paused: nothing new is learned", color = Milo.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Switch(checked = ts.enabled, onCheckedChange = { ts.setEnabled(it); vm.refreshMixes(); tick++ }, colors = SwitchDefaults.colors(checkedTrackColor = Milo.amber, checkedThumbColor = Color.Black))
                }
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(if (confirm) "Tap again to erase" else "Reset what milo learned", null, confirm, compact = true) {
                        if (confirm) { ts.reset(); confirm = false; vm.refreshMixes(); tick++ } else confirm = true
                    }
                    Pill("Refresh my mixes", null, false, compact = true) { vm.refreshHome() }
                }
                Text("Reset clears skips, play history for mixes, active times and boosts. Play counts in Insights and your blocked list are kept.", color = Milo.dim, style = Ty.micro, modifier = Modifier.padding(top = 6.dp))
            }
        }

        item {
            Box2(card) {
                Text("Listening", color = Milo.text, style = Ty.heading)
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    Stat2(if (mins >= 120) "${mins / 60}h" else "${mins}m", "listened")
                    Stat2(stats.plays.toString(), "plays")
                    Stat2(ts.skipCount().toString(), "skips noticed")
                }
                if (ts.since > 0) Text("Learning since " + java.text.DateFormat.getDateInstance().format(java.util.Date(ts.since)) + " · " + ts.songCount() + " songs heard", color = Milo.dim, style = Ty.micro, modifier = Modifier.padding(top = 8.dp))
            }
        }

        item {
            Box2(card) {
                Text("Top artists", color = Milo.text, style = Ty.heading)
                if (stats.artists.isEmpty()) Dim("Play a few songs first.")
                val max = (stats.artists.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
                stats.artists.forEach { (a, n) -> BarRow(a, n.toString() + "×", n / max.toFloat()) }
                Text("Top songs", color = Milo.text, style = Ty.heading, modifier = Modifier.padding(top = 14.dp))
                if (stats.songs.isEmpty()) Dim("Nothing yet.")
                val ms = (stats.songs.maxOfOrNull { it.plays } ?: 1).coerceAtLeast(1)
                stats.songs.forEach { r -> BarRow(r.title + " · " + r.artist, r.plays.toString() + "×", r.plays / ms.toFloat()) }
            }
        }

        item {
            Box2(card) {
                Text("Favourite genres", color = Milo.text, style = Ty.heading)
                if (genres.isEmpty()) Dim("Not available: your songs don't carry genre tags, or none have been played since they were read (genre needs Android 11 or newer).")
                val mg = (genres.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
                genres.forEach { (g, n) -> BarRow(g, n.toString() + "×", n / mg.toFloat()) }
            }
        }

        item {
            Box2(card) {
                Text("When you listen", color = Milo.text, style = Ty.heading)
                if (hours.sum() == 0L) Dim("Not enough listening recorded yet.") else {
                    val mh = hours.max().coerceAtLeast(1)
                    Row(Modifier.fillMaxWidth().height(70.dp).padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
                        hours.forEach { h -> Box(Modifier.weight(1f).fillMaxWidth().height((6 + 64 * h / mh.toFloat()).dp).clip(RoundedCornerShape(3.dp)).background(Milo.amber)) }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) { listOf("0h", "6h", "12h", "18h", "23h").forEach { Text(it, color = Milo.dim, style = Ty.micro) } }
                    val peak = hours.indices.maxByOrNull { hours[it] } ?: 0
                    val dn = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
                    Text("Most active around " + peak + ":00, and on " + dn[days.indices.maxByOrNull { days[it] } ?: 0] + "s.", color = Milo.dim, style = Ty.caption, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }

        item {
            Box2(card) {
                Text("Recently discovered artists", color = Milo.text, style = Ty.heading)
                if (newArtists.isEmpty()) Dim("No new artists in the last 30 days.") else Text(newArtists.joinToString(" · "), color = Milo.text, style = Ty.body, modifier = Modifier.padding(top = 6.dp))
            }
        }

        item {
            Box2(card) {
                Text("Trend", color = Milo.text, style = Ty.heading)
                if (weeks.size < 2) Dim("Needs a couple of weeks of listening.") else {
                    val mw = weeks.max().coerceAtLeast(1L)
                    Row(Modifier.fillMaxWidth().height(70.dp).padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
                        weeks.forEach { w -> Box(Modifier.weight(1f).fillMaxWidth().height((6 + 64 * w / mw.toFloat()).dp).clip(RoundedCornerShape(4.dp)).background(Milo.amber)) }
                    }
                    Text("Minutes per week, last ${weeks.size} weeks. Latest: ${weeks.last()} min.", color = Milo.dim, style = Ty.caption, modifier = Modifier.padding(top = 6.dp))
                }
                Text("Playlist variety", color = Milo.text, style = Ty.heading, modifier = Modifier.padding(top = 14.dp))
                if (variety == null) Dim("Needs a playlist with 4+ songs from your phone.") else
                    Text("${variety.first}% across ${variety.second} playlists (100% means every song has a different artist).", color = Milo.dim, style = Ty.caption, modifier = Modifier.padding(top = 4.dp))
            }
        }

        item {
            Box2(card) {
                Text("Blocked from recommendations", color = Milo.text, style = Ty.heading)
                if (blockedA.isEmpty() && blockedS.isEmpty()) Dim("Nothing blocked. Use \"Don't recommend\" in a song's menu.")
                blockedA.forEach { (k, name) -> Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) { Text("Artist · $name", color = Milo.text, style = Ty.body, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis); Pill("Allow", null, false, compact = true) { ts.unbanArtist(k); vm.refreshMixes(); tick++ } } }
                blockedS.forEach { (k, label) -> Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) { Text("Song · $label", color = Milo.text, style = Ty.body, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis); Pill("Allow", null, false, compact = true) { ts.unbanSong(k); vm.refreshMixes(); tick++ } } }
            }
        }
        item {
            Text("Moods use genre tags, release year and title words only. Tempo and energy aren't analysed, so a mood with too little information is left out instead of guessed.", color = Milo.dim, style = Ty.micro, modifier = Modifier.padding(bottom = 16.dp))
        }
    }
}

@Composable
private fun Box2(shape: RoundedCornerShape, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(shape).background(Milo.card2).padding(16.dp)) { content() }
}

@Composable
private fun Dim(t: String) { Text(t, color = Milo.dim, style = Ty.caption, modifier = Modifier.padding(top = 6.dp)) }

@Composable
private fun Stat2(big: String, small: String) {
    Column { Text(big, color = Milo.text, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold); Text(small, color = Milo.dim, fontSize = 13.sp) }
}

@Composable
private fun BarRow(label: String, value: String, frac: Float) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row { Text(label, color = Milo.text, style = Ty.label, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)); Text(value, color = Milo.dim, style = Ty.caption) }
        Box(Modifier.fillMaxWidth().padding(top = 4.dp).height(5.dp).clip(RoundedCornerShape(3.dp)).background(Milo.line)) {
            Box(Modifier.fillMaxWidth(frac.coerceIn(0.04f, 1f)).height(5.dp).clip(RoundedCornerShape(3.dp)).background(Milo.amber))
        }
    }
}
