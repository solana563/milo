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
        if (com.ubayanda.milonative.BuildConfig.LITE) return
        val app = ctx.applicationContext
        val item = p.currentMediaItem
        val title = item?.mediaMetadata?.title?.toString().orEmpty()
        val artist = item?.mediaMetadata?.artist?.toString().orEmpty()
        val playing = p.playWhenReady && p.playbackState != Player.STATE_ENDED && p.playbackState != Player.STATE_IDLE
        val pos = p.currentPosition.coerceAtLeast(0L)
        val dur = if (p.duration > 0) p.duration else 0L
        val id = item?.mediaId.orEmpty()
        val shuffle = p.shuffleModeEnabled
        val media = item?.localConfiguration?.uri
        val artwork = item?.mediaMetadata?.artworkUri
        scope.launch {
            try {
                if (id != lastId) { lastId = id; if (id.isEmpty()) WidgetState.clear(app) else WidgetState.saveArt(app, media, artwork) }
                WidgetState.write(app, title, artist, playing, pos, dur, id, shuffle)
                refresh(app)
            } catch (_: Throwable) {}
        }
    }

    /** The player service just started: the widgets' buttons can talk to it now. */
    fun serviceUp(ctx: Context) {
        if (com.ubayanda.milonative.BuildConfig.LITE) return
        val app = ctx.applicationContext
        scope.launch { try { WidgetState.setAlive(app, true); refresh(app) } catch (_: Throwable) {} }
    }

    /** The Liked songs list changed (in the app or from a widget): redraw the heart. */
    fun pokeLiked(ctx: Context) {
        if (com.ubayanda.milonative.BuildConfig.LITE) return
        val app = ctx.applicationContext
        scope.launch { try { app.getSharedPreferences("milo_widget", Context.MODE_PRIVATE).edit().putBoolean("liked", WidgetState.isLiked(app, WidgetState.currentId(app))).apply(); refresh(app) } catch (_: Throwable) {} }
    }

    fun clear(ctx: Context) {
        if (com.ubayanda.milonative.BuildConfig.LITE) return
        val app = ctx.applicationContext
        scope.launch { try { lastId = ""; WidgetState.clear(app); refresh(app) } catch (_: Throwable) {} }
    }

    private suspend fun refresh(app: Context) {
        PlayerWidget().updateAll(app)
        PillWidget().updateAll(app)
        ArtWidget().updateAll(app)
    }
}
