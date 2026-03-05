package com.swipeapply.app.data.manager

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Persists and manages the user's daily swipe streak.
 *
 * Streak rules:
 * - First ever swipe        → streak = 1
 * - Swiped again today      → streak unchanged, todayCount++
 * - Swiped yesterday        → streak++, todayCount = 1
 * - Missed ≥1 day           → streak resets to 1
 */
class StreakManager private constructor(context: Context) {

    companion object {
        private const val PREFS_NAME = "streak_prefs"
        private const val KEY_LAST_SWIPE_DATE = "last_swipe_date"
        private const val KEY_STREAK = "current_streak"
        private const val KEY_TODAY_SWIPES = "today_swipe_count"

        const val DAILY_GOAL = 10

        @Volatile
        private var INSTANCE: StreakManager? = null

        fun getInstance(context: Context): StreakManager {
            return INSTANCE ?: synchronized(this) {
                StreakManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val fmt: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /** Current streak in days. Reads fresh every time (safe to call from StateFlow). */
    val currentStreak: Int
        get() {
            refreshIfNewDay()
            return prefs.getInt(KEY_STREAK, 0)
        }

    /** Swipes completed today. Resets to 0 on a new calendar day. */
    val todaySwipeCount: Int
        get() {
            refreshIfNewDay()
            return prefs.getInt(KEY_TODAY_SWIPES, 0)
        }

    /**
     * Call once per swipe action. Atomically updates today's count and the streak.
     * Returns the updated (streak, todayCount) pair.
     */
    fun recordSwipe(): Pair<Int, Int> {
        val today = LocalDate.now().format(fmt)
        val yesterday = LocalDate.now().minusDays(1).format(fmt)
        val lastDate = prefs.getString(KEY_LAST_SWIPE_DATE, null)

        val (newStreak, newCount) = when (lastDate) {
            null -> 1 to 1
            today -> prefs.getInt(KEY_STREAK, 1) to (prefs.getInt(KEY_TODAY_SWIPES, 0) + 1)
            yesterday -> (prefs.getInt(KEY_STREAK, 0) + 1) to 1
            else -> 1 to 1   // missed at least one day — reset
        }

        prefs.edit()
            .putString(KEY_LAST_SWIPE_DATE, today)
            .putInt(KEY_STREAK, newStreak)
            .putInt(KEY_TODAY_SWIPES, newCount)
            .apply()

        return newStreak to newCount
    }

    /** If the stored date is not today, reset todaySwipeCount (streak checked on next swipe). */
    private fun refreshIfNewDay() {
        val today = LocalDate.now().format(fmt)
        val lastDate = prefs.getString(KEY_LAST_SWIPE_DATE, null)
        if (lastDate != null && lastDate != today) {
            prefs.edit().putInt(KEY_TODAY_SWIPES, 0).apply()
        }
    }
}
