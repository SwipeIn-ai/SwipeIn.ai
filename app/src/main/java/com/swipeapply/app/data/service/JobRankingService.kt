package com.swipeapply.app.data.service

import android.util.Log
import com.swipeapply.app.BuildConfig
import com.swipeapply.app.data.model.JobCard
import com.swipeapply.app.data.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Service for AI-powered job ranking using OpenRouter API.
 * 
 * This service takes a user's profile and a list of jobs, then uses an LLM
 * to rank the jobs based on how well they match the user's skills and experience.
 */
object JobRankingService {
    private const val TAG = "JobRankingService"
    private const val OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions"
    
    // Using a free/cheap model for ranking - can be changed
    private const val MODEL = "tngtech/deepseek-r1t2-chimera:free"
    
    // Fallback model if primary fails
    private const val FALLBACK_MODEL = "tngtech/deepseek-r1t2-chimera:free"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Represents a job's ranking score from the AI
     */
    data class JobRankScore(
        val jobId: String,
        val score: Int, // 0-100
        val reason: String = ""
    )

    /**
     * Ranks a list of jobs based on how well they match the user's profile.
     * 
     * @param jobs List of JobCards to rank
     * @param userProfile User's profile containing skills, experience, etc.
     * @return List of JobCards sorted by relevance (best match first), or original list on failure
     */
    suspend fun rankJobs(
        jobs: List<JobCard>,
        userProfile: UserProfile
    ): List<JobCard> = withContext(Dispatchers.IO) {
        if (jobs.isEmpty()) return@withContext jobs
        if (userProfile.techStack.isEmpty() && userProfile.skills.isEmpty() && userProfile.bio.isEmpty()) {
            Log.w(TAG, "User profile is empty, skipping AI ranking")
            return@withContext jobs
        }

        val apiKey = BuildConfig.OPENROUTER_API_KEY
        if (apiKey.isBlank()) {
            Log.e(TAG, "OpenRouter API key is not configured")
            return@withContext jobs
        }

        try {
            val rankings = callOpenRouterForRanking(jobs, userProfile, apiKey)
            if (rankings.isEmpty()) {
                Log.w(TAG, "AI ranking returned empty, using original order")
                return@withContext jobs
            }

            // Sort jobs by AI score (descending)
            val rankedMap = rankings.associateBy { it.jobId }
            val sortedJobs = jobs.sortedByDescending { job ->
                rankedMap[job.id]?.score ?: 50 // Default to 50 if not ranked
            }

            Log.d(TAG, "Successfully ranked ${sortedJobs.size} jobs")
            return@withContext sortedJobs

        } catch (e: Exception) {
            Log.e(TAG, "AI ranking failed, using original order: ${e.message}", e)
            return@withContext jobs
        }
    }

    /**
     * Calls OpenRouter API to get job rankings
     */
    private fun callOpenRouterForRanking(
        jobs: List<JobCard>,
        userProfile: UserProfile,
        apiKey: String
    ): List<JobRankScore> {
        val prompt = buildRankingPrompt(jobs, userProfile)
        
        val requestBody = JSONObject().apply {
            put("model", MODEL)
            put("messages", JSONArray().apply {
                // System prompt - crucial for getting clean JSON output
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", SYSTEM_PROMPT)
                })
                // User prompt with jobs and profile
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("temperature", 0.2) // Low temperature for consistent output
            put("max_tokens", 2048)
            put("response_format", JSONObject().apply {
                put("type", "json_object")
            })
        }

        val request = Request.Builder()
            .url(OPENROUTER_URL)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("HTTP-Referer", "https://swipeapply.app")
            .addHeader("X-Title", "SwipeApply Job Ranker")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string()

        if (!response.isSuccessful) {
            Log.e(TAG, "OpenRouter API error: ${response.code} - $responseBody")
            return emptyList()
        }

        return parseRankingResponse(responseBody)
    }

    /**
     * System prompt that instructs the LLM to return only valid JSON
     */
    private val SYSTEM_PROMPT = """
You are a job matching AI assistant. Your ONLY task is to analyze job postings and rank them based on how well they match a candidate's profile.

CRITICAL INSTRUCTIONS:
1. You MUST respond with ONLY a valid JSON object, nothing else.
2. Do NOT include any explanation, thinking, or text outside the JSON.
3. Do NOT wrap the response in markdown code blocks.
4. The JSON must have a single key "rankings" containing an array.
5. Each item in the array must have: "id" (string), "score" (integer 0-100).

SCORING GUIDELINES:
- 90-100: Perfect match - job requirements align closely with candidate's tech stack and experience
- 70-89: Strong match - most key skills match, good fit overall  
- 50-69: Moderate match - some skills overlap, candidate could do the job
- 30-49: Weak match - few skills align, would require significant learning
- 0-29: Poor match - almost no skill overlap, not recommended

OUTPUT FORMAT (exactly this structure):
{"rankings":[{"id":"job_id_1","score":85},{"id":"job_id_2","score":72}]}
""".trimIndent()

    /**
     * Builds the user prompt containing jobs and profile data
     */
    private fun buildRankingPrompt(jobs: List<JobCard>, userProfile: UserProfile): String {
        // Build minified job list to save tokens
        val jobsJson = JSONArray()
        jobs.forEach { job ->
            jobsJson.put(JSONObject().apply {
                put("id", job.id)
                put("title", job.title)
                put("company", job.company.name)
                put("tech", job.techStack.take(10).joinToString(", "))
                put("desc", job.roleDescription.take(300)) // Truncate long descriptions
            })
        }

        // Build minified profile
        val profileJson = JSONObject().apply {
            put("skills", userProfile.skills.take(15).joinToString(", "))
            put("tech_stack", userProfile.techStack.take(15).joinToString(", "))
            put("bio", userProfile.bio.take(200))
            if (userProfile.experience.isNotEmpty()) {
                val expSummary = userProfile.experience.take(3).joinToString("; ") { 
                    "${it.role} at ${it.company}"
                }
                put("experience", expSummary)
            }
        }

        return """
CANDIDATE PROFILE:
$profileJson

JOBS TO RANK (rank ALL of them):
$jobsJson

Analyze each job and return a JSON with rankings for ALL jobs listed above.
""".trimIndent()
    }

    /**
     * Parses the AI response into JobRankScore objects
     */
    private fun parseRankingResponse(responseBody: String?): List<JobRankScore> {
        if (responseBody.isNullOrBlank()) return emptyList()

        try {
            val jsonResponse = JSONObject(responseBody)
            val choices = jsonResponse.optJSONArray("choices")
            if (choices == null || choices.length() == 0) {
                Log.e(TAG, "No choices in response")
                return emptyList()
            }

            val message = choices.getJSONObject(0).optJSONObject("message")
            val content = message?.optString("content") ?: return emptyList()

            // Clean the content (remove any markdown, thinking tags, etc.)
            val cleanedContent = cleanJsonResponse(content)
            
            // Parse the rankings JSON
            val rankingsJson = JSONObject(cleanedContent)
            val rankingsArray = rankingsJson.optJSONArray("rankings") ?: return emptyList()

            val rankings = mutableListOf<JobRankScore>()
            for (i in 0 until rankingsArray.length()) {
                val item = rankingsArray.getJSONObject(i)
                rankings.add(
                    JobRankScore(
                        jobId = item.getString("id"),
                        score = item.optInt("score", 50)
                    )
                )
            }

            Log.d(TAG, "Parsed ${rankings.size} job rankings")
            return rankings

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing ranking response: ${e.message}", e)
            return emptyList()
        }
    }

    /**
     * Cleans AI response by removing markdown, thinking tags, etc.
     */
    private fun cleanJsonResponse(response: String): String {
        var cleaned = response
            .replace(Regex("```json\\s*"), "")
            .replace(Regex("```\\s*"), "")
            .replace(Regex("<think>.*?</think>", RegexOption.DOT_MATCHES_ALL), "")
            .trim()

        // Find JSON boundaries
        val jsonStart = cleaned.indexOf('{')
        val jsonEnd = cleaned.lastIndexOf('}')

        if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
            cleaned = cleaned.substring(jsonStart, jsonEnd + 1)
        }

        return cleaned
    }

    /**
     * Quick relevance check without full AI ranking.
     * Useful for filtering obviously irrelevant jobs.
     */
    fun quickMatchScore(job: JobCard, userProfile: UserProfile): Int {
        if (userProfile.techStack.isEmpty() && userProfile.skills.isEmpty()) {
            return 50 // Neutral score if no profile
        }

        val userTechSet = (userProfile.techStack + userProfile.skills)
            .map { it.lowercase() }
            .toSet()

        val jobTechSet = job.techStack
            .map { it.lowercase() }
            .toSet()

        // Calculate overlap
        val overlap = userTechSet.intersect(jobTechSet)
        val overlapRatio = if (jobTechSet.isNotEmpty()) {
            overlap.size.toFloat() / jobTechSet.size
        } else {
            0f
        }

        // Score based on overlap (0-100)
        return (overlapRatio * 100).toInt().coerceIn(0, 100)
    }
}
