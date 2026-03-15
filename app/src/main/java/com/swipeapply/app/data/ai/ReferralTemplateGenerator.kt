package com.swipeapply.app.data.ai

import android.util.Log
import com.swipeapply.app.BuildConfig
import com.swipeapply.app.data.model.Employee
import com.swipeapply.app.data.model.FormalityLevel
import com.swipeapply.app.data.model.ReferralEmail
import com.swipeapply.app.data.model.ReferralEmailConfig
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
 * AI-powered cold referral email generator.
 * 
 * Generates personalized referral request emails based on:
 * - User's profile (skills, experience, background)
 * - Target employee's details (name, role, company)
 * - Optional job context
 * 
 * Uses OpenRouter API with a fast, free model for real-time generation.
 */
object ReferralTemplateGenerator {

    private const val TAG = "ReferralTemplateGen"
    private const val OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions"
    private const val MODEL = "google/gemma-3-4b-it:free"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Generate a personalized cold referral email.
     *
     * @param employee Target employee at the company
     * @param companyName The company where the employee works
     * @param userProfile User's profile for personalization
     * @param jobTitle Optional specific job they're applying for
     * @param config Generation configuration options
     * @return Generated ReferralEmail or null if generation fails
     */
    suspend fun generate(
        employee: Employee,
        companyName: String,
        userProfile: UserProfile?,
        jobTitle: String? = null,
        config: ReferralEmailConfig = ReferralEmailConfig()
    ): ReferralEmail? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.OPENROUTER_API_KEY
        if (apiKey.isBlank()) {
            Log.e(TAG, "OpenRouter API key not configured — using fallback template")
            return@withContext buildFallbackEmail(employee, companyName, userProfile, jobTitle)
        }

        val prompt = buildPrompt(employee, companyName, userProfile, jobTitle, config)

        val requestBody = JSONObject().apply {
            put("model", MODEL)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("temperature", 0.7)
            put("max_tokens", 600)
        }

        val request = Request.Builder()
            .url(OPENROUTER_URL)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("HTTP-Referer", "https://swipeapply.app")
            .addHeader("X-Title", "SwipeApply Referral Generator")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return@withContext try {
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string()

            if (!response.isSuccessful) {
                Log.e(TAG, "OpenRouter error ${response.code}: $body")
                return@withContext buildFallbackEmail(employee, companyName, userProfile, jobTitle)
            }

            val message = JSONObject(body ?: "")
                .optJSONArray("choices")
                ?.getJSONObject(0)
                ?.optJSONObject("message")
            
            val content = message?.optString("content")?.takeIf { it.isNotBlank() && it != "null" }
                ?: message?.optString("reasoning")?.takeIf { it.isNotBlank() && it != "null" }
                ?: return@withContext buildFallbackEmail(employee, companyName, userProfile, jobTitle)

            parseResponse(content, employee, companyName, userProfile, jobTitle)
        } catch (e: Exception) {
            Log.e(TAG, "Generation failed: ${e.message}", e)
            buildFallbackEmail(employee, companyName, userProfile, jobTitle)
        }
    }

    // ─── Prompt Builder ───────────────────────────────────────────────────────

    private fun buildPrompt(
        employee: Employee,
        companyName: String,
        profile: UserProfile?,
        jobTitle: String?,
        config: ReferralEmailConfig
    ): String {
        val hasProfile = profile != null && 
            (profile.fullName.isNotBlank() || profile.skills.isNotEmpty() || profile.experience.isNotEmpty())

        val firstName = employee.fullName.split(" ").firstOrNull() ?: "there"
        val employeeRole = employee.jobTitle ?: "employee"

        val candidateSection = if (hasProfile && profile != null) {
            val name = profile.fullName.ifBlank { "the applicant" }
            val skills = (profile.skills + profile.techStack)
                .distinct().take(6).joinToString(", ").ifBlank { "not specified" }
            val latestRole = profile.experience.firstOrNull()
                ?.let { "${it.role} at ${it.company}" } ?: "not specified"
            val years = profile.experience.size.let { if (it > 0) "${it}+ years in industry" else "" }
            
            """
SENDER (CANDIDATE):
- Name: $name
- Current/Recent Role: $latestRole
- Key Skills: $skills
${if (years.isNotBlank()) "- Experience: $years" else ""}
            """.trimIndent()
        } else {
            "SENDER: Anonymous candidate (write in first person, no specific claims)"
        }

        val formalityGuide = when (config.formalityLevel) {
            FormalityLevel.CASUAL -> "friendly and conversational, like texting a colleague"
            FormalityLevel.PROFESSIONAL -> "professional but warm, like emailing a potential mentor"
            FormalityLevel.FORMAL -> "formal and respectful, like writing to a senior executive"
        }

        return """
You are an expert at writing cold outreach emails that actually get responses. Write a referral request email.

STRICT RULES:
1. Total email body: 80-120 words MAXIMUM. Every word must earn its place.
2. Subject line: Under 8 words, specific, creates curiosity without being clickbait.
3. Opening: One sentence acknowledging them specifically (their role/company), then immediately state your intent.
4. Middle: ONE concrete connection point (skill match, shared background, or genuine interest in their work).
5. Ask: Clear, low-commitment request — "quick chat", "insights", or "if you're open to referring".
6. Sign off with just "Best," and then the sender's first name on the next line.
7. Tone: $formalityGuide
8. DO NOT use: "I hope this finds you well", "reaching out", "passionate", "excited", "opportunity", "leverage", "synergy", generic flattery.
9. DO NOT mention you found their email through a tool/database.
10. Be human. Be brief. Be specific.

$candidateSection

RECIPIENT:
- Name: ${employee.fullName}
- Role: $employeeRole
- Company: $companyName
${if (employee.department != null) "- Department: ${employee.department}" else ""}
${if (employee.city != null) "- Location: ${employee.getFormattedLocation()}" else ""}

${if (jobTitle != null) "TARGET ROLE: $jobTitle at $companyName" else "CONTEXT: Looking for opportunities at $companyName"}

OUTPUT FORMAT (strictly follow — no markdown, no extra text, no JSON):
SUBJECT: <subject line>
BODY:
<email body starting with greeting>

Remember: You are writing TO $firstName, not about them. Use "you/your" naturally.
        """.trimIndent()
    }

    // ─── Response Parser ──────────────────────────────────────────────────────

    private fun parseResponse(
        raw: String,
        employee: Employee,
        companyName: String,
        profile: UserProfile?,
        jobTitle: String?
    ): ReferralEmail {
        // Strip any <think> or reasoning blocks
        val cleaned = raw
            .replace(Regex("<think>[\\s\\S]*?</think>"), "")
            .replace(Regex("\\[THINKING\\][\\s\\S]*?\\[/THINKING\\]"), "")
            .trim()

        val subjectRegex = Regex("SUBJECT:\\s*(.+)", RegexOption.IGNORE_CASE)
        val bodyRegex = Regex("BODY:\\s*([\\s\\S]+)", RegexOption.IGNORE_CASE)

        val subject = subjectRegex.find(cleaned)?.groupValues?.get(1)?.trim()
        val body = bodyRegex.find(cleaned)?.groupValues?.get(1)?.trim()

        val senderFirstName = profile?.fullName?.split(" ")?.firstOrNull() ?: ""

        return ReferralEmail(
            fromName = profile?.fullName ?: "",
            fromEmail = profile?.email ?: "",
            toName = employee.fullName,
            toEmail = employee.email,
            subject = subject ?: buildFallbackSubject(companyName, jobTitle),
            body = body ?: buildFallbackBody(employee, companyName, profile, jobTitle),
            companyName = companyName,
            jobTitle = jobTitle
        )
    }

    // ─── Fallback Templates ───────────────────────────────────────────────────

    private fun buildFallbackEmail(
        employee: Employee,
        companyName: String,
        profile: UserProfile?,
        jobTitle: String?
    ): ReferralEmail {
        return ReferralEmail(
            fromName = profile?.fullName ?: "",
            fromEmail = profile?.email ?: "",
            toName = employee.fullName,
            toEmail = employee.email,
            subject = buildFallbackSubject(companyName, jobTitle),
            body = buildFallbackBody(employee, companyName, profile, jobTitle),
            companyName = companyName,
            jobTitle = jobTitle
        )
    }

    private fun buildFallbackSubject(companyName: String, jobTitle: String?): String {
        return if (jobTitle != null) {
            "Quick question about $jobTitle"
        } else {
            "Quick question about $companyName"
        }
    }

    private fun buildFallbackBody(
        employee: Employee,
        companyName: String,
        profile: UserProfile?,
        jobTitle: String?
    ): String {
        val firstName = employee.fullName.split(" ").firstOrNull() ?: "there"
        val senderName = profile?.fullName?.split(" ")?.firstOrNull() ?: ""
        val role = jobTitle ?: "roles"
        
        val skillMention = if (profile != null && profile.skills.isNotEmpty()) {
            val topSkills = profile.skills.take(2).joinToString(" and ")
            "My background is in $topSkills, and"
        } else {
            "I've been working in the industry, and"
        }

        return """
Hi $firstName,

I noticed your work at $companyName and wanted to reach out directly. $skillMention I'm very interested in the $role you have open.

Would you be open to a brief chat about your experience there, or if you're comfortable, considering a referral?

Totally understand if you're busy — either way, thanks for your time.

Best,
$senderName
        """.trimIndent()
    }
}
