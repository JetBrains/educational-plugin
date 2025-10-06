package com.jetbrains.edu.coursecreator.framework.storage.migration

import com.jetbrains.edu.coursecreator.framework.storage.CCUserChanges
import com.jetbrains.edu.learning.framework.impl.TextualContentChange
import com.jetbrains.edu.learning.framework.impl.migration.RecordConverter
import java.io.DataInput
import java.io.DataOutput

class To1VersionRecordConverter : RecordConverter {
  override fun convert(input: DataInput, output: DataOutput) {
    val oldState = CCUserChanges0.read(input).state
    val newState = oldState.mapValues { TextualContentChange(it.value) }
    val newUserChanges = CCUserChanges(newState)
    newUserChanges.write(output)
  }
}