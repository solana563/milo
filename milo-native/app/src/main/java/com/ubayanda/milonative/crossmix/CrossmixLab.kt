package com.ubayanda.milonative.crossmix

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import com.ubayanda.milonative.player.YtSource
import com.ubayanda.milonative.util.ErrorLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Crossmix stage 1 (ported idea from Mochy Music, GPL-3.0): reads songs for tempo, beats, key and energy and writes what it found to
 * Diagnostics. It changes nothing about playback. Off unless "Song analysis" is on in the Sound panel (pref "xanalyse" in "milo_fx").
 * Local and saved songs are read from the file; a YouTube song that isn't saved is streamed once to be read, which uses data.
 */
@UnstableApi
object CrossmixLab {
    private var repo: CrossmixAnalysisRepository? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val seen = HashSet<String>()

    fun enabled(ctx: Context) = ctx.getSharedPreferences("milo_fx", Context.MODE_PRIVATE).getBoolean("xanalyse", false)

    fun repo(ctx: Context): CrossmixAnalysisRepository =
        repo ?: CrossmixAnalysisRepository(ctx.applicationContext) { YtSource.dataSourceFactory(ctx.applicationContext) }.also { repo = it }

    /** reads this song (whole, as it's the one being mixed out of) and the next one (its opening), once each per session */
    fun look(ctx: Context, current: MediaItem?, next: MediaItem?) {
        if (!enabled(ctx)) return
        val r = repo(ctx)
        current?.let { run(r, it, true) }
        next?.let { run(r, it, false) }
    }

    private fun run(r: CrossmixAnalysisRepository, item: MediaItem, whole: Boolean) {
        val id = item.mediaId.ifBlank { return }
        if (!synchronized(seen) { seen.add("$id|$whole") }) return
        scope.launch {
            val a = try { r.analyze(item, whole) } catch (e: Throwable) { null }
            if (a == null) {
                ErrorLog.add("crossmix", "could not read ${item.mediaMetadata.title ?: id}: ${r.lastFailure(id) ?: "no reason"}")
            } else {
                ErrorLog.add(
                    "crossmix",
                    "${item.mediaMetadata.title ?: id} [${if (whole) "whole" else "opening"}]: bpm=${a.bpm?.let { "%.1f".format(it) }} sure=${"%.2f".format(a.bpmConfidence)} " +
                        "beats=${a.beatsMs.size} bars=${a.barBeats.size} key=${a.key} energy=${"%.2f".format(a.vibe.energy)} " +
                        "quietStart=${a.leadingSilenceEndMs}ms quietEnd=${a.trailingSilenceStartMs}ms",
                )
            }
        }
    }
}
