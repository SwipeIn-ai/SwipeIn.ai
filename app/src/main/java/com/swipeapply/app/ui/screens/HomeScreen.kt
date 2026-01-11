package com.swipeapply.app.ui.screens

import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.gestures.detectTapGestures
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swipeapply.app.data.model.JobCard
import com.swipeapply.app.data.model.SwipeDirection
import com.swipeapply.app.ui.components.ShimmerCardStack
import com.swipeapply.app.ui.components.swipe.SwipeCardStack
import com.swipeapply.app.ui.theme.*
import com.swipeapply.app.ui.viewmodel.HomeViewModel
import com.swipeapply.app.ui.viewmodel.UndoableAction

/**
 * Main home screen with card stack and swipe actions.
 * Core Tinder-like experience.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onCardClicked: (JobCard) -> Unit,
    onRequestIntro: (JobCard) -> Unit,
    modifier: Modifier = Modifier,
    onDarkModeToggle: (Boolean?) -> Unit = {},
    isDarkModeEnabled: Boolean? = null
) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return HomeViewModel(context.applicationContext as android.app.Application) as T
            }
        }
    )
    val uiState by viewModel.uiState.collectAsState()
    val view = LocalView.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Show error snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Long
            )
        }
    }
    
    // Bottom sheet state
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    
    // Undo history dialog state
    var showUndoDialog by remember { mutableStateOf(false) }

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
            .background(MaterialTheme.colorScheme.background) // Fix: Use Theme color, not hardcoded Light
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
                },
                onUndoLongPress = {
                    if (uiState.undoHistory.isNotEmpty()) {
                        showUndoDialog = true
                    }
                },
                isDarkMode = isDarkModeEnabled, 
                onDarkModeToggle = onDarkModeToggle
            )
            
            // Card stack area
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                when {
                    uiState.isLoading -> {
                        ShimmerCardStack()
                    }
                    uiState.error != null -> {
                        ErrorState(
                            error = uiState.error!!,
                            onRetry = { viewModel.refreshCards() }
                        )
                    }
                    // Truly empty - no cards AND no more pages to load
                    uiState.isEmpty && !uiState.hasMorePages && !uiState.isLoadingMore -> {
                        EmptyState(
                            interestedCount = uiState.interestedCards.size,
                            onReset = { viewModel.resetCards() }
                        )
                    }
                    // Cards are empty but more are coming
                    uiState.cards.isEmpty() && (uiState.hasMorePages || uiState.isLoadingMore) -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = GradientStart,
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Loading more jobs...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                    // Normal state - show cards
                    uiState.cards.isNotEmpty() -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Jobs counter
                            if (uiState.totalJobs > 0) {
                                Text(
                                    text = "${uiState.cards.size} remaining of ${uiState.totalJobs} jobs",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextTertiary,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            
                            SwipeCardStack(
                                cards = uiState.cards,
                                onCardSwiped = { card, direction ->
                                    viewModel.onCardSwiped(card, direction)
                                },
                                onCardClicked = { card ->
                                    viewModel.selectCard(card)
                                }
                            )
                            
                            // Loading more indicator
                            if (uiState.isLoadingMore) {
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth(0.5f)
                                        .height(2.dp),
                                    color = GradientStart
                                )
                            }
                        }
                    }
                }
            }
            
            // Bottom action buttons
            AnimatedVisibility(
                visible = uiState.cards.isNotEmpty() && !uiState.isLoading,
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
        
        // Undo history dialog
        if (showUndoDialog) {
            UndoHistoryDialog(
                undoHistory = uiState.undoHistory,
                onDismiss = { showUndoDialog = false },
                onUndoCount = { count ->
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    viewModel.undoSwipes(count)
                    showUndoDialog = false
                },
                onClearHistory = {
                    viewModel.clearUndoHistory()
                    showUndoDialog = false
                }
            )
        }
        
        // Snackbar for errors
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}

@Composable
private fun HomeTopBar(
    stats: com.swipeapply.app.ui.viewmodel.SwipeStats,
    onUndo: () -> Unit,
    onUndoLongPress: () -> Unit,
    isDarkMode: Boolean? = null,
    onDarkModeToggle: (Boolean?) -> Unit = {}
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
                
                // Undo button with count badge
                Box {
                    IconButton(
    onClick = onUndo,
    modifier = Modifier.pointerInput(Unit) {
        detectTapGestures(
            onLongPress = { onUndoLongPress() }
        )
    }
) {
    Icon(
        imageVector = Icons.Default.Refresh,
        contentDescription = "Undo (long press for history)",
        tint = if (stats.undoCount > 0) Primary else TextSecondary
    )
}

                    
                    // Undo count badge
                    if (stats.undoCount > 0) {
                        Surface(
                            shape = CircleShape,
                            color = Primary,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(16.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = if (stats.undoCount > 9) "9+" else stats.undoCount.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                
                // Dark mode toggle button
                IconButton(onClick = {
                    val newMode = when (isDarkMode) {
                        null -> true // System -> Dark
                        true -> false // Dark -> Light
                        false -> null // Light -> System
                    }
                    onDarkModeToggle(newMode)
                }) {
                    Icon(
                        imageVector = when (isDarkMode) {
                            true -> Icons.Default.LightMode
                            false -> Icons.Default.DarkMode
                            null -> Icons.Default.DarkMode
                        },
                        contentDescription = "Toggle dark mode",
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
        
        // CTA Button with gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                        colors = listOf(GradientStart, GradientEnd)
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .clip(RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            TextButton(
                onClick = onRequestIntro,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "Request intro template",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UndoHistoryDialog(
    undoHistory: List<UndoableAction>,
    onDismiss: () -> Unit,
    onUndoCount: (Int) -> Unit,
    onClearHistory: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Undo History",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${undoHistory.size} actions can be undone",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // History list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(undoHistory.reversed()) { action ->
                        UndoHistoryItem(action = action)
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Undo all button
                TextButton(
                    onClick = { onUndoCount(undoHistory.size) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Primary
                    )
                ) {
                    Text("Undo All")
                }
                
                // Undo last 5
                if (undoHistory.size >= 5) {
                    TextButton(
                        onClick = { onUndoCount(5) },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Primary
                        )
                    ) {
                        Text("Undo 5")
                    }
                }
            }
        },
        dismissButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onClearHistory,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = AccentRed
                    )
                ) {
                    Text("Clear")
                }
                
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        },
        containerColor = BackgroundCard,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun UndoHistoryItem(action: UndoableAction) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = BackgroundSecondary,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = action.card.company.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = action.card.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            
            // Direction badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (action.result.isInterested) AccentGreenLight else AccentRedLight
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (action.result.isInterested) Icons.Default.Favorite else Icons.Default.Close,
                        contentDescription = null,
                        tint = if (action.result.isInterested) AccentGreen else AccentRed,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (action.result.isInterested) "Liked" else "Skipped",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (action.result.isInterested) AccentGreen else AccentRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// Extension for graphicsLayer on Text
@Composable
private fun ErrorState(
    error: String,
    onRetry: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(32.dp)
    ) {
        // Error icon
        Text(
            text = "⚠️",
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = "Oops! Something went wrong",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Error message
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = AccentRedLight,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = AccentRed,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Retry button
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Primary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Try Again",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Check your internet connection and try again",
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun Modifier.graphicsLayer(block: androidx.compose.ui.graphics.GraphicsLayerScope.() -> Unit): Modifier {
    return this.then(
        Modifier.graphicsLayer(block)
    )
}
