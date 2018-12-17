package com.jetbrains.edu.java

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.learning.gradle.generation.GradleCourseProjectGenerator

class JCourseBuilder : GradleCourseBuilderBase() {

  override val buildGradleTemplateName: String = JAVA_BUILD_GRADLE_TEMPLATE_NAME

  override fun getTaskTemplateName(): String = JConfigurator.TASK_JAVA
  override fun getTestTemplateName(): String = JConfigurator.TEST_JAVA
  override fun getLanguageSettings() = JavaLanguageSettings()

  override fun getCourseProjectGenerator(course: Course): GradleCourseProjectGenerator =
    JCourseProjectGenerator(this, course)

  companion object {
    private const val JAVA_BUILD_GRADLE_TEMPLATE_NAME = "java-build.gradle"
  }
}
