package com.jetbrains.edu.learning.framework.impl.migration

import com.intellij.util.io.DataInputOutputUtil
import com.jetbrains.edu.learning.framework.impl.FrameworkStorageData
import java.io.DataInput
import java.io.DataOutput
import java.io.IOException

data class UserChanges0(val changes: List<Change0>) : FrameworkStorageData {
  @Throws(IOException::class)
  override fun write(out: DataOutput) {
    DataInputOutputUtil.writeINT(out, changes.size)
    changes.forEach { it.write(out) }
  }

  companion object {
    @Throws(IOException::class)
    fun read(input: DataInput): UserChanges0 {
      val size = DataInputOutputUtil.readINT(input)
      val changes = ArrayList<Change0>(size)
      for (i in 0 until size) {
        changes += Change0.read(input)
      }
      return UserChanges0(changes)
    }
  }
}

data class Change0(val ordinal: Int, val path: String, val text: String) {
  fun write(out: DataOutput) {
    out.writeInt(ordinal)
    out.writeUTF(path)
    out.writeUTF(text)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Change0) return false
    if (javaClass != other.javaClass) return false

    if (path != other.path) return false
    if (text != other.text) return false

    return true
  }

  override fun hashCode(): Int {
    var result = path.hashCode()
    result = 31 * result + text.hashCode()
    return result
  }

  companion object {
    fun read(input: DataInput): Change0 {
      val ordinal = input.readInt()
      if (ordinal !in 0..4) error("Unexpected change type: $ordinal")
      val path = input.readUTF()
      val text = input.readUTF()
      return Change0(ordinal, path, text)
    }

    fun addFile(path: String, text: String) = Change0(0, path, text)
    fun changeFile(path: String, text: String) = Change0(2, path, text)
  }
}
