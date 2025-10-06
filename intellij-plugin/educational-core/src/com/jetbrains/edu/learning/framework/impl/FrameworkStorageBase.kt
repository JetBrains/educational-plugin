package com.jetbrains.edu.learning.framework.impl

import com.intellij.util.io.StorageLockContext
import com.intellij.util.io.UnsyncByteArrayInputStream
import com.intellij.util.io.UnsyncByteArrayOutputStream
import com.intellij.util.io.storage.AbstractRecordsTable
import com.intellij.util.io.storage.AbstractStorage
import com.jetbrains.edu.learning.framework.impl.migration.RecordConverter
import org.jetbrains.annotations.TestOnly
import org.jetbrains.annotations.VisibleForTesting
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.nio.file.Path
import kotlin.system.measureTimeMillis

abstract class FrameworkStorageBase(storagePath: Path) : AbstractStorage(storagePath) {
  @Volatile
  var isDisposed = false

  override fun createRecordsTable(@Suppress("UnstableApiUsage") context: StorageLockContext, recordsFile: Path): AbstractRecordsTable =
    FrameworkRecordsTable(recordsFile, context)

  override fun dispose() {
    super.dispose()
    isDisposed = true
  }

  @VisibleForTesting
  @Throws(IOException::class)
  fun createNewRecord(): Int = myRecordsTable.createNewRecord()

  @Throws(IOException::class)
  fun migrate(newVersion: Int) {
    val migrationTime = measureTimeMillis {
      withWriteLock<Unit, IOException> {
        val currentVersion = version
        if (currentVersion >= newVersion) return@withWriteLock

        val recordIterator = myRecordsTable.createRecordIdIterator()
        while (recordIterator.hasNextId()) {
          val recordId = recordIterator.nextId()
          migrateRecord(recordId, currentVersion, newVersion)
        }

        version = newVersion
      }
    }
    LOG.info("Migration to $newVersion version took $migrationTime ms")
  }

  protected abstract fun getRecordConverter(version: Int): RecordConverter?

  @Throws(IOException::class)
  private fun migrateRecord(recordId: Int, currentVersion: Int, newVersion: Int) {
    var version = currentVersion

    val bytes = readBytes(recordId)
    var input = UnsyncByteArrayInputStream(bytes)
    var output = UnsyncByteArrayOutputStream()

    while (version < newVersion) {
      val converter: RecordConverter? = getRecordConverter(version)

      if (converter != null) {
        output = UnsyncByteArrayOutputStream()
        converter.convert(DataInputStream(input), DataOutputStream(output))
        input = UnsyncByteArrayInputStream(output.toByteArray())
      }
      version++
    }

    writeBytes(recordId, output.toByteArraySequence(), false)
  }

  @TestOnly
  @Throws(IOException::class)
  fun createRecordWithData(data: FrameworkStorageData): Int {
    return withWriteLock<Int, IOException> {
      val record = createNewRecord()
      writeStream(record).use(data::write)
      record
    }
  }
}