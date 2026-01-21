package com.swipeapply.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "swiped_jobs")
data class SwipedJobEntity(
    @PrimaryKey val jobId: String,
    val direction: String, 
    val timestamp: Long = System.currentTimeMillis()
)