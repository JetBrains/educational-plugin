package com.jetbrains.edu.kotlin

import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.learning.courseFormat.Course

open class KtCourseBuilder : GradleCourseBuilderBase() {

  override fun buildGradleTemplateName(course: Course): String = KOTLIN_BUILD_GRADLE_TEMPLATE_NAME
  override fun taskTemplateName(course: Course): String = KtConfigurator.TASK_KT
  override fun mainTemplateName(course: Course): String = KtConfigurator.MAIN_KT
  override fun testTemplateName(course: Course): String = KtConfigurator.TESTS_KT

  companion object {
    const val KOTLIN_BUILD_GRADLE_TEMPLATE_NAME = "kotlin-build.gradle"
  }
}
