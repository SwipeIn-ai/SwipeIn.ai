package com.swipeapply.app.data.model

/**
 * Represents a company with job listings
 */
data class Company(
    val id: String,
    val name: String,
    val logoUrl: String? = null,
    val description: String,
    val industry: String,
    val size: CompanySize,
    val founded: Int? = null,
    val website: String? = null
)

enum class CompanySize(val label: String) {
    STARTUP("1-10"),
    SMALL("11-50"),
    MEDIUM("51-200"),
    LARGE("201-1000"),
    ENTERPRISE("1000+")
}
