package com.swipeapply.app.data.ranking

/**
 * Structured breakdown of how well a candidate matches a job.
 * This is the OUTPUT of the deterministic ranking engine.
 * AI uses this data ONLY for generating explanations - never for ranking.
 */
data class MatchBreakdown(
    val jobId: String,
    val finalScore: Double,                    // 0.0 to 1.0
    val skillMatch: SkillMatchResult,
    val experienceMatch: ExperienceMatchResult,
    val roleAlignment: RoleAlignmentResult,
    val overallFit: FitCategory,
    val scoreComponents: ScoreComponents       // For full auditability
)

/**
 * Detailed skill matching results
 */
data class SkillMatchResult(
    val requiredMatched: List<SkillMatch>,     // Required skills the candidate has
    val requiredMissing: List<String>,         // Required skills candidate lacks
    val preferredMatched: List<SkillMatch>,    // Nice-to-have skills matched
    val preferredMissing: List<String>,        // Nice-to-have skills missing
    val bonusSkills: List<String>,             // Extra skills candidate has
    val matchPercentage: Double                // 0.0 to 1.0
)

/**
 * Individual skill match with match type
 */
data class SkillMatch(
    val candidateSkill: String,                // What the candidate listed
    val jobSkill: String,                      // What the job requires
    val matchType: MatchType,                  // How they matched
    val weight: Double                         // Contribution to score (0.0 to 1.0)
)

enum class MatchType {
    EXACT,              // Direct match (React = React)
    NORMALIZED,         // Alias match (nodejs = Node.js)
    PARTIAL,            // Taxonomy match (Vue.js partial credit for React)
    ECOSYSTEM           // Related ecosystem (AWS = S3, Lambda, etc.)
}

/**
 * Experience level matching
 */
data class ExperienceMatchResult(
    val candidateYears: Int,
    val requiredYears: Int,
    val experienceFit: ExperienceFit,
    val penalty: Double                        // 0.0 = perfect, negative = gap
)

enum class ExperienceFit {
    EXCEEDS,            // Candidate has more experience
    MATCHES,            // Within acceptable range
    SLIGHTLY_UNDER,     // 1-2 years under
    SIGNIFICANTLY_UNDER // 3+ years under
}

/**
 * Role and domain alignment
 */
data class RoleAlignmentResult(
    val candidateRoles: List<String>,          // Roles from candidate's experience
    val targetRole: String,                    // Job's role
    val domainMatch: Boolean,                  // Same domain (fintech, healthcare, etc.)
    val seniorityMatch: SeniorityMatch,
    val alignmentScore: Double                 // 0.0 to 1.0
)

enum class SeniorityMatch {
    EXACT,              // Junior -> Junior, Senior -> Senior
    STRETCH_UP,         // Candidate applying for higher level
    OVERQUALIFIED       // Candidate may be overqualified
}

/**
 * Component scores for full transparency and auditability
 */
data class ScoreComponents(
    val skillScore: Double,                    // Weight: 50%
    val experienceScore: Double,               // Weight: 25%
    val roleScore: Double,                     // Weight: 15%
    val techStackDepthScore: Double,           // Weight: 10%
    val weights: ScoreWeights = ScoreWeights()
)

data class ScoreWeights(
    val skills: Double = 0.50,
    val experience: Double = 0.25,
    val role: Double = 0.15,
    val techStackDepth: Double = 0.10
)

/**
 * Human-readable fit category derived deterministically from score
 */
enum class FitCategory(val label: String, val minScore: Double) {
    STRONG("Strong Fit", 0.75),
    MODERATE("Moderate Fit", 0.50),
    STRETCH("Stretch Role", 0.30),
    WEAK("Weak Fit", 0.0);
    
    companion object {
        fun fromScore(score: Double): FitCategory = when {
            score >= STRONG.minScore -> STRONG
            score >= MODERATE.minScore -> MODERATE
            score >= STRETCH.minScore -> STRETCH
            else -> WEAK
        }
    }
}

/**
 * Candidate's normalized profile for ranking
 */
data class CandidateProfile(
    val normalizedSkills: Set<String>,         // Canonical skill names
    val techStack: Set<String>,                // Tech stack (also normalized)
    val yearsOfExperience: Int,
    val roles: List<String>,                   // Job titles held
    val domains: Set<String>                   // Industries worked in
)

/**
 * Job's normalized requirements for ranking
 */
data class JobRequirements(
    val jobId: String,
    val requiredSkills: Set<String>,           // Must-have (normalized)
    val preferredSkills: Set<String>,          // Nice-to-have (normalized)
    val minExperience: Int,
    val maxExperience: Int?,                   // Null = no upper limit
    val targetRole: String,
    val seniority: String,                     // junior, mid, senior, lead
    val domain: String?                        // Industry/domain if specified
)
