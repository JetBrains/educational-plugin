package com.jetbrains.edu.learning.serialization.converter

import com.jetbrains.edu.learning.EduNames

@JvmField
val LANGUAGE_TASK_ROOTS: Map<String, TaskRoots> = mapOf(
  EduNames.KOTLIN to TaskRoots("src", "test"),
  EduNames.JAVA to TaskRoots("src", "test"),
  EduNames.SCALA to TaskRoots("src", "test"),
  // TODO: ask Senya if we need this. Migrate if so.
  // EduNames.ANDROID to TaskRoots("src/main", "src/test"),
  // For test purposes
  "FakeGradleBasedLanguage" to TaskRoots("src", "test")
)

data class TaskRoots(val taskFilesRoot: String, val testFilesRoot: String)
