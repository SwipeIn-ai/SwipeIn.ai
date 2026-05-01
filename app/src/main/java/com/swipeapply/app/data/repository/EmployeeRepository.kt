package com.swipeapply.app.data.repository

import android.util.Log
import com.swipeapply.app.data.api.EmployeeApiClient
import com.swipeapply.app.data.model.EmployeeSearchResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for fetching employee/referral contacts from the API.
 * Acts as the single source of truth between the API and the UI layer.
 */
class EmployeeRepository {

    companion object {
        private const val TAG = "EmployeeRepository"
        
        @Volatile
        private var INSTANCE: EmployeeRepository? = null

        fun getInstance(): EmployeeRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EmployeeRepository().also { INSTANCE = it }
            }
        }
    }

    /**
     * Fetch employees for a given company.
     * @param companyIdentifier Company domain (e.g., "google.com") or name (e.g., "google")
     * @param userId The X-User-ID header value for rate limiting
     * @return Result wrapping the API response
     */
    suspend fun fetchEmployees(
        companyIdentifier: String,
        userId: String
    ): Result<EmployeeSearchResponse> = withContext(Dispatchers.IO) {
        // Sanitize input to prevent path traversal
        val sanitizedIdentifier = companyIdentifier
            .replace("/", "")
            .replace("\\", "")
            .replace("..", "")
            .trim()
        
        if (sanitizedIdentifier.isBlank()) {
            return@withContext Result.failure(Exception("Invalid company name"))
        }

        try {
            Log.d(TAG, "Fetching employees for: $sanitizedIdentifier")
            
            val response = EmployeeApiClient.service.getCompanyEmployees(
                companyIdentifier = sanitizedIdentifier,
                userId = userId
            )

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                Log.d(TAG, "Found ${body.employees.size} employees")
                Log.d(TAG, "Remaining swipes: ${body.remainingSwipes}")
                Result.success(body)
            } else {
                // Try to parse the JSON error body for a user-friendly message
                val errorBody = response.errorBody()?.string()
                val serverMessage = try {
                    val json = org.json.JSONObject(errorBody ?: "")
                    json.optString("message", "").takeIf { it.isNotBlank() }
                } catch (_: Exception) { null }
                
                val errorMsg = serverMessage
                    ?: "Could not load contacts. Please try again."
                Log.e(TAG, "❌ API Error ${response.code()}: ${response.message()}")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Network error: ${e.message}", e)
            Result.failure(e)
        }
    }
}
