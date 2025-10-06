package com.jetbrains.edu.learning.framework.impl

import com.jetbrains.edu.learning.framework.impl.migration.RecordConverter
import com.jetbrains.edu.learning.framework.impl.migration.To1VersionRecordConverter
import com.jetbrains.edu.learning.framework.impl.migration.To2VersionRecordConverter
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

  override fun getRecordConverter(version: Int): RecordConverter? = when (version) {
    0 -> To1VersionRecordConverter()
    1 -> To2VersionRecordConverter()
    else -> null
  }
}

