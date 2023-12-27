package com.jetbrains.edu.learning.storage

enum class LearningObjectStorageType {
  InMemory,
  YAML,
  SQLite;

  companion object {

    fun safeValueOf(name: String?): LearningObjectStorageType? {
      return values().find { it.name == name }
    }
  }
}