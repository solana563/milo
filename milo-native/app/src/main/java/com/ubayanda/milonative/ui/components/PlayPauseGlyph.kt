package com.ubayanda.milonative.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.ubayanda.milonative.model.PlayerUi
import com.ubayanda.milonative.ui.theme.MiloIcons

/** The play/pause icon, replaced by a spinning ring while the song is buffering. */
@Composable
fun PlayPauseGlyph(ui: PlayerUi, size: Dp, tint: Color = Color.Black, desc: String = "Play or pause") {
    if (ui.buffering) {
        CircularProgressIndicator(Modifier.size(size * 0.9f), color = tint, strokeWidth = size * 0.09f)
    } else {
        Icon(if (ui.isPlaying) MiloIcons.Pause else MiloIcons.PlayArrow, desc, tint = tint, modifier = Modifier.size(size))
    }
}
