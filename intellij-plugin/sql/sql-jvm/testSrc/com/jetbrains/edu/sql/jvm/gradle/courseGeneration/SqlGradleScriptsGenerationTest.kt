package com.jetbrains.edu.sql.jvm.gradle.courseGeneration

import com.intellij.sql.psi.SqlLanguage
import com.jetbrains.edu.jvm.courseGeneration.GradleScriptsGenerationTestBase
import com.jetbrains.edu.learning.CourseBuilder
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.sql.jvm.gradle.SqlGradleCourseBuilder.Companion.SQL_JAVA_BUILD_GRADLE_TEMPLATE_NAME
import com.jetbrains.edu.sql.jvm.gradle.SqlGradleCourseBuilder.Companion.SQL_KOTLIN_BUILD_GRADLE_TEMPLATE_NAME
import com.jetbrains.edu.sql.jvm.gradle.SqlGradleStartupActivity
import com.jetbrains.edu.sql.jvm.gradle.SqlTestLanguage
import com.jetbrains.edu.sql.jvm.gradle.sqlTestLanguage

abstract class SqlGradleScriptsGenerationTest(private val sqlLanguage: SqlTestLanguage) : GradleScriptsGenerationTestBase() {
  override fun setUp() {
    super.setUp()
    SqlGradleStartupActivity.disable(testRootDisposable)
  }

  override fun createCourse(courseMode: CourseMode, buildCourse: CourseBuilder.() -> Unit): Course {
    return course(courseMode = courseMode, language = SqlLanguage.INSTANCE, buildCourse = buildCourse).apply {
      sqlTestLanguage = sqlLanguage
    }
  }
}

class SqlKotlinGradleScriptsGenerationTest : SqlGradleScriptsGenerationTest(SqlTestLanguage.KOTLIN) {
  override val defaultBuildGradleTemplateName: String = SQL_KOTLIN_BUILD_GRADLE_TEMPLATE_NAME
}

class SqlJavaGradleScriptsGenerationTest : SqlGradleScriptsGenerationTest(SqlTestLanguage.JAVA) {
  override val defaultBuildGradleTemplateName: String = SQL_JAVA_BUILD_GRADLE_TEMPLATE_NAME
}