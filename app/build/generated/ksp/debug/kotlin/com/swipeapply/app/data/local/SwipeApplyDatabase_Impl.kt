package com.swipeapply.app.`data`.local

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.swipeapply.app.`data`.local.dao.JobDao
import com.swipeapply.app.`data`.local.dao.JobDao_Impl
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class SwipeApplyDatabase_Impl : SwipeApplyDatabase() {
  private val _jobDao: Lazy<JobDao> = lazy {
    JobDao_Impl(this)
  }


  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(3,
        "0d1b7d2b939251c8512df7730cf091f5", "1d8e65cd41ba1acfea3a80b62c74a5e1") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `jobs` (`id` TEXT NOT NULL, `role` TEXT NOT NULL, `companyName` TEXT NOT NULL, `location` TEXT NOT NULL, `remote` INTEGER NOT NULL, `url` TEXT, `description` TEXT, `datePosted` TEXT, `keywords` TEXT, `source` TEXT, `employmentType` TEXT, `logoUrl` TEXT, `fetchedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `swiped_jobs` (`jobId` TEXT NOT NULL, `userId` TEXT NOT NULL, `direction` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`jobId`, `userId`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '0d1b7d2b939251c8512df7730cf091f5')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `jobs`")
        connection.execSQL("DROP TABLE IF EXISTS `swiped_jobs`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection):
          RoomOpenDelegate.ValidationResult {
        val _columnsJobs: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsJobs.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsJobs.put("role", TableInfo.Column("role", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsJobs.put("companyName", TableInfo.Column("companyName", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsJobs.put("location", TableInfo.Column("location", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsJobs.put("remote", TableInfo.Column("remote", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsJobs.put("url", TableInfo.Column("url", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsJobs.put("description", TableInfo.Column("description", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsJobs.put("datePosted", TableInfo.Column("datePosted", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsJobs.put("keywords", TableInfo.Column("keywords", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsJobs.put("source", TableInfo.Column("source", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsJobs.put("employmentType", TableInfo.Column("employmentType", "TEXT", false, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsJobs.put("logoUrl", TableInfo.Column("logoUrl", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsJobs.put("fetchedAt", TableInfo.Column("fetchedAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysJobs: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesJobs: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoJobs: TableInfo = TableInfo("jobs", _columnsJobs, _foreignKeysJobs, _indicesJobs)
        val _existingJobs: TableInfo = read(connection, "jobs")
        if (!_infoJobs.equals(_existingJobs)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |jobs(com.swipeapply.app.data.local.entity.JobEntity).
              | Expected:
              |""".trimMargin() + _infoJobs + """
              |
              | Found:
              |""".trimMargin() + _existingJobs)
        }
        val _columnsSwipedJobs: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsSwipedJobs.put("jobId", TableInfo.Column("jobId", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSwipedJobs.put("userId", TableInfo.Column("userId", "TEXT", true, 2, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSwipedJobs.put("direction", TableInfo.Column("direction", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSwipedJobs.put("timestamp", TableInfo.Column("timestamp", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysSwipedJobs: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesSwipedJobs: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoSwipedJobs: TableInfo = TableInfo("swiped_jobs", _columnsSwipedJobs,
            _foreignKeysSwipedJobs, _indicesSwipedJobs)
        val _existingSwipedJobs: TableInfo = read(connection, "swiped_jobs")
        if (!_infoSwipedJobs.equals(_existingSwipedJobs)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |swiped_jobs(com.swipeapply.app.data.local.entity.SwipedJobEntity).
              | Expected:
              |""".trimMargin() + _infoSwipedJobs + """
              |
              | Found:
              |""".trimMargin() + _existingSwipedJobs)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "jobs", "swiped_jobs")
  }

  public override fun clearAllTables() {
    super.performClear(false, "jobs", "swiped_jobs")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(JobDao::class, JobDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override
      fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>):
      List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun jobDao(): JobDao = _jobDao.value
}
