package com.jetbrains.edu.sql.java

import com.jetbrains.edu.java.JConfigurator
import com.jetbrains.edu.sql.jvm.gradle.SqlGradleConfigurator

class SqlJConfigurator : SqlGradleConfigurator() {
  override val courseBuilder: SqlJCourseBuilder
    get() = SqlJCourseBuilder()

  override val testFileName: String
    get() = JConfigurator.TEST_JAVA
}
