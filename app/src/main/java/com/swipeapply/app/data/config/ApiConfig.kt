package com.swipeapply.app.data.config

/**
 * API Configuration
 * Store your FindWork.dev API key here
 */
object ApiConfig {
    // TODO: Replace with your actual API key from https://findwork.dev/developers/
    // Format: Just the key itself, without "Token " prefix
    const val FINDWORK_API_KEY = "88dfaa3a4e8c9614a5514897a5e4b1ae711a4c3f"
    
    // Search preferences - null to get ALL jobs without filtering
    val DEFAULT_SEARCH_QUERY: String? = null  // null = all jobs, or set specific keyword like "android"
    val DEFAULT_LOCATION: String? = "India"  // Filter for India jobs
    // null = show all jobs (remote + on-site), true = remote only, false = on-site only
    val DEFAULT_REMOTE_ONLY: Boolean? = null
}