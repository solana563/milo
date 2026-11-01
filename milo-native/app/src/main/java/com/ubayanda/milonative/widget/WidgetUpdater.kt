package com.ubayanda.milonative.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Called by the playback service whenever the song or play state changes. */
object WidgetUpdater {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastId = ""

    fun push(ctx: Context, p: Player) {
        val app = ctx.applicationContext
        val item = p.currentMediaItem
        val title = item?.mediaMetadata?.title?.toString().orEmpty()
        val artist = item?.mediaMetadata?.artist?.toString().orEmpty()
        val playing = p.playWhenReady && p.playbackState != Player.STATE_ENDED && p.playbackState != Player.STATE_IDLE
        val pos = p.currentPosition.coerceAtLeast(0L)
        val dur = if (p.duration > 0) p.duration else 0L
        val id = item?.mediaId.orEmpty()
        val media = item?.localConfiguration?.uri
        val artwork = item?.mediaMetadata?.artworkUri
        scope.launch {
            try {
                if (id != lastId) { lastId = id; if (id.isEmpty()) WidgetState.clear(app) else WidgetState.saveArt(app, media, artwork) }
                WidgetState.write(app, title, artist, playing, pos, dur)
                refresh(app)
            } catch (_: Throwable) {}
        }
    }

    fun clear(ctx: Context) {
        val app = ctx.applicationContext
        scope.launch { try { lastId = ""; WidgetState.clear(app); refresh(app) } catch (_: Throwable) {} }
    }

    private suspend fun refresh(app: Context) {
        PlayerWidget().updateAll(app)
        PillWidget().updateAll(app)
        ArtWidget().updateAll(app)
    }
}
