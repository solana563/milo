package com.ubayanda.milonative.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubayanda.milonative.model.Playlist
import com.ubayanda.milonative.ui.theme.Milo

@Composable
fun NameDialog(title: String, initial: String, okLabel: String, onOk: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Milo.card, shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
        title = { com.ubayanda.milonative.ui.components.BlurBehind(); Text(title, color = Milo.text) },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, placeholder = { Text("Playlist name") })
        },
        confirmButton = { TextButton(onClick = { onOk(name) }) { Text(okLabel, color = Milo.amber) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Milo.dim) } },
    )
}

@Composable
fun EditInfoDialog(title0: String, artist0: String, album0: String, edited: Boolean, onSave: (String, String, String) -> Unit, onReset: () -> Unit, onDismiss: () -> Unit) {
    var t by remember { mutableStateOf(title0) }
    var a by remember { mutableStateOf(artist0) }
    var al by remember { mutableStateOf(album0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Milo.card, shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
        title = { com.ubayanda.milonative.ui.components.BlurBehind(); Text("Edit song info", color = Milo.text) },
        text = {
            Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = t, onValueChange = { t = it }, singleLine = true, label = { Text("Title") })
                OutlinedTextField(value = a, onValueChange = { a = it }, singleLine = true, label = { Text("Artist") })
                OutlinedTextField(value = al, onValueChange = { al = it }, singleLine = true, label = { Text("Album") })
                Text("Saved inside milo. The audio file isn't changed.", color = Milo.dim, fontSize = 12.sp)
                if (edited) Text("Undo my changes", color = Milo.amber, fontSize = 14.sp, modifier = Modifier.clickable { onReset() }.padding(vertical = 6.dp))
            }
        },
        confirmButton = { TextButton(onClick = { onSave(t, a, al) }) { Text("Save", color = Milo.amber) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Milo.dim) } },
    )
}

@Composable
fun PlaylistPicker(playlists: List<Playlist>, onPick: (Playlist) -> Unit, onNew: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Milo.card, shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
        title = { com.ubayanda.milonative.ui.components.BlurBehind(); Text("Add to playlist", color = Milo.text) },
        text = {
            Column {
                Text(
                    "+  New playlist", color = Milo.amber, fontSize = 16.sp,
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onNew).padding(vertical = 12.dp),
                )
                LazyColumn(Modifier.heightIn(max = 300.dp)) {
                    items(playlists, key = { it.id }) { p ->
                        Text(
                            p.name, color = Milo.text, fontSize = 16.sp,
                            modifier = Modifier.fillMaxWidth().clickable { onPick(p) }.padding(vertical = 12.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Milo.dim) } },
    )
}

/**
 * Fix this song's lyrics: nudge the timing when the words come too early or late, or give milo your own lyrics file (.lrc with times, or plain text).
 * Saved inside milo for this song only. Open the lyrics again to see the change.
 */
@Composable
fun LyricsFixDialog(trackId: Long, title: String, onDismiss: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var offset by remember { mutableStateOf(com.ubayanda.milonative.data.SongLyricFx.offset(ctx, trackId)) }
    var custom by remember { mutableStateOf(com.ubayanda.milonative.data.SongLyricFx.hasCustom(ctx, trackId)) }
    var note by remember { mutableStateOf("") }
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = try { ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } } catch (_: Exception) { null }
            if (text.isNullOrBlank()) note = "Couldn't read that file."
            else { com.ubayanda.milonative.data.SongLyricFx.setCustom(ctx, trackId, text); custom = true; note = "Using your lyrics file." }
        }
    }
    fun step(d: Long) { offset = (offset + d).coerceIn(com.ubayanda.milonative.data.SongLyricFx.MIN_MS, com.ubayanda.milonative.data.SongLyricFx.MAX_MS); com.ubayanda.milonative.data.SongLyricFx.setOffset(ctx, trackId, offset) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Milo.card, shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
        title = { com.ubayanda.milonative.ui.components.BlurBehind(); Text("Lyrics for " + title, color = Milo.text, maxLines = 2) },
        text = {
            Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                Text("Timing", color = Milo.text, fontSize = 14.sp)
                Text("Words too early? Push them later. Too late? Pull them earlier.", color = Milo.dim, fontSize = 12.sp)
                androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = { step(-1000) }) { Text("-1 s", color = Milo.amber) }
                    TextButton(onClick = { step(-250) }) { Text("-0.25", color = Milo.amber) }
                    Text((if (offset > 0) "+" else "") + "%.2f s".format(offset / 1000.0), color = Milo.text, fontSize = 14.sp)
                    TextButton(onClick = { step(250) }) { Text("+0.25", color = Milo.amber) }
                    TextButton(onClick = { step(1000) }) { Text("+1 s", color = Milo.amber) }
                }
                Text("Your own lyrics", color = Milo.text, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
                TextButton(onClick = { picker.launch(arrayOf("*/*")) }) { Text(if (custom) "Choose a different file" else "Choose a lyrics file (.lrc or .txt)", color = Milo.amber) }
                if (custom) Text("Use the found lyrics again", color = Milo.amber, fontSize = 14.sp, modifier = Modifier.clickable { com.ubayanda.milonative.data.SongLyricFx.setCustom(ctx, trackId, null); custom = false; note = "Back to the found lyrics." }.padding(vertical = 6.dp))
                if (offset != 0L) Text("Reset timing", color = Milo.amber, fontSize = 14.sp, modifier = Modifier.clickable { offset = 0L; com.ubayanda.milonative.data.SongLyricFx.setOffset(ctx, trackId, 0L) }.padding(vertical = 6.dp))
                if (note.isNotEmpty()) Text(note, color = Milo.dim, fontSize = 12.sp)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done", color = Milo.amber) } },
    )
}
