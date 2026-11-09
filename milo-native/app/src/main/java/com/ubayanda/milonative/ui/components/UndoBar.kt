package com.ubayanda.milonative.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubayanda.milonative.data.OfflineStore
import com.ubayanda.milonative.ui.MainViewModel
import com.ubayanda.milonative.ui.theme.Milo
import kotlinx.coroutines.delay

/** "Removed from Liked songs  [Undo]" for five seconds after something is removed. */
@Composable
fun UndoBar(vm: MainViewModel, modifier: Modifier = Modifier) {
    val item by vm.undo.collectAsState()
    val cur = item
    LaunchedEffect(cur?.id) {
        if (cur != null) { delay(5000); if (vm.undo.value?.id == cur.id) vm.dismissUndo() }
    }
    AnimatedVisibility(visible = cur != null, enter = fadeIn() + slideInVertically { it / 2 }, exit = fadeOut(), modifier = modifier) {
        val shape = RoundedCornerShape(22.dp)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp).clip(shape).background(Milo.card2, shape).border(1.dp, Milo.line, shape).padding(start = 18.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(cur?.message.orEmpty(), color = Milo.text, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text(
                "Undo", color = Milo.amber, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable { vm.doUndo() }.padding(horizontal = 16.dp, vertical = 14.dp),
            )
        }
    }
}

/** "Downloading 3 songs left · 42%" while the download queue is working. */
@Composable
fun DownloadBar(modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    val left by OfflineStore.pending.collectAsState()
    val prog by OfflineStore.progress.collectAsState()
    val active = prog.values.filter { it >= 0f }
    AnimatedVisibility(visible = left > 0, enter = fadeIn() + slideInVertically { it / 2 }, exit = fadeOut(), modifier = modifier) {
        val shape = RoundedCornerShape(22.dp)
        val avg = if (active.isEmpty()) 0f else active.average().toFloat()
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp).clip(shape).clickable(onClick = onClick).background(Milo.card2, shape).border(1.dp, Milo.line, shape).padding(horizontal = 18.dp, vertical = 12.dp)) {
            Text(
                "Downloading · " + left + (if (left == 1) " song" else " songs") + " left" + (if (active.isNotEmpty()) " · " + (avg * 100).toInt() + "%" else ""),
                color = Milo.text, fontSize = 14.sp, maxLines = 1,
            )
            LinearProgressIndicator(progress = { avg }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(4.dp).clip(RoundedCornerShape(2.dp)), color = Milo.amber, trackColor = Milo.line)
        }
    }
}
