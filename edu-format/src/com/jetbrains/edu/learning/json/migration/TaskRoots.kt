package com.jetbrains.edu.learning.json.migration

import com.jetbrains.edu.learning.json.migration.MigrationNames.JAVA
import com.jetbrains.edu.learning.json.migration.MigrationNames.KOTLIN
import com.jetbrains.edu.learning.json.migration.MigrationNames.SCALA

@JvmField
val LANGUAGE_TASK_ROOTS: Map<String, TaskRoots> = mapOf(
  KOTLIN to TaskRoots("src", "test"),
  JAVA to TaskRoots("src", "test"),
  SCALA to TaskRoots("src", "test"),
  // For test purposes
  "FakeGradleBasedLanguage" to TaskRoots("src", "test")
)

data class TaskRoots(val taskFilesRoot: String, val testFilesRoot: String)
