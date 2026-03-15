package com.swipeapply.app.data.service

import android.content.Context
import android.util.Log
import com.swipeapply.app.data.model.JobCard
import com.swipeapply.app.data.model.UserProfile
import com.swipeapply.app.data.ranking.JobRankingEngine
import com.swipeapply.app.data.ranking.MatchBreakdown
import com.swipeapply.app.data.ranking.FitCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ╔════════════════════════════════════════════════════════════════════════════╗
 * ║                    DETERMINISTIC JOB RANKING SERVICE                       ║
 * ╠════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                            ║
 * ║  CRITICAL DESIGN CONSTRAINT:                                               ║
 * ║  AI must NOT participate in job ranking, scoring, sorting, or             ║
 * ║  prioritization in ANY form.                                               ║
 * ║                                                                            ║
 * ║  This service uses ONLY rule-based, deterministic logic for ranking.      ║
 * ║  NO AI, NO ML models, NO neural networks, NO LLM calls.                   ║
 * ║                                                                            ║
 * ║  The ranking is:                                                           ║
 * ║  - FAST: O(n) per job, cacheable                                          ║
 * ║  - PREDICTABLE: Same inputs always produce same outputs                   ║
 * ║  - AUDITABLE: Every score component is traceable                          ║
 * ║  - TESTABLE: Unit tests can verify exact behavior                         ║
 * ║                                                                            ║
 * ║  AI is ONLY used in FitReasoningModule for generating EXPLANATIONS        ║
 * ║  AFTER ranking is complete. AI CANNOT influence job order.                ║
 * ║                                                                            ║
 * ╚════════════════════════════════════════════════════════════════════════════╝
 */
object JobRankingService {
    private const val TAG = "JobRankingService"
    
    // Singleton reference to ranking engine (initialized lazily with context)
    private var rankingEngine: JobRankingEngine? = null
    
    // Cache for match breakdowns (job_id -> breakdown)
    private val matchCache = mutableMapOf<String, MatchBreakdown>()
    
    /**
     * Initialize the ranking engine with application context.
     * Must be called before using rankJobs().
     */
    fun initialize(context: Context) {
        if (rankingEngine == null) {
            rankingEngine = JobRankingEngine.getInstance(context)
            Log.d(TAG, "Deterministic ranking engine initialized")
        }
    }
    
    /**
     * Represents a job's ranking score (deterministic, not AI-generated)
     */
    data class JobRankScore(
        val jobId: String,
        val score: Int,                        // 0-100 (derived from finalScore * 100)
        val fitCategory: FitCategory,          // Strong/Moderate/Stretch/Weak
        val matchBreakdown: MatchBreakdown?    // Full audit trail
    )

    /**
     * Ranks a list of jobs based on how well they match the user's profile.
     * 
     * USES DETERMINISTIC ALGORITHM ONLY - NO AI.
     * 
     * The ranking formula:
     * - 50% Skill Match (required + preferred skills)
     * - 25% Experience Match (years of experience)
     * - 15% Role Alignment (title, seniority, domain)
     * - 10% Tech Stack Depth (ecosystem knowledge)
     * 
     * @param jobs List of JobCards to rank
     * @param userProfile User's profile containing skills, experience, etc.
     * @return List of JobCards sorted by relevance (best match first)
     */
    suspend fun rankJobs(
        jobs: List<JobCard>,
        userProfile: UserProfile
    ): List<JobCard> = withContext(Dispatchers.Default) {
        if (jobs.isEmpty()) return@withContext jobs
        
        val engine = rankingEngine
        if (engine == null) {
            Log.w(TAG, "Ranking engine not initialized, using quick match fallback")
            return@withContext jobs.sortedByDescending { quickMatchScore(it, userProfile) }
        }
        
        if (userProfile.techStack.isEmpty() && userProfile.skills.isEmpty()) {
            Log.w(TAG, "User profile is empty, using quick match fallback")
            return@withContext jobs.sortedByDescending { quickMatchScore(it, userProfile) }
        }

        try {
            Log.d(TAG, "Starting deterministic ranking for ${jobs.size} jobs")
            
            // Extract candidate profile
            val candidateProfile = engine.extractCandidateProfile(userProfile)
            
            // Compute match for each job (deterministic)
            val rankedJobs = jobs.map { job ->
                val jobRequirements = engine.extractJobRequirements(job)
                val matchBreakdown = engine.computeMatch(candidateProfile, jobRequirements)
                
                // Cache the breakdown for later explanation generation
                matchCache[job.id] = matchBreakdown
                
                Pair(job, matchBreakdown)
            }
            
            // Sort by final score (descending) - DETERMINISTIC
            val sortedJobs = rankedJobs
                .sortedByDescending { it.second.finalScore }
                .map { it.first }
            
            Log.d(TAG, "Deterministic ranking complete. Top job: ${sortedJobs.firstOrNull()?.title}")
            logRankingSummary(rankedJobs)
            
            return@withContext sortedJobs

        } catch (e: Exception) {
            Log.e(TAG, "Ranking failed, using quick match fallback: ${e.message}", e)
            return@withContext jobs.sortedByDescending { quickMatchScore(it, userProfile) }
        }
    }
    
    /**
     * Get the cached match breakdown for a job.
     * Returns null if not yet computed.
     */
    fun getMatchBreakdown(jobId: String): MatchBreakdown? {
        return matchCache[jobId]
    }
    
    /**
     * Get ranking scores for all cached jobs.
     */
    fun getAllRankScores(): List<JobRankScore> {
        return matchCache.map { (jobId, breakdown) ->
            JobRankScore(
                jobId = jobId,
                score = (breakdown.finalScore * 100).toInt(),
                fitCategory = breakdown.overallFit,
                matchBreakdown = breakdown
            )
        }.sortedByDescending { it.score }
    }
    
    /**
     * Clear the match cache.
     */
    fun clearCache() {
        matchCache.clear()
        Log.d(TAG, "Match cache cleared")
    }
    
    /**
     * Log a summary of the ranking for debugging.
     */
    private fun logRankingSummary(rankedJobs: List<Pair<JobCard, MatchBreakdown>>) {
        if (rankedJobs.isEmpty()) return
        
        val summary = rankedJobs.take(5).mapIndexed { index, (job, breakdown) ->
            "#${index + 1}: ${job.title} - ${breakdown.overallFit.label} " +
            "(${String.format("%.0f", breakdown.finalScore * 100)}%)"
        }.joinToString("\n")
        
        Log.d(TAG, "Top 5 ranked jobs:\n$summary")
    }

    /**
     * Quick relevance check without full ranking engine.
     * DETERMINISTIC - uses simple set intersection.
     * 
     * Used as fallback when:
     * - Engine not initialized
     * - Profile is empty
     * - Full ranking fails
     */
    fun quickMatchScore(job: JobCard, userProfile: UserProfile): Int {
        if (userProfile.techStack.isEmpty() && userProfile.skills.isEmpty()) {
            return 50 // Neutral score if no profile
        }

        val userTechSet = (userProfile.techStack + userProfile.skills)
            .map { normalizeSkill(it) }
            .toSet()

        val jobTechSet = job.techStack
            .map { normalizeSkill(it) }
            .toSet()

        if (jobTechSet.isEmpty()) return 50

        // Calculate overlap with partial matching
        var score = 0.0
        for (jobSkill in jobTechSet) {
            val exactMatch = userTechSet.contains(jobSkill)
            if (exactMatch) {
                score += 1.0
            } else {
                // Check for partial matches (substring)
                val partialMatch = userTechSet.any { 
                    it.contains(jobSkill) || jobSkill.contains(it) 
                }
                if (partialMatch) {
                    score += 0.5
                }
            }
        }

        val overlapRatio = score / jobTechSet.size
        return (overlapRatio * 100).toInt().coerceIn(0, 100)
    }
    
    /**
     * Simple skill normalization for quick matching.
     */
    private fun normalizeSkill(skill: String): String {
        return skill.lowercase()
            .replace(Regex("[^a-z0-9.+#]"), "")
            .trim()
    }
}
