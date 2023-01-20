package com.jetbrains.edu.learning.framework.impl

import com.intellij.util.io.StorageLockContext
import com.intellij.util.io.storage.AbstractRecordsTable
import java.nio.file.Path

@Suppress("UnstableApiUsage")
class FrameworkRecordsTable(storageFilePath: Path, context: StorageLockContext) : AbstractRecordsTable(storageFilePath, context) {

  override fun getImplVersion(): Int = 1

  override fun getZeros(): ByteArray = ZEROS
  override fun getRecordSize(): Int = RECORD_SIZE

  companion object {
    private const val RECORD_SIZE = DEFAULT_RECORD_SIZE

    private val ZEROS = ByteArray(RECORD_SIZE)
  }
}
