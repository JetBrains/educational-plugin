package com.jetbrains.edu.coursecreator.framework.storage

import com.jetbrains.edu.coursecreator.framework.storage.migration.To1VersionRecordConverter
import com.jetbrains.edu.learning.framework.impl.FLTaskState
import com.jetbrains.edu.learning.framework.impl.FrameworkStorageBase
import com.jetbrains.edu.learning.framework.impl.migration.RecordConverter
import java.io.IOException
import java.nio.file.Path

class CCFrameworkStorage(storagePath: Path) : FrameworkStorageBase(storagePath) {
  constructor(storageFilePath: Path, version: Int) : this(storageFilePath) {
    setVersion(version)
  }

  @Throws(IOException::class)
  fun updateState(record: Int, state: FLTaskState): Int {
    val userChanges = CCUserChanges(state)
    return withWriteLock<Int, IOException> {
      val id = if (record == -1) createNewRecord() else record
      writeStream(id, true).use(userChanges::write)
      id
    }
  }

  @Throws(IOException::class)
  fun getState(record: Int): CCUserChanges {
    return if (record == -1) {
      CCUserChanges.EMPTY
    }
    else {
      withReadLock<CCUserChanges, IOException> {
        readStream(record).use(CCUserChanges.Companion::read)
      }
    }
  }

  override fun getRecordConverter(version: Int): RecordConverter? = when (version) {
    0 -> To1VersionRecordConverter()
    else -> null
  }
}