package com.ubayanda.milonative.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A small text-symbol button with a 48 dp touch area and a spoken label for TalkBack. */
@Composable
fun IconAction(glyph: String, label: String, tint: Color, onClick: () -> Unit) {
    Box(Modifier.size(48.dp).pressable(true, onClick).semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        Text(glyph, color = tint, fontSize = 18.sp)
    }
}
