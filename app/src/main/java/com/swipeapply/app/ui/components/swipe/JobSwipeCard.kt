package com.swipeapply.app.ui.components.swipe

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.swipeapply.app.data.model.JobCard
import com.swipeapply.app.data.model.SwipeDirection
import com.swipeapply.app.ui.components.TechStackIcons
import com.swipeapply.app.ui.theme.*

@Composable
fun JobSwipeCard(
    jobCard: JobCard,
    swipeProgress: Float,
    swipeDirection: SwipeDirection,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rightSwipeColor = MaterialTheme.colorScheme.tertiary 
    val leftSwipeColor = MaterialTheme.colorScheme.error
    var isDescriptionExpanded by remember { mutableStateOf(false) }

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
            .aspectRatio(0.65f)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
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
            containerColor = MaterialTheme.colorScheme.surface 
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top section - Hiring badge + Logo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    if (jobCard.isHiringNow) {
                        HiringBadge()
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                    
                    // Company Logo
                    CompanyLogo(
                        companyName = jobCard.company.name,
                        logoUrl = jobCard.company.logoUrl
                    )
                }

                // Company name
                Text(
                    text = jobCard.company.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Job title
                Text(
                    text = jobCard.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Location chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LocationChip(
                        location = jobCard.location,
                        locationType = jobCard.locationType.label
                    )
                }

                // Salary if available
                jobCard.salary?.let { salary ->
                    Text(
                        text = salary.formatted(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Job Description with Read More
                JobDescription(
                    description = jobCard.roleDescription,
                    isExpanded = isDescriptionExpanded,
                    onToggle = { isDescriptionExpanded = !isDescriptionExpanded }
                )

                // Tech stack
                if (jobCard.techStack.isNotEmpty()) {
                    Column {
                        Text(
                            text = "Tech Stack",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TechStackChips(techStack = jobCard.techStack)
                    }
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
private fun JobDescription(
    description: String,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    // Clean HTML tags from description
    val cleanDescription = description
        .replace(Regex("<[^>]*>"), "")
        .replace("&amp;", "&")
        .replace("&#x2F;", "/")
        .replace("&quot;", "\"")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .trim()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Text(
            text = "About this role",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = cleanDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 4,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                )
                
                if (cleanDescription.length > 200) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isExpanded) "Show less" else "Read more...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onToggle() }
                    )
                }
            }
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
            .size(56.dp)
            .clip(RoundedCornerShape(14.dp))
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
                    .crossfade(true)
                    .build(),
                contentDescription = "$companyName logo",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = initials,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
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