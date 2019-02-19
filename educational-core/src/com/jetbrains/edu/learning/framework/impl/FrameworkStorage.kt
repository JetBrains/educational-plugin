package com.jetbrains.edu.learning.framework.impl

import com.intellij.util.io.PagePool
import com.intellij.util.io.storage.AbstractRecordsTable
import com.intellij.util.io.storage.AbstractStorage
import java.io.File
import java.io.IOException

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
}
