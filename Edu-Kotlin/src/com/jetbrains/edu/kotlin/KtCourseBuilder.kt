package com.jetbrains.edu.kotlin

import com.jetbrains.edu.learning.intellij.GradleCourseBuilderBase

open class KtCourseBuilder : GradleCourseBuilderBase() {

  override val buildGradleTemplateName: String = KOTLIN_BUILD_GRADLE_TEMPLATE_NAME
  override val taskTemplateName: String = KtConfigurator.TASK_KT
  override val testTemplateName: String = KtConfigurator.TESTS_KT
  override val subtaskTestTemplateName: String = KtConfigurator.SUBTASK_TESTS_KT

  companion object {
    private const val KOTLIN_BUILD_GRADLE_TEMPLATE_NAME = "kotlin-build.gradle"
  }
}
