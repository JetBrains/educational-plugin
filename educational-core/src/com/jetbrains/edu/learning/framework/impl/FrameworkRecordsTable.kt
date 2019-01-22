package com.jetbrains.edu.learning.framework.impl

import com.intellij.util.io.PagePool
import com.intellij.util.io.storage.AbstractRecordsTable
import java.io.File

class FrameworkRecordsTable(storageFilePath: File, pool: PagePool) : AbstractRecordsTable(storageFilePath, pool) {

  override fun getImplVersion(): Int = 1

  override fun getZeros(): ByteArray = ZEROS
  override fun getRecordSize(): Int = RECORD_SIZE

  companion object {
    private const val RECORD_SIZE = DEFAULT_RECORD_SIZE

    private val ZEROS = ByteArray(RECORD_SIZE)
  }
}
