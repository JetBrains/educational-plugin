package com.jetbrains.edu.learning.framework.impl.migration

import com.intellij.util.io.DataInputOutputUtil
import com.jetbrains.edu.learning.framework.impl.FrameworkStorageData
import java.io.DataInput
import java.io.DataOutput
import java.io.IOException

data class UserChanges1(val changes: List<Change1>, val timestamp: Long) : FrameworkStorageData {
  @Throws(IOException::class)
  override fun write(out: DataOutput) {
    DataInputOutputUtil.writeINT(out, changes.size)
    changes.forEach { it.write(out) }
    DataInputOutputUtil.writeLONG(out, timestamp)
  }

  companion object {
    @Throws(IOException::class)
    fun read(input: DataInput): UserChanges1 {
      val size = DataInputOutputUtil.readINT(input)
      val changes = ArrayList<Change1>(size)
      for (i in 0 until size) {
        changes += Change1.read(input)
      }
      val timestamp = DataInputOutputUtil.readLONG(input)
      return UserChanges1(changes, timestamp)
    }
  }
}

typealias Change1 = Change0