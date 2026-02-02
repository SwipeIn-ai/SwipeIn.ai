package com.swipeapply.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val bio: String = "",
    val skills: List<String> = emptyList(),
    val techStack: List<String> = emptyList(),
    val education: List<EducationItem> = emptyList(),
    val experience: List<ExperienceItem> = emptyList(),
    val projects: List<ProjectItem> = emptyList()
)

@Serializable
data class EducationItem(
    val school: String = "",
    val degree: String = "",
    val year: String = ""
)

@Serializable
data class ExperienceItem(
    val company: String = "",
    val role: String = "",
    val duration: String = "",
    val description: String = ""
)

@Serializable
data class ProjectItem(
    val name: String = "",
    val description: String = "",
    val techUsed: String = "" 
)