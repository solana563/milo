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
        containerColor = Milo.card,
        title = { Text(title, color = Milo.text) },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, placeholder = { Text("Playlist name") })
        },
        confirmButton = { TextButton(onClick = { onOk(name) }) { Text(okLabel, color = Milo.amber) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Milo.dim) } },
    )
}

@Composable
fun PlaylistPicker(playlists: List<Playlist>, onPick: (Playlist) -> Unit, onNew: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Milo.card,
        title = { Text("Add to playlist", color = Milo.text) },
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
