package com.ubayanda.milonative.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.ubayanda.milonative.model.Track

/** Things any song row or the player can ask the app to do (they need a dialog or an Android intent). */
class TrackActions(
    val addToPlaylist: (Track) -> Unit,
    val share: (Track) -> Unit,
    val carMode: () -> Unit = {},
)

val LocalActions = staticCompositionLocalOf { TrackActions({}, {}) }
