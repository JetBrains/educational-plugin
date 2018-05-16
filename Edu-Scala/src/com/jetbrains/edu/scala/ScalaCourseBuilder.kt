package com.jetbrains.edu.scala

import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.intellij.GradleCourseBuilderBase
import com.jetbrains.edu.learning.intellij.JdkLanguageSettings
import com.jetbrains.edu.learning.intellij.JdkProjectSettings
import com.jetbrains.edu.learning.intellij.generation.GradleCourseProjectGenerator

class ScalaCourseBuilder : GradleCourseBuilderBase() {

  override val buildGradleTemplateName: String = SCALA_BUILD_GRADLE_TEMPLATE_NAME

  override fun getTaskTemplateName(): String = ScalaConfigurator.TASK_SCALA
  override fun getTestTemplateName(): String = ScalaConfigurator.TEST_SCALA

  override fun getLanguageSettings(): EduCourseBuilder.LanguageSettings<JdkProjectSettings> = JdkLanguageSettings()
  override fun getCourseProjectGenerator(course: Course): GradleCourseProjectGenerator =
    GradleCourseProjectGenerator(this, course)

  companion object {
    private const val SCALA_BUILD_GRADLE_TEMPLATE_NAME = "scala-build.gradle"
  }
}
