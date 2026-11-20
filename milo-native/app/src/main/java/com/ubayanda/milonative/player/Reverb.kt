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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.tanh

/**
 * Reverb: a room around the music. A freeverb-style reverb (eight damped combs into four allpasses a side, the right side detuned for width)
 * with a pre-delay and a low cut going in so the bass stays dry. It runs inside the player's own audio path, so it works on any output.
 * The reverb and its presets come from Mochy Music (GPL-3.0, github.com/realidkroo/Mochy-Music, eq/ReverbSettings + eq/audio/ReverbAudioProcessor,
 * itself after Jezar's public-domain freeverb); rewritten for milo's processor chain.
 */
object Reverb {
    class Preset(val name: String, val mix: Float, val roomSize: Float, val damping: Float, val wet: Float, val width: Float, val preDelayMs: Float, val lowCutHz: Float)

    val presets = listOf(
        Preset("Room", 0.5f, 0.55f, 0.45f, 0.6f, 1f, 12f, 180f),
        Preset("Studio", 0.4f, 0.3f, 0.65f, 0.45f, 0.8f, 5f, 250f),
        Preset("Bathroom", 0.5f, 0.42f, 0.05f, 0.7f, 0.6f, 2f, 120f),
        Preset("Plate", 0.45f, 0.65f, 0.1f, 0.55f, 1f, 0f, 300f),
        Preset("Hall", 0.5f, 0.78f, 0.35f, 0.6f, 1f, 25f, 150f),
        Preset("Cathedral", 0.55f, 0.95f, 0.2f, 0.7f, 1f, 40f, 120f),
        Preset("Stadium", 0.6f, 0.9f, 0.5f, 0.65f, 1f, 60f, 200f),
        Preset("Dreamy", 0.7f, 0.93f, 0.6f, 0.85f, 1f, 30f, 300f),
    )

    data class State(val enabled: Boolean = false, val preset: Int = 0, val mix: Float = presets[0].mix)

    @Volatile private var live = State()
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()
    private var prefs: android.content.SharedPreferences? = null

    fun init(c: Context) {
        if (prefs != null) return
        val p = c.applicationContext.getSharedPreferences("milo_reverb", Context.MODE_PRIVATE)
        prefs = p
        set(State(p.getBoolean("on", false), p.getInt("preset", 0).coerceIn(presets.indices), p.getFloat("mix", presets[0].mix).coerceIn(0f, 1f)), save = false)
    }

    private fun set(s: State, save: Boolean = true) {
        live = s; _state.value = s
        if (save) prefs?.edit()?.putBoolean("on", s.enabled)?.putInt("preset", s.preset)?.putFloat("mix", s.mix)?.apply()
    }

    fun setEnabled(on: Boolean) = set(live.copy(enabled = on))
    fun setPreset(i: Int) { if (i in presets.indices) set(live.copy(preset = i, mix = presets[i].mix)) }
    fun setMix(m: Float) = set(live.copy(mix = m.coerceIn(0f, 1f)))

    private val combTuning = intArrayOf(1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617)
    private val allpassTuning = intArrayOf(556, 441, 341, 225)
    private const val SPREAD = 23
    private const val FIXED_GAIN = 0.015f
    private const val MAX_PRE_MS = 150f
    private const val KNEE = 0.89f

    private class Comb(size: Int) {
        private val line = FloatArray(size.coerceAtLeast(1)); private var i = 0; private var store = 0f
        var feedback = 0.84f; var damp = 0.2f
        fun process(x: Float): Float {
            val o = line[i]
            store = o * (1f - damp) + store * damp
            line[i] = x + store * feedback
            i = if (i + 1 == line.size) 0 else i + 1
            return o
        }
        fun clear() { line.fill(0f); store = 0f }
    }

    private class Allpass(size: Int) {
        private val line = FloatArray(size.coerceAtLeast(1)); private var i = 0
        fun process(x: Float): Float {
            val b = line[i]
            line[i] = x + b * 0.5f
            i = if (i + 1 == line.size) 0 else i + 1
            return b - x
        }
        fun clear() = line.fill(0f)
    }

    /** a soft knee above about -1 dB instead of a hard clip: a full room adds level on loud songs */
    private fun soft(x: Float): Float {
        val a = abs(x)
        if (a <= KNEE) return x
        val over = (a - KNEE) / (1f - KNEE)
        return (KNEE + (1f - KNEE) * tanh(over)) * if (x < 0) -1f else 1f
    }

    @OptIn(UnstableApi::class)
    class Processor : BaseAudioProcessor() {
        private var rate = 0
        private var ch = 2
        private var float = false
        private var builtRate = 0
        private var combsL = emptyArray<Comb>(); private var combsR = emptyArray<Comb>()
        private var apL = emptyArray<Allpass>(); private var apR = emptyArray<Allpass>()
        private var pre = FloatArray(1); private var preI = 0
        private var lowState = 0f; private var lowIn = 0f
        private var amount = 0f
        private var mixNow = 0f
        private var empty = true

        override fun onConfigure(f: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
            if (f.encoding != C.ENCODING_PCM_16BIT && f.encoding != C.ENCODING_PCM_FLOAT) throw AudioProcessor.UnhandledAudioFormatException(f)
            rate = f.sampleRate; ch = f.channelCount; float = f.encoding == C.ENCODING_PCM_FLOAT
            builtRate = 0
            return f
        }

        private fun build() {
            val k = rate / 44_100f
            combsL = Array(combTuning.size) { Comb((combTuning[it] * k).roundToInt()) }
            combsR = Array(combTuning.size) { Comb(((combTuning[it] + SPREAD) * k).roundToInt()) }
            apL = Array(allpassTuning.size) { Allpass((allpassTuning[it] * k).roundToInt()) }
            apR = Array(allpassTuning.size) { Allpass(((allpassTuning[it] + SPREAD) * k).roundToInt()) }
            pre = FloatArray((rate * MAX_PRE_MS / 1000f).roundToInt() + 2); preI = 0
            lowState = 0f; lowIn = 0f; builtRate = rate; empty = true
        }

        private fun clear() {
            combsL.forEach { it.clear() }; combsR.forEach { it.clear() }
            apL.forEach { it.clear() }; apR.forEach { it.clear() }
            pre.fill(0f); lowState = 0f; lowIn = 0f; empty = true
        }

        override fun queueInput(input: ByteBuffer) {
            val n = input.remaining()
            if (n == 0) return
            val out = replaceOutputBuffer(n)
            val s = live
            val target = if (s.enabled && ch in 1..2) 1f else 0f
            if (target == 0f && amount == 0f) {
                if (!empty) clear()
                out.put(input); out.flip(); return
            }
            if (builtRate != rate) build()
            empty = false
            val p = presets[s.preset.coerceIn(presets.indices)]
            val feedback = p.roomSize * 0.28f + 0.7f
            val damp = p.damping * 0.4f
            for (c in combsL) { c.feedback = feedback; c.damp = damp }
            for (c in combsR) { c.feedback = feedback; c.damp = damp }
            val delay = (p.preDelayMs.coerceIn(0f, MAX_PRE_MS) / 1000f * rate).roundToInt().coerceIn(0, pre.size - 1)
            val low = exp(-2.0 * PI * p.lowCutHz / rate).toFloat()
            val wetLevel = p.wet * 3f
            val wet1 = wetLevel * (p.width / 2f + 0.5f)
            val wet2 = wetLevel * ((1f - p.width) / 2f)
            val toggle = 1f / (rate * 0.15f)
            val bytes = if (float) 4 else 2
            val frames = n / (bytes * ch)
            for (f in 0 until frames) {
                val inL = if (float) input.getFloat() else input.getShort() / 32768f
                val inR = if (ch == 2) (if (float) input.getFloat() else input.getShort() / 32768f) else inL
                amount = if (target > amount) minOf(target, amount + toggle) else maxOf(target, amount - toggle)
                mixNow += (s.mix - mixNow) * 0.002f
                pre[preI] = (inL + inR) * FIXED_GAIN
                var r = preI - delay; if (r < 0) r += pre.size
                preI = if (preI + 1 == pre.size) 0 else preI + 1
                val d = pre[r]
                lowState = low * (lowState + d - lowIn); lowIn = d
                val x = lowState + 1e-18f
                var aL = 0f; var aR = 0f
                for (c in combsL) aL += c.process(x)
                for (a in apL) aL = a.process(aL)
                if (ch == 2) { for (c in combsR) aR += c.process(x); for (a in apR) aR = a.process(aR) }
                val g = amount * mixNow
                if (ch == 2) {
                    val rl = aL * wet1 + aR * wet2 + inL
                    val rr = aR * wet1 + aL * wet2 + inR
                    put(out, soft(inL + (rl - inL) * g)); put(out, soft(inR + (rr - inR) * g))
                } else {
                    put(out, soft(inL + (aL * (wet1 + wet2) + inL - inL) * g))
                }
            }
            out.put(input)   // a partial frame at the end passes through
            out.flip()
        }

        private fun put(out: ByteBuffer, v: Float) {
            if (float) out.putFloat(v) else out.putShort((v * 32767f).roundToInt().coerceIn(-32768, 32767).toShort())
        }
    }
}
