package com.ubayanda.milonative.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubayanda.milonative.model.PlayerUi
import com.ubayanda.milonative.model.Track
import com.ubayanda.milonative.ui.MainViewModel
import com.ubayanda.milonative.ui.components.Cover
import com.ubayanda.milonative.ui.components.TrackRow
import com.ubayanda.milonative.ui.components.artBackdrop
import com.ubayanda.milonative.ui.components.rememberSoft
import com.ubayanda.milonative.ui.theme.Milo
import com.ubayanda.milonative.util.plural

/** Album or artist page: header card with the art, play / shuffle, then the songs. */
@Composable
fun DetailScreen(
    vm: MainViewModel, title: String, subtitle: String, tracks: List<Track>, ui: PlayerUi, accent: Color,
    bottomPad: Dp, onBack: () -> Unit,
) {
    val lead = tracks.first()
    val soft = rememberSoft(vm, lead).value
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(bottom = bottomPad)) {
        item {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBackIosNew, "Back", tint = Milo.text) }
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(24.dp)).artBackdrop(soft).padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Cover(vm, lead, 180.dp, 16.dp, px = 480)
                Spacer(Modifier.height(12.dp))
                Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text(subtitle + " · " + plural(tracks.size, "song"), color = Color(0xFFD8D8D8), fontSize = 13.sp)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { vm.player.playQueue(tracks, 0) }, shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    ) { Icon(Icons.Rounded.PlayArrow, null); Text("Play", fontWeight = FontWeight.Bold) }
                    Button(
                        onClick = { vm.player.playShuffled(tracks) }, shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.18f), contentColor = Color.White),
                    ) { Icon(Icons.Rounded.Shuffle, null); Text("Shuffle", fontWeight = FontWeight.Bold) }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        itemsIndexed(tracks, key = { _, t -> "d" + t.id }) { i, t ->
            TrackRow(vm, t, t.id == ui.currentId, accent) { vm.player.playQueue(tracks, i) }
        }
    }
}
