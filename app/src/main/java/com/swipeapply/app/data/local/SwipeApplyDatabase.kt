package com.swipeapply.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.swipeapply.app.data.local.dao.JobDao
import com.swipeapply.app.data.local.entity.Converters
import com.swipeapply.app.data.local.entity.JobEntity
import com.swipeapply.app.data.local.entity.SwipedJobEntity

/**
 * Room database for SwipeApply app
 * 
 * Version History:
 * - v1: Initial schema
 * - v2: Added SwipedJobEntity
 * - v3: Added userId to SwipedJobEntity (user-specific swipe tracking)
 */
@Database(
    entities = [JobEntity::class, SwipedJobEntity::class], 
    version = 3, 
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SwipeApplyDatabase : RoomDatabase() {
    
    abstract fun jobDao(): JobDao
    
    companion object {
        @Volatile
        private var INSTANCE: SwipeApplyDatabase? = null
        
        fun getDatabase(context: Context): SwipeApplyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SwipeApplyDatabase::class.java,
                    "swipe_apply_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}