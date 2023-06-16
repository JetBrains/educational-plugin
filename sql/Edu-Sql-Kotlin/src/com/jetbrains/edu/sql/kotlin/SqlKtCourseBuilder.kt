package com.jetbrains.edu.sql.kotlin

import com.jetbrains.edu.kotlin.KtConfigurator
import com.jetbrains.edu.kotlin.KtCourseBuilder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.sql.jvm.gradle.SqlGradleCourseBuilderBase

class SqlKtCourseBuilder : SqlGradleCourseBuilderBase() {
  override fun testTemplateName(course: Course): String = KtConfigurator.TESTS_KT
  override fun buildGradleTemplateName(course: Course): String = KtCourseBuilder.KOTLIN_BUILD_GRADLE_TEMPLATE_NAME

  override fun templateVariables(projectName: String): Map<String, Any> {
    return super.templateVariables(projectName) + KtCourseBuilder.getKotlinTemplateVariables()
  }
}
