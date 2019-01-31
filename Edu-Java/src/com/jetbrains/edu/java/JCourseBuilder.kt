package com.jetbrains.edu.java

import com.jetbrains.edu.learning.gradle.GradleCourseBuilderBase

open class JCourseBuilder : GradleCourseBuilderBase() {

  override val buildGradleTemplateName: String = JAVA_BUILD_GRADLE_TEMPLATE_NAME

  override fun getTaskTemplateName(): String = JConfigurator.TASK_JAVA
  override fun getTestTemplateName(): String = JConfigurator.TEST_JAVA
  override fun getLanguageSettings() = JLanguageSettings()

  companion object {
    private const val JAVA_BUILD_GRADLE_TEMPLATE_NAME = "java-build.gradle"
  }
}
