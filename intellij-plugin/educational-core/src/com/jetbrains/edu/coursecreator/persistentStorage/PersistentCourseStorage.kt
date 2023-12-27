package com.jetbrains.edu.coursecreator.persistentStorage

import com.intellij.openapi.Disposable
import com.jetbrains.edu.learning.courseFormat.BinaryContents
import com.jetbrains.edu.learning.courseFormat.TextualContents
import com.jetbrains.edu.learning.courseFormat.UndeterminedContents
import org.jetbrains.sqlite.ObjectBinder
import org.jetbrains.sqlite.SqliteConnection
import java.nio.file.Path

private const val AUTHOR_CONTENTS_TABLE = "Contents"

@Suppress("SqlNoDataSourceInspection", "SqlResolve")
class PersistentCourseStorage(val db: Path) : Disposable {

  private val connection = SqliteConnection(db)

  private val getAuthorContentsStatement = connection.statementPool(
    sql = "SELECT `value` FROM $AUTHOR_CONTENTS_TABLE WHERE `key`=?"
  ) { ObjectBinder(1) }

  private val putAuthorContentsStatement = connection.statementPool(
    sql = "INSERT OR REPLACE INTO `$AUTHOR_CONTENTS_TABLE`(`key`, `value`) VALUES (?, ?)"
  ) { ObjectBinder(2) }

  fun get(key: String): ByteArray = getAuthorContentsStatement.use { statement, binder ->
    binder.bind(key)
    val resultSet = statement.executeQuery()
    resultSet.next()
    resultSet.getBytes(0) ?: byteArrayOf()
  }

  fun put(key: String, value: ByteArray): Unit = putAuthorContentsStatement.use { statement, binder ->
    binder.bind(key, value)
    statement.executeUpdate()
  }

  fun put(key: String, value: String): Unit = put(key, value.byteInputStream().readAllBytes())

  private fun createDB() = connection.execute(
    """
    CREATE TABLE IF NOT EXISTS `$AUTHOR_CONTENTS_TABLE` (
      `key` TEXT PRIMARY KEY,
      `value` BLOB
    )
  """
  )

  override fun dispose() = connection.interruptAndClose()

  companion object {
    fun openOrCreateDB(db: Path): PersistentCourseStorage {
      val storage = PersistentCourseStorage(db)
      storage.createDB()
      return storage
    }
  }
}

interface ContentsFromCourseStorage {
  val storage: PersistentCourseStorage
  val path: String
}

class TextualContentsFromCourseStorage(
  override val storage: PersistentCourseStorage,
  override val path: String
) : TextualContents, ContentsFromCourseStorage {
  override val text: String
    get() = String(storage.get(path))
}

class BinaryContentsFromCourseStorage(
  override val storage: PersistentCourseStorage,
  override val path: String
) : BinaryContents, ContentsFromCourseStorage {
  override val bytes: ByteArray
    get() = storage.get(path)
}

class UndeterminedContentsFromCourseStorage(
  override val storage: PersistentCourseStorage,
  override val path: String
) : UndeterminedContents, ContentsFromCourseStorage {
  override val textualRepresentation: String
    get() = String(storage.get(path))
}