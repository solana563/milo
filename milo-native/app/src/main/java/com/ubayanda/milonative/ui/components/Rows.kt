package com.ubayanda.milonative.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubayanda.milonative.model.Track
import com.ubayanda.milonative.ui.LocalActions
import com.ubayanda.milonative.ui.MainViewModel
import com.ubayanda.milonative.ui.theme.Figtree
import com.ubayanda.milonative.ui.theme.Milo
import com.ubayanda.milonative.util.fmtTime

class RowAction(val label: String, val run: () -> Unit)

@Composable
fun TrackRow(
    vm: MainViewModel, t: Track, current: Boolean, accent: Color,
    extra: List<RowAction> = emptyList(), onClick: () -> Unit,
) {
    val actions = LocalActions.current
    val liked = vm.favs.collectAsState().value.contains(t.id)
    var menu by remember { mutableStateOf(false) }
    Column {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Cover(vm, t, 48.dp, 12.dp, px = 120)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    t.title, color = if (current) Milo.amber else Milo.text, fontSize = 16.sp, fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(t.artist, color = Milo.dim, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(fmtTime(t.durationMs), color = Milo.dim, fontSize = 12.sp)
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Rounded.MoreVert, "More", tint = Milo.dim, modifier = Modifier.size(22.dp))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Play next") }, onClick = { menu = false; vm.player.playNext(t) })
                    DropdownMenuItem(text = { Text("Add to queue") }, onClick = { menu = false; vm.player.addToQueue(t) })
                    DropdownMenuItem(text = { Text(if (liked) "Remove from Liked songs" else "Add to Liked songs") }, onClick = { menu = false; vm.toggleFav(t.id) })
                    DropdownMenuItem(text = { Text("Add to playlist") }, onClick = { menu = false; actions.addToPlaylist(t) })
                    DropdownMenuItem(text = { Text("Share") }, onClick = { menu = false; actions.share(t) })
                    extra.forEach { a -> DropdownMenuItem(text = { Text(a.label) }, onClick = { menu = false; a.run() }) }
                }
            }
        }
        Box(Modifier.fillMaxWidth().padding(start = 78.dp, end = 16.dp).height(1.dp).background(Milo.line))
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text, color = Milo.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(horizontal = 20.dp, vertical = 12.dp),
    )
}

@Composable
fun ScreenTitle(text: String) {
    Text(
        text, color = Milo.text, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.padding(start = 20.dp, top = 22.dp, bottom = 12.dp),
    )
}

@Composable
fun LetterHeader(letter: String, accent: Color) {
    Text(
        letter, color = Milo.amber, fontSize = 13.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
    )
}

@Composable
fun EmptyNote(title: String, body: String) {
    Column(
        Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, color = Milo.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Text(body, color = Milo.dim, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}

/** Amber when selected, dark pill with a hairline border otherwise: the web milo. chip. */
@Composable
fun MiloChip(label: String, on: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Text(
        label, color = if (on) Color.Black else Milo.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clip(shape).background(if (on) Milo.amber else Milo.card2, shape)
            .border(1.dp, if (on) Milo.amber else Milo.line, shape).clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
    )
}
