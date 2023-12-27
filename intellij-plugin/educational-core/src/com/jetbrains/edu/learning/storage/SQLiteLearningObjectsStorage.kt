package com.jetbrains.edu.learning.storage

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.project.stateStore
import org.jetbrains.sqlite.ObjectBinder
import org.jetbrains.sqlite.SqliteConnection
import java.nio.file.Path

private const val AUTHOR_CONTENTS_TABLE = "Contents"

@Suppress("SqlNoDataSourceInspection", "SqlResolve")
class SQLiteLearningObjectsStorage(val db: Path) : LearningObjectsStorage, Disposable {

  private val connection = SqliteConnection(db)

  private val getAuthorContentsStatement = connection.statementPool(
    sql = "SELECT `value` FROM $AUTHOR_CONTENTS_TABLE WHERE `key`=?"
  ) { ObjectBinder(1) }

  private val putAuthorContentsStatement = connection.statementPool(
    sql = "INSERT OR REPLACE INTO `$AUTHOR_CONTENTS_TABLE`(`key`, `value`) VALUES (?, ?)"
  ) { ObjectBinder(2) }

  override val writeTextInYaml: Boolean = false

  override fun load(key: String): ByteArray = getAuthorContentsStatement.use { statement, binder ->
    binder.bind(key)
    val resultSet = statement.executeQuery()
    resultSet.next()
    resultSet.getBytes(0) ?: byteArrayOf()
  }

  //TODO we need to encrypt stored data: EDU-6744 Encrypt data in the learning objects storage
  override fun store(key: String, value: ByteArray) = putAuthorContentsStatement.use { statement, binder ->
    binder.bind(key, value)
    statement.executeUpdate()
  }

  private fun createDB() = connection.execute("""
    CREATE TABLE IF NOT EXISTS `$AUTHOR_CONTENTS_TABLE` (
      `key` TEXT PRIMARY KEY,
      `value` BLOB
    )
  """)

  override fun dispose() = connection.interruptAndClose()

  companion object {
    private fun openOrCreateDB(db: Path): LearningObjectsStorage {
      val storage = SQLiteLearningObjectsStorage(db)
      storage.createDB()
      return storage
    }

    private const val COURSE_AUTHOR_CONTENTS_FILE = ".author_contents_storage_db"

    fun openOrCreateDB(project: Project): LearningObjectsStorage {
      val ideaPath = project.stateStore.directoryStorePath
      val sqlFilePath =  ideaPath?.resolve(COURSE_AUTHOR_CONTENTS_FILE)
      if (sqlFilePath == null) {
        logger<LearningObjectsStorageManager>().error("Failed to get path for a SQLite file for the learning objects storage. The created storage is not persistent")
        return InMemoryLearningObjectsStorage()
      }
      return openOrCreateDB(sqlFilePath)
    }
  }
}