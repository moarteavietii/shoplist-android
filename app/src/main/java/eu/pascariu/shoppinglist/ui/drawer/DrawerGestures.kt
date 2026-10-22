package eu.pascariu.shoppinglist.ui.drawer

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity

private class DragAccumulator {
    var total by mutableFloatStateOf(0f)
    var startedAtEdge by mutableStateOf(false)
}

fun Modifier.leftEdgeSwipeToOpen(
    isEnabled: Boolean,
    onOpen: () -> Unit,
): Modifier = composed {
    val density = LocalDensity.current
    val edgeSlopPx = with(density) { DrawerConstants.EDGE_SLOP.toPx() }
    val openThresholdPx = with(density) { DrawerConstants.OPEN_THRESHOLD.toPx() }
    val drag = remember(isEnabled) { DragAccumulator() }
    pointerInput(isEnabled) {
        if (!isEnabled) return@pointerInput
        detectHorizontalDragGestures(
            onDragStart = { startOffset ->
                drag.startedAtEdge = startOffset.x <= edgeSlopPx
                drag.total = 0f
            },
        ) { _, dragAmount ->
            drag.total += dragAmount
            if (drag.startedAtEdge && drag.total >= openThresholdPx) {
                drag.startedAtEdge = false
                drag.total = 0f
                onOpen()
            }
        }
    }
}

fun Modifier.swipeLeftToClose(
    isEnabled: Boolean,
    onClose: () -> Unit,
): Modifier = composed {
    val closeThresholdPx = with(LocalDensity.current) { DrawerConstants.CLOSE_THRESHOLD.toPx() }
    val drag = remember(isEnabled) { DragAccumulator() }
    pointerInput(isEnabled) {
        if (!isEnabled) return@pointerInput
        detectHorizontalDragGestures(
            onDragStart = { drag.total = 0f },
        ) { _, dragAmount ->
            drag.total += dragAmount
            if (drag.total <= -closeThresholdPx) {
                drag.total = 0f
                onClose()
            }
        }
    }
}

fun Modifier.swipeUpFromBottomToClose(
    isEnabled: Boolean,
    onClose: () -> Unit,
): Modifier = composed {
    val density = LocalDensity.current
    val closeThresholdPx = with(density) { DrawerConstants.BOTTOM_THRESHOLD.toPx() }
    val bottomSlopPx = with(density) { DrawerConstants.EDGE_SLOP.toPx() }
    val drag = remember(isEnabled) { DragAccumulator() }
    pointerInput(isEnabled) {
        if (!isEnabled) return@pointerInput
        detectVerticalDragGestures(
            onDragStart = { drag.total = 0f },
            onVerticalDrag = { change, dragAmount ->
                if (change.previousPosition.y >= size.height - bottomSlopPx) {
                    drag.total += dragAmount
                    if (drag.total <= -closeThresholdPx) {
                        drag.total = 0f
                        onClose()
                    }
                }
            },
        )
    }
}
