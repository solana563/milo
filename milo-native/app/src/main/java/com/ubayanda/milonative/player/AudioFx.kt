package com.ubayanda.milonative.player

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FxState(
    val available: Boolean = false,
    val freqsHz: List<Int> = emptyList(),
    val levelsDb: List<Float> = emptyList(),
    val minDb: Float = -12f,
    val maxDb: Float = 12f,
    val bass: Int = 0,
    val spatial: Int = 0,
    val preset: String = "Flat",
)

/**
 * Sound controls (equaliser, bass boost, spatial) using Android's own audio effects on the player's audio session.
 * The player service and the screens run in the same process, so they share this object.
 */
object AudioFx {
    private var eq: Equalizer? = null
    private var bassFx: BassBoost? = null
    private var virt: Virtualizer? = null
    private var prefs: android.content.SharedPreferences? = null

    private val _state = MutableStateFlow(FxState())
    val state: StateFlow<FxState> = _state.asStateFlow()

    /** Preset curves, described over the low to high range (0..1) so they fit any number of bands. dB values. */
    val presets: Map<String, (Float) -> Float> = linkedMapOf(
        "Flat" to { _: Float -> 0f },
        "Bass boost" to { x: Float -> 7f * (1f - x).let { it * it } },
        "Treble boost" to { x: Float -> 6f * x * x },
        "Vocal" to { x: Float -> if (x in 0.3f..0.8f) 4f else -1f },
        "Rock" to { x: Float -> if (x < 0.25f || x > 0.75f) 4f else -1f },
        "Pop" to { x: Float -> if (x in 0.35f..0.7f) 3f else if (x < 0.2f) 2f else 0f },
        "Electronic" to { x: Float -> if (x < 0.2f || x > 0.8f) 5f else -1f },
        "Acoustic" to { x: Float -> if (x < 0.3f) 3f else if (x > 0.75f) 2f else 0f },
    )

    private var ctx: Context? = null
    var sessionId = 0
        private set

    /**
     * Remember the player's audio session. The effects themselves are only created once the person
     * opens the Sound sheet (or already used it before), because on some phones the audio-effect
     * drivers can crash the whole app while music is starting.
     */
    fun attach(context: Context, sessionId: Int) {
        release()
        ctx = context.applicationContext
        this.sessionId = sessionId
        prefs = context.getSharedPreferences("milo_fx", Context.MODE_PRIVATE)
        if (prefs?.getBoolean("used", false) == true) ensure()
    }

    /** Create the effects if they do not exist yet. Called when the Sound sheet opens. */
    fun ensure() {
        if (eq != null || sessionId == 0 || ctx == null) return
        try {
            val e = Equalizer(0, sessionId)
            e.enabled = true
            eq = e
            val range = e.bandLevelRange
            val n = e.numberOfBands.toInt()
            val freqs = (0 until n).map { e.getCenterFreq(it.toShort()) / 1000 }
            val saved = prefs?.getString("levels", null)?.split(",")?.mapNotNull { it.toFloatOrNull() }
            val levels = if (saved != null && saved.size == n) saved else List(n) { 0f }
            _state.value = FxState(
                available = true, freqsHz = freqs, levelsDb = levels,
                minDb = range[0] / 100f, maxDb = range[1] / 100f,
                bass = prefs?.getInt("bass", 0) ?: 0, spatial = prefs?.getInt("spatial", 0) ?: 0,
                preset = prefs?.getString("preset", "Flat") ?: "Flat",
            )
            levels.forEachIndexed { i, db -> e.setBandLevel(i.toShort(), (db * 100).toInt().toShort()) }
        } catch (t: Throwable) {
            eq = null
            _state.value = FxState(available = false)
            return
        }
        try {
            bassFx = BassBoost(0, sessionId).also { it.enabled = true; if (it.strengthSupported) it.setStrength(_state.value.bass.toShort()) }
        } catch (t: Throwable) { bassFx = null }
        try {
            virt = Virtualizer(0, sessionId).also { it.enabled = true; if (it.strengthSupported) it.setStrength(_state.value.spatial.toShort()) }
        } catch (t: Throwable) { virt = null }
    }

    fun release() {
        try { eq?.release() } catch (_: Throwable) {}
        try { bassFx?.release() } catch (_: Throwable) {}
        try { virt?.release() } catch (_: Throwable) {}
        eq = null; bassFx = null; virt = null
    }

    private fun persist() {
        val s = _state.value
        prefs?.edit()?.putBoolean("used", true)?.putString("levels", s.levelsDb.joinToString(","))?.putInt("bass", s.bass)?.putInt("spatial", s.spatial)?.putString("preset", s.preset)?.apply()
    }

    fun setBand(i: Int, db: Float) {
        val s = _state.value
        if (i !in s.levelsDb.indices) return
        val lv = s.levelsDb.toMutableList()
        lv[i] = db.coerceIn(s.minDb, s.maxDb)
        try { eq?.setBandLevel(i.toShort(), (lv[i] * 100).toInt().toShort()) } catch (_: Throwable) {}
        _state.value = s.copy(levelsDb = lv, preset = "Custom")
        persist()
    }

    fun applyPreset(name: String) {
        val curve = presets[name] ?: return
        val s = _state.value
        val n = s.levelsDb.size
        if (n == 0) return
        val lv = List(n) { i -> curve(if (n == 1) 0f else i.toFloat() / (n - 1)).coerceIn(s.minDb, s.maxDb) }
        lv.forEachIndexed { i, db -> try { eq?.setBandLevel(i.toShort(), (db * 100).toInt().toShort()) } catch (_: Throwable) {} }
        _state.value = s.copy(levelsDb = lv, preset = name)
        persist()
    }

    /** Apply a preset a friend sent: any number of bands is stretched to this phone's bands. */
    fun applyCustom(levels: List<Float>, bass: Int, spatial: Int, name: String) {
        ensure()
        val s = _state.value
        val n = s.levelsDb.size
        if (n == 0 || levels.isEmpty()) return
        val lv = List(n) { i ->
            val x = if (n == 1) 0f else i.toFloat() / (n - 1) * (levels.size - 1)
            val a = x.toInt().coerceAtMost(levels.size - 1)
            val b = (a + 1).coerceAtMost(levels.size - 1)
            (levels[a] + (levels[b] - levels[a]) * (x - a)).coerceIn(s.minDb, s.maxDb)
        }
        lv.forEachIndexed { i, db -> try { eq?.setBandLevel(i.toShort(), (db * 100).toInt().toShort()) } catch (_: Throwable) {} }
        val bs = bass.coerceIn(0, 1000); val sp = spatial.coerceIn(0, 1000)
        try { bassFx?.let { if (it.strengthSupported) it.setStrength(bs.toShort()) } } catch (_: Throwable) {}
        try { virt?.let { if (it.strengthSupported) it.setStrength(sp.toShort()) } } catch (_: Throwable) {}
        _state.value = s.copy(levelsDb = lv, preset = name.take(24), bass = bs, spatial = sp)
        persist()
    }

    fun setBass(v: Int) {
        try { bassFx?.let { if (it.strengthSupported) it.setStrength(v.toShort()) } } catch (_: Throwable) {}
        _state.value = _state.value.copy(bass = v)
        persist()
    }

    fun setSpatial(v: Int) {
        try { virt?.let { if (it.strengthSupported) it.setStrength(v.toShort()) } } catch (_: Throwable) {}
        _state.value = _state.value.copy(spatial = v)
        persist()
    }
}
