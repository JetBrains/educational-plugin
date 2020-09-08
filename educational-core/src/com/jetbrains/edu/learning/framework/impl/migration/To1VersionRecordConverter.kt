package com.jetbrains.edu.learning.framework.impl.migration

import java.io.DataInput
import java.io.DataOutput

class To1VersionRecordConverter : RecordConverter {

  override fun convert(input: DataInput, output: DataOutput) {
    val oldChanges = UserChanges0.read(input)
    val newChanges = UserChanges1(oldChanges.changes, TIMESTAMP)
    newChanges.write(output)
  }

  companion object {
    private const val TIMESTAMP: Long = -1
  }
}
