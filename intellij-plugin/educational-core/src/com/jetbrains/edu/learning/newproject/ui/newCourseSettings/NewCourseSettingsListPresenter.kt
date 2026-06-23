package com.jetbrains.edu.learning.newproject.ui.newCourseSettings

import com.intellij.openapi.util.NlsContexts
import com.jetbrains.edu.learning.newproject.newCourseSettings.NewCourseSettings
import com.jetbrains.edu.learning.newproject.newCourseSettings.NewCourseSettingsCatalog
import javax.swing.Icon

/**
 * View for [NewCourseSettings] if it is inside the [NewCourseSettingsCatalog.List].
 * Provides information for the [NewCourseSettingsComboBox].
 */
interface NewCourseSettingsListPresenter<in S : NewCourseSettings> {
  /**
   * The label written to the left of the combo box with a list of possible settings.
   */
  val label: String
    @NlsContexts.Label get

  fun name(settings: S): String
  fun icon(settings: S): Icon?
}