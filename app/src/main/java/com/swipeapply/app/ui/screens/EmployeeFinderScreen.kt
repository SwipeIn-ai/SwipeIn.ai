package com.swipeapply.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Verified
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swipeapply.app.data.model.ConfidenceTier
import com.swipeapply.app.data.model.Employee
import com.swipeapply.app.ui.components.ReferralEmailComposerContent
import com.swipeapply.app.ui.components.ShimmerCardStack
import com.swipeapply.app.ui.components.swipe.EmployeeCardStack
import com.swipeapply.app.ui.theme.AccentGreen
import com.swipeapply.app.ui.theme.AccentGreenLight
import com.swipeapply.app.ui.theme.AccentRed
import com.swipeapply.app.ui.theme.AccentRedLight
import com.swipeapply.app.ui.theme.LinkedInBlue
import com.swipeapply.app.ui.viewmodel.EmployeeFinderViewModel

// Referral color scheme
private val ReferralCyan = Color(0xFF06B6D4)
private val ReferralPurple = Color(0xFF8B5CF6)
private val VerifiedGreen = Color(0xFF10B981)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeFinderScreen(
    companyName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: EmployeeFinderViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val view = LocalView.current
    val context = LocalContext.current

    // Detail bottom sheet
    val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val emailComposerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSavedSheet by remember { mutableStateOf(false) }
    var selectedEmployee by remember { mutableStateOf<Employee?>(null) }
    var emailTargetEmployee by remember { mutableStateOf<Employee?>(null) }

    // Load employees when screen opens
    LaunchedEffect(companyName) {
        viewModel.loadEmployees(companyName)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            EmployeeFinderTopBar(
                companyName = companyName,
                savedCount = uiState.savedContacts.size,
                onBack = onBack,
                onViewSaved = { showSavedSheet = true },
                onUndo = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    viewModel.undoLast()
                }
            )

            // Progress Indicator
            AnimatedVisibility(
                visible = uiState.employees.isNotEmpty() && !uiState.allReviewed,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                LinearProgressIndicator(
                    progress = { uiState.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = ReferralCyan,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }

            // Main Content Area
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                when {
                    uiState.isLoading -> {
                        LoadingState()
                    }
                    uiState.error != null && uiState.employees.isEmpty() -> {
                        ErrorState(
                            error = uiState.error!!,
                            onRetry = { viewModel.loadEmployees(companyName) }
                        )
                    }
                    uiState.isEmpty -> {
                        EmptyEmployeeState(companyName = companyName, onBack = onBack)
                    }
                    uiState.allReviewed -> {
                        AllReviewedState(
                            savedCount = uiState.savedContacts.size,
                            onViewSaved = { showSavedSheet = true },
                            onReset = { viewModel.resetReview() },
                            onBack = onBack
                        )
                    }
                    uiState.visibleEmployees.isNotEmpty() -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Swipe hint for first-time users
                            androidx.compose.animation.AnimatedVisibility(
                                visible = uiState.reviewedCount == 0,
                                enter = fadeIn(tween(600)),
                                exit = fadeOut(tween(400))
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                ) {
                                    Text(
                                        text = "← Pass  ·  Connect →",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            }

                            EmployeeCardStack(
                            employees = uiState.visibleEmployees,
                            companyName = companyName,
                            onSaveContact = { employee ->
                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                viewModel.saveContact()
                                emailTargetEmployee = employee
                            },
                            onDismissContact = { employee ->
                                view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                                viewModel.dismissContact()
                            },
                            onCardClicked = { employee ->
                                selectedEmployee = employee
                            }
                        )
                        }
                    }
                }

            }
        }

        // Floating action buttons — rendered outside Column so they float over everything
        androidx.compose.animation.AnimatedVisibility(
            visible = uiState.visibleEmployees.isNotEmpty() && !uiState.allReviewed && !uiState.isLoading,
            enter = fadeIn(tween(300)) + slideInVertically(
                initialOffsetY = { it / 2 },
                animationSpec = spring(dampingRatio = 0.8f)
            ),
            exit = fadeOut(tween(200)) + slideOutVertically(
                targetOffsetY = { it / 2 }
            ),
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
        ) {
            EmployeeActionButtons(
                onDismiss = {
                    view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                    viewModel.dismissContact()
                },
                onSave = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    val topEmployee = uiState.visibleEmployees.firstOrNull()
                    viewModel.saveContact()
                    if (topEmployee != null) emailTargetEmployee = topEmployee
                }
            )
        }

        // Employee Detail Bottom Sheet
        if (selectedEmployee != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedEmployee = null },
                sheetState = detailSheetState,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                EmployeeDetailSheet(
                    employee = selectedEmployee!!,
                    companyName = companyName,
                    onDismiss = { selectedEmployee = null },
                    onCopyEmail = { email ->
                        copyToClipboard(context, email)
                    },
                    onOpenLinkedIn = { url ->
                        openUrl(context, url)
                    },
                    onSendReferralEmail = { employee ->
                        selectedEmployee = null
                        emailTargetEmployee = employee
                    }
                )
            }
        }

        // Saved Contacts Bottom Sheet
        if (showSavedSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSavedSheet = false },
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                SavedContactsSheet(
                    contacts = uiState.savedContacts,
                    companyName = companyName,
                    onDismiss = { showSavedSheet = false },
                    onCopyEmail = { email -> copyToClipboard(context, email) },
                    onOpenLinkedIn = { url -> openUrl(context, url) },
                    onSendEmail = { employee -> 
                        showSavedSheet = false
                        emailTargetEmployee = employee
                    }
                )
            }
        }

        // Referral Email Composer Bottom Sheet
        if (emailTargetEmployee != null) {
            ModalBottomSheet(
                onDismissRequest = { emailTargetEmployee = null },
                sheetState = emailComposerSheetState,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                ReferralEmailComposerContent(
                    employee = emailTargetEmployee!!,
                    companyName = companyName,
                    jobTitle = null, // Could be passed from job context if available
                    onDismiss = { emailTargetEmployee = null },
                    onEmailSent = { 
                        emailTargetEmployee = null
                        // Could show success snackbar here
                    }
                )
            }
        }
    }
}

// ========== Top Bar ==========

@Composable
private fun EmployeeFinderTopBar(
    companyName: String,
    savedCount: Int,
    onBack: () -> Unit,
    onViewSaved: () -> Unit,
    onUndo: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button + Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column {
                    Text(
                        text = "Referral Contacts",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = companyName,
                        style = MaterialTheme.typography.bodySmall,
                        color = ReferralCyan,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Undo
                IconButton(onClick = onUndo) {
                    Icon(
                        imageVector = Icons.Default.Undo,
                        contentDescription = "Undo",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Saved count badge
                AnimatedVisibility(
                    visible = savedCount > 0,
                    enter = scaleIn(spring(dampingRatio = 0.6f)) + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Surface(
                        onClick = onViewSaved,
                        shape = RoundedCornerShape(10.dp),
                        color = VerifiedGreen.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = VerifiedGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            AnimatedContent(
                                targetState = savedCount,
                                transitionSpec = {
                                    (slideInVertically { -it } + fadeIn()) togetherWith
                                            (slideOutVertically { it } + fadeOut())
                                },
                                label = "savedCount"
                            ) { count ->
                                Text(
                                    text = count.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = VerifiedGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ========== Action Buttons ==========

@Composable
private fun EmployeeActionButtons(
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 64.dp)
            .padding(bottom = 28.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Dismiss
        FilledIconButton(
            onClick = onDismiss,
            modifier = Modifier
                .size(64.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    spotColor = MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                ),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Pass",
                modifier = Modifier.size(30.dp)
            )
        }

        // Connect / Right Swipe
        FilledIconButton(
            onClick = onSave,
            modifier = Modifier
                .size(64.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    spotColor = Color(0xFFFD297B).copy(alpha = 0.5f)
                ),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = Color(0xFFFD297B)
            )
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Connect",
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

// ========== State Screens ==========

@Composable
private fun LoadingState() {
    val infiniteTransition = rememberInfiniteTransition(label = "loadingAnim")

    // Outer ring rotation
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing)),
        label = "outerRing"
    )
    // Inner ring counter-rotation
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing)),
        label = "innerRing"
    )
    // Center pulse
    val centerPulse by infiniteTransition.animateFloat(
        initialValue = 0.88f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ),
        label = "centerPulse"
    )
    // Glow pulse
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f, targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            tween(1400, easing = EaseInOutCubic), RepeatMode.Reverse
        ),
        label = "glow"
    )
    // Cycling hint text
    val phases = listOf(
        "🔍  Searching databases…",
        "📊  Verifying emails…",
        "🔗  Checking LinkedIn…",
        "✨  Almost ready…"
    )
    val phaseIndex by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = phases.size.toFloat(),
        animationSpec = infiniteRepeatable(tween(phases.size * 1500, easing = FastOutSlowInEasing)),
        label = "phaseIndex"
    )
    val phase = phases[phaseIndex.toInt().coerceIn(0, phases.lastIndex)]

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ─── Animated rings cluster ───
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(180.dp)
            ) {
                // Outer glow
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    ReferralCyan.copy(alpha = glowAlpha),
                                    ReferralPurple.copy(alpha = glowAlpha * 0.5f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Outer rotating ring
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .size(150.dp)
                        .graphicsLayer { rotationZ = outerRotation }
                ) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(Color.Transparent, ReferralCyan, Color.Transparent)
                        ),
                        startAngle = -90f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 4.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    )
                }

                // Inner counter-rotating ring
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .size(110.dp)
                        .graphicsLayer { rotationZ = innerRotation }
                ) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(Color.Transparent, ReferralPurple, Color.Transparent)
                        ),
                        startAngle = -90f,
                        sweepAngle = 200f,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 3.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    )
                }

                // Center pulsing icon
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .graphicsLayer { scaleX = centerPulse; scaleY = centerPulse }
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(listOf(ReferralCyan, ReferralPurple))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Heading
            Text(
                text = "Finding Employees",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Cycling phase text
            AnimatedContent(
                targetState = phase,
                transitionSpec = {
                    (fadeIn(tween(400)) + slideInVertically { it / 2 }) togetherWith
                            (fadeOut(tween(300)) + slideOutVertically { -it / 2 })
                },
                label = "phaseText"
            ) { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Shimmer progress bar
            val shimmerProgress by infiniteTransition.animateFloat(
                initialValue = 0f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(1800)),
                label = "shimmerBar"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(shimmerProgress)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(ReferralCyan, ReferralPurple)
                            )
                        )
                )
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
        Text(
            text = "😔",
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "Couldn't find contacts",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ReferralCyan),
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Try Again", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun EmptyEmployeeState(companyName: String, onBack: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(32.dp)
    ) {
        Text(
            text = "🔍",
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "No contacts found",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "We couldn't find employee contacts for $companyName. Try exploring other companies!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(
            onClick = onBack,
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Back to Jobs")
        }
    }
}

@Composable
private fun AllReviewedState(
    savedCount: Int,
    onViewSaved: () -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(32.dp)
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "doneAnim")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = EaseInOutCubic),
                repeatMode = RepeatMode.Reverse
            ),
            label = "doneScale"
        )

        Text(
            text = if (savedCount > 0) "🎯" else "✅",
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "All contacts reviewed!",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (savedCount > 0)
                "You saved $savedCount ${if (savedCount == 1) "contact" else "contacts"} for referrals!"
            else "No contacts were saved. You can review again or go back.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(28.dp))

        if (savedCount > 0) {
            Button(
                onClick = onViewSaved,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                contentPadding = PaddingValues(),
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(50.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(ReferralCyan, ReferralPurple)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "View Saved Contacts",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onReset,
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Review Again")
            }
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Back to Jobs")
            }
        }
    }
}

// ========== Detail Bottom Sheet ==========

@Composable
private fun EmployeeDetailSheet(
    employee: Employee,
    companyName: String,
    onDismiss: () -> Unit,
    onCopyEmail: (String) -> Unit,
    onOpenLinkedIn: (String) -> Unit,
    onSendReferralEmail: (Employee) -> Unit
) {
    val BrandPrimary = Color(0xFF0A66C2)
    val BrandSecondary = Color(0xFFE8F3FF)
    val BrandForeground = Color(0xFF1A1D21)
    val BrandMutedForeground = Color(0xFF666E76)
    val BrandBorder = Color(0xFFDEE2E6)
    val tier = employee.getConfidenceTier()
    val tierColor = when (tier) {
        ConfidenceTier.HIGH -> VerifiedGreen
        ConfidenceTier.MEDIUM -> Color(0xFFF59E0B)
        ConfidenceTier.LOW -> MaterialTheme.colorScheme.error
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Avatar ──
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    Brush.linearGradient(listOf(BrandSecondary, BrandPrimary.copy(alpha = 0.15f))),
                    CircleShape
                )
                .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = employee.getInitials(),
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = BrandPrimary,
                letterSpacing = (-0.5).sp
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = employee.fullName,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = BrandForeground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = employee.jobTitle ?: "Professional",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = BrandMutedForeground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = BrandSecondary,
            border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.15f))
        ) {
            Text(
                text = companyName,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandPrimary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = BrandBorder.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(20.dp))

        // ── Email Row ──
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = BrandPrimary
                )
                Text(
                    text = employee.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = tierColor.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        if (tier == ConfidenceTier.HIGH) {
                            Icon(Icons.Default.Verified, null, modifier = Modifier.size(10.dp), tint = tierColor)
                        }
                        Text(
                            text = "${employee.confidence}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = tierColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                IconButton(
                    onClick = { onCopyEmail(employee.email) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy email",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ── LinkedIn Row ──
        if (!employee.linkedinUrl.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = LinkedInBlue.copy(alpha = 0.06f),
                border = BorderStroke(1.dp, LinkedInBlue.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth(),
                onClick = { onOpenLinkedIn(employee.linkedinUrl!!) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = LinkedInBlue.copy(alpha = 0.15f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text("in", style = MaterialTheme.typography.labelMedium, color = LinkedInBlue, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    Text(
                        text = "View LinkedIn Profile",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LinkedInBlue,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(16.dp), tint = LinkedInBlue)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Primary CTA ──
        Surface(
            onClick = { onSendReferralEmail(employee) },
            shape = RoundedCornerShape(16.dp),
            color = BrandPrimary,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(Icons.Default.Email, null, tint = Color.White, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Compose Intro Email",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// ========== Saved Contacts Sheet ==========

@Composable
private fun SavedContactsSheet(
    contacts: List<Employee>,
    companyName: String,
    onDismiss: () -> Unit,
    onCopyEmail: (String) -> Unit,
    onOpenLinkedIn: (String) -> Unit,
    onSendEmail: (Employee) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
            .navigationBarsPadding()
    ) {
        Text(
            text = "Saved Contacts",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${contacts.size} contacts from $companyName",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (contacts.isEmpty()) {
            Text(
                text = "No contacts saved yet. Swipe right to save contacts!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                items(contacts) { employee ->
                    SavedContactItem(
                        employee = employee,
                        onCopyEmail = { onCopyEmail(employee.email) },
                        onSendEmail = { onSendEmail(employee) },
                        onOpenLinkedIn = {
                            employee.linkedinUrl?.let { onOpenLinkedIn(it) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedContactItem(
    employee: Employee,
    onCopyEmail: () -> Unit,
    onSendEmail: () -> Unit,
    onOpenLinkedIn: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ReferralCyan.copy(alpha = 0.12f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = employee.getInitials(),
                            style = MaterialTheme.typography.titleSmall,
                            color = ReferralCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = employee.fullName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = employee.jobTitle ?: "Employee",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Confidence badge
                val tier = employee.getConfidenceTier()
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (tier) {
                        ConfidenceTier.HIGH -> VerifiedGreen.copy(alpha = 0.12f)
                        ConfidenceTier.MEDIUM -> Color(0xFFF59E0B).copy(alpha = 0.12f)
                        ConfidenceTier.LOW -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        if (tier == ConfidenceTier.HIGH) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                modifier = Modifier.size(10.dp),
                                tint = VerifiedGreen
                            )
                        }
                        Text(
                            text = "${employee.confidence}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = when (tier) {
                                ConfidenceTier.HIGH -> VerifiedGreen
                                ConfidenceTier.MEDIUM -> Color(0xFFF59E0B)
                                ConfidenceTier.LOW -> MaterialTheme.colorScheme.error
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Email + actions row
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF3B82F6)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = employee.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Copy button
                    IconButton(
                        onClick = onCopyEmail,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Email",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Send email button
                    IconButton(
                        onClick = onSendEmail,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "Send Email",
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF3B82F6)
                        )
                    }

                    // LinkedIn button
                    if (!employee.linkedinUrl.isNullOrBlank()) {
                        IconButton(
                            onClick = onOpenLinkedIn,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text(
                                text = "in",
                                style = MaterialTheme.typography.labelSmall,
                                color = LinkedInBlue,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ========== Utility Functions ==========

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("email", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Email copied to clipboard!", Toast.LENGTH_SHORT).show()
}

private fun openUrl(context: Context, url: String) {
    try {
        val fullUrl = if (url.startsWith("http")) url else "https://$url"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Couldn't open link", Toast.LENGTH_SHORT).show()
    }
}

private fun sendEmail(context: Context, email: String) {
    try {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$email")
            putExtra(Intent.EXTRA_SUBJECT, "Referral Request")
        }
        context.startActivity(Intent.createChooser(intent, "Send email via..."))
    } catch (e: Exception) {
        Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
    }
}
