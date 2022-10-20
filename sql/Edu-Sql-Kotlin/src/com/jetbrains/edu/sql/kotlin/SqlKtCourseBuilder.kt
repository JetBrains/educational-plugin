package com.jetbrains.edu.sql.kotlin

import com.jetbrains.edu.kotlin.KtConfigurator
import com.jetbrains.edu.kotlin.KtCourseBuilder
import com.jetbrains.edu.sql.jvm.gradle.SqlGradleCourseBuilderBase

class SqlKtCourseBuilder : SqlGradleCourseBuilderBase() {
  override val testTemplateName: String
    get() = KtConfigurator.TESTS_KT
  override val buildGradleTemplateName: String
    get() = KtCourseBuilder.KOTLIN_BUILD_GRADLE_TEMPLATE_NAME

  override fun templateVariables(projectName: String): Map<String, Any> {
    return super.templateVariables(projectName) + KtCourseBuilder.getKotlinTemplateVariables()
  }
}
