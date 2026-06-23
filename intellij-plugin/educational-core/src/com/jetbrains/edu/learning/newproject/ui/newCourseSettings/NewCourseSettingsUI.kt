package com.jetbrains.edu.learning.newproject.ui.newCourseSettings

import com.jetbrains.edu.learning.newproject.newCourseSettings.NewCourseSettings
import com.jetbrains.edu.learning.newproject.newCourseSettings.NewCourseSettingsCatalog

sealed interface NewCourseSettingsUI<out S : NewCourseSettings> {
  val catalog: NewCourseSettingsCatalog<S>

  data class List<S : NewCourseSettings>(
    override val catalog: NewCourseSettingsCatalog.List<S>,
    val presenter: NewCourseSettingsListPresenter<S>,
  ) : NewCourseSettingsUI<S>

  data object NoSettings : NewCourseSettingsUI<NewCourseSettings> {
    override val catalog: NewCourseSettingsCatalog.Empty get() = NewCourseSettingsCatalog.Empty
  }
}