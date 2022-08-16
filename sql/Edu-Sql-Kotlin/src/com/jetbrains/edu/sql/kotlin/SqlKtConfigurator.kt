package com.jetbrains.edu.sql.kotlin

import com.jetbrains.edu.kotlin.KtConfigurator
import com.jetbrains.edu.sql.jvm.gradle.SqlGradleConfiguratorBase

class SqlKtConfigurator : SqlGradleConfiguratorBase() {
  override val courseBuilder: SqlKtCourseBuilder
    get() = SqlKtCourseBuilder()

  override val testFileName: String
    get() = KtConfigurator.TESTS_KT
}
