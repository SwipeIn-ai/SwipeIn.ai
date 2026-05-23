package com.swipeapply.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.swipeapply.app.data.local.entity.SavedContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedContactDao {

    @Query("SELECT * FROM saved_contacts WHERE userId = :userId ORDER BY savedAtMs DESC")
    fun getContactsByUser(userId: String): Flow<List<SavedContactEntity>>

    @Query("SELECT * FROM saved_contacts WHERE userId = :userId AND companyName = :companyName ORDER BY savedAtMs DESC")
    suspend fun getContactsByCompany(userId: String, companyName: String): List<SavedContactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<SavedContactEntity>)

    @Query("DELETE FROM saved_contacts WHERE userId = :userId AND companyName = :companyName")
    suspend fun deleteByCompany(userId: String, companyName: String)

    @Query("DELETE FROM saved_contacts WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Query("SELECT COUNT(*) FROM saved_contacts WHERE userId = :userId")
    suspend fun getCountForUser(userId: String): Int
}
