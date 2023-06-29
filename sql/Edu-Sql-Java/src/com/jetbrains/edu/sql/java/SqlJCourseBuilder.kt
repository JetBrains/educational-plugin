package com.jetbrains.edu.sql.java

import com.jetbrains.edu.java.JConfigurator
import com.jetbrains.edu.java.JCourseBuilder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.sql.jvm.gradle.SqlGradleCourseBuilder

class SqlJCourseBuilder : SqlGradleCourseBuilder() {
  override fun testTemplateName(course: Course): String = JConfigurator.TEST_JAVA
  override fun buildGradleTemplateName(course: Course): String = JCourseBuilder.JAVA_BUILD_GRADLE_TEMPLATE_NAME
}
