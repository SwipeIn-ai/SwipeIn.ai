package com.swipeapply.app.data.manager

import android.content.Context
import android.content.SharedPreferences
import com.swipeapply.app.data.model.ApplicationStatus

class SavedJobStatusManager private constructor(context: Context) {

    companion object {
        private const val PREFS_NAME = "saved_job_status_prefs"

        @Volatile
        private var INSTANCE: SavedJobStatusManager? = null

        fun getInstance(context: Context): SavedJobStatusManager {
            return INSTANCE ?: synchronized(this) {
                SavedJobStatusManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getStatus(userId: String, jobId: String): ApplicationStatus? {
        val raw = prefs.getString(statusKey(userId, jobId), null) ?: return null
        return runCatching { ApplicationStatus.valueOf(raw) }.getOrNull()
    }

    fun setStatus(userId: String, jobId: String, status: ApplicationStatus) {
        prefs.edit().putString(statusKey(userId, jobId), status.name).apply()
    }

    fun clearStatus(userId: String, jobId: String) {
        prefs.edit().remove(statusKey(userId, jobId)).apply()
    }

    fun clearAllForUser(userId: String) {
        val prefix = userPrefix(userId)
        val keysToRemove = prefs.all.keys.filter { it.startsWith(prefix) }
        prefs.edit().apply {
            keysToRemove.forEach(::remove)
        }.apply()
    }

    private fun statusKey(userId: String, jobId: String): String = "${userPrefix(userId)}$jobId"

    private fun userPrefix(userId: String): String = "status_${userId}_"
}
