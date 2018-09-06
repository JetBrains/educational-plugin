package com.jetbrains.edu.learning.serialization.converter

import com.jetbrains.edu.learning.EduNames

@JvmField
val LANGUAGE_TASK_ROOTS: Map<String, TaskRoots> = mapOf(
  EduNames.KOTLIN to TaskRoots("src", "test"),
  EduNames.JAVA to TaskRoots("src", "test"),
  EduNames.SCALA to TaskRoots("src", "test"),
  EduNames.ANDROID to TaskRoots("src/main", "src/test")
)

data class TaskRoots(val taskFilesRoot: String, val testFilesRoot: String)
