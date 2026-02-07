package com.swipeapply.app.`data`.local.dao

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.swipeapply.app.`data`.local.entity.JobEntity
import com.swipeapply.app.`data`.local.entity.SwipedJobEntity
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class JobDao_Impl(
  __db: RoomDatabase,
) : JobDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfJobEntity: EntityInsertAdapter<JobEntity>

  private val __insertAdapterOfSwipedJobEntity: EntityInsertAdapter<SwipedJobEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfJobEntity = object : EntityInsertAdapter<JobEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `jobs` (`id`,`role`,`companyName`,`location`,`remote`,`url`,`description`,`datePosted`,`keywords`,`source`,`employmentType`,`logoUrl`,`fetchedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: JobEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.role)
        statement.bindText(3, entity.companyName)
        statement.bindText(4, entity.location)
        val _tmp: Int = if (entity.remote) 1 else 0
        statement.bindLong(5, _tmp.toLong())
        val _tmpUrl: String? = entity.url
        if (_tmpUrl == null) {
          statement.bindNull(6)
        } else {
          statement.bindText(6, _tmpUrl)
        }
        val _tmpDescription: String? = entity.description
        if (_tmpDescription == null) {
          statement.bindNull(7)
        } else {
          statement.bindText(7, _tmpDescription)
        }
        val _tmpDatePosted: String? = entity.datePosted
        if (_tmpDatePosted == null) {
          statement.bindNull(8)
        } else {
          statement.bindText(8, _tmpDatePosted)
        }
        val _tmpKeywords: String? = entity.keywords
        if (_tmpKeywords == null) {
          statement.bindNull(9)
        } else {
          statement.bindText(9, _tmpKeywords)
        }
        val _tmpSource: String? = entity.source
        if (_tmpSource == null) {
          statement.bindNull(10)
        } else {
          statement.bindText(10, _tmpSource)
        }
        val _tmpEmploymentType: String? = entity.employmentType
        if (_tmpEmploymentType == null) {
          statement.bindNull(11)
        } else {
          statement.bindText(11, _tmpEmploymentType)
        }
        val _tmpLogoUrl: String? = entity.logoUrl
        if (_tmpLogoUrl == null) {
          statement.bindNull(12)
        } else {
          statement.bindText(12, _tmpLogoUrl)
        }
        statement.bindLong(13, entity.fetchedAt)
      }
    }
    this.__insertAdapterOfSwipedJobEntity = object : EntityInsertAdapter<SwipedJobEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `swiped_jobs` (`jobId`,`userId`,`direction`,`timestamp`) VALUES (?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: SwipedJobEntity) {
        statement.bindText(1, entity.jobId)
        statement.bindText(2, entity.userId)
        statement.bindText(3, entity.direction)
        statement.bindLong(4, entity.timestamp)
      }
    }
  }

  public override suspend fun insertJobs(jobs: List<JobEntity>): Unit = performSuspending(__db,
      false, true) { _connection ->
    __insertAdapterOfJobEntity.insert(_connection, jobs)
  }

  public override suspend fun insertJob(job: JobEntity): Unit = performSuspending(__db, false, true)
      { _connection ->
    __insertAdapterOfJobEntity.insert(_connection, job)
  }

  public override suspend fun insertSwipedJob(swipedJob: SwipedJobEntity): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfSwipedJobEntity.insert(_connection, swipedJob)
  }

  public override fun getAllJobs(): Flow<List<JobEntity>> {
    val _sql: String = "SELECT * FROM jobs ORDER BY fetchedAt DESC"
    return createFlow(__db, false, arrayOf("jobs")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _cursorIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _cursorIndexOfRole: Int = getColumnIndexOrThrow(_stmt, "role")
        val _cursorIndexOfCompanyName: Int = getColumnIndexOrThrow(_stmt, "companyName")
        val _cursorIndexOfLocation: Int = getColumnIndexOrThrow(_stmt, "location")
        val _cursorIndexOfRemote: Int = getColumnIndexOrThrow(_stmt, "remote")
        val _cursorIndexOfUrl: Int = getColumnIndexOrThrow(_stmt, "url")
        val _cursorIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _cursorIndexOfDatePosted: Int = getColumnIndexOrThrow(_stmt, "datePosted")
        val _cursorIndexOfKeywords: Int = getColumnIndexOrThrow(_stmt, "keywords")
        val _cursorIndexOfSource: Int = getColumnIndexOrThrow(_stmt, "source")
        val _cursorIndexOfEmploymentType: Int = getColumnIndexOrThrow(_stmt, "employmentType")
        val _cursorIndexOfLogoUrl: Int = getColumnIndexOrThrow(_stmt, "logoUrl")
        val _cursorIndexOfFetchedAt: Int = getColumnIndexOrThrow(_stmt, "fetchedAt")
        val _result: MutableList<JobEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: JobEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_cursorIndexOfId)
          val _tmpRole: String
          _tmpRole = _stmt.getText(_cursorIndexOfRole)
          val _tmpCompanyName: String
          _tmpCompanyName = _stmt.getText(_cursorIndexOfCompanyName)
          val _tmpLocation: String
          _tmpLocation = _stmt.getText(_cursorIndexOfLocation)
          val _tmpRemote: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_cursorIndexOfRemote).toInt()
          _tmpRemote = _tmp != 0
          val _tmpUrl: String?
          if (_stmt.isNull(_cursorIndexOfUrl)) {
            _tmpUrl = null
          } else {
            _tmpUrl = _stmt.getText(_cursorIndexOfUrl)
          }
          val _tmpDescription: String?
          if (_stmt.isNull(_cursorIndexOfDescription)) {
            _tmpDescription = null
          } else {
            _tmpDescription = _stmt.getText(_cursorIndexOfDescription)
          }
          val _tmpDatePosted: String?
          if (_stmt.isNull(_cursorIndexOfDatePosted)) {
            _tmpDatePosted = null
          } else {
            _tmpDatePosted = _stmt.getText(_cursorIndexOfDatePosted)
          }
          val _tmpKeywords: String?
          if (_stmt.isNull(_cursorIndexOfKeywords)) {
            _tmpKeywords = null
          } else {
            _tmpKeywords = _stmt.getText(_cursorIndexOfKeywords)
          }
          val _tmpSource: String?
          if (_stmt.isNull(_cursorIndexOfSource)) {
            _tmpSource = null
          } else {
            _tmpSource = _stmt.getText(_cursorIndexOfSource)
          }
          val _tmpEmploymentType: String?
          if (_stmt.isNull(_cursorIndexOfEmploymentType)) {
            _tmpEmploymentType = null
          } else {
            _tmpEmploymentType = _stmt.getText(_cursorIndexOfEmploymentType)
          }
          val _tmpLogoUrl: String?
          if (_stmt.isNull(_cursorIndexOfLogoUrl)) {
            _tmpLogoUrl = null
          } else {
            _tmpLogoUrl = _stmt.getText(_cursorIndexOfLogoUrl)
          }
          val _tmpFetchedAt: Long
          _tmpFetchedAt = _stmt.getLong(_cursorIndexOfFetchedAt)
          _item =
              JobEntity(_tmpId,_tmpRole,_tmpCompanyName,_tmpLocation,_tmpRemote,_tmpUrl,_tmpDescription,_tmpDatePosted,_tmpKeywords,_tmpSource,_tmpEmploymentType,_tmpLogoUrl,_tmpFetchedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getJobById(jobId: String): JobEntity? {
    val _sql: String = "SELECT * FROM jobs WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, jobId)
        val _cursorIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _cursorIndexOfRole: Int = getColumnIndexOrThrow(_stmt, "role")
        val _cursorIndexOfCompanyName: Int = getColumnIndexOrThrow(_stmt, "companyName")
        val _cursorIndexOfLocation: Int = getColumnIndexOrThrow(_stmt, "location")
        val _cursorIndexOfRemote: Int = getColumnIndexOrThrow(_stmt, "remote")
        val _cursorIndexOfUrl: Int = getColumnIndexOrThrow(_stmt, "url")
        val _cursorIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _cursorIndexOfDatePosted: Int = getColumnIndexOrThrow(_stmt, "datePosted")
        val _cursorIndexOfKeywords: Int = getColumnIndexOrThrow(_stmt, "keywords")
        val _cursorIndexOfSource: Int = getColumnIndexOrThrow(_stmt, "source")
        val _cursorIndexOfEmploymentType: Int = getColumnIndexOrThrow(_stmt, "employmentType")
        val _cursorIndexOfLogoUrl: Int = getColumnIndexOrThrow(_stmt, "logoUrl")
        val _cursorIndexOfFetchedAt: Int = getColumnIndexOrThrow(_stmt, "fetchedAt")
        val _result: JobEntity?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_cursorIndexOfId)
          val _tmpRole: String
          _tmpRole = _stmt.getText(_cursorIndexOfRole)
          val _tmpCompanyName: String
          _tmpCompanyName = _stmt.getText(_cursorIndexOfCompanyName)
          val _tmpLocation: String
          _tmpLocation = _stmt.getText(_cursorIndexOfLocation)
          val _tmpRemote: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_cursorIndexOfRemote).toInt()
          _tmpRemote = _tmp != 0
          val _tmpUrl: String?
          if (_stmt.isNull(_cursorIndexOfUrl)) {
            _tmpUrl = null
          } else {
            _tmpUrl = _stmt.getText(_cursorIndexOfUrl)
          }
          val _tmpDescription: String?
          if (_stmt.isNull(_cursorIndexOfDescription)) {
            _tmpDescription = null
          } else {
            _tmpDescription = _stmt.getText(_cursorIndexOfDescription)
          }
          val _tmpDatePosted: String?
          if (_stmt.isNull(_cursorIndexOfDatePosted)) {
            _tmpDatePosted = null
          } else {
            _tmpDatePosted = _stmt.getText(_cursorIndexOfDatePosted)
          }
          val _tmpKeywords: String?
          if (_stmt.isNull(_cursorIndexOfKeywords)) {
            _tmpKeywords = null
          } else {
            _tmpKeywords = _stmt.getText(_cursorIndexOfKeywords)
          }
          val _tmpSource: String?
          if (_stmt.isNull(_cursorIndexOfSource)) {
            _tmpSource = null
          } else {
            _tmpSource = _stmt.getText(_cursorIndexOfSource)
          }
          val _tmpEmploymentType: String?
          if (_stmt.isNull(_cursorIndexOfEmploymentType)) {
            _tmpEmploymentType = null
          } else {
            _tmpEmploymentType = _stmt.getText(_cursorIndexOfEmploymentType)
          }
          val _tmpLogoUrl: String?
          if (_stmt.isNull(_cursorIndexOfLogoUrl)) {
            _tmpLogoUrl = null
          } else {
            _tmpLogoUrl = _stmt.getText(_cursorIndexOfLogoUrl)
          }
          val _tmpFetchedAt: Long
          _tmpFetchedAt = _stmt.getLong(_cursorIndexOfFetchedAt)
          _result =
              JobEntity(_tmpId,_tmpRole,_tmpCompanyName,_tmpLocation,_tmpRemote,_tmpUrl,_tmpDescription,_tmpDatePosted,_tmpKeywords,_tmpSource,_tmpEmploymentType,_tmpLogoUrl,_tmpFetchedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getJobCount(): Int {
    val _sql: String = "SELECT COUNT(*) FROM jobs"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getSwipedJobIdsByUser(userId: String): List<String> {
    val _sql: String = "SELECT jobId FROM swiped_jobs WHERE userId = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, userId)
        val _result: MutableList<String> = mutableListOf()
        while (_stmt.step()) {
          val _item: String
          _item = _stmt.getText(0)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAllSwipedJobIds(): List<String> {
    val _sql: String = "SELECT DISTINCT jobId FROM swiped_jobs"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: MutableList<String> = mutableListOf()
        while (_stmt.step()) {
          val _item: String
          _item = _stmt.getText(0)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAvailableJobsForUser(userId: String): List<JobEntity> {
    val _sql: String =
        "SELECT * FROM jobs WHERE id NOT IN (SELECT jobId FROM swiped_jobs WHERE userId = ?) ORDER BY fetchedAt DESC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, userId)
        val _cursorIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _cursorIndexOfRole: Int = getColumnIndexOrThrow(_stmt, "role")
        val _cursorIndexOfCompanyName: Int = getColumnIndexOrThrow(_stmt, "companyName")
        val _cursorIndexOfLocation: Int = getColumnIndexOrThrow(_stmt, "location")
        val _cursorIndexOfRemote: Int = getColumnIndexOrThrow(_stmt, "remote")
        val _cursorIndexOfUrl: Int = getColumnIndexOrThrow(_stmt, "url")
        val _cursorIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _cursorIndexOfDatePosted: Int = getColumnIndexOrThrow(_stmt, "datePosted")
        val _cursorIndexOfKeywords: Int = getColumnIndexOrThrow(_stmt, "keywords")
        val _cursorIndexOfSource: Int = getColumnIndexOrThrow(_stmt, "source")
        val _cursorIndexOfEmploymentType: Int = getColumnIndexOrThrow(_stmt, "employmentType")
        val _cursorIndexOfLogoUrl: Int = getColumnIndexOrThrow(_stmt, "logoUrl")
        val _cursorIndexOfFetchedAt: Int = getColumnIndexOrThrow(_stmt, "fetchedAt")
        val _result: MutableList<JobEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: JobEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_cursorIndexOfId)
          val _tmpRole: String
          _tmpRole = _stmt.getText(_cursorIndexOfRole)
          val _tmpCompanyName: String
          _tmpCompanyName = _stmt.getText(_cursorIndexOfCompanyName)
          val _tmpLocation: String
          _tmpLocation = _stmt.getText(_cursorIndexOfLocation)
          val _tmpRemote: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_cursorIndexOfRemote).toInt()
          _tmpRemote = _tmp != 0
          val _tmpUrl: String?
          if (_stmt.isNull(_cursorIndexOfUrl)) {
            _tmpUrl = null
          } else {
            _tmpUrl = _stmt.getText(_cursorIndexOfUrl)
          }
          val _tmpDescription: String?
          if (_stmt.isNull(_cursorIndexOfDescription)) {
            _tmpDescription = null
          } else {
            _tmpDescription = _stmt.getText(_cursorIndexOfDescription)
          }
          val _tmpDatePosted: String?
          if (_stmt.isNull(_cursorIndexOfDatePosted)) {
            _tmpDatePosted = null
          } else {
            _tmpDatePosted = _stmt.getText(_cursorIndexOfDatePosted)
          }
          val _tmpKeywords: String?
          if (_stmt.isNull(_cursorIndexOfKeywords)) {
            _tmpKeywords = null
          } else {
            _tmpKeywords = _stmt.getText(_cursorIndexOfKeywords)
          }
          val _tmpSource: String?
          if (_stmt.isNull(_cursorIndexOfSource)) {
            _tmpSource = null
          } else {
            _tmpSource = _stmt.getText(_cursorIndexOfSource)
          }
          val _tmpEmploymentType: String?
          if (_stmt.isNull(_cursorIndexOfEmploymentType)) {
            _tmpEmploymentType = null
          } else {
            _tmpEmploymentType = _stmt.getText(_cursorIndexOfEmploymentType)
          }
          val _tmpLogoUrl: String?
          if (_stmt.isNull(_cursorIndexOfLogoUrl)) {
            _tmpLogoUrl = null
          } else {
            _tmpLogoUrl = _stmt.getText(_cursorIndexOfLogoUrl)
          }
          val _tmpFetchedAt: Long
          _tmpFetchedAt = _stmt.getLong(_cursorIndexOfFetchedAt)
          _item =
              JobEntity(_tmpId,_tmpRole,_tmpCompanyName,_tmpLocation,_tmpRemote,_tmpUrl,_tmpDescription,_tmpDatePosted,_tmpKeywords,_tmpSource,_tmpEmploymentType,_tmpLogoUrl,_tmpFetchedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAvailableJobs(): List<JobEntity> {
    val _sql: String =
        "SELECT * FROM jobs WHERE id NOT IN (SELECT jobId FROM swiped_jobs) ORDER BY fetchedAt DESC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _cursorIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _cursorIndexOfRole: Int = getColumnIndexOrThrow(_stmt, "role")
        val _cursorIndexOfCompanyName: Int = getColumnIndexOrThrow(_stmt, "companyName")
        val _cursorIndexOfLocation: Int = getColumnIndexOrThrow(_stmt, "location")
        val _cursorIndexOfRemote: Int = getColumnIndexOrThrow(_stmt, "remote")
        val _cursorIndexOfUrl: Int = getColumnIndexOrThrow(_stmt, "url")
        val _cursorIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _cursorIndexOfDatePosted: Int = getColumnIndexOrThrow(_stmt, "datePosted")
        val _cursorIndexOfKeywords: Int = getColumnIndexOrThrow(_stmt, "keywords")
        val _cursorIndexOfSource: Int = getColumnIndexOrThrow(_stmt, "source")
        val _cursorIndexOfEmploymentType: Int = getColumnIndexOrThrow(_stmt, "employmentType")
        val _cursorIndexOfLogoUrl: Int = getColumnIndexOrThrow(_stmt, "logoUrl")
        val _cursorIndexOfFetchedAt: Int = getColumnIndexOrThrow(_stmt, "fetchedAt")
        val _result: MutableList<JobEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: JobEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_cursorIndexOfId)
          val _tmpRole: String
          _tmpRole = _stmt.getText(_cursorIndexOfRole)
          val _tmpCompanyName: String
          _tmpCompanyName = _stmt.getText(_cursorIndexOfCompanyName)
          val _tmpLocation: String
          _tmpLocation = _stmt.getText(_cursorIndexOfLocation)
          val _tmpRemote: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_cursorIndexOfRemote).toInt()
          _tmpRemote = _tmp != 0
          val _tmpUrl: String?
          if (_stmt.isNull(_cursorIndexOfUrl)) {
            _tmpUrl = null
          } else {
            _tmpUrl = _stmt.getText(_cursorIndexOfUrl)
          }
          val _tmpDescription: String?
          if (_stmt.isNull(_cursorIndexOfDescription)) {
            _tmpDescription = null
          } else {
            _tmpDescription = _stmt.getText(_cursorIndexOfDescription)
          }
          val _tmpDatePosted: String?
          if (_stmt.isNull(_cursorIndexOfDatePosted)) {
            _tmpDatePosted = null
          } else {
            _tmpDatePosted = _stmt.getText(_cursorIndexOfDatePosted)
          }
          val _tmpKeywords: String?
          if (_stmt.isNull(_cursorIndexOfKeywords)) {
            _tmpKeywords = null
          } else {
            _tmpKeywords = _stmt.getText(_cursorIndexOfKeywords)
          }
          val _tmpSource: String?
          if (_stmt.isNull(_cursorIndexOfSource)) {
            _tmpSource = null
          } else {
            _tmpSource = _stmt.getText(_cursorIndexOfSource)
          }
          val _tmpEmploymentType: String?
          if (_stmt.isNull(_cursorIndexOfEmploymentType)) {
            _tmpEmploymentType = null
          } else {
            _tmpEmploymentType = _stmt.getText(_cursorIndexOfEmploymentType)
          }
          val _tmpLogoUrl: String?
          if (_stmt.isNull(_cursorIndexOfLogoUrl)) {
            _tmpLogoUrl = null
          } else {
            _tmpLogoUrl = _stmt.getText(_cursorIndexOfLogoUrl)
          }
          val _tmpFetchedAt: Long
          _tmpFetchedAt = _stmt.getLong(_cursorIndexOfFetchedAt)
          _item =
              JobEntity(_tmpId,_tmpRole,_tmpCompanyName,_tmpLocation,_tmpRemote,_tmpUrl,_tmpDescription,_tmpDatePosted,_tmpKeywords,_tmpSource,_tmpEmploymentType,_tmpLogoUrl,_tmpFetchedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAllJobs() {
    val _sql: String = "DELETE FROM jobs"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteOldJobs(timestamp: Long) {
    val _sql: String = "DELETE FROM jobs WHERE fetchedAt < ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, timestamp)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteSwipedJobsByUser(userId: String) {
    val _sql: String = "DELETE FROM swiped_jobs WHERE userId = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, userId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAllSwipedJobs() {
    val _sql: String = "DELETE FROM swiped_jobs"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
