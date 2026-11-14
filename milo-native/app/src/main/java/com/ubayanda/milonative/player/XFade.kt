package com.ubayanda.milonative.player

/**
 * What the screens should show while a crossfade is running: the song that is coming in (title, cover, queue position, progress),
 * not the one that is still fading out on the main player. The service sets it from the start of the fade; it is cleared when the fade is over.
 */
object XFadeLive {
    data class Info(val index: Int, val posMs: Long, val durMs: Long)
    val info = kotlinx.coroutines.flow.MutableStateFlow<Info?>(null)
}

/** Crossfade curves. [gains] returns (old song, new song) volume at progress f (0..1). */
object XFade {
    const val EQUAL_POWER = 0
    const val LINEAR = 1
    const val SMOOTH = 2
    val names = listOf("Equal power", "Linear", "Smooth")

    fun gains(f: Float, curve: Int): Pair<Float, Float> {
        val x = f.coerceIn(0f, 1f)
        return when (curve) {
            LINEAR -> (1f - x) to x
            SMOOTH -> { val s = x * x * (3f - 2f * x); Math.cos(s * Math.PI / 2).toFloat() to Math.sin(s * Math.PI / 2).toFloat() }
            else -> Math.cos(x * Math.PI / 2).toFloat() to Math.sin(x * Math.PI / 2).toFloat()
        }
    }
}
