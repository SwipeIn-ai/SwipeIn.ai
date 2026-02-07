package com.swipeapply.app.ui.components.swipe

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.swipeapply.app.data.model.JobCard
import com.swipeapply.app.data.model.SwipeDirection
import kotlinx.coroutines.launch

@Composable
fun SwipeCardStack(
    cards: List<JobCard>,
    onCardSwiped: (JobCard, SwipeDirection) -> Unit,
    onCardClicked: (JobCard) -> Unit,
    onSwipeTriggered: ((SwipeDirection) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val visibleCards by remember(cards) {
        derivedStateOf { cards.take(3) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        visibleCards.asReversed().forEachIndexed { reversedIndex, card ->
            val index = visibleCards.size - 1 - reversedIndex

            if (index > 0) {
                val targetScale = 1f - (index * 0.04f)
                val targetOffset = index * 12f
                val targetAlpha = 1f - (index * 0.15f)

                val scale by animateFloatAsState(
                    targetValue = targetScale,
                    animationSpec = spring(
                        dampingRatio = 0.75f,
                        stiffness = 400f
                    ),
                    label = "cardScale$index"
                )

                val offset by animateFloatAsState(
                    targetValue = targetOffset,
                    animationSpec = spring(
                        dampingRatio = 0.75f,
                        stiffness = 400f
                    ),
                    label = "cardOffset$index"
                )

                key(card.id) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationY = offset
                                alpha = targetAlpha
                            }
                    ) {
                        JobSwipeCard(
                            jobCard = card,
                            swipeProgress = 0f,
                            swipeDirection = SwipeDirection.NONE,
                            onClick = { }
                        )
                    }
                }
            }
        }

        if (visibleCards.isNotEmpty()) {
            val topCard = visibleCards.first()

            val swipeState = rememberSwipeCardState(
                key = topCard.id,
                onSwipe = { direction ->
                    onCardSwiped(topCard, direction)
                }
            )

            key(topCard.id) {
                SwipeableCard(
                    state = swipeState,
                    modifier = Modifier
                ) {
                    JobSwipeCard(
                        jobCard = topCard,
                        swipeProgress = swipeState.swipeProgress,
                        swipeDirection = swipeState.detectedDirection,
                        onClick = { onCardClicked(topCard) }
                    )
                }
            }
        }
    }
}
