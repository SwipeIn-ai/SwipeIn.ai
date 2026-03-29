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
    private const val MODEL = "stepfun/step-3.5-flash:free"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Generate a personalized cold referral email.
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
            Log.e(TAG, "OpenRouter API key not configured - using fallback template")
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
            put("temperature", 0.35)
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
        val senderFirstName = profile?.fullName?.split(" ")?.firstOrNull()?.takeIf { it.isNotBlank() }
            ?: "[Your first name]"
        val targetLength = config.maxBodyLength.coerceIn(90, 180)
        val minLength = (targetLength - 30).coerceAtLeast(75)
        val maxLength = (targetLength + 10).coerceAtMost(180)

        val candidateSection = if (hasProfile && profile != null) {
            val name = profile.fullName.ifBlank { "the applicant" }
            val skills = (profile.skills + profile.techStack)
                .distinct()
                .take(8)
                .joinToString(", ")
                .ifBlank { "not specified" }
            val latestRole = profile.experience.firstOrNull()
                ?.let { "${it.role} at ${it.company}" }
                ?: "not specified"
            val recentImpact = profile.experience.firstOrNull()?.description
                ?.replace(Regex("\\s+"), " ")
                ?.take(180)
                ?.takeIf { it.isNotBlank() }
                ?: "not specified"
            val projectHighlight = profile.projects.firstOrNull()
                ?.let { project ->
                    buildString {
                        append(project.name.ifBlank { "Project not named" })
                        if (project.techUsed.isNotBlank()) append(" (${project.techUsed})")
                        if (project.description.isNotBlank()) append(": ${project.description}")
                    }.take(180)
                } ?: "not specified"
            val bio = profile.bio
                .replace(Regex("\\s+"), " ")
                .take(160)
                .ifBlank { "not specified" }

            """
KNOWN FACTS ABOUT THE CANDIDATE:
- Name: $name
- Current/Recent Role: $latestRole
- Key Skills: $skills
- Recent Impact: $recentImpact
- Project Highlight: $projectHighlight
- Bio / Focus: $bio
            """.trimIndent()
        } else {
            "KNOWN FACTS ABOUT THE CANDIDATE: Limited profile data. Write in first person and keep claims conservative."
        }

        val formalityGuide = when (config.formalityLevel) {
            FormalityLevel.CASUAL -> "friendly and conversational, like messaging a colleague"
            FormalityLevel.PROFESSIONAL -> "professional but warm, like emailing a potential mentor"
            FormalityLevel.FORMAL -> "formal and respectful, like writing to a senior executive"
        }

        return """
TASK:
Write a cold referral request email to a real employee. The email should feel credible, specific, and easy to answer.

STRICT RULES:
1. Use ONLY details explicitly given below. Do not invent shared background, achievements, metrics, or personal context.
2. Email body length: $minLength-$maxLength words. Keep it tight and skimmable.
3. Subject line: 4-8 words, plain English, no hype, no clickbait.
4. Greeting must start with: "Hi $firstName,"
5. Opening must acknowledge their role or company and immediately state why the sender is writing.
6. Body must include exactly ONE concrete candidate detail that is actually supported by the profile.
7. Ask for one low-friction next step: brief advice, a short chat, or consideration for a referral.
8. Tone: $formalityGuide
9. Avoid fluff and banned phrases: "I hope this finds you well", "reaching out", "passionate", "excited", "opportunity", "leverage", "synergy", "pick your brain".
10. No generic praise. No overfamiliarity. No mention of finding their email through a tool or database.
11. End with:
Best,
$senderFirstName
12. Use short paragraphs. No bullet points. No emojis. No placeholders other than the provided sign-off name if necessary.

$candidateSection

KNOWN FACTS ABOUT THE RECIPIENT:
- Name: ${employee.fullName}
- Role: $employeeRole
- Company: $companyName
${if (employee.department != null) "- Department: ${employee.department}" else ""}
${if (employee.city != null) "- Location: ${employee.getFormattedLocation()}" else ""}
${if (employee.seniority != null) "- Seniority: ${employee.seniority}" else ""}

TARGET CONTEXT:
${if (jobTitle != null) "- Specific role of interest: $jobTitle at $companyName" else "- General goal: learn about roles at $companyName and ask for the most reasonable next step"}

WRITING OBJECTIVE:
- The email should sound like a thoughtful candidate who did enough homework to be relevant.
- It should make the recipient feel replying would be easy.
- The best version is specific, calm, and modestly confident.

OUTPUT FORMAT (follow exactly; no markdown, no explanations, no JSON):
SUBJECT: <subject line>
BODY:
<email body starting with greeting>

QUALITY CHECK BEFORE WRITING:
- If a profile fact is weak or missing, leave it out.
- Prefer one sharp detail over three vague ones.
- Make sure the ask is clear in one sentence.
        """.trimIndent()
    }

    private fun parseResponse(
        raw: String,
        employee: Employee,
        companyName: String,
        profile: UserProfile?,
        jobTitle: String?
    ): ReferralEmail {
        val cleaned = raw
            .replace(Regex("<think>[\\s\\S]*?</think>"), "")
            .replace(Regex("\\[THINKING\\][\\s\\S]*?\\[/THINKING\\]"), "")
            .replace("```", "")
            .trim()

        val subjectRegex = Regex("SUBJECT:\\s*(.+)", RegexOption.IGNORE_CASE)
        val bodyRegex = Regex("BODY:\\s*([\\s\\S]+)", RegexOption.IGNORE_CASE)

        val subject = subjectRegex.find(cleaned)?.groupValues?.get(1)?.trim()
        val body = bodyRegex.find(cleaned)?.groupValues?.get(1)?.trim()

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

Totally understand if you're busy - either way, thanks for your time.

Best,
$senderName
        """.trimIndent()
    }
}
