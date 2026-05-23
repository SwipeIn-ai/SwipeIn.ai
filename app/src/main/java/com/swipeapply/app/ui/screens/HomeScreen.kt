package com.swipeapply.app.ui.screens

import android.app.Application
import android.text.format.DateUtils
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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.swipeapply.app.SupabaseClient
import com.swipeapply.app.data.manager.GroqConsentManager
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
import com.swipeapply.app.ui.viewmodel.SwipeHistoryEntry
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
    onNavigateToEmployeeFinder: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    onDarkModeToggle: (Boolean?) -> Unit = {},
    isDarkModeEnabled: Boolean? = null,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToPrivacyPolicy: () -> Unit = {},
    onNavigateToTerms: () -> Unit = {},
    onSignOutSuccess: () -> Unit
) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: HomeViewModel = viewModel(factory = ViewModelFactory(application))
    val uiState by viewModel.uiState.collectAsState()
    val view = LocalView.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val supabase = SupabaseClient.client
    val groqConsentManager = remember(application) { GroqConsentManager.getInstance(application) }

    // Track the last right-swiped company for referral navigation
    var lastRightSwipedCompany by remember { mutableStateOf<String?>(null) }
    var isGroqConsentEnabled by remember { mutableStateOf(groqConsentManager.hasOutreachConsent()) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(message = error, duration = SnackbarDuration.Long)
        }
    }

    // Show referral finder prompt after right swipe
    LaunchedEffect(lastRightSwipedCompany) {
        lastRightSwipedCompany?.let { companyName ->
            val result = snackbarHostState.showSnackbar(
                message = "Finding employees at $companyName...",
                actionLabel = "View Contacts",
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
    val activitySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var showUndoDialog by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showActivitySheet by remember { mutableStateOf(false) }
    var showLogoutConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(showSettingsSheet) {
        if (showSettingsSheet) {
            isGroqConsentEnabled = groqConsentManager.hasOutreachConsent()
        }
    }

    LaunchedEffect(uiState.showBottomSheet) {
        if (uiState.showBottomSheet) detailSheetState.show() else detailSheetState.hide()
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            HomeTopBar(
                onUndo = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    viewModel.undoLastSwipe()
                },
                onActivityClick = { showActivitySheet = true },
                onSettingsClick = { showSettingsSheet = true }
            )

            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                when {
                    uiState.isLoading -> ShimmerCardStack()
                    uiState.error != null -> ErrorState(error = uiState.error ?: "Unknown error", onRetry = { viewModel.refreshCards() })
                    uiState.isEmpty && !uiState.hasMorePages && !uiState.isLoadingMore -> {
                        EmptyState(
                            interestedCount = uiState.swipeHistory.count { it.isInterested },
                            onReset = { viewModel.resetCards() }
                        )
                    }
                    uiState.cards.isEmpty() && (uiState.hasMorePages || uiState.isLoadingMore) -> {
                        ShimmerCardStack()
                    }
                    uiState.cards.isNotEmpty() -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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

                // Swipe hint — shown only when cards are visible
                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState.cards.isNotEmpty() && !uiState.isLoading && uiState.swipeHistory.isEmpty(),
                    enter = fadeIn(tween(600)),
                    exit = fadeOut(tween(400)),
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                    ) {
                        Text(
                            text = "← Not Interested  ·  Interested →",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

            }
        }

        // Floating action buttons — rendered outside Column so they float over everything
        androidx.compose.animation.AnimatedVisibility(
            visible = uiState.cards.isNotEmpty() && !uiState.isLoading,
            enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = spring(dampingRatio = 0.8f)),
            exit = fadeOut(tween(200)) + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
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

        if (uiState.showBottomSheet && uiState.selectedCard != null) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.dismissBottomSheet() },
                sheetState = detailSheetState,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                uiState.selectedCard?.let { card ->
                    CompanyDetailSheet(
                        jobCard = card,
                        onDismiss = { viewModel.dismissBottomSheet() },
                        onFindPeople = {
                            viewModel.dismissBottomSheet()
                            onNavigateToEmployeeFinder(card.company.name)
                        }
                    )
                }
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
                    isGroqConsentEnabled = isGroqConsentEnabled,
                    onGroqConsentToggle = { enabled ->
                        if (enabled) {
                            groqConsentManager.grantOutreachConsent()
                        } else {
                            groqConsentManager.revokeOutreachConsent()
                        }
                        isGroqConsentEnabled = enabled
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = if (enabled) {
                                    "AI personalization enabled."
                                } else {
                                    "AI personalization disabled."
                                },
                                duration = SnackbarDuration.Short
                            )
                        }
                    },
                    isRankingEnabled = uiState.isRankingEnabled,
                    onToggleRanking = { viewModel.toggleRanking() },
                    isRanking = uiState.isRanking,
                    onNavigateToProfile = {
                        showSettingsSheet = false
                        onNavigateToProfile()
                    },
                    onNavigateToPrivacyPolicy = {
                        showSettingsSheet = false
                        onNavigateToPrivacyPolicy()
                    },
                    onNavigateToTerms = {
                        showSettingsSheet = false
                        onNavigateToTerms()
                    },
                    onDeleteAccount = {
                        showSettingsSheet = false
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

        if (showActivitySheet) {
            ModalBottomSheet(
                onDismissRequest = { showActivitySheet = false },
                sheetState = activitySheetState,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                SwipeActivitySheet(
                    history = uiState.swipeHistory,
                    onDismiss = { showActivitySheet = false },
                    onJobSelected = { card ->
                        showActivitySheet = false
                        viewModel.selectCard(card)
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
private fun HomeQuickFilters(
    activeFilter: HomeQuickFilter,
    cards: List<JobCard>,
    onFilterSelected: (HomeQuickFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HomeQuickFilter.entries.forEach { filter ->
            val count = when (filter) {
                HomeQuickFilter.ALL -> cards.size
                HomeQuickFilter.TECH_STACK -> cards.count { it.techStack.isNotEmpty() }
            }
            FilterChip(
                selected = activeFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text("${filter.label} ($count)") }
            )
        }
    }
}

@Composable
private fun FilterEmptyState(
    activeFilter: HomeQuickFilter,
    onReset: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(32.dp)
    ) {
        Text(
            text = "No jobs match ${activeFilter.label.lowercase()} right now.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Try another quick filter or jump back to the full queue.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(18.dp))
        OutlinedButton(onClick = onReset) {
            Text("Show all jobs")
        }
    }
}

@Composable
private fun HomeTopBar(
    onUndo: () -> Unit,
    onActivityClick: () -> Unit,
    onSettingsClick: () -> Unit
) {

    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = buildAnnotatedString {
                        append("Swipe")
                        withStyle(style = SpanStyle(color = Color(0xFF0A66C2))) {
                            append("In")
                        }
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {

                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSheet(
    isDarkMode: Boolean,
    onDarkModeToggle: (Boolean) -> Unit,
    isGroqConsentEnabled: Boolean,
    onGroqConsentToggle: (Boolean) -> Unit,
    isRankingEnabled: Boolean,
    onToggleRanking: () -> Unit,
    isRanking: Boolean,
    onNavigateToProfile: () -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit = {},
    onNavigateToTerms: () -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    onUndoAll: () -> Unit,
    undoCount: Int,
    onLogout: () -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val supportContext = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 32.dp).navigationBarsPadding()
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
            icon = Icons.Default.Lock,
            title = "AI Personalization",
            subtitle = if (isGroqConsentEnabled) {
                "Personalized outreach messages enabled"
            } else {
                "Personalized outreach messages disabled"
            },
            onClick = { onGroqConsentToggle(!isGroqConsentEnabled) },
            trailing = {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isGroqConsentEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = if (isGroqConsentEnabled) "ON" else "OFF",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isGroqConsentEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
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

        // ── Legal & Support ──
        SettingsItem(
            icon = Icons.Default.Lock,
            title = "Privacy Policy",
            subtitle = "How we handle your data",
            onClick = onNavigateToPrivacyPolicy
        )

        SettingsItem(
            icon = Icons.Default.Star,
            title = "Terms of Service",
            subtitle = "Rules and guidelines",
            onClick = onNavigateToTerms
        )

        SettingsItem(
            icon = Icons.Default.Email,
            title = "Contact Support",
            subtitle = "swipein.ai@gmail.com",
            onClick = {
                try {
                    val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                        data = android.net.Uri.parse("mailto:swipein.ai@gmail.com")
                        putExtra(android.content.Intent.EXTRA_SUBJECT, "SwipeIn Support Request")
                    }
                    supportContext.startActivity(intent)
                } catch (_: Exception) { }
            }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // ── Account Actions ──
        SettingsItem(
            icon = Icons.Default.ExitToApp,
            title = "Sign Out",
            subtitle = "Log out of your account",
            onClick = onLogout
        )

        SettingsItem(
            icon = Icons.Default.Close,
            title = "Delete Account",
            subtitle = "Permanently remove your data",
            onClick = { showDeleteConfirmation = true },
            tint = MaterialTheme.colorScheme.error
        )
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Account", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to delete your account? This will permanently remove " +
                    "all your data including saved jobs, swipe history, and profile information. " +
                    "This action cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete Account") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) { Text("Cancel") }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 64.dp).padding(bottom = 28.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ActionButton(icon = Icons.Default.Close, contentDescription = "Not Interested", containerColor = MaterialTheme.colorScheme.surface, contentColor = AccentRed, onClick = onSkip, isLarge = true)
        ActionButton(icon = Icons.Default.Favorite, contentDescription = "Interested", containerColor = MaterialTheme.colorScheme.surface, contentColor = Color(0xFFFD297B), onClick = onInterested, isLarge = true)
    }
}

private enum class ActivityFilter {
    ALL,
    LIKED,
    DISLIKED
}

private enum class HomeQuickFilter(val label: String) {
    ALL("All"),
    TECH_STACK("Tech stack")
}

@Composable
private fun SwipeActivitySheet(
    history: List<SwipeHistoryEntry>,
    onDismiss: () -> Unit,
    onJobSelected: (JobCard) -> Unit
) {
    var filter by remember { mutableStateOf(ActivityFilter.ALL) }
    val likedCount = history.count { it.isInterested }
    val skippedCount = history.count { it.direction == SwipeDirection.LEFT }
    val filteredHistory = remember(history, filter) {
        when (filter) {
            ActivityFilter.ALL -> history
            ActivityFilter.LIKED -> history.filter { it.isInterested }
            ActivityFilter.DISLIKED -> history.filter { it.direction == SwipeDirection.LEFT }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Swipe Activity",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$likedCount interested • $skippedCount passed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilterChip(
                selected = filter == ActivityFilter.ALL,
                onClick = { filter = ActivityFilter.ALL },
                label = { Text("All (${history.size})") }
            )
            FilterChip(
                selected = filter == ActivityFilter.LIKED,
                onClick = { filter = ActivityFilter.LIKED },
                label = { Text("Interested ($likedCount)") }
            )
            FilterChip(
                selected = filter == ActivityFilter.DISLIKED,
                onClick = { filter = ActivityFilter.DISLIKED },
                label = { Text("Passed ($skippedCount)") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredHistory.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (history.isEmpty()) "Start swiping to build your activity feed." else "No jobs match this filter yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredHistory, key = { "${it.card.id}-${it.timestamp}" }) { item ->
                    SwipeActivityItem(item = item, onClick = { onJobSelected(item.card) })
                }
            }
        }
    }
}

@Composable
private fun SwipeActivityItem(
    item: SwipeHistoryEntry,
    onClick: () -> Unit
) {
    val accentColor = if (item.isInterested) AccentGreen else AccentRed
    val accentBackground = if (item.isInterested) AccentGreenLight else AccentRedLight

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.card.company.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = item.card.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = accentBackground.copy(alpha = 0.35f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (item.isInterested) Icons.Default.Favorite else Icons.Default.Close,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (item.isInterested) "Interested" else "Passed",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = accentColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = item.card.location,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            item.applicationStatus?.let { status ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Status: ${status.label}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (item.card.techStack.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = item.card.techStack.joinToString(" • "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = DateUtils.getRelativeTimeSpanString(item.timestamp).toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
    val size = if (isSmall) 48.dp else if (isLarge) 64.dp else 56.dp
    val iconSize = if (isSmall) 24.dp else if (isLarge) 30.dp else 28.dp
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
private fun CompanyDetailSheet(jobCard: JobCard, onDismiss: () -> Unit, onFindPeople: () -> Unit) {
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
        // Single primary action: Find Emails → intro → referral template
        Box(
            modifier = Modifier.fillMaxWidth().height(56.dp).background(
                brush = Brush.horizontalGradient(colors = listOf(GradientStart, GradientEnd)),
                shape = RoundedCornerShape(16.dp)
            ).clip(RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            TextButton(onClick = onFindPeople, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Find Emails & Send Intro",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
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
                        text = if (action.result.isInterested) "Interested" else "Passed",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (action.result.isInterested) AccentGreen else AccentRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
