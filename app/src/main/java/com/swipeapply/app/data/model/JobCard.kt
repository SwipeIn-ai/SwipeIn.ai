package com.swipeapply.app.data.model

/**
 * Represents a job posting card for swiping
 */
data class JobCard(
    val id: String,
    val company: Company,
    val title: String,
    val location: String,
    val locationType: LocationType,
    val salary: SalaryRange? = null,
    val techStack: List<String>,
    val isHiringNow: Boolean = true,
    val roleDescription: String,
    val matchReason: String,
    val introTemplate: IntroTemplate
)

data class SalaryRange(
    val min: Int,
    val max: Int,
    val currency: String = "USD"
) {
    fun formatted(): String = "$${min / 1000}k - $${max / 1000}k"
}

enum class LocationType(val label: String) {
    REMOTE("Remote"),
    HYBRID("Hybrid"),
    ONSITE("On-site")
}
