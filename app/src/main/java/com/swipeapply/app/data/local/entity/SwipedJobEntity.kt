package com.swipeapply.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * Tracks which jobs a user has swiped.
 * Uses composite primary key (jobId + userId) so each user has their own swipe history.
 * For dev mode (skip login), userId defaults to "local_user".
 * 
 * Indices:
 * - (userId, timestamp) for efficient sorted swipe history queries
 * - (userId, direction) for filtered direction-based lookups
 */
@Entity(
    tableName = "swiped_jobs",
    primaryKeys = ["jobId", "userId"],
    indices = [
        Index(value = ["userId", "timestamp"]),
        Index(value = ["userId", "direction"])
    ]
)
data class SwipedJobEntity(
    val jobId: String,
    val userId: String = "local_user",
    val direction: String, 
    val timestamp: Long = System.currentTimeMillis()
)