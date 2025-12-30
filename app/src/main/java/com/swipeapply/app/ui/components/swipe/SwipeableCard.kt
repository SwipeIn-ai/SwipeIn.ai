package com.swipeapply.app.ui.components.swipe

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import com.swipeapply.app.data.model.SwipeDirection
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * A swipeable card container with Tinder-like gestures.
 * Handles drag, rotation, and spring animations.
 */
@Composable
fun SwipeableCard(
    modifier: Modifier = Modifier,
    state: SwipeCardState,
    onSwiped: (SwipeDirection) -> Unit = {},
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    
    // Track previous direction for haptic feedback
    var previousDirection by remember { mutableStateOf(SwipeDirection.NONE) }
    
    // Trigger haptic feedback when crossing threshold
    LaunchedEffect(state.detectedDirection) {
        if (state.detectedDirection != SwipeDirection.NONE && 
            state.detectedDirection != previousDirection) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
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
                
                // Subtle scale effect during drag
                val dragScale = 1f - (state.swipeProgress * 0.02f)
                scaleX = dragScale
                scaleY = dragScale
            }
            .pointerInput(Unit) {
                var velocity = Offset.Zero
                
                detectDragGestures(
                    onDragStart = {
                        velocity = Offset.Zero
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        velocity = dragAmount
                        scope.launch {
                            state.drag(dragAmount)
                        }
                    },
                    onDragEnd = {
                        scope.launch {
                            state.dragEnd(velocity)
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
