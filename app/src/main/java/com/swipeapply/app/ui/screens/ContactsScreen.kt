package com.swipeapply.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swipeapply.app.data.model.ConfidenceTier
import com.swipeapply.app.data.model.Employee
import com.swipeapply.app.data.repository.SavedCompanyContacts
import com.swipeapply.app.data.repository.SavedContactsRepository
import com.swipeapply.app.ui.theme.AccentGreen
import com.swipeapply.app.ui.theme.GradientEnd
import com.swipeapply.app.ui.theme.GradientStart
import com.swipeapply.app.ui.theme.LinkedInBlue

private val ContactCyan   = Color(0xFF06B6D4)
private val ContactPurple = Color(0xFF8B5CF6)
private val VerifiedEmerald = Color(0xFF10B981)
private val AmbientGold  = Color(0xFFF59E0B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    modifier: Modifier = Modifier
) {
    val companies by SavedContactsRepository.companies.collectAsState()
    val context = LocalContext.current

    // track which companies are expanded
    val expanded = remember { mutableStateListOf<String>() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ——— Top bar ———
        ContactsTopBar(totalSaved = companies.sumOf { it.contacts.size })

        if (companies.isEmpty()) {
            EmptyContactsState()
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(
                    items = companies,
                    key = { _, c -> c.companyName }
                ) { index, company ->
                    val isExpanded = company.companyName in expanded

                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(250, delayMillis = index * 50)) +
                                slideInVertically(
                                    initialOffsetY = { it / 3 },
                                    animationSpec = spring(
                                        dampingRatio = 0.8f,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                    ) {
                        CompanyContactCard(
                            company = company,
                            isExpanded = isExpanded,
                            onToggle = {
                                if (isExpanded) expanded.remove(company.companyName)
                                else expanded.add(company.companyName)
                            },
                            onCopyEmail = { email -> copyToClipboardC(context, email) },
                            onSendEmail = { email -> sendEmailC(context, email) },
                            onOpenLinkedIn = { url -> openUrlC(context, url) },
                            onDeleteCompany = {
                                expanded.remove(company.companyName)
                                SavedContactsRepository.removeCompany(company.companyName)
                            }
                        )
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
// Top Bar
// ════════════════════════════════════════════════════════════

@Composable
private fun ContactsTopBar(totalSaved: Int) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Contacts",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Your saved referral contacts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Badge
            AnimatedVisibility(
                visible = totalSaved > 0,
                enter = scaleIn(spring(dampingRatio = 0.6f)) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(ContactCyan, ContactPurple)
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = totalSaved,
                        transitionSpec = {
                            (slideInVertically { -it } + fadeIn()) togetherWith
                                    (slideOutVertically { it } + fadeOut())
                        },
                        label = "totalBadge"
                    ) { count ->
                        Text(
                            text = "$count contacts",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

// ════════════════════════════════════════════════════════════
// Company Card (collapsible)
// ════════════════════════════════════════════════════════════

@Composable
private fun CompanyContactCard(
    company: SavedCompanyContacts,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onCopyEmail: (String) -> Unit,
    onSendEmail: (String) -> Unit,
    onOpenLinkedIn: (String) -> Unit,
    onDeleteCompany: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = if (isExpanded) 8.dp else 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // ─── Header row (always visible) ───
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onToggle() }
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Company logo placeholder – gradient circle
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(listOf(ContactCyan, ContactPurple))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = company.companyName
                            .split(" ")
                            .take(2)
                            .joinToString("") { it.firstOrNull()?.uppercase() ?: "" }
                            .take(2),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = company.companyName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = ContactCyan
                        )
                        Text(
                            text = "${company.contacts.size} ${if (company.contacts.size == 1) "contact" else "contacts"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ContactCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Delete button
                IconButton(
                    onClick = onDeleteCompany,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Expand chevron
                val chevronRotation by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (isExpanded) 180f else 0f,
                    animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f),
                    label = "chevron"
                )
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer { rotationZ = chevronRotation }
                )
            }

            // ─── Expanded contact list ───
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(tween(200)) + slideInVertically(
                    initialOffsetY = { -it / 4 },
                    animationSpec = spring(dampingRatio = 0.8f)
                ),
                exit = fadeOut(tween(150)) + slideOutVertically(
                    targetOffsetY = { -it / 4 }
                )
            ) {
                Column {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        modifier = Modifier.padding(horizontal = 18.dp)
                    )
                    company.contacts.forEachIndexed { idx, employee ->
                        ContactListItem(
                            employee = employee,
                            onCopyEmail = { onCopyEmail(employee.email) },
                            onSendEmail = { onSendEmail(employee.email) },
                            onOpenLinkedIn = { employee.linkedinUrl?.let { onOpenLinkedIn(it) } }
                        )
                        if (idx < company.contacts.lastIndex) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                                modifier = Modifier.padding(horizontal = 18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
// Individual Contact Row
// ════════════════════════════════════════════════════════════

@Composable
private fun ContactListItem(
    employee: Employee,
    onCopyEmail: () -> Unit,
    onSendEmail: () -> Unit,
    onOpenLinkedIn: () -> Unit
) {
    val tier = employee.getConfidenceTier()
    val tierColor = when (tier) {
        ConfidenceTier.HIGH -> VerifiedEmerald
        ConfidenceTier.MEDIUM -> AmbientGold
        ConfidenceTier.LOW -> MaterialTheme.colorScheme.error
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ContactCyan.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = employee.getInitials(),
                    style = MaterialTheme.typography.titleSmall,
                    color = ContactCyan,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                // Name + confidence
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = employee.fullName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    // Confidence pill
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = tierColor.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = if (tier == ConfidenceTier.HIGH)
                                    Icons.Default.Verified else Icons.Default.Shield,
                                contentDescription = null,
                                modifier = Modifier.size(9.dp),
                                tint = tierColor
                            )
                            Text(
                                text = "${employee.confidence}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = tierColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        }
                    }
                }

                // Job title
                Text(
                    text = "${employee.jobTitle ?: "Employee"}${employee.department?.let { " · $it" } ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Email row
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = Color(0xFF3B82F6)
                )
                Text(
                    text = employee.email,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Copy
                IconButton(
                    onClick = onCopyEmail,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy email",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Send email
                Surface(
                    onClick = onSendEmail,
                    shape = CircleShape,
                    color = Color(0xFF3B82F6).copy(alpha = 0.12f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "Send email",
                            modifier = Modifier.size(13.dp),
                            tint = Color(0xFF3B82F6)
                        )
                    }
                }

                // LinkedIn
                if (!employee.linkedinUrl.isNullOrBlank()) {
                    Surface(
                        onClick = onOpenLinkedIn,
                        shape = CircleShape,
                        color = LinkedInBlue.copy(alpha = 0.12f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "in",
                                style = MaterialTheme.typography.labelSmall,
                                color = LinkedInBlue,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
// Empty State
// ════════════════════════════════════════════════════════════

@Composable
private fun EmptyContactsState() {
    val infiniteTransition = rememberInfiniteTransition(label = "emptyAnim")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp)
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer { scaleX = pulse; scaleY = pulse }
                    .clip(RoundedCornerShape(36.dp))
                    .background(
                        Brush.linearGradient(listOf(ContactCyan, ContactPurple))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "No saved contacts yet",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Swipe right on a job card, then save\nemployee contacts to find referrals.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Instructional steps
            listOf(
                "1" to "Swipe right on a job",
                "2" to "Tap \"Find Emails\" in the prompt",
                "3" to "Save contacts from the card stack"
            ).forEach { (num, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(ContactCyan, ContactPurple))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = num,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
// Utility (local copies to avoid conflicts with EmployeeFinderScreen)
// ════════════════════════════════════════════════════════════

private fun copyToClipboardC(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("email", text))
    Toast.makeText(context, "Copied ✓", Toast.LENGTH_SHORT).show()
}

private fun sendEmailC(context: Context, email: String) {
    try {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$email")
            putExtra(Intent.EXTRA_SUBJECT, "Referral Request")
        }
        context.startActivity(Intent.createChooser(intent, "Send email via…"))
    } catch (_: Exception) {
        Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
    }
}

private fun openUrlC(context: Context, url: String) {
    try {
        val fullUrl = if (url.startsWith("http")) url else "https://$url"
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl)))
    } catch (_: Exception) {
        Toast.makeText(context, "Couldn't open link", Toast.LENGTH_SHORT).show()
    }
}
