package com.swipeapply.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swipeapply.app.ui.viewmodel.ProfileCreationViewModel
import com.swipeapply.app.ui.viewmodel.ViewModelFactory
import com.swipeapply.app.data.model.UserProfile
import com.swipeapply.app.data.model.EducationItem
import com.swipeapply.app.data.model.ExperienceItem
import com.swipeapply.app.data.model.ProjectItem
import android.app.Application

@Composable
fun ProfileCreationScreen(
    onNavigateHome: () -> Unit
) {
    // ViewModel setup matching HomeScreen architecture
    val application = LocalContext.current.applicationContext as Application
    val viewModel: ProfileCreationViewModel = viewModel(
        factory = ViewModelFactory(application)
    )
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // File Picker
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.parseResume(context, it) }
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onNavigateHome()
    }

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("Analyzing Resume, This may take a few moments...", style = MaterialTheme.typography.bodyMedium)
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (state.profile == null) {
            // Upload View
            Text("Build Your Profile", style = MaterialTheme.typography.headlineMedium)
            Text("Upload your resume to auto-fill your profile", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = { launcher.launch("application/pdf") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Upload Resume (PDF)")
            }
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = { viewModel.updateProfileField(UserProfile()) }) {
                Text("Skip and fill manually")
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
            // Verification View
            val profile = state.profile!!
            
            Text("Verify Your Details", style = MaterialTheme.typography.headlineMedium)
            Text("Please review and correct any information extracted from your resume", 
                style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(24.dp))

            // ===== BASIC INFO SECTION =====
            SectionHeader("Basic Information")
            
            OutlinedTextField(
                value = profile.fullName,
                onValueChange = { viewModel.updateProfileField(profile.copy(fullName = it)) },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = profile.email,
                onValueChange = { viewModel.updateProfileField(profile.copy(email = it)) },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = profile.phone,
                onValueChange = { viewModel.updateProfileField(profile.copy(phone = it)) },
                label = { Text("Phone") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = profile.bio,
                onValueChange = { viewModel.updateProfileField(profile.copy(bio = it)) },
                label = { Text("Professional Summary") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            Spacer(Modifier.height(24.dp))

            // ===== SKILLS & TECH STACK SECTION =====
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
                minLines = 2
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
                minLines = 2
            )
            Spacer(Modifier.height(24.dp))

            // ===== EDUCATION SECTION =====
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

            // ===== EXPERIENCE SECTION =====
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

            // ===== PROJECTS SECTION =====
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

            Button(
                onClick = { viewModel.saveProfile() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Confirm & Create Profile")
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.fillMaxWidth()
    )
    Divider(modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
fun EducationCard(
    education: EducationItem,
    index: Int,
    onUpdate: (EducationItem) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Education ${index + 1}", style = MaterialTheme.typography.titleSmall)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Close, contentDescription = "Delete")
                }
            }

            OutlinedTextField(
                value = education.school,
                onValueChange = { onUpdate(education.copy(school = it)) },
                label = { Text("School/University") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = education.degree,
                onValueChange = { onUpdate(education.copy(degree = it)) },
                label = { Text("Degree/Certification") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = education.year,
                onValueChange = { onUpdate(education.copy(year = it)) },
                label = { Text("Graduation Year") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Experience ${index + 1}", style = MaterialTheme.typography.titleSmall)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Close, contentDescription = "Delete")
                }
            }

            OutlinedTextField(
                value = experience.company,
                onValueChange = { onUpdate(experience.copy(company = it)) },
                label = { Text("Company") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = experience.role,
                onValueChange = { onUpdate(experience.copy(role = it)) },
                label = { Text("Job Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = experience.duration,
                onValueChange = { onUpdate(experience.copy(duration = it)) },
                label = { Text("Duration (e.g., Jan 2020 - Dec 2021)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = experience.description,
                onValueChange = { onUpdate(experience.copy(description = it)) },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Project ${index + 1}", style = MaterialTheme.typography.titleSmall)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Close, contentDescription = "Delete")
                }
            }

            OutlinedTextField(
                value = project.name,
                onValueChange = { onUpdate(project.copy(name = it)) },
                label = { Text("Project Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = project.description,
                onValueChange = { onUpdate(project.copy(description = it)) },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = project.techUsed,
                onValueChange = { onUpdate(project.copy(techUsed = it)) },
                label = { Text("Tech Used (comma separated)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}