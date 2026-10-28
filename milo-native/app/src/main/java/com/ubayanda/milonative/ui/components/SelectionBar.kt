package com.ubayanda.milonative.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubayanda.milonative.ui.theme.Milo

/** Shown while songs are selected: what to do with all of them at once. */
@Composable
fun SelectionBar(
    count: Int, onClose: () -> Unit, onPlayNext: () -> Unit, onQueue: () -> Unit, onLike: () -> Unit, onPlaylist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(26.dp)
    Column(modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).clip(shape).background(Milo.card2, shape).border(1.dp, Milo.line, shape).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Close, "Cancel selection", tint = Milo.text, modifier = Modifier.size(36.dp).clip(RoundedCornerShape(18.dp)).clickable(onClick = onClose).padding(6.dp))
            Spacer(Modifier.width(10.dp))
            Text("$count selected", color = Milo.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Action("Play next", Modifier.weight(1f), onPlayNext)
            Action("Queue", Modifier.weight(1f), onQueue)
            Action("Like", Modifier.weight(1f), onLike)
            Action("Playlist", Modifier.weight(1f), onPlaylist, primary = true)
        }
    }
}

@Composable
private fun Action(label: String, modifier: Modifier, onClick: () -> Unit, primary: Boolean = false) {
    val shape = RoundedCornerShape(20.dp)
    Text(
        label, color = if (primary) androidx.compose.ui.graphics.Color.Black else Milo.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 1,
        modifier = modifier.clip(shape).background(if (primary) Milo.amber else Milo.card, shape).clickable(onClick = onClick).padding(vertical = 12.dp),
    )
}
