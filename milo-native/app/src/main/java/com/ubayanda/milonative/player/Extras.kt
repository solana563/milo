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

/** Small listening extras: skip silence, mono, left/right balance, and an A-B loop for the current song. */
object Extras {
    data class State(val skipSilence: Boolean = false, val mono: Boolean = false, val balance: Float = 0f)
    data class Loop(val a: Long = -1L, val b: Long = -1L)

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()
    private val _loop = MutableStateFlow(Loop())
    val loop: StateFlow<Loop> = _loop.asStateFlow()
    val version = AtomicInteger()
    private var prefs: android.content.SharedPreferences? = null

    fun init(ctx: Context) {
        val p = ctx.getSharedPreferences("milo_extras", Context.MODE_PRIVATE); prefs = p
        _state.value = State(p.getBoolean("skip", false), p.getBoolean("mono", false), p.getFloat("bal", 0f))
        version.incrementAndGet()
    }

    fun set(s: State) {
        _state.value = s; version.incrementAndGet()
        prefs?.edit()?.putBoolean("skip", s.skipSilence)?.putBoolean("mono", s.mono)?.putFloat("bal", s.balance)?.apply()
    }

    /** Tap once to mark A, again to mark B (the song then repeats between them), a third time to clear. */
    fun markLoop(posMs: Long) {
        val l = _loop.value
        _loop.value = when {
            l.a < 0 -> Loop(posMs, -1L)
            l.b < 0 && posMs > l.a + 500 -> Loop(l.a, posMs)
            l.b < 0 -> Loop(posMs, -1L)
            else -> Loop()
        }
    }
    fun clearLoop() { if (_loop.value.a >= 0) _loop.value = Loop() }

    /** Called often by the player service: jump back to A when playback passes B. */
    fun loopCheck(p: androidx.media3.exoplayer.ExoPlayer) {
        val l = _loop.value
        if (l.a >= 0 && l.b > l.a && p.isPlaying && p.currentPosition >= l.b) p.seekTo(l.a)
    }

    @OptIn(UnstableApi::class)
    class Processor : BaseAudioProcessor() {
        private var ch = 2
        private var float = false
        private var seen = -1
        private var mono = false
        private var gl = 1f
        private var gr = 1f

        override fun onConfigure(f: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
            if (f.encoding != C.ENCODING_PCM_16BIT && f.encoding != C.ENCODING_PCM_FLOAT) throw AudioProcessor.UnhandledAudioFormatException(f)
            ch = f.channelCount; float = f.encoding == C.ENCODING_PCM_FLOAT; seen = -1
            return f
        }

        override fun queueInput(input: ByteBuffer) {
            val n = input.remaining()
            if (n == 0) return
            if (seen != version.get()) {
                val s = _state.value
                mono = s.mono
                gl = if (s.balance > 0f) 1f - s.balance else 1f
                gr = if (s.balance < 0f) 1f + s.balance else 1f
                seen = version.get()
            }
            val out = replaceOutputBuffer(n)
            if (ch != 2 || (!mono && gl == 1f && gr == 1f)) { out.put(input); out.flip(); return }
            val step = if (float) 4 else 2
            while (input.remaining() >= step * 2) {
                var l: Float; var r: Float
                if (float) { l = input.getFloat(); r = input.getFloat() } else { l = input.getShort() / 32768f; r = input.getShort() / 32768f }
                if (mono) { val m = (l + r) / 2f; l = m; r = m }
                l *= gl; r *= gr
                if (float) { out.putFloat(l); out.putFloat(r) }
                else {
                    out.putShort((l * 32768f).coerceIn(-32768f, 32767f).toInt().toShort())
                    out.putShort((r * 32768f).coerceIn(-32768f, 32767f).toInt().toShort())
                }
            }
            out.put(input)
            out.flip()
        }
    }
}
