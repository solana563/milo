package com.ubayanda.milonative.player

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Live spectrum of what is playing. A small processor sits at the end of the player's own audio chain and copies the samples it passes
 * (it never changes the sound), so this needs no microphone permission, works on every phone and every output, and sees exactly what the
 * equaliser produced. [Analyzer] turns the latest samples into bars for the screen.
 */
object Spectrum {
    const val BARS = 40
    private const val RING = 1 shl 15
    private const val FFT = 4096

    private val taps = CopyOnWriteArrayList<Tap>()

    @OptIn(UnstableApi::class)
    class Tap : BaseAudioProcessor() {
        private val ring = FloatArray(RING)
        @Volatile var written = 0L
        @Volatile var lastAt = 0L
        @Volatile var rate = 44100
        private var ch = 2
        private var float = false

        init { taps.add(this) }

        override fun onConfigure(f: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
            if (f.encoding != C.ENCODING_PCM_16BIT && f.encoding != C.ENCODING_PCM_FLOAT) throw AudioProcessor.UnhandledAudioFormatException(f)
            ch = f.channelCount.coerceAtLeast(1); float = f.encoding == C.ENCODING_PCM_FLOAT; rate = f.sampleRate
            return f
        }

        override fun queueInput(input: ByteBuffer) {
            val n = input.remaining()
            if (n == 0) return
            val view = input.duplicate().order(input.order())
            val step = if (float) 4 else 2
            var w = written
            while (view.remaining() >= step * ch) {
                var s = 0f
                for (c in 0 until ch) s += if (float) view.getFloat() else view.getShort() / 32768f
                ring[(w and (RING - 1).toLong()).toInt()] = s / ch
                w++
            }
            written = w
            lastAt = android.os.SystemClock.elapsedRealtime()
            val out = replaceOutputBuffer(n)
            out.put(input)
            out.flip()
        }

        /** The [len] samples that ended [delay] samples ago, into [dst] (zeros before the start). */
        fun snapshot(dst: DoubleArray, len: Int, delay: Int) {
            val end = written - delay
            for (i in 0 until len) {
                val idx = end - len + i
                dst[i] = if (idx < 0 || written - idx > RING - 8) 0.0 else ring[(idx and (RING - 1).toLong()).toInt()].toDouble()
            }
        }
    }

    /** Computes smoothed bar heights (0..1) and falling peak marks. One per screen; call [update] once per frame. */
    class Analyzer {
        val bars = FloatArray(BARS)
        val peaks = FloatArray(BARS)
        private val re = DoubleArray(FFT)
        private val im = DoubleArray(FFT)
        private val tmp = DoubleArray(FFT)
        private val spec = DoubleArray(FFT / 2)
        private val window = DoubleArray(FFT) { 0.5 - 0.5 * cos(2 * PI * it / (FFT - 1)) }
        private val target = FloatArray(BARS)
        private val cosT = DoubleArray(FFT / 2) { cos(-2 * PI * it / FFT) }
        private val sinT = DoubleArray(FFT / 2) { sin(-2 * PI * it / FFT) }
        private val rev = IntArray(FFT).also { r -> val bits = 12; for (i in 0 until FFT) { var x = i; var y = 0; for (b in 0 until bits) { y = (y shl 1) or (x and 1); x = x shr 1 }; r[i] = y } }

        fun update() {
            val now = android.os.SystemClock.elapsedRealtime()
            java.util.Arrays.fill(spec, 0.0)
            var rate = 44100
            var live = false
            for (t in taps) {
                if (now - t.lastAt > 300) continue
                live = true; rate = t.rate
                t.snapshot(tmp, FFT, (rate * 0.09).toInt())    // the sound a moment ago, to line up with what the ears hear
                for (i in 0 until FFT) { re[rev[i]] = tmp[i] * window[i] }
                java.util.Arrays.fill(im, 0.0)
                fft()
                for (k in 0 until FFT / 2) spec[k] += (re[k] * re[k] + im[k] * im[k])
            }
            if (live) {
                val binHz = rate.toDouble() / FFT
                val lo0 = 30.0; val hi0 = min(16000.0, rate / 2 * 0.95)
                for (b in 0 until BARS) {
                    val f1 = lo0 * (hi0 / lo0).pow(b.toDouble() / BARS)
                    val f2 = lo0 * (hi0 / lo0).pow((b + 1).toDouble() / BARS)
                    val k1 = (f1 / binHz).toInt().coerceAtLeast(1)
                    val k2 = (f2 / binHz).toInt().coerceAtLeast(k1 + 1).coerceAtMost(FFT / 2 - 1)
                    var s = 0.0; var c = 0
                    for (k in k1 until k2) { s += spec[k]; c++ }
                    val p = if (c > 0) s / c else spec[k1]
                    val amp = sqrt(p) / (FFT / 4.0)
                    val fc = sqrt(f1 * f2)
                    val db = 20 * log10(amp + 1e-9) + 3.5 * ln(fc / 1000.0) / ln(2.0)   // tilt: music falls off with pitch, the eye shouldn't
                    target[b] = ((db + 80.0) / 58.0).coerceIn(0.0, 1.0).toFloat()
                }
            } else java.util.Arrays.fill(target, 0f)
            for (b in 0 until BARS) {
                val tg = target[b]
                bars[b] += (tg - bars[b]) * (if (tg > bars[b]) 0.6f else 0.13f)
                peaks[b] = max(bars[b], peaks[b] - 0.012f)
            }
        }

        private fun fft() {
            var size = 2
            while (size <= FFT) {
                val half = size / 2; val stride = FFT / size
                var i = 0
                while (i < FFT) {
                    var k = 0
                    for (j in i until i + half) {
                        val c = cosT[k]; val s = sinT[k]
                        val tr = re[j + half] * c - im[j + half] * s
                        val ti = re[j + half] * s + im[j + half] * c
                        re[j + half] = re[j] - tr; im[j + half] = im[j] - ti
                        re[j] += tr; im[j] += ti
                        k += stride
                    }
                    i += size
                }
                size *= 2
            }
        }
    }
}
