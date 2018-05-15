package com.jetbrains.edu.kotlin

import com.intellij.util.text.VersionComparatorUtil
import com.jetbrains.edu.learning.intellij.GradleCourseBuilderBase
import com.jetbrains.edu.learning.pluginVersion

open class KtCourseBuilder : GradleCourseBuilderBase() {

  override val buildGradleTemplateName: String = KOTLIN_BUILD_GRADLE_TEMPLATE_NAME

  override val buildGradleVariables: Map<String, String>
    get() = super.buildGradleVariables + Pair("KOTLIN_VERSION", kotlinPluginVersion)

  override fun getTaskTemplateName(): String = KtConfigurator.TASK_KT
  override fun getTestTemplateName(): String = KtConfigurator.TESTS_KT

  companion object {
    private const val KOTLIN_BUILD_GRADLE_TEMPLATE_NAME = "kotlin-build.gradle"
    private const val KOTLIN_PLUGIN_ID = "org.jetbrains.kotlin"
    private const val DEFAULT_KOTLIN_VERSION = "1.2.41"

    private val kotlinPluginVersion: String get() {
      val kotlinPluginVersion = pluginVersion(KOTLIN_PLUGIN_ID)?.takeWhile { it != '-' } ?: DEFAULT_KOTLIN_VERSION
      return VersionComparatorUtil.max(kotlinPluginVersion, DEFAULT_KOTLIN_VERSION)
    }
  }
}
