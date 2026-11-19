package com.ubayanda.milonative.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubayanda.milonative.data.SongId
import com.ubayanda.milonative.ui.theme.Milo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** "Identify a song": listens for ten seconds, then offers to search for what it heard. [onSearch] gets "title artist". */
@Composable
fun IdentifyDialog(onSearch: (String) -> Unit, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var token by remember { mutableStateOf(SongId.token(ctx)) }
    var step by remember { mutableStateOf(0) }          // 0 ready, 1 listening, 2 asking, 3 done
    var heard by remember { mutableStateOf<SongId.Heard?>(null) }
    var note by remember { mutableStateOf("") }

    fun go() {
        SongId.setToken(ctx, token)
        step = 1; note = ""; heard = null
        scope.launch {
            val wav = withContext(Dispatchers.IO) { SongId.listen(10) }
            if (wav == null) { note = "Couldn't use the microphone."; step = 3; return@launch }
            step = 2
            try {
                heard = withContext(Dispatchers.IO) { SongId.identify(wav, token.trim()) }
                if (heard == null) note = "No match. Try again closer to the speaker, with less talking over it."
            } catch (e: Exception) {
                note = e.message?.takeIf { it.isNotBlank() } ?: "No connection."
            }
            step = 3
        }
    }

    val mic = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) go() else android.widget.Toast.makeText(ctx, "Allow the microphone to identify songs.", android.widget.Toast.LENGTH_SHORT).show()
    }
    fun start() {
        if (token.isBlank()) { note = "Paste your AudD token first."; return }
        if (androidx.core.content.ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) go()
        else mic.launch(android.Manifest.permission.RECORD_AUDIO)
    }

    AlertDialog(
        onDismissRequest = { if (step != 1 && step != 2) onDismiss() },
        containerColor = Milo.card, shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
        title = { Text("Identify a song", color = Milo.text) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (step) {
                    1 -> Text("Listening for ten seconds…", color = Milo.text, fontSize = 16.sp)
                    2 -> Text("Asking AudD…", color = Milo.text, fontSize = 16.sp)
                    3 -> heard?.let {
                        Text(it.title, color = Milo.text, fontSize = 18.sp)
                        Text(listOf(it.artist, it.album).filter { s -> s.isNotBlank() }.joinToString(" · "), color = Milo.dim, fontSize = 14.sp)
                    }
                    else -> {}
                }
                if (note.isNotEmpty()) Text(note, color = Milo.dim, fontSize = 14.sp)
                if (step == 0 || step == 3) {
                    OutlinedTextField(value = token, onValueChange = { token = it }, singleLine = true, label = { Text("AudD token") })
                    Text("Hold the phone near the music. milo records ten seconds and sends only that clip to AudD (audd.io) to find the song. Get a free token at audd.io; it stays on this phone.", color = Milo.dim, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            if (step == 3 && heard != null) TextButton(onClick = { heard?.let { onSearch(it.title + " " + it.artist) } }) { Text("Search in milo", color = Milo.amber) }
            else if (step == 0 || step == 3) TextButton(onClick = { start() }) { Text(if (step == 3) "Try again" else "Listen", color = Milo.amber) }
        },
        dismissButton = { if (step != 1 && step != 2) TextButton(onClick = onDismiss) { Text("Close", color = Milo.dim) } },
    )
}
