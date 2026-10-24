package com.ubayanda.milonative.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.ubayanda.milonative.R

/** The same palette as the web milo. (dark theme): near-black, warm cream text, amber accent. */
object Milo {
    val bg = Color(0xFF000000)
    val panel = Color(0xFF0B0A09)
    val card = Color(0xFF141210)
    val card2 = Color(0xFF1D1A17)
    val line = Color(0xFF2B2723)
    val text = Color(0xFFF3EDE0)
    val dim = Color(0xFFA8A194)
    val amber = Color(0xFFF99C00)
}

val Figtree = FontFamily(
    listOf(400, 500, 600, 700, 800).map { w ->
        Font(R.font.figtree, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
    }
)

val Fraunces = FontFamily(
    listOf(500, 600).map { w ->
        Font(R.font.fraunces, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
    }
)

@Composable
fun MiloTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Milo.amber,
            onPrimary = Color.Black,
            background = Milo.bg,
            surface = Milo.card,
            onSurface = Milo.text,
            onBackground = Milo.text,
            surfaceVariant = Milo.card2,
        ),
    ) {
        ProvideTextStyle(TextStyle(fontFamily = Figtree)) { content() }
    }
}
