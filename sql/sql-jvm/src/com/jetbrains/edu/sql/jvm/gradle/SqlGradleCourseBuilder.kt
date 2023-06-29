package com.jetbrains.edu.sql.jvm.gradle

import com.jetbrains.edu.coursecreator.actions.TemplateFileInfo
import com.jetbrains.edu.coursecreator.actions.studyItem.NewStudyItemInfo
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.jvm.gradle.generation.GradleCourseProjectGenerator
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.sql.core.SqlConfiguratorBase

open class SqlGradleCourseBuilder : GradleCourseBuilderBase() {
  override fun taskTemplateName(course: Course): String = SqlConfiguratorBase.TASK_SQL

  override fun getDefaultTaskTemplates(
    course: Course,
    info: NewStudyItemInfo,
    withSources: Boolean,
    withTests: Boolean
  ): List<TemplateFileInfo> {
    val templates = super.getDefaultTaskTemplates(course, info, withSources, withTests)
    return if (withSources) {
      templates + TemplateFileInfo(INIT_SQL, INIT_SQL, isVisible = false)
    } else {
      templates
    }
  }

  override fun getCourseProjectGenerator(course: Course): GradleCourseProjectGenerator {
    return SqlGradleCourseProjectGenerator(this, course)
  }

  override fun buildGradleTemplateName(course: Course): String {
    return when (course.sqlTestLanguage) {
      SqlTestLanguage.KOTLIN -> "sql-kotlin-build.gradle"
      SqlTestLanguage.JAVA -> "sql-java-build.gradle"
    }
  }

  override fun testTemplateName(course: Course): String {
    return when (course.sqlTestLanguage) {
      SqlTestLanguage.KOTLIN -> "SqlTest.kt"
      SqlTestLanguage.JAVA -> "SqlTest.java"
    }
  }

  override fun getLanguageSettings(): LanguageSettings<JdkProjectSettings> = SqlJdkLanguageSettings()

  companion object {
    const val INIT_SQL = "init.sql"
  }
}

