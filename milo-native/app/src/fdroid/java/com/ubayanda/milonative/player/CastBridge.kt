package com.ubayanda.milonative.player

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession

/** F-Droid build: Google Cast needs Google's proprietary libraries, so casting is left out and this does nothing. */
@Suppress("UNUSED_PARAMETER")
class CastBridge(ctx: Context, session: () -> MediaSession?, local: ExoPlayer, sessionLocal: androidx.media3.common.Player? = null) {
    fun init() {}
    fun release() {}
}
