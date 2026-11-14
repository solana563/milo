package com.ubayanda.milonative.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubayanda.milonative.data.OfflineStore
import com.ubayanda.milonative.data.OfflineStore.State
import com.ubayanda.milonative.ui.theme.Milo
import com.ubayanda.milonative.ui.theme.Ty

private fun mb(b: Long): String = if (b >= 1_073_741_824L) "%.1f GB".format(b / 1_073_741_824.0) else "%.1f MB".format(b / 1_048_576.0)

/** Everything about saved and queued songs in one place: what is downloading, waiting, paused or failed, what is saved, and the limits. */
@Composable
fun DownloadManagerScreen(vm: com.ubayanda.milonative.ui.MainViewModel, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val jobs by OfflineStore.jobs.collectAsState()
    val items by OfflineStore.items.collectAsState()
    val st by OfflineStore.settings.collectAsState()
    val waiting by OfflineStore.waiting.collectAsState()
    val prog by OfflineStore.progress.collectAsState()
    val used = items.sumOf { it.bytes }
    val free = OfflineStore.freeBytes()
    val left = OfflineStore.leftoverBytes()
    val card = RoundedCornerShape(20.dp)

    LazyColumn(
        Modifier.fillMaxSize().background(Milo.bg).statusBarsPadding().navigationBarsPadding().clickable(enabled = false) {},
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill("Back", null, false, compact = true, onClick = onClose)
                Spacer(Modifier.padding(6.dp))
                Text("Downloads", color = Milo.text, style = Ty.title)
            }
        }
        item {
            Column(Modifier.fillMaxWidth().clip(card).background(Milo.card2).padding(16.dp)) {
                Text("Storage", color = Milo.text, style = Ty.heading)
                Text("${items.size} saved · ${mb(used)} used · ${mb(free)} free on the phone", color = Milo.dim, style = Ty.caption, modifier = Modifier.padding(top = 4.dp))
                if (st.limitMb > 0) {
                    LinearProgressIndicator(
                        progress = { (used / (st.limitMb * 1_048_576f)).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(5.dp).clip(RoundedCornerShape(3.dp)), color = Milo.amber, trackColor = Milo.line,
                    )
                    Text("Limit ${st.limitMb} MB", color = Milo.dim, style = Ty.micro, modifier = Modifier.padding(top = 4.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text("Limit for downloads", color = Milo.dim, style = Ty.caption)
                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0 to "None", 500 to "500 MB", 1024 to "1 GB", 2048 to "2 GB", 5120 to "5 GB").forEach { (v, l) ->
                        Pill(l, null, st.limitMb == v, compact = true) { OfflineStore.setSettings(st.copy(limitMb = v)) }
                    }
                }
                Toggle("When the limit is reached, remove the oldest songs", st.autoClean, st.limitMb > 0) { OfflineStore.setSettings(st.copy(autoClean = it)) }
                if (left > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text("Unfinished files: ${mb(left)}", color = Milo.dim, style = Ty.caption)
                    Pill("Clear unfinished files", null, false, compact = true) { OfflineStore.clearLeftovers() }
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().clip(card).background(Milo.card2).padding(16.dp)) {
                Text("Download options", color = Milo.text, style = Ty.heading)
                Toggle("Wi-Fi only (no mobile data)", st.wifiOnly, true) { OfflineStore.setSettings(st.copy(wifiOnly = it)) }
                Toggle("Data saver: smaller files, lower audio quality (new downloads)", st.saver, true) { OfflineStore.setSettings(st.copy(saver = it)) }
                Text("Failed downloads retry up to 3 times by themselves.", color = Milo.dim, style = Ty.caption, modifier = Modifier.padding(top = 6.dp))
            }
        }
        if (jobs.isNotEmpty()) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("Pause all", null, false, compact = true) { OfflineStore.pauseAll() }
                    Pill("Resume all", null, false, compact = true) { OfflineStore.resumeAll() }
                    if (jobs.any { it.state == State.FAILED }) Pill("Retry failed", null, true, compact = true) { OfflineStore.retryAllFailed() }
                }
                if (waiting.isNotEmpty()) Text(waiting, color = Milo.amber, style = Ty.label, modifier = Modifier.padding(top = 8.dp))
            }
            for ((state, title) in listOf(State.ACTIVE to "Downloading", State.QUEUED to "Queued", State.PAUSED to "Paused", State.FAILED to "Failed")) {
                val rows = jobs.filter { it.state == state }
                if (rows.isEmpty()) continue
                item(key = "h-$title") { Text(title + " · " + rows.size, color = Milo.dim, style = Ty.label, modifier = Modifier.padding(top = 4.dp)) }
                items(rows, key = { "j-" + it.id }) { j ->
                    Column(Modifier.fillMaxWidth().clip(card).background(Milo.card).padding(14.dp)) {
                        Text(j.track.title, color = Milo.text, style = Ty.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            j.track.artist + " · " + j.source + " · " + (if (j.totalBytes > 0) mb(j.totalBytes) else "about " + mb(j.estimate(st.saver))),
                            color = Milo.dim, style = Ty.caption, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        if (state == State.ACTIVE) LinearProgressIndicator(
                            progress = { (prog[j.id] ?: 0f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(4.dp).clip(RoundedCornerShape(2.dp)), color = Milo.amber, trackColor = Milo.line,
                        )
                        if (j.error.isNotEmpty()) Text(j.error, color = if (state == State.FAILED) Color(0xFFFF8A80) else Milo.dim, style = Ty.caption, modifier = Modifier.padding(top = 4.dp), maxLines = 2)
                        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            when (state) {
                                State.ACTIVE -> Pill("Pause", null, false, compact = true) { OfflineStore.pause(j.id) }
                                State.QUEUED -> { Pill("Pause", null, false, compact = true) { OfflineStore.pause(j.id) }; Pill("Do first", null, false, compact = true) { OfflineStore.prioritise(j.id) } }
                                State.PAUSED -> { Pill("Resume", null, true, compact = true) { OfflineStore.resume(j.id) }; Pill("Do first", null, false, compact = true) { OfflineStore.prioritise(j.id) } }
                                State.FAILED -> Pill("Retry", null, true, compact = true) { OfflineStore.retry(j.id) }
                            }
                            Pill("Cancel", null, false, compact = true) { OfflineStore.cancel(j.id) }
                        }
                    }
                }
            }
        }
        item { Text("Saved on this phone · " + items.size, color = Milo.dim, style = Ty.label, modifier = Modifier.padding(top = 4.dp)) }
        if (items.isEmpty()) item { Text("Nothing saved yet. Open a YouTube song's menu and choose Download, or use Download on an album or playlist.", color = Milo.dim, style = Ty.caption) }
        // kept the way they were downloaded: each playlist, album and artist together, then songs saved one by one
        val groups = items.groupBy { it.gKind to it.gName }.entries.sortedWith(
            compareBy({ when (it.key.first) { "playlist" -> 0; "album" -> 1; "artist" -> 2; else -> 3 } }, { -(it.value.maxOfOrNull { x -> x.at } ?: 0L) })
        )
        groups.forEach { g ->
            val kind = g.key.first; val gname = g.key.second; val list = g.value.sortedBy { it.at }
            item(key = "g-$kind-$gname") {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (kind.isEmpty()) "Singles" else gname, color = Milo.text, style = Ty.heading, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            (when (kind) { "playlist" -> "Playlist"; "album" -> "Album"; "artist" -> "Artist"; else -> "Saved one by one" }) +
                                " · " + list.size + (if (list.size == 1) " song · " else " songs · ") + mb(list.sumOf { it.bytes }),
                            color = Milo.dim, style = Ty.caption,
                        )
                    }
                    Pill("Play", null, true, compact = true) { val tr = OfflineStore.tracksOf(kind, gname); vm.rememberRemote(tr); vm.player.playQueue(tr, 0) }
                    Spacer(Modifier.width(8.dp))
                    Pill("Remove all", null, false, compact = true) { list.forEach { OfflineStore.remove(it.id) } }
                }
            }
            items(list, key = { "s-" + it.id }) { it ->
                Row(Modifier.fillMaxWidth().clip(card).background(Milo.card).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(it.title, color = Milo.text, style = Ty.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(it.artist + " · " + mb(it.bytes), color = Milo.dim, style = Ty.caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Pill("Remove", null, false, compact = true) { OfflineStore.remove(it.id) }
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().clip(card).background(Milo.card2).padding(16.dp)) {
                Text("What's what", color = Milo.text, style = Ty.heading)
                Text("Downloaded: YouTube songs saved here with Download. They play with no connection.", color = Milo.dim, style = Ty.caption, modifier = Modifier.padding(top = 6.dp))
                Text("On this phone: your own music files. They always work offline.", color = Milo.dim, style = Ty.caption, modifier = Modifier.padding(top = 4.dp))
                Text("Streaming only: any other YouTube song. It needs a connection and is not stored.", color = Milo.dim, style = Ty.caption, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun Toggle(label: String, on: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = if (enabled) Milo.text else Milo.dim, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f).padding(end = 12.dp))
        Switch(checked = on, onCheckedChange = onChange, enabled = enabled, colors = SwitchDefaults.colors(checkedTrackColor = Milo.amber, checkedThumbColor = Color.Black))
    }
}
