package com.swipeapply.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * API response model for the Employee Search endpoint.
 * Endpoint: POST /api/v1/companies/{identifier}/employees
 */
data class EmployeeSearchResponse(
    @SerializedName("companyDomain")
    val companyDomain: String = "",
    @SerializedName("companyName")
    val companyName: String? = null,
    @SerializedName("fromCache")
    val fromCache: Boolean = false,
    @SerializedName("source")
    val source: String = "",
    @SerializedName("employees")
    val employees: List<Employee> = emptyList(),
    @SerializedName("totalAvailable")
    val totalAvailable: Int = 0,
    @SerializedName("remainingSwipes")
    val remainingSwipes: Int = 0,
    @SerializedName("message")
    val message: String? = null
)

/**
 * Represents an employee contact found via the API.
 * Used to display referral contact cards to the user.
 */
data class Employee(
    @SerializedName("fullName")
    val fullName: String = "",
    @SerializedName("email")
    val email: String = "",
    @SerializedName("emailStatus")
    val emailStatus: String = "",  // "verified", "likely", etc.
    @SerializedName("jobTitle")
    val jobTitle: String? = null,
    @SerializedName("seniority")
    val seniority: String? = null,
    @SerializedName("department")
    val department: String? = null,
    @SerializedName("linkedinUrl")
    val linkedinUrl: String? = null,
    @SerializedName("city")
    val city: String? = null,
    @SerializedName("state")
    val state: String? = null,
    @SerializedName("country")
    val country: String? = null,
    @SerializedName("confidence")
    val confidence: Int = 0
) {
    /**
     * Get formatted location string
     */
    fun getFormattedLocation(): String {
        val parts = listOfNotNull(city, state, country).filter { it.isNotBlank() }
        return parts.joinToString(", ").ifEmpty { "Location unknown" }
    }

    /**
     * Get confidence label and color tier
     */
    fun getConfidenceTier(): ConfidenceTier {
        return when {
            confidence >= 90 -> ConfidenceTier.HIGH
            confidence >= 70 -> ConfidenceTier.MEDIUM
            else -> ConfidenceTier.LOW
        }
    }
    
    /**
     * Get initials from full name
     */
    fun getInitials(): String {
        return fullName.split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .joinToString("")
    }
}

enum class ConfidenceTier(val label: String) {
    HIGH("Verified"),
    MEDIUM("Likely"),
    LOW("Uncertain")
}
