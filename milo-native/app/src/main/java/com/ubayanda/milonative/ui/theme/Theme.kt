package com.ubayanda.milonative.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ubayanda.milonative.R

/** Colours that follow the playing song (like the web milo.). MiloApp animates these; screens just read Milo.panel etc. */
object TintState {
    var panel by mutableStateOf(Color(0xFF0B0A09))
    var card by mutableStateOf(Color(0xFF141210))
    var card2 by mutableStateOf(Color(0xFF1D1A17))
    var line by mutableStateOf(Color(0xFF2B2723))
    var art by mutableStateOf(false)
    var accent by mutableStateOf(Color(0xFFF99C00))
}

/** The accent colour the person picked (amber by default). */
object Accent {
    val choices = listOf(0xFFF99C00, 0xFFFF6B6B, 0xFFE06BFF, 0xFF6B9BFF, 0xFF3DDC97, 0xFFF4EFE4).map { Color(it) }
    private fun p(c: android.content.Context) = c.getSharedPreferences("milo_ui", android.content.Context.MODE_PRIVATE)
    fun load(c: android.content.Context) { val v = p(c).getLong("accent", 0L); if (v != 0L) TintState.accent = Color(v.toULong()) }
    fun set(c: android.content.Context, col: Color) { TintState.accent = col; p(c).edit().putLong("accent", col.value.toLong()).apply() }
}

/** The same palette as the web milo.: near-black, warm cream text, amber accent; panels, cards and lines pick up the song's colour. */
object Milo {
    val bg = Color(0xFF000000)
    val panel: Color get() = TintState.panel
    val card: Color get() = TintState.card
    val card2: Color get() = TintState.card2
    val line: Color get() = TintState.line
    val text = Color(0xFFF3EDE0)
    val cream = Color(0xFFF4EFE4)
    val dim = Color(0xFFA8A194)
    val amber: Color get() = TintState.accent
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
    val ctx = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.remember { Accent.load(ctx) }
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Milo.amber,
            onPrimary = Color.Black,
            background = Milo.bg,
            surface = Milo.card,
            onSurface = Milo.text,
            onBackground = Milo.text,
            surfaceVariant = Milo.card2,
            surfaceContainer = Milo.card2,
            surfaceContainerHigh = Milo.card2,
            surfaceContainerHighest = Milo.card2,
        ),
        shapes = androidx.compose.material3.Shapes(
            extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
            small = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            medium = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
            extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(34.dp),
        ),
    ) {
        ProvideTextStyle(TextStyle(fontFamily = Figtree)) { content() }
    }
}
