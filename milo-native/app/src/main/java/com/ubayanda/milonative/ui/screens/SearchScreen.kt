package com.ubayanda.milonative.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubayanda.milonative.model.PlayerUi
import com.ubayanda.milonative.model.Track
import com.ubayanda.milonative.ui.MainViewModel
import com.ubayanda.milonative.ui.components.EmptyNote
import com.ubayanda.milonative.ui.components.TrackRow
import com.ubayanda.milonative.ui.theme.Milo

@Composable
fun SearchScreen(vm: MainViewModel, tracks: List<Track>, ui: PlayerUi, accent: Color, bottomPad: Dp) {
    var q by rememberSaveable { mutableStateOf("") }
    val hits = remember(q, tracks) {
        val s = q.trim()
        if (s.isEmpty()) emptyList()
        else tracks.filter { it.title.contains(s, true) || it.artist.contains(s, true) || it.album.contains(s, true) }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text("Search", color = Milo.text, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 16.dp, top = 18.dp, bottom = 10.dp))
        OutlinedTextField(
            value = q, onValueChange = { q = it }, singleLine = true,
            placeholder = { Text("Songs, artists, albums") },
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = bottomPad)) {
            if (q.isNotBlank() && hits.isEmpty()) item { EmptyNote("Nothing found", "Try a different spelling.") }
            items(hits, key = { "s" + it.id }) { t ->
                TrackRow(vm, t, t.id == ui.currentId, accent) { vm.player.playQueue(hits, hits.indexOf(t)) }
            }
        }
    }
}
