package com.ubayanda.milonative.player

import android.content.Context
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.SessionAvailabilityListener
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.google.android.gms.cast.framework.CastContext
import java.util.concurrent.Executors

/**
 * Hands the queue to a Cast device (Chromecast, Google TV, Nest speaker...) when one is connected, and back to the phone when it ends.
 * Songs that are files on the phone are served over Wi-Fi by [LocalAudioServer]. Everything here is guarded, so a phone without
 * Cast support just never offers it.
 */
class CastBridge(private val ctx: Context, private val session: () -> MediaSession?, private val local: ExoPlayer) {
    private var cast: CastPlayer? = null
    private val server = LocalAudioServer(ctx)
    private val originals = HashMap<String, MediaItem>()

    // what the Cast device was playing, kept so the queue can come back to the phone after the session has gone
    private var snapItems: List<MediaItem> = emptyList()
    private var snapIndex = 0
    private var snapPos = 0L

    fun init() {
        try {
            CastContext.getSharedInstance(ctx, Executors.newSingleThreadExecutor()).addOnSuccessListener { cc ->
                try {
                    val cp = CastPlayer(cc)
                    cast = cp
                    cp.addListener(object : Player.Listener {
                        override fun onEvents(player: Player, events: Player.Events) {
                            if (player.mediaItemCount > 0) {
                                snapItems = (0 until player.mediaItemCount).map { player.getMediaItemAt(it) }
                                snapIndex = player.currentMediaItemIndex.coerceAtLeast(0)
                                snapPos = player.currentPosition.coerceAtLeast(0L)
                            }
                        }
                    })
                    cp.setSessionAvailabilityListener(object : SessionAvailabilityListener {
                        override fun onCastSessionAvailable() { toCast(cp) }
                        override fun onCastSessionUnavailable() { toLocal() }
                    })
                    if (cp.isCastSessionAvailable) toCast(cp)
                } catch (_: Throwable) {}
            }
        } catch (_: Throwable) {}
    }

    fun release() {
        try { cast?.setSessionAvailabilityListener(null); cast?.release() } catch (_: Throwable) {}
        server.stop()
    }

    private fun castable(item: MediaItem): MediaItem {
        val cfg = item.localConfiguration ?: return item
        originals[item.mediaId] = item
        val scheme = cfg.uri.scheme
        if (scheme == "http" || scheme == "https") return item.buildUpon().setMimeType(cfg.mimeType ?: "audio/mpeg").build()
        val url = server.urlFor(item.mediaId, cfg.uri) ?: return item
        val mime = ctx.contentResolver.getType(cfg.uri) ?: "audio/mpeg"
        return item.buildUpon().setUri(url).setMimeType(mime).build()
    }

    private fun toCast(cp: CastPlayer) {
        try {
            val s = session() ?: return
            val old = s.player
            if (old === cp) return
            val items = (0 until old.mediaItemCount).map { old.getMediaItemAt(it) }
            val idx = old.currentMediaItemIndex.coerceAtLeast(0)
            val pos = old.currentPosition.coerceAtLeast(0L)
            val play = old.playWhenReady
            old.pause(); old.stop(); old.clearMediaItems()
            server.start()
            if (items.isNotEmpty()) {
                cp.setMediaItems(items.map { castable(it) }, idx, pos)
                cp.prepare()
                cp.playWhenReady = play
            }
            s.player = cp
        } catch (_: Throwable) {}
    }

    private fun toLocal() {
        try {
            val s = session() ?: return
            if (s.player === local) return
            val items = snapItems.map { originals[it.mediaId] ?: it }
            val idx = snapIndex; val pos = snapPos
            local.volume = 1f
            if (items.isNotEmpty()) {
                local.setMediaItems(items, idx.coerceIn(0, items.size - 1), pos)
                local.prepare()
                local.playWhenReady = false // never blast music on the phone's speaker the moment the TV disconnects
            }
            s.player = local
            server.stop()
        } catch (_: Throwable) {}
    }
}
