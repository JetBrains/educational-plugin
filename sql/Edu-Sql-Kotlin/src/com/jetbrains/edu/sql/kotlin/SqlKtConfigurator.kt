package com.jetbrains.edu.sql.kotlin

import com.jetbrains.edu.kotlin.KtConfigurator
import com.jetbrains.edu.sql.jvm.gradle.SqlGradleConfigurator

class SqlKtConfigurator : SqlGradleConfigurator() {
  override val courseBuilder: SqlKtCourseBuilder
    get() = SqlKtCourseBuilder()

  override val testFileName: String
    get() = KtConfigurator.TESTS_KT
}
