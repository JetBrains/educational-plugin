package com.jetbrains.edu.sql.jvm.gradle

import com.jetbrains.edu.coursecreator.actions.TemplateFileInfo
import com.jetbrains.edu.coursecreator.actions.studyItem.NewStudyItemInfo
import com.jetbrains.edu.jvm.environment.JdkLanguageEnvironment
import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.jvm.gradle.JdkEnvironmentPresenter
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.ui.EnvironmentAndNewCourseSettings
import com.jetbrains.edu.learning.newproject.ui.newCourseSettings.NewCourseSettingsUI
import com.jetbrains.edu.sql.core.SqlConfiguratorBase
import org.jetbrains.annotations.VisibleForTesting

class SqlGradleCourseBuilder : GradleCourseBuilderBase() {
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
    }
    else {
      templates
    }
  }

  override fun buildGradleTemplateName(course: Course): String {
    return when (course.sqlTestLanguage) {
      SqlTestLanguage.KOTLIN -> SQL_KOTLIN_BUILD_GRADLE_TEMPLATE_NAME
      SqlTestLanguage.JAVA -> SQL_JAVA_BUILD_GRADLE_TEMPLATE_NAME
    }
  }

  override fun testTemplateName(course: Course): String {
    return when (course.sqlTestLanguage) {
      SqlTestLanguage.KOTLIN -> "SqlTest.kt"
      SqlTestLanguage.JAVA -> "SqlTest.java"
    }
  }

  override fun getLanguageSettings(): LanguageSettings<JdkLanguageEnvironment> = EnvironmentAndNewCourseSettings(
    getLanguageEnvironmentCatalogProvider(),
    JdkEnvironmentPresenter(),
    NewCourseSettingsUI.List(
      SqlNewCourseSettingsCatalog,
      SqlNewCourseSettingsPresentation
    )
  )

  companion object {
    const val INIT_SQL = "init.sql"

    @VisibleForTesting
    const val SQL_KOTLIN_BUILD_GRADLE_TEMPLATE_NAME = "sql-kotlin-build.gradle"

    @VisibleForTesting
    const val SQL_JAVA_BUILD_GRADLE_TEMPLATE_NAME = "sql-java-build.gradle"
  }
}

