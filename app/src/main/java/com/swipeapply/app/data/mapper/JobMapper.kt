package com.swipeapply.app.data.mapper

import android.util.Log
import com.swipeapply.app.data.api.model.FindWorkJob
import com.swipeapply.app.data.local.entity.JobEntity
import com.swipeapply.app.data.model.*

/**
 * Mappers to convert between API models, database entities, and domain models
 */

private const val TAG = "JobMapper"

// API to Entity
fun FindWorkJob.toEntity(): JobEntity {
    try {
        Log.d(TAG, "toEntity() - ID: $id, Company: $companyName")
        fun String?.cleanHtml(): String {
        if (this == null) return ""
        return this.replace(Regex("<[^>]*>"), "") // Strips all <tags>
            .replace("&amp;", "&")
            .replace("&nbsp;", " ")
            .replace("&quot;", "\"")
            .replace(Regex("\\s+"), " ") // Collapses extra whitespace
            .trim()
    }
        // Safely trim and validate
        val roleText = this.role.trim().takeIf { it.isNotEmpty() } ?: "Unknown Role"
        val companyText = this.companyName.trim().takeIf { it.isNotEmpty() } ?: "Unknown Company"
        val locationText = this.location?.trim() ?: "Remote"
        
        
        val entity = JobEntity(
            id = this.id.trim(),
            role = roleText,
            companyName = companyText,
            location = locationText,
            remote = this.remote,
            url = this.url?.trim(),
            description = this.text?.cleanHtml(),
            datePosted = this.datePosted,
            keywords = this.keywords?.joinToString(","),
            source = this.source,
            employmentType = this.employmentType,
            logoUrl = this.logo?.trim()
        )
        
        Log.d(TAG, "toEntity() - ✓ Success")
        return entity
    } catch (e: Exception) {
        Log.e(TAG, "toEntity() - ERROR: ${e.message}", e)
        throw e
    }
}

// Entity to Domain Model (JobCard)
fun JobEntity.toJobCard(): JobCard {
    try {
        Log.d(TAG, "toJobCard() - ID: $id, Company: $companyName")
        
        // Safely create company ID
        val companyId = this.companyName
            .lowercase()
            .replace(" ", "_")
            .replace(Regex("[^a-z0-9_]"), "")
            .takeIf { it.isNotEmpty() } ?: "unknown_company"
        
        
        val company = Company(
            id = companyId,
            name = this.companyName.trim(),
            logoUrl = this.logoUrl?.takeIf { it.isNotEmpty() },
            description = "A great company looking for talented individuals.",
            industry = extractIndustryFromKeywords(this.keywords),
            size = CompanySize.MEDIUM,
            website = extractWebsiteFromUrl(this.url)
        )
        
        val locationType = when {
            this.remote -> LocationType.REMOTE
            this.location.contains("hybrid", ignoreCase = true) -> LocationType.HYBRID
            else -> LocationType.ONSITE
        }
        
        val techStack = extractTechStack(this.keywords)
        val description = this.description?.trim()?.takeIf { it.isNotEmpty() } 
            ?: "Exciting opportunity to join ${this.companyName} as a ${this.role}."
        
        
        val jobCard = JobCard(
            id = this.id.trim(),
            company = company,
            title = this.role.trim(),
            location = this.location.trim(),
            locationType = locationType,
            salary = null,
            techStack = techStack,
            isHiringNow = true,
            roleDescription = description,
            matchReason = generateMatchReason(this.role, techStack),
            introTemplate = generateIntroTemplate(this.role, this.companyName)
        )
        
        Log.d(TAG, "toJobCard() - ✓ Success: ${jobCard.title}")
        return jobCard
    } catch (e: Exception) {
        Log.e(TAG, "toJobCard() - ERROR: ${e.message}", e)
        throw e
    }
}

// Helper functions
private fun extractIndustryFromKeywords(keywords: String?): String {
    if (keywords.isNullOrBlank()) return "Technology"
    
    val industryKeywords = mapOf(
        "fintech" to listOf("fintech", "finance", "banking", "payment"),
        "healthcare" to listOf("health", "medical", "healthcare"),
        "e-commerce" to listOf("ecommerce", "retail", "shopping"),
        "saas" to listOf("saas", "software", "platform"),
        "gaming" to listOf("game", "gaming", "entertainment"),
        "ai/ml" to listOf("ai", "ml", "machine learning", "artificial intelligence")
    )
    
    val lowerKeywords = keywords.lowercase()
    for ((industry, terms) in industryKeywords) {
        if (terms.any { lowerKeywords.contains(it) }) {
            return industry.replaceFirstChar { it.uppercase() }
        }
    }
    
    return "Technology"
}

private fun extractWebsiteFromUrl(url: String?): String? {
    if (url.isNullOrBlank()) return null
    return try {
        val domain = url.substringAfter("://").substringBefore("/")
        val cleanDomain = domain.removePrefix("www.")
        if (cleanDomain.equals("findwork.dev", ignoreCase = true)) null else cleanDomain
    } catch (e: Exception) {
        null
    }
}

private fun extractTechStack(keywords: String?): List<String> {
    if (keywords.isNullOrBlank()) return emptyList()
    
    val commonTech = listOf(
        "Kotlin", "Java", "Swift", "React", "TypeScript", "JavaScript",
        "Python", "Go", "Rust", "C++", "Node.js", "Django", "Flask",
        "React Native", "Flutter", "Jetpack Compose", "SwiftUI",
        "GraphQL", "REST", "PostgreSQL", "MongoDB", "Redis",
        "AWS", "GCP", "Azure", "Docker", "Kubernetes"
    )
    
    val lowerKeywords = keywords.lowercase()
    return commonTech.filter { tech ->
        lowerKeywords.contains(tech.lowercase())
    }.take(4)
}

private fun generateMatchReason(role: String, techStack: List<String>): String {
    val reasons = listOf(
        "Your skills align perfectly with this role.",
        "Great opportunity to work with ${techStack.joinToString(", ")}.",
        "This position matches your experience level.",
        "Strong cultural fit based on your profile.",
        "Exciting chance to grow your career in this direction."
    )
    return reasons.random()
}

private fun generateIntroTemplate(role: String, companyName: String): IntroTemplate {
    return IntroTemplate(
        subject = "$role opportunity at $companyName",
        body = """Hi [Hiring Manager],

I came across the $role position at $companyName and I'm very interested.

I believe my background and skills would be a great fit for this role, and I'm excited about the opportunity to contribute to your team.

Would you have 15 minutes for a quick chat?

Best,
[Your Name]"""
    )
}