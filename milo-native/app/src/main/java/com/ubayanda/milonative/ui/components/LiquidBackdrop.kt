package com.ubayanda.milonative.ui.components

import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * Liquid glass, the way the web "quickLiquid" does it, built for Compose:
 *  - the page behind a bar is recorded into a layer (backdropSource),
 *  - the bar draws its own slice of that layer, blurred, with the edges bent like a lens and a touch of colour fringing (Android 13+),
 *  - then a tint, a sheen and a bright rim go on top.
 * On Android 12 only the blur is used; below that (or with no backdrop) the bar falls back to the ordinary glass look.
 */

/** The recorded page that glass bars look through. [origin] is where that page starts on screen. */
class BackdropState(val layer: GraphicsLayer) {
    var origin by mutableStateOf(Offset.Zero)
}

@Composable
fun rememberBackdropState(): BackdropState {
    val l = rememberGraphicsLayer()
    return remember(l) { BackdropState(l) }
}

/** Put on the content that scrolls behind glass bars. Never on a parent of the bars themselves. */
fun Modifier.backdropSource(state: BackdropState): Modifier = this
    .onGloballyPositioned { state.origin = it.positionInRoot() }
    .drawWithContent {
        state.layer.record {
            this@drawWithContent.drawContent()
        }
        drawLayer(state.layer)
    }

/** Remembers what the effect was built for, so it is only rebuilt when the bar's size changes. */
private class FxCache {
    var w = -1f; var h = -1f; var r = -1f; var b = -1f; var refract = false
    fun apply(l: GraphicsLayer, w: Float, h: Float, r: Float, b: Float, bevel: Float, refract: Boolean) {
        if (w == this.w && h == this.h && r == this.r && b == this.b && refract == this.refract) return
        this.w = w; this.h = h; this.r = r; this.b = b; this.refract = refract
        l.clip = true
        l.renderEffect = try {
            if (Build.VERSION.SDK_INT >= 33 && refract) Agsl.effect(w, h, r, b, bevel)
            else Blur.effect(b)
        } catch (e: Throwable) { null }
    }
}

@RequiresApi(31)
private object Blur {
    fun effect(px: Float) = AndroidRenderEffect.createBlurEffect(px, px, android.graphics.Shader.TileMode.MIRROR).asComposeRenderEffect()
}

@RequiresApi(33)
private object Agsl {
    private const val SRC = """
uniform shader content;
uniform float2 size;
uniform float radius;
uniform float bevel;
half4 main(float2 p) {
    float2 c = p - size * 0.5;
    float2 q = abs(c) - (size * 0.5 - float2(radius));
    float d = length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - radius;
    float t = clamp(-d / bevel, 0.0, 1.0);
    float k = (1.0 - t) * (1.0 - t);
    float2 g;
    if (q.x > 0.0 && q.y > 0.0) { g = normalize(q) * sign(c); }
    else if (q.x > q.y) { g = float2(sign(c.x), 0.0); }
    else { g = float2(0.0, sign(c.y)); }
    float amt = bevel * 0.9 * k;
    float2 lo = float2(0.5);
    float2 hi = size - float2(0.5);
    half4 cr = content.eval(clamp(p - g * amt, lo, hi));
    half4 cg = content.eval(clamp(p - g * amt * 1.07, lo, hi));
    half4 cb = content.eval(clamp(p - g * amt * 1.14, lo, hi));
    return half4(cr.r, cg.g, cb.b, max(cr.a, max(cg.a, cb.a)));
}
"""

    fun effect(w: Float, h: Float, r: Float, blurPx: Float, bevel: Float): androidx.compose.ui.graphics.RenderEffect {
        val sh = RuntimeShader(SRC)
        sh.setFloatUniform("size", w, h)
        sh.setFloatUniform("radius", r)
        sh.setFloatUniform("bevel", bevel)
        val bend = AndroidRenderEffect.createRuntimeShaderEffect(sh, "content")
        val blur = AndroidRenderEffect.createBlurEffect(blurPx, blurPx, android.graphics.Shader.TileMode.MIRROR)
        return AndroidRenderEffect.createChainEffect(bend, blur).asComposeRenderEffect()
    }
}

/**
 * A liquid glass surface. [radius] is the corner radius of [shape] (the lens edge follows it). [tint] is laid over the blurred page.
 * Include the shadow with lift() before it, as with glass().
 */
@Composable
fun Modifier.liquidGlass(
    shape: Shape, radius: Dp, backdrop: BackdropState?, tint: Color, strength: Float = 1f,
    tintAlpha: Float = 0.42f, blur: Dp = 14.dp, refract: Boolean = true,
): Modifier {
    if (backdrop == null || Build.VERSION.SDK_INT < 31) return this.glass(shape, strength, tint.copy(alpha = 0.94f))
    val fx = rememberGraphicsLayer()
    var pos by remember { mutableStateOf(Offset.Zero) }
    val cache = remember { FxCache() }
    return this
        .onGloballyPositioned { pos = it.positionInRoot() }
        .clip(shape)
        .drawWithContent {
            val off = pos - backdrop.origin
            val bevel = minOf(size.height * 0.5f, 22.dp.toPx())
            cache.apply(fx, size.width, size.height, radius.toPx(), blur.toPx(), bevel, refract)
            fx.record {
                translate(-off.x, -off.y) { drawLayer(backdrop.layer) }
            }
            drawLayer(fx)
            drawRect(tint.copy(alpha = tintAlpha))
            drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.12f * strength), Color.White.copy(alpha = 0.02f * strength))))
            drawContent()
        }
        .border(1.dp, glassStroke(strength), shape)
        .specular(shape, strength)
}
