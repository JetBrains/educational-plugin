package com.jetbrains.edu.sql

import com.intellij.codeInspection.ex.InspectionToolRegistrar
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.profile.codeInspection.InspectionProjectProfileManager
import com.intellij.sql.dialects.SqlDialectMappings
import com.intellij.sql.dialects.generic.GenericDialect
import com.intellij.sql.inspections.configuration.SqlNoDataSourceInspection
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator

class SqlCourseProjectGenerator(
  builder: SqlCourseBuilder,
  course: Course
) : CourseProjectGenerator<Unit>(builder, course) {
  override fun afterProjectGenerated(project: Project, projectSettings: Unit) {
    super.afterProjectGenerated(project, projectSettings)
    SqlDialectMappings.getInstance(project).setMapping(null, GenericDialect.INSTANCE)
    // `SqlNoDataSourceInspection` is not needed since we don't provide any data source for now
    disableNoDataSourceInspection(project)
  }

  private fun disableNoDataSourceInspection(project: Project) {
    val inspection = InspectionToolRegistrar.getInstance().createTools().find { it.tool is SqlNoDataSourceInspection } ?: return
    val currentProfile = InspectionProjectProfileManager.getInstance(project).currentProfile
    val inspectionShortName = inspection.shortName
    val tool = currentProfile.getToolsOrNull(inspectionShortName, project)
    if (tool == null) {
      // `InspectionProjectProfileManager` doesn't load inspections automatically in tests.
      if (!isUnitTestMode) {
        LOG.warn("Can't find `$inspectionShortName` in project inspection profile")
      }
      return
    }
    currentProfile.setToolEnabled(inspectionShortName, false, project)
  }

  companion object {
    private val LOG: Logger = logger<SqlCourseProjectGenerator>()
  }
}
