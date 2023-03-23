package com.jetbrains.edu.kotlin

import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.learning.kotlinVersion

open class KtCourseBuilder : GradleCourseBuilderBase() {

  override val buildGradleTemplateName: String = KOTLIN_BUILD_GRADLE_TEMPLATE_NAME
  override val taskTemplateName: String = KtConfigurator.TASK_KT
  override val mainTemplateName: String = KtConfigurator.MAIN_KT
  override val testTemplateName: String = KtConfigurator.TESTS_KT

  override val buildGradleKtsTemplateName: String = KOTLIN_BUILD_GRADLE_KOTLIN_DSL_TEMPLATE_NAME
  override val settingGradleKtsTemplateName: String = KOTLIN_SETTINGS_GRADLE_KOTLIN_DSL_TEMPLATE_NAME

  override fun getSupportedLanguageVersions(): List<String> = listOf("1.2", "1.3", "1.4", "1.5", "1.6", "1.7")

  override fun templateVariables(projectName: String): Map<String, Any> {
    return super.templateVariables(projectName) + getKotlinTemplateVariables()
  }

  override fun getLanguageSettings() = KtLanguageSettings()

  companion object {
    const val KOTLIN_BUILD_GRADLE_TEMPLATE_NAME = "kotlin-build.gradle"

    const val KOTLIN_BUILD_GRADLE_KOTLIN_DSL_TEMPLATE_NAME = "kotlin-build.gradle.kts"
    const val KOTLIN_SETTINGS_GRADLE_KOTLIN_DSL_TEMPLATE_NAME = "settings.gradle.kts"

    fun getKotlinTemplateVariables(): Map<String, Any> {
      val kotlinVersion = kotlinVersion()
      return mapOf(
        "KOTLIN_VERSION" to kotlinVersion.version
      )
    }
  }
}
