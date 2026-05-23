package com.swipeapply.app.ui.components.swipe

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Work
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swipeapply.app.data.model.ConfidenceTier
import com.swipeapply.app.data.model.Employee
import com.swipeapply.app.data.model.SwipeDirection
import com.swipeapply.app.ui.theme.LinkedInBlue

// ── Design tokens matching JobSwipeCard ──────────────────────────────────────
private val CardBrandPrimary    = Color(0xFF0A66C2)   // LinkedIn Blue
private val CardBrandSecondary  = Color(0xFFE8F3FF)   // Light blue tint
private val CardBrandForeground = Color(0xFF1A1D21)   // Near-black text
private val CardBrandMuted      = Color(0xFFF0F2F5)   // Subtle chip bg
private val CardBrandBorder     = Color(0xFFDEE2E6)   // Border

private val TierHigh   = Color(0xFF10B981)  // Emerald
private val TierMedium = Color(0xFFF59E0B)  // Amber
private val TinderPink = Color(0xFFFD297B)  // Tinder / right-swipe accent

@Composable
fun EmployeeSwipeCard(
    employee: Employee,
    companyName: String,
    swipeProgress: Float = 0f,
    swipeDirection: SwipeDirection = SwipeDirection.NONE,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val activeBorderColor by animateColorAsState(
        targetValue = when (swipeDirection) {
            SwipeDirection.RIGHT -> TinderPink.copy(alpha = swipeProgress * 0.85f)
            SwipeDirection.LEFT  -> Color(0xFFFF6B6B).copy(alpha = swipeProgress * 0.85f)
            SwipeDirection.NONE  -> CardBrandBorder
        },
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 500f),
        label = "empBorderColor"
    )

    val tier      = employee.getConfidenceTier()
    val tierColor = when (tier) {
        ConfidenceTier.HIGH   -> TierHigh
        ConfidenceTier.MEDIUM -> TierMedium
        ConfidenceTier.LOW    -> Color(0xFFEF4444)
    }
    val cardBg = MaterialTheme.colorScheme.surface

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        shape = RoundedCornerShape(32.dp),
        color = cardBg,
        border = BorderStroke(
            if (swipeProgress > 0.05f) 3.dp else 1.dp,
            if (swipeProgress > 0.05f) activeBorderColor else CardBrandBorder
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {

                // ── Card Header ─────────────────────────────────────────────
                EmployeeCardHeader(
                    employee    = employee,
                    companyName = companyName
                )

                // ── Body ──────────────────────────────────────────
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        // Email chip
                        EmailRow(employee = employee, tierColor = tierColor, tier = tier)

                        // Confidence bar
                        ConfidenceRow(employee = employee, tierColor = tierColor)

                        // Detail chips
                        DetailChips(employee = employee)

                        // LinkedIn
                        employee.linkedinUrl?.takeIf { it.isNotBlank() }?.let { url ->
                            LinkedInRow(url = url)
                        }
                    }

                }
            }
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun EmployeeCardHeader(employee: Employee, companyName: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Column {
            // Top row: avatar + referral badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(CardBrandPrimary.copy(alpha = 0.15f), CardBrandSecondary)
                            )
                        )
                        .border(1.dp, CardBrandBorder, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = employee.getInitials(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = CardBrandPrimary,
                        letterSpacing = (-0.5).sp
                    )
                }

                // Referral badge  (mirrors "Referral Available" from JobSwipeCard)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CardBrandSecondary,
                    border = BorderStroke(1.dp, CardBrandPrimary.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(CardBrandPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                        Text(
                            text = "Referral Contact",
                            color = CardBrandPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Name
            Text(
                text = employee.fullName,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = CardBrandForeground,
                lineHeight = 28.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(5.dp))

            // Title · Company
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = employee.jobTitle ?: "Professional",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CardBrandForeground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            CircleShape
                        )
                )
                Text(
                    text = companyName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Location (if available)
            val location = employee.getFormattedLocation()
            if (location != "Location unknown") {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = location,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
    HorizontalDivider(color = CardBrandMuted.copy(alpha = 0.6f))
}

// ── Email Row ─────────────────────────────────────────────────────────────────

@Composable
private fun EmailRow(employee: Employee, tierColor: Color, tier: ConfidenceTier) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = CardBrandMuted.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, CardBrandBorder.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Default.Email,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = CardBrandPrimary
            )
            Text(
                text = employee.email,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = CardBrandForeground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            // Verification badge
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = tierColor.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        if (tier == ConfidenceTier.HIGH) Icons.Default.Verified else Icons.Default.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(11.dp),
                        tint = tierColor
                    )
                    Text(
                        text = tier.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = tierColor
                    )
                }
            }
        }
    }
}

// ── Confidence Bar ────────────────────────────────────────────────────────────

@Composable
private fun ConfidenceRow(employee: Employee, tierColor: Color) {
    val animatedProgress by animateFloatAsState(
        targetValue = employee.confidence / 100f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "confidenceBar"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "REFERRAL PROBABILITY",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp
            )
            Text(
                text = "${employee.confidence}%",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = tierColor
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(CardBrandMuted)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(tierColor.copy(alpha = 0.7f), tierColor)
                        )
                    )
            )
        }
    }
}

// ── Detail Chips ──────────────────────────────────────────────────────────────

@Composable
private fun DetailChips(employee: Employee) {
    val chips = buildList {
        employee.department?.takeIf { it.isNotBlank() }?.let { add(Pair(Icons.Default.Work, it)) }
        employee.seniority?.takeIf { it.isNotBlank() }?.let { add(Pair(Icons.Default.Work, it)) }
    }
    if (chips.isEmpty()) return

    Column {
        Text(
            text = "DETAILS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            chips.forEach { (icon, label) ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CardBrandMuted,
                    border = BorderStroke(1.dp, CardBrandBorder.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(icon, null, tint = CardBrandPrimary, modifier = Modifier.size(13.dp))
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CardBrandForeground
                        )
                    }
                }
            }
        }
    }
}

// ── LinkedIn Row ──────────────────────────────────────────────────────────────

@Composable
private fun LinkedInRow(url: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = LinkedInBlue.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, LinkedInBlue.copy(alpha = 0.12f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = LinkedInBlue.copy(alpha = 0.15f),
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        "in",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = LinkedInBlue
                    )
                }
            }
            Text(
                text = "LinkedIn Profile",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = LinkedInBlue,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.OpenInNew,
                null,
                modifier = Modifier.size(15.dp),
                tint = LinkedInBlue
            )
        }
    }
}
