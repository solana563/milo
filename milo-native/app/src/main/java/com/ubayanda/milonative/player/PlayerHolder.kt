package com.ubayanda.milonative.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.ubayanda.milonative.model.PlayerUi
import com.ubayanda.milonative.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** The UI's remote control for [PlaybackService]. Everything the screens show about playback comes from [state]. */
class PlayerHolder(context: Context) {
    private val app = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.Main)
    private val _state = MutableStateFlow(PlayerUi())
    val state: StateFlow<PlayerUi> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var ticker: Job? = null
    private var sleepJob: Job? = null
    private var pending: (() -> Unit)? = null

    fun connect() {
        if (controller != null) return
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()
        future.addListener({
            val c = try { future.get() } catch (e: Exception) { return@addListener }
            controller = c
            c.addListener(object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) = refresh()
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    // a stream that won't play: tell the person and empty the queue so the player isn't left half-broken
                    android.widget.Toast.makeText(app, "That one won't play right now.", android.widget.Toast.LENGTH_SHORT).show()
                    controller?.let { it.stop(); it.clearMediaItems() }
                }
            })
            refresh()
            startTicker()
            pending?.invoke()
            pending = null
        }, ContextCompat.getMainExecutor(app))
    }

    fun release() {
        ticker?.cancel()
        sleepJob?.cancel()
        controller?.release()
        controller = null
    }

    private companion object { const val QUEUE_MAX = 120; const val QUEUE_BEHIND = 20 }

    private fun item(t: Track): MediaItem = MediaItem.Builder()
        .setMediaId(t.id.toString())
        .setUri(t.uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(t.title)
                .setArtist(t.artist)
                .setAlbumTitle(t.album)
                .build()
        )
        .build()

    private fun withController(block: (MediaController) -> Unit) {
        val c = controller
        if (c != null) block(c) else { pending = { controller?.let(block) }; connect() }
    }

    /** Play [tracks] as the queue, starting at [index]. */
    fun playQueue(tracks: List<Track>, index: Int) = withController { c ->
        if (tracks.isEmpty()) return@withController
        // Media3 1.4 can crash while syncing a very long queue to the notification/controller, so keep the queue to a window around the song.
        val i = index.coerceIn(0, tracks.size - 1)
        val from = (i - QUEUE_BEHIND).coerceAtLeast(0)
        val part = tracks.subList(from, (from + QUEUE_MAX).coerceAtMost(tracks.size))
        c.setMediaItems(part.map { item(it) }, i - from, 0L)
        c.prepare()
        c.play()
    }

    fun playShuffled(tracks: List<Track>) = withController { c ->
        if (tracks.isEmpty()) return@withController
        c.shuffleModeEnabled = true
        val part = if (tracks.size > QUEUE_MAX) tracks.shuffled().take(QUEUE_MAX) else tracks
        c.setMediaItems(part.map { item(it) }, part.indices.random(), 0L)
        c.prepare()
        c.play()
    }

    fun playNextMany(list: List<Track>) = withController { c ->
        if (list.isEmpty()) return@withController
        if (c.mediaItemCount == 0) { c.setMediaItems(list.map { item(it) }); c.prepare(); c.play() }
        else c.addMediaItems(c.currentMediaItemIndex + 1, list.map { item(it) })
    }
    fun addManyToQueue(list: List<Track>) = withController { c ->
        if (list.isEmpty()) return@withController
        if (c.mediaItemCount == 0) { c.setMediaItems(list.map { item(it) }); c.prepare(); c.play() }
        else c.addMediaItems(list.map { item(it) })
    }
    fun moveInQueue(from: Int, to: Int) = withController { c ->
        if (from in 0 until c.mediaItemCount && to in 0 until c.mediaItemCount) c.moveMediaItem(from, to)
    }

    fun playNext(t: Track) = withController { c ->
        if (c.mediaItemCount == 0) { c.setMediaItems(listOf(item(t))); c.prepare(); c.play() }
        else c.addMediaItem(c.currentMediaItemIndex + 1, item(t))
    }
    fun addToQueue(t: Track) = withController { c ->
        if (c.mediaItemCount == 0) { c.setMediaItems(listOf(item(t))); c.prepare(); c.play() }
        else c.addMediaItem(item(t))
    }
    fun removeFromQueue(i: Int) = withController { c -> if (i in 0 until c.mediaItemCount) c.removeMediaItem(i) }

    fun toggle() = withController { c -> if (c.isPlaying) c.pause() else c.play() }
    fun next() = withController { c -> c.seekToNextMediaItem() }
    fun previous() = withController { c -> if (c.currentPosition > 3000) c.seekTo(0) else c.seekToPreviousMediaItem() }
    fun seekTo(ms: Long) = withController { c -> c.seekTo(ms) }
    fun toggleShuffle() = withController { c -> c.shuffleModeEnabled = !c.shuffleModeEnabled }
    fun cycleRepeat() = withController { c ->
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }
    fun setSpeed(speed: Float) = withController { c -> c.setPlaybackSpeed(speed) }
    fun playQueueIndex(i: Int) = withController { c -> c.seekToDefaultPosition(i); c.play() }

    /** Pause after [minutes] (0 cancels). */
    fun sleepIn(minutes: Int) {
        sleepJob?.cancel()
        if (minutes <= 0) { _state.value = _state.value.copy(sleepLeftMs = 0L); return }
        val end = System.currentTimeMillis() + minutes * 60_000L
        sleepJob = scope.launch {
            while (true) {
                val left = end - System.currentTimeMillis()
                if (left <= 0) break
                _state.value = _state.value.copy(sleepLeftMs = left)
                delay(1000)
            }
            _state.value = _state.value.copy(sleepLeftMs = 0L)
            controller?.pause()
        }
    }

    private fun refresh() {
        val c = controller ?: return
        val ids = ArrayList<Long>(c.mediaItemCount)
        for (i in 0 until c.mediaItemCount) c.getMediaItemAt(i).mediaId.toLongOrNull()?.let { ids.add(it) }
        _state.value = _state.value.copy(
            currentId = c.currentMediaItem?.mediaId?.toLongOrNull(),
            isPlaying = c.isPlaying,
            positionMs = c.currentPosition.coerceAtLeast(0L),
            durationMs = if (c.duration > 0) c.duration else 0L,
            shuffle = c.shuffleModeEnabled,
            repeat = c.repeatMode,
            speed = c.playbackParameters.speed,
            queueIds = ids,
            queueIndex = c.currentMediaItemIndex.coerceAtLeast(0),
        )
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (true) {
                val c = controller
                if (c != null && c.isPlaying) {
                    _state.value = _state.value.copy(positionMs = c.currentPosition.coerceAtLeast(0L), durationMs = if (c.duration > 0) c.duration else _state.value.durationMs)
                }
                delay(250)
            }
        }
    }
}
