package com.jetbrains.edu.sql.java

import com.jetbrains.edu.java.JConfigurator
import com.jetbrains.edu.java.JCourseBuilder
import com.jetbrains.edu.sql.jvm.gradle.SqlGradleCourseBuilderBase

class SqlJCourseBuilder : SqlGradleCourseBuilderBase() {
  override val testTemplateName: String
    get() = JConfigurator.TEST_JAVA
  override val buildGradleTemplateName: String
    get() = JCourseBuilder.JAVA_BUILD_GRADLE_TEMPLATE_NAME
}
