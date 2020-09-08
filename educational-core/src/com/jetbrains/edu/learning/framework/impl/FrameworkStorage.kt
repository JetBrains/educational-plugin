package com.jetbrains.edu.learning.framework.impl

import com.intellij.util.io.PagePool
import com.intellij.util.io.UnsyncByteArrayInputStream
import com.intellij.util.io.UnsyncByteArrayOutputStream
import com.intellij.util.io.storage.AbstractRecordsTable
import com.intellij.util.io.storage.AbstractStorage
import com.jetbrains.edu.learning.framework.impl.migration.RecordConverter
import java.io.*

class FrameworkStorage(storageFilePath: String) : AbstractStorage(storageFilePath) {

  override fun createRecordsTable(pool: PagePool, recordsFile: File): AbstractRecordsTable =
    FrameworkRecordsTable(recordsFile, pool)

  @Throws(IOException::class)
  fun updateUserChanges(record: Int, changes: UserChanges): Int {
    return synchronized(myLock) {
      val id = if (record == -1) {
        myRecordsTable.createNewRecord()
      } else {
        record
      }
      writeStream(id, true).use(changes::write)
      id
    }
  }

  @Throws(IOException::class)
  fun getUserChanges(record: Int): UserChanges {
    return if (record == -1) {
      UserChanges.empty()
    } else {
      synchronized(myLock) {
        readStream(record).use(UserChanges.Companion::read)
      }
    }
  }

  @Throws(IOException::class)
  fun migrate(newVersion: Int) {
    synchronized(myLock) {
      val currentVersion = version
      if (currentVersion >= newVersion) return

      val recordIterator = myRecordsTable.createRecordIdIterator()
      while (recordIterator.hasNextId()) {
        val recordId = recordIterator.nextId()
        migrateRecord(recordId, currentVersion, newVersion)
      }

      version = newVersion
    }
  }

  @Throws(IOException::class)
  private fun migrateRecord(recordId: Int, currentVersion: Int, newVersion: Int) {
    var version = currentVersion

    val bytes = readBytes(recordId)
    var input = UnsyncByteArrayInputStream(bytes)
    var output = UnsyncByteArrayOutputStream()

    while (version < newVersion) {
      val converter: RecordConverter? = when (currentVersion) {
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

