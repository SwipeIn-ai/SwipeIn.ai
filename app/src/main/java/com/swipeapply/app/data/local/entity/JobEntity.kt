package com.swipeapply.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.internal.`$Gson$Types`

/**
 * Room entity for storing job data locally
 */
@Entity(tableName = "jobs")
data class JobEntity(
    @PrimaryKey
    val id: String,
    val role: String,
    val companyName: String,
    val location: String,
    val remote: Boolean,
    val url: String?,
    val description: String?,
    val datePosted: String?,
    val keywords: String?, // JSON string of List<String>
    val source: String?,
    val employmentType: String?,
    val logoUrl: String?,
    val fetchedAt: Long = System.currentTimeMillis()
)

/**
 * Type converters for Room database
 */
class Converters {
    private val gson = Gson()

    // Constructed without an anonymous class so KSP can safely enumerate declarations
    private val stringListType = `$Gson$Types`.newParameterizedTypeWithOwner(
        null, List::class.java, String::class.java
    )

    @TypeConverter
    fun fromStringList(value: List<String>?): String? = gson.toJson(value)

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        if (value == null) return null
        @Suppress("UNCHECKED_CAST")
        return gson.fromJson(value, stringListType) as? List<String>
    }
}