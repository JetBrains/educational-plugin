package com.jetbrains.edu.kotlin

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.project.Project
import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.learning.kotlinVersion

open class KtCourseBuilder : GradleCourseBuilderBase() {

  override val buildGradleTemplateName: String = KOTLIN_BUILD_GRADLE_TEMPLATE_NAME
  override val taskTemplateName: String = KtConfigurator.TASK_KT
  override val mainTemplateName: String = KtConfigurator.MAIN_KT
  override val testTemplateName: String = KtConfigurator.TESTS_KT

  override fun templateVariables(project: Project): Map<String, Any> {
    return super.templateVariables(project) + getKotlinTemplateVariables()
  }

  override fun getLanguageSettings() = KtLanguageSettings()

  companion object {
    private const val KOTLIN_BUILD_GRADLE_TEMPLATE_NAME = "kotlin-build.gradle"

    @VisibleForTesting
    fun getKotlinTemplateVariables(): Map<String, Any> {
      val kotlinVersion = kotlinVersion()
      return mapOf(
        "KOTLIN_VERSION" to kotlinVersion.version
      )
    }
  }
}
