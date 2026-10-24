package com.ubayanda.milonative.model

/** One song on the phone (or a USB stick / SD card). Straight from Android's MediaStore, nothing is copied. */
data class Track(
    val id: Long,
    val uri: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val trackNo: Int,
    val year: Int,
    val addedSec: Long,
)

data class Album(val id: Long, val title: String, val artist: String, val year: Int, val tracks: List<Track>)

data class Artist(val name: String, val tracks: List<Track>)

/** Everything the UI needs about playback, in one immutable snapshot. */
data class PlayerUi(
    val currentId: Long? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffle: Boolean = false,
    /** 0 = off, 1 = repeat one, 2 = repeat all (same numbers as Media3). */
    val repeat: Int = 0,
    val speed: Float = 1f,
    val queueIds: List<Long> = emptyList(),
    val queueIndex: Int = 0,
    val sleepLeftMs: Long = 0L,
)
