package app.vitriol.ui.util

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs
import kotlin.math.hypot

private const val MIN_SWIPE_DISTANCE = 50f

// * Adds swipe gesture detection to a composable
internal fun Modifier.detectSwipeGestures(
    onSwipeUp: () -> Unit = {},
    onSwipeDown: () -> Unit = {},
    onSwipeLeft: () -> Unit = {},
    onSwipeRight: () -> Unit = {},
): Modifier =
    this.then(
        Modifier.pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                val (x, y) = dragAmount

                // Determine which direction has the highest magnitude
                when {
                    abs(x) > abs(y) && abs(x) > MIN_SWIPE_DISTANCE -> {
                        // Horizontal swipe
                        if (x > 0) {
                            onSwipeRight()
                        } else {
                            onSwipeLeft()
                        }
                    }
                    abs(y) > abs(x) && abs(y) > MIN_SWIPE_DISTANCE -> {
                        // Vertical swipe
                        if (y > 0) {
                            onSwipeDown()
                        } else {
                            onSwipeUp()
                        }
                    }
                }
            }
        },
    )

internal fun Modifier.detectTwoFingerSwipes(
    onSwipeUp: () -> Unit = {},
    onSwipeDown: () -> Unit = {},
    onSwipeLeft: () -> Unit = {},
    onSwipeRight: () -> Unit = {},
): Modifier =
    this.then(
        Modifier.pointerInput(Unit) {
            awaitEachGesture {
                val startPositions = mutableMapOf<PointerId, Offset>()

                // Wait for two fingers to be down
                while (startPositions.size < 2) {
                    val event = awaitPointerEvent()
                    event.changes.filter { it.pressed }.forEach {
                        startPositions[it.id] = it.position
                    }
                }

                val startXs = startPositions.values.map { it.x }
                val startYs = startPositions.values.map { it.y }

                var endXs = startXs
                var endYs = startYs

                // Track finger movement until one is lifted
                while (true) {
                    val event = awaitPointerEvent()
                    val pressed = event.changes.filter { it.pressed }

                    if (pressed.size < 2) break

                    endXs = pressed.map { it.position.x }
                    endYs = pressed.map { it.position.y }

                    pressed.forEach { it.consume() }
                }

                val dx = endXs.zip(startXs) { end, start -> end - start }
                val dy = endYs.zip(startYs) { end, start -> end - start }

                handleTwoFingerSwipe(dx, dy, SwipeCallbacks(onSwipeUp, onSwipeDown, onSwipeLeft, onSwipeRight))
            }
        },
    )

private data class SwipeCallbacks(
    val onSwipeUp: () -> Unit = {},
    val onSwipeDown: () -> Unit = {},
    val onSwipeLeft: () -> Unit = {},
    val onSwipeRight: () -> Unit = {},
)

private fun handleTwoFingerSwipe(
    dx: List<Float>,
    dy: List<Float>,
    callbacks: SwipeCallbacks,
) {
    if (dy.all { abs(it) > abs(dx.first()) }) {
        when {
            dy.all { it > MIN_SWIPE_DISTANCE } -> callbacks.onSwipeDown()
            dy.all { it < -MIN_SWIPE_DISTANCE } -> callbacks.onSwipeUp()
        }
    } else if (dx.all { abs(it) > abs(dy.first()) }) {
        when {
            dx.all { it > MIN_SWIPE_DISTANCE } -> callbacks.onSwipeRight()
            dx.all { it < -MIN_SWIPE_DISTANCE } -> callbacks.onSwipeLeft()
        }
    }
}

internal fun Modifier.detectPinchGestures(onPinch: (zoomDelta: Float) -> Unit): Modifier =
    pointerInput(Unit) {
        awaitEachGesture {
            var pointer1: PointerInputChange? = null
            var pointer2: PointerInputChange? = null

            while (pointer1 == null || pointer2 == null) {
                val event = awaitPointerEvent()
                val pressed = event.changes.filter { it.pressed }
                if (pressed.size >= 2) {
                    pointer1 = pressed[0]
                    pointer2 = pressed[1]
                }
            }

            var previousDistance = distanceBetween(pointer1.position, pointer2.position)

            while (true) {
                val event = awaitPointerEvent()
                val changes = event.changes.filter { it.pressed }
                if (changes.size < 2) break

                val pos1 = changes[0].position
                val pos2 = changes[1].position

                val currentDistance = distanceBetween(pos1, pos2)
                val zoomDelta = currentDistance - previousDistance

                if (zoomDelta != 0f) {
                    onPinch(zoomDelta)
                }

                previousDistance = currentDistance
                changes.forEach { it.consume() }
            }
        }
    }

private fun distanceBetween(
    p1: Offset,
    p2: Offset,
): Float = hypot(p2.x - p1.x, p2.y - p1.y)
