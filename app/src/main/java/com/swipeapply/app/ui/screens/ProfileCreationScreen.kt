package com.swipeapply.app.ui.screens

import android.app.Application
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swipeapply.app.data.model.EducationItem
import com.swipeapply.app.data.model.ExperienceItem
import com.swipeapply.app.data.model.ProjectItem
import com.swipeapply.app.data.model.UserProfile
import com.swipeapply.app.ui.theme.GradientEnd
import com.swipeapply.app.ui.theme.GradientStart
import com.swipeapply.app.ui.viewmodel.ProfileCreationViewModel
import com.swipeapply.app.ui.viewmodel.ViewModelFactory

@Composable
fun ProfileCreationScreen(
    onNavigateHome: () -> Unit
) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: ProfileCreationViewModel = viewModel(
        factory = ViewModelFactory(application)
    )
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.parseResume(context, it) }
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onNavigateHome()
    }

    if (state.isLoading) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    strokeWidth = 3.dp,
                    color = GradientStart
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    "Analyzing Resume...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "This may take a few moments",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))

        if (state.profile == null) {
            Icon(
                Icons.Default.CloudUpload,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = GradientStart
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "Build Your Profile",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Upload your resume to auto-fill your profile",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(40.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(GradientStart, GradientEnd)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clip(RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                TextButton(
                    onClick = { launcher.launch("application/pdf") },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                        Text(
                            "Upload Resume (PDF)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            TextButton(onClick = { viewModel.updateProfileField(UserProfile()) }) {
                Text(
                    "Skip and fill manually",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (state.error != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            val profile = state.profile!!

            Text(
                "Verify Your Details",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Review and correct any information extracted from your resume",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))

            SectionHeader("Basic Information")

            OutlinedTextField(
                value = profile.fullName,
                onValueChange = { viewModel.updateProfileField(profile.copy(fullName = it)) },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = profile.email,
                onValueChange = { viewModel.updateProfileField(profile.copy(email = it)) },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = profile.phone,
                onValueChange = { viewModel.updateProfileField(profile.copy(phone = it)) },
                label = { Text("Phone") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = profile.bio,
                onValueChange = { viewModel.updateProfileField(profile.copy(bio = it)) },
                label = { Text("Professional Summary") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(24.dp))

            SectionHeader("Skills & Tech Stack")

            var skillsText by remember { mutableStateOf(profile.skills.joinToString(", ")) }
            OutlinedTextField(
                value = skillsText,
                onValueChange = {
                    skillsText = it
                    val list = it.split(",").map { s -> s.trim() }.filter { s -> s.isNotEmpty() }
                    viewModel.updateProfileField(profile.copy(skills = list))
                },
                label = { Text("Skills (comma separated)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))

            var techText by remember { mutableStateOf(profile.techStack.joinToString(", ")) }
            OutlinedTextField(
                value = techText,
                onValueChange = {
                    techText = it
                    val list = it.split(",").map { s -> s.trim() }.filter { s -> s.isNotEmpty() }
                    viewModel.updateProfileField(profile.copy(techStack = list))
                },
                label = { Text("Tech Stack (comma separated)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(24.dp))

            SectionHeader("Education (${profile.education.size} items)")
            profile.education.forEachIndexed { index, edu ->
                EducationCard(
                    education = edu,
                    index = index,
                    onUpdate = { updatedEdu ->
                        val newEducation = profile.education.toMutableList()
                        newEducation[index] = updatedEdu
                        viewModel.updateProfileField(profile.copy(education = newEducation))
                    },
                    onDelete = {
                        val newEducation = profile.education.toMutableList()
                        newEducation.removeAt(index)
                        viewModel.updateProfileField(profile.copy(education = newEducation))
                    }
                )
                Spacer(Modifier.height(12.dp))
            }
            Spacer(Modifier.height(12.dp))

            SectionHeader("Experience (${profile.experience.size} items)")
            profile.experience.forEachIndexed { index, exp ->
                ExperienceCard(
                    experience = exp,
                    index = index,
                    onUpdate = { updatedExp ->
                        val newExperience = profile.experience.toMutableList()
                        newExperience[index] = updatedExp
                        viewModel.updateProfileField(profile.copy(experience = newExperience))
                    },
                    onDelete = {
                        val newExperience = profile.experience.toMutableList()
                        newExperience.removeAt(index)
                        viewModel.updateProfileField(profile.copy(experience = newExperience))
                    }
                )
                Spacer(Modifier.height(12.dp))
            }
            Spacer(Modifier.height(12.dp))

            SectionHeader("Projects (${profile.projects.size} items)")
            profile.projects.forEachIndexed { index, project ->
                ProjectCard(
                    project = project,
                    index = index,
                    onUpdate = { updatedProject ->
                        val newProjects = profile.projects.toMutableList()
                        newProjects[index] = updatedProject
                        viewModel.updateProfileField(profile.copy(projects = newProjects))
                    },
                    onDelete = {
                        val newProjects = profile.projects.toMutableList()
                        newProjects.removeAt(index)
                        viewModel.updateProfileField(profile.copy(projects = newProjects))
                    }
                )
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(24.dp))

            if (state.error != null) {
                Text(
                    state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(16.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(GradientStart, GradientEnd)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clip(RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                TextButton(
                    onClick = { viewModel.saveProfile() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        "Confirm & Create Profile",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.height(32.dp).navigationBarsPadding())
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(4.dp))
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
fun EducationCard(
    education: EducationItem,
    index: Int,
    onUpdate: (EducationItem) -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Education ${index + 1}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Delete",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            OutlinedTextField(
                value = education.school,
                onValueChange = { onUpdate(education.copy(school = it)) },
                label = { Text("School/University") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = education.degree,
                onValueChange = { onUpdate(education.copy(degree = it)) },
                label = { Text("Degree/Certification") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = education.year,
                onValueChange = { onUpdate(education.copy(year = it)) },
                label = { Text("Graduation Year") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
fun ExperienceCard(
    experience: ExperienceItem,
    index: Int,
    onUpdate: (ExperienceItem) -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Experience ${index + 1}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Delete",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            OutlinedTextField(
                value = experience.company,
                onValueChange = { onUpdate(experience.copy(company = it)) },
                label = { Text("Company") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = experience.role,
                onValueChange = { onUpdate(experience.copy(role = it)) },
                label = { Text("Job Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = experience.duration,
                onValueChange = { onUpdate(experience.copy(duration = it)) },
                label = { Text("Duration (e.g., Jan 2020 - Dec 2021)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = experience.description,
                onValueChange = { onUpdate(experience.copy(description = it)) },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
fun ProjectCard(
    project: ProjectItem,
    index: Int,
    onUpdate: (ProjectItem) -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Project ${index + 1}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Delete",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            OutlinedTextField(
                value = project.name,
                onValueChange = { onUpdate(project.copy(name = it)) },
                label = { Text("Project Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = project.description,
                onValueChange = { onUpdate(project.copy(description = it)) },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = project.techUsed,
                onValueChange = { onUpdate(project.copy(techUsed = it)) },
                label = { Text("Tech Used (comma separated)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}
