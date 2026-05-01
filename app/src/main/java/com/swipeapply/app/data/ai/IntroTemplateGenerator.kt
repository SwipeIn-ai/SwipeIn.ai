package com.swipeapply.app.data.ai

import android.util.Log
import com.swipeapply.app.data.model.JobCard
import com.swipeapply.app.data.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * AI-powered cold outreach email generator.
 *
 * Uses Groq (Llama 3.1 8B Instant) via GroqApiClient to produce a personalized,
 * crisp intro email from the user's qualifications and the job's requirements.
 * Includes retry, rate-limit, and timeout handling via the shared client.
 *
 * Output: subject line + body, ready to edit and copy.
 */
object IntroTemplateGenerator {

    private const val TAG = "IntroTemplateGenerator"

    data class GeneratedTemplate(
        val subject: String,
        val body: String
    )

    suspend fun generate(jobCard: JobCard, profile: UserProfile): GeneratedTemplate? =
        withContext(Dispatchers.IO) {
            val prompt = buildPrompt(jobCard, profile)
            val systemInstruction = "You are an expert career coach who writes ultra-concise, " +
                "human-sounding cold outreach emails. " +
                "You never use buzzwords, filler phrases, or generic claims. " +
                "You write like a confident professional, not a template engine.\n\n"

            val result = GroqApiClient.complete(
                prompt = systemInstruction + prompt,
                temperature = 0.2,
                maxTokens = 512
            )

            when (result) {
                is GroqApiClient.GroqResult.Success -> {
                    parseResponse(result.content, jobCard)
                }
                is GroqApiClient.GroqResult.Error -> {
                    Log.e(TAG, "Generation failed: ${result.message}")
                    null
                }
            }
        }

    private fun buildPrompt(job: JobCard, profile: UserProfile): String {
        val name = profile.fullName.ifBlank { "the applicant" }
        val skills = (profile.skills + profile.techStack)
            .distinct()
            .take(8)
            .joinToString(", ")
            .ifBlank { "not specified" }
        val latestRole = profile.experience.firstOrNull()
            ?.let { "${it.role} at ${it.company} (${it.duration})" } ?: "not specified"
        val projectHighlight = profile.projects.firstOrNull()
            ?.let { project ->
                buildString {
                    append(project.name.ifBlank { "Project not named" })
                    if (project.techUsed.isNotBlank()) append(" (${project.techUsed})")
                    if (project.description.isNotBlank()) append(": ${project.description}")
                }.take(180)
            } ?: "not specified"
        val bio = profile.bio.take(200).ifBlank { "not specified" }

        val candidateSection = """
CANDIDATE:
- Name: $name
- Most recent role: $latestRole
- Top skills: $skills
- Project highlight: $projectHighlight
- About: $bio
        """.trimIndent()

        val allSkills = (profile.skills + profile.techStack).map { it.lowercase() }
        val overlap = job.techStack.filter { tech ->
            allSkills.any { skill ->
                skill.contains(tech.lowercase()) || tech.lowercase().contains(skill)
            }
        }.take(3).joinToString(", ")

        return """
You are writing a cold outreach email for a job application. Follow ALL rules below:

RULES:
1. Total email body: 80-120 words MAX. Be ruthlessly concise.
2. No subject line fluff. Subject should be under 10 words, direct, and specific.
3. Opening line: one crisp sentence stating who you are and why THIS company/role interests you - be specific, not generic.
4. Middle: one sentence connecting 1-2 concrete skills/experiences to the role's needs.
5. CTA: one short ask - "15-minute chat?" or "Would love to connect."
6. Sign-off: just "Best," then a blank line for name.
7. Do NOT use: "I hope this finds you well", "I'm passionate about", "leverage", "synergy", "exciting opportunity", "would be a great fit".
8. Write in first person. Sound human. No em dashes.
9. Personalization is mandatory: include at least two candidate-specific facts from the profile section.

$candidateSection

JOB:
- Title: ${job.title}
- Company: ${job.company.name}
- Industry: ${job.company.industry}
- Location: ${job.location}
- Role summary: ${job.roleDescription.take(300)}
- Required tech: ${job.techStack.joinToString(", ").ifBlank { "not listed" }}
${if (overlap.isNotBlank()) "- Overlapping skills to highlight: $overlap" else ""}

OUTPUT FORMAT (strictly follow this - no extra text, no markdown, no JSON):
SUBJECT: <subject line here>
BODY:
<email body here>
        """.trimIndent()
    }

    private fun parseResponse(raw: String, job: JobCard): GeneratedTemplate? {
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
            ?: deriveSubjectFromBody(body, job.title)

        return GeneratedTemplate(subject = subject, body = body)
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

        // Fallback: treat plain model output as body text.
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
            "$fallbackHint Intro"
        }
    }
}
