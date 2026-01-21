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
    val borderColor by animateColorAsState(
        targetValue = when (swipeDirection) {
            SwipeDirection.RIGHT -> Color(0xFF4CAF50).copy(alpha = swipeProgress)
            SwipeDirection.LEFT -> Color(0xFFE57373).copy(alpha = swipeProgress)
            SwipeDirection.NONE -> Color.Transparent
        },
        label = "borderColor"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.75f) // Increased height slightly for better fit
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .border(3.dp, borderColor, RoundedCornerShape(24.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // --- Header Section (Fixed Height) ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = jobCard.company.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = jobCard.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    CompanyLogo(jobCard.company.name, jobCard.company.logoUrl)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // --- Chips Row ---
                Row(modifier = Modifier.fillMaxWidth()) {
                    LocationChip(jobCard.location, jobCard.locationType.label)
                    Spacer(modifier = Modifier.width(8.dp))
                    if(jobCard.isHiringNow) HiringBadge()
                }

                Spacer(modifier = Modifier.height(12.dp))

                Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                // --- Scrollable Description (Flexible Height) ---
                // This 'weight' ensures it takes up remaining space but doesn't push bottom content off
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 12.dp)
                ) {
                    Text(
                        text = "About the Role",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = jobCard.roleDescription.cleanHtml(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4
                    )
                }

                Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                Spacer(modifier = Modifier.height(12.dp))

                // --- Footer / Tech Stack (Fixed Bottom) ---
                if (jobCard.techStack.isNotEmpty()) {
                    Text(
                        text = "Tech Stack",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TechStackChips(jobCard.techStack)
                }
            }

            // Overlay for Swiping
            SwipeOverlay(swipeDirection, swipeProgress)
        }
    }
}

// Helper to clean description
fun String.cleanHtml(): String {
    return this.replace(Regex("<[^>]*>"), "")
        .replace("&amp;", "&")
        .replace("&nbsp;", " ")
        .trim()
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