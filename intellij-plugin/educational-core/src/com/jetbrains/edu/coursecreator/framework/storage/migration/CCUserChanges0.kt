package com.jetbrains.edu.coursecreator.framework.storage.migration

import com.intellij.util.io.DataInputOutputUtil
import com.jetbrains.edu.learning.framework.impl.FrameworkStorageData
import java.io.DataInput
import java.io.DataOutput

data class CCUserChanges0(val state: Map<String, String>) : FrameworkStorageData {
  override fun write(out: DataOutput) {
    DataInputOutputUtil.writeINT(out, state.size)
    state.forEach {
      out.writeUTF(it.key)
      out.writeUTF(it.value)
    }
  }

  companion object {
    fun read(input: DataInput): CCUserChanges0 {
      val size = DataInputOutputUtil.readINT(input)
      val state = mutableMapOf<String, String>()
      for (i in 0 until size) {
        val key = input.readUTF()
        val value = input.readUTF()
        state[key] = value
      }
      return CCUserChanges0(state)
    }
  }
}