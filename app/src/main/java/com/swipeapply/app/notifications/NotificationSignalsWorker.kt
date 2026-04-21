package com.swipeapply.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.swipeapply.app.SupabaseClient
import com.swipeapply.app.data.config.ApiConfig
import com.swipeapply.app.data.model.ApplicationStatus
import com.swipeapply.app.data.model.UserProfile
import com.swipeapply.app.data.repository.JobRepository
import com.swipeapply.app.data.service.JobRankingService
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class NotificationSignalsWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    override suspend fun doWork(): Result {
        AppNotificationService.ensureChannels(applicationContext)
        if (!AppNotificationService.canNotify(applicationContext)) return Result.success()

        runCatching {
            processSavedJobFollowUps()
            processHighMatchJobs()
        }.onFailure {
            return Result.retry()
        }

        return Result.success()
    }

    private suspend fun processSavedJobFollowUps() {
        val repository = JobRepository.getInstance(
            context = applicationContext,
            apiKey = ApiConfig.FINDWORK_API_KEY
        )

        val userId = SupabaseClient.client.auth.currentUserOrNull()?.id
        repository.setCurrentUser(userId)

        val likedJobs = repository.getLikedJobs(limit = 200)
        val now = System.currentTimeMillis()

        val target = likedJobs.firstOrNull { item ->
            item.applicationStatus == ApplicationStatus.SAVED &&
                now - item.timestamp >= SAVED_FOLLOW_UP_DELAY_MS &&
                shouldNotify("saved_follow_up_${item.card.id}", REMINDER_DEDUP_MS)
        } ?: return

        AppNotificationService.post(
            context = applicationContext,
            channelId = AppNotificationService.CHANNEL_FOLLOW_UP,
            notificationId = ("saved_follow_up_${target.card.id}").hashCode(),
            title = "Follow up on ${target.card.title}",
            message = "You saved ${target.card.company.name}. Send your intro while it is still fresh."
        )

        markNotified("saved_follow_up_${target.card.id}")
    }

    private suspend fun processHighMatchJobs() {
        val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return
        val profile = fetchProfile(userId) ?: return

        val repository = JobRepository.getInstance(
            context = applicationContext,
            apiKey = ApiConfig.FINDWORK_API_KEY
        )
        repository.setCurrentUser(userId)

        val searchQuery = buildSearchQuery(profile)
        val jobsResult = repository.refreshJobs(
            search = searchQuery,
            location = ApiConfig.DEFAULT_LOCATION,
            remote = ApiConfig.DEFAULT_REMOTE_ONLY
        )

        val jobs = jobsResult.getOrNull() ?: return

        val topCandidate = jobs
            .map { job -> job to JobRankingService.quickMatchScore(job, profile) }
            .filter { (_, score) -> score >= HIGH_MATCH_THRESHOLD }
            .sortedByDescending { it.second }
            .firstOrNull { (job, _) -> shouldNotify("high_match_${job.id}", HIGH_MATCH_DEDUP_MS) }
            ?: return

        val (job, score) = topCandidate

        AppNotificationService.post(
            context = applicationContext,
            channelId = AppNotificationService.CHANNEL_MATCHES,
            notificationId = ("high_match_${job.id}").hashCode(),
            title = "${score}% match: ${job.title}",
            message = "${job.company.name} looks like a strong fit. Open SwipeApply to review."
        )

        markNotified("high_match_${job.id}")
    }

    private suspend fun fetchProfile(userId: String): UserProfile? {
        val response = SupabaseClient.client
            .from("profiles")
            .select { filter { eq("id", userId) } }

        val data = response.data
        if (data == "[]" || data.isNullOrEmpty()) return null

        val profiles = jsonParser.decodeFromString<List<ProfileResponse>>(data)
        val profile = profiles.firstOrNull() ?: return null

        return UserProfile(
            fullName = profile.full_name ?: "",
            email = profile.email ?: "",
            phone = profile.phone ?: "",
            bio = profile.bio ?: "",
            skills = profile.skills ?: emptyList(),
            techStack = profile.tech_stack ?: emptyList(),
            education = profile.education ?: emptyList(),
            experience = profile.experience ?: emptyList(),
            projects = profile.projects ?: emptyList()
        )
    }

    private fun buildSearchQuery(profile: UserProfile): String? {
        val keywords = (profile.skills + profile.techStack)
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(12)

        return if (keywords.isEmpty()) ApiConfig.DEFAULT_SEARCH_QUERY else keywords.joinToString(" ")
    }

    private fun shouldNotify(key: String, dedupWindowMs: Long): Boolean {
        val lastSentAt = prefs.getLong(key, 0L)
        return System.currentTimeMillis() - lastSentAt > dedupWindowMs
    }

    private fun markNotified(key: String) {
        prefs.edit().putLong(key, System.currentTimeMillis()).apply()
    }

    @Serializable
    private data class ProfileResponse(
        val id: String,
        val full_name: String? = null,
        val email: String? = null,
        val phone: String? = null,
        val bio: String? = null,
        val skills: List<String>? = null,
        val tech_stack: List<String>? = null,
        val education: List<com.swipeapply.app.data.model.EducationItem>? = null,
        val experience: List<com.swipeapply.app.data.model.ExperienceItem>? = null,
        val projects: List<com.swipeapply.app.data.model.ProjectItem>? = null
    )

    companion object {
        private const val PREFS_NAME = "notification_signals"
        private const val HIGH_MATCH_THRESHOLD = 80
        private const val SAVED_FOLLOW_UP_DELAY_MS = 48L * 60 * 60 * 1000
        private const val REMINDER_DEDUP_MS = 24L * 60 * 60 * 1000
        private const val HIGH_MATCH_DEDUP_MS = 7L * 24 * 60 * 60 * 1000
    }
}
