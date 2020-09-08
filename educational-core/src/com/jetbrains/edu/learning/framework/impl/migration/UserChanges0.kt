package com.jetbrains.edu.learning.framework.impl.migration

import com.intellij.util.io.DataInputOutputUtil
import com.jetbrains.edu.learning.framework.impl.Change
import com.jetbrains.edu.learning.framework.impl.FrameworkStorageData
import java.io.DataInput
import java.io.DataOutput
import java.io.IOException

data class UserChanges0(val changes: List<Change>) : FrameworkStorageData {

  @Throws(IOException::class)
  override fun write(out: DataOutput) {
    DataInputOutputUtil.writeINT(out, changes.size)
    changes.forEach { Change.writeChange(it, out) }
  }

  companion object {

    @Throws(IOException::class)
    fun read(input: DataInput): UserChanges0 {
      val size = DataInputOutputUtil.readINT(input)
      val changes = ArrayList<Change>(size)
      for (i in 0 until size) {
        changes += Change.readChange(input)
      }
      return UserChanges0(changes)
    }
  }
}
