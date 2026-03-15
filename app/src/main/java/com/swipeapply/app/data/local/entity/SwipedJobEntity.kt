package com.swipeapply.app.data.local.entity

import androidx.room.Entity

/**
 * Tracks which jobs a user has swiped.
 * Uses composite primary key (jobId + userId) so each user has their own swipe history.
 * For dev mode (skip login), userId defaults to "local_user".
 */
@Entity(
    tableName = "swiped_jobs",
    primaryKeys = ["jobId", "userId"]
)
data class SwipedJobEntity(
    val jobId: String,
    val userId: String = "local_user", // Default for dev mode / no auth
    val direction: String, 
    val timestamp: Long = System.currentTimeMillis()
)