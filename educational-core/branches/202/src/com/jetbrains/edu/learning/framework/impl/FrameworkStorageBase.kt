package com.jetbrains.edu.learning.framework.impl

import com.intellij.util.io.PagePool
import com.intellij.util.io.storage.AbstractRecordsTable
import com.intellij.util.io.storage.AbstractStorage
import java.io.File
import java.nio.file.Path

@Suppress("UnstableApiUsage")
abstract class FrameworkStorageBase(storagePath: Path) : AbstractStorage(storagePath.toString(), true) {

  override fun createRecordsTable(pool: PagePool, recordsFile: File): AbstractRecordsTable {
    return FrameworkRecordsTable(recordsFile.toPath(), pool)
  }
}
