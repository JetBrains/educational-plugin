package com.jetbrains.edu.kotlin

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.learning.kotlinPluginVersion

open class KtCourseBuilder : GradleCourseBuilderBase() {

  override val buildGradleTemplateName: String = KOTLIN_BUILD_GRADLE_TEMPLATE_NAME

  override fun templateVariables(project: Project): Map<String, String> =
    super.templateVariables(project) + Pair("KOTLIN_VERSION", kotlinPluginVersion())

  override fun getTaskTemplateName(): String = KtConfigurator.TASK_KT
  override fun getTestTemplateName(): String = KtConfigurator.TESTS_KT

  companion object {
    private const val KOTLIN_BUILD_GRADLE_TEMPLATE_NAME = "kotlin-build.gradle"
  }
}
