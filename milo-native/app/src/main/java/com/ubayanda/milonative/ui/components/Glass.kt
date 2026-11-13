package com.ubayanda.milonative.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubayanda.milonative.ui.theme.Milo
import com.ubayanda.milonative.ui.theme.Y2K
import com.ubayanda.milonative.ui.theme.Spider
import com.ubayanda.milonative.ui.theme.Sticker
import com.ubayanda.milonative.ui.theme.Nebula
import kotlin.math.roundToInt

/*
 * The glass look: surfaces are lightly translucent layers with a bright rim on the top-left edge that fades away at the bottom-right,
 * soft shadows that lift floating things off the page, big continuous corners, capsule controls and springy motion.
 */

/** The rim light: bright where the light hits (top-left), almost gone at the opposite corner. */
fun glassStroke(strength: Float = 1f): Brush = Brush.linearGradient(
    listOf(Color.White.copy(alpha = 0.34f * strength), Color.White.copy(alpha = 0.06f * strength), Color.White.copy(alpha = 0.14f * strength)),
)

/** A glass panel: the surface colour with a soft sheen from the top and a rim light. Use instead of a flat background + border. */
fun Modifier.glass(shape: Shape, strength: Float = 1f, tint: Color? = null): Modifier = this
    .clip(shape)
    .background(tint ?: Milo.card, shape)
    .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f * strength), Color.White.copy(alpha = 0.025f * strength))), shape)
    // Y2K: the aqua-gel look: a bright glossy top half with a hard edge, a cool glow at the bottom, a brighter rim
    .then(
        if (Y2K.on) Modifier.background(
            Brush.verticalGradient(0f to Color.White.copy(alpha = 0.24f * strength), 0.5f to Color.White.copy(alpha = 0.06f * strength), 0.5f to Color.Transparent, 1f to Y2K.aqua.copy(alpha = 0.12f * strength)), shape,
        ) else Modifier,
    )
    .then(if (Nebula.on) Modifier.background(Brush.verticalGradient(listOf(Nebula.violet.copy(alpha = 0.16f * strength), Color.Transparent)), shape) else Modifier)
    .then(if (Spider.on) Modifier.webCorner() else if (Sticker.on) Modifier.sparkleCorner() else Modifier)
    .border(if (Sticker.on) 2.dp else if (Nebula.on) 1.5.dp else 1.dp, if (Nebula.on) Brush.linearGradient(listOf(Nebula.cyan.copy(alpha = 0.9f), Nebula.magenta.copy(alpha = 0.55f), Nebula.cyan.copy(alpha = 0.4f))) else if (Sticker.on) Brush.linearGradient(listOf(Color.White, Color.White)) else if (Spider.on) Brush.linearGradient(listOf(Spider.red.copy(alpha = 0.75f), Color.White.copy(alpha = 0.10f), Spider.red.copy(alpha = 0.35f))) else if (Y2K.on) Brush.linearGradient(listOf(Color.White.copy(alpha = 0.8f), Y2K.aqua.copy(alpha = 0.35f), Color.White.copy(alpha = 0.5f))) else glassStroke(strength), shape)
    .specular(shape, strength)

/** The liquid edge: a thin bright line just inside the rim at the top, fading out, with a faint glow coming back at the bottom. */
fun Modifier.specular(shape: Shape, strength: Float = 1f): Modifier = this.drawWithContent {
    drawContent()
    val o = shape.createOutline(size, layoutDirection, this)
    drawOutline(
        o,
        Brush.verticalGradient(0f to Color.White.copy(alpha = 0.30f * strength), 0.45f to Color.Transparent, 1f to Color.White.copy(alpha = 0.10f * strength)),
        style = Stroke(2.dp.toPx()),
    )
}

/** Lifts a floating element (tab bar, player, menus) off the page. Put it BEFORE any clip. */
fun Modifier.lift(shape: Shape, elevation: Float = 16f): Modifier =
    this.shadow(elevation.dp, shape, clip = false, ambientColor = Color.Black.copy(alpha = 0.55f), spotColor = Color.Black.copy(alpha = 0.6f))

/** A round glass button (back, close, share). */
@Composable
fun GlassIconButton(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 44.dp, tint: Color = Milo.text) {
    Box(
        modifier.size(size).pressable(true, onClick).glass(CircleShape).semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.5f)) }
}

/** A switch in the same family: a capsule track that lights up in the accent colour with a soft glow, and a big white thumb that springs across. */
@Composable
fun MiloSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, colors: Any? = null) {
    val haptic = LocalHapticFeedback.current
    val x by animateDpAsState(if (checked) 22.dp else 0.dp, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMedium), label = "thumb")
    val track by animateColorAsState(if (checked) Milo.amber else Color(0xFF3A3733), label = "track")
    val shape = RoundedCornerShape(50)
    Box(
        modifier.width(54.dp).height(32.dp)
            .then(if (checked) Modifier.shadow(10.dp, shape, clip = false, ambientColor = Milo.amber, spotColor = Milo.amber) else Modifier)
            .clip(shape).background(track, shape)
            .border(1.dp, glassStroke(0.8f), shape)
            .toggleable(value = checked, enabled = onCheckedChange != null, role = Role.Switch) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onCheckedChange?.invoke(it) },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier.offset(x = 3.dp + x).size(26.dp)
                .shadow(4.dp, CircleShape, clip = false, ambientColor = Color.Black, spotColor = Color.Black)
                .clip(CircleShape).background(Color.White, CircleShape)
                .then(if (Y2K.on) Modifier.background(Brush.verticalGradient(listOf(Color.Transparent, Y2K.chromeDark.copy(alpha = 0.7f))), CircleShape) else Modifier),
        )
    }
}

/** A slider with a thick capsule track and a big white pill thumb. Same parameters as the Material one that it replaces. */
@Composable
fun MiloSlider(
    value: Float, onValueChange: (Float) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f, steps: Int = 0, onValueChangeFinished: (() -> Unit)? = null, colors: Any? = null,
) {
    val change by rememberUpdatedState(onValueChange)
    val done by rememberUpdatedState(onValueChangeFinished)
    val amber = Milo.amber
    val span = valueRange.endInclusive - valueRange.start
    val frac = if (span == 0f) 0f else ((value - valueRange.start) / span).coerceIn(0f, 1f)
    Canvas(
        modifier.fillMaxWidth().height(40.dp)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(value, valueRange)
                setProgress { v -> change(v.coerceIn(valueRange.start, valueRange.endInclusive)); true }
            }
            .pointerInput(enabled, valueRange, steps) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val edge = 17.dp.toPx()
                    fun set(x: Float) {
                        var f = ((x - edge) / (size.width - 2 * edge)).coerceIn(0f, 1f)
                        if (steps > 0) f = (f * (steps + 1)).roundToInt() / (steps + 1f)
                        change(valueRange.start + f * span)
                    }
                    set(down.position.x); down.consume()
                    drag(down.id) { ch -> set(ch.position.x); ch.consume() }
                    done?.invoke()
                }
            },
    ) {
        val edge = 17.dp.toPx()
        val th = 8.dp.toPx()
        val cy = size.height / 2f
        val x = edge + frac * (size.width - 2 * edge)
        drawRoundRect(Color(0x33FFFFFF), Offset(edge - 6.dp.toPx(), cy - th / 2), Size(size.width - 2 * edge + 12.dp.toPx(), th), CornerRadius(th / 2))
        if (frac > 0f) {
            if (Nebula.on) drawRoundRect(Brush.horizontalGradient(listOf(Nebula.cyan, Nebula.magenta)), Offset(edge - 6.dp.toPx(), cy - th / 2), Size(x - edge + 6.dp.toPx(), th), CornerRadius(th / 2))
            else if (Sticker.on) drawRoundRect(Color.White, Offset(edge - 6.dp.toPx(), cy - th / 2), Size(x - edge + 6.dp.toPx(), th), CornerRadius(th / 2))
            else if (Spider.on) drawRoundRect(Brush.horizontalGradient(listOf(Spider.deepRed, Spider.red)), Offset(edge - 6.dp.toPx(), cy - th / 2), Size(x - edge + 6.dp.toPx(), th), CornerRadius(th / 2))
            else if (Y2K.on) drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFF1FA8FF), amber)), Offset(edge - 6.dp.toPx(), cy - th / 2), Size(x - edge + 6.dp.toPx(), th), CornerRadius(th / 2))
            else drawRoundRect(amber, Offset(edge - 6.dp.toPx(), cy - th / 2), Size(x - edge + 6.dp.toPx(), th), CornerRadius(th / 2))
        }
        val tw = 34.dp.toPx(); val thh = 24.dp.toPx()
        if (Nebula.on) { drawCircle(Nebula.cyan.copy(alpha = 0.22f), 18.dp.toPx(), Offset(x, cy)); drawCircle(Nebula.cyan.copy(alpha = 0.5f), 12.dp.toPx(), Offset(x, cy)); drawCircle(Color.White, 7.dp.toPx(), Offset(x, cy)); return@Canvas }
        if (Sticker.on) { drawSparkle(Offset(x, cy), 16.dp.toPx(), Color.White); return@Canvas }
        if (Spider.on) { drawSpider(Offset(x, cy), 12.dp.toPx(), body = Color.Black, mark = Spider.red, phase = frac * 40f); return@Canvas }
        drawRoundRect(Color(0x55000000), Offset(x - tw / 2, cy - thh / 2 + 2.dp.toPx()), Size(tw, thh), CornerRadius(thh / 2))
        if (Y2K.on) drawRoundRect(Brush.verticalGradient(listOf(Y2K.chromeLight, Y2K.chromeDark), cy - thh / 2, cy + thh / 2), Offset(x - tw / 2, cy - thh / 2), Size(tw, thh), CornerRadius(thh / 2))
        else drawRoundRect(Color.White, Offset(x - tw / 2, cy - thh / 2), Size(tw, thh), CornerRadius(thh / 2))
    }
}

/** A glass segmented control: the selected option rides on a raised capsule that slides between positions. */
@Composable
fun GlassSegmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    BoxWithConstraints(
        modifier.fillMaxWidth().height(46.dp).clip(shape).background(Color(0x40000000), shape).border(1.dp, glassStroke(0.7f), shape),
    ) {
        val seg = maxWidth / options.size
        val x by animateDpAsState(seg * selected, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow), label = "seg")
        Box(Modifier.offset(x = x + 3.dp, y = 3.dp).width(seg - 6.dp).height(40.dp).glass(shape, 1.5f, Milo.card2))
        Box(Modifier.fillMaxWidth().fillMaxHeight()) {
            options.forEachIndexed { i, label ->
                Box(
                    Modifier.offset(x = seg * i).width(seg).fillMaxHeight().pressable(true) { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, color = if (i == selected) Milo.text else Milo.dim, fontSize = 14.sp, fontWeight = if (i == selected) FontWeight.Bold else FontWeight.SemiBold, maxLines = 1)
                }
            }
        }
    }
}
