package com.jetbrains.edu.learning.framework.impl.migration

import com.jetbrains.edu.learning.framework.impl.TextualContentChange
import java.io.DataInput
import java.io.DataOutput

class To2VersionRecordConverter : RecordConverter {
  override fun convert(input: DataInput, output: DataOutput) {
    val (oldChanges, timestamp) = UserChanges1.read(input)
    val newChanges = oldChanges.map { Change2(it.ordinal, it.path, TextualContentChange(it.text)) }
    val newUserChanges = UserChanges2(newChanges, timestamp)
    newUserChanges.write(output)
  }
}