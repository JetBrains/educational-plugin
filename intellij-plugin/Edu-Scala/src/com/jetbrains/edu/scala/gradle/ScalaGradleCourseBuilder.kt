package com.jetbrains.edu.scala.gradle

import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.learning.courseFormat.Course
import org.jetbrains.annotations.VisibleForTesting

open class ScalaGradleCourseBuilder : GradleCourseBuilderBase() {

  override fun buildGradleTemplateName(course: Course): String = SCALA_BUILD_GRADLE_TEMPLATE_NAME
  override fun taskTemplateName(course: Course): String = ScalaGradleConfigurator.TASK_SCALA
  override fun mainTemplateName(course: Course): String = ScalaGradleConfigurator.MAIN_SCALA
  override fun testTemplateName(course: Course): String = ScalaGradleConfigurator.TEST_SCALA

  companion object {
    @VisibleForTesting
    const val SCALA_BUILD_GRADLE_TEMPLATE_NAME = "scala-build.gradle"
  }
}
