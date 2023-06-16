package com.jetbrains.edu.kotlin

import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.kotlinVersion

open class KtCourseBuilder : GradleCourseBuilderBase() {

  override fun buildGradleTemplateName(course: Course): String = KOTLIN_BUILD_GRADLE_TEMPLATE_NAME
  override fun taskTemplateName(course: Course): String = KtConfigurator.TASK_KT
  override fun mainTemplateName(course: Course): String = KtConfigurator.MAIN_KT
  override fun testTemplateName(course: Course): String = KtConfigurator.TESTS_KT

  override fun getSupportedLanguageVersions(): List<String> = listOf("1.2", "1.3", "1.4", "1.5", "1.6", "1.7")

  override fun templateVariables(projectName: String): Map<String, Any> {
    return super.templateVariables(projectName) + getKotlinTemplateVariables()
  }

  override fun getLanguageSettings() = KtLanguageSettings()

  companion object {
    const val KOTLIN_BUILD_GRADLE_TEMPLATE_NAME = "kotlin-build.gradle"

    fun getKotlinTemplateVariables(): Map<String, Any> {
      val kotlinVersion = kotlinVersion()
      return mapOf(
        "KOTLIN_VERSION" to kotlinVersion.version
      )
    }
  }
}
