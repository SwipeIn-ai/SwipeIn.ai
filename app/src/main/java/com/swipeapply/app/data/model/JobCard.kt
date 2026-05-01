package com.swipeapply.app.data.model

/**
 * Represents a job posting card for swiping
 */
data class JobCard(
    val id: String,
    val company: Company,
    val title: String,
    val location: String,
    val techStack: List<String>,
    val isHiringNow: Boolean = true,
    val roleDescription: String,
    val matchReason: String,
    val introTemplate: IntroTemplate
)
