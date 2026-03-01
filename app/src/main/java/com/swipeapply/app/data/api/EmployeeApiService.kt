package com.swipeapply.app.data.api

import com.swipeapply.app.data.model.EmployeeSearchResponse
import retrofit2.Response
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Retrofit API interface for the Employee/Referral Search service.
 * Base URL: http://144.24.154.69:8080/
 * 
 * The identifier can be a domain (e.g., "maqsoftware.com") 
 * OR a company name (e.g., "maqsoftware").
 */
interface EmployeeApiService {

    @POST("api/v1/companies/{identifier}/employees")
    suspend fun getCompanyEmployees(
        @Path("identifier") companyIdentifier: String,
        @Header("X-User-ID") userId: String
    ): Response<EmployeeSearchResponse>
}
