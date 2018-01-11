package com.jetbrains.edu.java.subtask

import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import com.jetbrains.edu.coursecreator.subtask.CCSubtaskTestBase
import com.jetbrains.edu.java.JConfigurator
import com.jetbrains.edu.java.JCourseBuilder
import com.jetbrains.edu.learning.EduCourseBuilder

class JSubtaskTest : CCSubtaskTestBase() {
  override val courseBuilder: EduCourseBuilder<*> = JCourseBuilder()
  override val taskFileName: String = JConfigurator.TASK_JAVA
  override val testFileName: String = JConfigurator.TEST_JAVA
  override val language: Language = JavaLanguage.INSTANCE
}
