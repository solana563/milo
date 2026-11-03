package com.ubayanda.milonative.player

import android.media.audiofx.Visualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

/**
 * Live spectrum of what is playing, in [BARS] smoothed bands (0..1). Uses Android's Visualizer on the player's audio session,
 * which needs the microphone permission the app already asks for (Android treats the Visualizer like a recording).
 */
object Spectrum {
    const val BARS = 32
    private val _bars = MutableStateFlow(FloatArray(BARS))
    val bars: StateFlow<FloatArray> = _bars.asStateFlow()
    private var vis: Visualizer? = null
    private var session = 0
    private val smooth = FloatArray(BARS)

    /** Starts (or restarts on a new session). Returns false when it can't run, e.g. no permission. */
    fun start(sessionId: Int): Boolean {
        if (sessionId == 0) return false
        if (vis != null && session == sessionId) return true
        stop()
        return try {
            val v = Visualizer(sessionId)
            v.captureSize = Visualizer.getCaptureSizeRange()[1]
            v.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(visualizer: Visualizer?, waveform: ByteArray?, samplingRate: Int) {}
                override fun onFftDataCapture(visualizer: Visualizer?, fft: ByteArray?, samplingRate: Int) { if (fft != null) update(fft) }
            }, Visualizer.getMaxCaptureRate() / 2, false, true)
            v.enabled = true
            vis = v; session = sessionId
            true
        } catch (_: Throwable) { vis = null; false }
    }

    fun stop() {
        try { vis?.enabled = false; vis?.release() } catch (_: Throwable) {}
        vis = null; session = 0
        _bars.value = FloatArray(BARS)
    }

    private fun update(fft: ByteArray) {
        val n = fft.size / 2
        val out = FloatArray(BARS)
        // log-spaced bands from ~40 Hz upward so bass and treble both move
        for (b in 0 until BARS) {
            val lo = max(1, (Math.pow(n.toDouble(), b.toDouble() / BARS) ).toInt())
            val hi = max(lo + 1, (Math.pow(n.toDouble(), (b + 1).toDouble() / BARS)).toInt())
            var m = 0f
            for (k in lo until min(hi, n)) m = max(m, hypot(fft[2 * k].toFloat(), fft[2 * k + 1].toFloat()))
            val v = (ln(1f + m) / ln(1f + 180f)).coerceIn(0f, 1f)
            out[b] = v
            smooth[b] = if (v > smooth[b]) v else smooth[b] * 0.86f + v * 0.14f   // quick rise, slow fall
            out[b] = smooth[b]
        }
        _bars.value = out
    }
}
