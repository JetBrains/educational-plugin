package com.jetbrains.edu.java

import com.jetbrains.edu.learning.intellij.GradleCourseBuilderBase

class JCourseBuilder : GradleCourseBuilderBase() {

  override val buildGradleTemplateName: String = JAVA_BUILD_GRADLE_TEMPLATE_NAME
  override val subtaskTestTemplateName: String = JConfigurator.SUBTASK_TEST_JAVA

  override fun getTaskTemplateName(): String = JConfigurator.TASK_JAVA
  override fun getTestTemplateName(): String = JConfigurator.TEST_JAVA

  companion object {
    private val JAVA_BUILD_GRADLE_TEMPLATE_NAME = "java-build.gradle"
  }
}
