package com.ubayanda.milonative.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubayanda.milonative.data.LyricsTranslator
import com.ubayanda.milonative.ui.FloatingLyrics
import com.ubayanda.milonative.ui.theme.Milo

/** How the floating lyrics card looks and behaves. Every change shows on the card straight away. */
@Composable
fun FloatingLyricsSettings(onClose: () -> Unit) {
    val ctx = LocalContext.current
    var meta by remember { mutableStateOf(FloatingLyrics.showMeta(ctx)) }
    var op by remember { mutableFloatStateOf(FloatingLyrics.opacity(ctx).toFloat()) }
    var sz by remember { mutableFloatStateOf(FloatingLyrics.textSize(ctx).toFloat()) }
    var lang by remember { mutableStateOf(FloatingLyrics.lang(ctx)) }
    var lock by remember { mutableStateOf(FloatingLyrics.locked(ctx)) }
    var langMenu by remember { mutableStateOf(false) }
    val sliderColors = SliderDefaults.colors(thumbColor = Milo.amber, activeTrackColor = Milo.amber, inactiveTrackColor = Milo.card2)
    val switchColors = SwitchDefaults.colors(checkedThumbColor = Milo.bg, checkedTrackColor = Milo.amber)

    AlertDialog(
        onDismissRequest = onClose,
        containerColor = Milo.card,
        title = { Text("Floating lyrics", color = Milo.text) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Cover, title and artist", color = Milo.text, modifier = Modifier.weight(1f))
                    Switch(meta, { meta = it; FloatingLyrics.setShowMeta(ctx, it) }, colors = switchColors)
                }
                Text("Window opacity: ${op.toInt()}%", color = Milo.text, fontSize = 14.sp)
                Slider(op, { op = it; FloatingLyrics.setOpacity(ctx, it.toInt()) }, valueRange = 20f..100f, colors = sliderColors)
                Text("Text size: ${sz.toInt()}", color = Milo.text, fontSize = 14.sp)
                Slider(sz, { sz = it; FloatingLyrics.setTextSize(ctx, it.toInt()) }, valueRange = 12f..32f, colors = sliderColors)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Translation", color = Milo.text, modifier = Modifier.weight(1f))
                    Box {
                        TextButton(onClick = { langMenu = true }) { Text(LyricsTranslator.languages.firstOrNull { it.first == lang }?.second ?: "Off", color = Milo.amber) }
                        DropdownMenu(langMenu, { langMenu = false }) {
                            DropdownMenuItem(text = { Text("Off") }, onClick = { lang = ""; FloatingLyrics.setLang(ctx, ""); langMenu = false })
                            LyricsTranslator.languages.forEach { (code, name) ->
                                DropdownMenuItem(text = { Text(name) }, onClick = { lang = code; FloatingLyrics.setLang(ctx, code); langMenu = false })
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Lock (click-through)", color = Milo.text)
                        Text("Touches pass through the card. A small lock button stays on screen to unlock it.", color = Milo.dim, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp, end = 8.dp))
                    }
                    Switch(lock, { lock = it; FloatingLyrics.setLocked(ctx, it) }, colors = switchColors)
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Done", color = Milo.amber) } },
    )
}
