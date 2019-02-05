package com.jetbrains.edu.learning.framework.impl

import com.intellij.util.io.DataInputOutputUtil
import com.intellij.util.io.IOUtil
import java.io.DataInput
import java.io.DataOutput
import java.io.IOException

class UserChanges(val changes: Map<String, String>) {

  @Throws(IOException::class)
  fun write(out: DataOutput) {
    DataInputOutputUtil.writeINT(out, changes.size)
    changes.forEach { path, text ->
      IOUtil.writeUTF(out, path)
      IOUtil.writeUTF(out, text)
    }
  }

  companion object {

    private val EMPTY = UserChanges(emptyMap())

    @JvmStatic
    fun empty(): UserChanges = EMPTY

    @Throws(IOException::class)
    fun read(input: DataInput): UserChanges {
      val size = DataInputOutputUtil.readINT(input)
      val changes = HashMap<String, String>(size)
      for (i in 0 until size) {
        val path = IOUtil.readUTF(input)
        val text = IOUtil.readUTF(input)
        changes[path] = text
      }
      return UserChanges(changes)
    }
  }
}
