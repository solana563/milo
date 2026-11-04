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
import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

/**
 * A real parametric equaliser: up to five peaking filters, each with its own frequency, gain and Q.
 * It runs inside the player's audio path (before the speaker, Bluetooth or DAC), so it works on any phone and any output.
 * The curve is remembered per output device, so your headphones and your speaker can each have their own.
 */
object ParamEq {
    const val BANDS = 5
    data class Band(val hz: Float, val db: Float, val q: Float)
    data class State(val enabled: Boolean = false, val bands: List<Band> = defaults(), val device: String = "default")

    fun defaults() = listOf(Band(60f, 0f, 1f), Band(250f, 0f, 1f), Band(1000f, 0f, 1f), Band(4000f, 0f, 1f), Band(12000f, 0f, 1f))

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()
    val version = AtomicInteger(0)
    private var prefs: android.content.SharedPreferences? = null

    fun init(c: Context) {
        if (prefs != null) return
        prefs = c.applicationContext.getSharedPreferences("milo_peq", Context.MODE_PRIVATE)
        useDevice("default")
    }

    private fun key(d: String) = "dev:$d"

    private fun load(d: String): State? {
        val s = prefs?.getString(key(d), null) ?: return null
        return try {
            val o = JSONObject(s)
            val a = o.getJSONArray("b")
            State(o.optBoolean("on"), (0 until a.length()).map { val x = a.getJSONObject(it); Band(x.getDouble("f").toFloat(), x.getDouble("g").toFloat(), x.getDouble("q").toFloat()) }.take(BANDS), d)
        } catch (_: Exception) { null }
    }

    private fun persist() {
        val s = _state.value
        val a = JSONArray(); s.bands.forEach { a.put(JSONObject().put("f", it.hz.toDouble()).put("g", it.db.toDouble()).put("q", it.q.toDouble())) }
        prefs?.edit()?.putString(key(s.device), JSONObject().put("on", s.enabled).put("b", a).toString())?.apply()
    }

    /** Switch to the curve saved for this output (keeps the current curve if there is none yet). */
    fun useDevice(name: String) {
        val d = name.ifBlank { "default" }
        if (_state.value.device == d && prefs?.contains(key(d)) == true) return
        val saved = load(d)
        _state.value = saved ?: _state.value.copy(device = d)
        version.incrementAndGet()
    }

    fun setEnabled(on: Boolean) { _state.value = _state.value.copy(enabled = on); persist(); version.incrementAndGet() }
    fun setBand(i: Int, b: Band) {
        val l = _state.value.bands.toMutableList(); if (i !in l.indices) return
        l[i] = b; _state.value = _state.value.copy(bands = l); persist(); version.incrementAndGet()
    }
    fun reset() { _state.value = _state.value.copy(bands = defaults()); persist(); version.incrementAndGet() }

    /** One filter step per band, all channels. Created once per player (each keeps its own memory of past samples). */
    @OptIn(UnstableApi::class)
    class Processor : BaseAudioProcessor() {
        private var seen = -1
        private var rate = 44100
        private var ch = 2
        private var float = false
        private var coef = Array(BANDS) { DoubleArray(5) }
        private var z = Array(0) { DoubleArray(0) }
        private var pre = 1.0
        private var on = false

        override fun onConfigure(f: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
            if (f.encoding != C.ENCODING_PCM_16BIT && f.encoding != C.ENCODING_PCM_FLOAT) throw AudioProcessor.UnhandledAudioFormatException(f)
            rate = f.sampleRate; ch = f.channelCount; float = f.encoding == C.ENCODING_PCM_FLOAT
            z = Array(ch) { DoubleArray(BANDS * 2) }
            seen = -1
            return f
        }

        private fun recompute() {
            val s = _state.value
            on = s.enabled && s.bands.any { it.db != 0f }
            var maxBoost = 0f
            s.bands.forEachIndexed { i, b ->
                val a = 10.0.pow(b.db / 40.0)
                val w = 2 * PI * b.hz.coerceIn(20f, (rate / 2f) * 0.95f) / rate
                val al = sin(w) / (2 * b.q.coerceIn(0.2f, 8f))
                val b0 = 1 + al * a; val b1 = -2 * cos(w); val b2 = 1 - al * a
                val a0 = 1 + al / a; val a1 = -2 * cos(w); val a2 = 1 - al / a
                coef[i] = doubleArrayOf(b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)
                maxBoost = max(maxBoost, b.db)
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
                    for (b in 0 until BANDS) {
                        val k = coef[b]
                        val y = k[0] * x + zc[b * 2]
                        zc[b * 2] = k[1] * x - k[3] * y + zc[b * 2 + 1]
                        zc[b * 2 + 1] = k[2] * x - k[4] * y
                        x = y
                    }
                    if (float) out.putFloat(x.toFloat()) else out.putShort((x * 32768.0).coerceIn(-32768.0, 32767.0).toInt().toShort())
                }
            }
            out.put(input)   // any partial frame at the end passes through
            out.flip()
        }

        override fun onReset() { z = Array(ch) { DoubleArray(BANDS * 2) }; seen = -1 }
    }
}
