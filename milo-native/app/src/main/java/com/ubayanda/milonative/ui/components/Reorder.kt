package com.ubayanda.milonative.ui.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Drag-to-reorder for a LazyColumn. The caller keeps a local copy of the list, [onMove] swaps it while the finger is down,
 * and [onCommit] (original index, final index) runs once when the finger lifts.
 */
class ReorderState(
    private val lazy: LazyListState,
    private val headerCount: Int,
    private val onMove: (Int, Int) -> Unit,
    private val onCommit: (Int, Int) -> Unit,
) {
    var dragIndex by mutableIntStateOf(-1)
        private set
    var offset by mutableFloatStateOf(0f)
        private set
    private var startIndex = -1
    private var draggedKey: Any? = null
    private var settling = false

    fun isDragging(key: Any) = dragIndex >= 0 && key == draggedKey

    fun start(index: Int, key: Any) {
        dragIndex = index; startIndex = index; draggedKey = key; offset = 0f; settling = false
    }

    fun drag(dy: Float) {
        if (dragIndex < 0) return
        offset += dy
        val infos = lazy.layoutInfo.visibleItemsInfo
        val cur = infos.firstOrNull { it.key == draggedKey } ?: return
        // after a swap, wait until the list has been laid out again before looking for the next one
        if (settling) { if (cur.index - headerCount == dragIndex) settling = false else return }
        val center = cur.offset + cur.size / 2f + offset
        val target = infos.firstOrNull { it.index != cur.index && it.index >= headerCount && center >= it.offset && center <= it.offset + it.size } ?: return
        val to = target.index - headerCount
        offset -= (target.offset - cur.offset)
        onMove(dragIndex, to)
        dragIndex = to
        settling = true
    }

    fun end() {
        val from = startIndex; val to = dragIndex
        dragIndex = -1; offset = 0f; draggedKey = null; settling = false
        if (from >= 0 && to >= 0 && from != to) onCommit(from, to)
    }

    fun handle(index: () -> Int, key: Any): Modifier = Modifier.pointerInput(key) {
        detectDragGestures(
            onDragStart = { start(index(), key) },
            onDragEnd = { end() },
            onDragCancel = { end() },
            onDrag = { change, amount -> change.consume(); drag(amount.y) },
        )
    }
}
