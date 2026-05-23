package com.swipeapply.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for persisting saved employee contacts across app restarts.
 */
@Entity(
    tableName = "saved_contacts",
    indices = [
        Index(value = ["companyName"]),
        Index(value = ["userId"])
    ]
)
data class SavedContactEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val companyName: String,
    val fullName: String,
    val email: String,
    val emailStatus: String = "",
    val jobTitle: String? = null,
    val seniority: String? = null,
    val department: String? = null,
    val linkedinUrl: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    val confidence: Int = 0,
    val savedAtMs: Long = System.currentTimeMillis()
)
