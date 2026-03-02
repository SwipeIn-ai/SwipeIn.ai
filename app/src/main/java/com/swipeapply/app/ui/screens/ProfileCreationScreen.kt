package com.swipeapply.app.ui.screens

import android.app.Application
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swipeapply.app.data.model.EducationItem
import com.swipeapply.app.data.model.ExperienceItem
import com.swipeapply.app.data.model.ProjectItem
import com.swipeapply.app.data.model.UserProfile
import com.swipeapply.app.ui.theme.GradientEnd
import com.swipeapply.app.ui.theme.GradientStart
import com.swipeapply.app.ui.viewmodel.ProfileCreationViewModel
import com.swipeapply.app.ui.viewmodel.ViewModelFactory

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
private val Chart4 = Color(0xFF00A0DC)

@Composable
fun ProfileCreationScreen(
    onNavigateHome: () -> Unit
) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: ProfileCreationViewModel = viewModel(factory = ViewModelFactory(application))
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.parseResume(context, it) }
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onNavigateHome()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandBackground)
    ) {
        // Decorative Background Blobs
        Box(
            modifier = Modifier
                .offset(x = (-60).dp, y = (-80).dp)
                .size(300.dp)
                .background(Chart3.copy(alpha = 0.1f), CircleShape)
                .blur(40.dp)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 60.dp)
                .size(250.dp)
                .background(Chart2.copy(alpha = 0.1f), CircleShape)
                .blur(40.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                state.isLoading -> {
                    ResumeAnalyzingState()
                }
                state.profile == null -> {
                    ResumeUploadState(
                        onUploadPdf = { launcher.launch("application/pdf") },
                        onSkipUpload = { viewModel.updateProfileField(UserProfile()) },
                        error = state.error
                    )
                }
                else -> {
                    ProfileConfirmationState(
                        profile = state.profile!!,
                        viewModel = viewModel,
                        onBack = { onNavigateHome() } // Or go back to upload
                    )
                }
            }
        }
    }
}

@Composable
private fun ResumeUploadState(
    onUploadPdf: () -> Unit,
    onSkipUpload: () -> Unit,
    error: String?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                border = BorderStroke(1.dp, BrandBorder),
                modifier = Modifier.size(40.dp),
                onClick = onSkipUpload
            ) {
                Icon(Icons.Default.ArrowBack, null, tint = BrandForeground, modifier = Modifier.padding(8.dp))
            }
            // Step dots
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp, 6.dp).background(BrandMuted, CircleShape))
                Box(modifier = Modifier.size(32.dp, 6.dp).background(BrandPrimary, CircleShape))
                Box(modifier = Modifier.size(8.dp, 6.dp).background(BrandMuted, CircleShape))
            }
            Spacer(modifier = Modifier.size(40.dp))
        }

        Text(
            text = "Build your profile",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = BrandForeground,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Upload your resume to instantly extract your skills, experience, and education.",
            fontSize = 15.sp,
            color = BrandMutedForeground,
            lineHeight = 22.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Upload Zone
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            border = BorderStroke(2.dp, BrandBorder),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onUploadPdf() }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                // Icon Cluster
                Box(contentAlignment = Alignment.Center, modifier = Modifier.height(100.dp)) {
                    // PDF Icon
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = BrandDestructive.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, BrandDestructive.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .offset(x = 16.dp, y = (-12).dp)
                            .rotate(-12f)
                            .size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Description, null, tint = BrandDestructive, modifier = Modifier.size(28.dp))
                            Text("PDF", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = BrandDestructive, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp))
                        }
                    }

                    // DOC Icon
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Chart4.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, Chart4.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .offset(x = (-16).dp, y = 12.dp)
                            .rotate(12f)
                            .size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Description, null, tint = Chart4, modifier = Modifier.size(28.dp))
                            Text("DOC", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Chart4, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp))
                        }
                    }

                    // Main Upload Icon
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = BrandPrimary.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.2f)),
                        shadowElevation = 4.dp,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, null, tint = BrandPrimary, modifier = Modifier.padding(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("Upload Resume", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = BrandForeground)
                Text("Tap to browse or drag file here", fontSize = 14.sp, color = BrandMutedForeground, modifier = Modifier.padding(vertical = 12.dp))
                
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = BrandMuted.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, BrandBorder.copy(alpha = 0.5f))
                ) {
                    Text("Max file size 5MB. PDF, DOCX", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = BrandMutedForeground, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (error != null) {
            Text(text = error, color = BrandDestructive, fontSize = 13.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp))
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Box(modifier = Modifier.weight(1f).height(1.dp).background(BrandBorder))
            Text("OR", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = BrandMutedForeground, modifier = Modifier.padding(horizontal = 16.dp))
            Box(modifier = Modifier.weight(1f).height(1.dp).background(BrandBorder))
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        // Manual Fill Button
        Surface(
            onClick = onSkipUpload,
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            border = BorderStroke(2.dp, BrandBorder),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("Fill Manually Instead", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = BrandForeground)
            }
        }
    }
}

@Composable
private fun ResumeAnalyzingState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // AI Parsing Active State Card
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.2f)),
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Scanner Icon
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = BrandSecondary,
                    border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.2f)),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Description, null, tint = BrandPrimary.copy(alpha = 0.6f), modifier = Modifier.size(28.dp))
                        // Simulated scanner line
                        val infiniteTransition = rememberInfiniteTransition()
                        val offset by infiniteTransition.animateFloat(
                            initialValue = 0f, targetValue = 56f,
                            animationSpec = infiniteRepeatable(animation = tween(1500, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse)
                        )
                        Box(modifier = Modifier.fillMaxWidth().height(2.dp).offset(y = (offset-28).dp).background(BrandPrimary).shadow(8.dp, spotColor = BrandPrimary))
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Text("Analyzing Profile", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = BrandForeground)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(BrandMuted, CircleShape)) {
                        Box(modifier = Modifier.fillMaxWidth(0.6f).height(8.dp).background(BrandPrimary, CircleShape))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Extracting skills, experience, tech stack...", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = BrandMutedForeground, maxLines = 1)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileConfirmationState(
    profile: UserProfile,
    viewModel: ProfileCreationViewModel,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Sticky Header Area
        Surface(color = BrandBackground.copy(alpha = 0.9f)) {
            Column(modifier = Modifier.padding(horizontal = 24.dp).padding(top = 8.dp, bottom = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        border = BorderStroke(1.dp, BrandBorder),
                        modifier = Modifier.size(40.dp),
                        onClick = onBack
                    ) {
                        Icon(Icons.Default.ArrowBack, null, tint = BrandForeground, modifier = Modifier.padding(8.dp))
                    }
                    // Step dots
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp, 6.dp).background(BrandMuted, CircleShape))
                        Box(modifier = Modifier.size(8.dp, 6.dp).background(BrandMuted, CircleShape))
                        Box(modifier = Modifier.size(32.dp, 6.dp).background(BrandPrimary, CircleShape))
                    }
                    Spacer(modifier = Modifier.size(40.dp))
                }
                Text("Confirm Profile", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = BrandForeground)
                Text("Review and edit your extracted details.", fontSize = 14.sp, color = BrandMutedForeground)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Profile Picture Placeholder
            Box(modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 16.dp)) {
                Surface(
                    shape = CircleShape,
                    color = BrandSecondary,
                    border = BorderStroke(4.dp, BrandBackground),
                    shadowElevation = 4.dp,
                    modifier = Modifier.size(112.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, null, tint = BrandPrimary.copy(alpha = 0.3f), modifier = Modifier.size(56.dp))
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = BrandPrimary,
                    border = BorderStroke(3.dp, BrandBackground),
                    modifier = Modifier.size(36.dp).align(Alignment.BottomEnd).offset(x = 0.dp, y = (-4).dp)
                ) {
                    Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.padding(8.dp))
                }
            }

            // Styled Input Field Helper
            @Composable
            fun StyledTextField(value: String, onValueChange: (String) -> Unit, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, singleLine: Boolean = true) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    label = { Text(label, fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                    leadingIcon = { Icon(icon, null, tint = BrandMutedForeground, modifier = Modifier.size(20.dp)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = BrandPrimary,
                        unfocusedBorderColor = BrandBorder,
                        focusedLabelColor = BrandPrimary,
                        unfocusedLabelColor = BrandMutedForeground
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = singleLine,
                    minLines = if(singleLine) 1 else 3
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            StyledTextField(profile.fullName, { viewModel.updateProfileField(profile.copy(fullName = it)) }, "Full Name", Icons.Default.Person)
            StyledTextField(profile.email, { viewModel.updateProfileField(profile.copy(email = it)) }, "Email", Icons.Default.Description)
            StyledTextField(profile.phone, { viewModel.updateProfileField(profile.copy(phone = it)) }, "Phone", Icons.Default.Description)
            StyledTextField(profile.bio, { viewModel.updateProfileField(profile.copy(bio = it)) }, "Professional Summary", Icons.Default.Description, false)
            
            var skillsText by remember { mutableStateOf(profile.skills.joinToString(", ")) }
            StyledTextField(skillsText, { 
                skillsText = it
                viewModel.updateProfileField(profile.copy(skills = it.split(",").map { s -> s.trim() }.filter { s -> s.isNotEmpty() })) 
            }, "Skills (comma separated)", Icons.Default.BusinessCenter, false)

            var techText by remember { mutableStateOf(profile.techStack.joinToString(", ")) }
            StyledTextField(techText, { 
                techText = it
                viewModel.updateProfileField(profile.copy(techStack = it.split(",").map { s -> s.trim() }.filter { s -> s.isNotEmpty() })) 
            }, "Tech Stack (comma separated)", Icons.Default.BusinessCenter, false)


            Spacer(modifier = Modifier.height(24.dp))
            
            // Fixed Bottom CTA Placeholder (Visual only, real CTA at bottom of scroll right now)
            Surface(
                onClick = { viewModel.saveProfile() },
                shape = RoundedCornerShape(28.dp),
                color = BrandPrimary,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("Launch Application", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp).navigationBarsPadding())
        }
    }
}
