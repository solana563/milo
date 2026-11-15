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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
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
    /** The song's own accent (from the cover); used while the person has not picked a fixed accent. */
    var songAccent by mutableStateOf<Color?>(null)
    var picked by mutableStateOf(false)
}

/** The accent colour the person picked (amber by default). */
object Accent {
    val choices = listOf(0xFFF99C00, 0xFFFF6B6B, 0xFFE06BFF, 0xFF6B9BFF, 0xFF3DDC97, 0xFFF4EFE4).map { Color(it) }
    private fun p(c: android.content.Context) = c.getSharedPreferences("milo_ui", android.content.Context.MODE_PRIVATE)
    fun load(c: android.content.Context) { val v = p(c).getLong("accent", 0L); if (v != 0L) { TintState.accent = Color(v.toULong()); TintState.picked = true } }
    fun set(c: android.content.Context, col: Color) { TintState.accent = col; TintState.picked = true; p(c).edit().putLong("accent", col.value.toLong()).apply() }
}

/** The same palette as the web milo.: near-black, warm cream text, amber accent; panels, cards and lines pick up the song's colour. */
object Milo {
    val bg = Color(0xFF000000)
    val panel: Color get() = TintState.panel
    val card: Color get() = TintState.card
    val card2: Color get() = TintState.card2
    val line: Color get() = TintState.line
    val text: Color get() = if (ChromeStyle.on) ChromeStyle.text else if (Nebula.on) Nebula.text else if (Sticker.on) Color.White else if (Spider.on) Spider.bone else if (Y2K.on) Y2K.ice else Color(0xFFF3EDE0)
    val cream: Color get() = if (ChromeStyle.on) ChromeStyle.text else if (Nebula.on) Nebula.text else if (Sticker.on) Color.White else if (Spider.on) Spider.bone else if (Y2K.on) Y2K.ice else Color(0xFFF4EFE4)
    val dim: Color get() = if (ChromeStyle.on) ChromeStyle.dim else if (Nebula.on) Nebula.dim else if (Sticker.on) Sticker.dim else if (Spider.on) Spider.dim else if (Y2K.on) Y2K.dim else Color(0xFFA8A194)
    val amber: Color get() = if (ChromeStyle.on) ChromeStyle.silver else if (Nebula.on) Nebula.cyan else if (Sticker.on) Color.White else if (Spider.on) Spider.red else if (Y2K.on) Y2K.aqua else if (!TintState.picked) (TintState.songAccent ?: TintState.accent) else TintState.accent
}

val Figtree = FontFamily(
    listOf(400, 500, 600, 700, 800).map { w ->
        Font(R.font.figtree, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
    }
)

/** milo.'s own face: Figtree ExtraBold / Black Italic (SIL OFL). Used for the wordmark, never for body text. */
val MiloBrand = FontFamily(
    listOf(800, 900).map { w ->
        Font(R.font.figtree_italic, FontWeight(w), FontStyle.Italic, variationSettings = FontVariation.Settings(FontVariation.weight(w)))
    }
)

/** The milo. wordmark as text: heavy italic, tight, with the full stop in the accent colour. */
@Composable
fun MiloWordmark(size: androidx.compose.ui.unit.TextUnit, modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier, color: Color = Milo.text, dot: Color = Milo.amber) {
    androidx.compose.material3.Text(
        androidx.compose.ui.text.buildAnnotatedString {
            append("milo")
            pushStyle(androidx.compose.ui.text.SpanStyle(color = dot)); append("."); pop()
        },
        modifier = modifier, color = color, fontSize = size, fontFamily = MiloBrand, fontWeight = FontWeight(900), fontStyle = FontStyle.Italic,
        letterSpacing = (-0.04f * size.value).sp,
    )
}

val Fraunces = FontFamily(
    listOf(500, 600).map { w ->
        Font(R.font.fraunces, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
    }
)

@Composable
fun MiloTheme(content: @Composable () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.remember { Accent.load(ctx); Y2K.load(ctx); Spider.load(ctx); Sticker.load(ctx); Nebula.load(ctx); ChromeStyle.load(ctx); ClassicMode.load(ctx) }
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
