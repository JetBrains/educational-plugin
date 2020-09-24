package com.jetbrains.edu.learning.framework.impl

import com.intellij.openapi.util.ThrowableComputable
import com.intellij.util.io.PagePool
import com.intellij.util.io.storage.AbstractRecordsTable
import com.intellij.util.io.storage.AbstractStorage
import java.io.File
import java.nio.file.Path

@Suppress("UnstableApiUsage", "DEPRECATION")
abstract class FrameworkStorageBase(storagePath: Path) : AbstractStorage(storagePath.toString()) {

  override fun createRecordsTable(pool: PagePool, recordsFile: File): AbstractRecordsTable {
    return FrameworkRecordsTable(recordsFile.toPath(), pool)
  }

  protected fun <T, E : Throwable> withWriteLock(runnable: ThrowableComputable<T, E>): T {
    return synchronized(myLock) { runnable.compute() }
  }

  protected fun <T, E : Throwable> withReadLock(runnable: ThrowableComputable<T, E>): T {
    return synchronized(myLock) { runnable.compute() }
  }
}
