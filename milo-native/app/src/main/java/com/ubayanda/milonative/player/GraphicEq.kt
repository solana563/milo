package com.ubayanda.milonative.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

/**
 * A ten-band graphic equaliser (32 Hz to 16 kHz, +/-12 dB) that runs inside the player's audio path, so it works on every phone and every output.
 * Each fader is a peaking filter at a fixed frequency. The parametric EQ stays available for fine work.
 */
object GraphicEq {
    val FREQS = floatArrayOf(32f, 64f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f)
    const val N = 10
    private const val Q = 1.35

    data class State(val enabled: Boolean = false, val gains: List<Float> = List(N) { 0f }, val preset: String = "Flat")

    val presets: Map<String, List<Float>> = linkedMapOf(
        "Flat" to List(N) { 0f },
        "Bass boost" to listOf(6f, 5.5f, 4.5f, 2.5f, 1f, 0f, 0f, 0f, 0f, 0f),
        "Treble boost" to listOf(0f, 0f, 0f, 0f, 0f, 1f, 2.5f, 4.5f, 5.5f, 6f),
        "Vocal" to listOf(-2f, -2f, -1f, 1f, 3f, 4f, 3f, 1f, 0f, -1f),
        "Rock" to listOf(4.5f, 3.5f, 2f, 0.5f, -1f, -0.5f, 2f, 3.5f, 4.5f, 4.5f),
        "Electronic" to listOf(5f, 4f, 1.5f, 0f, -2f, 1.5f, 1f, 2.5f, 4f, 4.5f),
        "Classical" to listOf(3f, 2.5f, 2f, 1.5f, -0.5f, -0.5f, 0f, 1.5f, 2.5f, 3f),
        "Late night" to listOf(-3f, -2f, 0f, 1f, 2f, 2f, 1f, 0f, -1f, -2f),
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()
    val version = AtomicInteger(0)
    private var prefs: android.content.SharedPreferences? = null

    fun init(c: Context) {
        if (prefs != null) return
        val p = c.applicationContext.getSharedPreferences("milo_geq", Context.MODE_PRIVATE); prefs = p
        val g = (p.getString("g", null) ?: "").split(',').mapNotNull { it.toFloatOrNull() }
        _state.value = State(p.getBoolean("on", false), if (g.size == N) g else List(N) { 0f }, p.getString("preset", "Flat") ?: "Flat")
        version.incrementAndGet()
    }

    private fun commit(s: State) {
        _state.value = s; version.incrementAndGet()
        prefs?.edit()?.putBoolean("on", s.enabled)?.putString("g", s.gains.joinToString(","))?.putString("preset", s.preset)?.apply()
    }

    fun setEnabled(on: Boolean) = commit(_state.value.copy(enabled = on))
    fun setGain(i: Int, db: Float) {
        val l = _state.value.gains.toMutableList(); if (i !in l.indices) return
        l[i] = db.coerceIn(-12f, 12f)
        val name = presets.entries.firstOrNull { e -> e.value == l }?.key ?: "Custom"
        commit(_state.value.copy(gains = l, preset = name, enabled = true))
    }
    fun applyPreset(name: String) { presets[name]?.let { commit(_state.value.copy(gains = it, preset = name, enabled = true)) } }
    fun reset() = commit(_state.value.copy(gains = List(N) { 0f }, preset = "Flat"))

    @OptIn(UnstableApi::class)
    class Processor : BaseAudioProcessor() {
        private var seen = -1
        private var rate = 44100
        private var ch = 2
        private var float = false
        private val coef = Array(N) { DoubleArray(5) }
        private var z = Array(0) { DoubleArray(0) }
        private var pre = 1.0
        private var on = false

        override fun onConfigure(f: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
            if (f.encoding != C.ENCODING_PCM_16BIT && f.encoding != C.ENCODING_PCM_FLOAT) throw AudioProcessor.UnhandledAudioFormatException(f)
            rate = f.sampleRate; ch = f.channelCount; float = f.encoding == C.ENCODING_PCM_FLOAT
            z = Array(ch) { DoubleArray(N * 2) }
            seen = -1
            return f
        }

        private fun recompute() {
            val s = _state.value
            on = s.enabled && s.gains.any { it != 0f }
            var maxBoost = 0f
            for (i in 0 until N) {
                val db = s.gains[i]
                val a = 10.0.pow(db / 40.0)
                val w = 2 * PI * FREQS[i].coerceAtMost(rate / 2f * 0.95f) / rate
                val al = sin(w) / (2 * Q)
                val b0 = 1 + al * a; val b1 = -2 * cos(w); val b2 = 1 - al * a
                val a0 = 1 + al / a; val a1 = -2 * cos(w); val a2 = 1 - al / a
                coef[i] = doubleArrayOf(b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)
                maxBoost = max(maxBoost, db)
            }
            pre = 10.0.pow(-maxBoost / 20.0)   // headroom so boosts don't clip
            seen = version.get()
        }

        override fun queueInput(input: ByteBuffer) {
            val n = input.remaining()
            if (n == 0) return
            if (seen != version.get()) recompute()
            val out = replaceOutputBuffer(n)
            if (!on) { out.put(input); out.flip(); return }
            val step = if (float) 4 else 2
            while (input.remaining() >= step * ch) {
                for (c in 0 until ch) {
                    var x = if (float) input.getFloat().toDouble() else input.getShort() / 32768.0
                    x *= pre
                    val zc = z[c]
                    for (b in 0 until N) {
                        val k = coef[b]
                        val y = k[0] * x + zc[b * 2]
                        zc[b * 2] = k[1] * x - k[3] * y + zc[b * 2 + 1]
                        zc[b * 2 + 1] = k[2] * x - k[4] * y
                        x = y
                    }
                    if (float) out.putFloat(x.toFloat()) else out.putShort((x * 32768.0).coerceIn(-32768.0, 32767.0).toInt().toShort())
                }
            }
            out.put(input)
            out.flip()
        }

        override fun onReset() { z = Array(ch) { DoubleArray(N * 2) }; seen = -1 }
    }
}
