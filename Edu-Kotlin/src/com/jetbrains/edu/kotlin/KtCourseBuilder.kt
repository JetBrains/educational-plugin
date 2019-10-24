package com.jetbrains.edu.kotlin

import com.intellij.openapi.project.Project
import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.learning.kotlinVersion

open class KtCourseBuilder : GradleCourseBuilderBase() {

  override val buildGradleTemplateName: String = KOTLIN_BUILD_GRADLE_TEMPLATE_NAME
  override val taskTemplateName: String = KtConfigurator.TASK_KT
  override val testTemplateName: String = KtConfigurator.TESTS_KT

  override fun templateVariables(project: Project): Map<String, Any> {
    val kotlinVersion = kotlinVersion()
    return super.templateVariables(project) + mapOf(
      "KOTLIN_VERSION" to kotlinVersion.version,
      "NEED_KOTLIN_EAP_REPOSITORY" to !kotlinVersion.isRelease
    )
  }

  override fun getLanguageSettings() = KtLanguageSettings()

  companion object {
    private const val KOTLIN_BUILD_GRADLE_TEMPLATE_NAME = "kotlin-build.gradle"
  }
}
