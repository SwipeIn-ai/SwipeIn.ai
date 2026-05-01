package com.swipeapply.app.ui.components.swipe

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.draw.blur
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
import com.swipeapply.app.data.model.Company
import com.swipeapply.app.data.model.JobCard
import com.swipeapply.app.data.model.SwipeDirection

private val BrandPrimary = Color(0xFF0A66C2)
private val BrandSecondary = Color(0xFFE8F3FF)
private val BrandForeground = Color(0xFF1A1D21)
private val BrandMuted = Color(0xFFF0F2F5)
private val BrandBorder = Color(0xFFDEE2E6)
private val Chart2 = Color(0xFFFF5F6D)
private val Chart3 = Color(0xFFFFC371)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JobSwipeCard(
    jobCard: JobCard,
    swipeProgress: Float,
    swipeDirection: SwipeDirection,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = BrandBorder

    val activeBorderColor by animateColorAsState(
        targetValue = when (swipeDirection) {
            SwipeDirection.RIGHT -> Color(0xFF00D26A).copy(alpha = swipeProgress * 0.8f)
            SwipeDirection.LEFT -> Color(0xFFFF6B6B).copy(alpha = swipeProgress * 0.8f)
            SwipeDirection.NONE -> borderColor
        },
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 500f),
        label = "jobBorderColor"
    )

    val cardBg = MaterialTheme.colorScheme.surface

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        shape = RoundedCornerShape(32.dp),
        color = cardBg,
        shadowElevation = 8.dp,
        border = BorderStroke(if (swipeProgress > 0.05f) 3.dp else 1.dp, if (swipeProgress > 0.05f) activeBorderColor else borderColor)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Card Header
                CardHeader(jobCard)

                // Scrollable Body
                Box(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 20.dp)
                            .padding(bottom = 80.dp), // Extra padding for action buttons
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Badges Row
                        BadgesRow(jobCard)

                        // Tech Stack
                        if (jobCard.techStack.isNotEmpty()) {
                            Column {
                                Text(
                                    text = "TECH STACK",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                TechStackChips(jobCard.techStack)
                            }
                        }

                        // Description
                        Column(modifier = Modifier.padding(bottom = 16.dp)) {
                            Text(
                                text = "ABOUT THE ROLE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = jobCard.roleDescription.cleanHtml(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                lineHeight = 24.sp
                            )
                        }
                    }

                    // Bottom Fade
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, cardBg, cardBg)
                                )
                            )
                    )
                }
            }


        }
    }
}

@Composable
private fun CardHeader(jobCard: JobCard) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        // Decorative background glow
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(128.dp)
                .background(BrandPrimary.copy(alpha = 0.05f), CircleShape)
                .blur(24.dp)
        )

        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                CompanyLogo(jobCard.company)
                
                // Referral Badge Mock (or based on logic)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = BrandSecondary,
                    border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(BrandPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                        }
                        Text(
                            text = "Referral Available",
                            color = BrandPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Text(
                text = jobCard.title,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = BrandForeground,
                lineHeight = 28.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = jobCard.company.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandForeground
                )
                Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), CircleShape))
                Text(
                    text = "3 days ago", // Mock or from jobCard.postedAt
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    HorizontalDivider(color = BrandMuted.copy(alpha = 0.6f))
}

private fun extractDomain(url: String?): String? {
    if (url.isNullOrBlank()) return null
    return try {
        var host = url.lowercase().trim()
        if (host.startsWith("http://")) host = host.substring(7)
        if (host.startsWith("https://")) host = host.substring(8)
        val slashIndex = host.indexOf('/')
        if (slashIndex != -1) host = host.substring(0, slashIndex)
        if (host.startsWith("www.")) host = host.substring(4)
        if (host.isBlank()) null else host
    } catch (e: Exception) {
        null
    }
}

@Composable
private fun CompanyLogo(company: Company) {
    val initials = company.name.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")
    val domain = extractDomain(company.website) ?: (company.name.replace(" ", "").lowercase() + ".com")
    val displayUrl = company.logoUrl ?: "https://logos.hunter.io/$domain"

    Surface(
        modifier = Modifier.size(56.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, BrandBorder)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Chart2.copy(alpha = 0.1f), Chart3.copy(alpha = 0.1f), BrandPrimary.copy(alpha = 0.1f))
                        )
                    )
            )
            Text(
                text = initials,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = BrandForeground
            )
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(displayUrl)
                    .crossfade(300)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun BadgesRow(jobCard: JobCard) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BadgeChip(
            icon = Icons.Default.LocationOn,
            text = jobCard.location.take(15) + if(jobCard.location.length > 15) "..." else "",
            bg = BrandMuted.copy(alpha = 0.5f),
            textColor = BrandForeground
        )
    }
}

@Composable
private fun BadgeChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, bg: Color, textColor: Color, borderColor: Color = BrandBorder.copy(alpha = 0.5f)) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, null, tint = textColor.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
            Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textColor)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TechStackChips(techStack: List<String>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        techStack.take(6).forEachIndexed { index, tech ->
            val isPrimary = index < 2
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isPrimary) BrandSecondary else BrandMuted,
                border = BorderStroke(1.dp, if (isPrimary) BrandPrimary.copy(alpha = 0.1f) else BrandBorder.copy(alpha = 0.5f)),
                shadowElevation = if (isPrimary) 1.dp else 0.dp
            ) {
                Text(
                    text = tech,
                    fontSize = 13.sp,
                    fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.Medium,
                    color = if (isPrimary) BrandPrimary else BrandForeground,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

fun String.cleanHtml(): String {
    return this.replace(Regex("<[^>]*>"), "")
        .replace("&amp;", "&").replace("&nbsp;", " ")
        .replace("&#x2F;", "/").replace("&quot;", "\"")
        .replace("&lt;", "<").replace("&gt;", ">").trim()
}


