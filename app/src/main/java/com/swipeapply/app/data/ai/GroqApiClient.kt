package com.swipeapply.app.data.ai

import android.util.Log
import com.swipeapply.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * Centralized Groq API client with retry, rate-limit, and timeout handling.
 *
 * All Groq callers (ResumeParser, IntroTemplateGenerator, ReferralTemplateGenerator)
 * should use this client instead of making raw HTTP calls.
 */
object GroqApiClient {

    private const val TAG = "GroqApiClient"
    private const val GROQ_URL = "https://api.groq.com/openai/v1/chat/completions"
    const val DEFAULT_MODEL = "llama-3.1-8b-instant"

    private const val MAX_RETRIES = 3
    private const val INITIAL_BACKOFF_MS = 1000L
    private const val MAX_BACKOFF_MS = 16000L

    // Rate limit tracking
    private val lastRequestTime = AtomicLong(0L)
    private const val MIN_REQUEST_INTERVAL_MS = 200L // 5 req/s safety margin

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    data class GroqRequest(
        val model: String = DEFAULT_MODEL,
        val messages: List<GroqMessage>,
        val temperature: Double = 0.2,
        val maxTokens: Int = 512
    )

    data class GroqMessage(
        val role: String,
        val content: String
    )

    sealed class GroqResult {
        data class Success(val content: String) : GroqResult()
        data class Error(val message: String, val isRetryable: Boolean = false) : GroqResult()
    }

    /**
     * Send a completion request to Groq with automatic retry and rate-limit handling.
     */
    suspend fun complete(request: GroqRequest): GroqResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GROQ_API_KEY
        if (apiKey.isBlank()) {
            return@withContext GroqResult.Error("Groq API key not configured")
        }

        var lastError: String? = null
        var backoffMs = INITIAL_BACKOFF_MS

        for (attempt in 1..MAX_RETRIES) {
            // Rate limit: space out requests
            enforceRateLimit()

            val requestBody = JSONObject().apply {
                put("model", request.model)
                put("messages", JSONArray().apply {
                    request.messages.forEach { msg ->
                        put(JSONObject().apply {
                            put("role", msg.role)
                            put("content", msg.content)
                        })
                    }
                })
                put("temperature", request.temperature)
                put("max_tokens", request.maxTokens)
            }

            val httpRequest = Request.Builder()
                .url(GROQ_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            try {
                val response = httpClient.newCall(httpRequest).execute()
                val body = response.body.string()

                when {
                    response.isSuccessful -> {
                        val content = extractContent(body)
                        if (content != null) {
                            Log.d(TAG, "Groq success on attempt $attempt")
                            return@withContext GroqResult.Success(content)
                        } else {
                            lastError = "Empty response from Groq"
                            Log.w(TAG, "Empty content on attempt $attempt")
                        }
                    }
                    response.code == 429 -> {
                        // Rate limited - use server's retry-after if available
                        val retryAfter = response.header("retry-after")?.toLongOrNull()
                        val waitMs = if (retryAfter != null) {
                            (retryAfter * 1000L).coerceAtMost(MAX_BACKOFF_MS)
                        } else {
                            backoffMs
                        }
                        lastError = "Rate limited (429)"
                        Log.w(TAG, "Rate limited on attempt $attempt, waiting ${waitMs}ms")
                        delay(waitMs)
                        backoffMs = (backoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
                        continue
                    }
                    response.code in 500..599 -> {
                        lastError = "Server error ${response.code}"
                        Log.w(TAG, "Server error on attempt $attempt: ${response.code}")
                        delay(backoffMs)
                        backoffMs = (backoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
                        continue
                    }
                    response.code == 401 -> {
                        return@withContext GroqResult.Error("Invalid Groq API key", isRetryable = false)
                    }
                    else -> {
                        lastError = "HTTP ${response.code}: $body"
                        Log.e(TAG, "Non-retryable error on attempt $attempt: ${response.code}")
                        return@withContext GroqResult.Error(lastError)
                    }
                }
            } catch (e: SocketTimeoutException) {
                lastError = "Request timed out"
                Log.w(TAG, "Timeout on attempt $attempt: ${e.message}")
                if (attempt < MAX_RETRIES) {
                    delay(backoffMs)
                    backoffMs = (backoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
                }
            } catch (e: IOException) {
                lastError = "Network error: ${e.message}"
                Log.w(TAG, "Network error on attempt $attempt: ${e.message}")
                if (attempt < MAX_RETRIES) {
                    delay(backoffMs)
                    backoffMs = (backoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
                }
            } catch (e: Exception) {
                lastError = "Unexpected error: ${e.message}"
                Log.e(TAG, "Unexpected error on attempt $attempt", e)
                return@withContext GroqResult.Error(lastError ?: "Unexpected error")
            }
        }

        GroqResult.Error("Failed after $MAX_RETRIES attempts: $lastError", isRetryable = true)
    }

    /**
     * Convenience method for single-message user prompts.
     */
    suspend fun complete(
        prompt: String,
        model: String = DEFAULT_MODEL,
        temperature: Double = 0.2,
        maxTokens: Int = 512
    ): GroqResult {
        return complete(
            GroqRequest(
                model = model,
                messages = listOf(GroqMessage("user", prompt)),
                temperature = temperature,
                maxTokens = maxTokens
            )
        )
    }

    private fun extractContent(responseBody: String): String? {
        return try {
            val json = JSONObject(responseBody)
            val choices = json.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                val message = choices.getJSONObject(0).optJSONObject("message")
                message?.optString("content")?.takeIf { it.isNotBlank() && it != "null" }
                    ?: message?.optString("reasoning")?.takeIf { it.isNotBlank() && it != "null" }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Groq response: ${e.message}")
            null
        }
    }

    private suspend fun enforceRateLimit() {
        val now = System.currentTimeMillis()
        val last = lastRequestTime.get()
        val elapsed = now - last
        if (elapsed < MIN_REQUEST_INTERVAL_MS) {
            delay(MIN_REQUEST_INTERVAL_MS - elapsed)
        }
        lastRequestTime.set(System.currentTimeMillis())
    }
}
