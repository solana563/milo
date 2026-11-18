package com.ubayanda.milonative.ui.components

import android.content.Context
import android.net.Uri
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

/**
 * Animated covers ("canvas"): a short looping video the person chooses for a song, played silently over the cover on the player
 * (the idea of Mochy Music's canvas player, GPL-3.0, github.com/realidkroo/Mochy-Music, ui/player/CanvasArtworkPlayer.kt; written
 * for milo). The video file is the person's own: picked from the phone and remembered per song. milo does not fetch canvases from any
 * streaming service.
 */
object CanvasStore {
    /** bumped when a choice changes, so screens reading [get] redraw */
    val version = mutableIntStateOf(0)
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("milo_canvas", Context.MODE_PRIVATE)

    fun get(ctx: Context, songId: Long): String? { version.intValue; return prefs(ctx).getString("c_$songId", null) }

    fun set(ctx: Context, songId: Long, uri: Uri?) {
        if (uri != null) {
            try { ctx.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Throwable) {}
            prefs(ctx).edit().putString("c_$songId", uri.toString()).apply()
        } else prefs(ctx).edit().remove("c_$songId").apply()
        version.intValue++
    }
}

@OptIn(UnstableApi::class)
@Composable
fun CanvasLoop(uri: String, playing: Boolean, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var ready by remember(uri) { mutableStateOf(false) }
    val player = remember(uri) {
        ExoPlayer.Builder(ctx).build().apply {
            volume = 0f
            repeatMode = Player.REPEAT_MODE_ONE
            // never touches the sound: no audio track, no audio focus
            trackSelectionParameters = trackSelectionParameters.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true).build()
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            addListener(object : Player.Listener {
                override fun onRenderedFirstFrame() { ready = true }
                override fun onPlayerError(error: PlaybackException) { ready = false }
            })
            setMediaItem(MediaItem.fromUri(Uri.parse(uri)))
            prepare()
        }
    }
    LaunchedEffect(player, playing) { player.playWhenReady = playing }
    DisposableEffect(player) { onDispose { try { player.release() } catch (_: Throwable) {} } }
    val a by animateFloatAsState(if (ready) 1f else 0f, tween(600), label = "canvasFade")
    AndroidView(
        factory = { c -> TextureView(c).also { player.setVideoTextureView(it) } },
        modifier = modifier.graphicsLayer { alpha = a },
    )
}
