package com.jetbrains.edu.java.subtask

import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import com.jetbrains.edu.coursecreator.subtask.CCSubtaskTestBase
import com.jetbrains.edu.java.JConfigurator

class JSubtaskTest : CCSubtaskTestBase() {
  override val srcDirPath: String = "lesson1/task1/src"
  override val testDirPath: String = "lesson1/task1/test"
  override val taskFileName: String = JConfigurator.TASK_JAVA
  override val testFileName: String = JConfigurator.TEST_JAVA
  override val language: Language = JavaLanguage.INSTANCE

  override fun shouldRunTest(): Boolean = false
}
