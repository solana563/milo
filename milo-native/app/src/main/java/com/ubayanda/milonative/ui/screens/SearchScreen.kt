package com.ubayanda.milonative.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ubayanda.milonative.model.PlayerUi
import com.ubayanda.milonative.model.Track
import com.ubayanda.milonative.ui.MainViewModel
import com.ubayanda.milonative.ui.components.EmptyNote
import com.ubayanda.milonative.ui.components.ScreenTitle
import com.ubayanda.milonative.ui.components.TrackRow
import com.ubayanda.milonative.ui.theme.Milo

@Composable
fun SearchScreen(vm: MainViewModel, tracks: List<Track>, ui: PlayerUi, bottomPad: Dp) {
    var q by rememberSaveable { mutableStateOf("") }
    val hits = remember(q, tracks) {
        val s = q.trim()
        if (s.isEmpty()) emptyList()
        else tracks.filter { it.title.contains(s, true) || it.artist.contains(s, true) || it.album.contains(s, true) }
    }
    Column(Modifier.fillMaxSize()) {
        ScreenTitle("Search")
        OutlinedTextField(
            value = q, onValueChange = { q = it }, singleLine = true,
            placeholder = { Text("Songs, artists, albums", color = Milo.dim) },
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Milo.card, unfocusedContainerColor = Milo.card,
                focusedBorderColor = Milo.amber, unfocusedBorderColor = Milo.line,
                cursorColor = Milo.amber, focusedTextColor = Milo.text, unfocusedTextColor = Milo.text,
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        )
        LazyColumn(contentPadding = PaddingValues(top = 10.dp, bottom = bottomPad)) {
            if (q.isNotBlank() && hits.isEmpty()) item { EmptyNote("Nothing found", "Try a different spelling.") }
            items(hits, key = { "s" + it.id }) { t ->
                TrackRow(vm, t, t.id == ui.currentId, Milo.amber) { vm.player.playQueue(hits, hits.indexOf(t)) }
            }
        }
    }
}
