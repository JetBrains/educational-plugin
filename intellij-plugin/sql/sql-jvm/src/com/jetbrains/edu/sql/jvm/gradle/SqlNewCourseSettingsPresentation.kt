package com.jetbrains.edu.sql.jvm.gradle

import com.jetbrains.edu.learning.newproject.ui.newCourseSettings.NewCourseSettingsListPresenter
import com.jetbrains.edu.sql.core.EduSqlBundle
import javax.swing.Icon

object SqlNewCourseSettingsPresentation : NewCourseSettingsListPresenter<SqlNewCourseSettings> {
  override fun label(): String = EduSqlBundle.message("edu.sql.test.language")

  override fun name(settings: SqlNewCourseSettings): String {
    return settings.testLanguage.getLanguage()?.displayName ?: ""
  }
  override fun icon(settings: SqlNewCourseSettings): Icon = settings.testLanguage.logo
}