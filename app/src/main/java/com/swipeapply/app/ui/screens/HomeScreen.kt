package com.swipeapply.app.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swipeapply.app.data.model.JobCard
import com.swipeapply.app.data.model.SwipeDirection
import com.swipeapply.app.ui.components.swipe.SwipeCardStack
import com.swipeapply.app.ui.theme.*
import com.swipeapply.app.ui.viewmodel.HomeViewModel

/**
 * Main home screen with card stack and swipe actions.
 * Core Tinder-like experience.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onCardClicked: (JobCard) -> Unit,
    onRequestIntro: (JobCard) -> Unit,
    viewModel: HomeViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val view = LocalView.current
    
    // Bottom sheet state
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    
    LaunchedEffect(uiState.showBottomSheet) {
        if (uiState.showBottomSheet) {
            sheetState.show()
        } else {
            sheetState.hide()
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top bar
            HomeTopBar(
                stats = viewModel.getStats(),
                onUndo = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    viewModel.undoLastSwipe()
                }
            )
            
            // Card stack area
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                when {
                    uiState.isLoading -> {
                        LoadingState()
                    }
                    uiState.isEmpty -> {
                        EmptyState(
                            interestedCount = uiState.interestedCards.size,
                            onReset = { viewModel.resetCards() }
                        )
                    }
                    else -> {
                        SwipeCardStack(
                            cards = uiState.cards,
                            onCardSwiped = { card, direction ->
                                viewModel.onCardSwiped(card, direction)
                            },
                            onCardClicked = { card ->
                                viewModel.selectCard(card)
                            }
                        )
                    }
                }
            }
            
            // Bottom action buttons
            AnimatedVisibility(
                visible = !uiState.isEmpty && !uiState.isLoading,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                ActionButtons(
                    onSkip = {
                        view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                        uiState.cards.firstOrNull()?.let { card ->
                            viewModel.onCardSwiped(card, SwipeDirection.LEFT)
                        }
                    },
                    onInterested = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        uiState.cards.firstOrNull()?.let { card ->
                            viewModel.onCardSwiped(card, SwipeDirection.RIGHT)
                        }
                    }
                )
            }
        }
        
        // Company detail bottom sheet
        if (uiState.showBottomSheet && uiState.selectedCard != null) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.dismissBottomSheet() },
                sheetState = sheetState,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                containerColor = BackgroundCard,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                CompanyDetailSheet(
                    jobCard = uiState.selectedCard!!,
                    onDismiss = { viewModel.dismissBottomSheet() },
                    onRequestIntro = {
                        viewModel.dismissBottomSheet()
                        onRequestIntro(uiState.selectedCard!!)
                    }
                )
            }
        }
    }
}

@Composable
private fun HomeTopBar(
    stats: com.swipeapply.app.ui.viewmodel.SwipeStats,
    onUndo: () -> Unit
) {
    Surface(
        color = BackgroundLight,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App name
            Text(
                text = "SwipeApply",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            
            // Stats and undo
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interested count
                if (stats.interested > 0) {
                    StatBadge(
                        count = stats.interested,
                        color = AccentGreen
                    )
                }
                
                // Undo button
                IconButton(onClick = onUndo) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Undo",
                        tint = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun StatBadge(
    count: Int,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun LoadingState() {
    CircularProgressIndicator(
        color = GradientStart,
        strokeWidth = 3.dp
    )
}

@Composable
private fun EmptyState(
    interestedCount: Int,
    onReset: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(32.dp)
    ) {
        // Celebration animation
        val infiniteTransition = rememberInfiniteTransition(label = "empty")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = EaseInOutCubic),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
        
        Text(
            text = "🎉",
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.graphicsLayer { 
                scaleX = scale
                scaleY = scale
            }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "You're all caught up!",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = if (interestedCount > 0) {
                "You're interested in $interestedCount ${if (interestedCount == 1) "role" else "roles"}.\nTime to send some intros!"
            } else {
                "Check back later for new opportunities."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Reset button
        OutlinedButton(
            onClick = onReset,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Start over")
        }
    }
}

@Composable
private fun ActionButtons(
    onSkip: () -> Unit,
    onInterested: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp)
            .padding(bottom = 32.dp)
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Skip button
        ActionButton(
            icon = Icons.Default.Close,
            contentDescription = "Skip",
            containerColor = AccentRedLight,
            contentColor = AccentRed,
            onClick = onSkip
        )
        
        // Interested button
        ActionButton(
            icon = Icons.Default.Favorite,
            contentDescription = "Interested",
            containerColor = AccentGreenLight,
            contentColor = AccentGreen,
            onClick = onInterested,
            isLarge = true
        )
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    contentDescription: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    isLarge: Boolean = false
) {
    val size = if (isLarge) 72.dp else 60.dp
    val iconSize = if (isLarge) 32.dp else 24.dp
    
    FilledIconButton(
        onClick = onClick,
        modifier = Modifier
            .size(size)
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                spotColor = contentColor.copy(alpha = 0.3f)
            ),
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun CompanyDetailSheet(
    jobCard: JobCard,
    onDismiss: () -> Unit,
    onRequestIntro: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .navigationBarsPadding()
    ) {
        // Company info header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Logo
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = GradientStart.copy(alpha = 0.1f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = jobCard.company.name.take(2).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = GradientStart,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Column {
                Text(
                    text = jobCard.company.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = jobCard.company.industry,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Company description
        Text(
            text = "About the company",
            style = MaterialTheme.typography.labelMedium,
            color = TextTertiary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = jobCard.company.description,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Role details
        Text(
            text = "The role",
            style = MaterialTheme.typography.labelMedium,
            color = TextTertiary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = jobCard.title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = jobCard.roleDescription,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Why it matches
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = ChipBackgroundAccent
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Why this matches you",
                    style = MaterialTheme.typography.labelMedium,
                    color = ChipTextAccent,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = jobCard.matchReason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // CTA Button
        Button(
            onClick = onRequestIntro,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Primary
            )
        ) {
            Text(
                text = "Request intro template",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// Extension for graphicsLayer on Text
@Composable
private fun Modifier.graphicsLayer(block: androidx.compose.ui.graphics.GraphicsLayerScope.() -> Unit): Modifier {
    return this.then(
        Modifier.graphicsLayer(block)
    )
}
