package com.swipeapply.app.data.api

import com.swipeapply.app.data.api.model.FindWorkApiResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * FindWork.dev API service interface
 * API Documentation: https://findwork.dev/developers/
 */
interface FindWorkApiService {
    
    @GET("api/jobs/")
    suspend fun getJobs(
        @Header("Authorization") authToken: String,
        @Query("search") search: String? = null,
        @Query("location") location: String? = null,
        @Query("remote") remote: Boolean? = null,
        @Query("employment_type") employmentType: String? = null,
        @Query("sort_by") sortBy: String? = "date_posted",
        @Query("page") page: Int? = 1
    ): FindWorkApiResponse
    
    companion object {
        const val BASE_URL = "https://findwork.dev/"
        
        // Format: "Token YOUR_API_KEY"
        fun formatAuthToken(apiKey: String): String = "Token $apiKey"
    }
}