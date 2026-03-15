package com.swipeapply.app.ui.components.swipe

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import com.swipeapply.app.data.model.SwipeDirection
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun SwipeableCard(
    modifier: Modifier = Modifier,
    state: SwipeCardState,
    onSwiped: (SwipeDirection) -> Unit = {},
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    var previousDirection by remember { mutableStateOf(SwipeDirection.NONE) }
    var hasTriggeredHaptic by remember { mutableStateOf(false) }

    LaunchedEffect(state.detectedDirection) {
        if (state.detectedDirection != SwipeDirection.NONE &&
            state.detectedDirection != previousDirection) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            hasTriggeredHaptic = true
        }
        if (state.detectedDirection == SwipeDirection.NONE) {
            hasTriggeredHaptic = false
        }
        previousDirection = state.detectedDirection
    }

    Box(
        modifier = modifier
            .offset {
                IntOffset(
                    x = state.offset.value.x.roundToInt(),
                    y = state.offset.value.y.roundToInt()
                )
            }
            .graphicsLayer {
                rotationZ = state.rotation
                val dragProgress = state.swipeProgress
                val dragScale = 1f - (dragProgress * 0.03f)
                scaleX = dragScale
                scaleY = dragScale
                alpha = 1f - (dragProgress * 0.05f)
            }
            .pointerInput(state) {
                val velocityTracker = VelocityTracker()
                detectDragGestures(
                    onDragStart = {
                        velocityTracker.resetTracking()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val currentTime = change.uptimeMillis
                        val currentPosition = change.position
                        velocityTracker.addPosition(currentTime, currentPosition)
                        scope.launch {
                            state.drag(dragAmount)
                        }
                    },
                    onDragEnd = {
                        val velocity = try {
                            val v = velocityTracker.calculateVelocity()
                            Offset(v.x, v.y)
                        } catch (e: Exception) {
                            Offset.Zero
                        }
                        scope.launch {
                            state.dragEnd(
                                Offset(velocity.x * 0.001f, velocity.y * 0.001f)
                            )
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            state.resetPosition()
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
