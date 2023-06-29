package com.jetbrains.edu.sql.kotlin

import com.jetbrains.edu.kotlin.KtConfigurator
import com.jetbrains.edu.kotlin.KtCourseBuilder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.sql.jvm.gradle.SqlGradleCourseBuilder

class SqlKtCourseBuilder : SqlGradleCourseBuilder() {
  override fun testTemplateName(course: Course): String = KtConfigurator.TESTS_KT
  override fun buildGradleTemplateName(course: Course): String = KtCourseBuilder.KOTLIN_BUILD_GRADLE_TEMPLATE_NAME
}
