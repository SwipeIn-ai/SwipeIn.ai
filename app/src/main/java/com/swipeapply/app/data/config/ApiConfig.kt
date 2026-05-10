package com.swipeapply.app.data.config

/**
 * API Configuration
 * Store your API keys and defaults here
 */
object ApiConfig {
    // API key loaded from BuildConfig
    const val FINDWORK_API_KEY = com.swipeapply.app.BuildConfig.FINDWORK_API_KEY
    
    // JobSpy API base URL (defaults to hosted instance, can be overridden via BuildConfig)
    val JOBSPY_BASE_URL: String = com.swipeapply.app.BuildConfig.JOBSPY_BASE_URL.ifEmpty {
        "https://api.sudhirsharma.dev/"
    }
    
    // Search preferences - null to get ALL jobs without filtering
    val DEFAULT_SEARCH_QUERY: String? = null  // null = all jobs, or set specific keyword like "android"
    val DEFAULT_LOCATION: String? = null  // null = worldwide (no location filter)
}