package com.jetbrains.edu.learning.storage

import com.intellij.openapi.Disposable

interface LearningObjectsStorage : Disposable {
  val writeTextInYaml: Boolean
  fun load(key: String): ByteArray
  fun store(key: String, value: ByteArray)
}

fun LearningObjectsStorage.store(key: String, value: String) = store(key, value.toByteArray())