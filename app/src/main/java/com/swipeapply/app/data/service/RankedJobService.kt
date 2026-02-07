package com.swipeapply.app.data.service

import android.content.Context
import android.util.Log
import com.swipeapply.app.data.model.JobCard
import com.swipeapply.app.data.model.UserProfile
import com.swipeapply.app.data.ranking.JobRankingEngine
import com.swipeapply.app.data.ranking.MatchBreakdown
import com.swipeapply.app.data.ai.FitReasoningModule
import com.swipeapply.app.data.ai.ExplanationCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * RANKED JOB SERVICE
 * 
 * Orchestrates the job discovery flow:
 * 1. Jobs are fetched by JobRepository (unchanged)
 * 2. Jobs are RANKED by JobRankingEngine (deterministic, no AI)
 * 3. Explanations are generated LAZILY by FitReasoningModule (AI, on-demand only)
 * 
 * SAFETY GUARANTEES:
 * - Ranking is 100% deterministic and auditable
 * - AI cannot influence job order
 * - Explanations are cached per user-job pair
 * - All scores are traceable via MatchBreakdown
 */
class RankedJobService(private val context: Context) {
    
    companion object {
        private const val TAG = "RankedJobService"
        
        @Volatile
        private var INSTANCE: RankedJobService? = null
        
        fun getInstance(context: Context): RankedJobService {
            return INSTANCE ?: synchronized(this) {
                RankedJobService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val rankingEngine = JobRankingEngine.getInstance(context)
    private val fitReasoningModule = FitReasoningModule.getInstance()
    
    // Cache for match breakdowns (for quick lookup when generating explanations)
    private val matchCache = mutableMapOf<String, MatchBreakdown>()
    
    /**
     * Rank a list of jobs for a candidate.
     * Returns jobs sorted by relevance score (highest first).
     * 
     * This is a DETERMINISTIC operation - same inputs always produce same order.
     */
    suspend fun rankJobsForCandidate(
        jobs: List<JobCard>,
        userProfile: UserProfile
    ): List<RankedJob> = withContext(Dispatchers.Default) {
        Log.d(TAG, "Ranking ${jobs.size} jobs for candidate")
        
        if (jobs.isEmpty()) return@withContext emptyList()
        
        // Extract candidate profile
        val candidateProfile = rankingEngine.extractCandidateProfile(userProfile)
        
        // Rank each job
        val rankedJobs = jobs.map { job ->
            val jobRequirements = rankingEngine.extractJobRequirements(job)
            val matchBreakdown = rankingEngine.computeMatch(candidateProfile, jobRequirements)
            
            // Cache for later explanation generation
            matchCache[job.id] = matchBreakdown
            
            RankedJob(
                job = job,
                matchBreakdown = matchBreakdown,
                rank = 0 // Will be set after sorting
            )
        }.sortedByDescending { it.matchBreakdown.finalScore }
        
        // Assign ranks
        rankedJobs.mapIndexed { index, rankedJob ->
            rankedJob.copy(rank = index + 1)
        }.also {
            Log.d(TAG, "Ranking complete. Top job: ${it.firstOrNull()?.job?.title} " +
                    "(score: ${it.firstOrNull()?.matchBreakdown?.finalScore})")
        }
    }
    
    /**
     * Get the match breakdown for a specific job.
     * Returns cached value if available.
     */
    fun getMatchBreakdown(jobId: String): MatchBreakdown? {
        return matchCache[jobId]
    }
    
    /**
     * Generate a fit explanation for a job.
     * This is LAZY - only called when user clicks "Why this job?"
     * 
     * Uses AI for natural language generation, but does NOT influence ranking.
     */
    suspend fun getJobExplanation(
        job: JobCard,
        userProfile: UserProfile,
        userId: String
    ): FitReasoningModule.FitExplanation = withContext(Dispatchers.Default) {
        val cacheKey = fitReasoningModule.getCacheKey(userId, job.id)
        
        // Check cache first
        ExplanationCache.get(cacheKey)?.let { 
            Log.d(TAG, "Returning cached explanation for job: ${job.id}")
            return@withContext it 
        }
        
        // Get or compute match breakdown
        val matchBreakdown = matchCache[job.id] ?: run {
            val candidateProfile = rankingEngine.extractCandidateProfile(userProfile)
            val jobRequirements = rankingEngine.extractJobRequirements(job)
            rankingEngine.computeMatch(candidateProfile, jobRequirements).also {
                matchCache[job.id] = it
            }
        }
        
        // Generate explanation
        Log.d(TAG, "Generating explanation for job: ${job.id}")
        val explanation = fitReasoningModule.generateExplanation(userProfile, job, matchBreakdown)
        
        // Cache it
        ExplanationCache.put(cacheKey, explanation)
        
        explanation
    }
    
    /**
     * Get the AI prompt that would be used for LLM-based explanation.
     * Useful for debugging and transparency.
     */
    fun getExplanationPrompt(
        job: JobCard,
        userProfile: UserProfile
    ): String? {
        val matchBreakdown = matchCache[job.id] ?: return null
        return fitReasoningModule.generateAIPrompt(userProfile, job, matchBreakdown)
    }
    
    /**
     * Clear all caches.
     */
    fun clearCaches() {
        matchCache.clear()
        ExplanationCache.clear()
        Log.d(TAG, "All caches cleared")
    }
}

/**
 * A job with its ranking information.
 */
data class RankedJob(
    val job: JobCard,
    val matchBreakdown: MatchBreakdown,
    val rank: Int
) {
    /**
     * Get a quick summary for UI display.
     */
    fun getQuickSummary(): String {
        val fit = matchBreakdown.overallFit.label
        val skillPercent = (matchBreakdown.skillMatch.matchPercentage * 100).toInt()
        return "$fit • $skillPercent% skill match"
    }
    
    /**
     * Check if this is a strong match (for badge/highlight).
     */
    fun isStrongMatch(): Boolean = matchBreakdown.finalScore >= 0.75
    
    /**
     * Check if this is a stretch role (for warning).
     */
    fun isStretchRole(): Boolean = matchBreakdown.finalScore < 0.50
}
