package com.swipeapply.app.data.model

/**
 * Swipe direction enum for card interactions
 */
enum class SwipeDirection {
    LEFT,
    RIGHT,
    NONE
}

/**
 * Result of a swipe action
 */
data class SwipeResult(
    val cardId: String,
    val direction: SwipeDirection,
    val isInterested: Boolean
)
