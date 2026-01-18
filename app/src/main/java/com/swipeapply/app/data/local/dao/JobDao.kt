package com.swipeapply.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.swipeapply.app.data.local.entity.JobEntity
import kotlinx.coroutines.flow.Flow

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
}