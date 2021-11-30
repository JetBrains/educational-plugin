package com.jetbrains.edu.sql

import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.isFeatureEnabled
import icons.DatabaseIcons
import javax.swing.Icon

class SqlConfigurator : EduConfigurator<Unit> {
  override val courseBuilder: SqlCourseBuilder
    get() = SqlCourseBuilder()
  override val testFileName: String
    get() = ""
  override val taskCheckerProvider: TaskCheckerProvider
    get() = SqlTaskCheckerProvider()

  override val logo: Icon
    get() = DatabaseIcons.Sql

  override val isEnabled: Boolean
    get() = isFeatureEnabled(EduExperimentalFeatures.SQL_COURSES)
}
