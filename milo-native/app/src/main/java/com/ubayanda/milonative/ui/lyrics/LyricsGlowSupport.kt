package com.ubayanda.milonative.ui.lyrics

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// from Mochy Music (GPL-3.0): the settings and constants its lyric line reads

/** the settings the lyric line reads (Mochy keeps them in its animation settings) */
object LyricsGlow {
    /** false: lyric lines stay sharp instead of blurring the ones that aren't sung */
    @Volatile var elementBlur: Boolean = true
}

/** where the words start across a lyric row, level with the artwork's left edge */
internal val LyricsHeaderArtX = 36.dp

/** an instrumental gap this long reads as interlude dots */
const val MinInterludeGapMs = 4_000L

/** Mochy bounds repaints of busy areas in a layer of their own; milo draws them as they are */
fun Modifier.damageBoundary(): Modifier = this

// risesWords: words rise and lines cascade (style 1's motion), shaded: the agsl glow and wave run on top
enum class LyricsMotionStyle(
    val displayName: String,
    val description: String,
    val risesWords: Boolean,
    val shaded: Boolean,
) {
    /** Style 1: words rise into place as they're sung, held notes swell and glow, lines cascade. */
    MOTION("Style 1", "Words rise as they're sung, long notes swell, lines cascade", risesWords = true, shaded = true),

    /** Style 2: the original - a wave across the sung letters, the list scrolls as one piece. */
    LEGACY("Style 2", "The original wave across the sung letters", risesWords = false, shaded = true),

    // style 1 without the shader, for phones that struggle with it
    BORING("Boring", "Style 1's rise, without the glow", risesWords = true, shaded = false),

    // style 2 without the shader, just the colour sweeping along
    PLAIN("Plain", "Style 2's sweep, without the glow or the wave", risesWords = false, shaded = false);

    // the same motion with the shader switched off, used when moving people off the old shader switch
    val withoutShader: LyricsMotionStyle get() = if (risesWords) BORING else PLAIN
}
