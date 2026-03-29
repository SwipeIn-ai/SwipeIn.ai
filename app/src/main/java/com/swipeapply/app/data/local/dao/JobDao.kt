package com.swipeapply.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.swipeapply.app.data.local.entity.JobEntity
import kotlinx.coroutines.flow.Flow
import com.swipeapply.app.data.local.entity.SwipedJobEntity

/**
 * Data Access Object for Job entities
 */
@Dao
interface JobDao {
    
    @Query("SELECT * FROM jobs ORDER BY fetchedAt DESC")
    fun getAllJobs(): Flow<List<JobEntity>>
    
    @Query("SELECT * FROM jobs WHERE id = :jobId")
    suspend fun getJobById(jobId: String): JobEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJobs(jobs: List<JobEntity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: JobEntity)
    
    @Query("DELETE FROM jobs")
    suspend fun deleteAllJobs()
    
    @Query("SELECT COUNT(*) FROM jobs")
    suspend fun getJobCount(): Int
    
    @Query("DELETE FROM jobs WHERE fetchedAt < :timestamp")
    suspend fun deleteOldJobs(timestamp: Long)

    // ===== USER-AWARE SWIPE TRACKING =====
    
    /** Get all job IDs swiped by a specific user */
    @Query("SELECT jobId FROM swiped_jobs WHERE userId = :userId")
    suspend fun getSwipedJobIdsByUser(userId: String): List<String>
    
    /** Legacy: Get ALL swiped job IDs (for backward compatibility during migration) */
    @Query("SELECT DISTINCT jobId FROM swiped_jobs")
    suspend fun getAllSwipedJobIds(): List<String>

    @Query("SELECT * FROM swiped_jobs WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentSwipedJobsByUser(userId: String, limit: Int): List<SwipedJobEntity>

    @Query("SELECT * FROM swiped_jobs WHERE userId = :userId AND direction = :direction ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentSwipedJobsByUserAndDirection(
        userId: String,
        direction: String,
        limit: Int
    ): List<SwipedJobEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSwipedJob(swipedJob: SwipedJobEntity)

    @Query("DELETE FROM swiped_jobs WHERE jobId = :jobId AND userId = :userId")
    suspend fun deleteSwipedJobByUser(jobId: String, userId: String)

    /** Clear swipe history for a specific user only */
    @Query("DELETE FROM swiped_jobs WHERE userId = :userId")
    suspend fun deleteSwipedJobsByUser(userId: String)
    
    /** Legacy: Clear ALL swipe history (destructive) */
    @Query("DELETE FROM swiped_jobs")
    suspend fun deleteAllSwipedJobs()

    /** Get jobs not swiped by a specific user */
    @Query("SELECT * FROM jobs WHERE id NOT IN (SELECT jobId FROM swiped_jobs WHERE userId = :userId) ORDER BY fetchedAt DESC")
    suspend fun getAvailableJobsForUser(userId: String): List<JobEntity>
    
    /** Legacy: Get jobs not swiped by anyone (backward compatibility) */
    @Query("SELECT * FROM jobs WHERE id NOT IN (SELECT jobId FROM swiped_jobs) ORDER BY fetchedAt DESC")
    suspend fun getAvailableJobs(): List<JobEntity>
}
