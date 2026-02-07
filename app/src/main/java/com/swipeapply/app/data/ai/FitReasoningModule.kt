package com.swipeapply.app.data.ai

import android.util.Log
import com.swipeapply.app.data.model.UserProfile
import com.swipeapply.app.data.model.JobCard
import com.swipeapply.app.data.ranking.MatchBreakdown
import com.swipeapply.app.data.ranking.MatchType
import com.swipeapply.app.data.ranking.ExperienceFit
import com.swipeapply.app.data.ranking.SeniorityMatch
import com.swipeapply.app.data.ranking.FitCategory

/**
 * AI FIT REASONING MODULE (Explainability Only)
 * 
 * ╔════════════════════════════════════════════════════════════════════╗
 * ║  CRITICAL SAFETY CONSTRAINT                                         ║
 * ║                                                                      ║
 * ║  This module generates EXPLANATIONS only.                            ║
 * ║  It does NOT and MUST NOT:                                           ║
 * ║  - Score jobs                                                        ║
 * ║  - Rank jobs                                                         ║
 * ║  - Influence job order                                               ║
 * ║  - Compare jobs                                                      ║
 * ║  - Infer or invent qualifications                                   ║
 * ║                                                                      ║
 * ║  AI receives the PRECOMPUTED MatchBreakdown from the deterministic  ║
 * ║  ranking engine and generates a human-readable explanation.          ║
 * ╚════════════════════════════════════════════════════════════════════╝
 * 
 * The explanation helps users understand:
 * - "Why does this job fit me?"
 * - "Why might this job not be a good fit?"
 */
class FitReasoningModule {
    
    companion object {
        private const val TAG = "FitReasoningModule"
        
        @Volatile
        private var INSTANCE: FitReasoningModule? = null
        
        fun getInstance(): FitReasoningModule {
            return INSTANCE ?: synchronized(this) {
                FitReasoningModule().also { INSTANCE = it }
            }
        }
    }
    
    /**
     * Output format for AI-generated explanations.
     */
    data class FitExplanation(
        val jobId: String,
        val fitSummary: String,                    // "Strong Fit", "Moderate Fit", "Stretch Role"
        val strengthPoints: List<String>,          // What matches well
        val gapPoints: List<String>,               // What's missing or weak
        val experienceNote: String,                // Experience alignment statement
        val conclusion: String,                    // Brief "why this role may suit you"
        val generatedAt: Long = System.currentTimeMillis()
    )
    
    /**
     * Generate a fit explanation from the precomputed match breakdown.
     * 
     * NOTE: In production, this would call an LLM API with the structured prompt.
     * Here we generate the explanation deterministically as a template, 
     * demonstrating the data flow. An LLM call would make it more natural.
     */
    fun generateExplanation(
        candidate: UserProfile,
        job: JobCard,
        matchBreakdown: MatchBreakdown
    ): FitExplanation {
        Log.d(TAG, "Generating explanation for job: ${job.id}")
        
        // Build strength points
        val strengths = buildStrengthPoints(matchBreakdown, candidate)
        
        // Build gap points
        val gaps = buildGapPoints(matchBreakdown)
        
        // Build experience note
        val experienceNote = buildExperienceNote(matchBreakdown)
        
        // Build conclusion
        val conclusion = buildConclusion(matchBreakdown, job)
        
        return FitExplanation(
            jobId = job.id,
            fitSummary = matchBreakdown.overallFit.label,
            strengthPoints = strengths,
            gapPoints = gaps,
            experienceNote = experienceNote,
            conclusion = conclusion
        )
    }
    
    /**
     * Generate the AI prompt for LLM-based explanation generation.
     * This is the template that would be sent to an AI model.
     */
    fun generateAIPrompt(
        candidate: UserProfile,
        job: JobCard,
        matchBreakdown: MatchBreakdown
    ): String {
        return """
            |## SYSTEM INSTRUCTIONS (STRICT)
            |
            |You are an AI assistant generating job fit explanations. You MUST follow these rules:
            |
            |1. Do NOT score, rank, or compare jobs
            |2. Do NOT introduce skills or qualifications not in the provided data
            |3. Base ALL reasoning strictly on the Match Breakdown provided
            |4. Clearly separate Strengths from Gaps
            |5. Maintain a neutral, trust-building tone
            |6. Do NOT use superlatives or exaggerated claims
            |
            |## INPUT: CANDIDATE PROFILE
            |
            |Name: ${candidate.fullName}
            |Skills: ${candidate.skills.joinToString(", ")}
            |Tech Stack: ${candidate.techStack.joinToString(", ")}
            |Experience: ${candidate.experience.map { "${it.role} at ${it.company} (${it.duration})" }.joinToString("; ")}
            |
            |## INPUT: JOB DETAILS
            |
            |Title: ${job.title}
            |Company: ${job.company.name}
            |Location: ${job.location} (${job.locationType.label})
            |Tech Stack Required: ${job.techStack.joinToString(", ")}
            |
            |## INPUT: PRECOMPUTED MATCH BREAKDOWN (From Ranking Engine)
            |
            |Overall Fit: ${matchBreakdown.overallFit.label}
            |Final Score: ${String.format("%.2f", matchBreakdown.finalScore)}
            |
            |### Skill Match (${String.format("%.0f", matchBreakdown.skillMatch.matchPercentage * 100)}%)
            |
            |Required Skills Matched:
            |${matchBreakdown.skillMatch.requiredMatched.joinToString("\n") { 
                "- ${it.candidateSkill} ↔ ${it.jobSkill} (${it.matchType.name})"
            }.ifEmpty { "- None" }}
            |
            |Required Skills Missing:
            |${matchBreakdown.skillMatch.requiredMissing.joinToString("\n") { "- $it" }.ifEmpty { "- None" }}
            |
            |Preferred Skills Matched:
            |${matchBreakdown.skillMatch.preferredMatched.joinToString("\n") { 
                "- ${it.candidateSkill} ↔ ${it.jobSkill} (${it.matchType.name})"
            }.ifEmpty { "- None" }}
            |
            |Preferred Skills Missing:
            |${matchBreakdown.skillMatch.preferredMissing.joinToString("\n") { "- $it" }.ifEmpty { "- None" }}
            |
            |Bonus Skills (Extra):
            |${matchBreakdown.skillMatch.bonusSkills.joinToString(", ").ifEmpty { "None" }}
            |
            |### Experience Match
            |
            |Candidate Years: ${matchBreakdown.experienceMatch.candidateYears}
            |Required Years: ${matchBreakdown.experienceMatch.requiredYears}
            |Fit: ${matchBreakdown.experienceMatch.experienceFit.name}
            |
            |### Role Alignment
            |
            |Target Role: ${matchBreakdown.roleAlignment.targetRole}
            |Candidate Roles: ${matchBreakdown.roleAlignment.candidateRoles.joinToString(", ")}
            |Seniority Match: ${matchBreakdown.roleAlignment.seniorityMatch.name}
            |Domain Match: ${matchBreakdown.roleAlignment.domainMatch}
            |
            |## OUTPUT FORMAT
            |
            |Provide a JSON response with this structure:
            |{
            |  "fitSummary": "Strong Fit | Moderate Fit | Stretch Role | Weak Fit",
            |  "strengthPoints": ["point 1", "point 2", ...],
            |  "gapPoints": ["gap 1", "gap 2", ...],
            |  "experienceNote": "One sentence about experience alignment",
            |  "conclusion": "2-3 sentences explaining why this role may or may not suit the candidate"
            |}
            |
            |Remember: You are explaining a PRECOMPUTED match. Do not recalculate or reinterpret the scores.
        """.trimMargin()
    }
    
    /**
     * Build strength points from match breakdown.
     */
    private fun buildStrengthPoints(breakdown: MatchBreakdown, candidate: UserProfile): List<String> {
        val strengths = mutableListOf<String>()
        
        // Skill matches
        val exactMatches = breakdown.skillMatch.requiredMatched.filter { it.matchType == MatchType.EXACT }
        if (exactMatches.isNotEmpty()) {
            val skillList = exactMatches.take(3).joinToString(", ") { it.jobSkill }
            strengths.add("Direct experience with required technologies: $skillList")
        }
        
        val partialMatches = breakdown.skillMatch.requiredMatched.filter { it.matchType == MatchType.PARTIAL }
        if (partialMatches.isNotEmpty()) {
            strengths.add("Related skills that transfer well: ${partialMatches.take(2).joinToString(", ") { "${it.candidateSkill} (similar to ${it.jobSkill})" }}")
        }
        
        // Experience fit
        when (breakdown.experienceMatch.experienceFit) {
            ExperienceFit.EXCEEDS -> strengths.add("Experience exceeds requirements (${breakdown.experienceMatch.candidateYears} years)")
            ExperienceFit.MATCHES -> strengths.add("Experience level aligns well with the role")
            else -> {}
        }
        
        // Role alignment
        if (breakdown.roleAlignment.seniorityMatch == SeniorityMatch.EXACT) {
            strengths.add("Seniority level matches the position")
        }
        if (breakdown.roleAlignment.domainMatch) {
            strengths.add("Relevant industry/domain experience")
        }
        
        // Bonus skills
        if (breakdown.skillMatch.bonusSkills.isNotEmpty()) {
            strengths.add("Additional relevant skills: ${breakdown.skillMatch.bonusSkills.take(3).joinToString(", ")}")
        }
        
        return strengths
    }
    
    /**
     * Build gap points from match breakdown.
     */
    private fun buildGapPoints(breakdown: MatchBreakdown): List<String> {
        val gaps = mutableListOf<String>()
        
        // Missing required skills
        if (breakdown.skillMatch.requiredMissing.isNotEmpty()) {
            gaps.add("Missing required skills: ${breakdown.skillMatch.requiredMissing.take(3).joinToString(", ")}")
        }
        
        // Missing preferred skills
        if (breakdown.skillMatch.preferredMissing.isNotEmpty()) {
            val missing = breakdown.skillMatch.preferredMissing.take(2).joinToString(", ")
            gaps.add("Nice-to-have skills to develop: $missing")
        }
        
        // Experience gaps
        when (breakdown.experienceMatch.experienceFit) {
            ExperienceFit.SLIGHTLY_UNDER -> {
                val gap = breakdown.experienceMatch.requiredYears - breakdown.experienceMatch.candidateYears
                gaps.add("Slightly under experience requirement by ~$gap year(s)")
            }
            ExperienceFit.SIGNIFICANTLY_UNDER -> {
                val gap = breakdown.experienceMatch.requiredYears - breakdown.experienceMatch.candidateYears
                gaps.add("Experience gap of $gap years - may be a stretch")
            }
            else -> {}
        }
        
        // Seniority stretch
        if (breakdown.roleAlignment.seniorityMatch == SeniorityMatch.STRETCH_UP) {
            gaps.add("Role is at a higher seniority level than current experience")
        }
        
        return gaps
    }
    
    /**
     * Build experience note from match breakdown.
     */
    private fun buildExperienceNote(breakdown: MatchBreakdown): String {
        val exp = breakdown.experienceMatch
        return when (exp.experienceFit) {
            ExperienceFit.EXCEEDS -> 
                "With ${exp.candidateYears} years of experience, you exceed the ${exp.requiredYears}+ year requirement."
            ExperienceFit.MATCHES -> 
                "Your ${exp.candidateYears} years of experience meets the role's ${exp.requiredYears} year requirement."
            ExperienceFit.SLIGHTLY_UNDER -> 
                "At ${exp.candidateYears} years, you're slightly under the ${exp.requiredYears} year target, but transferable skills may bridge the gap."
            ExperienceFit.SIGNIFICANTLY_UNDER -> 
                "This role seeks ${exp.requiredYears}+ years; with ${exp.candidateYears} years, this would be a significant stretch."
        }
    }
    
    /**
     * Build conclusion from match breakdown.
     */
    private fun buildConclusion(breakdown: MatchBreakdown, job: JobCard): String {
        return when (breakdown.overallFit) {
            FitCategory.STRONG -> 
                "Based on your profile, this ${job.title} role at ${job.company.name} appears to be a strong match. " +
                "Your skill set aligns well with the requirements, and your experience level fits the position."
                
            FitCategory.MODERATE -> 
                "This ${job.title} position shows moderate alignment with your background. " +
                "While you have relevant skills, there are some gaps that could be addressed through learning or on-the-job training."
                
            FitCategory.STRETCH -> 
                "This role would be a stretch based on your current profile. " +
                "It could be a growth opportunity if you're looking to develop new skills, but expect a steeper learning curve."
                
            FitCategory.WEAK -> 
                "This position may not be the best fit based on the current requirements. " +
                "Consider roles more aligned with your ${breakdown.skillMatch.bonusSkills.firstOrNull() ?: "existing"} expertise."
        }
    }
    
    /**
     * Create a cached key for explanation lookups.
     */
    fun getCacheKey(userId: String, jobId: String): String {
        return "${userId}_${jobId}"
    }
}

/**
 * Simple in-memory cache for explanations.
 * In production, use a proper caching solution (Room, SharedPreferences, etc.)
 */
object ExplanationCache {
    private val cache = mutableMapOf<String, FitReasoningModule.FitExplanation>()
    private const val MAX_SIZE = 100
    private const val TTL_MS = 24 * 60 * 60 * 1000L // 24 hours
    
    fun get(key: String): FitReasoningModule.FitExplanation? {
        val explanation = cache[key] ?: return null
        
        // Check TTL
        if (System.currentTimeMillis() - explanation.generatedAt > TTL_MS) {
            cache.remove(key)
            return null
        }
        
        return explanation
    }
    
    fun put(key: String, explanation: FitReasoningModule.FitExplanation) {
        // Simple LRU eviction
        if (cache.size >= MAX_SIZE) {
            val oldest = cache.entries.minByOrNull { it.value.generatedAt }
            oldest?.key?.let { cache.remove(it) }
        }
        cache[key] = explanation
    }
    
    fun clear() {
        cache.clear()
    }
}
