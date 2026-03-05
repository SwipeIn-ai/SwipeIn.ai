package com.swipeapply.app.ui.screens

import android.app.Application
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.swipeapply.app.SupabaseClient
import com.swipeapply.app.data.model.JobCard
import com.swipeapply.app.data.model.SwipeDirection
import com.swipeapply.app.ui.components.ShimmerCardStack
import com.swipeapply.app.ui.components.swipe.SwipeCardStack
import com.swipeapply.app.ui.theme.AccentGreen
import com.swipeapply.app.ui.theme.AccentGreenLight
import com.swipeapply.app.ui.theme.AccentRed
import com.swipeapply.app.ui.theme.AccentRedLight
import com.swipeapply.app.ui.theme.GradientEnd
import com.swipeapply.app.ui.theme.GradientStart
import com.swipeapply.app.ui.viewmodel.HomeViewModel
import com.swipeapply.app.ui.viewmodel.UndoableAction
import com.swipeapply.app.ui.viewmodel.ViewModelFactory
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onCardClicked: (JobCard) -> Unit,
    onRequestIntro: (JobCard) -> Unit,
    onNavigateToEmployeeFinder: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    onDarkModeToggle: (Boolean?) -> Unit = {},
    isDarkModeEnabled: Boolean? = null,
    onNavigateToProfile: () -> Unit = {},
    onSignOutSuccess: () -> Unit
) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: HomeViewModel = viewModel(factory = ViewModelFactory(application))
    val uiState by viewModel.uiState.collectAsState()
    val view = LocalView.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val supabase = SupabaseClient.client

    // Track the last right-swiped company for referral navigation
    var lastRightSwipedCompany by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(message = error, duration = SnackbarDuration.Long)
        }
    }

    // Show referral finder prompt after right swipe
    LaunchedEffect(lastRightSwipedCompany) {
        lastRightSwipedCompany?.let { companyName ->
            val result = snackbarHostState.showSnackbar(
                message = "Find referral contacts at $companyName?",
                actionLabel = "Find Emails",
                duration = SnackbarDuration.Short
            )
            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                onNavigateToEmployeeFinder(companyName)
            }
            lastRightSwipedCompany = null
        }
    }

    val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val settingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var showUndoDialog by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showLogoutConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.showBottomSheet) {
        if (uiState.showBottomSheet) detailSheetState.show() else detailSheetState.hide()
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            HomeTopBar(
                stats = viewModel.getStats(),
                currentStreak = uiState.currentStreak,
                todaySwipeCount = uiState.todaySwipeCount,
                onUndo = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    viewModel.undoLastSwipe()
                },
                onSettingsClick = { showSettingsSheet = true }
            )

            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                when {
                    uiState.isLoading -> ShimmerCardStack()
                    uiState.error != null -> ErrorState(error = uiState.error!!, onRetry = { viewModel.refreshCards() })
                    uiState.isEmpty && !uiState.hasMorePages && !uiState.isLoadingMore -> {
                        EmptyState(interestedCount = uiState.interestedCards.size, onReset = { viewModel.resetCards() })
                    }
                    uiState.cards.isEmpty() && (uiState.hasMorePages || uiState.isLoadingMore) -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            CircularProgressIndicator(color = GradientStart, strokeWidth = 3.dp, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = "Loading more jobs...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    uiState.cards.isNotEmpty() -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (uiState.totalJobs > 0) {
                                Text(
                                    text = "${uiState.cards.size} of ${uiState.totalJobs} jobs remaining",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                            SwipeCardStack(
                                cards = uiState.cards,
                                onCardSwiped = { card, direction ->
                                    viewModel.onCardSwiped(card, direction)
                                    if (direction == SwipeDirection.RIGHT) {
                                        lastRightSwipedCompany = card.company.name
                                    }
                                },
                                onCardClicked = { card -> viewModel.selectCard(card) }
                            )
                            AnimatedVisibility(visible = uiState.isLoadingMore) {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(0.4f).height(2.dp).clip(RoundedCornerShape(1.dp)),
                                    color = GradientStart, trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }

                // Floating Action Buttons OVER the cards
                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState.cards.isNotEmpty() && !uiState.isLoading,
                    enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = spring(dampingRatio = 0.8f)),
                    exit = fadeOut(tween(200)) + slideOutVertically(targetOffsetY = { it / 2 }),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    ActionButtons(
                        onSkip = {
                            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                            uiState.cards.firstOrNull()?.let { viewModel.onCardSwiped(it, SwipeDirection.LEFT) }
                        },
                        onInterested = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            uiState.cards.firstOrNull()?.let { card ->
                                viewModel.onCardSwiped(card, SwipeDirection.RIGHT)
                                lastRightSwipedCompany = card.company.name
                            }
                        }
                    )
                }
            }
        }

        if (uiState.showBottomSheet && uiState.selectedCard != null) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.dismissBottomSheet() },
                sheetState = detailSheetState,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
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

        if (showSettingsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSettingsSheet = false },
                sheetState = settingsSheetState,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                SettingsSheet(
                    isDarkMode = isDarkModeEnabled ?: false,
                    onDarkModeToggle = { enabled ->
                        onDarkModeToggle(if (enabled) true else false)
                    },
                    isRankingEnabled = uiState.isRankingEnabled,
                    onToggleRanking = { viewModel.toggleRanking() },
                    isRanking = uiState.isRanking,
                    onNavigateToProfile = {
                        showSettingsSheet = false
                        onNavigateToProfile()
                    },
                    onUndoAll = {
                        if (uiState.undoHistory.isNotEmpty()) {
                            showSettingsSheet = false
                            showUndoDialog = true
                        }
                    },
                    undoCount = uiState.undoHistory.size,
                    onLogout = {
                        showSettingsSheet = false
                        showLogoutConfirmation = true
                    }
                )
            }
        }

        if (showLogoutConfirmation) {
            AlertDialog(
                onDismissRequest = { showLogoutConfirmation = false },
                title = { Text("Sign Out", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to sign out? Your swiped jobs will be saved.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLogoutConfirmation = false
                            scope.launch {
                                try {
                                    viewModel.performSignOut()
                                    com.swipeapply.app.data.repository.UserSyncRepository.clearSession()
                                    com.swipeapply.app.data.repository.SavedContactsRepository.clearAll()
                                    supabase.auth.signOut(scope = SignOutScope.LOCAL)
                                    delay(250L)
                                } catch (_: Exception) { } finally { onSignOutSuccess() }
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Sign Out") }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutConfirmation = false }) { Text("Cancel") }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(24.dp)
            )
        }

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

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        )
    }
}

@Composable
private fun HomeTopBar(
    stats: com.swipeapply.app.ui.viewmodel.SwipeStats,
    currentStreak: Int,
    todaySwipeCount: Int,
    onUndo: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val dailyGoal = com.swipeapply.app.data.manager.StreakManager.DAILY_GOAL
    val goalReached = todaySwipeCount >= dailyGoal
    val progress = (todaySwipeCount.toFloat() / dailyGoal).coerceIn(0f, 1f)

    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SwipeApply",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Streak pill — only shown once user has started a streak
                    AnimatedVisibility(
                        visible = currentStreak > 0,
                        enter = scaleIn(spring(dampingRatio = 0.6f)) + fadeIn(),
                        exit = scaleOut() + fadeOut()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (goalReached)
                                AccentGreen.copy(alpha = 0.15f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "\uD83D\uDD25",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = "$currentStreak",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (goalReached) AccentGreen else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "\u2022",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$todaySwipeCount/$dailyGoal",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (goalReached) AccentGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = stats.interested > 0,
                        enter = scaleIn(spring(dampingRatio = 0.6f)) + fadeIn(),
                        exit = scaleOut() + fadeOut()
                    ) {
                        StatBadge(count = stats.interested, color = AccentGreen)
                    }

                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Daily goal progress bar — subtle, only visible when streak is active
            AnimatedVisibility(
                visible = currentStreak > 0,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = if (goalReached) AccentGreen else GradientStart,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsSheet(
    isDarkMode: Boolean,
    onDarkModeToggle: (Boolean) -> Unit,
    isRankingEnabled: Boolean,
    onToggleRanking: () -> Unit,
    isRanking: Boolean,
    onNavigateToProfile: () -> Unit,
    onUndoAll: () -> Unit,
    undoCount: Int,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp).navigationBarsPadding()
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(24.dp))

        SettingsItem(
            icon = Icons.Default.Person,
            title = "My Profile",
            subtitle = "View and edit your profile",
            onClick = onNavigateToProfile
        )

        SettingsItem(
            icon = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
            title = "Dark Mode",
            subtitle = if (isDarkMode) "Currently on" else "Currently off",
            onClick = { onDarkModeToggle(!isDarkMode) },
            trailing = {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDarkMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = if (isDarkMode) "ON" else "OFF",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        )

        SettingsItem(
            icon = Icons.Default.AutoAwesome,
            title = "Smart Ranking",
            subtitle = "Sort jobs by match score",
            onClick = onToggleRanking,
            trailing = {
                if (isRanking) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isRankingEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = if (isRankingEnabled) "ON" else "OFF",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isRankingEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        )

        if (undoCount > 0) {
            SettingsItem(
                icon = Icons.Default.Refresh,
                title = "Undo History",
                subtitle = "$undoCount actions can be undone",
                onClick = onUndoAll
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        SettingsItem(
            icon = Icons.Default.ExitToApp,
            title = "Sign Out",
            subtitle = "Log out of your account",
            onClick = onLogout,
            tint = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    trailing: @Composable (() -> Unit)? = null
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = RoundedCornerShape(12.dp), color = tint.copy(alpha = 0.1f), modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = tint, fontWeight = FontWeight.Medium)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            trailing?.invoke()
        }
    }
}

@Composable
private fun StatBadge(count: Int, color: Color) {
    Surface(shape = RoundedCornerShape(10.dp), color = color.copy(alpha = 0.12f)) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(imageVector = Icons.Default.Favorite, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            AnimatedContent(
                targetState = count,
                transitionSpec = { (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { it } + fadeOut()) },
                label = "statCount"
            ) { targetCount ->
                Text(text = targetCount.toString(), style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ErrorState(error: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(32.dp)
    ) {
        Text(text = "⚠️", style = MaterialTheme.typography.displayLarge, modifier = Modifier.padding(bottom = 16.dp))
        Text(
            text = "Something went wrong",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
        ) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Try Again", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Check your internet connection and try again",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EmptyState(interestedCount: Int, onReset: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(32.dp)
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "empty")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(animation = tween(1200, easing = EaseInOutCubic), repeatMode = RepeatMode.Reverse),
            label = "scale"
        )
        Text(
            text = "🎉",
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "You're all caught up!",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (interestedCount > 0) {
                "You're interested in $interestedCount ${if (interestedCount == 1) "role" else "roles"}.\nTime to send some intros!"
            } else { "Check back later for new opportunities." },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedButton(onClick = onReset, shape = RoundedCornerShape(14.dp)) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Start over")
        }
    }
}

@Composable
private fun ActionButtons(onSkip: () -> Unit, onInterested: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp).padding(bottom = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ActionButton(icon = Icons.Default.Close, contentDescription = "Skip", containerColor = Color.White, contentColor = Color(0xFF657786), onClick = onSkip, modifier = Modifier.offset(y = (-16).dp))
        ActionButton(icon = Icons.Default.Bookmark, contentDescription = "Saved", containerColor = Color.White, contentColor = Color(0xFF2196F3), onClick = {}, isSmall = true, modifier = Modifier.offset(y = 8.dp))
        ActionButton(icon = Icons.Default.Favorite, contentDescription = "Interested", containerColor = Color.White, contentColor = Color(0xFFFF5252), onClick = onInterested, modifier = Modifier.offset(y = (-16).dp))
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    contentDescription: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLarge: Boolean = false,
    isSmall: Boolean = false
) {
    val size = if (isSmall) 48.dp else if (isLarge) 68.dp else 56.dp
    val iconSize = if (isSmall) 24.dp else if (isLarge) 32.dp else 28.dp
    FilledIconButton(
        onClick = onClick,
        modifier = modifier.size(size).shadow(elevation = 16.dp, shape = CircleShape, spotColor = contentColor.copy(alpha = 0.4f)),
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(containerColor = containerColor, contentColor = contentColor)
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, modifier = Modifier.size(iconSize))
    }
}

private fun extractDomain(url: String?): String? {
    if (url.isNullOrBlank()) return null
    return try {
        var host = url.lowercase().trim()
        if (host.startsWith("http://")) host = host.substring(7)
        if (host.startsWith("https://")) host = host.substring(8)
        val slashIndex = host.indexOf('/')
        if (slashIndex != -1) host = host.substring(0, slashIndex)
        if (host.startsWith("www.")) host = host.substring(4)
        if (host.isBlank()) null else host
    } catch (e: Exception) {
        null
    }
}

@Composable
private fun CompanyDetailSheet(jobCard: JobCard, onDismiss: () -> Unit, onRequestIntro: () -> Unit) {
    val domain = extractDomain(jobCard.company.website) ?: (jobCard.company.name.replace(" ", "").lowercase() + ".com")
    val displayUrl = "https://logos.hunter.io/$domain"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 500.dp)
            .padding(horizontal = 24.dp)
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = jobCard.company.name.take(2).uppercase(), style = MaterialTheme.typography.titleLarge, color = GradientStart, fontWeight = FontWeight.Bold)
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(displayUrl)
                                .crossfade(300)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                Column {
                    Text(text = jobCard.company.name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    Text(text = jobCard.company.industry, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "About the company", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = jobCard.company.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "The role", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = jobCard.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = jobCard.roleDescription, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(24.dp))
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Why this matches you", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = jobCard.matchReason, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(56.dp).background(
                brush = Brush.horizontalGradient(colors = listOf(GradientStart, GradientEnd)),
                shape = RoundedCornerShape(16.dp)
            ).clip(RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            TextButton(onClick = onRequestIntro, modifier = Modifier.fillMaxSize()) {
                Text(text = "Request intro template", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
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
        title = { Text(text = "Undo History", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = "${undoHistory.size} actions can be undone", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(undoHistory.reversed()) { action -> UndoHistoryItem(action = action) }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onUndoCount(undoHistory.size) }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)) { Text("Undo All") }
                if (undoHistory.size >= 5) {
                    TextButton(onClick = { onUndoCount(5) }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)) { Text("Undo 5") }
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onClearHistory, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Clear") }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun UndoHistoryItem(action: UndoableAction) {
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = action.card.company.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                Text(text = action.card.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(shape = RoundedCornerShape(8.dp), color = if (action.result.isInterested) AccentGreen.copy(alpha = 0.12f) else AccentRed.copy(alpha = 0.12f)) {
                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
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
