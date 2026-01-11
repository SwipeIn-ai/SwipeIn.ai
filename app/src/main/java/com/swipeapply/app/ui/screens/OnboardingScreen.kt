package com.swipeapply.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.swipeapply.app.ui.theme.*

/**
 * Onboarding screen - first screen users see.
 * Minimal, professional, with LinkedIn CTA.
 */
@Composable
fun OnboardingScreen(
    onContinueWithLinkedIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDebugScreen by remember { mutableStateOf(false) }
    
    if (showDebugScreen) {
        DebugScreen(onBack = { showDebugScreen = false })
        return
    }
    
    // Subtle fade-in animation
    val animatedAlpha = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        animatedAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 800,
                easing = EaseOutCubic
            )
        )
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(horizontal = 32.dp)
            .alpha(animatedAlpha.value),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))
            
            // App logo / branding
            AppLogo()
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // App name
            Text(
                text = "SwipeApply",
                style = MaterialTheme.typography.displayMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Tagline
            Text(
                text = "Discover hiring teams.\nApply smarter.",
                style = MaterialTheme.typography.titleLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = MaterialTheme.typography.titleLarge.lineHeight
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // LinkedIn CTA Button
            LinkedInButton(
                onClick = onContinueWithLinkedIn,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Trust text
            Text(
                text = "No spam. No auto emails.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextTertiary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Debug button (tap to test API)
            TextButton(
                onClick = { showDebugScreen = true }
            ) {
                Text(
                    "🧪 Test API",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
            }
        }
    }
}

@Composable
private fun AppLogo() {
    // Animated gradient logo
    val infiniteTransition = rememberInfiniteTransition(label = "logo")
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientOffset"
    )
    
    Box(
        modifier = Modifier
            .size(100.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        GradientStart,
                        GradientEnd,
                        GradientStart
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "SA",
            style = MaterialTheme.typography.displaySmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LinkedInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .background(
                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                    colors = listOf(LinkedInBlue, LinkedInBlue.copy(alpha = 0.85f))
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        TextButton(
            onClick = onClick,
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // LinkedIn icon placeholder
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            color = Color.White,
                            shape = RoundedCornerShape(4.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "in",
                        style = MaterialTheme.typography.labelMedium,
                        color = LinkedInBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = "Continue with LinkedIn",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
