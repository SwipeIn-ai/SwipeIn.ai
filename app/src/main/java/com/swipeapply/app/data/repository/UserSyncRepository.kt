package com.swipeapply.app.data.repository

import android.util.Log
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val TAG = "UserSyncRepo"
private const val BASE_URL = "http://144.24.154.69:8080"

/**
 * Syncs the Supabase-authenticated user with the SwipeIn backend.
 *
 * The backend uses its own UUID as X-User-ID for employee searches.
 * This repository calls POST /api/v1/users/sync with the Supabase UUID + email,
 * so the backend creates (or returns) a user record whose UUID == the Supabase UUID.
 * After this one call, the Supabase auth UID can be used directly as X-User-ID.
 */
object UserSyncRepository {

    // In-memory cache: once synced per app session we don't need to re-call
    @Volatile
    private var syncedUserId: String? = null

    private val client: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Syncs user with the backend and returns the backend userId that should be
     * used as X-User-ID in all employee API calls.
     *
     * Behaviour:
     * - If [supabaseId] is null/blank (guest mode) returns the dev fallback UUID.
     * - If already synced this session, returns the cached result.
     * - Calls POST /api/v1/users/sync and caches the returned userId.
     * - On any network failure, falls back to [supabaseId] itself (which will work
     *   once the user has been synced at least once).
     */
    suspend fun syncAndGetUserId(
        supabaseId: String?,
        email: String?,
        displayName: String? = null
    ): String = withContext(Dispatchers.IO) {
        // Guest / not logged in
        if (supabaseId.isNullOrBlank()) {
            Log.w(TAG, "No Supabase ID — using dev fallback")
            return@withContext DEV_FALLBACK_ID
        }

        // Already synced this session
        syncedUserId?.let { cached ->
            Log.d(TAG, "Using cached synced userId: $cached")
            return@withContext cached
        }

        return@withContext try {
            val payload = JSONObject().apply {
                put("supabaseId", supabaseId)
                put("email", email ?: "")
                put("displayName", displayName ?: "")
            }.toString()

            val request = Request.Builder()
                .url("$BASE_URL/api/v1/users/sync")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful && body.isNotBlank()) {
                val json = JSONObject(body)
                val userId = json.optString("userId", supabaseId)
                val status = json.optString("status", "unknown")
                Log.d(TAG, "✅ User synced — status=$status, userId=$userId")
                syncedUserId = userId  // cache for session
                userId
            } else {
                Log.w(TAG, "Sync response not OK (${response.code}) — falling back to supabaseId")
                supabaseId  // backend already has this user (likely)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Sync failed: ${e.message} — falling back to supabaseId")
            supabaseId  // use Supabase ID directly as best-effort
        }
    }

    /** Clear the session cache (call on logout). */
    fun clearSession() {
        syncedUserId = null
    }

    private const val DEV_FALLBACK_ID = "0f1b204c-4b01-448d-9b76-0a18828b114b"
}
