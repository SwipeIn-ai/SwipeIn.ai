package com.swipeapply.app.data.api

import com.swipeapply.app.data.api.model.JobSpyApiResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * JobSpy API service interface
 * API Documentation: https://api.sudhirsharma.dev/docs
 */
interface JobSpyApiService {

    @GET("api/v1/jobs")
    suspend fun getJobs(
        @Query("keyword") keyword: String? = null,
        @Query("location") location: String? = null,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("sort_by") sortBy: String? = "date",
        @Query("sort_order") sortOrder: String? = "desc",
        @Query("site") site: List<String>? = DEFAULT_SITES,
        @Query("country_indeed") countryIndeed: String? = "india"
    ): JobSpyApiResponse

    companion object {
        const val BASE_URL = "https://api.sudhirsharma.dev/"

        val DEFAULT_SITES = listOf("indeed", "linkedin", "glassdoor", "google")
    }
}
