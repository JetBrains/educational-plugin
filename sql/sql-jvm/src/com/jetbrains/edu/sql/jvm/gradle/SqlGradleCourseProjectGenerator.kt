package com.jetbrains.edu.sql.jvm.gradle

import com.intellij.openapi.project.Project
import com.intellij.sql.dialects.SqlDialectMappings
import com.intellij.sql.dialects.generic.GenericDialect
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.jvm.gradle.generation.GradleCourseProjectGenerator
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.sql.core.disableNoDataSourceInspection

class SqlGradleCourseProjectGenerator(
  builder: SqlGradleCourseBuilderBase,
  course: Course
) : GradleCourseProjectGenerator(builder, course) {

  override fun afterProjectGenerated(project: Project, projectSettings: JdkProjectSettings) {
    super.afterProjectGenerated(project, projectSettings)
    SqlDialectMappings.getInstance(project).setMapping(null, GenericDialect.INSTANCE)
    // `SqlNoDataSourceInspection` is not needed since we don't provide any data source for now
    disableNoDataSourceInspection(project)
  }
}
