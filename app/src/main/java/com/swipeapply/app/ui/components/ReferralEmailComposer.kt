package com.swipeapply.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swipeapply.app.data.model.Employee
import com.swipeapply.app.ui.theme.GradientStart
import com.swipeapply.app.ui.viewmodel.ReferralEmailUiState
import com.swipeapply.app.ui.viewmodel.ReferralEmailViewModel
import com.swipeapply.app.ui.viewmodel.ViewModelFactory

// Brand colors
private val GmailRed = Color(0xFFEA4335)
private val ComposerBlue = Color(0xFF1A73E8)
private val ComposerCyan = Color(0xFF06B6D4)
private val SuccessGreen = Color(0xFF10B981)

/**
 * Bottom Sheet content for composing and sending a cold intro email.
 * 
 * Features:
 * - AI-generated personalized template
 * - Editable From, To, Subject, Body fields
 * - Direct Gmail integration
 * - Copy to clipboard fallback
 */
@Composable
fun ReferralEmailComposerContent(
    employee: Employee,
    companyName: String,
    jobTitle: String? = null,
    onDismiss: () -> Unit,
    onEmailSent: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val application = context.applicationContext as android.app.Application
    val viewModel: ReferralEmailViewModel = viewModel(
        key = "referral_${employee.email}", // Unique key per employee
        factory = ViewModelFactory(application)
    )
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val showEmailSkeleton = uiState.isGenerating && uiState.subject.isBlank() && uiState.body.isBlank()

    // Initialize on composition
    LaunchedEffect(employee.email) {
        viewModel.initialize(employee, companyName, jobTitle)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
    ) {
        // Header with AI badge and regenerate button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Compose Intro",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                // AI badge
                AnimatedVisibility(
                    visible = uiState.aiGenerated,
                    enter = fadeIn() + scaleIn()
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = GradientStart.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = GradientStart,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                "AI",
                                style = MaterialTheme.typography.labelSmall,
                                color = GradientStart,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Regenerate button
            IconButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    viewModel.regenerate()
                },
                enabled = !uiState.isGenerating && !uiState.requiresGroqConsent
            ) {
                if (uiState.isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = GradientStart
                    )
                } else {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = "Regenerate with AI",
                        tint = GradientStart
                    )
                }
            }
        }

        if (uiState.requiresGroqConsent) {
            GroqConsentScreen(
                onAllow = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    viewModel.grantGroqConsent()
                },
                onDecline = {
                    viewModel.declineGroqConsent()
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
            return@Column
        }

        // Recipient info card
        RecipientInfoCard(
            employee = employee,
            companyName = companyName,
            jobTitle = jobTitle
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Error banner
        AnimatedVisibility(
            visible = uiState.generationError != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Text(
                    text = uiState.generationError ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Email composer form
        if (showEmailSkeleton) {
            ShimmerEmailCard()
        } else {
            EmailComposerForm(
                uiState = uiState,
                onFromNameChange = viewModel::updateFromName,
                onFromEmailChange = viewModel::updateFromEmail,
                onSubjectChange = viewModel::updateSubject,
                onBodyChange = viewModel::updateBody,
                onToggleFrom = viewModel::toggleFromField,
                isGenerating = uiState.isGenerating
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action buttons
        ActionButtons(
            isValid = uiState.isValid(),
            onSend = {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                openEmailApp(context, uiState.toReferralEmail())
                viewModel.markEmailSent()
                onEmailSent()
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Helper text
        Text(
            text = when {
                uiState.aiGenerated -> "Edit freely before sending."
                uiState.isGenerating -> "Generating customized draft...This may take a moment."
                else -> "No draft yet. Tap regenerate to create an AI template."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun RecipientInfoCard(
    employee: Employee,
    companyName: String,
    jobTitle: String?
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar
            Surface(
                shape = CircleShape,
                color = ComposerCyan.copy(alpha = 0.12f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = employee.getInitials(),
                        style = MaterialTheme.typography.titleSmall,
                        color = ComposerCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

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
                    text = "${employee.jobTitle ?: "Employee"} at $companyName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (jobTitle != null) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = ComposerBlue.copy(alpha = 0.1f),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = "Role: $jobTitle",
                            style = MaterialTheme.typography.labelSmall,
                            color = ComposerBlue,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmailComposerForm(
    uiState: ReferralEmailUiState,
    onFromNameChange: (String) -> Unit,
    onFromEmailChange: (String) -> Unit,
    onSubjectChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onToggleFrom: () -> Unit,
    isGenerating: Boolean
) {
    // Shimmer border when generating
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )
    val borderColor = if (isGenerating) GradientStart.copy(alpha = shimmerAlpha) else Color.Transparent

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, borderColor, RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // From field (collapsible)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleFrom() }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "From",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!uiState.showFromField && uiState.fromName.isNotBlank()) {
                        Text(
                            text = uiState.fromName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                Icon(
                    if (uiState.showFromField) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Toggle",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Expandable From fields
            AnimatedVisibility(
                visible = uiState.showFromField,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(bottom = 10.dp)) {
                    CompactTextField(
                        label = "Your Name",
                        value = uiState.fromName,
                        onValueChange = onFromNameChange,
                        placeholder = "Your name"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CompactTextField(
                        label = "Your Email",
                        value = uiState.fromEmail,
                        onValueChange = onFromEmailChange,
                        placeholder = "your@email.com"
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // To field (read-only display)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Email,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = ComposerBlue
                )
                Text(
                    text = "To",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${uiState.toName} <${uiState.toEmail}>",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Subject
            Text(
                text = "Subject",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                BasicTextField(
                    value = uiState.subject,
                    onValueChange = onSubjectChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(ComposerBlue),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (uiState.subject.isEmpty()) {
                            Text(
                                "Enter subject",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                        innerTextField()
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Body
            Text(
                text = "Message",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                BasicTextField(
                    value = uiState.body,
                    onValueChange = onBodyChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp, max = 300.dp)
                        .padding(12.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    ),
                    cursorBrush = SolidColor(ComposerBlue),
                    decorationBox = { innerTextField ->
                        if (uiState.body.isEmpty()) {
                            Text(
                                "Type your message...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }
    }
}

@Composable
private fun CompactTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 3.dp)
        )
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(ComposerBlue),
                singleLine = true,
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

@Composable
private fun ActionButtons(
    isValid: Boolean,
    onSend: () -> Unit
) {
    Button(
        onClick = onSend,
        enabled = isValid,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        contentPadding = PaddingValues(),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = if (isValid) {
                        Brush.horizontalGradient(listOf(Color(0xFF0A66C2), Color(0xFF06B6D4)))
                    } else {
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                            )
                        )
                    },
                    shape = RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    tint = if (isValid) Color.White
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Send Referral Email",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isValid) Color.White
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
        }
    }
}

// ─── Utilities ────────────────────────────────────────────────────────────────

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("referral_email", text))
    Toast.makeText(context, "Email copied!", Toast.LENGTH_SHORT).show()
}

private fun openEmailApp(
    context: Context,
    email: com.swipeapply.app.data.model.ReferralEmail
) {
    try {
        // Try Gmail first
        val gmailIntent = email.toGmailIntent()
        if (isAppInstalled(context, "com.google.android.gm")) {
            context.startActivity(gmailIntent)
            return
        }

        // Fallback to generic email
        val genericIntent = email.toEmailIntent()
        context.startActivity(Intent.createChooser(genericIntent, "Send email via..."))
    } catch (e: Exception) {
        try {
            val mailtoIntent = Intent(Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse(email.toMailtoUri())
            }
            context.startActivity(mailtoIntent)
        } catch (e2: Exception) {
            Toast.makeText(context, "No email app found. Email copied instead!", Toast.LENGTH_LONG).show()
            copyToClipboard(context, email.toFullText())
        }
    }
}

private fun isAppInstalled(context: Context, packageName: String): Boolean {
    return try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}
