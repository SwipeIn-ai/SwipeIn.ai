package com.swipeapply.app.ui.components.swipe

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.swipeapply.app.data.model.JobCard
import com.swipeapply.app.data.model.SwipeDirection
import com.swipeapply.app.ui.components.TechStackIcons
import com.swipeapply.app.ui.theme.*

/**
 * Visual job card for the swipe experience.
 * Shows company info, job details, and swipe feedback overlays.
 */
@Composable
fun JobSwipeCard(
    jobCard: JobCard,
    swipeProgress: Float,
    swipeDirection: SwipeDirection,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animate border color based on swipe direction
    val borderColor by animateColorAsState(
        targetValue = when (swipeDirection) {
            SwipeDirection.RIGHT -> AccentGreen.copy(alpha = swipeProgress)
            SwipeDirection.LEFT -> AccentRed.copy(alpha = swipeProgress)
            SwipeDirection.NONE -> Color.Transparent
        },
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "borderColor"
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.7f)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = CardShadow
            )
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 3.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = BackgroundCard
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top section - Company info
                Column {
                    // Hiring badge
                    if (jobCard.isHiringNow) {
                        HiringBadge()
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // Company logo placeholder
                    CompanyLogo(companyName = jobCard.company.name)
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Company name
                    Text(
                        text = jobCard.company.name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Job title
                    Text(
                        text = jobCard.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextSecondary
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Location
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LocationChip(
                            location = jobCard.location,
                            locationType = jobCard.locationType.label
                        )
                    }
                    
                    // Salary (if available)
                    jobCard.salary?.let { salary ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = salary.formatted(),
                            style = MaterialTheme.typography.titleMedium,
                            color = ChipTextAccent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                // Bottom section - Tech stack
                Column {
                    Text(
                        text = "Tech Stack",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextTertiary
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    TechStackChips(techStack = jobCard.techStack)
                }
            }
            
            // Swipe feedback overlays
            SwipeOverlay(
                direction = swipeDirection,
                progress = swipeProgress
            )
        }
    }
}

@Composable
private fun HiringBadge() {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = BadgeHiringBackground
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(BadgeHiring, CircleShape)
            )
            Text(
                text = "Hiring now",
                style = MaterialTheme.typography.labelMedium,
                color = BadgeHiring,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun CompanyLogo(companyName: String) {
    // Generate initials from company name
    val initials = companyName.split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .joinToString("")
    
    Box(
        modifier = Modifier
            .size(72.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(GradientStart, GradientEnd)
                ),
                shape = RoundedCornerShape(16.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LocationChip(
    location: String,
    locationType: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = ChipBackground
        ) {
            Text(
                text = location,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
        
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = ChipBackgroundAccent
        ) {
            Text(
                text = locationType,
                style = MaterialTheme.typography.bodyMedium,
                color = ChipTextAccent,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun TechStackChips(techStack: List<String>) {
    // Use FlowRow-like layout with wrapping
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val chunkedStack = techStack.chunked(2)
        chunkedStack.forEach { rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { tech ->
                    val techIcon = TechStackIcons.getIcon(tech)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BackgroundSecondary
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
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
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
    if (progress > 0.1f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = when (direction) {
                SwipeDirection.RIGHT -> Alignment.TopEnd
                SwipeDirection.LEFT -> Alignment.TopStart
                SwipeDirection.NONE -> Alignment.Center
            }
        ) {
            when (direction) {
                SwipeDirection.RIGHT -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AccentGreenLight.copy(alpha = progress),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 2.dp,
                            color = AccentGreen.copy(alpha = progress)
                        )
                    ) {
                        Text(
                            text = "INTERESTED",
                            style = MaterialTheme.typography.titleMedium,
                            color = AccentGreen.copy(alpha = progress),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
                SwipeDirection.LEFT -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AccentRedLight.copy(alpha = progress),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 2.dp,
                            color = AccentRed.copy(alpha = progress)
                        )
                    ) {
                        Text(
                            text = "SKIP",
                            style = MaterialTheme.typography.titleMedium,
                            color = AccentRed.copy(alpha = progress),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
                SwipeDirection.NONE -> { }
            }
        }
    }
}
