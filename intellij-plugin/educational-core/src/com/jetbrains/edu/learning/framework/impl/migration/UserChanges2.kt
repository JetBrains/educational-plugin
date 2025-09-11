package com.jetbrains.edu.learning.framework.impl.migration

import com.intellij.util.io.DataInputOutputUtil
import com.jetbrains.edu.learning.courseFormat.FileContents
import com.jetbrains.edu.learning.framework.impl.FrameworkStorageData
import com.jetbrains.edu.learning.framework.impl.UserChangesContents
import java.io.DataInput
import java.io.DataOutput
import java.io.IOException

class UserChanges2(val changes: List<Change2>, val timestamp: Long) : FrameworkStorageData {
  @Throws(IOException::class)
  override fun write(out: DataOutput) {
    DataInputOutputUtil.writeINT(out, changes.size)
    changes.forEach { it.write(out) }
    DataInputOutputUtil.writeLONG(out, timestamp)
  }

  companion object {
    @Throws(IOException::class)
    fun read(input: DataInput): UserChanges2 {
      val size = DataInputOutputUtil.readINT(input)
      val changes = ArrayList<Change2>(size)
      for (i in 0 until size) {
        changes += Change2.read(input)
      }
      val timestamp = DataInputOutputUtil.readLONG(input)
      return UserChanges2(changes, timestamp)
    }
  }
}

data class Change2(val ordinal: Int, val path: String, val contents: FileContents) {
  fun write(out: DataOutput) {
    out.writeInt(ordinal)
    out.writeUTF(path)
    UserChangesContents.write(contents, out)
  }

  companion object {
    fun read(input: DataInput): Change2 {
      val ordinal = input.readInt()
      if (ordinal !in 0..4) error("Unexpected change type: $ordinal")
      val path = input.readUTF()
      val contents = UserChangesContents.read(input)
      return Change2(ordinal, path, contents)
    }
  }
}