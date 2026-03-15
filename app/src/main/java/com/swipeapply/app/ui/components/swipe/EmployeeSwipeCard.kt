package com.swipeapply.app.ui.components.swipe

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swipeapply.app.data.model.ConfidenceTier
import com.swipeapply.app.data.model.Employee
import com.swipeapply.app.data.model.SwipeDirection
import com.swipeapply.app.ui.theme.AccentGreen
import com.swipeapply.app.ui.theme.GradientEnd
import com.swipeapply.app.ui.theme.GradientStart
import com.swipeapply.app.ui.theme.LinkedInBlue

// Referral card accent colors
private val ReferralGradientStart = Color(0xFF06B6D4) // Cyan
private val ReferralGradientEnd = Color(0xFF8B5CF6)   // Purple
private val EmailAccent = Color(0xFF3B82F6)            // Blue
private val VerifiedGreen = Color(0xFF10B981)          // Emerald
private val WarningAmber = Color(0xFFF59E0B)           // Amber
private val GoldStar = Color(0xFFFBBF24)               // Gold

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmployeeSwipeCard(
    employee: Employee,
    companyName: String,
    swipeProgress: Float = 0f,
    swipeDirection: SwipeDirection = SwipeDirection.NONE,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = when (swipeDirection) {
            SwipeDirection.RIGHT -> AccentGreen.copy(alpha = swipeProgress * 0.9f)
            SwipeDirection.LEFT -> MaterialTheme.colorScheme.error.copy(alpha = swipeProgress * 0.9f)
            SwipeDirection.NONE -> Color.Transparent
        },
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 500f),
        label = "empBorderColor"
    )

    val cardShape = RoundedCornerShape(32.dp)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxSize()
            .clip(cardShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        border = if (swipeProgress > 0.05f) BorderStroke(3.dp, borderColor) else null
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Premium Header
                EmployeeCardHeader(employee = employee, companyName = companyName)

                // Content Area
                Box(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 22.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Spacer(modifier = Modifier.height(20.dp))

                        // Email Section - The Star of the Show
                        EmailSection(employee = employee)

                        Spacer(modifier = Modifier.height(20.dp))

                        // Info Grid
                        InfoGrid(employee = employee)

                        Spacer(modifier = Modifier.height(20.dp))

                        // Confidence Meter
                        ConfidenceMeter(employee = employee)

                        // LinkedIn Section
                        if (!employee.linkedinUrl.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            LinkedInSection(url = employee.linkedinUrl!!)
                        }

                        Spacer(modifier = Modifier.height(100.dp)) // Padding for action buttons
                    }

                    // Bottom Gradient
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface)
                                )
                            )
                    )
                }
            }

            // Swipe Overlays
            EmployeeSwipeOverlay(swipeDirection, swipeProgress)
        }
    }
}

@Composable
private fun EmployeeCardHeader(
    employee: Employee,
    companyName: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        ReferralGradientStart.copy(alpha = 0.12f),
                        ReferralGradientEnd.copy(alpha = 0.05f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Avatar
                EmployeeAvatar(employee = employee)

                // Referral Badge
                ReferralBadge()
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Full Name
            Text(
                text = employee.fullName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Job Title
            Text(
                text = employee.jobTitle ?: "Employee",
                style = MaterialTheme.typography.titleLarge,
                color = ReferralGradientStart,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 26.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Company + Location Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Business,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = companyName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val location = employee.getFormattedLocation()
                if (location != "Location unknown") {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = location,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmployeeAvatar(employee: Employee) {
    val infiniteTransition = rememberInfiniteTransition(label = "avatarGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = Modifier
            .size(68.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = ReferralGradientStart.copy(alpha = glowAlpha)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(ReferralGradientStart, ReferralGradientEnd)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = employee.getInitials(),
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun ReferralBadge() {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ReferralGradientStart.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = ReferralGradientStart
            )
            Text(
                text = "Referral",
                style = MaterialTheme.typography.labelMedium,
                color = ReferralGradientStart,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun EmailSection(employee: Employee) {
    val tier = employee.getConfidenceTier()
    val statusColor = when (tier) {
        ConfidenceTier.HIGH -> VerifiedGreen
        ConfidenceTier.MEDIUM -> WarningAmber
        ConfidenceTier.LOW -> MaterialTheme.colorScheme.error
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = EmailAccent.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, EmailAccent.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📧 Email Contact",
                    style = MaterialTheme.typography.labelLarge,
                    color = EmailAccent,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                // Verification badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (tier == ConfidenceTier.HIGH) Icons.Default.Verified 
                                         else Icons.Default.Shield,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = statusColor
                        )
                        Text(
                            text = tier.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // The Email Address - Big and Prominent
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = EmailAccent
                    )
                    Text(
                        text = employee.email,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy email",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InfoGrid(employee: Employee) {
    Text(
        text = "Details",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp
    )

    Spacer(modifier = Modifier.height(12.dp))

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Department
        if (!employee.department.isNullOrBlank()) {
            InfoChip(
                icon = Icons.Default.Work,
                label = "Department",
                value = employee.department!!,
                accentColor = ReferralGradientEnd
            )
        }

        // Seniority
        if (!employee.seniority.isNullOrBlank()) {
            InfoChip(
                icon = Icons.Default.Star,
                label = "Seniority",
                value = employee.seniority!!,
                accentColor = GoldStar
            )
        }
    }
}

@Composable
private fun InfoChip(
    icon: ImageVector,
    label: String,
    value: String,
    accentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = accentColor.copy(alpha = 0.08f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = accentColor
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ConfidenceMeter(employee: Employee) {
    val confidence = employee.confidence
    val tier = employee.getConfidenceTier()
    val meterColor = when (tier) {
        ConfidenceTier.HIGH -> VerifiedGreen
        ConfidenceTier.MEDIUM -> WarningAmber
        ConfidenceTier.LOW -> MaterialTheme.colorScheme.error
    }

    val animatedProgress by animateFloatAsState(
        targetValue = confidence / 100f,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "confidenceProgress"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Email Confidence",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                AnimatedContent(
                    targetState = confidence,
                    transitionSpec = {
                        (slideInVertically { -it } + fadeIn()) togetherWith
                                (slideOutVertically { it } + fadeOut())
                    },
                    label = "confidenceValue"
                ) { targetConfidence ->
                    Text(
                        text = "$targetConfidence%",
                        style = MaterialTheme.typography.titleMedium,
                        color = meterColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    meterColor.copy(alpha = 0.7f),
                                    meterColor
                                )
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun LinkedInSection(url: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = LinkedInBlue.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, LinkedInBlue.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = LinkedInBlue.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "in",
                        style = MaterialTheme.typography.titleMedium,
                        color = LinkedInBlue,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "LinkedIn Profile",
                    style = MaterialTheme.typography.labelMedium,
                    color = LinkedInBlue,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = url.removePrefix("https://").removePrefix("www.").take(40),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = "Open LinkedIn",
                modifier = Modifier.size(18.dp),
                tint = LinkedInBlue
            )
        }
    }
}

@Composable
private fun EmployeeSwipeOverlay(
    direction: SwipeDirection,
    progress: Float
) {
    if (progress > 0.05f) {
        val overlayAlpha = (progress * 0.95f).coerceAtMost(1f)
        val scale by animateFloatAsState(
            targetValue = 0.9f + (progress * 0.1f),
            animationSpec = spring(dampingRatio = 0.7f),
            label = "empOverlayScale"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            contentAlignment = when (direction) {
                SwipeDirection.RIGHT -> Alignment.TopEnd
                SwipeDirection.LEFT -> Alignment.TopStart
                SwipeDirection.NONE -> Alignment.Center
            }
        ) {
            when (direction) {
                SwipeDirection.RIGHT -> {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = AccentGreen.copy(alpha = overlayAlpha * 0.15f),
                        border = BorderStroke(3.dp, AccentGreen.copy(alpha = overlayAlpha)),
                        modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = AccentGreen.copy(alpha = overlayAlpha),
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "SAVE",
                                style = MaterialTheme.typography.titleMedium,
                                color = AccentGreen.copy(alpha = overlayAlpha),
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.5.sp
                            )
                        }
                    }
                }
                SwipeDirection.LEFT -> {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = overlayAlpha * 0.15f),
                        border = BorderStroke(
                            3.dp,
                            MaterialTheme.colorScheme.error.copy(alpha = overlayAlpha)
                        ),
                        modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
                    ) {
                        Text(
                            text = "SKIP",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error.copy(alpha = overlayAlpha),
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                        )
                    }
                }
                SwipeDirection.NONE -> { }
            }
        }
    }
}
