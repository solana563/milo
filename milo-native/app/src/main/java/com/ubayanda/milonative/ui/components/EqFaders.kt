package com.ubayanda.milonative.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubayanda.milonative.player.GraphicEq
import com.ubayanda.milonative.ui.theme.Milo
import kotlin.math.abs
import kotlin.math.roundToInt

private fun label(hz: Float) = if (hz >= 1000f) (hz / 1000f).roundToInt().toString() + "k" else hz.roundToInt().toString()

/** One vertical fader, -12..+12 dB. Drag or tap to set it, double-tap to return it to 0. */
@Composable
private fun Fader(db: Float, lit: Boolean, name: String, onChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    val cb by rememberUpdatedState(onChange)
    var lastDown by remember { mutableLongStateOf(0L) }
    val amber = Milo.amber
    Canvas(
        modifier
            .semantics { contentDescription = "$name, " + (if (db > 0f) "+" else "") + db.roundToInt() + " decibels" }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val pad = 16.dp.toPx()
                    fun set(y: Float) {
                        val f = ((y - pad) / (size.height - 2 * pad)).coerceIn(0f, 1f)
                        var v = ((12f - f * 24f) * 2f).roundToInt() / 2f
                        if (abs(v) < 0.5f) v = 0f
                        cb(v)
                    }
                    if (down.uptimeMillis - lastDown < 320L) { lastDown = 0L; cb(0f); down.consume(); return@awaitEachGesture }
                    lastDown = down.uptimeMillis
                    set(down.position.y); down.consume()
                    drag(down.id) { ch -> set(ch.position.y); ch.consume() }
                }
            },
    ) {
        val pad = 16.dp.toPx()
        val cx = size.width / 2f
        val gw = 7.dp.toPx()
        val top = pad; val bottom = size.height - pad
        val mid = (top + bottom) / 2f
        val ky = top + (12f - db) / 24f * (bottom - top)
        // the groove, dark and slightly inset
        drawRoundRect(Color(0xFF080706), Offset(cx - gw / 2, top - 4.dp.toPx()), Size(gw, bottom - top + 8.dp.toPx()), CornerRadius(gw / 2))
        drawRoundRect(Color(0x22FFFFFF), Offset(cx - gw / 2, top - 4.dp.toPx()), Size(gw, bottom - top + 8.dp.toPx()), CornerRadius(gw / 2), style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
        // centre tick and the lit part between centre and knob
        drawLine(Color(0x55FFFFFF), Offset(cx - 13.dp.toPx(), mid), Offset(cx - 7.dp.toPx(), mid), 1.5f)
        drawLine(Color(0x55FFFFFF), Offset(cx + 7.dp.toPx(), mid), Offset(cx + 13.dp.toPx(), mid), 1.5f)
        if (db != 0f) drawRoundRect(
            if (lit) amber else Color(0xFF6B655A),
            Offset(cx - gw / 2 + 1.5f, minOf(ky, mid)), Size(gw - 3f, abs(ky - mid)), CornerRadius(gw / 2),
        )
        // the knob: a soft metal disc
        val r = 13.dp.toPx()
        drawCircle(Color(0x66000000), r + 2.dp.toPx(), Offset(cx, ky + 2.dp.toPx()))
        drawCircle(Brush.verticalGradient(listOf(Color(0xFFF6F1E6), Color(0xFFA9A396)), startY = ky - r, endY = ky + r), r, Offset(cx, ky))
        drawCircle(Color(0x44000000), r, Offset(cx, ky), style = androidx.compose.ui.graphics.drawscope.Stroke(1.2f))
        drawCircle(if (lit && db != 0f) amber else Color(0xFF7A7467), 3.5.dp.toPx(), Offset(cx, ky))
    }
}

/** The graphic equaliser: presets, ten faders (32 Hz to 16 kHz), a power button and a reset. */
@Composable
fun GraphicEqPanel() {
    val st by GraphicEq.state.collectAsState()
    var sel by remember { mutableIntStateOf(-1) }
    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF211F1C), Color(0xFF151412))), shape)
            .border(1.dp, Color(0x22FFFFFF), shape).padding(vertical = 14.dp),
    ) {
        LazyRow(contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(GraphicEq.presets.keys.toList()) { n -> MiloChip(n, st.preset == n && st.enabled) { GraphicEq.applyPreset(n) } }
        }
        Row(Modifier.padding(top = 10.dp, start = 6.dp, end = 10.dp)) {
            // dB scale
            Column(Modifier.width(30.dp).height(214.dp).padding(vertical = 8.dp), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.End) {
                Text("+12", color = Milo.dim, fontSize = 10.sp)
                Text("0", color = Milo.dim, fontSize = 10.sp)
                Text("-12", color = Milo.dim, fontSize = 10.sp)
            }
            Row(Modifier.weight(1f)) {
                for (i in 0 until GraphicEq.N) {
                    val db = st.gains[i]
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(5.dp).clip(CircleShape).background(if (st.enabled && db != 0f) Milo.amber else Color(0xFF3A3631)))
                        Fader(db, st.enabled, label(GraphicEq.FREQS[i]) + " hertz", { sel = i; GraphicEq.setGain(i, it) }, Modifier.fillMaxWidth().height(200.dp))
                        Text(label(GraphicEq.FREQS[i]), color = if (sel == i) Milo.amber else Milo.dim, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        Row(Modifier.padding(horizontal = 16.dp).padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(if (st.enabled) Milo.amber else Color(0xFF2A2724), CircleShape)
                    .border(1.dp, Color(0x33FFFFFF), CircleShape).clickable { GraphicEq.setEnabled(!st.enabled) }
                    .semantics { contentDescription = if (st.enabled) "Equaliser on. Tap to turn off" else "Equaliser off. Tap to turn on" },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.PowerSettingsNew, null, tint = if (st.enabled) Color.Black else Milo.dim, modifier = Modifier.size(24.dp)) }
            Text(
                if (sel in 0 until GraphicEq.N) label(GraphicEq.FREQS[sel]) + " Hz  " + "%+.1f dB".format(st.gains[sel]) else if (st.enabled) st.preset else "Off",
                color = Milo.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f).padding(horizontal = 14.dp),
            )
            MiloChip("Reset", false) { GraphicEq.reset(); sel = -1 }
        }
        Text("Drag a fader, or tap where you want it. Double-tap to zero one.", color = Milo.dim, fontSize = 12.sp, modifier = Modifier.padding(start = 16.dp, top = 8.dp))
    }
}
