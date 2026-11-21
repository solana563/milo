// Ported from Mochy Music's ui/player/MiniLyricsTicker.kt (GPL-3.0, github.com/realidkroo/Mochy-Music).
// Single-row card: the line being sung with the karaoke sweep, long lines slide sideways following the
// sweep, new lines roll up from below, instrumental gaps show dots. Not ported: the "tall card" mode
// (readings + upcoming list), which needs Mochy's player editor.
@file:OptIn(androidx.compose.animation.ExperimentalAnimationApi::class)

package com.ubayanda.milonative.ui.lyrics

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubayanda.milonative.ui.components.InterludeDots

private val TickerFontSize = 17.sp
private val TickerLineHeight = 22.sp
private val TickerHeight = 44.dp

/** The letter motion is sized for 32sp lines; scaled down to match. */
private const val TickerMotionScale = 17f / 32f
private val TickerEdgeFade = 18.dp
private const val SweepKeepFraction = 0.62f
private const val SlideFollowSec = 0.32f
private const val UnsungAlpha = 0.45f

private val LineChangeSpring = spring(
    dampingRatio = 0.86f,
    stiffness = Spring.StiffnessLow,
    visibilityThreshold = IntOffset.VisibilityThreshold,
)
private val LineFadeSpring = spring<Float>(dampingRatio = 1f, stiffness = Spring.StiffnessMediumLow)

/** Mochy's LyricsUtils line helpers. */
object TickerLyrics {
    const val LineLeadInMs = 300L
    private const val UntimedLineDurationMs = 5_000L

    fun lineEndMs(lines: List<LyricsEntry>, index: Int): Long {
        val entry = lines.getOrNull(index) ?: return 0L
        entry.words?.lastOrNull()?.let { return (it.endTime * 1000).toLong() }
        lines.getOrNull(index + 1)?.let { return it.time }
        return entry.time + UntimedLineDurationMs
    }

    fun lyricsEndMs(lines: List<LyricsEntry>): Long =
        if (lines.isEmpty()) 0L else lineEndMs(lines, lines.lastIndex)

    /** Index of the line being sung, -1 before the first, lines.size after the last. */
    fun findCurrentLineIndex(lines: List<LyricsEntry>, position: Long): Int {
        if (lines.isEmpty()) return -1
        if (position >= lyricsEndMs(lines)) return lines.size
        for (index in lines.indices) {
            if (lines[index].time >= position + LineLeadInMs) return index - 1
        }
        return lines.lastIndex
    }
}

/** What the card is showing. Equal slots are the same content, so nothing re-animates. */
@Immutable
internal data class TickerSlot(val kind: Int, val index: Int) {
    companion object {
        const val EMPTY = 0
        const val LINE = 1
        const val GAP = 2
        const val END = 3
        const val LOADING = 4
    }
}

internal fun tickerSlot(lines: List<LyricsEntry>, position: Long): TickerSlot {
    if (lines.isEmpty()) return TickerSlot(TickerSlot.EMPTY, -1)
    val index = TickerLyrics.findCurrentLineIndex(lines, position)
    if (index >= lines.size) return TickerSlot(TickerSlot.END, lines.lastIndex)
    if (index < 0) {
        return if (lines.first().time >= MinInterludeGapMs) TickerSlot(TickerSlot.GAP, -1)
        else TickerSlot(TickerSlot.LINE, 0)
    }
    val next = lines.getOrNull(index + 1)
    if (next != null) {
        val end = TickerLyrics.lineEndMs(lines, index)
        if (next.time - end >= MinInterludeGapMs && position > end) return TickerSlot(TickerSlot.GAP, index)
    }
    return TickerSlot(TickerSlot.LINE, index)
}

/**
 * [lines] must be time-ordered; [loaded] false shows a quiet placeholder until lookup finishes.
 * [positionProvider] returns the lyric clock in ms (already offset for Bluetooth / user timing).
 * Only the slot is read from the clock in composition, so the card recomposes once per line.
 */
@Composable
fun MiniLyricsTicker(
    lines: List<LyricsEntry>,
    loaded: Boolean,
    positionProvider: () -> Long,
    modifier: Modifier = Modifier,
    motionStyle: LyricsMotionStyle = LyricsMotionStyle.MOTION,
    highBloom: Boolean = true,
) {
    val pos by rememberUpdatedState(positionProvider)
    val slot by remember(lines) { derivedStateOf { tickerSlot(lines, pos()) } }
    val shown = if (!loaded && slot.kind == TickerSlot.EMPTY) TickerSlot(TickerSlot.LOADING, -1) else slot
    val waveEnabled = motionStyle.shaded
    val rowPx = with(androidx.compose.ui.platform.LocalDensity.current) { TickerHeight.roundToPx() }

    AnimatedContent(
        targetState = shown,
        transitionSpec = {
            (slideInVertically(LineChangeSpring) { minOf(it, rowPx) } + fadeIn(LineFadeSpring))
                .togetherWith(slideOutVertically(LineChangeSpring) { -minOf(it, rowPx) } + fadeOut(LineFadeSpring))
                .using(SizeTransform(clip = false))
        },
        contentAlignment = Alignment.CenterStart,
        label = "miniLyricsLine",
        modifier = modifier.fillMaxWidth().height(TickerHeight).clipToBounds(),
    ) { current ->
        Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxSize()) {
            when (current.kind) {
                TickerSlot.LINE, TickerSlot.END -> lines.getOrNull(current.index)?.let { entry ->
                    TickerLine(
                        entry = entry,
                        dim = current.kind == TickerSlot.END,
                        lineEndMs = TickerLyrics.lineEndMs(lines, current.index),
                        positionProvider = { pos() },
                        motionStyle = motionStyle,
                        waveEnabled = waveEnabled,
                        highBloom = highBloom,
                    )
                }
                TickerSlot.GAP -> {
                    val startMs = if (current.index < 0) 0L else TickerLyrics.lineEndMs(lines, current.index)
                    val endMs = lines.getOrNull(current.index + 1)?.time ?: startMs
                    var now by remember { androidx.compose.runtime.mutableLongStateOf(pos()) }
                    LaunchedEffect(Unit) { while (true) { withFrameMillis { }; now = pos() } }
                    // scaled to card size, like Mochy's DotsScale
                    Box(Modifier.graphicsLayer {
                        scaleX = 0.5f; scaleY = 0.5f
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                    }) { InterludeDots(startMs, endMs, now) }
                }
                TickerSlot.LOADING -> Text("…", color = Color.White.copy(alpha = 0.45f), style = MaterialTheme.typography.bodyLarge)
                else -> Text(
                    "No lyrics",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.55f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun smoothstepT(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}

@Composable
private fun TickerLine(
    entry: LyricsEntry,
    dim: Boolean,
    lineEndMs: Long,
    positionProvider: () -> Long,
    motionStyle: LyricsMotionStyle,
    waveEnabled: Boolean,
    highBloom: Boolean,
) {
    val parts = remember(entry.text, entry.words) { splitBackingVocal(entry.text, entry.words) }
    val text = parts.lead.ifBlank { entry.text }
    val words = parts.leadWords
    val karaoke = !dim && !words.isNullOrEmpty() && text.isNotEmpty()

    val probe = remember(entry) { SweepProbe() }
    val viewport = remember { mutableFloatStateOf(0f) }
    val scroll = remember(entry) { mutableFloatStateOf(0f) }

    // The sideways slide follows the sweep (word-synced) or the clock (line-synced): a critically
    // damped follow, so a sweep that jumps (a seek) still glides.
    LaunchedEffect(entry, karaoke) {
        var value = 0f
        var velocity = 0f
        var lastMs = -1L
        while (true) {
            withFrameMillis { frameMs ->
                val dt = if (lastMs < 0L) 0.016f else ((frameMs - lastMs) / 1000f).coerceIn(0f, 0.064f)
                lastMs = frameMs
                val overflow = (probe.textWidth - viewport.floatValue).coerceAtLeast(0f)
                val target = when {
                    overflow <= 0f -> 0f
                    karaoke -> (probe.headX - viewport.floatValue * SweepKeepFraction).coerceIn(0f, overflow)
                    else -> {
                        val span = (lineEndMs - entry.time).coerceAtLeast(1L).toFloat()
                        val through = (positionProvider() - entry.time) / span
                        overflow * smoothstepT((through - 0.1f) / 0.75f)
                    }
                }
                val omega = 1f / SlideFollowSec
                val y = value - target
                val e = kotlin.math.exp(-omega * dt)
                val tmp = (velocity + omega * y) * dt
                value = target + (y + tmp) * e
                velocity = (velocity - omega * tmp) * e
                if (kotlin.math.abs(value - target) < 0.05f && kotlin.math.abs(velocity) < 1f) { value = target; velocity = 0f }
                if (value != scroll.floatValue) scroll.floatValue = value
            }
        }
    }

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { viewport.floatValue = it.width.toFloat() }
            .clipToBounds()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                // fade only the edge the line actually runs past, and only as far as it does
                val overflow = (probe.textWidth - size.width).coerceAtLeast(0f)
                if (overflow <= 0f) return@drawWithContent
                val fade = TickerEdgeFade.toPx()
                val left = (scroll.floatValue / fade).coerceIn(0f, 1f)
                val right = ((overflow - scroll.floatValue) / fade).coerceIn(0f, 1f)
                val edge = (fade / size.width).coerceIn(0f, 0.5f)
                drawRect(
                    brush = Brush.horizontalGradient(
                        0f to Color.Black.copy(alpha = 1f - left),
                        edge to Color.Black,
                        1f - edge to Color.Black,
                        1f to Color.Black.copy(alpha = 1f - right),
                    ),
                    blendMode = BlendMode.DstIn,
                )
            },
    ) {
        Box(
            modifier = Modifier
                .wrapContentWidth(align = Alignment.Start, unbounded = true)
                .graphicsLayer { translationX = -scroll.floatValue },
        ) {
            if (karaoke) {
                KaraokeSweepText(
                    text = text,
                    words = words!!,
                    positionProvider = positionProvider,
                    waveAmplitudeProvider = { if (waveEnabled) 1f else 0f },
                    riseAmplitudeProvider = { 1f },
                    baseColor = { Color.White.copy(alpha = UnsungAlpha) },
                    highlightColor = Color.White,
                    fontSize = TickerFontSize,
                    lineHeight = TickerLineHeight,
                    highBloom = highBloom,
                    motionStyle = motionStyle,
                    singleLine = true,
                    motionScale = TickerMotionScale,
                    probe = probe,
                )
            } else {
                Text(
                    text = text,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = TickerFontSize,
                        lineHeight = TickerLineHeight,
                    ).withLetterMotionFeatures(motionStyle.risesWords),
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = if (dim) 0.55f else 1f),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    onTextLayout = { probe.textWidth = it.size.width.toFloat() },
                )
            }
        }
    }
}
