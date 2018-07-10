package com.jetbrains.edu.scala

import com.jetbrains.edu.learning.intellij.GradleCourseBuilderBase

class ScalaCourseBuilder : GradleCourseBuilderBase() {

  override val buildGradleTemplateName: String = SCALA_BUILD_GRADLE_TEMPLATE_NAME

  override fun getTaskTemplateName(): String = ScalaConfigurator.TASK_SCALA
  override fun getTestTemplateName(): String = ScalaConfigurator.TEST_SCALA

  companion object {
    private const val SCALA_BUILD_GRADLE_TEMPLATE_NAME = "scala-build.gradle"
  }
}
