// Ported from Mochy Music's CoverGradientBackdrop (GPL-3.0, github.com/realidkroo/Mochy-Music): the softened cover stretched over the
// screen and warped slowly by one AGSL pass (API 33+); older phones get the same picture zoomed, still.
package com.ubayanda.milonative.ui.player

import android.graphics.Bitmap
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import com.ubayanda.milonative.ui.lyrics.asAgslPaintShader
import com.ubayanda.milonative.ui.lyrics.createAgslShader
import com.ubayanda.milonative.ui.lyrics.setAgslInputShader
import com.ubayanda.milonative.ui.lyrics.setAgslUniform

/** one warp shader for every draw: each sets its own numbers first, a song change compiles nothing */
private val warpShader: Any? by lazy {
    if (android.os.Build.VERSION.SDK_INT >= 33) createAgslShader(WarpSource) else null
}

/** how far the backdrop is zoomed past its box, so the warp's edges stay off screen */
private const val BackdropZoom = 1.6f

@Composable
fun LiquidWarpBackdrop(picture: Bitmap, animate: Boolean, modifier: Modifier = Modifier) {
    val time = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(animate) {
        if (!animate) return@LaunchedEffect
        while (true) {
            withInfiniteAnimationFrameNanos { nanos -> time.floatValue = ((nanos / 1_000_000f) * (100f / 100_000f)).mod(100f) }
        }
    }
    val shader = warpShader
    Box(modifier) {
        if (shader != null && android.os.Build.VERSION.SDK_INT >= 33) {
            Box(
                Modifier.fillMaxSize().drawWithCache {
                    val input = android.graphics.BitmapShader(picture, android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP).apply {
                        filterMode = android.graphics.BitmapShader.FILTER_MODE_LINEAR
                        // the picture stretched over the whole box
                        setLocalMatrix(android.graphics.Matrix().apply { setScale(size.width / picture.width, size.height / picture.height) })
                    }
                    onDrawBehind {
                        shader.setAgslInputShader("image", input)
                        shader.setAgslUniform("resolution", size.width, size.height)
                        shader.setAgslUniform("time", time.floatValue)
                        shader.setAgslUniform("zoom", BackdropZoom)
                        drawRect(brush = ShaderBrush(shader.asAgslPaintShader()))
                    }
                }
            )
        } else {
            Image(
                picture.asImageBitmap(), null, contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = BackdropZoom; scaleY = BackdropZoom },
            )
        }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.22f)))
    }
}

private const val WarpSource = """
uniform float2 resolution;
uniform float time;
uniform float zoom;
uniform shader image;

half4 main(float2 fragCoord) {
    float2 centre = resolution * 0.5;
    float2 p = centre + (fragCoord - centre) / zoom;
    float2 uv = p / resolution;
    float t = time * 0.5;

    float2 warpOffset = float2(
        sin(uv.y * 3.0 + t) * 0.05 + cos(uv.x * 2.0 - t * 0.6) * 0.04,
        cos(uv.x * 3.0 + t * 0.7) * 0.05 + sin(uv.y * 2.5 + t * 0.9) * 0.04
    );

    return image.eval(p + warpOffset * resolution);
}
"""
