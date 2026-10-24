package com.ubayanda.milonative.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object Milo {
    val bg = Color(0xFF000000)
    val card = Color(0xFF121212)
    val card2 = Color(0xFF1A1A1A)
    val text = Color(0xFFFFFFFF)
    val dim = Color(0xFFA0A0A0)
}

@Composable
fun MiloTheme(accent: Color, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = accent,
            background = Milo.bg,
            surface = Milo.card,
            onSurface = Milo.text,
            onBackground = Milo.text,
        ),
        content = content,
    )
}
