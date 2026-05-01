package com.swipeapply.app.data.ai

import android.util.Log
import com.swipeapply.app.data.model.Employee
import com.swipeapply.app.data.model.FormalityLevel
import com.swipeapply.app.data.model.ReferralEmail
import com.swipeapply.app.data.model.ReferralEmailConfig
import com.swipeapply.app.data.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * AI-powered cold referral email generator.
 *
 * Generates personalized referral request emails based on:
 * - User's profile (skills, experience, background)
 * - Target employee's details (name, role, company)
 * - Optional job context
 *
 * Uses Groq API via GroqApiClient with retry, rate-limit, and timeout handling.
 */
object ReferralTemplateGenerator {

    private const val TAG = "ReferralTemplateGen"

    suspend fun generate(
        employee: Employee,
        companyName: String,
        userProfile: UserProfile,
        jobTitle: String? = null,
        config: ReferralEmailConfig = ReferralEmailConfig()
    ): ReferralEmail? = withContext(Dispatchers.IO) {
        val prompt = buildPrompt(employee, companyName, userProfile, jobTitle, config)

        val result = GroqApiClient.complete(
            prompt = prompt,
            temperature = 0.2,
            maxTokens = 600
        )

        when (result) {
            is GroqApiClient.GroqResult.Success -> {
                parseResponse(result.content, employee, companyName, userProfile, jobTitle)
            }
            is GroqApiClient.GroqResult.Error -> {
                Log.e(TAG, "Generation failed: ${result.message}")
                null
            }
        }
    }

    private fun buildPrompt(
        employee: Employee,
        companyName: String,
        profile: UserProfile,
        jobTitle: String?,
        config: ReferralEmailConfig
    ): String {
        val firstName = employee.fullName.split(" ").firstOrNull() ?: "there"
        val employeeRole = employee.jobTitle ?: "employee"
        val senderFirstName = profile.fullName.split(" ").firstOrNull()?.takeIf { it.isNotBlank() }
            ?: "[Your first name]"
        val targetLength = config.maxBodyLength.coerceIn(90, 180)
        val minLength = (targetLength - 30).coerceAtLeast(75)
        val maxLength = (targetLength + 10).coerceAtMost(180)

        val candidateSection = run {
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
        }

        val formalityGuide = when (config.formalityLevel) {
            FormalityLevel.CASUAL -> "friendly and conversational, like messaging a colleague"
            FormalityLevel.PROFESSIONAL -> "professional but warm, like emailing a potential mentor"
            FormalityLevel.FORMAL -> "formal and respectful, like writing to a senior executive"
        }

        return """
TASK:
Write a cold intro email to a real employee. The email should feel credible, specific, and easy to answer.

STRICT RULES:
1. Use ONLY details explicitly given below. Do not invent shared background, achievements, metrics, or personal context.
2. Email body length: $minLength-$maxLength words. Keep it tight and skimmable.
3. Subject line: 4-8 words, plain English, no hype, no clickbait.
4. Greeting must start with: "Hi $firstName,"
5. Opening must acknowledge their role or company and immediately state why the sender is writing.
6. Body must include exactly ONE concrete candidate detail that is actually supported by the profile.
6b. Personalization is mandatory: mention at least one skill and one experience or project detail from the profile.
7. Ask for one low-friction next step: brief advice, a short chat, or the best intro path.
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
${if (jobTitle != null) "- Specific role of interest: $jobTitle at $companyName" else "- General goal: learn about roles at $companyName and request a practical intro path"}

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
        profile: UserProfile,
        jobTitle: String?
    ): ReferralEmail? {
        val cleaned = raw
            .replace(Regex("<think>[\\s\\S]*?</think>"), "")
            .replace(Regex("\\[THINKING\\][\\s\\S]*?\\[/THINKING\\]"), "")
            .replace("```", "")
            .trim()

        val (parsedSubject, parsedBody) = extractSubjectAndBody(cleaned)
        val body = parsedBody?.trim().takeIf { !it.isNullOrBlank() }

        if (body == null) {
            Log.w(TAG, "Groq response could not be parsed into email body")
            return null
        }

        val subject = parsedSubject?.trim().takeUnless { it.isNullOrBlank() }
            ?: deriveSubjectFromBody(body, jobTitle ?: companyName)

        return ReferralEmail(
            fromName = profile.fullName,
            fromEmail = profile.email,
            toName = employee.fullName,
            toEmail = employee.email,
            subject = subject,
            body = body,
            companyName = companyName,
            jobTitle = jobTitle
        )
    }

    private fun extractSubjectAndBody(text: String): Pair<String?, String?> {
        val labeledSubject = Regex("(?im)^subject\\s*[:\\-]\\s*(.+)$")
            .find(text)
            ?.groupValues
            ?.get(1)
            ?.trim()
        val labeledBody = Regex("(?is)\\bbody\\s*[:\\-]\\s*(.+)$")
            .find(text)
            ?.groupValues
            ?.get(1)
            ?.trim()

        if (!labeledBody.isNullOrBlank()) {
            return labeledSubject to labeledBody
        }

        val jsonObjectText = extractJsonObject(text)
        if (jsonObjectText != null) {
            runCatching {
                val json = JSONObject(jsonObjectText)
                val jsonSubject = json.optString("subject")
                    .ifBlank { json.optString("SUBJECT") }
                    .ifBlank { null }
                val jsonBody = json.optString("body")
                    .ifBlank { json.optString("BODY") }
                    .ifBlank { json.optString("message") }
                    .ifBlank { null }

                if (!jsonBody.isNullOrBlank()) {
                    return jsonSubject to jsonBody
                }
            }
        }

        // Fallback: treat plain model output as the message body.
        return null to text
    }

    private fun extractJsonObject(text: String): String? {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start == -1 || end == -1 || end <= start) return null
        return text.substring(start, end + 1)
    }

    private fun deriveSubjectFromBody(body: String, fallbackHint: String): String {
        val firstMeaningfulLine = body
            .lineSequence()
            .map { it.trim() }
            .firstOrNull { line ->
                line.isNotBlank() &&
                    !line.startsWith("hi ", ignoreCase = true) &&
                    !line.startsWith("hello", ignoreCase = true)
            }
            ?: body

        val words = firstMeaningfulLine
            .replace(Regex("[^A-Za-z0-9 ]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        val dynamic = words.take(6).joinToString(" ").trim()
        return if (dynamic.isNotBlank()) {
            dynamic.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        } else {
            "Quick intro about $fallbackHint"
        }
    }
}
