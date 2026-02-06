package com.swipeapply.app.ui.screens

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swipeapply.app.ui.theme.*
import com.swipeapply.app.ui.viewmodel.ProfileViewModel
import com.swipeapply.app.ui.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit
) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: ProfileViewModel = viewModel(factory = ViewModelFactory(application))
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Show snackbar for success/error
    LaunchedEffect(state.saveSuccess, state.error) {
        if (state.saveSuccess) {
            snackbarHostState.showSnackbar("Profile saved successfully!")
            viewModel.clearMessages()
        }
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("My Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (state.profile != null) {
                        if (state.isEditMode) {
                            // Cancel button
                            TextButton(onClick = { viewModel.toggleEditMode() }) {
                                Text("Cancel")
                            }
                            // Save button
                            TextButton(
                                onClick = { viewModel.saveProfile() },
                                enabled = !state.isSaving
                            ) {
                                if (state.isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Save", color = AccentGreen)
                                }
                            }
                        } else {
                            // Edit button
                            IconButton(onClick = { viewModel.toggleEditMode() }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> {
                    // Loading State
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(16.dp))
                            Text("Loading profile...")
                        }
                    }
                }

                state.profile == null && !state.isLoading -> {
                    // Error / No Profile State
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                Icons.Default.PersonOff,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Profile not found",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                state.error ?: "Please create your profile first",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(24.dp))
                            Button(onClick = { viewModel.fetchProfile() }) {
                                Text("Retry")
                            }
                        }
                    }
                }

                else -> {
                    // Profile Content
                    val profile = state.profile!!
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        // Profile Header
                        ProfileHeader(
                            name = if (state.isEditMode) state.editFullName else profile.fullName,
                            email = if (state.isEditMode) state.editEmail else profile.email
                        )

                        Spacer(Modifier.height(24.dp))

                        // Edit Mode or View Mode
                        AnimatedVisibility(
                            visible = state.isEditMode,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            EditModeContent(
                                state = state,
                                onFullNameChange = viewModel::updateFullName,
                                onEmailChange = viewModel::updateEmail,
                                onPhoneChange = viewModel::updatePhone,
                                onBioChange = viewModel::updateBio,
                                onSkillsChange = viewModel::updateSkills,
                                onTechStackChange = viewModel::updateTechStack
                            )
                        }

                        AnimatedVisibility(
                            visible = !state.isEditMode,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            ViewModeContent(profile = profile)
                        }

                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    name: String,
    email: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(GradientStart),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.take(2).uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(16.dp))

            Column {
                Text(
                    text = name.ifEmpty { "Your Name" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (email.isNotEmpty()) {
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ViewModeContent(profile: com.swipeapply.app.data.model.UserProfile) {
    Column {
        // Bio Section
        if (profile.bio.isNotEmpty()) {
            ProfileSection(title = "About Me", icon = Icons.Default.Person) {
                Text(
                    text = profile.bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Contact Section
        if (profile.phone.isNotEmpty()) {
            ProfileSection(title = "Contact", icon = Icons.Default.Phone) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(profile.phone, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // Skills Section
        if (profile.skills.isNotEmpty()) {
            ProfileSection(title = "Skills", icon = Icons.Default.Star) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    profile.skills.forEach { skill ->
                        ChipItem(text = skill, color = AccentGreen)
                    }
                }
            }
        }

        // Tech Stack Section
        if (profile.techStack.isNotEmpty()) {
            ProfileSection(title = "Tech Stack", icon = Icons.Default.Code) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    profile.techStack.forEach { tech ->
                        ChipItem(text = tech, color = GradientStart)
                    }
                }
            }
        }

        // Experience Section
        if (profile.experience.isNotEmpty()) {
            ProfileSection(title = "Experience", icon = Icons.Default.Work) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    profile.experience.forEach { exp ->
                        ExperienceCard(
                            role = exp.role,
                            company = exp.company,
                            duration = exp.duration,
                            description = exp.description
                        )
                    }
                }
            }
        }

        // Education Section
        if (profile.education.isNotEmpty()) {
            ProfileSection(title = "Education", icon = Icons.Default.School) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    profile.education.forEach { edu ->
                        Row {
                            Column {
                                Text(
                                    text = edu.degree,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${edu.school} • ${edu.year}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Projects Section
        if (profile.projects.isNotEmpty()) {
            ProfileSection(title = "Projects", icon = Icons.Default.Folder) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    profile.projects.forEach { project ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = project.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                if (project.description.isNotEmpty()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = project.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (project.techUsed.isNotEmpty()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Tech: ${project.techUsed}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GradientStart
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditModeContent(
    state: com.swipeapply.app.ui.viewmodel.ProfileUiState,
    onFullNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onSkillsChange: (String) -> Unit,
    onTechStackChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = state.editFullName,
            onValueChange = onFullNameChange,
            label = { Text("Full Name *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
        )

        OutlinedTextField(
            value = state.editEmail,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) }
        )

        OutlinedTextField(
            value = state.editPhone,
            onValueChange = onPhoneChange,
            label = { Text("Phone") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) }
        )

        OutlinedTextField(
            value = state.editBio,
            onValueChange = onBioChange,
            label = { Text("Bio / About Me") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5,
            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) }
        )

        OutlinedTextField(
            value = state.editSkills,
            onValueChange = onSkillsChange,
            label = { Text("Skills (comma separated)") },
            placeholder = { Text("e.g., Android, Kotlin, Jetpack Compose") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) }
        )

        OutlinedTextField(
            value = state.editTechStack,
            onValueChange = onTechStackChange,
            label = { Text("Tech Stack (comma separated)") },
            placeholder = { Text("e.g., Kotlin, Java, Python, React") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            leadingIcon = { Icon(Icons.Default.Code, contentDescription = null) }
        )

        Text(
            text = "* Required field",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "Note: Education, Experience, and Projects can be edited by re-uploading your resume.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProfileSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = GradientStart
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        content()
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

@Composable
private fun ChipItem(text: String, color: androidx.compose.ui.graphics.Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
    }
}

@Composable
private fun ExperienceCard(
    role: String,
    company: String,
    duration: String,
    description: String
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = role,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$company • $duration",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (description.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
    }
}
