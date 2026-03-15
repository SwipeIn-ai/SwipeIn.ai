package com.swipeapply.app.data.config

/**
 * API Configuration
 * Store your FindWork.dev API key here
 */
object ApiConfig {
    // API key loaded from BuildConfig
    const val FINDWORK_API_KEY = com.swipeapply.app.BuildConfig.FINDWORK_API_KEY
    
    // Search preferences - null to get ALL jobs without filtering
    val DEFAULT_SEARCH_QUERY: String? = null  // null = all jobs, or set specific keyword like "android"
    val DEFAULT_LOCATION: String? = null  // null = worldwide (no location filter)
    // null = show all jobs (remote + on-site), true = remote only, false = on-site only
    val DEFAULT_REMOTE_ONLY: Boolean? = true  // Show all remote jobs worldwide
}