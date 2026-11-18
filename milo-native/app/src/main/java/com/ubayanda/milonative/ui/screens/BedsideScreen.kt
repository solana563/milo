package com.ubayanda.milonative.ui.screens

import android.text.format.DateFormat
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubayanda.milonative.model.PlayerUi
import com.ubayanda.milonative.model.Track
import com.ubayanda.milonative.ui.MainViewModel
import com.ubayanda.milonative.ui.components.Cover
import com.ubayanda.milonative.ui.theme.Milo
import com.ubayanda.milonative.ui.theme.MiloIcons
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Bedside clock: a very dark, calm screen with a large time, the date, what's playing and simple controls. The screen stays on.
 * Tap the time to cycle brightness (bright, dim, very dim). The clock drifts a few pixels every minute so nothing burns in.
 * Idea from Mochy Music's bedside mode (GPL-3.0, github.com/realidkroo/Mochy-Music, ui/player/bedside); written for milo.
 */
@Composable
fun BedsideScreen(vm: MainViewModel, t: Track?, ui: PlayerUi, onClose: () -> Unit) {
    val view = LocalView.current
    val ctx = LocalContext.current
    DisposableEffect(Unit) {
        val old = view.keepScreenOn
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = old }
    }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            kotlinx.coroutines.delay(60_000L - now % 60_000L + 50L)
        }
    }
    val is24 = remember { DateFormat.is24HourFormat(ctx) }
    val cal = Calendar.getInstance().apply { timeInMillis = now }
    val time = if (is24) "%02d:%02d".format(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
    else "%d:%02d".format(cal.get(Calendar.HOUR).let { if (it == 0) 12 else it }, cal.get(Calendar.MINUTE))
    val suffix = if (is24) "" else if (cal.get(Calendar.AM_PM) == Calendar.AM) " am" else " pm"
    val date = remember(now / 3_600_000L) { java.text.SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date(now)) }

    var level by remember { mutableIntStateOf(1) }
    val bright by animateFloatAsState(listOf(1f, 0.55f, 0.25f)[level], tween(400), label = "bedBright")
    val minute = (now / 60_000L).toInt()
    val dx by animateDpAsState(((minute * 7) % 25 - 12).dp, tween(2000), label = "bedDx")
    val dy by animateDpAsState(((minute * 11) % 25 - 12).dp, tween(2000), label = "bedDy")
    val ink = Color.White.copy(alpha = bright)
    val soft = Color.White.copy(alpha = bright * 0.55f)

    Column(
        Modifier.fillMaxSize().background(Color.Black).statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Close", color = soft, fontSize = 16.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.clip(CircleShape).clickable(onClick = onClose).padding(horizontal = 16.dp, vertical = 10.dp))
            Spacer(Modifier.weight(1f))
            val left = ui.sleepLeftMs
            Text(if (left > 0) "Sleep in ${(left / 60_000L) + 1} min · cancel" else "Sleep in 30 min", color = soft, fontSize = 16.sp,
                modifier = Modifier.clip(CircleShape).clickable { vm.player.sleepIn(if (left > 0) 0 else 30) }.padding(horizontal = 16.dp, vertical = 10.dp))
        }
        Spacer(Modifier.weight(1f))
        Column(
            Modifier.offset(dx, dy).clickable { level = (level + 1) % 3 },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(time, color = ink, fontSize = 112.sp, fontWeight = FontWeight.Light, maxLines = 1)
                if (suffix.isNotEmpty()) Text(suffix, color = soft, fontSize = 28.sp, modifier = Modifier.padding(bottom = 22.dp))
            }
            Text(date, color = soft, fontSize = 20.sp)
        }
        Spacer(Modifier.weight(1f))
        if (t != null) {
            Row(Modifier.fillMaxWidth().offset(dx, dy), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(56.dp)) { Cover(vm, t, 56.dp, 12.dp, px = 200) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(t.title, color = ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(t.artist, color = soft, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.height(10.dp))
            val frac = if (ui.durationMs > 0) (ui.positionMs.toFloat() / ui.durationMs).coerceIn(0f, 1f) else 0f
            Box(Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(Color.White.copy(alpha = bright * 0.15f))) {
                Box(Modifier.fillMaxWidth(frac).height(3.dp).background(soft))
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            Ctl(MiloIcons.SkipPrevious, "Previous", 64.dp, soft) { vm.player.previous() }
            Ctl(if (ui.isPlaying) MiloIcons.Pause else MiloIcons.PlayArrow, "Play or pause", 84.dp, ink) { vm.player.toggle() }
            Ctl(MiloIcons.SkipNext, "Next", 64.dp, soft) { vm.player.next() }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun Ctl(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, size: androidx.compose.ui.unit.Dp, tint: Color, onClick: () -> Unit) {
    Box(Modifier.size(size).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, desc, tint = tint, modifier = Modifier.size(size * 0.55f))
    }
}
