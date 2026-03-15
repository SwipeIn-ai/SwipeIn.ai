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
import com.swipeapply.app.data.model.Employee
import com.swipeapply.app.data.model.SwipeDirection
import kotlinx.coroutines.launch

/**
 * Stacked card display for Employee/Referral contacts.
 * Mirrors the SwipeCardStack behavior but uses EmployeeSwipeCard 
 * for a distinct referral-focused visual experience.
 */
@Composable
fun EmployeeCardStack(
    employees: List<Employee>,
    companyName: String,
    onSaveContact: (Employee) -> Unit,
    onDismissContact: (Employee) -> Unit,
    onCardClicked: (Employee) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val visibleCards by remember(employees) {
        derivedStateOf { employees.take(3) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Background cards (stacked behind)
        visibleCards.asReversed().forEachIndexed { reversedIndex, employee ->
            val index = visibleCards.size - 1 - reversedIndex

            if (index > 0) {
                val targetScale = 1f - (index * 0.04f)
                val targetOffset = index * 12f
                val targetAlpha = 1f - (index * 0.15f)

                val scale by animateFloatAsState(
                    targetValue = targetScale,
                    animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
                    label = "empCardScale$index"
                )

                val offset by animateFloatAsState(
                    targetValue = targetOffset,
                    animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
                    label = "empCardOffset$index"
                )

                key(employee.email) {
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
                        EmployeeSwipeCard(
                            employee = employee,
                            companyName = companyName,
                            swipeProgress = 0f,
                            swipeDirection = SwipeDirection.NONE,
                            onClick = { }
                        )
                    }
                }
            }
        }

        // Top card (swipeable)
        if (visibleCards.isNotEmpty()) {
            val topEmployee = visibleCards.first()

            val swipeState = rememberSwipeCardState(
                key = topEmployee.email,
                onSwipe = { direction ->
                    when (direction) {
                        SwipeDirection.RIGHT -> onSaveContact(topEmployee)
                        SwipeDirection.LEFT -> onDismissContact(topEmployee)
                        SwipeDirection.NONE -> { }
                    }
                }
            )

            key(topEmployee.email) {
                SwipeableCard(
                    state = swipeState,
                    modifier = Modifier
                ) {
                    EmployeeSwipeCard(
                        employee = topEmployee,
                        companyName = companyName,
                        swipeProgress = swipeState.swipeProgress,
                        swipeDirection = swipeState.detectedDirection,
                        onClick = { onCardClicked(topEmployee) }
                    )
                }
            }
        }
    }
}
