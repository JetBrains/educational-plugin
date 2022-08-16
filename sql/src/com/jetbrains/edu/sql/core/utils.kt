package com.jetbrains.edu.sql.core

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.profile.codeInspection.InspectionProjectProfileManager
import com.intellij.sql.inspections.configuration.SqlNoDataSourceInspection
import com.jetbrains.edu.learning.isUnitTestMode

private val LOG: Logger = Logger.getInstance(":com.jetbrains.edu.sql.core:UtilsKt")

fun disableNoDataSourceInspection(project: Project) {
  val currentProfile = InspectionProjectProfileManager.getInstance(project).currentProfile
  val inspectionShortName = SqlNoDataSourceInspection().shortName
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
