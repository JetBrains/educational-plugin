package com.jetbrains.edu.coursecreator.framework.storage

import com.intellij.util.io.DataInputOutputUtil
import com.jetbrains.edu.learning.courseFormat.FileContents
import com.jetbrains.edu.learning.framework.impl.FLTaskState
import com.jetbrains.edu.learning.framework.impl.FrameworkStorageData
import com.jetbrains.edu.learning.framework.impl.UserChangesContents
import java.io.DataInput
import java.io.DataOutput
import java.io.IOException

data class CCUserChanges(val state: FLTaskState) : FrameworkStorageData {
  @Throws(IOException::class)
  override fun write(out: DataOutput) {
    DataInputOutputUtil.writeINT(out, state.size)
    state.forEach {
      out.writeUTF(it.key)
      UserChangesContents.write(it.value, out)
    }
  }

  companion object {
    val EMPTY = CCUserChanges(emptyMap())

    fun read(input: DataInput): CCUserChanges {
      val size = DataInputOutputUtil.readINT(input)
      val state = mutableMapOf<String, FileContents>()
      for (i in 0 until size) {
        val key = input.readUTF()
        val value = UserChangesContents.read(input)
        state[key] = value
      }
      return CCUserChanges(state)
    }
  }
}