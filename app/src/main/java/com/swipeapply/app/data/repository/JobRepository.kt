package com.swipeapply.app.data.repository

import android.content.Context
import android.util.Log
import com.swipeapply.app.data.api.FindWorkApiService
import com.swipeapply.app.data.local.SwipeApplyDatabase
import com.swipeapply.app.data.local.entity.SwipedJobEntity
import com.swipeapply.app.data.manager.SavedJobStatusManager
import com.swipeapply.app.data.mapper.toEntity
import com.swipeapply.app.data.mapper.toJobCard
import com.swipeapply.app.data.model.ApplicationStatus
import com.swipeapply.app.data.model.JobCard
import com.swipeapply.app.data.model.EducationItem
import com.swipeapply.app.data.model.ExperienceItem
import com.swipeapply.app.data.model.ProjectItem
import com.swipeapply.app.data.model.SwipeDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import com.swipeapply.app.data.model.UserProfile
import io.github.jan.supabase.postgrest.from

/**
 * Data class for Supabase profile table - must match column names exactly
 */
@Serializable
data class SupabaseProfileData(
    val id: String,
    val full_name: String,
    val email: String,
    val phone: String,
    val bio: String,
    val skills: List<String>,
    val tech_stack: List<String>,
    val education: List<EducationItem>,
    val experience: List<ExperienceItem>,
    val projects: List<ProjectItem>
)

data class SwipedJobHistoryItem(
    val card: JobCard,
    val direction: SwipeDirection,
    val timestamp: Long,
    val applicationStatus: ApplicationStatus? = null
)

class JobRepository(private val context: Context, private val apiKey: String) {

    private val jobDao = SwipeApplyDatabase.getDatabase(context).jobDao()
    private val savedJobStatusManager = SavedJobStatusManager.getInstance(context)
    
    // Current user ID for user-specific swipe tracking
    // Defaults to "local_user" for dev mode (skip login)
    private var currentUserId: String = "local_user"
    
    /**
     * Set the current user ID. Call this after authentication.
     * For dev mode / skip login, this defaults to "local_user".
     */
    fun setCurrentUser(userId: String?) {
        currentUserId = userId ?: "local_user"
        Log.d(TAG, "Current user set to: $currentUserId")
    }
    
    /**
     * Get current user ID (for external access if needed)
     */
    fun getCurrentUserId(): String = currentUserId

    private val apiService: FindWorkApiService by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(FindWorkApiService.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FindWorkApiService::class.java)
    }

    // --- QUEUE SYSTEM ---
    private val jobQueue = ArrayDeque<JobCard>()
    private val queuedJobIds = HashSet<String>()
    
    private var currentPage = 1
    private var hasMorePages = true
    private var totalCount = 0
    private var isFetching = false
    private var isFallbackMode = false

    /**
     * Get INITIAL batch of 15 jobs.
     * Logic: Try API -> If Empty/Fail -> Load from DB
     */
    fun getJobCards(
        search: String? = null,
        location: String? = null,
        remote: Boolean? = null,
        forceRefresh: Boolean = false
    ): Flow<Result<List<JobCard>>> = flow {
        if (forceRefresh) clearCache()

        // 1. Try filling queue from API
        if (jobQueue.isEmpty()) {
            // Reset pagination state so we always get a fresh fetch attempt
            currentPage = 1
            hasMorePages = true
            isFallbackMode = false
            fetchAndQueueJobs(search, location, remote)
        }

        // 2. DB FALLBACK: If API didn't give us anything (network error or empty), check local DB
        if (jobQueue.isEmpty()) {
            Log.w(TAG, "API empty/failed. Attempting Local Database Fallback...")
            loadFromDatabaseFallback()
        }

        // 3. REMOTE FALLBACK: If even DB is empty, try broader API search
        if (jobQueue.isEmpty() && !isFallbackMode) {
            Log.w(TAG, "Local DB empty. Switching to Remote Fallback.")
            isFallbackMode = true
            currentPage = 1
            hasMorePages = true
            fetchAndQueueJobs(null, null, true)
        }

        // 4. Prepare Batch for UI (Max 15)
        val initialBatch = mutableListOf<JobCard>()
        repeat(15) {
            jobQueue.removeFirstOrNull()?.let { 
                initialBatch.add(it)
                queuedJobIds.remove(it.id)
            }
        }

        if (initialBatch.isNotEmpty()) {
            emit(Result.success(initialBatch))
        } else {
            emit(Result.failure(Exception("No jobs found (Online or Offline). Try clearing history.")))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Pops ONE job from queue.
     * Triggers background fetch if low.
     */
    suspend fun getNextJobFromQueue(
        search: String?, location: String?, remote: Boolean?
    ): JobCard? = withContext(Dispatchers.IO) {
        
        // Background fetch trigger
        if (jobQueue.size < 5 && hasMorePages && !isFetching) {
            try {
                if (isFallbackMode) fetchAndQueueJobs(null, null, true)
                else fetchAndQueueJobs(search, location, remote)
            } catch (e: Exception) {
                Log.e(TAG, "Background fetch failed, trying DB...", e)
                // If background fetch fails, try squeezing more from DB
                loadFromDatabaseFallback()
            }
        }

        val job = jobQueue.removeFirstOrNull()
        if (job != null) queuedJobIds.remove(job.id)
        return@withContext job
    }

    /**
     * Fetch from API -> Save to DB -> Add to Queue
     */
    private suspend fun fetchAndQueueJobs(search: String?, location: String?, remote: Boolean?) {
        if (isFetching) return
        isFetching = true
        
        val authToken = FindWorkApiService.formatAuthToken(apiKey)
        // Use user-specific swiped IDs
        val swipedIds = jobDao.getSwipedJobIdsByUser(currentUserId).toSet()

        try {
            while (hasMorePages && jobQueue.size < 20) {
                Log.d(TAG, "Fetching Page $currentPage...")
                val response = apiService.getJobs(
                    authToken = authToken,
                    search = search,
                    location = location,
                    remote = remote,
                    sortBy = "date_posted",
                    page = currentPage
                )

                hasMorePages = response.next != null
                totalCount = response.count

                // 1. Map to Entities (Clean HTML happens here via Mapper)
                // Use mapNotNull so one bad job doesn't kill the entire page
                val jobEntities = response.results.mapNotNull { 
                    try { it.toEntity() } catch (e: Exception) {
                        Log.w(TAG, "Skipping malformed job: ${e.message}")
                        null
                    }
                }
                
                // 2. SAVE TO DB (Persistence)
                if (jobEntities.isNotEmpty()) {
                    jobDao.insertJobs(jobEntities)
                    Log.d(TAG, "Saved ${jobEntities.size} jobs to local database.")
                }

                // 3. Convert to Cards for Queue
                val rawJobs = jobEntities.mapNotNull { 
                    try { it.toJobCard() } catch (e: Exception) { null } 
                }

                // 4. Filter Duplicates (Swiped or already queued)
                val validJobs = rawJobs.filter { 
                    !swipedIds.contains(it.id) && !queuedJobIds.contains(it.id) 
                }

                validJobs.forEach { 
                    jobQueue.add(it)
                    queuedJobIds.add(it.id)
                }

                currentPage++
                
                if (validJobs.isEmpty() && !hasMorePages) break
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fetch error: ${e.message}")
        } finally {
            isFetching = false
        }
    }

    /**
     * Load unswiped jobs from Local DB into the Queue
     */
    private suspend fun loadFromDatabaseFallback() {
        // Use user-specific query
        val dbJobs = jobDao.getAvailableJobsForUser(currentUserId)
        
        val newCards = dbJobs.mapNotNull { 
            try { it.toJobCard() } catch(e: Exception) { null } 
        }.filter { 
            !queuedJobIds.contains(it.id) // Don't add if already in queue
        }

        if (newCards.isNotEmpty()) {
            Log.d(TAG, "Loaded ${newCards.size} jobs from Offline Database for user: $currentUserId")
            // Add to front of queue to show immediately
            newCards.forEach { 
                if (!queuedJobIds.contains(it.id)) {
                    jobQueue.add(it)
                    queuedJobIds.add(it.id)
                }
            }
        } else {
            Log.w(TAG, "Database is empty or all jobs swiped by user: $currentUserId")
        }
    }

    suspend fun recordSwipe(jobId: String, direction: String) = withContext(Dispatchers.IO) {
        try { 
            // Include userId in the swipe record
            jobDao.insertSwipedJob(SwipedJobEntity(jobId = jobId, userId = currentUserId, direction = direction)) 
        } 
        catch (e: Exception) { Log.e(TAG, "DB Error: ${e.message}") }
    }

    suspend fun removeSwipe(jobId: String) = withContext(Dispatchers.IO) {
        try {
            jobDao.deleteSwipedJobByUser(jobId, currentUserId)
            savedJobStatusManager.clearStatus(currentUserId, jobId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove swipe for job $jobId: ${e.message}", e)
        }
    }

    suspend fun getSwipeHistory(limit: Int = 50): List<SwipedJobHistoryItem> = withContext(Dispatchers.IO) {
        jobDao.getRecentSwipedJobsByUser(currentUserId, limit).mapNotNull { swipedJob ->
            val job = jobDao.getJobById(swipedJob.jobId) ?: return@mapNotNull null
            val direction = runCatching { SwipeDirection.valueOf(swipedJob.direction) }
                .getOrDefault(SwipeDirection.NONE)
            if (direction == SwipeDirection.NONE) return@mapNotNull null
            val applicationStatus = if (direction == SwipeDirection.RIGHT) {
                savedJobStatusManager.getStatus(currentUserId, swipedJob.jobId) ?: ApplicationStatus.SAVED
            } else {
                null
            }

            SwipedJobHistoryItem(
                card = job.toJobCard(),
                direction = direction,
                timestamp = swipedJob.timestamp,
                applicationStatus = applicationStatus
            )
        }
    }

    suspend fun getLikedJobs(limit: Int = 200): List<SwipedJobHistoryItem> = withContext(Dispatchers.IO) {
        jobDao.getRecentSwipedJobsByUserAndDirection(currentUserId, SwipeDirection.RIGHT.name, limit).mapNotNull { swipedJob ->
            val job = jobDao.getJobById(swipedJob.jobId) ?: return@mapNotNull null
            SwipedJobHistoryItem(
                card = job.toJobCard(),
                direction = SwipeDirection.RIGHT,
                timestamp = swipedJob.timestamp,
                applicationStatus = savedJobStatusManager.getStatus(currentUserId, swipedJob.jobId) ?: ApplicationStatus.SAVED
            )
        }
    }

    suspend fun updateApplicationStatus(jobId: String, status: ApplicationStatus) = withContext(Dispatchers.IO) {
        savedJobStatusManager.setStatus(currentUserId, jobId, status)
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        jobQueue.clear()
        queuedJobIds.clear()
        currentPage = 1
        hasMorePages = true
        isFallbackMode = false
    }
    
    suspend fun clearSwipeHistory() = withContext(Dispatchers.IO) {
        // Clear only for current user (user-specific)
        jobDao.deleteSwipedJobsByUser(currentUserId)
        savedJobStatusManager.clearAllForUser(currentUserId)
        clearCache()
        Log.d(TAG, "Cleared swipe history for user: $currentUserId")
    }
    
    /**
     * Clear ALL local data on logout.
     * Resets queue, cache, and user ID to default.
     */
    suspend fun clearAllLocalData() = withContext(Dispatchers.IO) {
        Log.d(TAG, "Clearing all local data for user: $currentUserId")
        jobDao.deleteSwipedJobsByUser(currentUserId)
        savedJobStatusManager.clearAllForUser(currentUserId)
        clearCache()
        currentUserId = "local_user" // Reset to default
    }

    suspend fun refreshJobs(search: String?, location: String?, remote: Boolean?): Result<List<JobCard>> {
        var result: Result<List<JobCard>> = Result.failure(Exception("Unknown"))
        getJobCards(search, location, remote, true).collect { result = it }
        return result
    }

    fun hasMorePages() = hasMorePages
    fun getTotalCount() = totalCount
    suspend fun getJobCardById(id: String): JobCard? = withContext(Dispatchers.IO) {
        jobDao.getJobById(id)?.toJobCard()
    }
    
    /**
     * Check if user profile exists in database
     */
    suspend fun hasUserProfile(userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Checking if profile exists for user: $userId")
            
            val response = com.swipeapply.app.SupabaseClient.client
                .from("profiles")
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
            
            // response.data is a JSON string like "[]" or "[{...}]"
            val data = response.data
            Log.d(TAG, "Profile query response: $data")
            
            // Check if the JSON array has any items
            // "[]" means empty, "[{...}]" means has data
            val hasProfile = data != "[]" && data.isNotEmpty() && data != "null"
            Log.d(TAG, "Has profile: $hasProfile")
            
            return@withContext hasProfile
        } catch (e: Exception) {
            Log.e(TAG, "Error checking profile existence", e)
            // On error, assume no profile exists so user can create one
            return@withContext false
        }
    }
    
    suspend fun saveUserProfile(userId: String, profile: UserProfile): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Create a properly typed @Serializable object for Supabase
            val profileData = SupabaseProfileData(
                id = userId,
                full_name = profile.fullName,
                email = profile.email,
                phone = profile.phone,
                bio = profile.bio,
                skills = profile.skills,
                tech_stack = profile.techStack,
                education = profile.education,
                experience = profile.experience,
                projects = profile.projects
            )

            // Using Supabase-kt client with properly serializable data class
            com.swipeapply.app.SupabaseClient.client.from("profiles").upsert(profileData) {
                select()
            }

            Log.d(TAG, "Profile saved successfully for user: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving profile", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "JobRepository"
        @Volatile private var INSTANCE: JobRepository? = null
        fun getInstance(context: Context, apiKey: String): JobRepository {
            return INSTANCE ?: synchronized(this) {
                JobRepository(context.applicationContext, apiKey).also { INSTANCE = it }
            }
        }
    }
}
