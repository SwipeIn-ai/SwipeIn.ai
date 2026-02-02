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
import com.swipeapply.app.SupabaseClient
import com.swipeapply.app.ui.theme.*
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.OAuthProvider
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

object LinkedInOidc : OAuthProvider() {
    override val name = "linkedin_oidc"
}

@Composable
fun OnboardingScreen(
    onContinue: () -> Unit,
    onSkipLogin: () -> Unit = {}, // Dev mode bypass
    modifier: Modifier = Modifier
) {
    val supabase = SupabaseClient.client
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isJustLoggedOut by remember { mutableStateOf(false) }
    
    // --- Debug/Test State ---
    var showDebugScreen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        supabase.auth.sessionStatus.collectLatest { status ->
            when (status) {
                is SessionStatus.Authenticated -> {
                    if (!isJustLoggedOut) {
                        onContinue()
                    }
                }
                else -> {
                    isJustLoggedOut = false
                }
            }
        }
    }

    val animatedAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        animatedAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = EaseOutCubic)
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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

                AppLogo()

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "SwipeApply",
                    style = MaterialTheme.typography.displayMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Discover hiring teams.\nApply smarter.",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = MaterialTheme.typography.titleLarge.lineHeight
                )

                Spacer(modifier = Modifier.weight(1f))

                GoogleButton(
                    onClick = {
                        scope.launch {
                            try {
                                supabase.auth.signInWith(
                                    provider = Google,
                                    redirectUrl = "swipeapply://callback"
                                )
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Error: ${e.message}")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                LinkedInButton(
                    onClick = {
                        scope.launch {
                            try {
                                supabase.auth.signInWith(
                                    provider = LinkedInOidc,
                                    redirectUrl = "swipeapply://callback"
                                )
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Error: ${e.message}")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "No spam. No auto emails.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextTertiary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // --- 🚀 NEW BYPASS BUTTON ---
                // Only visible in debug/testing builds if you want, 
                // but keeping it simple for now as requested.
                TextButton(
                    onClick = { onSkipLogin() }, // <--- BYPASS LOGIC (Dev Mode)
                    colors = ButtonDefaults.textButtonColors(contentColor = AccentRed)
                ) {
                    Text("Skip Login (Dev Mode)")
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// ... (Keep AppLogo, GoogleButton, LinkedInButton Helpers same as before) ...
@Composable
private fun AppLogo() {
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
                    colors = listOf(GradientStart, GradientEnd, GradientStart)
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
private fun GoogleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val GoogleBlue = Color(0xFF4285F4)

    Box(
        modifier = modifier
            .height(56.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(GoogleBlue, GoogleBlue.copy(alpha = 0.85f))
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        TextButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(color = Color.White, shape = RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "G",
                        style = MaterialTheme.typography.labelMedium,
                        color = GoogleBlue,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Continue with Google",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun LinkedInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val LinkedInBlue = Color(0xFF0A66C2)

    Box(
        modifier = modifier
            .height(56.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(LinkedInBlue, LinkedInBlue.copy(alpha = 0.85f))
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        TextButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(color = Color.White, shape = RoundedCornerShape(4.dp)),
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