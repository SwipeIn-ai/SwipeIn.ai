package com.swipeapply.app.ui.components.swipe

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
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

class SwipeCardState(
    private val screenWidth: Float,
    private val onSwipe: (SwipeDirection) -> Unit
) {
    val offset = Animatable(Offset.Zero, Offset.VectorConverter)

    var rotation by mutableFloatStateOf(0f)
        private set

    var scale by mutableFloatStateOf(1f)
        private set

    var detectedDirection by mutableStateOf(SwipeDirection.NONE)
        private set

    private val swipeThreshold = screenWidth * 0.35f
    private val maxRotation = 12f
    private val velocityMultiplier = 0.15f

    var isDragging by mutableStateOf(false)
        private set

    val swipeProgress: Float
        get() = (abs(offset.value.x) / swipeThreshold).coerceIn(0f, 1f)

    suspend fun drag(dragAmount: Offset) {
        isDragging = true
        val newOffset = offset.value + dragAmount
        offset.snapTo(newOffset)
        rotation = (newOffset.x / screenWidth) * maxRotation
        detectedDirection = when {
            newOffset.x > swipeThreshold * 0.25f -> SwipeDirection.RIGHT
            newOffset.x < -swipeThreshold * 0.25f -> SwipeDirection.LEFT
            else -> SwipeDirection.NONE
        }
    }

    suspend fun dragEnd(velocity: Offset) {
        isDragging = false
        val currentOffset = offset.value
        val velocityBoost = velocity.x * velocityMultiplier
        val effectiveX = currentOffset.x + velocityBoost

        when {
            effectiveX > swipeThreshold -> swipeAway(SwipeDirection.RIGHT)
            effectiveX < -swipeThreshold -> swipeAway(SwipeDirection.LEFT)
            else -> resetPosition()
        }
    }

    suspend fun swipe(direction: SwipeDirection) {
        swipeAway(direction)
    }

    private suspend fun swipeAway(direction: SwipeDirection) {
        val targetX = when (direction) {
            SwipeDirection.RIGHT -> screenWidth * 1.5f
            SwipeDirection.LEFT -> -screenWidth * 1.5f
            SwipeDirection.NONE -> 0f
        }
        detectedDirection = direction
        offset.animateTo(
            targetValue = Offset(targetX, offset.value.y * 0.5f),
            animationSpec = spring(
                dampingRatio = 0.75f,
                stiffness = 200f
            )
        )
        onSwipe(direction)
    }

    suspend fun resetPosition() {
        detectedDirection = SwipeDirection.NONE
        offset.animateTo(
            targetValue = Offset.Zero,
            animationSpec = spring(
                dampingRatio = 0.6f,
                stiffness = 500f
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
