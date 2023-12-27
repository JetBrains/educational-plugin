package com.jetbrains.edu.learning.storage

import java.util.concurrent.ConcurrentHashMap

open class InMemoryLearningObjectsStorage : LearningObjectsStorage {

  private val storage = ConcurrentHashMap<String, ByteArray>()
  override val writeTextInYaml = false

  override fun load(key: String) = storage[key] ?: EMPTY_BYTE_ARRAY

  override fun store(key: String, value: ByteArray) {
    storage[key] = value
  }

  fun clear() {
    storage.clear()
  }

  override fun dispose() {
    storage.clear()
  }

  companion object {
    private val EMPTY_BYTE_ARRAY = byteArrayOf()
  }
}