package com.jetbrains.edu.scala.gradle

import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase

class ScalaGradleCourseBuilder : GradleCourseBuilderBase() {

  override val buildGradleTemplateName: String = SCALA_BUILD_GRADLE_TEMPLATE_NAME
  override val taskTemplateName: String = ScalaGradleConfigurator.TASK_SCALA
  override val testTemplateName: String = ScalaGradleConfigurator.TEST_SCALA

  companion object {
    private const val SCALA_BUILD_GRADLE_TEMPLATE_NAME = "scala-build.gradle"
  }
}
