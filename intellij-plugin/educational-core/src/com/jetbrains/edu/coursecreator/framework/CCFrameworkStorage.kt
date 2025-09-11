package com.jetbrains.edu.coursecreator.framework

import com.intellij.util.io.DataInputOutputUtil
import com.jetbrains.edu.learning.framework.impl.FrameworkStorageBase
import java.io.DataInput
import java.io.DataOutput
import java.io.IOException
import java.nio.file.Path

class CCFrameworkStorage(storagePath: Path) : FrameworkStorageBase(storagePath) {
  @Throws(IOException::class)
  fun updateState(record: Int, state: FLTaskStateCC): Int {
    return withWriteLock<Int, IOException> {
      val id = if (record == -1) createNewRecord() else record
      writeStream(id, true).use { state.write(it) }
      id
    }
  }

  @Throws(IOException::class)
  fun getState(record: Int): FLTaskStateCC {
    return if (record == -1) {
      emptyMap()
    }
    else {
      withReadLock<FLTaskStateCC, IOException> {
        readStream(record).use(::readState)
      }
    }
  }

  @Throws(IOException::class)
  private fun FLTaskStateCC.write(out: DataOutput) {
    DataInputOutputUtil.writeINT(out, size)
    forEach {
      out.writeUTF(it.key)
      out.writeUTF(it.value)
    }
  }

  private fun readState(input: DataInput): FLTaskStateCC {
    val size = DataInputOutputUtil.readINT(input)
    val state = mutableMapOf<String, String>()
    for (i in 0 until size) {
      val key = input.readUTF()
      val value = input.readUTF()
      state[key] = value
    }
    return state
  }

  override fun migrateRecord(recordId: Int, currentVersion: Int, newVersion: Int) {
    error("Could not migrate record from $currentVersion to $newVersion")
  }
}