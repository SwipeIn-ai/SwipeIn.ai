package com.swipeapply.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.swipeapply.app.ui.theme.AccentGreen
import com.swipeapply.app.ui.theme.GradientEnd
import com.swipeapply.app.ui.theme.GradientStart
import com.swipeapply.app.ui.viewmodel.ReferralEmailViewModel
import com.swipeapply.app.ui.viewmodel.ViewModelFactory

// Brand colors for the email composer
private val GmailRed = Color(0xFFEA4335)
private val ComposerBlue = Color(0xFF1A73E8)
private val ComposerCyan = Color(0xFF06B6D4)
private val SuccessGreen = Color(0xFF10B981)

/**
 * Professional Cold Referral Email Composer Screen.
 * 
 * Features:
 * - AI-generated personalized referral template
 * - Editable From, To, Subject, and Body fields
 * - Direct "Open in Gmail" integration
 * - Copy to clipboard option
 * - Real-time validation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferralEmailComposerScreen(
    employee: Employee,
    companyName: String,
    jobTitle: String? = null,
    onBack: () -> Unit,
    onEmailSent: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val application = context.applicationContext as android.app.Application
    val viewModel: ReferralEmailViewModel = viewModel(factory = ViewModelFactory(application))
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    var isCopied by remember { mutableStateOf(false) }

    // Initialize on first composition
    LaunchedEffect(employee.email) {
        viewModel.initialize(employee, companyName, jobTitle)
    }

    // Reset copied state after delay
    LaunchedEffect(isCopied) {
        if (isCopied) {
            kotlinx.coroutines.delay(2000)
            isCopied = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Compose Referral",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
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
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Regenerate AI button
                    IconButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            viewModel.regenerate()
                        },
                        enabled = !uiState.isGenerating
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            // Recipient Context Card
            RecipientContextCard(
                employee = employee,
                companyName = companyName,
                jobTitle = jobTitle,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Error banner
            AnimatedVisibility(
                visible = uiState.generationError != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
                modifier = Modifier.padding(horizontal = 20.dp)
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

            // Email Composer Card
            EmailComposerCard(
                uiState = uiState,
                onFromNameChange = viewModel::updateFromName,
                onFromEmailChange = viewModel::updateFromEmail,
                onToNameChange = viewModel::updateToName,
                onToEmailChange = viewModel::updateToEmail,
                onSubjectChange = viewModel::updateSubject,
                onBodyChange = viewModel::updateBody,
                onToggleFrom = viewModel::toggleFromField,
                isGenerating = uiState.isGenerating,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            ActionButtonsRow(
                isValid = uiState.isValid(),
                isCopied = isCopied,
                onCopy = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    copyEmailToClipboard(context, viewModel.getFullEmailText())
                    isCopied = true
                },
                onOpenGmail = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    openEmailClient(context, uiState.toReferralEmail())
                    viewModel.markEmailSent()
                    onEmailSent()
                },
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Helper text
            Text(
                text = if (uiState.aiGenerated) 
                    "✨ Personalized using your profile. Edit freely before sending."
                else 
                    "Edit the template above, then send via your email app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp).navigationBarsPadding())
        }
    }
}

// ─── Recipient Context Card ───────────────────────────────────────────────────

@Composable
private fun RecipientContextCard(
    employee: Employee,
    companyName: String,
    jobTitle: String?,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Avatar
            Surface(
                shape = CircleShape,
                color = ComposerCyan.copy(alpha = 0.12f),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = employee.getInitials(),
                        style = MaterialTheme.typography.titleMedium,
                        color = ComposerCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = employee.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = employee.jobTitle ?: "Employee",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ComposerBlue.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = companyName,
                            style = MaterialTheme.typography.labelSmall,
                            color = ComposerBlue,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    if (jobTitle != null) {
                        Text(
                            text = "•",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            fontSize = 10.sp
                        )
                        Text(
                            text = jobTitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// ─── Email Composer Card ──────────────────────────────────────────────────────

@Composable
private fun EmailComposerCard(
    uiState: com.swipeapply.app.ui.viewmodel.ReferralEmailUiState,
    onFromNameChange: (String) -> Unit,
    onFromEmailChange: (String) -> Unit,
    onToNameChange: (String) -> Unit,
    onToEmailChange: (String) -> Unit,
    onSubjectChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onToggleFrom: () -> Unit,
    isGenerating: Boolean,
    modifier: Modifier = Modifier
) {
    // Shimmer border animation when generating
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
    val borderColor = if (isGenerating) {
        GradientStart.copy(alpha = shimmerAlpha)
    } else {
        Color.Transparent
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )
            .border(1.5.dp, borderColor, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // From field (collapsible)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleFrom() }
                    .padding(vertical = 8.dp),
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
                        modifier = Modifier.size(18.dp),
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
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                Icon(
                    if (uiState.showFromField) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Toggle from field",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Expandable From fields
            AnimatedVisibility(
                visible = uiState.showFromField,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(bottom = 12.dp)) {
                    EmailTextField(
                        label = "Your Name",
                        value = uiState.fromName,
                        onValueChange = onFromNameChange,
                        placeholder = "John Doe"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    EmailTextField(
                        label = "Your Email",
                        value = uiState.fromEmail,
                        onValueChange = onFromEmailChange,
                        placeholder = "john@email.com"
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // To field
            EmailFieldRow(
                label = "To",
                value = "${uiState.toName} <${uiState.toEmail}>",
                icon = Icons.Default.Email,
                isEditable = false,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Subject
            Text(
                text = "Subject",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                BasicTextField(
                    value = uiState.subject,
                    onValueChange = onSubjectChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(ComposerBlue),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (uiState.subject.isEmpty()) {
                            Text(
                                "Enter subject line",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                        innerTextField()
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Body
            Text(
                text = "Message",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                BasicTextField(
                    value = uiState.body,
                    onValueChange = onBodyChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 220.dp, max = 400.dp)
                        .padding(14.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    ),
                    cursorBrush = SolidColor(ComposerBlue),
                    decorationBox = { innerTextField ->
                        if (uiState.body.isEmpty()) {
                            Text(
                                "Type your message here...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
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
private fun EmailTextField(
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
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(ComposerBlue),
                singleLine = true,
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
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

@Composable
private fun EmailFieldRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isEditable: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = ComposerBlue
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ─── Action Buttons ───────────────────────────────────────────────────────────

@Composable
private fun ActionButtonsRow(
    isValid: Boolean,
    isCopied: Boolean,
    onCopy: () -> Unit,
    onOpenGmail: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Primary: Open in Gmail button
        Button(
            onClick = onOpenGmail,
            enabled = isValid,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent
            ),
            contentPadding = PaddingValues(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = if (isValid) {
                            Brush.horizontalGradient(listOf(GmailRed, Color(0xFFFF6B6B)))
                        } else {
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                )
                            )
                        },
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        tint = if (isValid) Color.White 
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Send Referral",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isValid) Color.White 
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
            }
        }

        // Secondary: Copy to clipboard
        OutlinedButton(
            onClick = onCopy,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (isCopied) SuccessGreen.copy(alpha = 0.08f) 
                                else Color.Transparent
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (isCopied) SuccessGreen else MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (isCopied) "Copied to Clipboard!" else "Copy Email Text",
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCopied) SuccessGreen else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ─── Utility Functions ────────────────────────────────────────────────────────

private fun copyEmailToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("referral_email", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Email copied to clipboard!", Toast.LENGTH_SHORT).show()
}

private fun openEmailClient(
    context: Context,
    email: com.swipeapply.app.data.model.ReferralEmail
) {
    try {
        // First try Gmail specifically
        val gmailIntent = email.toGmailIntent()
        if (isPackageInstalled(context, "com.google.android.gm")) {
            context.startActivity(gmailIntent)
            return
        }
        
        // Fallback to generic email intent
        val genericIntent = email.toEmailIntent()
        context.startActivity(Intent.createChooser(genericIntent, "Send email via..."))
    } catch (e: Exception) {
        // Last resort: try mailto URI
        try {
            val mailtoIntent = Intent(Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse(email.toMailtoUri())
            }
            context.startActivity(mailtoIntent)
        } catch (e2: Exception) {
            Toast.makeText(context, "No email app found. Email copied instead!", Toast.LENGTH_LONG).show()
            copyEmailToClipboard(context, email.toFullText())
        }
    }
}

private fun isPackageInstalled(context: Context, packageName: String): Boolean {
    return try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}
