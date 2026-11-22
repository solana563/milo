package com.ubayanda.milonative.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ubayanda.milonative.model.Track
import com.ubayanda.milonative.ui.MainViewModel
import com.ubayanda.milonative.ui.theme.Milo
import com.ubayanda.milonative.ui.theme.MiloIcons

/**
 * A playlist's cover: the art of its songs set side by side. One picture alone, two side by side halves, three or more as a 2 x 2 grid.
 * Songs from the same album count once, so a playlist full of one album doesn't show four copies of the same cover.
 * Idea from Mochy Music's playlist covers (GPL-3.0, github.com/realidkroo/Mochy-Music); written for milo on its own cover loader.
 */
@Composable
fun CoverCollage(vm: MainViewModel, tracks: List<Track>, size: Dp, radius: Dp = 14.dp, modifier: Modifier = Modifier, singlePx: Int = 300, tilePx: Int = 200) {
    val shown = remember(tracks) {
        val seen = HashSet<String>()
        val out = ArrayList<Track>(4)
        for (t in tracks) {
            val key = if (t.artUrl.isNotEmpty()) t.artUrl else "a" + t.albumId
            if (seen.add(key)) out.add(t)
            if (out.size == 4) break
        }
        out
    }
    val shape = RoundedCornerShape(radius)
    Box(modifier.size(size).clip(shape).background(Brush.verticalGradient(listOf(Milo.card2, Milo.card))), contentAlignment = Alignment.Center) {
        when (shown.size) {
            0 -> Icon(MiloIcons.QueueMusic, null, tint = Milo.dim, modifier = Modifier.size(size * 0.45f))
            1 -> Cover(vm, shown[0], size, 0.dp, px = singlePx)
            2 -> Row(Modifier.fillMaxSize()) {
                // two halves: each cover is cropped to a tall strip
                Box(Modifier.size(size / 2, size).clip(RoundedCornerShape(0.dp))) { Cover(vm, shown[0], size, 0.dp, px = tilePx, modifier = Modifier) }
                Box(Modifier.size(size / 2, size).clip(RoundedCornerShape(0.dp))) { Cover(vm, shown[1], size, 0.dp, px = tilePx, modifier = Modifier) }
            }
            else -> Column(Modifier.fillMaxSize()) {
                val h = size / 2
                Row { Cover(vm, shown[0], h, 0.dp, px = tilePx); Cover(vm, shown[1], h, 0.dp, px = tilePx) }
                Row {
                    Cover(vm, shown[2], h, 0.dp, px = tilePx)
                    if (shown.size > 3) Cover(vm, shown[3], h, 0.dp, px = tilePx) else Box(Modifier.size(h).background(Color(0x22FFFFFF)))
                }
            }
        }
    }
}
