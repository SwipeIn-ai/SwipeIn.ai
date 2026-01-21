package com.swipeapply.app.data.repository

import android.content.Context
import android.util.Log
import com.swipeapply.app.data.api.FindWorkApiService
import com.swipeapply.app.data.local.SwipeApplyDatabase
import com.swipeapply.app.data.local.entity.SwipedJobEntity
import com.swipeapply.app.data.mapper.toEntity
import com.swipeapply.app.data.mapper.toJobCard
import com.swipeapply.app.data.model.JobCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class JobRepository(private val context: Context, private val apiKey: String) {

    private val jobDao = SwipeApplyDatabase.getDatabase(context).jobDao()

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

    // To prevent infinite fallback loops
    private var isFallbackMode = false

    /**
     * Get INITIAL batch of 15 jobs.
     */
    fun getJobCards(
        search: String? = null,
        location: String? = null,
        remote: Boolean? = null,
        forceRefresh: Boolean = false
    ): Flow<Result<List<JobCard>>> = flow {
        if (forceRefresh) clearCache()

        // 1. Try filling queue with requested criteria
        if (jobQueue.isEmpty()) {
            fetchAndQueueJobs(search, location, remote)
        }

        // 2. FALLBACK: If queue is still empty (all duplicates?), try broad search
        if (jobQueue.isEmpty() && !isFallbackMode) {
            Log.w(TAG, "Primary search exhausted. Switching to Remote Fallback.")
            isFallbackMode = true
            currentPage = 1
            hasMorePages = true
            fetchAndQueueJobs(null, null, true) // Fetch Remote jobs
        }

        // 3. Prepare Initial Batch (Max 15)
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
            // Only emit failure if BOTH primary and fallback failed
            emit(Result.failure(Exception("No new jobs found. Try clearing history.")))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Pops ONE job from the queue to refill the UI.
     */
    suspend fun getNextJobFromQueue(
        search: String?, location: String?, remote: Boolean?
    ): JobCard? = withContext(Dispatchers.IO) {
        
        // Trigger fetch if low (< 5)
        if (jobQueue.size < 5 && hasMorePages && !isFetching) {
            try {
                // If we are in fallback mode, ignore user filters and fetch remote
                if (isFallbackMode) {
                    fetchAndQueueJobs(null, null, true)
                } else {
                    fetchAndQueueJobs(search, location, remote)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Background fetch failed", e)
            }
        }

        val job = jobQueue.removeFirstOrNull()
        if (job != null) queuedJobIds.remove(job.id)
        return@withContext job
    }

    /**
     * Recursive fetcher. Loops pages until it finds unseen jobs.
     */
    private suspend fun fetchAndQueueJobs(search: String?, location: String?, remote: Boolean?) {
        if (isFetching) return
        isFetching = true
        
        val authToken = FindWorkApiService.formatAuthToken(apiKey)
        val swipedIds = jobDao.getAllSwipedJobIds().toSet()

        try {
            // Fetch until queue has buffer or we run out of pages
            while (hasMorePages && jobQueue.size < 20) {
                Log.d(TAG, "Fetching Page $currentPage (Fallback: $isFallbackMode)")
                
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

                val rawJobs = response.results.mapNotNull { 
                    try { it.toEntity().toJobCard() } catch (e: Exception) { null } 
                }

                // FILTER: Remove jobs already swiped OR already in the queue
                val validJobs = rawJobs.filter { 
                    !swipedIds.contains(it.id) && !queuedJobIds.contains(it.id) 
                }

                validJobs.forEach { 
                    jobQueue.add(it)
                    queuedJobIds.add(it.id)
                }

                currentPage++
                
                if (validJobs.isEmpty() && hasMorePages) {
                    Log.w(TAG, "Page yielded duplicates only. Next page...")
                } else if (validJobs.isEmpty() && !hasMorePages) {
                    Log.w(TAG, "End of results reached.")
                    break
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fetch error: ${e.message}")
        } finally {
            isFetching = false
        }
    }

    suspend fun recordSwipe(jobId: String, direction: String) = withContext(Dispatchers.IO) {
        try { jobDao.insertSwipedJob(SwipedJobEntity(jobId, direction)) } 
        catch (e: Exception) { Log.e(TAG, "DB Error: ${e.message}") }
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        jobQueue.clear()
        queuedJobIds.clear()
        currentPage = 1
        hasMorePages = true
        isFallbackMode = false
    }
    
    suspend fun clearSwipeHistory() = withContext(Dispatchers.IO) {
        jobDao.deleteAllSwipedJobs()
        clearCache()
    }

    // Helper to allow refresh to work with existing ViewModel logic
    suspend fun refreshJobs(search: String?, location: String?, remote: Boolean?): Result<List<JobCard>> {
        var result: Result<List<JobCard>> = Result.failure(Exception("Unknown"))
        getJobCards(search, location, remote, true).collect { result = it }
        return result
    }

    fun hasMorePages() = hasMorePages
    fun getTotalCount() = totalCount
    suspend fun getJobCardById(id: String): JobCard? = null // Simplified for now

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