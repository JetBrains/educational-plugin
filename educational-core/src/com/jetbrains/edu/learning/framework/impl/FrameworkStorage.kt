package com.jetbrains.edu.learning.framework.impl

import com.google.common.annotations.VisibleForTesting
import com.intellij.util.io.UnsyncByteArrayInputStream
import com.intellij.util.io.UnsyncByteArrayOutputStream
import com.intellij.util.io.storage.AbstractRecordsTable
import com.jetbrains.edu.learning.framework.impl.migration.RecordConverter
import com.jetbrains.edu.learning.framework.impl.migration.To1VersionRecordConverter
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.nio.file.Path
import kotlin.system.measureTimeMillis

class FrameworkStorage(storagePath: Path) : FrameworkStorageBase(storagePath) {

  constructor(storageFilePath: Path, version: Int) : this(storageFilePath) {
    setVersion(version)
  }

  override fun createRecordsTable(@Suppress("UnstableApiUsage") context: StorageContext, recordsFile: Path): AbstractRecordsTable =
    FrameworkRecordsTable(recordsFile, context)

  @Throws(IOException::class)
  fun updateUserChanges(record: Int, changes: UserChanges): Int {
    return withWriteLock<Int, IOException> {
      val id = if (record == -1) createNewRecord() else record
      writeStream(id, true).use(changes::write)
      id
    }
  }

  @VisibleForTesting
  @Throws(IOException::class)
  fun createNewRecord(): Int = myRecordsTable.createNewRecord()

  @Throws(IOException::class)
  fun getUserChanges(record: Int): UserChanges {
    return if (record == -1) {
      UserChanges.empty()
    }
    else {
      withReadLock<UserChanges, IOException> {
        readStream(record).use(UserChanges.Companion::read)
      }
    }
  }

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

  @Throws(IOException::class)
  private fun migrateRecord(recordId: Int, currentVersion: Int, newVersion: Int) {
    var version = currentVersion

    val bytes = readBytes(recordId)
    var input = UnsyncByteArrayInputStream(bytes)
    var output = UnsyncByteArrayOutputStream()

    while (version < newVersion) {
      val converter: RecordConverter? = when (currentVersion) {
        0 -> To1VersionRecordConverter()
        else -> null
      }

      if (converter != null) {
        output = UnsyncByteArrayOutputStream()
        converter.convert(DataInputStream(input), DataOutputStream(output))
        input = UnsyncByteArrayInputStream(output.toByteArray())
      }
      version++
    }

    writeBytes(recordId, output.toByteArraySequence(), false)
  }
}

