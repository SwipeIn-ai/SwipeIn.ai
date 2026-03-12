package com.swipeapply.app.data.ai

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
 * AI-powered cold outreach email generator.
 *
 * Uses OpenRouter (StepFun step-3.5-flash) to produce a personalised, crisp intro email
 * from the user's qualifications + the job's requirements.
 *
 * Output: subject line + body, ready to edit and copy.
 */
object IntroTemplateGenerator {

    private const val TAG = "IntroTemplateGenerator"
    private const val OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions"
    private const val MODEL = "stepfun/step-3.5-flash:free"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    data class GeneratedTemplate(
        val subject: String,
        val body: String
    )

    /**
     * Generate a personalised intro email.
     *
     * @param jobCard  The target job
     * @param profile  The user's profile (may be empty / anonymous)
     * @return GeneratedTemplate or null if the call fails
     */
    suspend fun generate(jobCard: JobCard, profile: UserProfile?): GeneratedTemplate? =
        withContext(Dispatchers.IO) {
            val apiKey = BuildConfig.OPENROUTER_API_KEY
            if (apiKey.isBlank()) {
                Log.e(TAG, "OpenRouter API key not configured — falling back to static template")
                return@withContext null
            }

            val prompt = buildPrompt(jobCard, profile)

            // StepFun free tier does not reliably honour a separate system role —
            // prepend the persona instruction directly into the user turn.
            val systemInstruction = "You are an expert career coach who writes ultra-concise, " +
                "human-sounding cold outreach emails. " +
                "You never use buzzwords, filler phrases, or generic claims. " +
                "You write like a confident professional, not a template engine.\n\n"

            val requestBody = JSONObject().apply {
                put("model", MODEL)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", systemInstruction + prompt)
                    })
                })
                put("temperature", 0.7)
                put("max_tokens", 512)
            }

            val request = Request.Builder()
                .url(OPENROUTER_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("HTTP-Referer", "https://swipeapply.app")
                .addHeader("X-Title", "SwipeApply Intro Generator")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            return@withContext try {
                val response = httpClient.newCall(request).execute()
                val body = response.body?.string()

                if (!response.isSuccessful) {
                    Log.e(TAG, "OpenRouter error ${response.code}: $body")
                    return@withContext null
                }

                val content = JSONObject(body ?: "")
                    .optJSONArray("choices")
                    ?.getJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content")
                    ?: return@withContext null

                parseResponse(content, jobCard, profile)
            } catch (e: Exception) {
                Log.e(TAG, "Generation failed: ${e.message}", e)
                null
            }
        }

    // ─── Prompt ──────────────────────────────────────────────────────────────

    private fun buildPrompt(job: JobCard, profile: UserProfile?): String {
        val hasProfile = profile != null &&
                (profile.fullName.isNotBlank() || profile.skills.isNotEmpty() || profile.experience.isNotEmpty())

        val candidateSection = if (hasProfile && profile != null) {
            val name = profile.fullName.ifBlank { "the applicant" }
            val skills = (profile.skills + profile.techStack)
                .distinct().take(8).joinToString(", ").ifBlank { "not specified" }
            val latestRole = profile.experience.firstOrNull()
                ?.let { "${it.role} at ${it.company} (${it.duration})" } ?: "not specified"
            val bio = profile.bio.take(200).ifBlank { "" }

            """
CANDIDATE:
- Name: $name
- Most recent role: $latestRole
- Top skills: $skills
${if (bio.isNotBlank()) "- About: $bio" else ""}
            """.trimIndent()
        } else {
            "CANDIDATE: No profile provided — write for a strong but anonymous candidate."
        }

        val overlap = if (profile != null) {
            val allSkills = (profile.skills + profile.techStack).map { it.lowercase() }
            job.techStack.filter { tech ->
                allSkills.any { s -> s.contains(tech.lowercase()) || tech.lowercase().contains(s) }
            }.take(3).joinToString(", ")
        } else ""

        return """
You are writing a cold outreach email for a job application. Follow ALL rules below:

RULES:
1. Total email body: 80–120 words MAX. Be ruthlessly concise.
2. No subject line fluff. Subject should be under 10 words, direct, and specific.
3. Opening line: one crisp sentence stating who you are and why THIS company/role excites you — be specific, not generic.
4. Middle: one sentence connecting 1–2 concrete skills/experiences to the role's needs.
5. CTA: one short ask — "15-minute chat?" or "Would love to connect."
6. Sign-off: just "Best," then a blank line for name.
7. Do NOT use: "I hope this finds you well", "I'm passionate about", "leverage", "synergy", "exciting opportunity", "would be a great fit".
8. Write in first person. Sound human. No em-dashes.

$candidateSection

JOB:
- Title: ${job.title}
- Company: ${job.company.name}
- Industry: ${job.company.industry}
- Location: ${job.location} (${job.locationType.label})
- Role summary: ${job.roleDescription.take(300)}
- Required tech: ${job.techStack.joinToString(", ").ifBlank { "not listed" }}
${if (overlap.isNotBlank()) "- Overlapping skills to highlight: $overlap" else ""}

OUTPUT FORMAT (strictly follow this — no extra text, no markdown, no JSON):
SUBJECT: <subject line here>
BODY:
<email body here>
        """.trimIndent()
    }

    // ─── Response parser ──────────────────────────────────────────────────────

    private fun parseResponse(raw: String, job: JobCard, profile: UserProfile?): GeneratedTemplate {
        // StepFun (and DeepSeek) reasoning models wrap chain-of-thought in <think> blocks — strip them
        val cleaned = raw
            .replace(Regex("<think>[\\s\\S]*?</think>"), "")
            .replace(Regex("\\[THINKING\\][\\s\\S]*?\\[/THINKING\\]"), "")  // StepFun alternate tag
            .trim()

        val subjectRegex = Regex("SUBJECT:\\s*(.+)", RegexOption.IGNORE_CASE)
        val bodyRegex = Regex("BODY:\\s*([\\s\\S]+)", RegexOption.IGNORE_CASE)

        val subject = subjectRegex.find(cleaned)?.groupValues?.get(1)?.trim()
        val body = bodyRegex.find(cleaned)?.groupValues?.get(1)?.trim()

        // Graceful fallback if parsing fails
        return GeneratedTemplate(
            subject = subject ?: "${job.title} — ${profile?.fullName?.split(" ")?.firstOrNull() ?: "Introduction"}",
            body = body ?: cleaned.ifBlank {
                // Last resort static fallback
                "Hi,\n\nI came across the ${job.title} role at ${job.company.name} and wanted to reach out directly.\n\nMy background in ${job.techStack.take(2).joinToString(" and ").ifBlank { "this space" }} aligns well with what you're building, and I'd love to learn more.\n\nWould you have 15 minutes for a quick chat?\n\nBest,"
            }
        )
    }
}
