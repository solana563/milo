package com.ubayanda.milonative.ui.screens

import android.content.Context
import android.media.AudioManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubayanda.milonative.model.PlayerUi
import com.ubayanda.milonative.model.Track
import com.ubayanda.milonative.ui.MainViewModel
import com.ubayanda.milonative.ui.components.Cover
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min

/**
 * iPod skin: a small grey-green screen on top and a click wheel below. Turn your finger around the wheel to change the volume;
 * press the middle to switch the wheel to scrubbing the song and back. Top of the ring = close, left/right = previous/next, bottom = play or pause.
 * Tap the screen's title bar to switch between the white and the black iPod.
 * Idea from Mochy Music's iPod player style (GPL-3.0, github.com/realidkroo/Mochy-Music, ui/player/ipod); written for milo, much smaller.
 */
@Composable
fun IpodScreen(vm: MainViewModel, t: Track?, ui: PlayerUi, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val prefs = remember { ctx.getSharedPreferences("milo_fx", Context.MODE_PRIVATE) }
    var black by remember { mutableStateOf(prefs.getBoolean("ipod_black", false)) }
    val body = if (black) Color(0xFF16161A) else Color(0xFFEDEBE6)
    val wheel = if (black) Color(0xFF26262B) else Color(0xFFFAF9F5)
    val hub = if (black) Color(0xFF16161A) else Color(0xFFDAD8D2)
    val label = if (black) Color(0xFFE8E6E0) else Color(0xFF8A8780)

    val am = remember { ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxV = remember { am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var vol by remember { mutableIntStateOf(am.getStreamVolume(AudioManager.STREAM_MUSIC)) }
    var mode by remember { mutableIntStateOf(0) }       // 0 progress, 1 volume bar showing, 2 scrubbing
    var scrubTo by remember { mutableLongStateOf(0L) }
    var lastTurn by remember { mutableLongStateOf(0L) }

    // put the volume bar / scrub marker away a moment after the last turn
    LaunchedEffect(lastTurn) {
        if (lastTurn == 0L) return@LaunchedEffect
        kotlinx.coroutines.delay(1600)
        if (mode == 1) mode = 0
    }

    fun turn(dir: Int) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        lastTurn = System.currentTimeMillis()
        if (mode == 2) {
            val step = (ui.durationMs / 60).coerceIn(2_000L, 10_000L)
            scrubTo = (scrubTo + dir * step).coerceIn(0L, ui.durationMs.coerceAtLeast(0L))
            vm.player.seekTo(scrubTo)
        } else {
            vol = (am.getStreamVolume(AudioManager.STREAM_MUSIC) + dir).coerceIn(0, maxV)
            am.setStreamVolume(AudioManager.STREAM_MUSIC, vol, 0)
            mode = 1
        }
    }

    Column(
        Modifier.fillMaxSize().background(body).statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // screen
        val lcd = if (black) Color(0xFFB9C6B0) else Color(0xFFD5DFCC)
        val ink = Color(0xFF1B1F1A)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(lcd)) {
            Row(
                Modifier.fillMaxWidth().background(Color(0xFF1B1F1A).copy(alpha = 0.12f))
                    .clickable { black = !black; prefs.edit().putBoolean("ipod_black", black).apply() }.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (ui.isPlaying) "▶" else "❚❚", color = ink, fontSize = 12.sp)
                Text("Now Playing", color = ink, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                Text(if (ui.queueIds.isEmpty()) "" else "${ui.queueIndex + 1} of ${ui.queueIds.size}", color = ink, fontSize = 12.sp)
            }
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                if (t != null) Box(Modifier.size(104.dp).clip(RoundedCornerShape(4.dp))) { Cover(vm, t, 104.dp, 4.dp, px = 300) }
                else Box(Modifier.size(104.dp).background(ink.copy(alpha = 0.1f)))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(t?.title ?: "Nothing playing", color = ink, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(t?.artist.orEmpty(), color = ink, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(t?.album.orEmpty(), color = ink.copy(alpha = 0.7f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            // the bar: volume while turning, otherwise progress (or the scrub position)
            val pos = if (mode == 2) scrubTo else ui.positionMs
            val frac = when {
                mode == 1 -> vol.toFloat() / maxV
                ui.durationMs > 0 -> (pos.toFloat() / ui.durationMs).coerceIn(0f, 1f)
                else -> 0f
            }
            Box(Modifier.padding(horizontal = 14.dp).fillMaxWidth().height(10.dp).clip(RoundedCornerShape(2.dp)).background(ink.copy(alpha = 0.18f))) {
                Box(Modifier.fillMaxWidth(frac).height(10.dp).background(ink))
            }
            Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 10.dp)) {
                Text(if (mode == 1) "Volume" else fmt(pos), color = ink, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text(if (mode == 2) "Scrubbing" else if (mode == 1) "" else "-" + fmt((ui.durationMs - pos).coerceAtLeast(0L)), color = ink, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.weight(1f))

        // click wheel (the gesture blocks live for the whole screen, so they read the newest state through these)
        val uiNow by rememberUpdatedState(ui)
        val turnNow by rememberUpdatedState<(Int) -> Unit>({ d -> turn(d) })
        var box by remember { mutableStateOf(IntSize.Zero) }
        var lastAngle by remember { mutableStateOf(0.0) }
        var acc by remember { mutableStateOf(0.0) }
        var onRing by remember { mutableStateOf(false) }
        Box(
            Modifier.fillMaxWidth(0.82f).widthIn(max = 340.dp).aspectRatio(1f).clip(CircleShape).background(wheel)
                .onSizeChanged { box = it }
                .pointerInput(Unit) {
                    detectTapGestures { p ->
                        val cx = box.width / 2f; val cy = box.height / 2f
                        val dx = p.x - cx; val dy = p.y - cy
                        val r = hypot(dx, dy)
                        if (r < box.width * 0.17f) { mode = if (mode == 2) 0 else 2; scrubTo = uiNow.positionMs; haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                        else if (abs(dy) > abs(dx)) { if (dy < 0) onClose() else vm.player.toggle() }
                        else if (dx < 0) vm.player.previous() else vm.player.next()
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { p ->
                            val cx = box.width / 2f; val cy = box.height / 2f
                            onRing = hypot(p.x - cx, p.y - cy) >= box.width * 0.17f
                            lastAngle = Math.toDegrees(atan2((p.y - cy).toDouble(), (p.x - cx).toDouble())); acc = 0.0
                        },
                        onDrag = { change, _ ->
                            if (onRing) {
                                val cx = box.width / 2f; val cy = box.height / 2f
                                val a = Math.toDegrees(atan2((change.position.y - cy).toDouble(), (change.position.x - cx).toDouble()))
                                var d = a - lastAngle
                                if (d > 180) d -= 360
                                if (d < -180) d += 360
                                lastAngle = a; acc += d
                                while (abs(acc) >= 16.0) { val dir = if (acc > 0) 1 else -1; turnNow(dir); acc -= 16.0 * dir }
                                change.consume()
                            }
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Text("MENU", color = label, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.TopCenter).padding(top = 22.dp))
            Text(if (ui.isPlaying) "❚❚" else "▶", color = label, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp))
            Text("⏮", color = label, fontSize = 20.sp, modifier = Modifier.align(Alignment.CenterStart).padding(start = 24.dp))
            Text("⏭", color = label, fontSize = 20.sp, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 24.dp))
            Box(Modifier.fillMaxWidth(0.34f).aspectRatio(1f).clip(CircleShape).background(hub))
        }
        Spacer(Modifier.height(14.dp))
        Text(if (mode == 2) "Middle button: back to volume" else "Turn for volume · middle button to scrub", color = label, fontSize = 12.sp)
    }
}

private fun fmt(ms: Long): String { val s = (ms / 1000).coerceAtLeast(0); return "%d:%02d".format(s / 60, s % 60) }
