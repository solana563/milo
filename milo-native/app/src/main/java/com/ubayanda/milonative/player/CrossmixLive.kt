package com.ubayanda.milonative.player

import android.content.SharedPreferences
import com.ubayanda.milonative.crossmix.CrossmixLength
import com.ubayanda.milonative.crossmix.CrossmixSettings
import com.ubayanda.milonative.crossmix.CrossmixStyle
import com.ubayanda.milonative.crossmix.CrossmixTempoMatch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

/** What the Crossmix engine is doing, and the choices that steer it (kept in the "milo_fx" prefs; the service watches them). */
object CrossmixLive {
    const val ENGINE = "xengine"        // "classic" (milo's own crossfade) or "crossmix" (Mochy's engine)
    const val LENGTH = "xlen"           // CrossmixLength ordinal
    const val TEMPO = "xtempo"          // CrossmixTempoMatch ordinal
    const val STYLE = "xstyle"          // CrossmixStyle ordinal, -1 = the one that fits the pair
    const val STUNTS = "xstunts"
    const val ALWAYS = "xalways"
    const val LATENCY = "xstartlat"
    val keys = setOf(ENGINE, LENGTH, TEMPO, STYLE, STUNTS, ALWAYS)

    /** what Crossmix is doing now, in a line */
    val status = MutableStateFlow("Crossmix is waiting for a song")
    /** two songs are playing together right now */
    val mixing = MutableStateFlow(false)
    /** the settings screen's "Try a mix": jump to just before the next hand-over */
    val test = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    fun on(p: SharedPreferences) = p.getString(ENGINE, "classic") == "crossmix"

    fun settings(p: SharedPreferences): CrossmixSettings {
        val lengths = CrossmixLength.values()
        val tempos = CrossmixTempoMatch.values()
        val styles = CrossmixStyle.values()
        val style = p.getInt(STYLE, -1)
        return CrossmixSettings(
            length = lengths.getOrElse(p.getInt(LENGTH, CrossmixLength.MEDIUM.ordinal)) { CrossmixLength.MEDIUM },
            tempoMatch = tempos.getOrElse(p.getInt(TEMPO, CrossmixTempoMatch.FULL.ordinal)) { CrossmixTempoMatch.FULL },
            style = styles.getOrNull(style),
            stunts = p.getBoolean(STUNTS, true),
            alwaysMix = p.getBoolean(ALWAYS, true),
        )
    }
}
