package com.swipeapply.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swipeapply.app.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.OAuthProvider
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private val BrandPrimary = Color(0xFF0A66C2)
private val BrandSecondary = Color(0xFFE8F3FF)
private val BrandBackground = Color(0xFFF8F9FA)
private val BrandForeground = Color(0xFF1A1D21)
private val BrandMuted = Color(0xFFF0F2F5)
private val BrandMutedForeground = Color(0xFF666E76)
private val BrandBorder = Color(0xFFDEE2E6)
private val BrandDestructive = Color(0xFFDC3545)
private val Chart2 = Color(0xFFFF5F6D)
private val Chart3 = Color(0xFFFFC371)

object LinkedInOidc : OAuthProvider() {
    override val name = "linkedin_oidc"
}

@Composable
fun OnboardingScreen(
    onContinue: () -> Unit,
    onSkipLogin: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val supabase = SupabaseClient.client
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isJustLoggedOut by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var isLinkedInLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        supabase.auth.sessionStatus.collectLatest { status ->
            when (status) {
                is SessionStatus.Authenticated -> {
                    if (!isJustLoggedOut) {
                        isGoogleLoading = false
                        isLinkedInLoading = false
                        onContinue()
                    }
                }
                else -> { 
                    isJustLoggedOut = false
                    isGoogleLoading = false
                    isLinkedInLoading = false
                }
            }
        }
    }

    val animatedAlpha = remember { Animatable(0f) }
    val animatedOffset = remember { Animatable(30f) }

    LaunchedEffect(Unit) {
        launch { animatedAlpha.animateTo(1f, tween(600, easing = EaseOutCubic)) }
        launch { animatedOffset.animateTo(0f, tween(600, easing = EaseOutCubic)) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize(),
        containerColor = BrandBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Decorative Background Blobs
            Box(
                modifier = Modifier
                    .offset(x = (-60).dp, y = (-80).dp)
                    .size(300.dp)
                    .background(
                        color = Chart3.copy(alpha = 0.15f),
                        shape = CircleShape
                    )
                    .blurEffect()
            )
            
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 60.dp, y = 60.dp)
                    .size(250.dp)
                    .background(
                        color = Chart2.copy(alpha = 0.1f),
                        shape = CircleShape
                    )
                    .blurEffect()
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .alpha(animatedAlpha.value)
                    .offset(y = animatedOffset.value.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(0.15f))

                // Logo & Header Section
                SwipeInLogoHeader()

                Spacer(modifier = Modifier.weight(0.1f))

                // Center Illustration (Abstract Swipe Cards)
                CenterIllustration()

                Spacer(modifier = Modifier.weight(0.15f))

                // Action Buttons Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    GoogleSignInButton(
                        isLoading = isGoogleLoading,
                        onClick = {
                            isGoogleLoading = true
                            scope.launch {
                                try {
                                    supabase.auth.signInWith(
                                        provider = Google,
                                        redirectUrl = "swipeapply://callback"
                                    )
                                } catch (e: Exception) {
                                    isGoogleLoading = false
                                    snackbarHostState.showSnackbar("Sign in failed. Please try again.")
                                }
                            }
                        }
                    )

                    LinkedInSignInButton(
                        isLoading = isLinkedInLoading,
                        onClick = {
                            isLinkedInLoading = true
                            scope.launch {
                                try {
                                    supabase.auth.signInWith(
                                        provider = LinkedInOidc,
                                        redirectUrl = "swipeapply://callback"
                                    )
                                } catch (e: Exception) {
                                    isLinkedInLoading = false
                                    snackbarHostState.showSnackbar("Sign in failed. Please try again.")
                                }
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(
                    onClick = { onSkipLogin() },
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        "Skip for now (Dev Mode)",
                        color = BrandMutedForeground,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                // Legal Footer
                Text(
                    text = "By continuing, you agree to our Terms of Service and Privacy Policy.",
                    style = MaterialTheme.typography.labelSmall,
                    color = BrandMutedForeground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp).padding(horizontal = 16.dp),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

// Helper for soft blur without heavy performance hit
@Composable
private fun Modifier.blurEffect() = this.graphicsLayer {
    // Basic graphic layer for composition, since actual blur requires RenderEffect (API 31+)
    // We achieve a soft look by just using low alpha circles.
    alpha = 0.8f
}

@Composable
private fun SwipeInLogoHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text(
                text = "Swipe",
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                color = BrandForeground,
                letterSpacing = (-1).sp
            )
            Box(
                modifier = Modifier
                    .padding(start = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BrandPrimary)
                    .padding(horizontal = 10.dp, vertical = 2.dp)
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "in",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = (-1).sp
                )
            }
        }
        Text(
            text = "Swipe Your Way to Referrals",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = BrandMutedForeground,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CenterIllustration() {
    Box(
        modifier = Modifier
            .size(320.dp)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Background Card (Swiped Left)
        Surface(
            modifier = Modifier
                .width(220.dp)
                .height(300.dp)
                .offset(x = (-24).dp, y = 12.dp)
                .rotate(-8f)
                .scale(0.9f)
                .alpha(0.7f),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, BrandBorder.copy(alpha = 0.5f)),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(64.dp).background(BrandMuted, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("A", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = BrandMutedForeground)
                }
                Spacer(modifier = Modifier.height(24.dp))
                Box(modifier = Modifier.width(140.dp).height(14.dp).background(BrandMuted, RoundedCornerShape(8.dp)))
                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.width(100.dp).height(14.dp).background(BrandMuted, RoundedCornerShape(8.dp)))
            }
        }

        // Foreground Card (Active)
        Surface(
            modifier = Modifier
                .width(250.dp)
                .height(340.dp)
                .rotate(2f),
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, BrandBorder.copy(alpha = 0.8f)),
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top section
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            shadowElevation = 2.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BrandBorder)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("G", fontSize = 24.sp, fontWeight = FontWeight.Black, color = BrandForeground)
                            }
                        }
                        
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = BrandSecondary,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.2f))
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Referral", color = BrandPrimary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Senior Frontend Engineer", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = BrandForeground)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Google • 3 days ago", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = BrandMutedForeground)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(shape = RoundedCornerShape(8.dp), color = BrandMuted.copy(alpha = 0.5f)) {
                            Text("Mountain View, CA", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = BrandForeground, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                        Surface(shape = RoundedCornerShape(8.dp), color = BrandSecondary.copy(alpha = 0.4f)) {
                            Text("Remote", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = BrandPrimary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Fading bottom content + Buttons
                Box(
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.White)))
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 8.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Close, null, tint = Color(0xFF657786), modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Surface(
                            modifier = Modifier.size(56.dp),
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 8.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Check, null, tint = Color(0xFFFF5252), modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoogleSignInButton(
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = { if (!isLoading) onClick() },
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, BrandBorder),
        shadowElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = BrandForeground
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Simple G logo mock
                    Text(
                        text = "G",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDB4437),
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Continue with Google",
                        fontSize = 16.sp,
                        color = BrandForeground,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun LinkedInSignInButton(
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = { if (!isLoading) onClick() },
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(28.dp),
        color = BrandPrimary,
        shadowElevation = 4.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "in",
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Continue with LinkedIn",
                        fontSize = 16.sp,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
