package com.swipeapply.app.ui.components.swipe

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Work
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.swipeapply.app.data.model.JobCard
import com.swipeapply.app.data.model.SwipeDirection
import com.swipeapply.app.ui.components.TechStackIcons
import com.swipeapply.app.ui.theme.AccentGreen
import com.swipeapply.app.ui.theme.BadgeHiring
import com.swipeapply.app.ui.theme.BadgeHiringBackground
import com.swipeapply.app.ui.theme.GradientEnd
import com.swipeapply.app.ui.theme.GradientStart

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JobSwipeCard(
    jobCard: JobCard,
    swipeProgress: Float,
    swipeDirection: SwipeDirection,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = when (swipeDirection) {
            SwipeDirection.RIGHT -> AccentGreen.copy(alpha = swipeProgress * 0.9f)
            SwipeDirection.LEFT -> MaterialTheme.colorScheme.error.copy(alpha = swipeProgress * 0.9f)
            SwipeDirection.NONE -> Color.Transparent
        },
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 500f),
        label = "borderColor"
    )

    val cardShape = RoundedCornerShape(32.dp)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.80f)
            .clip(cardShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = if (swipeProgress > 0.05f) BorderStroke(3.dp, borderColor) else null
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Hero Header with Company Info
                CardHeader(jobCard)

                // Main Content Area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 22.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(18.dp))
                    
                    // Quick Info Row
                    QuickInfoRow(jobCard)
                    
                    Spacer(modifier = Modifier.height(18.dp))
                    
                    // Role Description
                    Text(
                        text = "About the Role",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = jobCard.roleDescription.cleanHtml(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )
                    
                    Spacer(modifier = Modifier.height(22.dp))
                    
                    // Tech Stack
                    if (jobCard.techStack.isNotEmpty()) {
                        Text(
                            text = "Tech Stack",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TechStackChips(jobCard.techStack)
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Bottom gradient fade
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            )
                        )
                )
            }

            // Swipe Overlay
            SwipeOverlay(swipeDirection, swipeProgress)
        }
    }
}

@Composable
private fun CardHeader(jobCard: JobCard) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        GradientStart.copy(alpha = 0.08f),
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
                // Company Logo
                CompanyLogo(jobCard.company.name, jobCard.company.logoUrl)
                
                // Badges Row
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (jobCard.isHiringNow) {
                        HiringBadge()
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Company Name
            Text(
                text = jobCard.company.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Job Title
            Text(
                text = jobCard.title,
                style = MaterialTheme.typography.titleLarge,
                color = GradientStart,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 26.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Location Row - avoid duplicating "Remote"
            val locationText = jobCard.location
            val locationType = jobCard.locationType.label
            val showLocationChip = !locationText.equals(locationType, ignoreCase = true) && 
                                   !locationText.contains(locationType, ignoreCase = true)
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = locationText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (showLocationChip) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = locationType,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickInfoRow(jobCard: JobCard) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Industry chip
        QuickInfoChip(
            icon = Icons.Default.Work,
            text = jobCard.company.industry.take(20)
        )
    }
}

@Composable
private fun QuickInfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun String.cleanHtml(): String {
    return this.replace(Regex("<[^>]*>"), "")
        .replace("&amp;", "&")
        .replace("&nbsp;", " ")
        .replace("&#x2F;", "/")
        .replace("&quot;", "\"")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .trim()
}

@Composable
private fun HiringBadge() {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = BadgeHiringBackground
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(BadgeHiring, CircleShape)
            )
            Text(
                text = "Hiring",
                style = MaterialTheme.typography.labelMedium,
                color = BadgeHiring,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CompanyLogo(
    companyName: String,
    logoUrl: String? = null
) {
    val initials = companyName.split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .joinToString("")

    Box(
        modifier = Modifier
            .size(64.dp)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(18.dp), spotColor = GradientStart)
            .clip(RoundedCornerShape(18.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(GradientStart, GradientEnd)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!logoUrl.isNullOrEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(logoUrl)
                    .crossfade(300)
                    .build(),
                contentDescription = "$companyName logo",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = initials,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TechStackChips(techStack: List<String>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        techStack.take(8).forEach { tech ->
            val techIcon = TechStackIcons.getIcon(tech)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = techIcon.icon,
                        contentDescription = null,
                        tint = techIcon.color,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = tech,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun SwipeOverlay(
    direction: SwipeDirection,
    progress: Float
) {
    if (progress > 0.05f) {
        val overlayAlpha = (progress * 0.95f).coerceAtMost(1f)
        val scale by animateFloatAsState(
            targetValue = 0.9f + (progress * 0.1f),
            animationSpec = spring(dampingRatio = 0.7f),
            label = "scale"
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
                        border = BorderStroke(
                            width = 3.dp,
                            color = AccentGreen.copy(alpha = overlayAlpha)
                        ),
                        modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = AccentGreen.copy(alpha = overlayAlpha),
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "INTERESTED",
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
                            width = 3.dp,
                            color = MaterialTheme.colorScheme.error.copy(alpha = overlayAlpha)
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
