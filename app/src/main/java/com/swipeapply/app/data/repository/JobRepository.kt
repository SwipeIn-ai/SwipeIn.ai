package com.swipeapply.app.data.repository

import android.content.Context
import android.util.Log
import com.swipeapply.app.data.api.FindWorkApiService
import com.swipeapply.app.data.local.SwipeApplyDatabase
import com.swipeapply.app.data.mapper.toEntity
import com.swipeapply.app.data.mapper.toJobCard
import com.swipeapply.app.data.model.JobCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Repository for fetching and caching job data
 * Supports pagination to load all results
 */
class JobRepository(private val context: Context, private val apiKey: String) {
    
    private val apiService: FindWorkApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        
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
    
    // In-memory cache for all jobs
    private var jobsCache: MutableList<JobCard> = mutableListOf()
    private var currentPage = 1
    private var hasMorePages = true
    private var totalCount = 0

    /**
     * Get jobs from API - fetches first page initially
     */
    fun getJobCards(
        search: String? = null,
        location: String? = null,
        remote: Boolean? = null,
        forceRefresh: Boolean = false
    ): Flow<Result<List<JobCard>>> = flow {
        try {
            // If we have cached data and not forced refresh, emit it first
            if (!forceRefresh && jobsCache.isNotEmpty()) {
                Log.d(TAG, "Emitting cached jobs: ${jobsCache.size} jobs")
                emit(Result.success(jobsCache.toList()))
            } else {
                // Reset pagination state for fresh fetch
                currentPage = 1
                hasMorePages = true
                jobsCache.clear()
                
                Log.d(TAG, "Fetching jobs from API with search='$search', location='$location', remote=$remote")
                val authToken = FindWorkApiService.formatAuthToken(apiKey)
                
                try {
                    val response = apiService.getJobs(
                        authToken = authToken,
                        search = search,
                        location = location,
                        remote = remote,
                        sortBy = "relevance",
                        page = currentPage
                    )
                    
                    totalCount = response.count
                    hasMorePages = response.next != null
                    
                    Log.d(TAG, "API Response - Total: $totalCount, Page results: ${response.results.size}, Has more: $hasMorePages")
                    
                    if (response.results.isEmpty()) {
                        Log.w(TAG, "API returned empty results")
                        emit(Result.failure(Exception("No jobs found matching your criteria")))
                        return@flow
                    }
                    
                    // Map ALL results from first page
                    val jobs = response.results.mapNotNull { findWorkJob ->
                        try {
                            findWorkJob.toEntity().toJobCard()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error mapping job ${findWorkJob.id}: ${e.message}")
                            null
                        }
                    }
                    
                    Log.d(TAG, "Successfully processed ${jobs.size} jobs from page $currentPage")
                    
                    if (jobs.isEmpty()) {
                        emit(Result.failure(Exception("Failed to process job data")))
                        return@flow
                    }
                    
                    // Store in cache
                    jobsCache.addAll(jobs)
                    currentPage++
                    
                    emit(Result.success(jobsCache.toList()))
                    
                } catch (apiException: Exception) {
                    Log.e(TAG, "API call failed: ${apiException.message}", apiException)
                    val errorMessage = when {
                        apiException.message?.contains("401") == true -> 
                            "Authentication failed. Please check your API key."
                        apiException.message?.contains("404") == true -> 
                            "API endpoint not found."
                        apiException.message?.contains("Unable to resolve host") == true -> 
                            "Cannot reach findwork.dev. Check your internet connection."
                        apiException.message?.contains("timeout") == true ->
                            "Request timeout. Please try again."
                        else -> "API Error: ${apiException.message ?: "Unknown error"}"
                    }
                    emit(Result.failure(Exception(errorMessage)))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in getJobCards: ${e.message}", e)
            emit(Result.failure(Exception("An unexpected error occurred: ${e.message}")))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Load more jobs (next page)
     */
    suspend fun loadMoreJobs(
        search: String? = null,
        location: String? = null,
        remote: Boolean? = null
    ): Result<List<JobCard>> = withContext(Dispatchers.IO) {
        if (!hasMorePages) {
            Log.d(TAG, "No more pages to load")
            return@withContext Result.success(jobsCache.toList())
        }
        
        try {
            Log.d(TAG, "Loading more jobs - page $currentPage")
            val authToken = FindWorkApiService.formatAuthToken(apiKey)
            
            val response = apiService.getJobs(
                authToken = authToken,
                search = search,
                location = location,
                remote = remote,
                sortBy = "relevance",
                page = currentPage
            )
            
            hasMorePages = response.next != null
            
            val jobs = response.results.mapNotNull { findWorkJob ->
                try {
                    findWorkJob.toEntity().toJobCard()
                } catch (e: Exception) {
                    Log.e(TAG, "Error mapping job: ${e.message}")
                    null
                }
            }
            
            jobsCache.addAll(jobs)
            currentPage++
            
            Log.d(TAG, "Loaded ${jobs.size} more jobs. Total: ${jobsCache.size}")
            Result.success(jobsCache.toList())
        } catch (e: Exception) {
            Log.e(TAG, "Error loading more jobs: ${e.message}", e)
            Result.failure(Exception(e.message ?: "Failed to load more jobs"))
        }
    }
    
    /**
     * Check if there are more pages to load
     */
    fun hasMorePages(): Boolean = hasMorePages
    
    /**
     * Get total count of jobs
     */
    fun getTotalCount(): Int = totalCount
    
    /**
     * Get a specific job by ID from in-memory cache
     */
    suspend fun getJobCardById(id: String): JobCard? = withContext(Dispatchers.IO) {
        jobsCache.find { it.id == id }
    }
    
    /**
     * Refresh jobs from API (resets pagination)
     */
    suspend fun refreshJobs(
        search: String? = null,
        location: String? = null,
        remote: Boolean? = null
    ): Result<List<JobCard>> = withContext(Dispatchers.IO) {
        try {
            // Reset pagination
            currentPage = 1
            hasMorePages = true
            jobsCache.clear()
            
            Log.d(TAG, "Refreshing jobs from API...")
            val authToken = FindWorkApiService.formatAuthToken(apiKey)
            
            val response = apiService.getJobs(
                authToken = authToken,
                search = search,
                location = location,
                remote = remote,
                sortBy = "relevance",
                page = 1
            )
            
            totalCount = response.count
            hasMorePages = response.next != null
            
            if (response.results.isEmpty()) {
                return@withContext Result.failure(Exception("No jobs found"))
            }
            
            val jobCards = response.results.mapNotNull { findWorkJob ->
                try {
                    findWorkJob.toEntity().toJobCard()
                } catch (e: Exception) {
                    Log.e(TAG, "Error mapping job: ${e.message}")
                    null
                }
            }
            
            if (jobCards.isEmpty()) {
                return@withContext Result.failure(Exception("Failed to process job data"))
            }
            
            jobsCache.addAll(jobCards)
            currentPage++
            
            Log.d(TAG, "Refreshed with ${jobCards.size} jobs. Total available: $totalCount")
            Result.success(jobsCache.toList())
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing jobs: ${e.message}", e)
            Result.failure(Exception(e.message ?: "Unknown error"))
        }
    }
    
    /**
     * Clear all cached jobs
     */
    suspend fun clearCache() = withContext(Dispatchers.IO) {
        jobsCache.clear()
        currentPage = 1
        hasMorePages = true
        totalCount = 0
    }
    
    companion object {
        private const val TAG = "JobRepository"
        
        @Volatile
        private var INSTANCE: JobRepository? = null
        
        fun getInstance(context: Context, apiKey: String): JobRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = JobRepository(context.applicationContext, apiKey)
                INSTANCE = instance
                instance
            }
        }
    }
}
