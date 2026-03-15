package com.swipeapply.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.swipeapply.app.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.OAuthProvider
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// ─── Brand Palette ────────────────────────────────────────────────────────────
private val LinkedInBlue   = Color(0xFF0A66C2)
private val LinkedInSurface= Color(0xFFE8F3FF)
private val PageBg         = Color(0xFFF5F7FA)
private val DarkText       = Color(0xFF1A1D21)
private val MutedText      = Color(0xFF6B7280)
private val BorderColor    = Color(0xFFE5E7EB)
private val TinderRed      = Color(0xFFFF4458)
private val AccentOrange   = Color(0xFFFF6B35)
private val GoogleRed      = Color(0xFFDB4437)
private val GoogleGreen    = Color(0xFF0F9D58)
private val GoogleBlue     = Color(0xFF4285F4)
private val GoogleYellow   = Color(0xFFF4B400)

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

    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(40f) }

    LaunchedEffect(Unit) {
        launch { alpha.animateTo(1f, tween(700, easing = EaseOutCubic)) }
        launch { offsetY.animateTo(0f, tween(700, easing = EaseOutCubic)) }
    }

    // Floating blob animation
    val infiniteTransition = rememberInfiniteTransition(label = "blobs")
    val blobOffset by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blobFloat"
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize(),
        containerColor = PageBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── Decorative gradient background ────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFEEF4FF),
                                PageBg,
                                Color(0xFFFFF0F3)
                            )
                        )
                    )
            )

            // ── Floating colour blobs ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .offset(x = (-48).dp, y = (blobOffset - 40).dp)
                    .size(260.dp)
                    .background(LinkedInBlue.copy(alpha = 0.06f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 48.dp, y = (-blobOffset + 20).dp)
                    .size(220.dp)
                    .background(TinderRed.copy(alpha = 0.07f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-30).dp, y = (blobOffset).dp)
                    .size(160.dp)
                    .background(AccentOrange.copy(alpha = 0.06f), CircleShape)
            )

            // ── Main content ──────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .alpha(alpha.value)
                    .offset(y = offsetY.value.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Spacer(modifier = Modifier.weight(0.05f))

                // ── App Logo (animated hero) ──────────────────────────────────
                AnimatedLogoHero()

                Spacer(modifier = Modifier.weight(0.04f))

                // ── Tagline ───────────────────────────────────────────────────
                TaglineSection()

                Spacer(modifier = Modifier.weight(0.12f))

                // ── Sign-in Buttons ─────────────────────────────────────────
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

                // ── Legal Footer ─────────────────────────────────────────────
                Text(
                    text = "By continuing, you agree to our Terms of Service and Privacy Policy.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedText.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 20.dp, top = 4.dp).padding(horizontal = 8.dp),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun AnimatedLogoHero() {
    val infiniteTransition = rememberInfiniteTransition(label = "logoAnim")

    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse1"
    )
    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.30f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = EaseInOutSine, delayMillis = 400),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse2"
    )
    val pulseAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.18f, targetValue = 0.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = EaseInOutSine, delayMillis = 400),
            repeatMode = RepeatMode.Reverse
        ), label = "alpha2"
    )
    val floatY by infiniteTransition.animateFloat(
        initialValue = -6f, targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "float"
    )
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = FastOutSlowInEasing)
        ), label = "rotation"
    )

    // Adaptive sizing: fills available width up to a max, scales all rings proportionally
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        val containerSize = minOf(maxWidth * 0.72f, 260.dp)
        val outerRing    = containerSize * 0.91f
        val midRing      = containerSize * 0.76f
        val spinRing     = containerSize * 0.70f
        val innerCircle  = containerSize * 0.636f
        val logoCard     = containerSize * 0.582f
        val logoImage    = containerSize * 0.473f
        val cornerRadius = containerSize * 0.164f
        val logoCorner   = containerSize * 0.127f

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(containerSize)
        ) {
            // Outermost pulse ring
            Box(
                modifier = Modifier
                    .size(outerRing)
                    .graphicsLayer { scaleX = pulseScale2; scaleY = pulseScale2; alpha = pulseAlpha2 }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                LinkedInBlue.copy(alpha = 0.6f),
                                TinderRed.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )
            // Mid pulse ring
            Box(
                modifier = Modifier
                    .size(midRing)
                    .graphicsLayer { scaleX = pulseScale1; scaleY = pulseScale1; alpha = 0.13f }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                LinkedInBlue,
                                AccentOrange.copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )
            // Spinning arc ring
            Box(
                modifier = Modifier
                    .size(spinRing)
                    .graphicsLayer { rotationZ = rotationAngle }
                    .background(
                        Brush.sweepGradient(
                            listOf(
                                LinkedInBlue.copy(alpha = 0.0f),
                                LinkedInBlue.copy(alpha = 0.5f),
                                TinderRed.copy(alpha = 0.6f),
                                AccentOrange.copy(alpha = 0.4f),
                                LinkedInBlue.copy(alpha = 0.0f)
                            )
                        ),
                        CircleShape
                    )
            )
            // Inner background circle
            Box(
                modifier = Modifier
                    .size(innerCircle)
                    .background(PageBg, CircleShape)
            )
            // Floating logo card
            Box(
                modifier = Modifier
                    .size(logoCard)
                    .offset(y = floatY.dp)
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(cornerRadius),
                        spotColor = LinkedInBlue.copy(alpha = 0.30f)
                    )
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = "android.resource://com.swipeapply.app/drawable/app_logo",
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .size(logoImage)
                        .clip(RoundedCornerShape(logoCorner)),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
private fun TaglineSection() {
    Text(
        text = "Where Tinder Meets LinkedIn.",
        fontSize = 26.sp,
        fontWeight = FontWeight.ExtraBold,
        color = DarkText,
        textAlign = TextAlign.Center,
        lineHeight = 34.sp,
        modifier = Modifier.padding(horizontal = 8.dp)
    )
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
            .height(56.dp)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = DarkText
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Colourful Google "G"
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = GoogleBlue))   { append("G") }
                        },
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = GoogleRed))    { append("o") }
                            withStyle(SpanStyle(color = GoogleYellow))  { append("o") }
                            withStyle(SpanStyle(color = GoogleBlue))   { append("g") }
                            withStyle(SpanStyle(color = GoogleGreen))  { append("l") }
                            withStyle(SpanStyle(color = GoogleRed))    { append("e") }
                        },
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    HorizontalDivider(
                        modifier = Modifier
                            .height(20.dp)
                            .width(1.dp),
                        color = BorderColor
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Continue with Google",
                        fontSize = 15.sp,
                        color = DarkText,
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
            .height(56.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = LinkedInBlue.copy(alpha = 0.35f)
            ),
        shape = RoundedCornerShape(16.dp),
        color = LinkedInBlue
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(LinkedInBlue, Color(0xFF0D7FDD))
                    )
                )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // LinkedIn "in" badge
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "in",
                            fontWeight = FontWeight.ExtraBold,
                            color = LinkedInBlue,
                            fontSize = 14.sp,
                            letterSpacing = (-0.5).sp
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(20.dp)
                            .background(Color.White.copy(alpha = 0.35f))
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Continue with LinkedIn",
                        fontSize = 15.sp,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
