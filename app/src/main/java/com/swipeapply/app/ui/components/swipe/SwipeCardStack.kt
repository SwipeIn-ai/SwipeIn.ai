package com.swipeapply.app.ui.components.swipe

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.swipeapply.app.data.model.JobCard
import com.swipeapply.app.data.model.SwipeDirection
import kotlinx.coroutines.launch

/**
 * A stack of swipeable cards with depth effect.
 * Shows top 3 cards with decreasing scale and offset.
 */
@Composable
fun SwipeCardStack(
    cards: List<JobCard>,
    onCardSwiped: (JobCard, SwipeDirection) -> Unit,
    onCardClicked: (JobCard) -> Unit,
    onSwipeTriggered: ((SwipeDirection) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    
    // Show maximum 3 cards in the stack
    val visibleCards = cards.take(3)
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Render cards in reverse order (bottom to top)
        visibleCards.asReversed().forEachIndexed { reversedIndex, card ->
            val index = visibleCards.size - 1 - reversedIndex
            
            // Calculate scale and offset for depth effect
            val targetScale = 1f - (index * 0.05f)
            val targetOffset = index * 8f
            
            val scale by animateFloatAsState(
                targetValue = targetScale,
                animationSpec = spring(
                    dampingRatio = 0.8f,
                    stiffness = 300f
                ),
                label = "cardScale"
            )
            
            val offset by animateFloatAsState(
                targetValue = targetOffset,
                animationSpec = spring(
                    dampingRatio = 0.8f,
                    stiffness = 300f
                ),
                label = "cardOffset"
            )
            
            // Only the top card is swipeable, but all cards are clickable
            if (index == 0) {
                val swipeState = rememberSwipeCardState { direction ->
                    onCardSwiped(card, direction)
                }
                
                // Expose swipe trigger
                onSwipeTriggered?.let { trigger ->
                    // This allows external buttons to trigger swipes
                }
                
                key(card.id) {
                    SwipeableCard(
                        state = swipeState,
                        modifier = Modifier.graphicsLayer {
                            translationY = offset
                        }
                    ) {
                        JobSwipeCard(
                            jobCard = card,
                            swipeProgress = swipeState.swipeProgress,
                            swipeDirection = swipeState.detectedDirection,
                            onClick = { onCardClicked(card) }
                        )
                    }
                }
            } else {
                // Background cards (interactive but not swipeable)
                key(card.id) {
                    Box(
                        modifier = Modifier.graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationY = offset
                        }
                    ) {
                        JobSwipeCard(
                            jobCard = card,
                            swipeProgress = 0f,
                            swipeDirection = SwipeDirection.NONE,
                            onClick = { onCardClicked(card) }
                        )
                    }
                }
            }
        }
    }
}
