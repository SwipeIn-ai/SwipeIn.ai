package com.swipeapply.app.ui.components.swipe

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.swipeapply.app.data.model.SwipeDirection
import kotlin.math.abs

/**
 * State holder for swipe card animations and gestures.
 * Manages position, rotation, and swipe detection.
 */
class SwipeCardState(
    private val screenWidth: Float,
    private val onSwipe: (SwipeDirection) -> Unit
) {
    // Position of the card
    val offset = Animatable(Offset.Zero, Offset.VectorConverter)
    
    // Rotation angle in degrees
    var rotation by mutableFloatStateOf(0f)
        private set
    
    // Scale factor for depth effect
    var scale by mutableFloatStateOf(1f)
        private set
    
    // Current detected swipe direction (for visual feedback)
    var detectedDirection by mutableStateOf(SwipeDirection.NONE)
        private set
    
    // Thresholds
    private val swipeThreshold = screenWidth * 0.4f
    private val maxRotation = 15f
    
    // Whether card is being actively dragged
    var isDragging by mutableStateOf(false)
        private set
    
    // Progress of swipe (0 to 1)
    val swipeProgress: Float
        get() = (abs(offset.value.x) / swipeThreshold).coerceIn(0f, 1f)
    
    /**
     * Handle drag gesture
     */
    suspend fun drag(dragAmount: Offset) {
        isDragging = true
        val newOffset = offset.value + dragAmount
        offset.snapTo(newOffset)
        
        // Calculate rotation based on drag
        rotation = (newOffset.x / screenWidth) * maxRotation
        
        // Determine direction for visual feedback
        detectedDirection = when {
            newOffset.x > swipeThreshold * 0.3f -> SwipeDirection.RIGHT
            newOffset.x < -swipeThreshold * 0.3f -> SwipeDirection.LEFT
            else -> SwipeDirection.NONE
        }
    }
    
    /**
     * Handle end of drag gesture
     */
    suspend fun dragEnd(velocity: Offset) {
        isDragging = false
        
        val currentOffset = offset.value
        val velocityBoost = velocity.x * 0.1f
        val effectiveX = currentOffset.x + velocityBoost
        
        when {
            effectiveX > swipeThreshold -> {
                swipeAway(SwipeDirection.RIGHT)
            }
            effectiveX < -swipeThreshold -> {
                swipeAway(SwipeDirection.LEFT)
            }
            else -> {
                resetPosition()
            }
        }
    }
    
    /**
     * Programmatically trigger a swipe
     */
    suspend fun swipe(direction: SwipeDirection) {
        swipeAway(direction)
    }
    
    /**
     * Animate card away in the specified direction
     */
    private suspend fun swipeAway(direction: SwipeDirection) {
        val targetX = when (direction) {
            SwipeDirection.RIGHT -> screenWidth * 1.5f
            SwipeDirection.LEFT -> -screenWidth * 1.5f
            SwipeDirection.NONE -> 0f
        }
        
        detectedDirection = direction
        
        // Animate out with spring physics
        offset.animateTo(
            targetValue = Offset(targetX, offset.value.y),
            animationSpec = spring(
                dampingRatio = 0.8f,
                stiffness = 300f
            )
        )
        
        onSwipe(direction)
    }
    
    /**
     * Reset card to center position
     */
    suspend fun resetPosition() {
        detectedDirection = SwipeDirection.NONE
        
        offset.animateTo(
            targetValue = Offset.Zero,
            animationSpec = spring(
                dampingRatio = 0.7f,
                stiffness = 400f
            )
        )
        rotation = 0f
    }
}

@Composable
fun rememberSwipeCardState(
    key: Any?, 
    onSwipe: (SwipeDirection) -> Unit = {}
): SwipeCardState {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val screenWidth = with(density) {
        configuration.screenWidthDp.dp.toPx()
    }
    return remember(key, screenWidth) {
        SwipeCardState(
            screenWidth = screenWidth,
            onSwipe = onSwipe
        )
    }
}