package com.swipeapply.app.ui.components.swipe

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import com.swipeapply.app.data.model.JobCard
import com.swipeapply.app.data.model.SwipeDirection
import com.swipeapply.app.ui.components.TechStackIcons
import com.swipeapply.app.ui.theme.* // We still need this for Gradient constants

@Composable
fun JobSwipeCard(
    jobCard: JobCard,
    swipeProgress: Float,
    swipeDirection: SwipeDirection,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Determine dynamic colors for swipe feedback
    // We use the Theme's Error (Red) and Tertiary (Green) colors so they adapt
    val rightSwipeColor = MaterialTheme.colorScheme.tertiary 
    val leftSwipeColor = MaterialTheme.colorScheme.error

    val borderColor by animateColorAsState(
        targetValue = when (swipeDirection) {
            SwipeDirection.RIGHT -> rightSwipeColor.copy(alpha = swipeProgress)
            SwipeDirection.LEFT -> leftSwipeColor.copy(alpha = swipeProgress)
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
                // Fix: Use a darker shadow in dark mode (if mapped in Theme), or fallback to black
                spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
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
            // FIX: Use the Theme's Surface color (White in Light Mode, Dark Grey in Dark Mode)
            containerColor = MaterialTheme.colorScheme.surface 
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
                    if (jobCard.isHiringNow) {
                        HiringBadge()
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    CompanyLogo(companyName = jobCard.company.name)

                    Spacer(modifier = Modifier.height(20.dp))

                    // Company name
                    Text(
                        text = jobCard.company.name,
                        style = MaterialTheme.typography.headlineMedium,
                        // FIX: Use OnSurface (Black in Light, White in Dark)
                        color = MaterialTheme.colorScheme.onSurface, 
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Job title
                    Text(
                        text = jobCard.title,
                        style = MaterialTheme.typography.titleLarge,
                        // FIX: Use OnSurfaceVariant (Grey in both, but readable)
                        color = MaterialTheme.colorScheme.onSurfaceVariant 
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

                    // Salary
                    jobCard.salary?.let { salary ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = salary.formatted(),
                            style = MaterialTheme.typography.titleMedium,
                            // FIX: Use Primary or Secondary color for emphasis
                            color = MaterialTheme.colorScheme.primary, 
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Bottom section - Tech stack
                Column {
                    Text(
                        text = "Tech Stack",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TechStackChips(techStack = jobCard.techStack)
                }
            }

            SwipeOverlay(
                direction = swipeDirection,
                progress = swipeProgress
            )
        }
    }
}

@Composable
private fun HiringBadge() {
    // Badge colors are specific, so we can keep using custom colors if they look good in both,
    // or map them to Theme Custom colors. For now, we manually adjust for dark mode safety.
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = BadgeHiringBackground // You might want to create BadgeHiringBackgroundDark in your Color.kt
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
                color = BadgeHiring, // Ensure this contrasts well
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun CompanyLogo(companyName: String) {
    val initials = companyName.split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .joinToString("")

    Box(
        modifier = Modifier
            .size(72.dp)
            .background(
                brush = Brush.linearGradient(
                    // Gradient usually looks good in both modes, but you can create GradientStartDark if needed
                    colors = listOf(GradientStart, GradientEnd) 
                ),
                shape = RoundedCornerShape(16.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White, // Always white on top of a colored gradient
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
        // Location Chip
        Surface(
            shape = RoundedCornerShape(8.dp),
            // FIX: Use SurfaceVariant (Light Grey in Light, Dark Grey in Dark)
            color = MaterialTheme.colorScheme.surfaceVariant 
        ) {
            Text(
                text = location,
                style = MaterialTheme.typography.bodyMedium,
                // FIX: Use OnSurfaceVariant
                color = MaterialTheme.colorScheme.onSurfaceVariant, 
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        // Location Type Chip (Remote/Onsite)
        Surface(
            shape = RoundedCornerShape(8.dp),
            // FIX: Use Secondary Container
            color = MaterialTheme.colorScheme.secondaryContainer 
        ) {
            Text(
                text = locationType,
                style = MaterialTheme.typography.bodyMedium,
                // FIX: Use OnSecondaryContainer
                color = MaterialTheme.colorScheme.onSecondaryContainer, 
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun TechStackChips(techStack: List<String>) {
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
                        // FIX: Use SurfaceVariant for the chip background
                        color = MaterialTheme.colorScheme.surfaceVariant 
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = techIcon.icon,
                                contentDescription = null,
                                tint = techIcon.color, // Icons usually keep their brand color
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = tech,
                                style = MaterialTheme.typography.bodyMedium,
                                // FIX: Use OnSurface (White in dark mode)
                                color = MaterialTheme.colorScheme.onSurface, 
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
                    // Interested Overlay
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        // FIX: Use Tertiary Container (Green-ish in both modes)
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = progress),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = progress)
                        )
                    ) {
                        Text(
                            text = "INTERESTED",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = progress),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
                SwipeDirection.LEFT -> {
                    // Skip Overlay
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = progress),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.error.copy(alpha = progress)
                        )
                    ) {
                        Text(
                            text = "SKIP",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error.copy(alpha = progress),
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