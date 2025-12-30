package com.swipeapply.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swipeapply.app.data.model.IntroTemplate
import com.swipeapply.app.data.model.JobCard
import com.swipeapply.app.ui.theme.*
import com.swipeapply.app.ui.viewmodel.IntroTemplateViewModel

/**
 * Intro template preview screen.
 * Displays editable email template with copy functionality.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntroTemplateScreen(
    jobCard: JobCard,
    onBack: () -> Unit,
    viewModel: IntroTemplateViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current
    val scrollState = rememberScrollState()
    
    // Initialize template on first composition
    LaunchedEffect(jobCard) {
        viewModel.initializeTemplate(
            subject = jobCard.introTemplate.subject,
            body = jobCard.introTemplate.body
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Intro Template",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundLight
                )
            )
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // Company context card
            CompanyContextCard(jobCard = jobCard)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Email preview card
            EmailPreviewCard(
                subject = uiState.subject,
                body = uiState.body,
                onSubjectChange = { viewModel.updateSubject(it) },
                onBodyChange = { viewModel.updateBody(it) }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Copy button
            CopyButton(
                isCopied = uiState.isCopied,
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    copyToClipboard(context, viewModel.getFullEmail())
                    viewModel.markAsCopied()
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Disclaimer
            Text(
                text = "You will send this manually via your email or LinkedIn.",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp).navigationBarsPadding())
        }
    }
}

@Composable
private fun CompanyContextCard(jobCard: JobCard) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = BackgroundSecondary
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Company logo
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = GradientStart.copy(alpha = 0.1f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = jobCard.company.name.take(2).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = GradientStart,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = jobCard.company.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = jobCard.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun EmailPreviewCard(
    subject: String,
    body: String,
    onSubjectChange: (String) -> Unit,
    onBodyChange: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = BackgroundCard,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = CardShadow
            )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Subject line
            Text(
                text = "Subject",
                style = MaterialTheme.typography.labelMedium,
                color = TextTertiary
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = BackgroundSecondary
            ) {
                BasicTextField(
                    value = subject,
                    onValueChange = onSubjectChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(GradientStart),
                    singleLine = true
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Body
            Text(
                text = "Message",
                style = MaterialTheme.typography.labelMedium,
                color = TextTertiary
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = BackgroundSecondary
            ) {
                BasicTextField(
                    value = body,
                    onValueChange = onBodyChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp)
                        .padding(14.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = TextPrimary
                    ),
                    cursorBrush = SolidColor(GradientStart)
                )
            }
        }
    }
}

@Composable
private fun CopyButton(
    isCopied: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isCopied) AccentGreen else Primary
        )
    ) {
        AnimatedVisibility(
            visible = isCopied,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Copied!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        
        AnimatedVisibility(
            visible = !isCopied,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Copy email",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Intro Email", text)
    clipboard.setPrimaryClip(clip)
}
