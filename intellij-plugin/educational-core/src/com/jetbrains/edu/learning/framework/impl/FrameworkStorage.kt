package com.jetbrains.edu.learning.framework.impl

import com.intellij.util.io.UnsyncByteArrayInputStream
import com.intellij.util.io.UnsyncByteArrayOutputStream
import com.jetbrains.edu.learning.framework.impl.migration.RecordConverter
import com.jetbrains.edu.learning.framework.impl.migration.To1VersionRecordConverter
import com.jetbrains.edu.learning.framework.impl.migration.To2VersionRecordConverter
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.nio.file.Path

class FrameworkStorage(storagePath: Path) : FrameworkStorageBase(storagePath) {
  constructor(storageFilePath: Path, version: Int) : this(storageFilePath) {
    setVersion(version)
  }

  @Throws(IOException::class)
  fun updateUserChanges(record: Int, changes: UserChanges): Int {
    return withWriteLock<Int, IOException> {
      val id = if (record == -1) createNewRecord() else record
      writeStream(id, true).use(changes::write)
      id
    }
  }

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
  override fun migrateRecord(recordId: Int, currentVersion: Int, newVersion: Int) {
    var version = currentVersion

    val bytes = readBytes(recordId)
    var input = UnsyncByteArrayInputStream(bytes)
    var output = UnsyncByteArrayOutputStream()

    while (version < newVersion) {
      val converter: RecordConverter? = when (version) {
        0 -> To1VersionRecordConverter()
        1 -> To2VersionRecordConverter()
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

