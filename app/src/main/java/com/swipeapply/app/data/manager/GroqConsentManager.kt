package com.swipeapply.app.data.manager

import android.content.Context
import android.content.SharedPreferences

/**
 * Stores user's consent for sharing profile/job context with Groq.
 */
class GroqConsentManager private constructor(context: Context) {

    companion object {
        private const val PREFS_NAME = "privacy_prefs"
        private const val KEY_GROQ_OUTREACH_CONSENT = "groq_outreach_consent"

        @Volatile
        private var INSTANCE: GroqConsentManager? = null

        fun getInstance(context: Context): GroqConsentManager {
            return INSTANCE ?: synchronized(this) {
                GroqConsentManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hasOutreachConsent(): Boolean {
        return prefs.getBoolean(KEY_GROQ_OUTREACH_CONSENT, false)
    }

    fun grantOutreachConsent() {
        prefs.edit().putBoolean(KEY_GROQ_OUTREACH_CONSENT, true).apply()
    }

    fun revokeOutreachConsent() {
        prefs.edit().putBoolean(KEY_GROQ_OUTREACH_CONSENT, false).apply()
    }
}
